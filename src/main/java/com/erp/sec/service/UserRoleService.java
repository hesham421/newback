package com.erp.sec.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.SecurityContextHelper;
import com.erp.sec.domain.UserRoleAssignmentDomain;
import com.erp.sec.dto.RoleSummaryResponse;
import com.erp.sec.dto.UserResponse;
import com.erp.sec.dto.UserRoleAssignmentRequest;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.User;
import com.erp.sec.entity.UserRoleAssignment;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.mapper.RoleMapper;
import com.erp.sec.mapper.UserMapper;
import com.erp.sec.mapper.UserRoleAssignmentMapper;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.RoleActionGrantRepository;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.repository.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration for ENT-SEC-003 (UserRoleAssignment) — API-SEC-008. The submitted role set
 * replaces the stored one; RULE-SEC-005 is decided by {@code UserRoleAssignmentDomain}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleService {

    /** AUDIT_EVENT_TYPE codes (CHK_SEC_AUDIT_LOG_EVENT_TYPE) this API appends. */
    private static final String EVENT_ROLE_ASSIGNED = "ROLE_ASSIGNED";
    private static final String EVENT_ROLE_REVOKED = "ROLE_REVOKED";

    private final UserRoleAssignmentRepository repository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleActionGrantRepository roleActionGrantRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final UserRoleAssignmentMapper mapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    /** API-SEC-008 — check RULE-SEC-005, replace the assignment set, audit each add and removal. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_USERS_UPDATE)")
    public ServiceResult<UserResponse> assign(Long userId, UserRoleAssignmentRequest request) {
        log.info("Assigning roles to User ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_USER, userId));

        List<RoleSummaryResponse> roles = replaceAssignments(user, request.getRoleIds());

        return ServiceResult.success(userMapper.toResponse(user, roles), Status.UPDATED);
    }

    /**
     * The assignment itself, without a gate of its own: the submitted set REPLACES the stored one,
     * RULE-SEC-005 is checked per requested role, and every add and removal is audited. Shared by
     * API-SEC-008 (gated by {@link #assign} above) and API-SEC-006's create-with-roles (gated by
     * {@code UserService.create}, which additionally demands PERM_SEC_USERS_UPDATE before it calls
     * here) — package-private precisely so no path outside {@code com.erp.sec.service} can reach an
     * ungated assignment, and so it always runs inside its caller's transaction.
     *
     * @return the resulting role set, in submitted order, ready for {@code UserResponse.roles}
     */
    List<RoleSummaryResponse> replaceAssignments(User user, List<Long> roleIds) {
        Long userId = user.getUserPk();
        Map<Long, Role> requestedRoles = loadRequestedRoles(roleIds);

        for (Role role : requestedRoles.values()) {
            UserRoleAssignmentDomain.create(userId, role.getRolePk(),
                holdsConflictingAction(userId, role.getRolePk()));
        }

        String principal = SecurityContextHelper.getCurrentUsername();
        List<UserRoleAssignment> existing = repository.findByUser(userId);

        List<UserRoleAssignment> removed = new ArrayList<>();
        Set<Long> retainedRoleIds = new LinkedHashSet<>();
        for (UserRoleAssignment assignment : existing) {
            Long roleId = assignment.getRole().getRolePk();
            if (requestedRoles.containsKey(roleId)) {
                retainedRoleIds.add(roleId);
            } else {
                removed.add(assignment);
            }
        }

        List<UserRoleAssignment> added = new ArrayList<>();
        for (Role role : requestedRoles.values()) {
            if (!retainedRoleIds.contains(role.getRolePk())) {
                UserRoleAssignment assignment = mapper.toEntity(user, role);
                assignment.setAssignedBy(principal);
                added.add(assignment);
            }
        }

        repository.deleteAll(removed);
        repository.saveAll(added);

        for (UserRoleAssignment assignment : removed) {
            appendRoleAudit(EVENT_ROLE_REVOKED, principal, userId, assignment.getRole(),
                "سحب الدور من المستخدم", "Role revoked from user");
        }
        for (UserRoleAssignment assignment : added) {
            appendRoleAudit(EVENT_ROLE_ASSIGNED, principal, userId, assignment.getRole(),
                "إسناد الدور إلى المستخدم", "Role assigned to user");
        }
        log.info("Assigned roles to User ID: {}, added: {}, removed: {}",
            userId, added.size(), removed.size());

        return requestedRoles.values().stream()
            .map(roleMapper::toSummaryResponse)
            .toList();
    }

    /**
     * The roles one user holds, for the {@code roles} member of a single-user response
     * (API-SEC-006 without roleIds, API-SEC-007). Package-private, same reasoning as above.
     */
    List<RoleSummaryResponse> rolesOf(Long userPk) {
        return repository.findByUser(userPk).stream()
            .map(assignment -> roleMapper.toSummaryResponse(assignment.getRole()))
            .toList();
    }

    /**
     * The same, for a whole page of users (API-SEC-005) — ONE query for the page, never one per
     * row. A user with no assignments is simply absent from the map; the caller substitutes an
     * empty list, so "holds no roles" stays distinguishable from a missing key on the wire.
     */
    Map<Long, List<RoleSummaryResponse>> rolesByUser(Collection<Long> userPks) {
        if (userPks == null || userPks.isEmpty()) {
            return Map.of();                       // IN () is a syntax error — never issue it
        }
        Map<Long, List<RoleSummaryResponse>> byUser = new LinkedHashMap<>();
        for (UserRoleAssignment assignment : repository.findByUserIn(userPks)) {
            byUser.computeIfAbsent(assignment.getUser().getUserPk(), key -> new ArrayList<>())
                .add(roleMapper.toSummaryResponse(assignment.getRole()));
        }
        return byUser;
    }

    private Map<Long, Role> loadRequestedRoles(List<Long> roleIds) {
        Map<Long, Role> roles = new LinkedHashMap<>();
        if (roleIds == null) {
            return roles;
        }
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new LocalizedException(
                    Status.NOT_FOUND, SecErrorCodes.SEC_404_ROLE, roleId));
            roles.put(role.getRolePk(), role);
        }
        return roles;
    }

    /** QR-SEC-031 (API-SEC-008 shape), asked once per conflicting counterpart of the new role. */
    private boolean holdsConflictingAction(Long userId, Long roleId) {
        for (Long counterpartActionId : conflictingCounterpartActions(roleId)) {
            if (roleActionGrantRepository.existsActionHeldByUser(userId, counterpartActionId)) {
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
    private Set<Long> conflictingCounterpartActions(Long roleId) {
        return Set.of();
    }

    private void appendRoleAudit(String eventTypeCode, String principal, Long userId, Role role,
                                 String detailsAr, String detailsEn) {
        auditLogEntryRepository.save(AuditLogEntry.builder()
            .eventTypeCode(eventTypeCode)
            .actor(userRepository.findByUsername(principal).orElse(null))
            .occurredAt(Instant.now())
            .targetRef(userId + "/" + role.getRolePk())
            .detailsAr(detailsAr + ": " + role.getNameAr())
            .detailsEn(detailsEn + ": " + role.getNameEn())
            .build());
    }
}
