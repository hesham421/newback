package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.domain.AccountDomain;
import com.erp.fin.domain.FiscalPeriodDomain;
import com.erp.fin.domain.FiscalYearDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.FiscalPeriodResponse;
import com.erp.fin.dto.FiscalYearCreateRequest;
import com.erp.fin.dto.FiscalYearResponse;
import com.erp.fin.dto.FiscalYearSearchRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineResponse;
import com.erp.fin.dto.YearEndCloseResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.FiscalYear;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.JournalLine;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.FiscalPeriodMapper;
import com.erp.fin.mapper.FiscalYearMapper;
import com.erp.fin.mapper.JournalEntryMapper;
import com.erp.fin.mapper.JournalEntryMapper.BuiltLine;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.repository.JournalLineRepository;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.fin.service.JournalPostingService.PostingRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Orchestration layer for ENT-FIN-007 — API-FIN-023 (create a fiscal year with its periods,
 * REQ-FIN-031) and API-FIN-027 (run year-end close, REQ-FIN-036), SVC-API-INT.md.
 *
 * <p>ENT-FIN-007 carries exactly one decision of its own — the year-end rerun guard on its
 * {@code statusCode} — and {@link FiscalYearDomain} holds it (added by ALIGN-BE; see that class
 * for why DATA-DOM-MASTER.md's "DOMAIN RULES: none scoped alone" no longer covers the entity).
 * Every other decision the two APIs below take belongs to an existing Domain class —
 * {@link FiscalPeriodDomain} for the close preconditions (RULE-FIN-015 is not among them — it
 * is the distinct-permission {@code @PreAuthorize} gate, see {@link #yearEndClose(Long)}),
 * {@link JournalEntryDomain} and {@link AccountDomain} for the closing/opening derivations — and
 * none is inlined here (A.5.18). {@code FIN-409-YEAR-DUP} is the one exception, and is not a
 * business rule: it is a PLATFORM-STD duplicate-key guard of exactly the shape
 * gov-enforce-error-handling's service-pattern checklist prescribes, placed the same way
 * {@code DimensionService} places {@code FIN-409-DIMENSION-DUP}.
 *
 * <p>No caching annotations — FIN's approved cache register is empty, and an accounting record is
 * never cacheable regardless.
 *
 * <p>{@code ALLOWED_SORT_FIELDS} arrived with the fiscal-year search added 2026-09-19 — see
 * {@link #search(FiscalYearSearchRequest)} for why a year that could be created but never listed
 * blocked three approved screens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalYearService {

    /**
     * A.5.6 — the fiscal-year search's filter/sort whitelist: ENT-FIN-007's flat columns. The
     * generated period set is not among them; a period is filtered through the fiscal-period
     * search, scoped by its own {@code fiscalYearId} filter.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "fiscalYearPk", "code", "startDate", "endDate", "statusCode", "isActiveFl", "createdAt");

    /** DBF-FIN-068/069 — {@code DATE} columns, so a JSON string bound needs coercing. */
    private static final Set<String> DATE_FILTER_FIELDS = Set.of("startDate", "endDate");

    private final FiscalYearRepository repository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;
    private final FiscalYearMapper mapper;
    private final FiscalPeriodMapper periodMapper;
    private final JournalEntryMapper journalEntryMapper;
    private final JournalPostingService postingService;

    /**
     * API-FIN-023 — validate code uniqueness → persist the year (statusCode=OPEN by its entity
     * default) together with {@code periodCount} periods, each OPEN (QR-FIN-038) → return.
     *
     * <p>Year and periods are written in ONE transaction: {@code FiscalYear} declares
     * {@code cascade = ALL} over its {@code periods} composition, so the single {@code save}
     * writes the whole set, and a failure anywhere leaves neither behind.
     *
     * <p>Fail-fast: uniqueness is the only check, so there is nothing to aggregate.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_PERIODS_CREATE)")
    public ServiceResult<FiscalYearResponse> create(FiscalYearCreateRequest request) {
        log.info("Creating FiscalYear with code: {} and {} period(s)",
            request.getCode(), request.getPeriodCount());

        if (repository.existsByCode(request.getCode())) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FinErrorCodes.FIN_409_YEAR_DUP, request.getCode());
        }

        FiscalYear fiscalYear = mapper.toEntity(request);
        for (int periodNo = 1; periodNo <= request.getPeriodCount(); periodNo++) {
            fiscalYear.getPeriods().add(
                mapper.toPeriodEntity(fiscalYear, periodNo, request.getPeriodCount()));
        }

        FiscalYear saved = repository.save(fiscalYear);
        log.info("Created FiscalYear ID: {} with {} period(s)",
            saved.getFiscalYearPk(), saved.getPeriods().size());

        List<FiscalPeriodResponse> periodResponses =
            saved.getPeriods().stream().map(periodMapper::toResponse).toList();

        return ServiceResult.success(mapper.toResponse(saved, periodResponses), Status.CREATED);
    }

    /**
     * API-FIN-027 — <b>request: the path {@code id} only</b>, exactly as SVC-API-INT.md states.
     * Both facts that were briefly taken from a request body are now derived by the system: the
     * Retained Earnings account from {@code IS_RETAINED_EARNINGS_FL} (DBF-FIN-147, see
     * {@link #retainedEarningsAccount()}) and the successor fiscal year by date adjacency (see
     * {@link #successorOf(FiscalYear)}). Verify every period is Hard Closed → compute the year's
     * closing balances
     * (QR-FIN-041) → build and post the closing entry (result accounts → Retained Earnings,
     * journalTypeCode=CLOSING) → build and post the next year's opening entry from the resulting
     * balance-sheet balances (journalTypeCode=OPENING, POL-FIN-010) → mark the year CLOSED and
     * every period YEAR_END_CLOSE → return both entries.
     *
     * <p><b>Atomic unit: the whole run.</b> REQ-FIN-036 states the outcome as a single act — "a
     * balanced closing entry <i>and</i> a balanced opening entry" — and POL-FIN-010's continuity
     * guarantee is only meaningful if both exist: a committed closing entry without its opening
     * entry would leave the next year permanently short of its carried-forward balances, and a
     * year marked CLOSED without them would be unrecoverable (RULE-FIN-016 forbids editing a
     * posted entry, RULE-FIN-014 forbids reopening the periods). One {@code @Transactional}
     * therefore spans both entries and both status changes; {@link JournalPostingService} joins it
     * rather than opening its own, so CORE.md's "ONE transaction per entry" still holds for each
     * entry individually and the run is all-or-nothing on top of that.
     *
     * <p><b>Fail-fast, per period, and IN THAT ORDER (ALIGN-BE).</b> The §10.4
     * all-periods-Hard-Closed precondition (409) is fail-fast and leaves nothing further to
     * validate. The body runs the year's own rerun guard, then the period states, <i>before</i>
     * the successor-year and Retained-Earnings lookups. Previously those two lookups ran first,
     * so a deployment missing either answered {@code FIN-404-YEAR} / {@code FIN-404-ACCOUNT}
     * where this javadoc and the plan promise 409, which would have made every test assertion on
     * this endpoint wrong whichever way it was written. The two generated entries' own post-time
     * rules are aggregated by the shared pipeline as everywhere else.
     *
     * <p><b>RULE-FIN-015 is enforced by the {@code @PreAuthorize} below and by nothing else in
     * this method.</b> The rule (srs-fin.md:1026-1030) requires only that the close-approval
     * action be gated by a permission distinct from the journal-entry-creation permission,
     * "enforced through the Security module", and its {@code Data source} line records that FIN
     * has no field to read for it. {@code PERM_FIN_PERIODS_CLOSE_APPROVE} is that distinct
     * permission, so the gate IS the enforcement. <b>Do not add a SoD check back into this
     * body.</b> A previous implementation resolved facts from SEC's user directory and looped
     * over the periods refusing the close whenever ANY user in the system held both permissions
     * — a global user-set disjointness the SRS never asks for. It was removed by an explicit
     * human decision, together with {@code FinSeparationOfDutiesService} and FIN's XM-FIN-002
     * dependency on SEC.
     *
     * <p><b>Rerun guard (ALIGN-BE).</b> {@code FiscalYearDomain.assertCanYearEndClose()} rejects a
     * second run on an already-CLOSED year with {@code FIN-409-INVALID-TRANSITION}, and sits
     * before the period check so that it — and not the incidental
     * {@code FIN-409-PERIODS-NOT-CLOSED} the first run's own YEAR_END_CLOSE transitions would
     * raise — is what a rerun is told. The guard is now a stated rule on the year's state machine
     * rather than a side effect of the periods'.
     *
     * <p><b>Order matters.</b> The balance-sheet balances feeding the opening entry are read
     * <i>after</i> the closing entry has posted, so they already carry its Retained Earnings line.
     * That is what makes the opening entry balanced by construction: once the result accounts are
     * zeroed, the remaining accounts' signed nets sum to zero.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_PERIODS_CLOSE_APPROVE)")
    public ServiceResult<YearEndCloseResponse> yearEndClose(Long id) {
        log.info("Running year-end close for FiscalYear ID: {}", id);

        FiscalYear fiscalYear = lockOrThrow(id);

        List<FiscalPeriod> periods =
            fiscalPeriodRepository.findByFiscalYearId(fiscalYear.getFiscalYearPk());
        FiscalYearDomain.from(fiscalYear).assertCanYearEndClose();
        for (FiscalPeriod period : periods) {
            FiscalPeriodDomain.from(period).assertHardClosedForYearEnd();
        }

        FiscalYear nextFiscalYear = successorOf(fiscalYear);
        Account retainedEarnings = retainedEarningsAccount();

        JournalEntry closingEntry = postingService.buildValidateAndPost(new PostingRequest(
            fiscalYear, lastPeriodOf(periods), fiscalYear.getEndDate(),
            JournalPostingService.JOURNAL_TYPE_CLOSING, null, null, null,
            closingLines(fiscalYear.getFiscalYearPk(), retainedEarnings)));

        JournalEntry openingEntry = postingService.buildValidateAndPost(new PostingRequest(
            nextFiscalYear, firstPeriodOf(fiscalPeriodRepository
                .findByFiscalYearId(nextFiscalYear.getFiscalYearPk())),
            nextFiscalYear.getStartDate(), JournalPostingService.JOURNAL_TYPE_OPENING,
            null, null, null, openingLines(fiscalYear.getFiscalYearPk())));

        for (FiscalPeriod period : periods) {
            period.yearEndClose();
        }
        fiscalPeriodRepository.saveAll(periods);
        fiscalYear.close();
        repository.save(fiscalYear);
        log.info("Closed FiscalYear ID: {} with closing entry {} and opening entry {}",
            fiscalYear.getFiscalYearPk(), closingEntry.getDocNo(), openingEntry.getDocNo());

        return ServiceResult.success(YearEndCloseResponse.builder()
            .closingEntry(toEntryResponse(closingEntry))
            .openingEntry(toEntryResponse(openingEntry))
            .build(), Status.CREATED);
    }

    /**
     * QR-FIN-041 (closing half) — one line per result account carrying a non-zero balance, each in
     * the direction that zeroes it, plus the single Retained Earnings line that absorbs their net
     * (POL-FIN-010, "open result accounts at zero after closing to Retained Earnings").
     *
     * <p>Both decisions are delegated: {@code AccountDomain.isResultAccountType(...)} partitions
     * the accounts, and {@code JournalEntryDomain.directionOfNet(...)} /
     * {@code reversalDirectionOf(...)} turn each signed net into a posting side. The entry
     * balances by construction — the closing lines' own net is exactly the negative of the result
     * accounts' total net, which the Retained Earnings line restores.
     */
    private List<BuiltLine> closingLines(Long fiscalYearPk, Account retainedEarnings) {
        List<BuiltLine> lines = new ArrayList<>();
        BigDecimal resultTotal = BigDecimal.ZERO;

        for (Map.Entry<Long, AccountBalance> balance : balancesOf(fiscalYearPk).entrySet()) {
            AccountBalance accountBalance = balance.getValue();
            if (!AccountDomain.isResultAccountType(
                accountBalance.account().getAccountTypeCode())) {
                continue;
            }
            if (accountBalance.net().signum() == 0) {
                continue;
            }
            lines.add(new BuiltLine(accountBalance.account(), accountBalance.net().abs(),
                JournalEntryDomain.reversalDirectionOf(
                    JournalEntryDomain.directionOfNet(accountBalance.net())),
                false, null, null, List.of()));
            resultTotal = resultTotal.add(accountBalance.net());
        }

        if (resultTotal.signum() != 0) {
            lines.add(new BuiltLine(retainedEarnings, resultTotal.abs(),
                JournalEntryDomain.directionOfNet(resultTotal), false, null, null, List.of()));
        }
        return lines;
    }

    /**
     * QR-FIN-041 (opening half) — one line per balance-sheet account carrying a non-zero balance,
     * each reproducing that balance's own side in the next fiscal year (POL-FIN-010's continuity
     * guarantee). Read after the closing entry has posted, so the Retained Earnings account
     * already carries the closed result. Balanced by construction: with the result accounts zeroed,
     * the remaining signed nets sum to zero.
     */
    private List<BuiltLine> openingLines(Long fiscalYearPk) {
        List<BuiltLine> lines = new ArrayList<>();
        for (Map.Entry<Long, AccountBalance> balance : balancesOf(fiscalYearPk).entrySet()) {
            AccountBalance accountBalance = balance.getValue();
            if (AccountDomain.isResultAccountType(
                accountBalance.account().getAccountTypeCode())) {
                continue;
            }
            if (accountBalance.net().signum() == 0) {
                continue;
            }
            lines.add(new BuiltLine(accountBalance.account(), accountBalance.net().abs(),
                JournalEntryDomain.directionOfNet(accountBalance.net()), false, null, null,
                List.of()));
        }
        return lines;
    }

    /**
     * QR-FIN-041's fact-gathering half: every account's signed net (debits minus credits) over the
     * fiscal year's POSTED lines, live from {@code FIN_JOURNAL_LINE} — db-script-fin.md declares no
     * stored balance column anywhere, so a balance is only ever derived (POL-FIN-009/011).
     *
     * <p>Read through the existing repository layer with no new query method: an explicit
     * {@code Specification} expresses the nested {@code journalEntry.fiscalYear.fiscalYearPk} and
     * {@code journalEntry.statusCode} paths the shared {@code SpecBuilder}'s flat
     * {@code root.get(field)} cannot (the same A.5.17-sanctioned technique a child search uses).
     * A reversed entry stays POSTED (classic reversal, RULE-FIN-011), so it and its mirror
     * reversal are BOTH included here and cancel each other out — net zero, exactly as they
     * should.
     */
    private Map<Long, AccountBalance> balancesOf(Long fiscalYearPk) {
        Specification<JournalLine> postedInYear = (root, query, cb) -> cb.and(
            cb.equal(root.get("journalEntry").get("fiscalYear").get("fiscalYearPk"), fiscalYearPk),
            cb.equal(root.get("journalEntry").get("statusCode"), JournalEntry.STATUS_POSTED));

        Map<Long, AccountBalance> balances = new LinkedHashMap<>();
        for (JournalLine line : journalLineRepository.findAll(postedInYear)) {
            Account account = line.getAccount();
            BigDecimal signed = JournalEntryDomain.DIRECTION_DEBIT
                .equalsIgnoreCase(line.getDirectionCode())
                ? line.getAmount()
                : line.getAmount().negate();
            balances.merge(account.getAccountPk(), new AccountBalance(account, signed),
                (existing, added) -> new AccountBalance(existing.account(),
                    existing.net().add(added.net())));
        }
        return balances;
    }

    /**
     * The period the closing entry posts into — the year's last by {@code periodNo}. A plain
     * selection over an already-loaded list, not a rule.
     */
    private FiscalPeriod lastPeriodOf(List<FiscalPeriod> periods) {
        return periods.stream()
            .max(Comparator.comparing(FiscalPeriod::getPeriodNo))
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_PERIOD, 0));
    }

    /** The period the opening entry posts into — the next year's first by {@code periodNo}. */
    private FiscalPeriod firstPeriodOf(List<FiscalPeriod> periods) {
        return periods.stream()
            .min(Comparator.comparing(FiscalPeriod::getPeriodNo))
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_PERIOD, 0));
    }

    private JournalEntryResponse toEntryResponse(JournalEntry entry) {
        List<JournalLineResponse> lines =
            entry.getLines().stream().map(journalEntryMapper::toLineResponse).toList();
        return journalEntryMapper.toResponse(entry, lines);
    }

    /**
     * REQ-FIN-036 / POL-FIN-010 — the account the year's result closes into. Derived from
     * {@code FIN_ACCOUNT.IS_RETAINED_EARNINGS_FL} (DBF-FIN-147, added by
     * {@code V23__fin_account_retained_earnings_flag.sql}), never taken from the caller: the
     * request body that used to carry it has been removed together with the whole
     * {@code YearEndCloseRequest}, so no client can direct the year's result into an account of
     * its choosing.
     *
     * <p>A second marked account is impossible — {@code UQ_FIN_ACCOUNT_RETAINED_EARNINGS} is a
     * partial unique index over the marked rows — so the only reachable failure is "none marked",
     * which raises the catalog's existing {@code FIN-404-ACCOUNT}: the account genuinely cannot be
     * found. No new catalog row is invented for it.
     */
    private Account retainedEarningsAccount() {
        return accountRepository.findFirstByIsRetainedEarningsFlTrueOrderByAccountPkAsc()
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                FinErrorCodes.FIN_404_ACCOUNT, "IS_RETAINED_EARNINGS_FL"));
    }

    /**
     * The fiscal year that receives the opening entry — the year whose {@code startDate} is the
     * day after this year's {@code endDate}.
     *
     * <p><b>DERIVED DECISION, not a specified one.</b> {@code FIN_FISCAL_YEAR} declares no
     * successor link anywhere (DBF-FIN-065..074) and neither srs-fin.md nor SVC-API-INT.md states
     * how the next year is found; adjacency is the only derivation the schema supports, and it is
     * recorded as such in {@code execution-state.json} so it can be overruled by a human. An
     * absent successor raises the catalog's existing {@code FIN-404-YEAR} rather than guessing a
     * different year.
     */
    private FiscalYear successorOf(FiscalYear fiscalYear) {
        return repository.findByStartDate(fiscalYear.getEndDate().plusDays(1))
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                FinErrorCodes.FIN_404_YEAR, fiscalYear.getEndDate().plusDays(1)));
    }

    /**
     * API-FIN-027 — the year row under {@code PESSIMISTIC_WRITE} at the top of the transaction,
     * before the OPEN check, so two concurrent year-end closes serialize (ADR-FIN-041). The same
     * row the docNo allocation locks later, so the existing finder is reused. Not a rule: an
     * unknown id is {@code FIN-404-YEAR}.
     */
    private FiscalYear lockOrThrow(Long id) {
        return repository.lockForDocNoAllocation(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_YEAR, id));
    }

    /** One account and its derived signed net over a fiscal year's POSTED lines. */
    private record AccountBalance(Account account, BigDecimal net) {
    }

    /**
     * Criteria search over ENT-FIN-007 — no join, read-only, and no contract id yet (the factory
     * assigns those). Rows map through {@code toSummaryResponse}: the year's own columns plus its
     * {@code @Formula} period count, no nested period set.
     *
     * <p>Gated on {@code PERM_FIN_PERIODS_VIEW}, the FIN_PERIODS screen's VIEW action — the same
     * permission the fiscal-period search already carries, because this exposes no field that
     * search's rows do not already reach through their {@code fiscalYearId}. V24's header calls
     * that permission the one ✓ cell in SEC-BE.md's matrix with no API-FIN beside it and notes
     * that "either the matrix's FIN_PERIODS/VIEW ✓ is spurious, or a FIN_PERIODS read endpoint is
     * missing"; this is that read endpoint, so the row is no longer a gateway-only grant.
     *
     * <p>An empty result is a success, per CORE.md's search contract.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_PERIODS_VIEW)")
    public ServiceResult<Page<FiscalYearResponse>> search(FiscalYearSearchRequest searchRequest) {
        log.debug("Searching FiscalYear");

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<FiscalYear> spec = SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(DATE_FILTER_FIELDS));
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        return ServiceResult.success(
            repository.findAll(spec, pageable).map(mapper::toSummaryResponse));
    }
}
