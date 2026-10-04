package com.erp.fin;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.fin.FinYearEndFixtures.YearEndFixture;
import com.erp.fin.domain.AccountDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.AccountBalanceGroupResponse;
import com.erp.fin.dto.AccountBalanceRowResponse;
import com.erp.fin.dto.BalanceSheetResponse;
import com.erp.fin.dto.IncomeStatementResponse;
import com.erp.fin.dto.JournalEntryCreateRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineCreateRequest;
import com.erp.fin.dto.YearEndCloseResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.FiscalYear;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.service.FiscalYearService;
import com.erp.fin.service.JournalEntryService;
import com.erp.fin.service.ReportService;
import com.erp.main.ErpMainApplication;
import com.erp.sec.permission.PermissionConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * TC-FIN-041 / AC-FIN-041 and TC-FIN-042 / AC-FIN-042 — what the two financial statements must
 * show on the far side of a year-end close: every balance-sheet account carried forward, every
 * result account restarted at zero. Both are stated over "every" account, so both are asserted
 * over the whole statement rather than one spot-checked row. Real dev Postgres under the
 * {@code dev} profile; the class-level {@link Transactional} rolls every write back.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class FinYearEndContinuityIntegrationTest {

    private static final LocalDate FIXTURE_YEAR_START = LocalDate.of(2240, 1, 1);
    private static final BigDecimal REVENUE_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal EXPENSE_AMOUNT = new BigDecimal("400.00");

    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;

    @Autowired private FiscalYearService fiscalYearService;
    @Autowired private JournalEntryService journalEntryService;
    @Autowired private ReportService reportService;

    private FinYearEndFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new FinYearEndFixtures(
            fiscalYearRepository, fiscalPeriodRepository, accountRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void everyBalanceSheetAccountOpensTheNewYearAtItsPriorYearClosingBalance() {
        // Covers: TC-FIN-041, AC-FIN-041 / REQ-FIN-041, API-FIN-030.
        //
        // AC-FIN-041 verbatim: "its opening balances equal the prior year's closing balances for
        // EVERY balance-sheet account". Asserted over the union of the two statements' account
        // sets — a single matching row would satisfy a spot check while an account silently
        // dropped from the opening entry would not be noticed.
        ClosedYear closed = closeAYearWithActivity();

        setStatementViewer(PermissionConstants.PERM_FIN_BALANCE_SHEET_VIEW);
        Map<Long, BigDecimal> priorYear = balanceSheetBalances(closed.year().getFiscalYearPk());
        Map<Long, BigDecimal> newYear = balanceSheetBalances(closed.successor().getFiscalYearPk());

        assertThat(priorYear).as("the closed year's balance sheet must not be empty").isNotEmpty();

        Set<Long> everyAccount = new LinkedHashSet<>(priorYear.keySet());
        everyAccount.addAll(newYear.keySet());
        assertThat(everyAccount)
            .as("both years must describe the same balance-sheet accounts")
            .containsExactlyInAnyOrderElementsOf(priorYear.keySet())
            .containsExactlyInAnyOrderElementsOf(newYear.keySet());

        for (Long accountPk : everyAccount) {
            assertThat(newYear.get(accountPk))
                .as("opening balance of account " + accountPk + " equals its closing balance")
                .isEqualByComparingTo(priorYear.get(accountPk));
        }

        // The continuity is over real money, not two empty statements: the asset and Retained
        // Earnings both carry the year's result forward.
        assertThat(priorYear.get(closed.assetAccount().getAccountPk()))
            .isEqualByComparingTo(REVENUE_AMOUNT.subtract(EXPENSE_AMOUNT));
        // signedBalance is stated against each account's own natureCode (ReportMapper
        // .signedAgainstNature), so a CREDIT-nature Retained Earnings carries the result POSITIVE.
        assertThat(newYear.get(closed.retainedEarningsAccount().getAccountPk()))
            .isEqualByComparingTo(REVENUE_AMOUNT.subtract(EXPENSE_AMOUNT));
    }

    @Test
    void everyResultAccountStandsAtZeroOnceTheYearIsClosed() {
        // Covers: TC-FIN-042, AC-FIN-042 / REQ-FIN-042, API-FIN-031.
        //
        // AC-FIN-042 verbatim: "every revenue/expense account shows a zero balance". Asserted on
        // the year that was ACTUALLY closed — where the revenue and expense accounts carry posted
        // lines and are therefore in the statement at all — and again on the new year, where the
        // AC's own wording puts it. On an untouched new year the statement is empty, so that half
        // alone is vacuous and could not tell a working close from a broken one.
        ClosedYear closed = closeAYearWithActivity();

        setStatementViewer(PermissionConstants.PERM_FIN_INCOME_STATEMENT_VIEW);

        IncomeStatementResponse closedYear =
            reportService.incomeStatement(closed.year().getFiscalYearPk(), null, null).getData();
        Map<Long, BigDecimal> closedBalances = balancesOf(closedYear.getGroups());
        assertThat(closedBalances.keySet())
            .as("the closed year's result accounts are in its income statement")
            .contains(closed.revenueAccount().getAccountPk(),
                closed.expenseAccount().getAccountPk());
        assertThat(closedBalances.values())
            .allSatisfy(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO));
        assertThat(closedYear.getNetResult()).isEqualByComparingTo(BigDecimal.ZERO);

        IncomeStatementResponse newYear = reportService
            .incomeStatement(closed.successor().getFiscalYearPk(), null, null).getData();
        assertThat(balancesOf(newYear.getGroups()).values())
            .allSatisfy(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO));
        assertThat(newYear.getNetResult()).isEqualByComparingTo(BigDecimal.ZERO);

        // The contrast TC-FIN-042 calls "the point": the balance-sheet accounts did NOT restart.
        setStatementViewer(PermissionConstants.PERM_FIN_BALANCE_SHEET_VIEW);
        assertThat(balanceSheetBalances(closed.successor().getFiscalYearPk()))
            .isNotEmpty()
            .anySatisfy((accountPk, balance) ->
                assertThat(balance).isNotEqualByComparingTo(BigDecimal.ZERO));
    }

    private ClosedYear closeAYearWithActivity() {
        YearEndFixture fixture = fixtures.openYearEndFixture(FIXTURE_YEAR_START);
        FiscalPeriod firstPeriod = fixture.periods().get(0);

        Account asset = fixtures.persistAccount("ASSET", JournalEntryDomain.DIRECTION_DEBIT);
        Account revenue = fixtures.persistAccount(
            AccountDomain.ACCOUNT_TYPE_REVENUE, JournalEntryDomain.DIRECTION_CREDIT);
        Account expense = fixtures.persistAccount(
            AccountDomain.ACCOUNT_TYPE_EXPENSE, JournalEntryDomain.DIRECTION_DEBIT);
        fixtures.clearRetainedEarningsMarkers();
        Account retainedEarnings =
            fixtures.persistAccount("EQUITY", JournalEntryDomain.DIRECTION_CREDIT, true);

        FinYearEndFixtures.setAuthenticatedPrincipal("fin-continuity-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE);
        postManualEntry(fixture.year(), firstPeriod, asset, REVENUE_AMOUNT,
            JournalEntryDomain.DIRECTION_DEBIT, revenue, REVENUE_AMOUNT,
            JournalEntryDomain.DIRECTION_CREDIT);
        postManualEntry(fixture.year(), firstPeriod, expense, EXPENSE_AMOUNT,
            JournalEntryDomain.DIRECTION_DEBIT, asset, EXPENSE_AMOUNT,
            JournalEntryDomain.DIRECTION_CREDIT);

        fixtures.hardClose(fixture.periods());
        SecurityContextHolder.clearContext();

        FinYearEndFixtures.setAuthenticatedPrincipal("fin-continuity-close-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_PERIODS_CLOSE_APPROVE);
        ServiceResult<YearEndCloseResponse> close =
            fiscalYearService.yearEndClose(fixture.year().getFiscalYearPk());
        assertThat(close.getStatus()).isEqualTo(Status.CREATED);
        SecurityContextHolder.clearContext();

        return new ClosedYear(fixture.year(), fixture.successor(), asset, revenue, expense,
            retainedEarnings);
    }

    private void postManualEntry(FiscalYear year, FiscalPeriod period,
                                 Account debitAccount, BigDecimal debitAmount,
                                 String debitDirection,
                                 Account creditAccount, BigDecimal creditAmount,
                                 String creditDirection) {
        ServiceResult<JournalEntryResponse> posted =
            journalEntryService.createManual(JournalEntryCreateRequest.builder()
                .docDate(period.getStartDate())
                .fiscalYearId(year.getFiscalYearPk())
                .periodId(period.getFiscalPeriodPk())
                .journalTypeCode("MANUAL")
                .descriptionEn("Year-end continuity fixture activity")
                .lines(List.of(
                    JournalLineCreateRequest.builder()
                        .accountId(debitAccount.getAccountPk()).amount(debitAmount)
                        .directionCode(debitDirection).build(),
                    JournalLineCreateRequest.builder()
                        .accountId(creditAccount.getAccountPk()).amount(creditAmount)
                        .directionCode(creditDirection).build()))
                .build());
        assertThat(posted.getStatus()).isEqualTo(Status.CREATED);
    }

    private Map<Long, BigDecimal> balanceSheetBalances(Long fiscalYearPk) {
        BalanceSheetResponse sheet =
            reportService.balanceSheet(fiscalYearPk, null).getData();
        return balancesOf(sheet.getGroups());
    }

    private static Map<Long, BigDecimal> balancesOf(List<AccountBalanceGroupResponse> groups) {
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        for (AccountBalanceGroupResponse group : groups) {
            for (AccountBalanceRowResponse row : group.getRows()) {
                balances.merge(row.getAccountId(), row.getSignedBalance(), BigDecimal::add);
            }
        }
        return balances;
    }

    private void setStatementViewer(String permission) {
        FinYearEndFixtures.setAuthenticatedPrincipal(
            "fin-continuity-view-" + uniqueSuffix(), permission);
    }

    private static String uniqueSuffix() {
        return FinYearEndFixtures.uniqueSuffix();
    }

    /** The closed year, its successor, and the accounts its activity used. */
    private record ClosedYear(FiscalYear year, FiscalYear successor, Account assetAccount,
                              Account revenueAccount, Account expenseAccount,
                              Account retainedEarningsAccount) {
    }
}
