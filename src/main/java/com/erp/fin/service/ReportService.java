package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.AccountBalanceGroupResponse;
import com.erp.fin.dto.AccountBalanceRowResponse;
import com.erp.fin.dto.AccountLedgerResponse;
import com.erp.fin.dto.BalanceSheetResponse;
import com.erp.fin.dto.DimensionReportResponse;
import com.erp.fin.dto.DimensionReportRowResponse;
import com.erp.fin.dto.IncomeStatementResponse;
import com.erp.fin.dto.TrialBalanceResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.ReportMapper;
import com.erp.fin.repository.AccountBalanceView;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.DimensionRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.repository.JournalLineDimensionRepository;
import com.erp.fin.repository.JournalLineRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for FIN's five reports — API-FIN-028 … API-FIN-032 (SVC-API-SEARCH.md),
 * REQ-FIN-039 … REQ-FIN-043 and REQ-FIN-046.
 *
 * <p><b>Every figure is derived live from POSTED lines.</b> There is no stored balance column
 * anywhere in db-script-fin.md and none may be introduced (POL-FIN-009, and the plan's ACCOUNTING
 * §12 point 9). The POSTED gate lives in the three report queries themselves — QR-FIN-042,
 * QR-FIN-043 and QR-FIN-044 all pin {@code statusCode} to
 * {@link JournalEntry#STATUS_POSTED} — so a DRAFT entry is invisible to every report. A reversed
 * entry stays POSTED (classic reversal, RULE-FIN-011) and is deliberately still counted: its
 * mirror reversal is POSTED too and carries the opposite direction on every line, so the pair
 * contributes zero while both stay visible in the ledger.
 *
 * <p><b>One aggregation, three statements.</b> QR-FIN-043 is explicitly a shared aggregation
 * "filtered per report by accountTypeCode", so exactly one repository method exists
 * ({@code JournalLineRepository.aggregateAccountBalances}) and each statement narrows its result:
 * the trial balance by the caller's optional single type, the balance sheet by
 * {@link #BALANCE_SHEET_TYPES}, the income statement by {@link #INCOME_STATEMENT_TYPES}. No
 * near-identical second or third query is written.
 *
 * <p>No caching annotations anywhere: gov-enforce-caching-rules bars caching financial records and
 * balances outright (D.5.3), and FIN's approved register is empty. Every method is
 * {@code @Transactional(readOnly = true)} with {@code log.debug} (A.5.4 / A.5.14).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    /**
     * API-FIN-030's scope, verbatim from its Orchestration line ("accountTypeCode IN
     * ASSET,LIABILITY,EQUITY"). The codes are SRS A6's {@code ACCOUNT_TYPE} values.
     */
    private static final List<String> BALANCE_SHEET_TYPES = List.of("ASSET", "LIABILITY", "EQUITY");

    /** API-FIN-031's scope, verbatim from its Orchestration line ("REVENUE,EXPENSE"). */
    private static final List<String> INCOME_STATEMENT_TYPES = List.of("REVENUE", "EXPENSE");

    /** Revenue less expense, in the order {@link #INCOME_STATEMENT_TYPES} declares. */
    private static final String TYPE_REVENUE = "REVENUE";

    private final AccountRepository accountRepository;
    private final DimensionRepository dimensionRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalLineDimensionRepository journalLineDimensionRepository;
    private final ReportMapper mapper;

    /**
     * API-FIN-028 — the account ledger (REQ-FIN-039, QR-FIN-042). Resolves the account
     * ({@code FIN-404-ACCOUNT}, the single code the plan's Errors line names), reads its POSTED
     * lines in date order and hands them to the mapper, which accumulates the running balance.
     *
     * <p>The date range and the dimension filters are optional; a null bound simply drops its
     * predicate. Each returned row carries its {@code journalEntryId} and {@code eventReference},
     * which is REQ-FIN-046's drill-down path out of the report.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ACCOUNT_LEDGER_VIEW)")
    public ServiceResult<AccountLedgerResponse> accountLedger(Long accountId,
                                                              LocalDate fromDate,
                                                              LocalDate toDate,
                                                              Long dimensionId,
                                                              Long dimensionValueId) {
        log.debug("Building account ledger for Account ID: {}", accountId);

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT, accountId));

        return ServiceResult.success(mapper.toAccountLedger(account, fromDate, toDate,
            dimensionId, dimensionValueId,
            journalLineRepository.findAccountLedgerLines(accountId, JournalEntry.STATUS_POSTED,
                fromDate, toDate, dimensionId, dimensionValueId)));
    }

    /**
     * API-FIN-029 — the trial balance (REQ-FIN-040, QR-FIN-043), one row per account, optionally
     * scoped to one fiscal period and one account type.
     *
     * <p>POL-FIN-008 is REPORTED, not enforced: the two column totals match because every
     * contributing entry satisfied RULE-FIN-006 before it could post, so each account's net sits
     * on its own side and the nets sum to zero. The {@code balanced} flag exposes that identity;
     * a false value means the posted data or the query is wrong, and the plan's own Validations
     * line says no separate check is required.
     *
     * <p>{@code periodId} is an OPTIONAL narrowing, not a key: omitting it means "no period
     * narrowing" and stays a 200 (AC-FIN-040's happy path). Supplied, it must resolve — an unknown
     * id is {@code FIN-404-PERIOD}, the same code {@link #incomeStatement} already raises for its
     * own period bounds — rather than silently yielding an all-zero report.
     *
     * <p>Note the flag is only meaningful over the UNFILTERED result: asking for one account type
     * deliberately returns a slice, and a slice of a balanced set need not balance.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_TRIAL_BALANCE_VIEW)")
    public ServiceResult<TrialBalanceResponse> trialBalance(Long periodId,
                                                            String accountTypeCode) {
        log.debug("Building trial balance for Period ID: {}", periodId);

        if (periodId != null) {
            periodOf(periodId);
        }

        List<AccountBalanceRowResponse> rows = balanceRows(periodId, null, null, null,
            accountTypeCode == null || accountTypeCode.isBlank()
                ? List.of() : List.of(accountTypeCode));

        BigDecimal totalDebit = mapper.sumDebitBalances(rows);
        BigDecimal totalCredit = mapper.sumCreditBalances(rows);

        return ServiceResult.success(TrialBalanceResponse.builder()
            .periodId(periodId)
            .accountTypeCode(accountTypeCode)
            .rows(rows)
            .totalDebitBalance(totalDebit)
            .totalCreditBalance(totalCredit)
            .balanced(totalDebit.compareTo(totalCredit) == 0)
            .build());
    }

    /**
     * API-FIN-030 — the balance sheet (REQ-FIN-041): the same QR-FIN-043 aggregation kept to
     * ASSET / LIABILITY / EQUITY, scoped to one fiscal year and optionally cut off at a date.
     *
     * <p>{@code fiscalYearId} is the REQUIRED keying identifier and is resolved first: an unknown
     * id is {@code FIN-404-YEAR}, never a 200 carrying an all-zero statement a controller could
     * not tell apart from a genuinely dormant year.
     *
     * <p>Continuity is not re-validated here, exactly as the plan's Validations line states: the
     * prior year's closing balances are this year's opening balances because REQ-FIN-036's
     * year-end close POSTED them as an opening entry (POL-FIN-010), and those are ordinary posted
     * lines this aggregation already reads.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_BALANCE_SHEET_VIEW)")
    public ServiceResult<BalanceSheetResponse> balanceSheet(Long fiscalYearId,
                                                            LocalDate asOfDate) {
        log.debug("Building balance sheet for FiscalYear ID: {}", fiscalYearId);

        assertFiscalYearExists(fiscalYearId);

        List<AccountBalanceRowResponse> rows =
            balanceRows(null, fiscalYearId, null, asOfDate, BALANCE_SHEET_TYPES);

        return ServiceResult.success(BalanceSheetResponse.builder()
            .fiscalYearId(fiscalYearId)
            .asOfDate(asOfDate)
            .groups(groupsOf(rows, BALANCE_SHEET_TYPES))
            .build());
    }

    /**
     * API-FIN-031 — the income statement (REQ-FIN-042): the same QR-FIN-043 aggregation kept to
     * REVENUE / EXPENSE, scoped to one fiscal year and to a range of its periods.
     *
     * <p>{@code fiscalYearId} is the REQUIRED keying identifier and is resolved first
     * ({@code FIN-404-YEAR}); the two period bounds stay OPTIONAL narrowings, validated only when
     * supplied. Resolving only the bounds and not the year was the asymmetry this closes.
     *
     * <p>The plan says "period range" without naming fields, so the range is expressed as two
     * period ids and translated here into the {@code docDate} bounds those periods span
     * (DBF-FIN-080 / DBF-FIN-081). An unknown period id raises the already-registered
     * {@code FIN-404-PERIOD} rather than being silently ignored — see the {@code api_doc_gaps}
     * entry recorded for this sub.
     *
     * <p>"Opens at zero" needs no check either (the plan's Validations line): year-end close moves
     * every result account's balance to Retained Earnings, so a fresh year simply has no posted
     * result lines and the aggregation returns nothing for them.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_INCOME_STATEMENT_VIEW)")
    public ServiceResult<IncomeStatementResponse> incomeStatement(Long fiscalYearId,
                                                                  Long fromPeriodId,
                                                                  Long toPeriodId) {
        log.debug("Building income statement for FiscalYear ID: {}", fiscalYearId);

        assertFiscalYearExists(fiscalYearId);

        LocalDate fromDate = fromPeriodId == null ? null : periodOf(fromPeriodId).getStartDate();
        LocalDate toDate = toPeriodId == null ? null : periodOf(toPeriodId).getEndDate();

        List<AccountBalanceRowResponse> rows =
            balanceRows(null, fiscalYearId, fromDate, toDate, INCOME_STATEMENT_TYPES);
        List<AccountBalanceGroupResponse> groups = groupsOf(rows, INCOME_STATEMENT_TYPES);

        BigDecimal revenueTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;
        for (AccountBalanceGroupResponse group : groups) {
            if (TYPE_REVENUE.equals(group.getAccountTypeCode())) {
                revenueTotal = group.getGroupTotal();
            } else {
                expenseTotal = group.getGroupTotal();
            }
        }

        return ServiceResult.success(IncomeStatementResponse.builder()
            .fiscalYearId(fiscalYearId)
            .fromPeriodId(fromPeriodId)
            .toPeriodId(toPeriodId)
            .fromDate(fromDate)
            .toDate(toDate)
            .groups(groups)
            .netResult(revenueTotal.subtract(expenseTotal))
            .build());
    }

    /**
     * API-FIN-032 — the dimension report (REQ-FIN-043, QR-FIN-044). Grouped by account AND
     * dimension value, never by the base account alone (POL-FIN-011, AC-FIN-043): one account
     * posted against two project values yields two rows.
     *
     * <p>An unknown dimension is {@code FIN-404-DIMENSION}, the single code the plan's Errors line
     * names. {@code dimensionValueId} and {@code periodId} are optional narrowings.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_DIMENSION_REPORTS_VIEW)")
    public ServiceResult<DimensionReportResponse> dimensionReport(Long dimensionId,
                                                                  Long dimensionValueId,
                                                                  Long periodId) {
        log.debug("Building dimension report for Dimension ID: {}", dimensionId);

        if (!dimensionRepository.existsById(dimensionId)) {
            throw new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_DIMENSION, dimensionId);
        }

        List<DimensionReportRowResponse> rows = journalLineDimensionRepository
            .aggregateByAccountAndDimensionValue(JournalEntry.STATUS_POSTED,
                JournalEntryDomain.DIRECTION_DEBIT, dimensionId, dimensionValueId, periodId)
            .stream()
            .map(mapper::toDimensionReportRow)
            .toList();

        return ServiceResult.success(DimensionReportResponse.builder()
            .dimensionId(dimensionId)
            .dimensionValueId(dimensionValueId)
            .periodId(periodId)
            .rows(rows)
            .build());
    }

    /**
     * The single QR-FIN-043 call plus the per-report {@code accountTypeCode} narrowing. An empty
     * {@code accountTypeCodes} means "every type", which is the trial balance's default.
     */
    private List<AccountBalanceRowResponse> balanceRows(Long periodId,
                                                        Long fiscalYearId,
                                                        LocalDate fromDate,
                                                        LocalDate toDate,
                                                        List<String> accountTypeCodes) {
        List<AccountBalanceView> views = journalLineRepository.aggregateAccountBalances(
            JournalEntry.STATUS_POSTED, JournalEntryDomain.DIRECTION_DEBIT,
            periodId, fiscalYearId, fromDate, toDate);

        return views.stream()
            .filter(view -> accountTypeCodes.isEmpty()
                || accountTypeCodes.contains(view.getAccountTypeCode()))
            .map(mapper::toAccountBalanceRow)
            .toList();
    }

    /** Groups already-narrowed rows in the declared type order, one group per type, always present. */
    private List<AccountBalanceGroupResponse> groupsOf(List<AccountBalanceRowResponse> rows,
                                                       List<String> accountTypeCodes) {
        List<AccountBalanceGroupResponse> groups = new ArrayList<>(accountTypeCodes.size());
        for (String accountTypeCode : accountTypeCodes) {
            groups.add(mapper.toGroup(accountTypeCode, rows.stream()
                .filter(row -> accountTypeCode.equals(row.getAccountTypeCode()))
                .toList()));
        }
        return groups;
    }

    /**
     * Asserts API-FIN-030 / API-FIN-031's keying fiscal year exists; unknown id is
     * {@code FIN-404-YEAR}, already registered in {@link FinErrorCodes} and in both bundles.
     *
     * <p>{@code existsById} rather than {@code findById().orElseThrow()}: no report reads a single
     * field of the year — every figure comes from the QR-FIN-043 aggregation, which takes the id
     * itself — so loading the row would leave an unused entity. This is exactly the shape
     * {@link #dimensionReport} already uses for its own existence-only key check.
     */
    private void assertFiscalYearExists(Long fiscalYearId) {
        if (!fiscalYearRepository.existsById(fiscalYearId)) {
            throw new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_YEAR, fiscalYearId);
        }
    }

    /** Resolves one bound of API-FIN-031's period range; unknown id is {@code FIN-404-PERIOD}. */
    private FiscalPeriod periodOf(Long periodId) {
        return fiscalPeriodRepository.findById(periodId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_PERIOD, periodId));
    }
}
