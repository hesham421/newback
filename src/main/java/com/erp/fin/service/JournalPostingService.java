package com.erp.fin.service;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.domain.AccountDomain;
import com.erp.fin.domain.DimensionValueDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.FiscalYear;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.JournalEntryMapper;
import com.erp.fin.mapper.JournalEntryMapper.BuiltDimension;
import com.erp.fin.mapper.JournalEntryMapper.BuiltEntry;
import com.erp.fin.mapper.JournalEntryMapper.BuiltLine;
import com.erp.fin.numbering.JournalDocNoGenerator;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.repository.JournalEntryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one build→validate→post pipeline every system-sourced entry goes through — QR-FIN-028
 * through QR-FIN-033, shared by API-FIN-020 (event build), API-FIN-014 (template run),
 * API-FIN-017 (allocation run), API-FIN-021 (reversal) and API-FIN-027 (year-end closing and
 * opening entries). Introduced by SVC-API-INT so those five paths cannot drift apart in how they
 * validate or how they number; API-FIN-019's own pipeline in {@code JournalEntryService} predates
 * it and is deliberately left untouched.
 *
 * <p><b>Transaction scope (CORE.md).</b> Every method here is {@code @Transactional} with the
 * default {@code REQUIRED} propagation, so it joins the calling service's transaction rather than
 * opening one of its own: the whole build→validate→post sequence — and, for a multi-entry path,
 * the whole run — is ONE transaction. Nothing is written before validation succeeds (the aggregate
 * is assembled in memory, every guard runs against that graph, and the single {@code save} happens
 * only after {@code JournalEntry.post(...)}), and any {@code LocalizedException} rolls the whole
 * attempt back regardless. No partially-built DRAFT can survive a failed validation.
 *
 * <p><b>Every failure returned together (REQ-FIN-015 / AC-FIN-015).</b> Each post-time rule is
 * evaluated through its domain class's non-throwing {@code check*} sibling, which returns an
 * {@link ErrorDetail} instead of throwing; the pipeline collects what comes back and throws ONCE
 * with the whole list. Not-found preconditions resolved by the callers (fiscal year, period,
 * account, template, rule) stay fail-fast — an unresolvable reference leaves nothing further to
 * validate.
 *
 * <p><b>Rules delegated, none inlined</b> (A.5.18) — this service only gathers facts and collects
 * answers:
 * <ul>
 *   <li>RULE-FIN-007 (QR-FIN-030) → {@code AccountDomain.checkPostable()}, once per line;</li>
 *   <li>RULE-FIN-009 (QR-FIN-032) → {@code DimensionValueDomain.checkUsableOnLine(...)}, once per
 *       tag;</li>
 *   <li>RULE-FIN-006 (QR-FIN-029) → {@code JournalEntryDomain.checkBalanced(lines)};</li>
 *   <li>RULE-FIN-008 (QR-FIN-031) →
 *       {@code JournalEntryDomain.checkTargetPeriodOpenForGeneratedEntry(journalTypeCode, ...)},
 *       which also owns the rule's own year-end exemption (srs-fin.md §A5, amended) — the two
 *       entries API-FIN-027 generates are exempt because the rule says so, not because a caller
 *       asked.</li>
 * </ul>
 *
 * <p><b>RULE-FIN-016.</b> No update and no delete path exists here, and none may be added — the
 * rule is enforced structurally, by omission.
 *
 * <p><b>Gate.</b> {@code @PreAuthorize("isAuthenticated()")} — this service fronts no endpoint of
 * its own and is only ever reached from another FIN service whose own {@code @PreAuthorize} has
 * already enforced the screen permission. Same spec-sanctioned form
 * {@link FinLookupValidationService} uses, never an unguarded public method (A.5.2).
 *
 * <p>No caching annotations: FIN's approved cache register is empty, and an accounting posting is
 * on gov-enforce-caching-rules' "never cacheable" list regardless. No
 * {@code ALLOWED_SORT_FIELDS}: this service exposes no search.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalPostingService {

    /** JOURNAL_TYPE (SRS A6) — an entry built by the rule engine from an event (API-FIN-020). */
    public static final String JOURNAL_TYPE_EVENT_GENERATED = "EVENT_GENERATED";

    /** JOURNAL_TYPE (SRS A6) — an entry produced by a recurring template run (API-FIN-014). */
    public static final String JOURNAL_TYPE_RECURRING = "RECURRING";

    /** JOURNAL_TYPE (SRS A6) — an entry produced by an allocation rule run (API-FIN-017). */
    public static final String JOURNAL_TYPE_ALLOCATION = "ALLOCATION";

    /** JOURNAL_TYPE (SRS A6) — a reversal entry (API-FIN-014 reversing templates, API-FIN-021). */
    public static final String JOURNAL_TYPE_REVERSAL = "REVERSAL";

    /**
     * JOURNAL_TYPE — the year-end closing entry (API-FIN-027). One of the two values the plan's
     * "Decisions Applied" adds to the JOURNAL_TYPE lookup this stage, recorded there as a
     * data-only, non-breaking extension rather than a new REQ or RULE. Defined by
     * {@link JournalEntryDomain}, which owns RULE-FIN-008 and its year-end exemption, and
     * re-exported here so every posting caller keeps one import.
     */
    public static final String JOURNAL_TYPE_CLOSING = JournalEntryDomain.JOURNAL_TYPE_CLOSING;

    /** JOURNAL_TYPE — the next year's opening entry (API-FIN-027); see {@link #JOURNAL_TYPE_CLOSING}. */
    public static final String JOURNAL_TYPE_OPENING = JournalEntryDomain.JOURNAL_TYPE_OPENING;

    private final JournalEntryRepository repository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final JournalEntryMapper mapper;

    /**
     * Build → validate (QR-FIN-029..032, every failure returned together) → docNo under the
     * fiscal-year row lock → post (QR-FIN-033), in one transaction. The write is flushed at once
     * (QR-FIN-025, ADR-FIN-030) so a store refusal — {@code UQ_FIN_JOURNAL_ENTRY_EVENT_REF} on a
     * concurrent duplicate event — is raised inside the call, where
     * {@code FinPersistenceRefusalAdvisor} translates it, never a generic error at commit.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public JournalEntry buildValidateAndPost(PostingRequest request) {
        log.info("Posting {} entry into fiscal year ID: {} period ID: {}",
            request.journalTypeCode(), request.fiscalYear().getFiscalYearPk(),
            request.period().getFiscalPeriodPk());

        List<ErrorDetail> failures = new ArrayList<>();
        for (BuiltLine line : request.lines()) {
            AccountDomain.from(line.account()).checkPostable().ifPresent(failures::add);
            for (BuiltDimension tag : line.dimensions()) {
                DimensionValueDomain.from(tag.dimensionValue())
                    .checkUsableOnLine(tag.dimension().getDimensionPk())
                    .ifPresent(failures::add);
            }
        }

        Long fiscalYearPk = request.fiscalYear().getFiscalYearPk();
        fiscalYearRepository.lockForDocNoAllocation(fiscalYearPk)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_YEAR, fiscalYearPk));
        String docNo = JournalDocNoGenerator.next(request.fiscalYear().getCode(),
            highestDocNoOf(fiscalYearPk));

        JournalEntry entry = mapper.toEntity(new BuiltEntry(docNo, request.docDate(),
            request.fiscalYear(), request.period(), request.journalTypeCode(),
            request.eventReference(), request.descriptionAr(), request.descriptionEn(),
            request.lines()));

        JournalEntryDomain entryDomain =
            JournalEntryDomain.create(request.eventReference(), false);
        entryDomain.checkBalanced(entry.getLines()).ifPresent(failures::add);
        entryDomain.checkTargetPeriodOpenForGeneratedEntry(
                request.journalTypeCode(), request.period().getStatusCode())
            .ifPresent(failures::add);

        if (!failures.isEmpty()) {
            throw new LocalizedException(Status.CONFLICT, failures);
        }

        entry.post(Instant.now());

        JournalEntry saved = repository.saveAndFlush(entry);
        log.info("Posted JournalEntry ID: {} docNo: {}", saved.getJournalEntryPk(),
            saved.getDocNo());
        return saved;
    }

    /**
     * The fiscal period whose span contains {@code date} — the target of a template run
     * (API-FIN-014) and an allocation run (API-FIN-017); none → {@code FIN-404-PERIOD}, v1 as
     * built (ADR-FIN-030). API-FIN-020 no longer comes here: it resolves its period through
     * {@code FiscalPeriodRepository.findCoveringDate} and answers {@code FIN-422-NO-DEFINED-PERIOD}.
     * The period's state is judged at post by RULE-FIN-008, not here.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public FiscalPeriod resolvePeriodContaining(LocalDate date) {
        log.debug("Resolving fiscal period containing date: {}", date);

        Specification<FiscalPeriod> covering = (root, query, cb) -> cb.and(
            cb.lessThanOrEqualTo(root.get("startDate"), date),
            cb.greaterThanOrEqualTo(root.get("endDate"), date));

        return fiscalPeriodRepository.findBy(covering, fluent -> fluent
                .sortBy(Sort.by(Sort.Direction.ASC, "periodNo"))
                .first())
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_PERIOD, date));
    }

    /**
     * The currently open period RULE-FIN-012 substitutes when a reversal's original period is no
     * longer Open (QR-FIN-036) — the earliest OPEN period by {@code periodNo}, or {@code null}
     * when the ledger has none. Returning {@code null} rather than throwing is deliberate:
     * {@code JournalEntryDomain.resolveReversalPeriodPk(...)} owns the decision about what an
     * absent open period means.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public FiscalPeriod findCurrentOpenPeriod() {
        log.debug("Resolving current open fiscal period");

        Specification<FiscalPeriod> open = (root, query, cb) ->
            cb.equal(root.get("statusCode"), FiscalPeriod.STATUS_OPEN);

        return fiscalPeriodRepository.findBy(open, fluent -> fluent
                .sortBy(Sort.by(Sort.Direction.ASC, "periodNo"))
                .first())
            .orElse(null);
    }

    /**
     * The fiscal year's greatest committed {@code docNo}, or {@code null} when it has no entry yet
     * — the one fact {@link JournalDocNoGenerator} needs. Same read
     * {@code JournalEntryService.createManual} performs for API-FIN-019: an explicit
     * {@code Specification} scopes it to the fiscal year (the nested
     * {@code fiscalYear.fiscalYearPk} path is beyond {@code SpecBuilder}'s flat
     * {@code root.get(field)}), and the fluent {@code first()} fetches exactly one row. Ordering
     * by {@code docNo} descending is numerically correct inside one fiscal year because every
     * value shares the {@code JV-{code}-} prefix and a fixed-width, zero-padded counter.
     */
    private String highestDocNoOf(Long fiscalYearPk) {
        Specification<JournalEntry> ofFiscalYear = (root, query, cb) ->
            cb.equal(root.get("fiscalYear").get("fiscalYearPk"), fiscalYearPk);

        return repository.findBy(ofFiscalYear, fluent -> fluent
                .sortBy(Sort.by(Sort.Direction.DESC, "docNo"))
                .first())
            .map(JournalEntry::getDocNo)
            .orElse(null);
    }

    /**
     * One assembled entry handed to {@link #buildValidateAndPost(PostingRequest)}.
     *
     * <p>There is deliberately no "does the period gate apply?" switch on this record. RULE-FIN-008
     * now carries its own exemption for the two year-end entries (srs-fin.md §A5, amended), and
     * {@code JournalEntryDomain.checkTargetPeriodOpenForGeneratedEntry(...)} derives it from
     * {@link #journalTypeCode()} — from what the entry <i>is</i>. A caller cannot ask for the
     * exemption, and the rule keeps a single implementation in the domain layer.
     */
    public record PostingRequest(FiscalYear fiscalYear,
                                 FiscalPeriod period,
                                 LocalDate docDate,
                                 String journalTypeCode,
                                 String eventReference,
                                 String descriptionAr,
                                 String descriptionEn,
                                 List<BuiltLine> lines) {
    }
}
