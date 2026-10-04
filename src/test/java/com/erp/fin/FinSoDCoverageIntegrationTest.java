package com.erp.fin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.YearEndCloseResponse;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.FiscalYear;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.repository.JournalEntryRepository;
import com.erp.fin.service.FiscalPeriodService;
import com.erp.fin.service.FiscalYearService;
import com.erp.fin.FinYearEndFixtures.YearEndFixture;
import com.erp.main.ErpMainApplication;
import com.erp.sec.permission.PermissionConstants;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coverage for the three FIN test-plan cases — TC-FIN-038, TC-FIN-060 and TC-FIN-061
 * ({@code governance/modules/FIN/packages/backend-test/RULE-SCENARIOS.md}) — that no HTTP-level
 * run can exercise, because each one needs a principal holding ONE FIN permission but not another
 * and the {@code api-verify} skill may not create roles or users to build one. An in-process JUnit
 * test sets the {@link SecurityContextHolder} directly, so the exact authority set each case names
 * is constructible here and nowhere else.
 *
 * <p>All three are AC-FIN-038 / RULE-FIN-015: the close-approval action is gated by a permission
 * distinct from the journal-entry-creation permission, and by nothing else. The denial therefore
 * comes from {@code @PreAuthorize(PERM_FIN_PERIODS_CLOSE_APPROVE)} on
 * {@code FiscalPeriodService.hardClose} / {@code FiscalYearService.yearEndClose}, and reaches the
 * caller as {@link LocalizedException} carrying {@code FIN-403-FORBIDDEN} because
 * {@code FinForbiddenAdvisor} re-raises every {@code AccessDeniedException} thrown inside
 * {@code com.erp.fin.service} as that catalog code. {@code FIN-403-SOD-VIOLATION} is never the
 * answer — {@code FinSeparationOfDutiesService} and
 * {@code FiscalPeriodDomain.assertCanHardClose(boolean, boolean)} were both deleted by a recorded
 * human decision, so the code has no throw site left anywhere in {@code com.erp.fin}. There is no
 * HTTP layer in this suite, so nothing here asserts a status line; the exception and the persisted
 * state are the observable outcome.
 *
 * <p>Runs against the real dev Postgres/Redis (docker/docker-compose.yml, {@code dev} profile) the
 * same way the running application does; every test wraps its writes in the outer class-level
 * {@link Transactional} and rolls back on completion, so nothing here leaves data behind.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class FinSoDCoverageIntegrationTest {

    /**
     * Far enough ahead of every fiscal year the dev database carries that the fixture's two years
     * and their successor-by-adjacency lookup cannot collide with existing rows.
     */
    private static final LocalDate FIXTURE_YEAR_START = LocalDate.of(2190, 1, 1);

    @Autowired
    private FiscalPeriodService fiscalPeriodService;
    @Autowired
    private FiscalYearService fiscalYearService;

    @Autowired
    private FiscalYearRepository fiscalYearRepository;
    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------------------------
    // TC-FIN-038 — close-approval permission distinct from entry-creation permission (SoD)
    // -----------------------------------------------------------------------------------------

    @Test
    void hardClose_isDeniedForAPrincipalHoldingOnlyTheEntryCreationPermission() {
        // Covers: TC-FIN-038, AC-FIN-038
        //
        // "A role holding only PERM_FIN_JOURNAL_ENTRIES_CREATE (no close-approve permission)" is
        // exactly the principal no api-verify HTTP client may construct. Built here as the literal
        // authority set, so the ONLY thing that can account for the refusal is the missing
        // PERM_FIN_PERIODS_CLOSE_APPROVE on FiscalPeriodService.hardClose.
        FiscalYear year = persistYear(FIXTURE_YEAR_START, FIXTURE_YEAR_START.plusYears(1).minusDays(1));
        FiscalPeriod period = persistPeriod(year, 1, FiscalPeriod.STATUS_OPEN);

        setAuthenticatedPrincipal("tc-fin-038-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE);

        Throwable thrown =
            catchThrowable(() -> fiscalPeriodService.hardClose(period.getFiscalPeriodPk()));

        assertThat(thrown).isInstanceOf(LocalizedException.class);
        LocalizedException denial = (LocalizedException) thrown;
        assertThat(denial.getErrorCode()).isEqualTo(FinErrorCodes.FIN_403_FORBIDDEN);
        assertThat(denial.getStatus()).isEqualTo(Status.FORBIDDEN);

        // "the period is unchanged" — asserted, not assumed.
        FiscalPeriod reloaded =
            fiscalPeriodRepository.findById(period.getFiscalPeriodPk()).orElseThrow();
        assertThat(reloaded.getStatusCode()).isEqualTo(FiscalPeriod.STATUS_OPEN);
        assertThat(reloaded.getClosedBy()).isNull();
        assertThat(reloaded.getClosedAt()).isNull();
    }

    // -----------------------------------------------------------------------------------------
    // TC-FIN-060 / TC-FIN-052 — the same AC-FIN-038 denial on year-end close (API-FIN-027)
    // -----------------------------------------------------------------------------------------

    @Test
    void yearEndClose_isDeniedForAPrincipalHoldingOnlyTheEntryCreationPermission() {
        // Covers: TC-FIN-060 (v1 plan) and TC-FIN-052 (v2 plan, API-SCENARIOS.md) — one and the
        // same case: AC-FIN-038 / RULE-FIN-015 on API-FIN-027. The v2 block's "assert the 403 at
        // the gate, never from inside the service" is what the fully-eligible fixture buys: the
        // year would otherwise close, so only @PreAuthorize can account for the refusal.
        //
        // The year is built FULLY ELIGIBLE on purpose — every period HARD_CLOSE, a Retained
        // Earnings account marked, an adjacent successor year — so that nothing but the missing
        // permission can account for the refusal. A refusal against an ineligible year would be
        // indistinguishable from FIN-409-PERIODS-NOT-CLOSED / FIN-404-YEAR / FIN-404-ACCOUNT and
        // would prove nothing.
        YearEndFixture fixture = eligibleYearEndFixture();

        setAuthenticatedPrincipal("tc-fin-060-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE);

        Throwable thrown = catchThrowable(
            () -> fiscalYearService.yearEndClose(fixture.year().getFiscalYearPk()));

        assertThat(thrown).isInstanceOf(LocalizedException.class);
        LocalizedException denial = (LocalizedException) thrown;
        assertThat(denial.getErrorCode()).isEqualTo(FinErrorCodes.FIN_403_FORBIDDEN);
        assertThat(denial.getStatus()).isEqualTo(Status.FORBIDDEN);

        // "the year stays OPEN and every period keeps its status".
        FiscalYear reloadedYear =
            fiscalYearRepository.findById(fixture.year().getFiscalYearPk()).orElseThrow();
        assertThat(reloadedYear.getStatusCode()).isEqualTo(FiscalYear.STATUS_OPEN);
        assertThat(fiscalPeriodRepository.findByFiscalYearId(reloadedYear.getFiscalYearPk()))
            .isNotEmpty()
            .allSatisfy(period ->
                assertThat(period.getStatusCode()).isEqualTo(FiscalPeriod.STATUS_HARD_CLOSE));

        // TC-FIN-052's "no entry is posted": the denial happens before the method body runs, so
        // neither the closing entry in this year nor the opening entry in its successor exists.
        assertThat(countEntriesIn(fixture.year().getFiscalYearPk())).isZero();
        assertThat(countEntriesIn(fixture.successor().getFiscalYearPk())).isZero();
    }

    private long countEntriesIn(Long fiscalYearPk) {
        return journalEntryRepository.count((org.springframework.data.jpa.domain.Specification
            <JournalEntry>) (root, query, cb) ->
            cb.equal(root.get("fiscalYear").get("fiscalYearPk"), fiscalYearPk));
    }

    // -----------------------------------------------------------------------------------------
    // TC-FIN-061 — a principal holding BOTH permissions CAN close (the satisfied direction)
    // -----------------------------------------------------------------------------------------

    @Test
    void yearEndClose_succeedsForAPrincipalHoldingBothPermissions() {
        // Covers: TC-FIN-061, REQ-FIN-038 (satisfied direction)
        //
        // The post-V30 `admin`/SYS_ADMIN state: ONE principal holding BOTH
        // PERM_FIN_JOURNAL_ENTRIES_CREATE and PERM_FIN_PERIODS_CLOSE_APPROVE — the exact overlap
        // the deleted global user-set disjointness check used to refuse. Same fully-eligible
        // fixture as TC-FIN-060, so the ONLY difference between the two tests is the authority set.
        YearEndFixture fixture = eligibleYearEndFixture();

        setAuthenticatedPrincipal("tc-fin-061-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE,
            PermissionConstants.PERM_FIN_PERIODS_CLOSE_APPROVE);

        AtomicReference<ServiceResult<YearEndCloseResponse>> result = new AtomicReference<>();
        Throwable thrown = catchThrowable(() ->
            result.set(fiscalYearService.yearEndClose(fixture.year().getFiscalYearPk())));

        // "Assert EXPLICITLY that the response is not a 403 and carries no FIN-403-SOD-VIOLATION":
        // the call must complete without any LocalizedException at all — no FORBIDDEN status, and
        // therefore neither FIN-403-FORBIDDEN nor the (throw-site-less) FIN-403-SOD-VIOLATION.
        assertThat(thrown)
            .as("year-end close must not be denied for a principal holding BOTH permissions — "
                + "RULE-FIN-015 is satisfied by the distinct permission, not by disjoint user sets")
            .isNull();

        ServiceResult<YearEndCloseResponse> closeResult = result.get();
        assertThat(closeResult).isNotNull();
        assertThat(closeResult.getStatus()).isEqualTo(Status.CREATED);

        YearEndCloseResponse response = closeResult.getData();
        assertThat(response).isNotNull();
        assertThat(response.getClosingEntry()).isNotNull();
        assertThat(response.getClosingEntry().getJournalTypeCode())
            .isEqualTo(JournalEntryDomain.JOURNAL_TYPE_CLOSING);
        assertThat(response.getOpeningEntry()).isNotNull();
        assertThat(response.getOpeningEntry().getJournalTypeCode())
            .isEqualTo(JournalEntryDomain.JOURNAL_TYPE_OPENING);

        FiscalYear reloadedYear =
            fiscalYearRepository.findById(fixture.year().getFiscalYearPk()).orElseThrow();
        assertThat(reloadedYear.getStatusCode()).isEqualTo(FiscalYear.STATUS_CLOSED);
        assertThat(fiscalPeriodRepository.findByFiscalYearId(reloadedYear.getFiscalYearPk()))
            .isNotEmpty()
            .allSatisfy(period ->
                assertThat(period.getStatusCode()).isEqualTo(FiscalPeriod.STATUS_YEAR_END_CLOSE));
    }

    // -----------------------------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------------------------

    /**
     * The one year-end fixture TC-FIN-060 and TC-FIN-061 share: a fiscal year every one of whose
     * periods is HARD_CLOSE, an account carrying {@code IS_RETAINED_EARNINGS_FL}, and an adjacent
     * successor year (its {@code startDate} the day after this year's {@code endDate}) holding a
     * period for the opening entry. Every precondition {@code FiscalYearService.yearEndClose}
     * checks is therefore satisfied before either test calls it.
     *
     * <p>Built by {@link FinYearEndFixtures}, which {@code FinYearEndCoverageIntegrationTest}
     * shares — the setup has one implementation, not two that can drift apart.
     */
    private YearEndFixture eligibleYearEndFixture() {
        return fixtures().eligibleYearEndFixture(FIXTURE_YEAR_START);
    }

    private FiscalYear persistYear(LocalDate startDate, LocalDate endDate) {
        return fixtures().persistYear(startDate, endDate);
    }

    private FiscalPeriod persistPeriod(FiscalYear year, int periodNo, String statusCode) {
        return fixtures().persistPeriod(year, periodNo, statusCode);
    }

    private FinYearEndFixtures fixtures() {
        return new FinYearEndFixtures(
            fiscalYearRepository, fiscalPeriodRepository, accountRepository);
    }

    private void setAuthenticatedPrincipal(String username, String... authorities) {
        FinYearEndFixtures.setAuthenticatedPrincipal(username, authorities);
    }

    private String uniqueSuffix() {
        return FinYearEndFixtures.uniqueSuffix();
    }

}
