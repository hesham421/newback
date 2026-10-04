package com.erp.cu.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.cu.crossmodule.SettingsApi;
import com.erp.cu.domain.AppConfigurationDomain;
import com.erp.cu.domain.SettingScope;
import com.erp.cu.dto.ConfigurationCreateRequest;
import com.erp.cu.dto.ConfigurationResponse;
import com.erp.cu.dto.ConfigurationSearchRequest;
import com.erp.cu.dto.ConfigurationUpdateRequest;
import com.erp.cu.entity.AppConfiguration;
import com.erp.cu.exception.CuErrorCodes;
import com.erp.cu.mapper.ConfigurationMapper;
import com.erp.cu.repository.AppConfigurationRepository;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for ENTITY-CU-001 (AppConfiguration), named Configuration per SVC-API.md's
 * deliberate DTO/service/controller family naming. Every non-search endpoint addresses the
 * resource by configKey (business key), not the surrogate id (DRV-003).
 *
 * <p><b>Scope (erp-core step 09).</b> Every CRUD operation takes a {@link SettingScope}: {@code TENANT}
 * (the default) addresses the caller's own overrides, {@code PLATFORM} the platform defaults
 * ({@code TENANT_ID IS NULL}), which require {@code PLATFORM_SETTINGS_MANAGE} and the PLATFORM tenant.
 * The entity is not tenant-filtered by Hibernate, so each repository call names its owner.
 *
 * <p><b>Cache (erp-core step 09).</b> AppConfiguration is the one entity on the caching register (the
 * step-09 plan's decision): {@link #resolve} is cached in {@value SettingsApi#CACHE_NAME} keyed
 * {@code <tenantId>:<KEY>}, and every write evicts the whole cache ({@code allEntries}) — a platform
 * default change affects every tenant's resolution, and writes are rare administrator actions.
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

    @CacheEvict(cacheNames = SettingsApi.CACHE_NAME, allEntries = true)
    @Transactional
    @PreAuthorize("(#scope == T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).PLATFORM_SETTINGS_MANAGE))"
        + " or (#scope != T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).CONFIG_CREATE))")
    public ServiceResult<ConfigurationResponse> create(SettingScope scope, ConfigurationCreateRequest request) {
        log.info("Creating Configuration with key: {} (scope {})", request.getConfigKey(), scope);
        Long owner = owner(scope);

        // 1. Fetch what RULE-CU-001 needs — pre-check against the normalized (uppercase) key within the
        // owner (platform defaults or the caller's tenant), since AppConfiguration.onCreate() always
        // uppercases configKey before insert.
        String key = normalize(request.getConfigKey());
        boolean keyTaken = owner == null
            ? repository.existsByTenantIdIsNullAndConfigKey(key)
            : repository.existsByTenantIdAndConfigKey(owner, key);

        // 2. Delegate the decision (RULE-CU-002 required fields, RULE-CU-001 uniqueness)
        AppConfigurationDomain.create(request.getConfigKey(), request.getConfigValue(), keyTaken);

        // 3. Map (with the owner), then persist (SEQ_CU_APP_CONFIGURATION; audit via AuditEntityListener)
        AppConfiguration saved = repository.save(mapper.toEntity(request, owner));
        log.info("Created Configuration ID: {}, key: {}", saved.getId(), saved.getConfigKey());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("(#scope == T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).PLATFORM_SETTINGS_MANAGE))"
        + " or (#scope != T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).CONFIG_VIEW))")
    public ServiceResult<Page<ConfigurationResponse>> search(SettingScope scope, ConfigurationSearchRequest searchRequest) {
        log.debug("Searching Configuration (scope {})", scope);
        Long owner = owner(scope);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();

        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_FILTER_FIELDS);
        Specification<AppConfiguration> spec =
            SpecBuilder.<AppConfiguration>build(commonRequest, allowedFields, DefaultFieldValueConverter.INSTANCE)
                .and(AppConfigurationRepository.ownedBy(owner));
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<AppConfiguration> page = repository.findAll(spec, pageable);

        return ServiceResult.success(page.map(mapper::toResponse));
    }

    @CacheEvict(cacheNames = SettingsApi.CACHE_NAME, allEntries = true)
    @Transactional
    @PreAuthorize("(#scope == T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).PLATFORM_SETTINGS_MANAGE))"
        + " or (#scope != T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).CONFIG_UPDATE))")
    public ServiceResult<ConfigurationResponse> update(SettingScope scope, String configKey,
                                                       ConfigurationUpdateRequest request) {
        log.info("Updating Configuration key: {} (scope {})", configKey, scope);

        // 1. Load by key within the owner (QR-CU-0001) — not-found throw
        AppConfiguration entity = findInScope(owner(scope), configKey);

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
    @PreAuthorize("(#scope == T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).PLATFORM_SETTINGS_MANAGE))"
        + " or (#scope != T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).CONFIG_VIEW))")
    public ServiceResult<ConfigurationResponse> getByKey(SettingScope scope, String configKey) {
        log.debug("Fetching Configuration key: {} (scope {})", configKey, scope);
        return ServiceResult.success(mapper.toResponse(findInScope(owner(scope), configKey)));
    }

    /**
     * API-CU-004 — DELETE verb mapped onto soft deactivate (SRS Operations = C, R, U, Deactivate
     * only; no hard delete anywhere in this entity's lifecycle). Mirrors build-create-service's
     * deactivate() steps (find → not-found-throw → entity.deactivate() → save) but returns void,
     * matching the controller's delete()-shaped endpoint (204, no wrapped response) — there is no
     * repository.delete(entity) call and no reference/child-count check, since nothing in the
     * schema can ever reference this entity (ROOT module, single table, no children).
     */
    @CacheEvict(cacheNames = SettingsApi.CACHE_NAME, allEntries = true)
    @Transactional
    @PreAuthorize("(#scope == T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).PLATFORM_SETTINGS_MANAGE))"
        + " or (#scope != T(com.erp.cu.domain.SettingScope).PLATFORM"
        + " and hasAuthority(T(com.erp.cu.permission.CuPermissions).CONFIG_DEACTIVATE))")
    public void deactivate(SettingScope scope, String configKey) {
        log.info("Deactivating Configuration key: {} (scope {})", configKey, scope);

        AppConfiguration entity = findInScope(owner(scope), configKey);

        entity.deactivate();
        repository.save(entity);
        log.info("Deactivated Configuration key: {}", configKey);
    }

    /**
     * erp-core step 09 — the resolved raw value of {@code configKey} for {@code tenantId} (its active
     * override, else the active platform default), or {@code null} when neither exists. Backs
     * {@code SettingsApi}; cached per {@code <tenantId>:<KEY>} ({@code null} is cached too, as "absent"),
     * and evicted by every write above.
     *
     * <p>Returns the raw value, not a {@code ServiceResult}: never bound to a controller, and the cached
     * value must stay a plain serializable {@code String} (Redis cache type). No {@code @PreAuthorize}: an
     * in-process library read used by other modules from system/startup/async paths that carry no
     * SecurityContext — the same reasoning that applied to the {@code getValue} accessor this replaces.
     * The tenant is a parameter (the caller passes {@code TenantContext.require()}), so the cache key can
     * never mix tenants.
     */
    @Cacheable(cacheNames = SettingsApi.CACHE_NAME, key = "#tenantId + ':' + #configKey")
    @Transactional(readOnly = true)
    public String resolve(Long tenantId, String configKey) {
        log.debug("Resolving setting {} for tenant {}", configKey, tenantId);
        Optional<String> value = AppConfigurationDomain.resolve(
            repository.findOverrideAndDefault(tenantId, configKey), tenantId);
        return value.orElse(null);
    }

    /**
     * The owner a scope addresses — {@code null} (platform defaults) or the caller's tenant — after the
     * tenant half of the scope rule (PLATFORM scope only from the PLATFORM tenant).
     */
    private static Long owner(SettingScope scope) {
        Long current = TenantContext.require();
        SettingScope effective = scope == null ? SettingScope.TENANT : scope;
        AppConfigurationDomain.assertScopeAllowed(effective,
            Long.valueOf(TenantConstants.PLATFORM_TENANT_ID).equals(current));
        return effective == SettingScope.PLATFORM ? null : current;
    }

    private AppConfiguration findInScope(Long owner, String configKey) {
        String key = normalize(configKey);
        Optional<AppConfiguration> found = owner == null
            ? repository.findByTenantIdIsNullAndConfigKey(key)
            : repository.findByTenantIdAndConfigKey(owner, key);
        return found.orElseThrow(() -> new LocalizedException(
            Status.NOT_FOUND, CuErrorCodes.APP_CONFIGURATION_NOT_FOUND, configKey));
    }

    /**
     * Normalizes a caller-supplied configKey to the same canonical uppercase form
     * AppConfiguration.onCreate()/onUpdate() always applies before persisting, so every lookup
     * (existence check, find-by-key, settings resolution) matches the stored value regardless of the
     * case the caller used.
     */
    public static String normalize(String configKey) {
        return configKey == null ? null : configKey.trim().toUpperCase();
    }
}
