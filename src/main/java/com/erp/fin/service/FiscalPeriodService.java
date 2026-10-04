package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.common.util.SecurityContextHelper;
import com.erp.fin.domain.FiscalPeriodDomain;
import com.erp.fin.dto.FiscalPeriodResponse;
import com.erp.fin.dto.FiscalPeriodSearchRequest;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.FiscalPeriodMapper;
import com.erp.fin.repository.FiscalPeriodRepository;
import java.time.Instant;
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
 * Orchestration layer for ENT-FIN-008's three guarded status transitions (QR-FIN-039) —
 * API-FIN-024 (open), API-FIN-025 (soft-close) and API-FIN-026 (hard-close), SVC-API-INT.md.
 * SRS A7's PERIOD_STATE machine is a plain guarded transition, never a workflow definition
 * (CORE.md "Workflow engine": forbidden).
 *
 * <p>Every decision belongs to {@link FiscalPeriodDomain}; each transition here only loads the
 * row under {@code PESSIMISTIC_WRITE} (ADR-FIN-041), gathers whatever facts the guard needs,
 * calls it, then calls the entity's own plain mutator and saves — no business-rule {@code if}
 * appears in this class (A.5.18).
 *
 * <p>No caching annotations — FIN's approved cache register is empty.
 *
 * <p>API-FIN-033 (search periods) was added here afterwards, with the A.5.6
 * {@code ALLOWED_SORT_FIELDS} whitelist it requires.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalPeriodService {

    /**
     * A.5.6 — the only fields API-FIN-033 may filter or sort by: SCR-REQ-FIN-007 §B2's named
     * {@code statusCode} filter plus ENT-FIN-008's remaining flat scalar columns, which are what a
     * client can usefully order a period list by. {@code fiscalYearId} is absent on purpose — it
     * is the association {@code fiscalYear}, not a flat path, and {@code SpecBuilder} resolves a
     * field as {@code root.get(field)}; it is ANDed in as an explicit join below (A.5.17).
     * {@code closedBy}/{@code closedAt} are absent too: {@code closedAt} is an {@code Instant} and
     * the module's only value-coercion helper covers {@code LocalDate}, so a JSON string bound
     * would reach the criteria build uncoerced, and {@code closedBy} is not a meaningful ordering
     * key on its own.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "fiscalPeriodPk", "periodNo", "nameAr", "nameEn", "startDate", "endDate",
        "statusCode", "createdAt");

    /** The {@code DATE} filter/sort fields of ENT-FIN-008; see {@code FinSearchSupport}. */
    private static final Set<String> DATE_FILTER_FIELDS = Set.of("startDate", "endDate");

    private final FiscalPeriodRepository repository;
    private final FiscalPeriodMapper mapper;

    /**
     * API-FIN-024 — load (QR-FIN-039) → check RULE-FIN-014 (QR-FIN-040) → transition → return.
     * Fail-fast: the endpoint carries a single decision, so there is nothing to aggregate.
     *
     * <p>{@code FiscalPeriodDomain.assertCanReopen()} is RULE-FIN-014's single implementation and
     * rejects exactly the two terminal states SRS A7 declares ({@code HARD_CLOSE},
     * {@code YEAR_END_CLOSE}) with the catalog's own {@code FIN-409-NOT-REOPENABLE}. Reopening a
     * period that is already OPEN is therefore idempotent rather than an error — the Error Catalog
     * registers no code for it, and API-FIN-024's Errors line names only
     * {@code FIN-409-NOT-REOPENABLE} and {@code FIN-404-PERIOD}.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_PERIODS_UPDATE)")
    public ServiceResult<FiscalPeriodResponse> open(Long id) {
        log.info("Opening FiscalPeriod ID: {}", id);

        FiscalPeriod period = lockOrThrow(id);
        FiscalPeriodDomain.from(period).assertCanReopen();
        period.open();

        return ServiceResult.success(mapper.toResponse(repository.save(period)), Status.UPDATED);
    }

    /**
     * API-FIN-025 — load (QR-FIN-039) → check the transition → soft-close → return. Fail-fast, for
     * the same reason as {@link #open(Long)}. The "current state must be OPEN" requirement is
     * {@code FiscalPeriodDomain.assertCanSoftClose()} ({@code FIN-409-INVALID-TRANSITION}), added
     * by this sub because the guard did not exist and an {@code if} here would be an A.5.18
     * violation.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_PERIODS_UPDATE)")
    public ServiceResult<FiscalPeriodResponse> softClose(Long id) {
        log.info("Soft-closing FiscalPeriod ID: {}", id);

        FiscalPeriod period = lockOrThrow(id);
        FiscalPeriodDomain.from(period).assertCanSoftClose();
        period.softClose();

        return ServiceResult.success(mapper.toResponse(repository.save(period)), Status.UPDATED);
    }

    /**
     * API-FIN-026 — load (QR-FIN-039) → check RULE-FIN-014 (QR-FIN-040) → transition, recording
     * {@code closedBy}/{@code closedAt} as the approving principal and moment (REQ-FIN-037,
     * DBF-FIN-083/084) → return.
     *
     * <p><b>RULE-FIN-014 reuses {@code assertCanReopen()} deliberately:</b> SVC-API-INT.md directs
     * API-FIN-026 to raise {@code FIN-409-NOT-REOPENABLE} ("reused message context") when the
     * period is already Hard Closed, and that method is the rule's single implementation. Adding a
     * second guard with the same condition and the same code would duplicate the rule.
     *
     * <p><b>RULE-FIN-015 is enforced by the {@code @PreAuthorize} below and by nothing else in
     * this method.</b> The rule (srs-fin.md:1026-1030) requires the close-approval action to be
     * "gated by a permission distinct from the journal-entry-creation permission, enforced
     * through the Security module", and its own {@code Data source} line records that FIN has no
     * field to read for it; AC-FIN-038 (srs-fin.md:789-792) names the CORE interceptor as the
     * mechanism. {@code PERM_FIN_PERIODS_CLOSE_APPROVE} is distinct from
     * {@code PERM_FIN_JOURNAL_ENTRIES_CREATE}, so the gate below IS the enforcement. <b>Do not
     * add a SoD check back into this body.</b> A previous implementation resolved facts from
     * SEC's user directory here and refused the close whenever ANY user in the system held both
     * permissions — a global user-set disjointness the SRS never asks for, which denied clean
     * approvers. It was removed by an explicit human decision, together with
     * {@code FinSeparationOfDutiesService} and FIN's XM-FIN-002 dependency on SEC.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_PERIODS_CLOSE_APPROVE)")
    public ServiceResult<FiscalPeriodResponse> hardClose(Long id) {
        log.info("Hard-closing FiscalPeriod ID: {}", id);

        FiscalPeriod period = lockOrThrow(id);
        FiscalPeriodDomain.from(period).assertCanReopen();

        period.hardClose(SecurityContextHelper.getCurrentUsername(), Instant.now());

        return ServiceResult.success(mapper.toResponse(repository.save(period)), Status.UPDATED);
    }

    /**
     * API-FIN-033 — the fiscal-period search SCR-REQ-FIN-007 §B1 lists among this screen's
     * operations ("create (year), search, read") and whose two filters §B2 names:
     * {@code fiscalYearId(EXACT)} and {@code statusCode(EXACT)}.
     *
     * <p>A.5.17 — {@code fiscalYearId} is the association {@code fiscalYear}, a nested
     * {@code fiscalYear.fiscalYearPk} path {@code SpecBuilder} cannot resolve, so it becomes an
     * explicit {@code Specification} join ANDed with the generic specification built from the
     * remaining filters. {@code startDate}/{@code endDate} bounds arrive as JSON strings and are
     * coerced by {@code FinSearchSupport.temporalFieldConverter}.
     *
     * <p><b>The parent id is OPTIONAL here — a deliberate divergence from
     * {@code DimensionValueService.search}, which rejects a null parent id with
     * {@code FIN-404-DIMENSION} (A.5.16). Do not "fix" this back.</b> §B2 makes both filters EXACT
     * but neither mandatory, and this endpoint exists precisely so that a client which did NOT
     * create the fiscal year in the same session can discover a period id at all — requiring the
     * year id first would leave that client with no way in, which is the gap API-FIN-033 closes.
     * The predicate is therefore applied only when the id is present, exactly as
     * {@code JournalEntryService.search} already does for its optional {@code periodId}.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_PERIODS_VIEW)")
    public ServiceResult<Page<FiscalPeriodResponse>> search(
            FiscalPeriodSearchRequest searchRequest) {
        log.debug("Searching FiscalPeriod");

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<FiscalPeriod> spec = SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(DATE_FILTER_FIELDS));

        Long fiscalYearId = searchRequest.getFiscalYearId();
        if (fiscalYearId != null) {
            Specification<FiscalPeriod> parentSpec = (root, query, cb) ->
                cb.equal(root.get("fiscalYear").get("fiscalYearPk"), fiscalYearId);
            spec = parentSpec.and(spec);
        }

        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        return ServiceResult.success(repository.findAll(spec, pageable).map(mapper::toResponse));
    }

    /**
     * FK resolution under the ADR-FIN-041 row lock, taken before the current-status check so two
     * concurrent transitions of one period serialize. Not a rule: an unknown id is
     * {@code FIN-404-PERIOD}.
     */
    private FiscalPeriod lockOrThrow(Long id) {
        return repository.lockForTransition(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_PERIOD, id));
    }
}
