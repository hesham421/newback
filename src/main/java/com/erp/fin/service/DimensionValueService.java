package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.fin.domain.DimensionValueDomain;
import com.erp.fin.dto.DimensionValueCreateRequest;
import com.erp.fin.dto.DimensionValueSearchRequest;
import com.erp.fin.dto.DimensionValueResponse;
import com.erp.fin.entity.Dimension;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.DimensionValueMapper;
import com.erp.fin.repository.DimensionRepository;
import com.erp.fin.repository.DimensionValueRepository;
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
 * Orchestration layer for ENT-FIN-003 (DimensionValue) — API-FIN-007 (SVC-API-CRUD.md). The
 * endpoint itself is a method on {@code DimensionController} (build-create-controller A.6.9 folds
 * child endpoints into the parent's controller); only the service is separate.
 *
 * <p>No caching annotations — FIN's approved cache register is empty. SVC-API-SEARCH added
 * API-FIN-008, the CHILD search, with its A.5.6 {@code ALLOWED_SORT_FIELDS} whitelist.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DimensionValueService {

    /**
     * A.5.6 — API-FIN-008's filter/sort whitelist. {@code dimensionId} is deliberately NOT here:
     * the parent scope never travels through the generic specification (A.5.17), it is an explicit
     * join below.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "dimensionValuePk", "code", "nameAr", "nameEn", "sortOrder", "isActiveFl", "createdAt");

    private final DimensionValueRepository repository;
    private final DimensionRepository dimensionRepository;
    private final DimensionValueMapper mapper;

    /**
     * API-FIN-007 — resolve the parent dimension ({@code FIN-404-DIMENSION} if unknown), ask
     * QR-FIN-010 whether the code is already taken inside it, then delegate RULE-FIN-002 to
     * {@link DimensionValueDomain#create(Long, String, boolean)}
     * ({@code FIN-409-DIMVALUE-DUP}).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_DIMENSIONS_CREATE)")
    public ServiceResult<DimensionValueResponse> create(Long dimensionId,
                                                        DimensionValueCreateRequest request) {
        log.info("Creating DimensionValue under Dimension ID: {} with code: {}",
            dimensionId, request.getCode());

        Dimension parent = dimensionRepository.findById(dimensionId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_DIMENSION, dimensionId));

        boolean codeAlreadyTakenInDimension =
            repository.existsByDimension_DimensionPkAndCode(dimensionId, request.getCode());

        DimensionValueDomain.create(dimensionId, request.getCode(), codeAlreadyTakenInDimension);

        DimensionValue saved = repository.save(mapper.toEntity(request, parent));
        log.info("Created DimensionValue ID: {}", saved.getDimensionValuePk());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    /**
     * API-FIN-035 — soft deactivation only: retire a dimension value so RULE-FIN-009 /
     * REQ-FIN-021 can reject a journal line that still cites it. No SRS rule answers "may this
     * value be deactivated?", so there is nothing to delegate to {@link DimensionValueDomain}
     * before the mutation — the same shape as {@code AccountService.deactivate} (API-FIN-004) —
     * and the flag moves through ENT-FIN-003's own {@code deactivate()} helper (DBF-FIN-029),
     * never a direct assignment.
     *
     * <p>Effect: {@link DimensionValueDomain#checkUsableOnLine(Long)} reads exactly this flag, so
     * once a value is deactivated every posting path that cites it (API-FIN-019, 020, 014, 017)
     * answers {@code FIN-409-INVALID-DIMENSION}. Gated on {@code PERM_FIN_DIMENSIONS_UPDATE} —
     * deactivate is modelled as UPDATE, exactly as API-FIN-004 and MDL_LOOKUPS do.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_DIMENSIONS_UPDATE)")
    public ServiceResult<DimensionValueResponse> deactivate(Long id) {
        log.info("Deactivating DimensionValue ID: {}", id);

        DimensionValue entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_DIMVALUE, id));

        entity.deactivate();

        DimensionValue saved = repository.save(entity);
        log.info("Deactivated DimensionValue ID: {}", saved.getDimensionValuePk());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * API-FIN-008 — QR-FIN-011, the CHILD search. The parent id arrives inside the body's filters
     * and is read through {@link DimensionValueSearchRequest#getDimensionId()}, never as a path
     * variable (the plan's own Request line says so); the endpoint itself is
     * {@code DimensionController.searchDimensionValues}.
     *
     * <p>A.5.16 — a non-null parent id is required, and an unknown one is
     * {@code FIN-404-DIMENSION}, the single code the plan's Errors line names for this API. A.5.17
     * — the parent scope is an explicit {@code Specification} join on the nested
     * {@code dimension.dimensionPk} path, because {@code SpecBuilder} resolves a filter field as a
     * flat {@code root.get(field)} and could not express it; it is ANDed with the generic
     * specification built from the remaining filters.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_DIMENSIONS_VIEW)")
    public ServiceResult<Page<DimensionValueResponse>> search(
            DimensionValueSearchRequest searchRequest) {
        Long dimensionId = searchRequest.getDimensionId();
        log.debug("Searching DimensionValue under Dimension ID: {}", dimensionId);

        if (dimensionId == null || !dimensionRepository.existsById(dimensionId)) {
            throw new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_DIMENSION, dimensionId);
        }

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);

        Specification<DimensionValue> parentSpec = (root, query, cb) ->
            cb.equal(root.get("dimension").get("dimensionPk"), dimensionId);
        Specification<DimensionValue> spec = parentSpec.and(SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(Set.of())));
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        return ServiceResult.success(repository.findAll(spec, pageable).map(mapper::toResponse));
    }
}
