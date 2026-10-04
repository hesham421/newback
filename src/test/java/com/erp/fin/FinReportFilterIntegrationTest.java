package com.erp.fin;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.fin.FinYearEndFixtures.YearEndFixture;
import com.erp.fin.domain.AccountDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.JournalEntryCreateRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineCreateRequest;
import com.erp.fin.dto.JournalLineDimensionCreateRequest;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.Dimension;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.repository.AccountBalanceView;
import com.erp.fin.repository.AccountLedgerLineView;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.DimensionBalanceView;
import com.erp.fin.repository.DimensionRepository;
import com.erp.fin.repository.DimensionValueRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.repository.JournalLineDimensionRepository;
import com.erp.fin.repository.JournalLineRepository;
import com.erp.fin.service.JournalEntryService;
import com.erp.main.ErpMainApplication;
import com.erp.sec.permission.PermissionConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * QR-FIN-042 / QR-FIN-043 / QR-FIN-044 exercised WITH their optional filters supplied — the case
 * API-FIN-028 / 030 / 031 returned 500 INTERNAL_ERROR on, because the untyped
 * {@code (:p IS NULL OR …)} idiom gave PostgreSQL no type to infer for a parameter that only ever
 * appeared against NULL ("could not determine data type of parameter $n").
 *
 * <p>Each filter is asserted three ways: supplied and matching (rows), omitted (the unfiltered
 * result, never an empty one), and supplied but legitimately matching nothing (empty, not an
 * error). Same conventions as {@link FinYearEndCoverageIntegrationTest}: the real dev Postgres
 * under the {@code dev} profile, and a class-level {@link Transactional} that rolls every write
 * back.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class FinReportFilterIntegrationTest {

    private static final LocalDate FIXTURE_YEAR_START = LocalDate.of(2043, 1, 1);
    private static final BigDecimal AMOUNT = new BigDecimal("500.00");

    @Autowired private AccountRepository accountRepository;
    @Autowired private DimensionRepository dimensionRepository;
    @Autowired private DimensionValueRepository dimensionValueRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private JournalEntryService journalEntryService;
    @Autowired private JournalLineRepository journalLineRepository;
    @Autowired private JournalLineDimensionRepository journalLineDimensionRepository;

    private FinYearEndFixtures fixtures;
    private PostedActivity activity;

    @BeforeEach
    void setUp() {
        fixtures = new FinYearEndFixtures(
            fiscalYearRepository, fiscalPeriodRepository, accountRepository);
        activity = postActivity();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------------------------
    // QR-FIN-042 — API-FIN-028's account ledger
    // ---------------------------------------------------------------------------------------

    @Test
    void accountLedgerHonoursItsOptionalDateRangeInsteadOfFailing() {
        List<AccountLedgerLineView> unfiltered = ledger(null, null, null, null);
        assertThat(unfiltered).as("the unfiltered ledger is the baseline").isNotEmpty();

        List<AccountLedgerLineView> filtered =
            ledger(activity.docDate(), activity.docDate(), null, null);

        assertThat(filtered).as("a date range that spans the activity must return rows")
            .isNotEmpty();
        assertThat(filtered).hasSameSizeAs(unfiltered);
    }

    @Test
    void accountLedgerHonoursItsOptionalDimensionFiltersInsteadOfFailing() {
        assertThat(ledger(null, null, activity.dimensionId(), null))
            .as("a dimension filter naming the tagged dimension must return rows").isNotEmpty();
        assertThat(ledger(null, null, null, activity.dimensionValueId()))
            .as("a dimension-value filter naming the tagged value must return rows").isNotEmpty();
        assertThat(ledger(activity.docDate(), activity.docDate(),
            activity.dimensionId(), activity.dimensionValueId()))
            .as("all four filters together must still return rows").isNotEmpty();
    }

    @Test
    void omittingTheLedgerFiltersReturnsTheUnfilteredResultNotAnEmptyOne() {
        assertThat(ledger(null, null, null, null))
            .as("a null bound drops its predicate — it must not match nothing")
            .isNotEmpty();
    }

    @Test
    void aLedgerFilterThatMatchesNothingReturnsAnEmptyResultNotAnError() {
        assertThat(ledger(FIXTURE_YEAR_START.minusYears(5),
            FIXTURE_YEAR_START.minusYears(5), null, null)).isEmpty();
        assertThat(ledger(null, null, activity.dimensionId(), -1L)).isEmpty();
    }

    // ---------------------------------------------------------------------------------------
    // QR-FIN-043 — API-FIN-029 / 030 / 031's shared aggregation
    // ---------------------------------------------------------------------------------------

    @Test
    void balanceAggregationHonoursItsOptionalAsOfDateInsteadOfFailing() {
        // API-FIN-030's balance sheet: fiscalYearId + asOfDate, which maps to (fiscalYearId, toDate).
        List<AccountBalanceView> filtered =
            balances(null, activity.fiscalYearId(), null, activity.docDate());

        assertThat(filtered).as("fiscalYearId + asOfDate must return rows").isNotEmpty();
        assertThat(rowFor(filtered, activity.assetAccount())).isNotNull();
    }

    @Test
    void balanceAggregationHonoursItsOptionalPeriodBoundsInsteadOfFailing() {
        // API-FIN-031's income statement: fiscalYearId + fromPeriodId/toPeriodId, which the
        // service resolves to a docDate range; and API-FIN-029's single periodId.
        assertThat(balances(activity.periodId(), null, null, null))
            .as("a periodId filter must return rows").isNotEmpty();
        assertThat(balances(activity.periodId(), activity.fiscalYearId(),
            activity.periodStart(), activity.periodEnd()))
            .as("every bound supplied at once must still return rows").isNotEmpty();
    }

    @Test
    void omittingTheAggregationFiltersReturnsTheUnfilteredResultNotAnEmptyOne() {
        List<AccountBalanceView> unfiltered = balances(null, null, null, null);
        assertThat(unfiltered).isNotEmpty();
        assertThat(rowFor(unfiltered, activity.assetAccount()))
            .as("the fixture's account is in the unfiltered aggregation").isNotNull();
    }

    @Test
    void anAggregationFilterThatMatchesNothingReturnsAnEmptyResultNotAnError() {
        assertThat(balances(-1L, null, null, null)).isEmpty();
        assertThat(balances(null, null, FIXTURE_YEAR_START.minusYears(5),
            FIXTURE_YEAR_START.minusYears(5))).isEmpty();
    }

    // ---------------------------------------------------------------------------------------
    // QR-FIN-044 — API-FIN-032's dimension report
    // ---------------------------------------------------------------------------------------

    @Test
    void dimensionAggregationHonoursItsOptionalFiltersInsteadOfFailing() {
        List<DimensionBalanceView> unfiltered = dimensionBalances(null, null);
        assertThat(unfiltered).as("the unfiltered dimension report is the baseline").isNotEmpty();

        assertThat(dimensionBalances(activity.dimensionValueId(), null))
            .as("a dimension-value filter must return rows").isNotEmpty();
        assertThat(dimensionBalances(activity.dimensionValueId(), activity.periodId()))
            .as("value + period together must still return rows").isNotEmpty();
        assertThat(dimensionBalances(-1L, null))
            .as("a value that matches nothing is empty, not an error").isEmpty();
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private List<AccountLedgerLineView> ledger(LocalDate fromDate, LocalDate toDate,
                                               Long dimensionId, Long dimensionValueId) {
        return journalLineRepository.findAccountLedgerLines(
            activity.assetAccount(), JournalEntry.STATUS_POSTED,
            fromDate, toDate, dimensionId, dimensionValueId);
    }

    private List<AccountBalanceView> balances(Long periodId, Long fiscalYearId,
                                              LocalDate fromDate, LocalDate toDate) {
        return journalLineRepository.aggregateAccountBalances(
            JournalEntry.STATUS_POSTED, JournalEntryDomain.DIRECTION_DEBIT,
            periodId, fiscalYearId, fromDate, toDate);
    }

    private List<DimensionBalanceView> dimensionBalances(Long dimensionValueId, Long periodId) {
        return journalLineDimensionRepository.aggregateByAccountAndDimensionValue(
            JournalEntry.STATUS_POSTED, JournalEntryDomain.DIRECTION_DEBIT,
            activity.dimensionId(), dimensionValueId, periodId);
    }

    private static AccountBalanceView rowFor(List<AccountBalanceView> rows, Long accountId) {
        return rows.stream()
            .filter(row -> accountId.equals(row.getAccountId()))
            .findFirst().orElse(null);
    }

    /** One balanced, POSTED, dimension-tagged entry in an OPEN period — the whole fixture. */
    private PostedActivity postActivity() {
        YearEndFixture fixture = fixtures.openYearEndFixture(FIXTURE_YEAR_START);
        FiscalPeriod period = fixture.periods().get(0);

        Account asset = fixtures.persistAccount("ASSET", JournalEntryDomain.DIRECTION_DEBIT);
        Account revenue = fixtures.persistAccount(
            AccountDomain.ACCOUNT_TYPE_REVENUE, JournalEntryDomain.DIRECTION_CREDIT);

        Dimension dimension = dimensionRepository.save(Dimension.builder()
            .code("D" + FinYearEndFixtures.uniqueSuffix())
            .nameAr("بُعد اختبار").nameEn("Test dimension")
            .isActiveFl(Boolean.TRUE).build());
        DimensionValue value = dimensionValueRepository.save(DimensionValue.builder()
            .dimension(dimension)
            .code("V" + FinYearEndFixtures.uniqueSuffix())
            .nameAr("قيمة اختبار").nameEn("Test value")
            .isActiveFl(Boolean.TRUE).build());

        FinYearEndFixtures.setAuthenticatedPrincipal(
            "fin-report-filter-" + FinYearEndFixtures.uniqueSuffix(), PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE);

        JournalLineDimensionCreateRequest tag = JournalLineDimensionCreateRequest.builder()
            .dimensionId(dimension.getDimensionPk())
            .dimensionValueId(value.getDimensionValuePk())
            .build();

        ServiceResult<JournalEntryResponse> posted =
            journalEntryService.createManual(JournalEntryCreateRequest.builder()
                .docDate(period.getStartDate())
                .fiscalYearId(fixture.year().getFiscalYearPk())
                .periodId(period.getFiscalPeriodPk())
                .journalTypeCode("MANUAL")
                .descriptionEn("Report filter fixture activity")
                .lines(List.of(
                    JournalLineCreateRequest.builder()
                        .accountId(asset.getAccountPk()).amount(AMOUNT)
                        .directionCode(JournalEntryDomain.DIRECTION_DEBIT)
                        .dimensions(List.of(tag)).build(),
                    JournalLineCreateRequest.builder()
                        .accountId(revenue.getAccountPk()).amount(AMOUNT)
                        .directionCode(JournalEntryDomain.DIRECTION_CREDIT)
                        .dimensions(List.of(tag)).build()))
                .build());

        assertThat(posted.getStatus()).isEqualTo(Status.CREATED);
        SecurityContextHolder.clearContext();

        return new PostedActivity(asset.getAccountPk(), fixture.year().getFiscalYearPk(),
            period.getFiscalPeriodPk(), period.getStartDate(), period.getEndDate(),
            period.getStartDate(), dimension.getDimensionPk(), value.getDimensionValuePk());
    }

    private record PostedActivity(Long assetAccount, Long fiscalYearId, Long periodId,
                                  LocalDate periodStart, LocalDate periodEnd, LocalDate docDate,
                                  Long dimensionId, Long dimensionValueId) {
    }
}
