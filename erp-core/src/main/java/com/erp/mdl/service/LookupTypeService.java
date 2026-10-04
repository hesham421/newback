package com.erp.mdl.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.BooleanFieldValueConverter;
import com.erp.common.search.FieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.mdl.domain.LookupTypeDomain;
import com.erp.mdl.dto.LookupTypeByOwnerSearchRequest;
import com.erp.mdl.dto.LookupTypeCreateRequest;
import com.erp.mdl.dto.LookupTypeResponse;
import com.erp.mdl.dto.LookupTypeSearchRequest;
import com.erp.mdl.dto.LookupTypeUpdateRequest;
import com.erp.mdl.dto.OwnerGroupResponse;
import com.erp.mdl.entity.LookupType;
import com.erp.mdl.exception.MdlErrorCodes;
import com.erp.mdl.mapper.LookupTypeMapper;
import com.erp.mdl.repository.LookupTypeRepository;
import com.erp.sec.crossmodule.SecModuleRegistryApi;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for ENT-MDL-001 (LookupType) — API-MDL-002/003/004 (SVC-API-CRUD.md) plus
 * API-MDL-001/010 (SVC-API-SEARCH.md, read-only). {@code ALLOWED_SORT_FIELDS} is declared now
 * that {@link #search} exists (A.5.6).
 *
 * <p>{@link SecModuleRegistryApi} is the ONLY cross-module interface reference in this module,
 * held here and nowhere else (never in {@code LookupTypeDomain}, the mapper, or the controller) —
 * build-create-service's "Cross-Module Calls" rule.
 *
 * <p>No caching annotations anywhere in this class: MDL's entities are absent from the project's
 * cache-eligibility register (currently empty), so neither {@code @Cacheable} nor
 * {@code @CacheEvict} applies (gov-enforce-caching-rules D.1.1/D.5.5) — and {@link #search} /
 * {@link #browseByOwner} are paginated/listing reads regardless, which the caching skill forbids
 * caching on independently of the register.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LookupTypeService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "lookupTypePk", "key", "ownerModuleCode", "nameAr", "nameEn", "isActiveFl", "createdAt"
    );

    /** {@code isActiveFl} arrives from a JSON body, which may carry it as a string. */
    private static final FieldValueConverter FILTER_VALUE_CONVERTER =
        new BooleanFieldValueConverter(Set.of("isActiveFl"));

    private final LookupTypeRepository repository;
    private final LookupTypeMapper mapper;
    private final SecModuleRegistryApi secModuleRegistryApi;

    /**
     * API-MDL-002 — RULE-MDL-001 (QR-MDL-012, XM-MDL-001) then key uniqueness (QR-MDL-013), both
     * resolved here and delegated to {@link LookupTypeDomain#create} for the actual decision.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_CREATE)")
    public ServiceResult<LookupTypeResponse> create(LookupTypeCreateRequest request) {
        log.info("Creating LookupType with key: {}", request.getKey());

        // 1. XM-MDL-001 / QR-MDL-012 — cross-module SOFT-READ, resolved by the service
        boolean ownerModuleRegistered = secModuleRegistryApi.isModuleActive(request.getOwnerModuleCode());

        // 2. QR-MDL-013 — key-uniqueness pre-check
        boolean keyAlreadyTaken = repository.existsByKey(request.getKey());

        // 3. Delegate the decision — throws LocalizedException on violation
        LookupTypeDomain.create(request.getKey(), request.getOwnerModuleCode(),
            ownerModuleRegistered, keyAlreadyTaken);

        // 4. Map, then persist (QR-MDL-002)
        LookupType saved = repository.save(mapper.toEntity(request));
        log.info("Created LookupType ID: {}", saved.getLookupTypePk());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    /**
     * API-MDL-003 — name-only update. RULE-MDL-003 (key immutability) needs no runtime guard
     * here: {@code key} is structurally absent from {@link LookupTypeUpdateRequest}.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_UPDATE)")
    public ServiceResult<LookupTypeResponse> update(Long id, LookupTypeUpdateRequest request) {
        log.info("Updating LookupType ID: {}", id);

        LookupType entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MdlErrorCodes.MDL_404_TYPE, id));

        mapper.updateEntityFromRequest(entity, request);
        LookupType saved = repository.save(entity);
        log.info("Updated LookupType ID: {}", saved.getLookupTypePk());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * API-MDL-004 — soft-deactivate only, no active-children guard (DATA-DOM's own report and this
     * API's orchestration line both confirm RULE-MDL-004 is a read-time filter owned by the future
     * consumer read, API-MDL-011 — not a write-time guard here).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_UPDATE)")
    public ServiceResult<LookupTypeResponse> deactivate(Long id) {
        log.info("Deactivating LookupType ID: {}", id);

        LookupType entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MdlErrorCodes.MDL_404_TYPE, id));

        entity.deactivate();
        LookupType saved = repository.save(entity);
        log.info("Deactivated LookupType ID: {}", saved.getLookupTypePk());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * API-MDL-001 — search lookup types (SVC-API-SEARCH.md). {@code key}, {@code
     * ownerModuleCode} and {@code isActiveFl} all flow through the generic {@code filters[]} list
     * on {@link LookupTypeSearchRequest} — the client now names the field and operator itself
     * (e.g. {@code key LIKE}) — reaching {@link SpecBuilder} untouched, the same shape as any
     * other module's {@code POST /search}. The earlier GET + scalar-argument deviation
     * (SVC-API-SEARCH.md point 7) was reversed alongside SEC's own GET-to-POST reversal
     * (governance/project-artifacts/sec-implementation-notes.md §8).
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_VIEW)")
    public ServiceResult<Page<LookupTypeResponse>> search(LookupTypeSearchRequest searchRequest) {
        log.debug("Searching LookupType");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();

        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<LookupType> spec =
            SpecBuilder.build(commonRequest, allowedFields, FILTER_VALUE_CONVERTER);
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<LookupType> resultPage = repository.findAll(spec, pageable);

        return ServiceResult.success(resultPage.map(mapper::toResponse));
    }

    /**
     * API-MDL-010 — browse the registry grouped by owner (SVC-API-SEARCH.md, QR-MDL-010).
     * Active types only — that predicate is ANDed in here unconditionally, never a
     * client-supplied filter (same pattern as SEC's {@code ActiveSessionService}'s unconditional
     * {@code terminatedAt IS NULL}) — optionally further filtered by the client's own
     * {@code ownerModuleCode}/{@code key} generic filters, then grouped by
     * {@code ownerModuleCode} in the service layer (a plain {@code Collectors.groupingBy} over a
     * single-table load — no SQL {@code GROUP BY}, per DATA-DOM.md's own "grouped in the service
     * layer" wording). Not paginated — the spec's Response line is a flat
     * {@code List<OwnerGroupResponse>}, not a {@code Page<T>}, so {@code page}/{@code size}/
     * {@code sortField} on {@link LookupTypeByOwnerSearchRequest} are inherited but unused.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_TYPE_REGISTRY_VIEW)")
    public ServiceResult<List<OwnerGroupResponse>> browseByOwner(LookupTypeByOwnerSearchRequest searchRequest) {
        log.debug("Browsing LookupType registry by owner");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(Set.of("ownerModuleCode", "key"));

        Specification<LookupType> activeOnly = (root, query, cb) -> cb.isTrue(root.get("isActiveFl"));
        Specification<LookupType> spec = activeOnly.and(
            SpecBuilder.build(commonRequest, allowedFields, FILTER_VALUE_CONVERTER));

        List<LookupType> types = repository.findAll(spec);

        Map<String, List<LookupTypeResponse>> grouped = types.stream()
            .map(mapper::toResponse)
            .collect(Collectors.groupingBy(
                LookupTypeResponse::getOwnerModuleCode, TreeMap::new, Collectors.toList()));

        List<OwnerGroupResponse> result = grouped.entrySet().stream()
            .map(entry -> OwnerGroupResponse.builder()
                .ownerModuleCode(entry.getKey())
                .types(entry.getValue())
                .build())
            .toList();

        return ServiceResult.success(result);
    }
}
