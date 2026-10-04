package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.fin.dto.DimensionCreateRequest;
import com.erp.fin.dto.DimensionSearchRequest;
import com.erp.fin.dto.DimensionResponse;
import com.erp.fin.entity.Dimension;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.DimensionMapper;
import com.erp.fin.repository.DimensionRepository;
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
 * Orchestration layer for ENT-FIN-002 (Dimension) — API-FIN-006 (SVC-API-CRUD.md).
 *
 * <p><b>Why the uniqueness check is not delegated to a Domain class.</b> ENT-FIN-002 has no
 * {@code DimensionDomain}: CORE.md's domain-class roster names {@code AccountDomain},
 * {@code DimensionValueDomain}, {@code EventTypeRuleDomain}, {@code JournalEntryDomain},
 * {@code FiscalPeriodDomain} and {@code AllocationRuleDomain}, and no SRS RULE is scoped to
 * ENT-FIN-002 at all. {@code FIN-409-DIMENSION-DUP} is a PLATFORM-STD catalog row, and
 * API-FIN-006's own Validations line places the check explicitly: "uniqueness of code (DB
 * {@code UQ_FIN_DIMENSION_CODE}, checked friendly at service layer)". A.5.18 targets business
 * rules answering "is this operation allowed?", and this is a duplicate-key guard of exactly the
 * shape gov-enforce-error-handling's own service-pattern checklist prescribes.
 *
 * <p>No caching annotations — FIN's approved cache register is empty. No
 * {@code ALLOWED_SORT_FIELDS} is present because SVC-API-SEARCH added API-FIN-005 here (A.5.6).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DimensionService {

    /**
     * A.5.6 — API-FIN-005's filter/sort whitelist. The plan names one filter, {@code code} (LIKE);
     * the remaining entries are ENT-FIN-002's other flat columns, offered for ordering only.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "dimensionPk", "code", "nameAr", "nameEn", "isActiveFl", "createdAt");

    private final DimensionRepository repository;
    private final DimensionMapper mapper;

    /** API-FIN-006 — validate uniqueness (QR-FIN-008's precondition), persist, return. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_DIMENSIONS_CREATE)")
    public ServiceResult<DimensionResponse> create(DimensionCreateRequest request) {
        log.info("Creating Dimension with code: {}", request.getCode());

        if (repository.existsByCode(request.getCode())) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FinErrorCodes.FIN_409_DIMENSION_DUP, request.getCode());
        }

        Dimension saved = repository.save(mapper.toEntity(request));
        log.info("Created Dimension ID: {}", saved.getDimensionPk());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    /**
     * API-FIN-005 — QR-FIN-007, criteria search over ENT-FIN-002, no join, read-only. Load → map
     * → return; an empty page is a success.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_DIMENSIONS_VIEW)")
    public ServiceResult<Page<DimensionResponse>> search(DimensionSearchRequest searchRequest) {
        log.debug("Searching Dimension");

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<Dimension> spec = SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(Set.of()));
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        return ServiceResult.success(repository.findAll(spec, pageable).map(mapper::toResponse));
    }
}
