package com.erp.mdl.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.mdl.domain.LookupValueDomain;
import com.erp.mdl.dto.LookupValueCreateRequest;
import com.erp.mdl.dto.LookupValueReorderRequest;
import com.erp.mdl.dto.LookupValueResponse;
import com.erp.mdl.dto.LookupValueSearchRequest;
import com.erp.mdl.dto.LookupValueUpdateRequest;
import com.erp.mdl.entity.LookupType;
import com.erp.mdl.entity.LookupValue;
import com.erp.mdl.exception.MdlErrorCodes;
import com.erp.mdl.mapper.LookupValueMapper;
import com.erp.mdl.repository.LookupTypeRepository;
import com.erp.mdl.repository.LookupValueRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
 * Orchestration layer for ENT-MDL-002 (LookupValue) — API-MDL-006/007/008/009 (SVC-API-CRUD.md)
 * plus API-MDL-005 (SVC-API-SEARCH.md, read-only). No caching annotations — same reasoning as
 * {@link LookupTypeService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LookupValueService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "lookupValuePk", "code", "nameAr", "nameEn", "sortOrder", "isActiveFl", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "sortOrder";

    private final LookupValueRepository repository;
    private final LookupTypeRepository lookupTypeRepository;
    private final LookupValueMapper mapper;

    /**
     * API-MDL-006 — resolve the parent type (404 if missing), then RULE-MDL-002 (QR-MDL-014),
     * delegated to {@link LookupValueDomain#create} for the actual decision.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_CREATE)")
    public ServiceResult<LookupValueResponse> create(Long lookupTypeId, LookupValueCreateRequest request) {
        log.info("Creating LookupValue under LookupType ID: {} with code: {}", lookupTypeId, request.getCode());

        LookupType parent = lookupTypeRepository.findById(lookupTypeId)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MdlErrorCodes.MDL_404_TYPE, lookupTypeId));

        boolean codeAlreadyTakenInType =
            repository.existsByLookupType_LookupTypePkAndCode(lookupTypeId, request.getCode());

        LookupValueDomain.create(lookupTypeId, request.getCode(), codeAlreadyTakenInType);

        LookupValue saved = repository.save(mapper.toEntity(request, parent));
        log.info("Created LookupValue ID: {}", saved.getLookupValuePk());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    /** API-MDL-007 — no business rule beyond existence; name/sortOrder are freely mutable. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_UPDATE)")
    public ServiceResult<LookupValueResponse> update(Long id, LookupValueUpdateRequest request) {
        log.info("Updating LookupValue ID: {}", id);

        LookupValue entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MdlErrorCodes.MDL_404_VALUE, id));

        mapper.updateEntityFromRequest(entity, request);
        LookupValue saved = repository.save(entity);
        log.info("Updated LookupValue ID: {}", saved.getLookupValuePk());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /** API-MDL-008 — soft-deactivate only. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_UPDATE)")
    public ServiceResult<LookupValueResponse> deactivate(Long id) {
        log.info("Deactivating LookupValue ID: {}", id);

        LookupValue entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MdlErrorCodes.MDL_404_VALUE, id));

        entity.deactivate();
        LookupValue saved = repository.save(entity);
        log.info("Deactivated LookupValue ID: {}", saved.getLookupValuePk());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * API-MDL-009 — validate that every requested id belongs to {@code lookupTypeId}, then assign
     * {@code sortOrder} = list position and persist all in ONE transaction (QR-MDL-009, batch;
     * CORE.md's transaction-scope note). The membership check is a structural/input-shape
     * validation ("does this id belong to this type"), not a "is this operation allowed?"
     * business rule — so it is a direct service-layer guard (Status.VALIDATION_ERROR), not a
     * {@code LookupValueDomain} delegation (A.5.18 does not apply to referential-shape checks over
     * an ad-hoc request collection, the same reasoning as a child search's explicit join, A.5.17).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_UPDATE)")
    public ServiceResult<List<LookupValueResponse>> reorder(Long lookupTypeId, LookupValueReorderRequest request) {
        log.info("Reordering LookupValues for LookupType ID: {}", lookupTypeId);

        List<Long> orderedIds = request.getOrderedValueIds();
        List<LookupValue> found = repository.findAllById(orderedIds);

        Map<Long, LookupValue> byId =
            found.stream().collect(Collectors.toMap(LookupValue::getLookupValuePk, Function.identity()));

        boolean allBelongToType = found.size() == orderedIds.size() && found.stream()
            .allMatch(value -> value.getLookupType() != null
                && lookupTypeId.equals(value.getLookupType().getLookupTypePk()));
        if (!allBelongToType) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                MdlErrorCodes.MDL_400_REORDER_MISMATCH, lookupTypeId);
        }

        List<LookupValue> ordered = new ArrayList<>(orderedIds.size());
        for (int position = 0; position < orderedIds.size(); position++) {
            LookupValue value = byId.get(orderedIds.get(position));
            value.setSortOrder(position);
            ordered.add(value);
        }

        repository.saveAll(ordered);
        log.info("Reordered {} LookupValues for LookupType ID: {}", ordered.size(), lookupTypeId);

        return ServiceResult.success(
            ordered.stream().map(mapper::toResponse).toList(), Status.UPDATED);
    }

    /**
     * API-MDL-005 — search values of one type (SVC-API-SEARCH.md). {@code code} flows through the
     * generic {@code filters[]} list on {@link LookupValueSearchRequest} (client-chosen operator,
     * e.g. LIKE); default sort is {@code sortOrder} when the caller supplies none. Validates the
     * parent type exists first ({@code MDL-404-TYPE} if not).
     *
     * <p>A.5.16/A.5.17 — a non-null {@code lookupTypeId} is required (read via
     * {@link LookupValueSearchRequest#getLookupTypeId()}, carried in the body's filters, never a
     * path variable), and the parent scope is an explicit {@code Specification} join rather than a
     * generic filter: {@link SpecBuilder}'s flat {@code root.get(field)} cannot express the nested
     * {@code lookupType.lookupTypePk} path, so that predicate is written directly here and ANDed
     * with the generic code-filter specification built from the shared plumbing.
     *
     * <p>The earlier GET + scalar-argument deviation (same reasoning as
     * {@link LookupTypeService#search}) was reversed to {@code POST /search}, mirroring FIN's
     * {@code DimensionValueService#search} child-search shape.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_VIEW)")
    public ServiceResult<Page<LookupValueResponse>> search(LookupValueSearchRequest searchRequest) {
        Long lookupTypeId = searchRequest.getLookupTypeId();
        log.debug("Searching LookupValue for LookupType ID: {}", lookupTypeId);

        if (lookupTypeId == null || !lookupTypeRepository.existsById(lookupTypeId)) {
            throw new LocalizedException(Status.NOT_FOUND, MdlErrorCodes.MDL_404_TYPE, lookupTypeId);
        }

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        if (commonRequest.getSortField() == null || commonRequest.getSortField().isBlank()) {
            commonRequest = SearchRequest.builder()
                .filters(commonRequest.getFilters())
                .sortField(DEFAULT_SORT_FIELD)
                .sortDirection(commonRequest.getSortDirection())
                .page(commonRequest.getPage())
                .size(commonRequest.getSize())
                .build();
        }

        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);

        Specification<LookupValue> parentSpec = (root, query, cb) ->
            cb.equal(root.get("lookupType").get("lookupTypePk"), lookupTypeId);
        Specification<LookupValue> spec = parentSpec.and(
            SpecBuilder.build(commonRequest, allowedFields, DefaultFieldValueConverter.INSTANCE));

        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);
        Page<LookupValue> resultPage = repository.findAll(spec, pageable);

        return ServiceResult.success(resultPage.map(mapper::toResponse));
    }
}
