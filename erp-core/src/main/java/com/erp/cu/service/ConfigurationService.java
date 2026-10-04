package com.erp.cu.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.cu.domain.AppConfigurationDomain;
import com.erp.cu.dto.ConfigurationCreateRequest;
import com.erp.cu.dto.ConfigurationResponse;
import com.erp.cu.dto.ConfigurationSearchRequest;
import com.erp.cu.dto.ConfigurationUpdateRequest;
import com.erp.cu.entity.AppConfiguration;
import com.erp.cu.exception.CuErrorCodes;
import com.erp.cu.mapper.ConfigurationMapper;
import com.erp.cu.repository.AppConfigurationRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for ENTITY-CU-001 (AppConfiguration), named Configuration per SVC-API.md's
 * deliberate DTO/service/controller family naming. Every non-search endpoint addresses the
 * resource by configKey (business key), not the surrogate id (DRV-003). No caching annotations
 * anywhere in this class — the project's cache-eligibility register is empty project-wide, so
 * AppConfiguration is not cache-eligible (gov-enforce-caching-rules D.1.1/D.5.5).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService {

    private final AppConfigurationRepository repository;
    private final ConfigurationMapper mapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "configKey", "createdAt", "updatedAt"
    );

    // Filterable fields for SpecBuilder. Distinct from ALLOWED_SORT_FIELDS: isActive is a valid
    // EXACT filter (API-CU-002) but not a sort key, and reusing the sort set as the filter allow-list
    // silently dropped the isActive predicate, returning every row regardless of the filter.
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
        "configKey", "isActive", "createdAt", "updatedAt"
    );

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".CONFIG_CREATE)")
    public ServiceResult<ConfigurationResponse> create(ConfigurationCreateRequest request) {
        log.info("Creating Configuration with key: {}", request.getConfigKey());

        // 1. Fetch what RULE-CU-001 needs — pre-check against the normalized (uppercase) key,
        // since AppConfiguration.onCreate() always uppercases configKey before insert; without
        // normalizing here a differently-cased duplicate would slip past this check and fail
        // later as a raw DataIntegrityViolationException instead of ERR-0001.
        boolean keyTaken = repository.existsByConfigKey(normalize(request.getConfigKey()));

        // 2. Delegate the decision (RULE-CU-002 required fields, RULE-CU-001 uniqueness)
        AppConfigurationDomain.create(request.getConfigKey(), request.getConfigValue(), keyTaken);

        // 3. Map, then persist (SEQ_CU_APP_CONFIGURATION; audit via AuditEntityListener)
        AppConfiguration entity = mapper.toEntity(request);
        AppConfiguration saved = repository.save(entity);
        log.info("Created Configuration ID: {}, key: {}", saved.getId(), saved.getConfigKey());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".CONFIG_VIEW)")
    public ServiceResult<Page<ConfigurationResponse>> search(ConfigurationSearchRequest searchRequest) {
        log.debug("Searching Configuration");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();

        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_FILTER_FIELDS);
        Specification<AppConfiguration> spec =
            SpecBuilder.build(commonRequest, allowedFields, DefaultFieldValueConverter.INSTANCE);
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<AppConfiguration> page = repository.findAll(spec, pageable);

        return ServiceResult.success(page.map(mapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".CONFIG_UPDATE)")
    public ServiceResult<ConfigurationResponse> update(String configKey, ConfigurationUpdateRequest request) {
        log.info("Updating Configuration key: {}", configKey);

        // 1. Load by key (QR-CU-0001) — not-found throw
        AppConfiguration entity = repository.findByConfigKey(normalize(configKey))
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, CuErrorCodes.APP_CONFIGURATION_NOT_FOUND, configKey));

        // 2. RULE-CU-003 (configKey immutability) needs no runtime guard here — configKey is
        // structurally absent from ConfigurationUpdateRequest, so there is no code path that
        // could attempt to change it.
        // 3. Delegate RULE-CU-002 (configValue required on update)
        AppConfigurationDomain.from(entity).assertCanUpdate(request.getConfigValue());

        // 4. Mutate + persist. saveAndFlush (not save) so Hibernate runs the UPDATE — and with it
        // AuditEntityListener's @PreUpdate — before the response is mapped; a plain save() defers
        // the flush to commit, which happens after this method returns, so the mapped response
        // would carry the pre-update updatedAt/updatedBy while the persisted row carries the new
        // ones.
        mapper.updateEntityFromRequest(entity, request);
        AppConfiguration saved = repository.saveAndFlush(entity);
        log.info("Updated Configuration key: {}", saved.getConfigKey());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".CONFIG_VIEW)")
    public ServiceResult<ConfigurationResponse> getByKey(String configKey) {
        log.debug("Fetching Configuration key: {}", configKey);

        AppConfiguration entity = repository.findByConfigKey(normalize(configKey))
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, CuErrorCodes.APP_CONFIGURATION_NOT_FOUND, configKey));

        return ServiceResult.success(mapper.toResponse(entity));
    }

    /**
     * API-CU-004 — DELETE verb mapped onto soft deactivate (SRS Operations = C, R, U, Deactivate
     * only; no hard delete anywhere in this entity's lifecycle). Mirrors build-create-service's
     * deactivate() steps (find → not-found-throw → entity.deactivate() → save) but returns void,
     * matching the controller's delete()-shaped endpoint (204, no wrapped response) — there is no
     * repository.delete(entity) call and no reference/child-count check, since nothing in the
     * schema can ever reference this entity (ROOT module, single table, no children).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".CONFIG_DEACTIVATE)")
    public void deactivate(String configKey) {
        log.info("Deactivating Configuration key: {}", configKey);

        AppConfiguration entity = repository.findByConfigKey(normalize(configKey))
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, CuErrorCodes.APP_CONFIGURATION_NOT_FOUND, configKey));

        entity.deactivate();
        repository.save(entity);
        log.info("Deactivated Configuration key: {}", configKey);
    }

    /**
     * In-process read used by other modules (SEC/FILE/NOTIF consume CU as a plain library —
     * SRS A7 / db-script.md §3 / master-registry §8: "a library relationship, not a
     * cross-module/XM relationship"). Deliberately NOT behind a crossmodule package/interface —
     * that pattern is reserved for genuine XM boundaries between business modules, which this is
     * not. Returns a raw String, not ServiceResult<String>: this method is never touched by a
     * controller/OperationCode, so there is nothing to unwrap for — wrapping it would only force
     * every future internal caller to unwrap .getData() for no reason. Narrow, deliberate
     * exception to A.5.8 for this one internal-only method.
     *
     * <p>No {@code @PreAuthorize}: this is an in-process library read invoked by other modules from
     * system/startup/async paths that carry no SecurityContext. A method-security check here would
     * throw AccessDeniedException on exactly those internal calls. Access to the public,
     * controller-facing read (getByKey) remains CONFIG_VIEW-gated; this internal accessor is never
     * bound to a controller/OperationCode, so it needs no authority of its own.
     */
    @Transactional(readOnly = true)
    public String getValue(String configKey) {
        log.debug("Fetching Configuration value for key: {}", configKey);

        AppConfiguration entity = repository.findByConfigKey(normalize(configKey))
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, CuErrorCodes.APP_CONFIGURATION_NOT_FOUND, configKey));

        return entity.getConfigValue();
    }

    /**
     * Normalizes a caller-supplied configKey to the same canonical uppercase form
     * AppConfiguration.onCreate()/onUpdate() always applies before persisting, so every lookup
     * (existence check, find-by-key) matches the stored value regardless of the case the caller
     * used in the request body or path variable.
     */
    private static String normalize(String configKey) {
        return configKey == null ? null : configKey.trim().toUpperCase();
    }
}
