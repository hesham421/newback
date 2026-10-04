package com.erp.fin;

import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.FiscalYear;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The year-end fixture builder shared by {@link FinSoDCoverageIntegrationTest} (TC-FIN-038 /
 * 060 / 061) and {@link FinYearEndCoverageIntegrationTest} (TC-FIN-036 / 044 / 056 / 086 / 102).
 * Extracted when the second suite landed so the ~40-line "fully eligible year-end state" setup has
 * exactly one implementation: both suites depend on every precondition
 * {@code FiscalYearService.yearEndClose} checks being satisfied the same way, and two copies would
 * drift the moment one of those preconditions changes.
 *
 * <p>Package-private test support, not a Spring bean — each suite constructs one from its own
 * autowired repositories and every write happens inside that suite's class-level
 * {@code @Transactional}, so nothing here survives the test that built it.
 */
final class FinYearEndFixtures {

    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AccountRepository accountRepository;

    FinYearEndFixtures(FiscalYearRepository fiscalYearRepository,
                       FiscalPeriodRepository fiscalPeriodRepository,
                       AccountRepository accountRepository) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * A fiscal year whose two periods are still OPEN, plus an adjacent successor year holding one
     * OPEN period. Nothing is marked as Retained Earnings yet and no activity is posted — this is
     * the starting point a caller either hard-closes straight away
     * ({@link #eligibleYearEndFixture(LocalDate)}) or posts into first.
     */
    YearEndFixture openYearEndFixture(LocalDate yearStart) {
        LocalDate yearEnd = yearStart.plusYears(1).minusDays(1);

        FiscalYear year = persistYear(yearStart, yearEnd);
        List<FiscalPeriod> periods = List.of(
            persistPeriod(year, 1, FiscalPeriod.STATUS_OPEN),
            persistPeriod(year, 2, FiscalPeriod.STATUS_OPEN));

        LocalDate successorStart = yearEnd.plusDays(1);
        FiscalYear successor =
            persistYear(successorStart, successorStart.plusYears(1).minusDays(1));
        persistPeriod(successor, 1, FiscalPeriod.STATUS_OPEN);

        return new YearEndFixture(year, successor, periods);
    }

    /**
     * The fully-eligible year-end setup: a fiscal year every one of whose periods is HARD_CLOSE,
     * an account carrying {@code IS_RETAINED_EARNINGS_FL}, and an adjacent successor year (its
     * {@code startDate} the day after this year's {@code endDate}) holding a period for the
     * opening entry.
     */
    YearEndFixture eligibleYearEndFixture(LocalDate yearStart) {
        YearEndFixture fixture = openYearEndFixture(yearStart);
        hardClose(fixture.periods());
        retainedEarningsAccount();
        return fixture;
    }

    /** Moves every given period to HARD_CLOSE as data — the state the close-approve APIs leave. */
    void hardClose(List<FiscalPeriod> periods) {
        periods.forEach(period -> period.setStatusCode(FiscalPeriod.STATUS_HARD_CLOSE));
        fiscalPeriodRepository.saveAll(periods);
    }

    FiscalYear persistYear(LocalDate startDate, LocalDate endDate) {
        return fiscalYearRepository.save(FiscalYear.builder()
            .code("Y" + uniqueSuffix())
            .startDate(startDate)
            .endDate(endDate)
            .statusCode(FiscalYear.STATUS_OPEN)
            .build());
    }

    FiscalPeriod persistPeriod(FiscalYear year, int periodNo, String statusCode) {
        LocalDate start = year.getStartDate().plusMonths(periodNo - 1L);
        return fiscalPeriodRepository.save(FiscalPeriod.builder()
            .fiscalYear(year)
            .periodNo(periodNo)
            .nameAr("فترة اختبار " + periodNo)
            .nameEn("Test period " + periodNo)
            .startDate(start)
            .endDate(start.plusMonths(1).minusDays(1))
            .statusCode(statusCode)
            .build());
    }

    /**
     * One postable (leaf + active) account of the given ACCOUNT_TYPE. Both codes are seeded
     * values: ACCOUNT_TYPE and DEBIT_CREDIT come from {@code V26__fin_mdl_lookup_seed.sql}, and
     * the nature reuses {@link JournalEntryDomain}'s DEBIT_CREDIT constants.
     */
    Account persistAccount(String accountTypeCode, String natureCode) {
        return persistAccount(accountTypeCode, natureCode, false);
    }

    Account persistAccount(String accountTypeCode, String natureCode, boolean retainedEarnings) {
        return accountRepository.save(Account.builder()
            .code("A" + uniqueSuffix())
            .nameAr("حساب اختبار")
            .nameEn("Test account")
            .accountTypeCode(accountTypeCode)
            .natureCode(natureCode)
            .isLeafFl(Boolean.TRUE)
            .isActiveFl(Boolean.TRUE)
            .isRetainedEarningsFl(retainedEarnings)
            .build());
    }

    /**
     * {@code UQ_FIN_ACCOUNT_RETAINED_EARNINGS} is a partial unique index over the marked rows, so
     * at most one account may carry the flag: reuse the marked account when the database already
     * has one, and mark a new EQUITY account only when it does not.
     */
    Account retainedEarningsAccount() {
        return accountRepository.findFirstByIsRetainedEarningsFlTrueOrderByAccountPkAsc()
            .orElseGet(() -> persistAccount(
                "EQUITY", JournalEntryDomain.DIRECTION_CREDIT, true));
    }

    /**
     * Clears every existing Retained Earnings marker and returns what was cleared. TC-FIN-086's
     * first step needs a state with NO account marked, which the dev database may not already be
     * in; the update is flushed so {@code FiscalYearService.retainedEarningsAccount()} genuinely
     * finds nothing, and is rolled back with the rest of the test transaction.
     */
    List<Account> clearRetainedEarningsMarkers() {
        List<Account> cleared = new java.util.ArrayList<>();
        Optional<Account> marked;
        while ((marked = accountRepository.findFirstByIsRetainedEarningsFlTrueOrderByAccountPkAsc())
                .isPresent()) {
            Account account = marked.get();
            account.setIsRetainedEarningsFl(Boolean.FALSE);
            cleared.add(accountRepository.saveAndFlush(account));
        }
        return cleared;
    }

    static void setAuthenticatedPrincipal(String username, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities =
            Stream.of(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, "N/A", grantedAuthorities));
    }

    static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** The fully-eligible year-end setup every year-end coverage test runs against. */
    record YearEndFixture(FiscalYear year, FiscalYear successor, List<FiscalPeriod> periods) {
    }
}
