package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.fin.domain.AccountDomain;
import com.erp.fin.domain.DimensionValueDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.JournalEntryCreateRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalEntrySearchRequest;
import com.erp.fin.dto.JournalLineCreateRequest;
import com.erp.fin.dto.JournalLineDimensionCreateRequest;
import com.erp.fin.dto.JournalLineResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.Dimension;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.FiscalYear;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.JournalLine;
import com.erp.fin.entity.JournalLineDimension;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.JournalEntryMapper;
import com.erp.fin.mapper.JournalEntryMapper.ResolvedDimension;
import com.erp.fin.mapper.JournalEntryMapper.ResolvedLine;
import com.erp.fin.numbering.JournalDocNoGenerator;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.DimensionRepository;
import com.erp.fin.repository.DimensionValueRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.repository.JournalEntryRepository;
import com.erp.fin.repository.JournalLineDimensionRepository;
import com.erp.fin.repository.JournalLineRepository;
import com.erp.fin.service.JournalPostingService.PostingRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for the ENT-FIN-004 posting aggregate — API-FIN-019, create a manual entry
 * (SVC-API-CRUD.md), and API-FIN-021, reverse a posted entry (SVC-API-INT.md). API-FIN-020's event
 * build has its own service ({@code EventEntryService}, matching SVC-API-INT.md's own
 * {@code Layers:} line); API-FIN-022 (read) and API-FIN-018 (search) belong to SVC-API-SEARCH and
 * are deliberately absent here.
 *
 * <p><b>RULE-FIN-016.</b> There is no update and no delete path on this service, and none may be
 * added: the rule is enforced structurally, by omission — {@code JournalEntryRepository} declares
 * no {@code @Modifying} query and no derived {@code deleteBy...}, and FIN's API registry declares
 * no PUT and no DELETE on {@code /journal-entries}.
 *
 * <p>No caching annotations — gov-enforce-caching-rules bars caching financial or transactional
 * records outright, and FIN's approved register is empty. SVC-API-SEARCH added API-FIN-018
 * (search entries) and API-FIN-022 (read one entry with its lines) here, with the A.5.6
 * {@code ALLOWED_SORT_FIELDS} whitelist the search requires — both read-only, neither of them a
 * mutation path, so RULE-FIN-016 is untouched.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalEntryService {

    /**
     * A.5.6 — API-FIN-018's filter/sort whitelist: the plan's {@code docNo} (LIKE),
     * {@code docDate} (DATE_RANGE), {@code statusCode} and {@code journalTypeCode} filters, plus
     * ENT-FIN-004's remaining flat columns for ordering. {@code periodId} is absent on purpose —
     * it is an association path, ANDed in as an explicit join below.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "journalEntryPk", "docNo", "docDate", "journalTypeCode", "statusCode",
        "eventReference", "postedAt", "createdAt");

    /** The one {@code DATE} filter field API-FIN-018 declares; see {@code FinSearchSupport}. */
    private static final Set<String> DATE_FILTER_FIELDS = Set.of("docDate");

    private final JournalEntryRepository repository;
    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AccountRepository accountRepository;
    private final DimensionRepository dimensionRepository;
    private final DimensionValueRepository dimensionValueRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalLineDimensionRepository journalLineDimensionRepository;
    private final JournalEntryMapper mapper;
    private final FinLookupValidationService lookupValidation;
    private final JournalPostingService postingService;

    /**
     * API-FIN-019 — generate {@code docNo} → build the DRAFT (QR-FIN-024) → validate
     * (QR-FIN-029..032) → post (QR-FIN-033), all inside ONE transaction, exactly as CORE.md's
     * transaction-scope paragraph and REQ-FIN-015 require.
     *
     * <p><b>Atomicity.</b> The whole aggregate — header, lines and their dimension tags — is
     * assembled in memory and every guard runs against that in-memory graph; the single
     * {@code repository.save(...)} happens only after {@code JournalEntry.post(...)} has flipped
     * the status. Nothing is written before validation succeeds, so no partially-built DRAFT can
     * survive a failure even in principle, and the surrounding {@code @Transactional} rolls the
     * whole attempt back on any {@code LocalizedException} regardless.
     *
     * <p><b>Every failure returned together (REQ-FIN-015 / AC-FIN-015).</b> Each rule guard is
     * evaluated through its domain class's non-throwing sibling, which returns an
     * {@link ErrorDetail} (registered code + message arguments) instead of throwing; the service
     * only collects what comes back and, if anything did, throws ONCE with the whole list via the
     * multi-error {@code LocalizedException(Status, List<ErrorDetail>)}. The response then carries
     * the first failure as {@code ApiError.code}/{@code message} and every failure — that one
     * included — in {@code ApiError.fieldErrors}. Each sibling is the rule's single
     * implementation; the matching {@code assert*} method delegates to it, so every other caller
     * (event build, recurring, allocation, reversal) keeps the unchanged fail-fast behaviour.
     * Not-found preconditions (fiscal year, period, account, dimension, dimension value) and
     * cross-module lookup validation stay fail-fast: an unresolvable reference leaves nothing to
     * validate.
     *
     * <p><b>Rules delegated, none inlined</b> (A.5.18) — the service only fetches facts, passes
     * them in and collects the answers; it contains no business condition of its own:
     * <ul>
     *   <li>RULE-FIN-007 (QR-FIN-030) → {@code AccountDomain.checkPostable()}, once per line;</li>
     *   <li>RULE-FIN-009 (QR-FIN-032) →
     *       {@code DimensionValueDomain.checkUsableOnLine(statedDimensionPk)}, once per tag;</li>
     *   <li>RULE-FIN-004 → {@code JournalEntryDomain.create(...)} — a manual entry carries no
     *       event reference, so it passes {@code null} / {@code false};</li>
     *   <li>RULE-FIN-006 (QR-FIN-029) → {@code JournalEntryDomain.checkBalanced(lines)};</li>
     *   <li>RULE-FIN-008 (QR-FIN-031) →
     *       {@code JournalEntryDomain.checkTargetPeriodOpen(statusCode)};</li>
     *   <li>RULE-FIN-017 → {@code JournalEntryDomain.assertHeaderCoherent(...)} — fail-fast, since
     *       an incoherent {@code fiscalYearId}/{@code periodId}/{@code docDate} triple makes the
     *       period gate itself meaningless. This is the only path that needs it: every
     *       system-generated entry derives the three facts from one another.</li>
     * </ul>
     *
     * <p><b>Numbering under concurrency (ALIGN-BE).</b> The fiscal year is loaded through
     * {@code FiscalYearRepository.lockForDocNoAllocation(...)} rather than {@code findById}, so the
     * {@code docNo} series is allocated under a row lock held to commit — see that method's own
     * javadoc for why a lock, and not a sequence or a retry.
     *
     * <p><b>Lookup validation.</b> {@code journalTypeCode} and every line's {@code directionCode}
     * are XM-FIN-001 columns in the DB Alignment Manifest, and CORE.md's "Lookup values"
     * paragraph requires every lookup-backed column to be validated against MDL before any write;
     * they are therefore checked here even though API-FIN-019's own Errors line enumerates only
     * its four rule codes. {@code statusCode} is system-assigned and never submitted, so it needs
     * no check.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_JOURNAL_ENTRIES_CREATE)")
    public ServiceResult<JournalEntryResponse> createManual(JournalEntryCreateRequest request) {
        log.info("Creating manual JournalEntry in fiscal year ID: {} period ID: {}",
            request.getFiscalYearId(), request.getPeriodId());

        FiscalYear fiscalYear =
            fiscalYearRepository.lockForDocNoAllocation(request.getFiscalYearId())
                .orElseThrow(() -> new LocalizedException(
                    Status.NOT_FOUND, FinErrorCodes.FIN_404_YEAR, request.getFiscalYearId()));
        FiscalPeriod period = fiscalPeriodRepository.findById(request.getPeriodId())
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_PERIOD, request.getPeriodId()));

        JournalEntryDomain.assertHeaderCoherent(fiscalYear.getFiscalYearPk(),
            period.getFiscalYear() == null ? null : period.getFiscalYear().getFiscalYearPk(),
            request.getDocDate(), period.getStartDate(), period.getEndDate());

        lookupValidation.assertValidCode(
            FinLookupValidationService.KEY_JOURNAL_TYPE, request.getJournalTypeCode());

        List<ErrorDetail> failures = new ArrayList<>();

        List<ResolvedLine> resolvedLines = resolveLines(request.getLines(), failures);

        String docNo = JournalDocNoGenerator.next(
            fiscalYear.getCode(), highestDocNoOf(fiscalYear.getFiscalYearPk()));

        JournalEntry entry = mapper.toEntity(request, docNo, fiscalYear, period, resolvedLines);

        JournalEntryDomain entryDomain = JournalEntryDomain.create(null, false);
        entryDomain.checkBalanced(entry.getLines()).ifPresent(failures::add);
        entryDomain.checkTargetPeriodOpen(period.getStatusCode()).ifPresent(failures::add);

        if (!failures.isEmpty()) {
            throw new LocalizedException(Status.CONFLICT, failures);
        }

        entry.post(Instant.now());

        JournalEntry saved = repository.save(entry);
        log.info("Posted JournalEntry ID: {} docNo: {}", saved.getJournalEntryPk(),
            saved.getDocNo());

        List<JournalLineResponse> lineResponses =
            saved.getLines().stream().map(mapper::toLineResponse).toList();

        return ServiceResult.success(mapper.toResponse(saved, lineResponses), Status.CREATED);
    }

    /**
     * API-FIN-021 — load the original → check RULE-FIN-013 (QR-FIN-035) → resolve the posting
     * period per RULE-FIN-012 (QR-FIN-036) → build the mirrored lines per RULE-FIN-011
     * (QR-FIN-034) → validate and post (QR-FIN-029..033, journalTypeCode=REVERSAL) → set
     * {@code originalEntryId}/{@code reversalEntryId} on both rows → return the reversal.
     *
     * <p><b>Atomicity.</b> One transaction covers the reversal's whole build→validate→post
     * sequence AND the write-back to the original: a reversal that posted while its original
     * stayed POSTED and unlinked would break RULE-FIN-011's "linked" half and let the entry be
     * reversed twice. {@link JournalPostingService} joins this transaction rather than opening its
     * own, so CORE.md's "ONE transaction per entry" holds.
     *
     * <p><b>Classic reversal.</b> The original stays POSTED; the mirror reversal is posted against
     * it and the two rows are linked in both directions. The net effect on every account balance
     * is therefore zero — both entries are POSTED, so both are seen by the report queries, and
     * each reversal line carries the opposite direction of its original. Voiding the original
     * instead would remove it from every POSTED-only report and leave the mirror standing alone,
     * i.e. {@code −(original)} rather than zero.
     *
     * <p><b>RULE-FIN-016 needs no carve-out here.</b> The original's accounting state is untouched:
     * {@code JournalEntry.linkReversal(...)} writes only {@code REVERSAL_ENTRY_ID} (DBF-FIN-043),
     * never {@code statusCode}. See {@code JournalEntryDomain}'s class javadoc.
     *
     * <p><b>Fail-fast.</b> API-FIN-021's Errors line names exactly two codes,
     * {@code FIN-409-NOT-POSTED} and {@code FIN-404-ENTRY}, and both are preconditions that leave
     * nothing further to validate: an entry that cannot be found or is not POSTED has no line set
     * worth checking. The four post-time rules on the newly built reversal are aggregated by the
     * shared pipeline, as on every other entry-creating path.
     *
     * <p><b>Rules delegated, none inlined</b> (A.5.18): RULE-FIN-013 →
     * {@code JournalEntryDomain.assertCanReverse()}; RULE-FIN-012 →
     * {@code JournalEntryDomain.resolveReversalPeriodPk(...)}, which also owns what an absent open
     * period means; RULE-FIN-011's direction mirroring →
     * {@code JournalEntryDomain.reversalDirectionOf(...)}; the link itself is plain state
     * mutation on the entities.
     *
     * <p><b>Concurrency (ALIGN-BE).</b> The original is locked with
     * {@code JournalEntryRepository.lockForReversal(id)} before RULE-FIN-013 is evaluated, so the
     * "reversed only once" half of the rule is read and written atomically: a second concurrent
     * reversal blocks, then sees the committed {@code REVERSAL_ENTRY_ID} and is rejected with
     * {@code FIN-409-ALREADY-REVERSED}, the same answer a sequential repeat gets. The lines are
     * loaded afterwards through the existing fetch query, which returns the same managed instance.
     *
     * <p><b>Lookup validation.</b> Nothing lookup-backed is submitted: {@code journalTypeCode} is
     * the system-assigned SRS A6 value {@code REVERSAL}, and every mirrored {@code directionCode}
     * is derived from a code already validated against MDL when the original was written
     * (XM-FIN-001).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_JOURNAL_ENTRIES_REVERSE)")
    public ServiceResult<JournalEntryResponse> reverse(Long id) {
        log.info("Reversing JournalEntry ID: {}", id);

        repository.lockForReversal(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ENTRY, id));
        JournalEntry original = repository.findOneWithLines(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ENTRY, id));
        JournalEntryDomain.from(original)
            .assertCanReverse(original.getReversalEntry() != null);

        FiscalPeriod originalPeriod = original.getPeriod();
        FiscalPeriod currentOpenPeriod = postingService.findCurrentOpenPeriod();
        Long targetPeriodPk = JournalEntryDomain.resolveReversalPeriodPk(
            originalPeriod.getFiscalPeriodPk(), originalPeriod.getStatusCode(),
            currentOpenPeriod == null ? null : currentOpenPeriod.getFiscalPeriodPk());
        FiscalPeriod targetPeriod =
            targetPeriodPk.equals(originalPeriod.getFiscalPeriodPk())
                ? originalPeriod
                : currentOpenPeriod;

        JournalEntry reversal = postingService.buildValidateAndPost(new PostingRequest(
            targetPeriod.getFiscalYear(), targetPeriod, LocalDate.now(),
            JournalPostingService.JOURNAL_TYPE_REVERSAL, null,
            original.getDescriptionAr(), original.getDescriptionEn(),
            mirroredLines(original)));

        reversal.linkOriginal(original);
        original.linkReversal(reversal);
        repository.save(original);
        JournalEntry linkedReversal = repository.save(reversal);
        log.info("Posted reversal JournalEntry ID: {} for original ID: {}",
            linkedReversal.getJournalEntryPk(), original.getJournalEntryPk());

        List<JournalLineResponse> lineResponses =
            linkedReversal.getLines().stream().map(mapper::toLineResponse).toList();

        return ServiceResult.success(
            mapper.toResponse(linkedReversal, lineResponses), Status.CREATED);
    }

    /**
     * RULE-FIN-011 / QR-FIN-034 — the original's lines mirrored: same account, same amount, the
     * opposite direction, and the same analytical dimension tags, so the reversal cancels the
     * original in every report dimension. Pure assembly — the one decision, each line's opposite
     * side, is {@code JournalEntryDomain.reversalDirectionOf(...)}.
     */
    private List<JournalEntryMapper.BuiltLine> mirroredLines(JournalEntry original) {
        Map<Long, List<JournalEntryMapper.BuiltDimension>> tagsByLine = new HashMap<>();
        for (JournalLineDimension tag : journalLineDimensionRepository
                .findByJournalEntryPk(original.getJournalEntryPk())) {
            tagsByLine.computeIfAbsent(tag.getJournalLine().getJournalLinePk(),
                    key -> new ArrayList<>())
                .add(new JournalEntryMapper.BuiltDimension(
                    tag.getDimension(), tag.getDimensionValue()));
        }

        List<JournalLine> originalLines =
            journalLineRepository.findByJournalEntryPk(original.getJournalEntryPk());
        List<JournalEntryMapper.BuiltLine> mirrored = new ArrayList<>(originalLines.size());
        for (JournalLine line : originalLines) {
            mirrored.add(new JournalEntryMapper.BuiltLine(line.getAccount(), line.getAmount(),
                JournalEntryDomain.reversalDirectionOf(line.getDirectionCode()),
                Boolean.TRUE.equals(line.getIsRemainderFl()),
                line.getDescriptionAr(), line.getDescriptionEn(),
                tagsByLine.getOrDefault(line.getJournalLinePk(), List.of())));
        }
        return mirrored;
    }

    /**
     * Resolves and validates every submitted line: its account must exist
     * ({@code FIN-404-ACCOUNT}) and be postable (RULE-FIN-007, delegated), and each dimension tag
     * must resolve on both sides and pass RULE-FIN-009 (delegated). Pure fact-gathering — the two
     * decisions belong to {@link AccountDomain} and {@link DimensionValueDomain}.
     */
    private List<ResolvedLine> resolveLines(List<JournalLineCreateRequest> lineRequests,
                                            List<ErrorDetail> failures) {
        List<ResolvedLine> resolved = new ArrayList<>(lineRequests.size());
        for (JournalLineCreateRequest lineRequest : lineRequests) {
            lookupValidation.assertValidCode(
                FinLookupValidationService.KEY_DEBIT_CREDIT, lineRequest.getDirectionCode());

            Account account = accountRepository.findById(lineRequest.getAccountId())
                .orElseThrow(() -> new LocalizedException(
                    Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT, lineRequest.getAccountId()));
            AccountDomain.from(account).checkPostable().ifPresent(failures::add);

            resolved.add(new ResolvedLine(lineRequest, account,
                resolveDimensions(lineRequest.getDimensions(), failures)));
        }
        return resolved;
    }

    private List<ResolvedDimension> resolveDimensions(
            List<JournalLineDimensionCreateRequest> dimensionRequests,
            List<ErrorDetail> failures) {
        if (dimensionRequests == null) {
            return List.of();
        }
        List<ResolvedDimension> resolved = new ArrayList<>(dimensionRequests.size());
        for (JournalLineDimensionCreateRequest dimensionRequest : dimensionRequests) {
            Dimension dimension = dimensionRepository.findById(dimensionRequest.getDimensionId())
                .orElseThrow(() -> new LocalizedException(Status.CONFLICT,
                    FinErrorCodes.FIN_409_INVALID_DIMENSION, dimensionRequest.getDimensionId()));
            DimensionValue dimensionValue =
                dimensionValueRepository.findById(dimensionRequest.getDimensionValueId())
                    .orElseThrow(() -> new LocalizedException(Status.CONFLICT,
                        FinErrorCodes.FIN_409_INVALID_DIMENSION,
                        dimensionRequest.getDimensionValueId()));

            DimensionValueDomain.from(dimensionValue)
                .checkUsableOnLine(dimension.getDimensionPk())
                .ifPresent(failures::add);

            resolved.add(new ResolvedDimension(dimensionRequest, dimension, dimensionValue));
        }
        return resolved;
    }

    /**
     * The fiscal year's greatest committed {@code docNo}, or {@code null} when it has no entry
     * yet — the one fact {@link JournalDocNoGenerator} needs.
     *
     * <p>Read through the existing repository layer with no new query method: an explicit
     * {@code Specification} scopes the read to the fiscal year (the same A.5.17-sanctioned
     * technique a child search uses, since {@code SpecBuilder}'s flat {@code root.get(field)}
     * cannot express the nested {@code fiscalYear.fiscalYearPk} path), and the fluent
     * {@code findBy(..., first())} fetches exactly one row rather than paging by hand. Ordering by
     * {@code docNo} descending is numerically correct inside one fiscal year because every value
     * shares the {@code JV-{code}-} prefix and a fixed-width, zero-padded counter.
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
     * API-FIN-018 — QR-FIN-023, criteria search over ENT-FIN-004, read-only. REQ-FIN-027 requires
     * the matching entries "unmodified": nothing here filters by status, hides a reversed entry or
     * rewrites a field — the caller decides what to look at, through {@code statusCode}.
     *
     * <p>{@code periodId} (DBF-FIN-038) is the association {@code period}, a nested path
     * {@code SpecBuilder} cannot resolve, so it becomes an explicit {@code Specification} join
     * when supplied (A.5.17's mechanism). {@code docDate} bounds arrive as JSON strings and are
     * coerced by {@code FinSearchSupport.temporalFieldConverter}.
     *
     * <p>Rows map through {@code toSummaryResponse}: header detail plus the {@code @Formula} line
     * count, no nested line sets. API-FIN-022 is the endpoint that returns those.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_JOURNAL_ENTRIES_VIEW)")
    public ServiceResult<Page<JournalEntryResponse>> search(
            JournalEntrySearchRequest searchRequest) {
        log.debug("Searching JournalEntry");

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<JournalEntry> spec = SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(
                DATE_FILTER_FIELDS, Set.of("createdAt", "updatedAt", "postedAt")));

        Long periodId = searchRequest.getPeriodId();
        if (periodId != null) {
            Specification<JournalEntry> periodSpec = (root, query, cb) ->
                cb.equal(root.get("period").get("fiscalPeriodPk"), periodId);
            spec = periodSpec.and(spec);
        }

        Long fiscalYearId = searchRequest.getFiscalYearId();
        if (fiscalYearId != null) {
            Specification<JournalEntry> yearSpec = (root, query, cb) ->
                cb.equal(root.get("fiscalYear").get("fiscalYearPk"), fiscalYearId);
            spec = yearSpec.and(spec);
        }

        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        return ServiceResult.success(
            repository.findAll(spec, pageable).map(mapper::toSummaryResponse));
    }

    /**
     * API-FIN-022 — QR-FIN-037, one entry with its nested lines and each line's analytical
     * dimensions (REQ-FIN-016, REQ-FIN-027). Intra-module join only: entry → line → line-dim.
     * Unknown id is {@code FIN-404-ENTRY}, the single code the plan's Errors line names.
     *
     * <p>Returned unmodified, whatever its status: a DRAFT entry and a POSTED one — reversed or
     * not — all read the same way, and the {@code originalEntryId}/{@code reversalEntryId} links
     * plus
     * {@code eventReference} are what REQ-FIN-046's drill-down follows out of a report row.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_JOURNAL_ENTRIES_VIEW)")
    public ServiceResult<JournalEntryResponse> read(Long id) {
        log.debug("Reading JournalEntry ID: {}", id);

        JournalEntry entry = repository.findOneWithLines(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ENTRY, id));

        List<JournalLineResponse> lineResponses = journalLineRepository
            .findByJournalEntryPk(entry.getJournalEntryPk()).stream()
            .map(mapper::toLineResponse)
            .toList();

        return ServiceResult.success(mapper.toResponse(entry, lineResponses));
    }
}
