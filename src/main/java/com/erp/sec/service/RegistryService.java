package com.erp.sec.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.sec.domain.ActionRegistryDomain;
import com.erp.sec.domain.ModuleRegistryDomain;
import com.erp.sec.domain.ScreenRegistryDomain;
import com.erp.sec.dto.ActionRegistryCreateRequest;
import com.erp.sec.dto.ActionRegistryResponse;
import com.erp.sec.dto.ModuleRegistryCreateRequest;
import com.erp.sec.dto.ModuleRegistryResponse;
import com.erp.sec.dto.RegistryRowResponse;
import com.erp.sec.dto.RegistrySearchRequest;
import com.erp.sec.dto.ScreenRegistryCreateRequest;
import com.erp.sec.dto.ScreenRegistryResponse;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.entity.ScreenRegistry;
import com.erp.sec.mapper.ActionRegistryMapper;
import com.erp.sec.mapper.ModuleRegistryMapper;
import com.erp.sec.mapper.ScreenRegistryMapper;
import com.erp.sec.repository.ActionRegistryRepository;
import com.erp.sec.repository.ModuleRegistryRepository;
import com.erp.sec.repository.ScreenRegistryRepository;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and tree search for ENT-SEC-004/005/006 — API-SEC-018/019/020/021, one screen
 * (SEC_MODULE_REGISTRY) and one permission across all three. SRS A6 defines no AUDIT_EVENT_TYPE for
 * a registration, so none of the three appends an audit row.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistryService {

    /** The permission-code shape of DBF-SEC-050 / AC-SEC-019: {@code PERM_<pageCode>_<actionCode>}. */
    private static final String PERMISSION_CODE_FORMAT = "PERM_%s_%s";

    /**
     * SCR-REQ-SEC-006 B2 filters are module code and screen page code; only the former is a column
     * of the paged root (the module), so it is the only sortable field (A.5.6).
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("code");

    private final ModuleRegistryRepository moduleRepository;
    private final ScreenRegistryRepository screenRepository;
    private final ActionRegistryRepository actionRepository;
    private final ModuleRegistryMapper moduleMapper;
    private final ScreenRegistryMapper screenMapper;
    private final ActionRegistryMapper actionMapper;

    /** API-SEC-018 — code uniqueness (QR-SEC-035) is decided by {@code ModuleRegistryDomain}. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_MODULE_REGISTRY_UPDATE)")
    public ServiceResult<ModuleRegistryResponse> registerModule(ModuleRegistryCreateRequest request) {
        log.info("Registering module with code: {}", request.getCode());

        String code = normalize(request.getCode());
        ModuleRegistryDomain.create(code, moduleRepository.existsByCode(code));

        ModuleRegistry saved = moduleRepository.save(moduleMapper.toEntity(request));
        log.info("Registered module ID: {}", saved.getModuleRegPk());

        return ServiceResult.success(moduleMapper.toResponse(saved), Status.CREATED);
    }

    /** API-SEC-019 — RULE-SEC-004 then page-code uniqueness, both decided by the Domain object. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_MODULE_REGISTRY_UPDATE)")
    public ServiceResult<ScreenRegistryResponse> registerScreen(ScreenRegistryCreateRequest request) {
        log.info("Registering screen with page code: {}", request.getPageCode());

        String pageCode = normalize(request.getPageCode());
        ModuleRegistry module = moduleRepository.findByCode(normalize(request.getModuleCode()))
            .orElse(null);

        ScreenRegistryDomain.create(pageCode, module != null,
            screenRepository.existsByPageCode(pageCode));

        ScreenRegistry saved = screenRepository.save(screenMapper.toEntity(request, module));
        log.info("Registered screen ID: {}", saved.getScreenRegPk());

        return ServiceResult.success(screenMapper.toResponse(saved), Status.CREATED);
    }

    /**
     * API-SEC-020, in the Orchestration line's order: resolve the screen by page code, derive the
     * permission code, then let {@code ActionRegistryDomain} decide on both facts (QR-SEC-037).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_MODULE_REGISTRY_UPDATE)")
    public ServiceResult<ActionRegistryResponse> registerAction(ActionRegistryCreateRequest request) {
        log.info("Registering action {} on page {}", request.getActionCode(), request.getPageCode());

        String pageCode = normalize(request.getPageCode());
        ScreenRegistry screen = screenRepository.findByPageCode(pageCode).orElse(null);
        String permissionCode = PERMISSION_CODE_FORMAT
            .formatted(pageCode, normalize(request.getActionCode()));

        ActionRegistryDomain.create(permissionCode, screen != null,
            actionRepository.existsByPermissionCode(permissionCode));

        ActionRegistry saved = actionRepository.save(
            actionMapper.toEntity(request, screen, permissionCode));
        log.info("Registered action ID: {}", saved.getActionRegPk());

        return ServiceResult.success(actionMapper.toResponse(saved), Status.CREATED);
    }

    /**
     * API-SEC-021 — the module is the paged unit; its active screens and their active actions are
     * fetched per page and grouped, so neither nesting level multiplies the page.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_MODULE_REGISTRY_VIEW)")
    public ServiceResult<Page<RegistryRowResponse>> search(RegistrySearchRequest searchRequest) {
        log.debug("Searching registry");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SecSearchSupport.assertSortAllowed(commonRequest.getSortField(), ALLOWED_SORT_FIELDS);

        Specification<ModuleRegistry> spec = SpecBuilder.build(commonRequest,
            new SetAllowedFields(ALLOWED_SORT_FIELDS), DefaultFieldValueConverter.INSTANCE);
        String pageCode = searchRequest.getPageCode();
        if (pageCode != null && !pageCode.isBlank()) {
            spec = spec.and(hasScreenWithPageCode(pageCode));
        }

        Page<ModuleRegistry> modules =
            moduleRepository.findAll(spec, PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS));
        Map<Long, List<ScreenRegistryResponse>> screensByModule = loadScreenTree(
            modules.getContent().stream().map(ModuleRegistry::getModuleRegPk).toList());

        return ServiceResult.success(modules.map(module -> moduleMapper.toRegistryRowResponse(
            module, screensByModule.getOrDefault(module.getModuleRegPk(), List.of()))));
    }

    /** {@code pageCode} filters on a child row, so it is an explicit join predicate (A.5.17). */
    private Specification<ModuleRegistry> hasScreenWithPageCode(String pageCode) {
        String pattern = "%" + pageCode.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ScreenRegistry> screen = subquery.from(ScreenRegistry.class);
            subquery.select(screen.get("screenRegPk")).where(
                cb.equal(screen.get("module"), root),
                cb.isTrue(screen.get("isActiveFl")),
                cb.like(cb.lower(screen.get("pageCode")), pattern));
            return cb.exists(subquery);
        };
    }

    /** Two flat reads (QR-SEC-021), grouped in memory — never a per-row query. */
    private Map<Long, List<ScreenRegistryResponse>> loadScreenTree(List<Long> moduleRegPks) {
        if (moduleRegPks.isEmpty()) {
            return Map.of();
        }
        List<ScreenRegistry> screens = screenRepository.findActiveByModules(moduleRegPks);
        List<Long> screenRegPks = screens.stream().map(ScreenRegistry::getScreenRegPk).toList();
        Map<Long, List<ActionRegistryResponse>> actionsByScreen = screenRegPks.isEmpty()
            ? Map.of()
            : actionRepository.findActiveByScreens(screenRegPks).stream()
                .collect(Collectors.groupingBy(action -> action.getScreen().getScreenRegPk(),
                    Collectors.mapping(actionMapper::toResponse, Collectors.toList())));
        return screens.stream().collect(Collectors.groupingBy(
            screen -> screen.getModule().getModuleRegPk(),
            Collectors.mapping(screen -> screenMapper.toResponse(screen,
                actionsByScreen.getOrDefault(screen.getScreenRegPk(), List.of())),
                Collectors.toList())));
    }

    /** Mirrors each entity's own {@code @PrePersist} case normalization so lookups cannot miss. */
    private String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}
