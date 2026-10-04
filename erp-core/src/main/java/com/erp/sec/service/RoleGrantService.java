package com.erp.sec.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.SecurityContextHelper;
import com.erp.sec.domain.ModuleRegistryDomain;
import com.erp.sec.domain.RoleActionGrantDomain;
import com.erp.sec.domain.RoleDomain;
import com.erp.sec.domain.RoleModuleGrantDomain;
import com.erp.sec.domain.RoleScreenGrantDomain;
import com.erp.sec.dto.ModuleGrantRevokeResponse;
import com.erp.sec.dto.RoleActionGrantNodeResponse;
import com.erp.sec.dto.RoleActionGrantRequest;
import com.erp.sec.dto.RoleActionGrantResponse;
import com.erp.sec.dto.RoleGrantTreeResponse;
import com.erp.sec.dto.RoleModuleGrantNodeResponse;
import com.erp.sec.dto.RoleModuleGrantRequest;
import com.erp.sec.dto.RoleModuleGrantResponse;
import com.erp.sec.dto.RoleScreenGrantNodeResponse;
import com.erp.sec.dto.RoleScreenGrantRequest;
import com.erp.sec.dto.RoleScreenGrantResponse;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.RoleActionGrant;
import com.erp.sec.entity.RoleModuleGrant;
import com.erp.sec.entity.RoleScreenGrant;
import com.erp.sec.entity.ScreenRegistry;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.mapper.RoleActionGrantMapper;
import com.erp.sec.mapper.RoleMapper;
import com.erp.sec.mapper.RoleModuleGrantMapper;
import com.erp.sec.mapper.RoleScreenGrantMapper;
import com.erp.sec.repository.ActionRegistryRepository;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.ModuleRegistryRepository;
import com.erp.sec.repository.RoleActionGrantRepository;
import com.erp.sec.repository.RoleModuleGrantRepository;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.RoleScreenGrantRepository;
import com.erp.sec.repository.ScreenRegistryRepository;
import com.erp.sec.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration for the three grant levels — ENT-SEC-007/008/009, API-SEC-014/015/016/017.
 * RULE-SEC-001/002/005/007 are decided by the grant Domain objects; RULE-SEC-003's cascade is
 * this service's own action (DATA-DOM-TRANSACTIONAL.md ENT-SEC-007, owner layer: service).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleGrantService {

    /** AUDIT_EVENT_TYPE codes (CHK_SEC_AUDIT_LOG_EVENT_TYPE) these APIs append. */
    private static final String EVENT_MODULE_GRANTED = "MODULE_GRANTED";
    private static final String EVENT_MODULE_REVOKED = "MODULE_REVOKED";
    private static final String EVENT_SCREEN_GRANTED = "SCREEN_GRANTED";
    private static final String EVENT_SCREEN_REVOKED = "SCREEN_REVOKED";
    private static final String EVENT_ACTION_GRANTED = "ACTION_GRANTED";
    private static final String EVENT_ACTION_REVOKED = "ACTION_REVOKED";

    private final RoleModuleGrantRepository roleModuleGrantRepository;
    private final RoleScreenGrantRepository roleScreenGrantRepository;
    private final RoleActionGrantRepository roleActionGrantRepository;
    private final RoleRepository roleRepository;
    private final ModuleRegistryRepository moduleRegistryRepository;
    private final ScreenRegistryRepository screenRegistryRepository;
    private final ActionRegistryRepository actionRegistryRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final UserRepository userRepository;
    private final RoleMapper roleMapper;
    private final RoleModuleGrantMapper moduleGrantMapper;
    private final RoleScreenGrantMapper screenGrantMapper;
    private final RoleActionGrantMapper actionGrantMapper;

    /**
     * Everything the role currently holds, nested module → screen → action. Three flat reads,
     * grouped in memory — never a query per row, the same shape API-SEC-021's tree is assembled
     * with. Gated on PERM_SEC_ROLES_VIEW: this is a read of the SEC_ROLES screen, and the writes
     * above keep their own UPDATE gate.
     *
     * <p>The tree is built from the union of what the three grant levels reach, not from the
     * module grants alone, so a screen or action grant whose parent grant is missing still
     * appears — under a node flagged {@code granted = false}. RULE-SEC-001/002 forbid creating
     * that state through the API, but an audit read that silently dropped it would hide the one
     * thing worth finding.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_ROLES_VIEW)")
    public ServiceResult<RoleGrantTreeResponse> grantsOf(Long roleId) {
        log.debug("Reading the grant tree of Role ID: {}", roleId);

        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_ROLE, roleId));

        List<RoleModuleGrant> moduleGrants = roleModuleGrantRepository.findAllByRoleWithModule(roleId);
        List<RoleScreenGrant> screenGrants = roleScreenGrantRepository.findAllByRoleWithScreen(roleId);
        List<RoleActionGrant> actionGrants = roleActionGrantRepository.findAllByRoleWithAction(roleId);

        Map<Long, RoleModuleGrant> moduleGrantsByModule = moduleGrants.stream().collect(
            Collectors.toMap(g -> g.getModule().getModuleRegPk(), Function.identity()));
        Map<Long, RoleScreenGrant> screenGrantsByScreen = screenGrants.stream().collect(
            Collectors.toMap(g -> g.getScreen().getScreenRegPk(), Function.identity()));
        Map<Long, List<RoleActionGrantNodeResponse>> actionNodesByScreen = actionGrants.stream()
            .sorted(Comparator.comparing(g -> g.getAction().getActionCode()))
            .collect(Collectors.groupingBy(g -> g.getAction().getScreen().getScreenRegPk(),
                LinkedHashMap::new,
                Collectors.mapping(actionGrantMapper::toNodeResponse, Collectors.toList())));

        // Every screen the role reaches, whether through its own grant or only through an action.
        Map<Long, ScreenRegistry> screens = new LinkedHashMap<>();
        screenGrants.forEach(g -> screens.putIfAbsent(g.getScreen().getScreenRegPk(), g.getScreen()));
        actionGrants.forEach(g -> {
            ScreenRegistry screen = g.getAction().getScreen();
            screens.putIfAbsent(screen.getScreenRegPk(), screen);
        });

        Map<Long, List<RoleScreenGrantNodeResponse>> screenNodesByModule = new LinkedHashMap<>();
        Map<Long, ModuleRegistry> modules = new LinkedHashMap<>();
        moduleGrants.forEach(g -> modules.putIfAbsent(g.getModule().getModuleRegPk(), g.getModule()));
        screens.values().stream()
            .sorted(Comparator.comparing(ScreenRegistry::getPageCode))
            .forEach(screen -> {
                ModuleRegistry module = screen.getModule();
                modules.putIfAbsent(module.getModuleRegPk(), module);
                screenNodesByModule
                    .computeIfAbsent(module.getModuleRegPk(), key -> new ArrayList<>())
                    .add(screenGrantMapper.toNodeResponse(screen,
                        screenGrantsByScreen.get(screen.getScreenRegPk()),
                        actionNodesByScreen.getOrDefault(screen.getScreenRegPk(), List.of())));
            });

        List<RoleModuleGrantNodeResponse> moduleNodes = modules.values().stream()
            .sorted(Comparator.comparing(ModuleRegistry::getCode))
            .map(module -> moduleGrantMapper.toNodeResponse(module,
                moduleGrantsByModule.get(module.getModuleRegPk()),
                screenNodesByModule.getOrDefault(module.getModuleRegPk(), List.of())))
            .toList();

        return ServiceResult.success(roleMapper.toGrantTreeResponse(role, moduleNodes));
    }

    /** API-SEC-014 — an inactive role or module resolves as a load-time not-found (no code of its own). */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_ROLES_UPDATE)")
    public ServiceResult<RoleModuleGrantResponse> grantModule(Long roleId, RoleModuleGrantRequest request) {
        log.info("Granting module {} to Role ID: {}", request.getModuleId(), roleId);

        Role role = roleRepository.findById(roleId)
            .filter(loaded -> RoleDomain.from(loaded).isActive())
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_ROLE, roleId));
        ModuleRegistry module = moduleRegistryRepository.findById(request.getModuleId())
            .filter(loaded -> ModuleRegistryDomain.from(loaded).isActive())
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_MODULE, request.getModuleId()));

        boolean grantExists = roleModuleGrantRepository
            .existsByRole_RolePkAndModule_ModuleRegPk(roleId, module.getModuleRegPk());
        RoleModuleGrantDomain.create(roleId, module.getModuleRegPk(), grantExists);

        String principal = SecurityContextHelper.getCurrentUsername();
        RoleModuleGrant grant = moduleGrantMapper.toEntity(role, module);
        grant.setGrantedBy(principal);
        RoleModuleGrant saved = roleModuleGrantRepository.save(grant);

        appendAudit(EVENT_MODULE_GRANTED, principal, roleId + "/" + module.getModuleRegPk(),
            "منح الوحدة للدور: " + module.getNameAr(),
            "Module granted to role: " + module.getNameEn());
        log.info("Granted module grant ID: {}", saved.getRoleModuleGrantPk());

        return ServiceResult.success(moduleGrantMapper.toResponse(saved), Status.CREATED);
    }

    /** API-SEC-015 — RULE-SEC-003: the dependent screen and action grants go with the module grant. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_ROLES_UPDATE)")
    public ServiceResult<ModuleGrantRevokeResponse> revokeModule(Long roleId, Long moduleId) {
        log.info("Revoking module {} from Role ID: {}", moduleId, roleId);

        RoleModuleGrant grant = roleModuleGrantRepository.findByRoleAndModule(roleId, moduleId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_GRANT, moduleId));

        List<RoleScreenGrant> screenGrants = roleScreenGrantRepository.findCascadeTargets(roleId, moduleId);
        List<RoleActionGrant> actionGrants = roleActionGrantRepository.findCascadeTargets(roleId, moduleId);

        String principal = SecurityContextHelper.getCurrentUsername();
        roleActionGrantRepository.deleteAll(actionGrants);
        roleScreenGrantRepository.deleteAll(screenGrants);
        roleModuleGrantRepository.delete(grant);

        for (RoleActionGrant actionGrant : actionGrants) {
            ActionRegistry action = actionGrant.getAction();
            appendAudit(EVENT_ACTION_REVOKED, principal, roleId + "/" + action.getActionRegPk(),
                "سحب الإجراء ضمن سحب الوحدة: " + action.getNameAr(),
                "Action revoked by module cascade: " + action.getNameEn());
        }
        for (RoleScreenGrant screenGrant : screenGrants) {
            ScreenRegistry screen = screenGrant.getScreen();
            appendAudit(EVENT_SCREEN_REVOKED, principal, roleId + "/" + screen.getScreenRegPk(),
                "سحب الشاشة ضمن سحب الوحدة: " + screen.getNameAr(),
                "Screen revoked by module cascade: " + screen.getNameEn());
        }
        appendAudit(EVENT_MODULE_REVOKED, principal, roleId + "/" + moduleId,
            "سحب الوحدة من الدور: " + grant.getModule().getNameAr(),
            "Module revoked from role: " + grant.getModule().getNameEn());
        log.info("Revoked module grant for Role ID: {}, screens: {}, actions: {}",
            roleId, screenGrants.size(), actionGrants.size());

        return ServiceResult.success(
            moduleGrantMapper.toRevokeResponse(screenGrants.size(), actionGrants.size()),
            Status.SUCCESS);
    }

    /** API-SEC-016 — RULE-SEC-001 (QR-SEC-028) then the duplication guard, both in the Domain. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_ROLES_UPDATE)")
    public ServiceResult<RoleScreenGrantResponse> grantScreen(Long roleId, RoleScreenGrantRequest request) {
        log.info("Granting screen {} to Role ID: {}", request.getScreenId(), roleId);

        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_ROLE, roleId));
        ScreenRegistry screen = screenRegistryRepository.findById(request.getScreenId())
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_SCREEN, request.getScreenId()));

        Long moduleId = screen.getModule().getModuleRegPk();
        boolean roleHoldsModuleGrant = roleModuleGrantRepository
            .existsByRole_RolePkAndModule_ModuleRegPk(roleId, moduleId);
        boolean grantExists = roleScreenGrantRepository
            .existsByRole_RolePkAndScreen_ScreenRegPk(roleId, screen.getScreenRegPk());
        RoleScreenGrantDomain.create(roleId, screen.getScreenRegPk(), roleHoldsModuleGrant, grantExists);

        String principal = SecurityContextHelper.getCurrentUsername();
        RoleScreenGrant grant = screenGrantMapper.toEntity(role, screen);
        grant.setGrantedBy(principal);
        RoleScreenGrant saved = roleScreenGrantRepository.save(grant);

        appendAudit(EVENT_SCREEN_GRANTED, principal, roleId + "/" + screen.getScreenRegPk(),
            "منح الشاشة للدور: " + screen.getNameAr(),
            "Screen granted to role: " + screen.getNameEn());
        log.info("Granted screen grant ID: {}", saved.getRoleScreenGrantPk());

        return ServiceResult.success(screenGrantMapper.toResponse(saved), Status.CREATED);
    }

    /** API-SEC-017 — RULE-SEC-002 (QR-SEC-029), RULE-SEC-007 (QR-SEC-030), RULE-SEC-005 (QR-SEC-031). */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_ROLES_UPDATE)")
    public ServiceResult<RoleActionGrantResponse> grantAction(Long roleId, RoleActionGrantRequest request) {
        log.info("Granting action {} to Role ID: {}", request.getActionId(), roleId);

        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_ROLE, roleId));
        ActionRegistry action = actionRegistryRepository.findById(request.getActionId())
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_ACTION, request.getActionId()));

        Long screenId = action.getScreen().getScreenRegPk();
        boolean roleHoldsScreenGrant = roleScreenGrantRepository
            .existsByRole_RolePkAndScreen_ScreenRegPk(roleId, screenId);
        boolean roleHoldsViewGrant = roleActionGrantRepository.existsGatewayGrantForScreen(
            roleId, screenId, RoleActionGrantDomain.GATEWAY_ACTION_CODE);
        boolean conflictingActionHeld = anyUserOfRoleHoldsConflictingAction(roleId, action.getActionRegPk());
        boolean grantExists = roleActionGrantRepository
            .existsByRole_RolePkAndAction_ActionRegPk(roleId, action.getActionRegPk());

        RoleActionGrantDomain.create(roleId, action.getActionRegPk(), action.getActionCode(),
            roleHoldsScreenGrant, roleHoldsViewGrant, conflictingActionHeld, grantExists);

        String principal = SecurityContextHelper.getCurrentUsername();
        RoleActionGrant grant = actionGrantMapper.toEntity(role, action);
        grant.setGrantedBy(principal);
        RoleActionGrant saved = roleActionGrantRepository.save(grant);

        appendAudit(EVENT_ACTION_GRANTED, principal, roleId + "/" + action.getActionRegPk(),
            "منح الإجراء للدور: " + action.getNameAr(),
            "Action granted to role: " + action.getNameEn());
        log.info("Granted action grant ID: {}", saved.getRoleActionGrantPk());

        return ServiceResult.success(actionGrantMapper.toResponse(saved), Status.CREATED);
    }

    /** QR-SEC-031 (API-SEC-017 shape), asked once per conflicting counterpart of the granted action. */
    private boolean anyUserOfRoleHoldsConflictingAction(Long roleId, Long actionId) {
        for (Long counterpartActionId : conflictingCounterpartActions(actionId)) {
            if (roleActionGrantRepository.existsActionHeldByAnyUserOfRole(roleId, counterpartActionId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * RULE-SEC-005's counterpart set. SEC v1 declares no conflicting-pair source anywhere, and the
     * platform's only real pair is FIN-owned and FIN-enforced
     * (governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066), so this resolves empty
     * and the guard above stays live for the day such a source exists.
     */
    private Set<Long> conflictingCounterpartActions(Long actionId) {
        return Set.of();
    }

    private void appendAudit(String eventTypeCode, String principal, String targetRef,
                             String detailsAr, String detailsEn) {
        auditLogEntryRepository.save(AuditLogEntry.builder()
            .eventTypeCode(eventTypeCode)
            .actor(userRepository.findByUsername(principal).orElse(null))
            .occurredAt(Instant.now())
            .targetRef(targetRef)
            .detailsAr(detailsAr)
            .detailsEn(detailsEn)
            .build());
    }
}
