package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.domain.RecurringTemplateDomain;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineResponse;
import com.erp.fin.dto.RecurringTemplateCreateRequest;
import com.erp.fin.dto.RecurringTemplateLineCreateRequest;
import com.erp.fin.dto.RecurringTemplateLineResponse;
import com.erp.fin.dto.RecurringTemplateResponse;
import com.erp.fin.dto.RecurringTemplateSearchRequest;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.JournalLine;
import com.erp.fin.entity.RecurringTemplate;
import com.erp.fin.entity.RecurringTemplateLine;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.JournalEntryMapper;
import com.erp.fin.mapper.JournalEntryMapper.BuiltLine;
import com.erp.fin.mapper.RecurringTemplateLineMapper;
import com.erp.fin.mapper.RecurringTemplateMapper;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.DimensionValueRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.JournalEntryRepository;
import com.erp.fin.repository.RecurringTemplateLineRepository;
import com.erp.fin.repository.RecurringTemplateRepository;
import com.erp.fin.service.JournalPostingService.PostingRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
 * Orchestration layer for ENT-FIN-011 / ENT-FIN-012 (RecurringTemplate and its lines) —
 * API-FIN-013 (SVC-API-CRUD.md) and API-FIN-014, run a template (SVC-API-INT.md).
 *
 * <p>No caching annotations — FIN's approved cache register is empty. No
 * {@code ALLOWED_SORT_FIELDS} is present because SVC-API-SEARCH added API-FIN-012 here (A.5.6).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTemplateService {

    /**
     * The {@code RECURRING_SCHEDULE_TYPE} value for which no frequency applies (SRS A6:
     * {@code RECURRING, REVERSING}; ENT-FIN-011 frequencyCode: "not applicable to a pure
     * reversing template").
     */
    public static final String SCHEDULE_TYPE_REVERSING = "REVERSING";

    /**
     * A.5.6 — API-FIN-012's filter/sort whitelist: the plan's {@code nameAr}/{@code nameEn},
     * {@code scheduleTypeCode} and {@code isActiveFl} filters, plus ENT-FIN-011's remaining flat
     * columns for ordering.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "recurringTemplatePk", "nameAr", "nameEn", "scheduleTypeCode", "frequencyCode",
        "startDate", "nextRunDate", "endDate", "isActiveFl", "createdAt");

    /**
     * DBF-FIN-114/115/116 — {@code DATE} columns. Three of the fields above are dates, and a JSON
     * body carries a bound as a string; without the coercion the criteria build would compare a
     * String to a {@code LocalDate} path. Added 2026-09-19 alongside publishing this search's
     * filterable field set, so what the api-docs now advertise is what the endpoint accepts.
     */
    private static final Set<String> DATE_FILTER_FIELDS =
        Set.of("startDate", "nextRunDate", "endDate");

    private final RecurringTemplateRepository repository;
    private final RecurringTemplateLineRepository lineRepository;
    private final AccountRepository accountRepository;
    private final DimensionValueRepository dimensionValueRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final RecurringTemplateMapper mapper;
    private final RecurringTemplateLineMapper lineMapper;
    private final JournalEntryMapper journalEntryMapper;
    private final FinLookupValidationService lookupValidation;
    private final JournalPostingService postingService;

    /**
     * API-FIN-013 — validate the schedule/frequency/direction codes against MDL (XM-FIN-001),
     * apply the conditional-frequency input guard, set {@code nextRunDate = startDate}, then
     * persist the template together with its lines (QR-FIN-018) in ONE transaction.
     *
     * <p><b>The frequency guard is an input-shape check, not a Domain delegation.</b> ENT-FIN-011
     * has no SRS RULE and therefore no Domain class in CORE.md's roster;
     * {@code FIN-400-MISSING-FREQUENCY} is a PLATFORM-STD catalog row at HTTP 400
     * ({@code Status.VALIDATION_ERROR}), i.e. a conditional-requiredness check on the request
     * body, the kind build-create-dto's own scope note keeps out of {@code <Entity>Domain}. It is
     * expressed structurally here only because bean validation cannot make one field's
     * requiredness depend on another's value while still carrying the catalog's own error code.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_RECURRING_TEMPLATES_CREATE)")
    public ServiceResult<RecurringTemplateResponse> create(
            RecurringTemplateCreateRequest request) {
        log.info("Creating RecurringTemplate with schedule type: {}",
            request.getScheduleTypeCode());

        lookupValidation.assertValidCode(
            FinLookupValidationService.KEY_RECURRING_SCHEDULE_TYPE, request.getScheduleTypeCode());
        lookupValidation.assertValidCodeIfPresent(
            FinLookupValidationService.KEY_RECURRING_FREQUENCY, request.getFrequencyCode());

        boolean frequencyMissing = request.getFrequencyCode() == null
            || request.getFrequencyCode().isBlank();
        boolean frequencyRequired =
            !SCHEDULE_TYPE_REVERSING.equals(request.getScheduleTypeCode());
        if (frequencyRequired && frequencyMissing) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                FinErrorCodes.FIN_400_MISSING_FREQUENCY, request.getScheduleTypeCode());
        }

        for (RecurringTemplateLineCreateRequest line : request.getLines()) {
            lookupValidation.assertValidCode(
                FinLookupValidationService.KEY_DEBIT_CREDIT, line.getDirectionCode());
        }

        RecurringTemplate saved = repository.save(mapper.toEntity(request));

        List<RecurringTemplateLine> lines = new ArrayList<>(request.getLines().size());
        int lineNo = 1;
        for (RecurringTemplateLineCreateRequest lineRequest : request.getLines()) {
            lines.add(lineMapper.toEntity(lineRequest, saved, lineNo,
                resolveAccount(lineRequest.getAccountId()),
                resolveDimensionValue(lineRequest.getDimensionValueId())));
            lineNo++;
        }
        List<RecurringTemplateLine> savedLines = lineRepository.saveAll(lines);

        log.info("Created RecurringTemplate ID: {} with {} line(s)",
            saved.getRecurringTemplatePk(), savedLines.size());

        List<RecurringTemplateLineResponse> lineResponses =
            savedLines.stream().map(lineMapper::toResponse).toList();

        return ServiceResult.success(mapper.toResponse(saved, lineResponses), Status.CREATED);
    }

    /**
     * API-FIN-014 — load the template (QR-FIN-019) → build an entry from its lines
     * (journalTypeCode=RECURRING) → validate and post (QR-FIN-029..033) → advance
     * {@code nextRunDate} per {@code frequencyCode} → for a REVERSING template, also build and
     * post the linked reversal in the next period (REQ-FIN-024, RULE-FIN-011/012 via
     * QR-FIN-034/036) → return the primary entry.
     *
     * <p><b>Atomic unit: the run.</b> For a plain RECURRING template the run is one entry, so
     * "per entry" and "per run" coincide. For a REVERSING template the spec makes the reversal a
     * consequence of the first entry posting ("When a reversing template's entry posts, the system
     * shall automatically build and post its exact reversal in the next period", REQ-FIN-024) and
     * the endpoint answers with a single 201, so the two entries — and the {@code nextRunDate}
     * advance — commit together or not at all. A committed entry whose mandated reversal never
     * landed could not be corrected afterwards: RULE-FIN-016 forbids editing a posted entry.
     * {@link JournalPostingService} joins this transaction rather than opening its own, so
     * CORE.md's "ONE transaction per entry" still holds for each entry individually.
     *
     * <p><b>Aggregated.</b> The Validations line is "the same RULE-FIN-006/007/008/009 checks as
     * API-FIN-019", and API-FIN-019 is the path REQ-FIN-015 / AC-FIN-015 is written about, so the
     * four post-time rules are reported together by the shared pipeline. The not-found
     * preconditions (template, target period) stay fail-fast.
     *
     * <p><b>Rules delegated, none inlined</b> (A.5.18): the active gate →
     * {@code RecurringTemplateDomain.assertCanRun()}, run first, before the period is resolved and
     * before any line is read, so a deactivated template is refused with
     * {@code FIN-409-NOT-ACTIVE} and nothing is built or posted; RULE-FIN-006/007/008/009 → the
     * shared pipeline's own delegations; the {@code nextRunDate} step →
     * {@code RecurringTemplateMapper}'s pure calendar derivation. The one conditional in this method — whether the template is
     * REVERSING — selects which of the two flows the spec defines is being executed; it evaluates
     * no rule, raises no catalog error, and permits nothing that would otherwise be denied.
     *
     * <p><b>Scheduler parity.</b> SVC-API-INT.md notes the endpoint is the same whether a user or
     * an internal scheduler triggers it. Any such scheduler must call this method through the
     * project's own security context propagation, since {@code @PreAuthorize} fires regardless of
     * call path — and it inherits the active gate for the same reason: this method is the module's
     * single run path, and no scheduled or internal caller exists that bypasses it (there is no
     * {@code @Scheduled} anywhere in {@code src/main/java}; {@code RecurringTemplateController} is
     * the only caller of this service today).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_RECURRING_TEMPLATES_UPDATE)")
    public ServiceResult<JournalEntryResponse> run(Long id) {
        log.info("Running RecurringTemplate ID: {}", id);

        RecurringTemplate template = repository.lockForRun(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_TEMPLATE, id));

        RecurringTemplateDomain.from(template).assertCanRun();

        LocalDate runDate = template.getNextRunDate();
        FiscalPeriod period = postingService.resolvePeriodContaining(runDate);

        List<BuiltLine> lines = new ArrayList<>();
        for (RecurringTemplateLine line
                : lineRepository.findByRecurringTemplatePk(template.getRecurringTemplatePk())) {
            lines.add(new BuiltLine(line.getAccount(), line.getAmount(), line.getDirectionCode(),
                false, template.getNameAr(), template.getNameEn(),
                dimensionTagsOf(line.getDimensionValue())));
        }

        JournalEntry posted = postingService.buildValidateAndPost(new PostingRequest(
            period.getFiscalYear(), period, runDate,
            JournalPostingService.JOURNAL_TYPE_RECURRING, null,
            template.getNameAr(), template.getNameEn(), lines));

        template.setNextRunDate(
            mapper.nextRunDateAfter(runDate, template.getFrequencyCode()));
        repository.save(template);

        if (SCHEDULE_TYPE_REVERSING.equals(template.getScheduleTypeCode())) {
            postReversalInNextPeriod(posted, period);
        }

        List<JournalLineResponse> lineResponses =
            posted.getLines().stream().map(journalEntryMapper::toLineResponse).toList();

        return ServiceResult.success(
            journalEntryMapper.toResponse(posted, lineResponses), Status.CREATED);
    }

    /**
     * REQ-FIN-024 — the automatic reversal of a reversing template's entry, in the next period.
     * RULE-FIN-011's mirroring is {@code JournalEntryDomain.reversalDirectionOf(...)}, and the two
     * rows are linked in both directions exactly as API-FIN-021 links them — classic reversal: the
     * accrual entry stays POSTED in its own period, the mirror posts in the next one, and the pair
     * nets to zero across the two periods. No POSTED row's accounting state is modified, so
     * RULE-FIN-016 needs no carve-out.
     *
     * <p><b>RULE-FIN-013 applies here too</b> — this is the module's second reversal path, so it
     * runs the same single guard, {@code JournalEntryDomain.assertCanReverse(boolean)}, with the
     * already-reversed fact read off the posted row's {@code REVERSAL_ENTRY_ID}. A run that
     * somehow re-entered on an entry already carrying a reversal link is rejected with
     * {@code FIN-409-ALREADY-REVERSED} rather than posting a second mirror.
     *
     * <p>The next period is the one whose {@code periodNo} follows the posted entry's, within the
     * same fiscal year; an absent successor is the catalog's own {@code FIN-404-PERIOD}. Whether
     * that period is Open is RULE-FIN-008's decision, taken inside the shared pipeline.
     */
    private void postReversalInNextPeriod(JournalEntry posted, FiscalPeriod period) {
        JournalEntryDomain.from(posted)
            .assertCanReverse(posted.getReversalEntry() != null);

        FiscalPeriod nextPeriod = nextPeriodAfter(period);

        List<BuiltLine> mirrored = new ArrayList<>(posted.getLines().size());
        for (JournalLine line : posted.getLines()) {
            mirrored.add(new BuiltLine(line.getAccount(), line.getAmount(),
                JournalEntryDomain.reversalDirectionOf(line.getDirectionCode()), false,
                line.getDescriptionAr(), line.getDescriptionEn(), List.of()));
        }

        JournalEntry reversal = postingService.buildValidateAndPost(new PostingRequest(
            nextPeriod.getFiscalYear(), nextPeriod, nextPeriod.getStartDate(),
            JournalPostingService.JOURNAL_TYPE_REVERSAL, null,
            posted.getDescriptionAr(), posted.getDescriptionEn(), mirrored));

        reversal.linkOriginal(posted);
        posted.linkReversal(reversal);
        journalEntryRepository.save(posted);
        journalEntryRepository.save(reversal);
        log.info("Posted template reversal JournalEntry ID: {} for entry ID: {}",
            reversal.getJournalEntryPk(), posted.getJournalEntryPk());
    }

    /**
     * The period immediately after {@code period} inside the same fiscal year — a plain selection
     * over an already-loaded list, not a rule.
     */
    private FiscalPeriod nextPeriodAfter(FiscalPeriod period) {
        return fiscalPeriodRepository
            .findByFiscalYearId(period.getFiscalYear().getFiscalYearPk()).stream()
            .filter(candidate -> candidate.getPeriodNo() > period.getPeriodNo())
            .min(Comparator.comparing(FiscalPeriod::getPeriodNo))
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                FinErrorCodes.FIN_404_PERIOD, period.getPeriodNo() + 1));
    }

    /**
     * A template line's optional analytical tag (DBF-FIN-128, nullable) as a posting-line
     * dimension pair. RULE-FIN-009 then judges it inside the shared pipeline, exactly as it judges
     * a manually submitted tag.
     */
    private List<JournalEntryMapper.BuiltDimension> dimensionTagsOf(DimensionValue dimensionValue) {
        if (dimensionValue == null) {
            return List.of();
        }
        return List.of(new JournalEntryMapper.BuiltDimension(
            dimensionValue.getDimension(), dimensionValue));
    }

    /** FK resolution, not a rule: an unknown account id is {@code FIN-404-ACCOUNT}. */
    private Account resolveAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT, accountId));
    }

    /**
     * FK resolution for the optional dimension value (DBF-FIN-128, nullable). An id that resolves
     * to nothing is {@code FIN-409-INVALID-DIMENSION} — the catalog's own row for an invalid
     * dimension value.
     */
    private DimensionValue resolveDimensionValue(Long dimensionValueId) {
        if (dimensionValueId == null) {
            return null;
        }
        return dimensionValueRepository.findById(dimensionValueId)
            .orElseThrow(() -> new LocalizedException(
                Status.CONFLICT, FinErrorCodes.FIN_409_INVALID_DIMENSION, dimensionValueId));
    }

    /**
     * API-FIN-036 — soft deactivation only: retire a recurring/reversing template so it stops
     * being run. No SRS rule answers "may this template be deactivated?" — RULE-FIN-001..017 were
     * each read and none constrains retiring a template (the closest, RULE-FIN-005, is about an
     * event having no ACTIVE {@code EventTypeRule}, a different entity) — so there is nothing to
     * delegate before the mutation. That is the same shape as {@code AccountService.deactivate}
     * (API-FIN-004), {@code EventTypeRuleService.deactivate} (API-FIN-034) and
     * {@code DimensionValueService.deactivate} (API-FIN-035). ENT-FIN-011 does now have a Domain
     * companion ({@code RecurringTemplateDomain}), but it owns the run-time gate only — it holds
     * no rule about entering the deactivated state. The flag moves through the entity's own
     * {@code deactivate()} helper, never a direct assignment.
     *
     * <p>Why this endpoint exists: srs-fin.md SCR-REQ-FIN-004 §B4 records the absence of a
     * template deactivate as an OPEN DEFECT rather than a scope decision — {@code IS_ACTIVE_FL} is
     * NOT NULL, {@code activate()}/{@code deactivate()} shipped with zero callers, B2 advertises an
     * {@code isActiveFl(EXACT)} search filter over a column nothing could set to FALSE, and
     * AC-FIN-023 is written "Given an active recurring template", presupposing a state nothing
     * could produce. A template created with a wrong account or amount could be neither retired nor
     * corrected, so every subsequent API-FIN-014 run posted a wrong entry recoverable only by
     * reversing each one individually.
     *
     * <p><b>This deactivate now gates the run</b>, by RECORDED HUMAN DECISION — it was not one
     * when API-FIN-036 shipped. {@link #run(Long)} calls
     * {@code RecurringTemplateDomain.assertCanRun()} on the row it loads, so a deactivated
     * template is refused with {@code FIN-409-NOT-ACTIVE} (HTTP 409) and posts nothing. No
     * RULE-FIN-* states this gate — RULE-FIN-001..017 were each read and none constrains running
     * a retired template, and AC-FIN-023 states an outcome only for the active case — so it rests
     * on that decision, not on a rule that was always there. Deactivation therefore remains
     * unguarded on the way IN (nothing to delegate before the mutation, the same shape as
     * {@code AccountService.deactivate}) while being enforced on the way OUT, at run time.
     * The flag also still drives API-FIN-012's advertised {@code isActiveFl} filter.
     *
     * <p>The response carries the template's lines because {@code RecurringTemplateResponse}
     * derives its {@code lineCount} from the list it is handed; passing an empty list would
     * misreport the aggregate as having none. Loading them is orchestration (load → map), not a
     * rule.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_RECURRING_TEMPLATES_UPDATE)")
    public ServiceResult<RecurringTemplateResponse> deactivate(Long id) {
        log.info("Deactivating RecurringTemplate ID: {}", id);

        RecurringTemplate entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_TEMPLATE, id));

        entity.deactivate();

        RecurringTemplate saved = repository.save(entity);
        log.info("Deactivated RecurringTemplate ID: {}", saved.getRecurringTemplatePk());

        List<RecurringTemplateLineResponse> lineResponses =
            lineRepository.findByRecurringTemplatePk(saved.getRecurringTemplatePk()).stream()
                .map(lineMapper::toResponse)
                .toList();

        return ServiceResult.success(
            mapper.toResponse(saved, lineResponses), Status.UPDATED);
    }

    /**
     * API-FIN-012 — QR-FIN-017, criteria search over ENT-FIN-011, read-only.
     *
     * <p>{@code RecurringTemplateResponse} reports a {@code lineCount}, so the page's lines are
     * fetched in ONE batch read keyed by the page's ids
     * ({@code findByRecurringTemplatePkIn}) and grouped in memory — not one child query per row.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_RECURRING_TEMPLATES_VIEW)")
    public ServiceResult<Page<RecurringTemplateResponse>> search(
            RecurringTemplateSearchRequest searchRequest) {
        log.debug("Searching RecurringTemplate");

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<RecurringTemplate> spec = SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(DATE_FILTER_FIELDS));
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<RecurringTemplate> page = repository.findAll(spec, pageable);
        List<Long> templatePks = page.getContent().stream()
            .map(RecurringTemplate::getRecurringTemplatePk)
            .toList();

        Map<Long, List<RecurringTemplateLineResponse>> linesByTemplate = templatePks.isEmpty()
            ? Map.of()
            : lineRepository.findByRecurringTemplatePkIn(templatePks).stream()
                .collect(Collectors.groupingBy(
                    line -> line.getRecurringTemplate().getRecurringTemplatePk(),
                    Collectors.mapping(lineMapper::toResponse, Collectors.toList())));

        return ServiceResult.success(page.map(template -> mapper.toResponse(template,
            linesByTemplate.getOrDefault(template.getRecurringTemplatePk(), List.of()))));
    }
}
