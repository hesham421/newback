package com.erp.fin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.FinYearEndFixtures.YearEndFixture;
import com.erp.fin.domain.AccountDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.JournalEntryCreateRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineCreateRequest;
import com.erp.fin.dto.JournalLineResponse;
import com.erp.fin.dto.YearEndCloseResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.FiscalYear;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.JournalLine;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.repository.JournalEntryRepository;
import com.erp.fin.repository.JournalLineRepository;
import com.erp.fin.service.FiscalYearService;
import com.erp.fin.service.JournalEntryService;
import com.erp.main.ErpMainApplication;
import com.erp.sec.permission.PermissionConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coverage for the FIN test-plan cases TC-FIN-036, TC-FIN-056, TC-FIN-086 and TC-FIN-102
 * ({@code governance/modules/FIN/packages/backend-test/RULE-SCENARIOS.md} and
 * {@code API-SCENARIOS.md}) that the api-verify HTTP run cannot reach: they all need a fiscal year
 * every one of whose periods is HARD_CLOSE <i>and</i> an account carrying
 * {@code FIN_ACCOUNT.IS_RETAINED_EARNINGS_FL}. The marker is READ-ONLY on every FIN endpoint
 * (DBF-FIN-147 — API-FIN-002/003 cannot set it), so no HTTP client can build the precondition, and
 * TC-FIN-086 additionally has to observe a database index reject a second marked row.
 *
 * <p><b>TC-FIN-044</b> (FIN registers itself into SEC at onboarding) used to live here too. It now
 * lives in {@code com.erp.sec.FinRegistrationInSecIntegrationTest}: its assertions read SEC's own
 * registry repositories, which {@code com.erp.architecture.CrossModuleBoundaryArchTest
 * #modules_only_expose_their_crossmodule_package_to_outsiders} forbids {@code com.erp.fin} from
 * touching. Do not move it back.
 *
 * <p>This is the year-end half of FIN's in-process coverage; {@link FinSoDCoverageIntegrationTest}
 * keeps the separation-of-duties half (TC-FIN-038 / 060 / 061). The two share one fixture builder,
 * {@link FinYearEndFixtures}. Same conventions as the house precedent
 * ({@code com.erp.sec.SecCoverageIntegrationTest}): the real dev Postgres/Redis under the
 * {@code dev} profile, a class-level {@link Transactional} that rolls every write back, and no
 * HTTP layer — the returned {@link ServiceResult}, the thrown {@link LocalizedException} and the
 * persisted rows are the observable outcome.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class FinYearEndCoverageIntegrationTest {

    /**
     * Far enough ahead of every fiscal year the dev database carries — and of
     * {@link FinSoDCoverageIntegrationTest}'s own 2190 range — that the fixture's two years and
     * {@code FiscalYearService.successorOf}'s {@code findByStartDate} adjacency lookup cannot
     * collide with existing rows.
     */
    private static final LocalDate FIXTURE_YEAR_START = LocalDate.of(2200, 1, 1);

    @Autowired
    private FiscalYearService fiscalYearService;
    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private FiscalYearRepository fiscalYearRepository;
    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private JournalEntryRepository journalEntryRepository;
    @Autowired
    private JournalLineRepository journalLineRepository;

    @Autowired
    private Validator validator;

    private FinYearEndFixtures fixtures;

    @BeforeEach
    void buildFixtureSupport() {
        fixtures = new FinYearEndFixtures(
            fiscalYearRepository, fiscalPeriodRepository, accountRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================================
    // TC-FIN-036 — run year-end close over a hard-closed year WITH posted activity
    // =========================================================================================

    @Test
    void yearEndClose_postsABalancedClosingAndABalancedOpeningEntryFromTheYearsBalances() {
        // Covers: TC-FIN-036, AC-FIN-036 / REQ-FIN-036, API-FIN-027.
        //
        // Test data line: "fully hard-closed fiscal year WITH POSTED ACTIVITY". The activity is
        // posted first, through API-FIN-019's own service, while the periods are still OPEN; only
        // then are they hard-closed and the close run. A dormant year would also close (that is
        // TC-FIN-102) but would prove nothing about §12.10's continuity guarantee, so every
        // assertion below is on non-zero money.
        PostedFixture posted = yearWithPostedActivity();

        setCloseApprover("tc-fin-036");
        ServiceResult<YearEndCloseResponse> result =
            fiscalYearService.yearEndClose(posted.fixture().year().getFiscalYearPk());

        assertThat(result.getStatus()).isEqualTo(Status.CREATED);
        YearEndCloseResponse response = result.getData();
        assertThat(response).isNotNull();

        // ---- the CLOSING entry: result accounts → Retained Earnings, balanced and non-zero ----
        JournalEntryResponse closing = response.getClosingEntry();
        assertThat(closing).isNotNull();
        assertThat(closing.getJournalTypeCode()).isEqualTo(JournalEntryDomain.JOURNAL_TYPE_CLOSING);
        assertThat(closing.getFiscalYearId())
            .isEqualTo(posted.fixture().year().getFiscalYearPk());
        assertBalancedAndNonZero(closing, "closing entry");

        // Each result account is zeroed in the direction opposite to its own net, and the whole
        // result sits on the Retained Earnings line — that IS "result accounts → Retained
        // Earnings", asserted line by line rather than assumed.
        assertSingleLine(closing, posted.revenueAccount(),
            REVENUE_AMOUNT, JournalEntryDomain.DIRECTION_DEBIT);
        assertSingleLine(closing, posted.expenseAccount(),
            EXPENSE_AMOUNT, JournalEntryDomain.DIRECTION_CREDIT);
        assertSingleLine(closing, posted.retainedEarningsAccount(),
            REVENUE_AMOUNT.subtract(EXPENSE_AMOUNT), JournalEntryDomain.DIRECTION_CREDIT);
        assertThat(closing.getLines()).hasSize(3);

        // ---- the OPENING entry: next year's carried-forward balances, balanced and non-zero ----
        JournalEntryResponse opening = response.getOpeningEntry();
        assertThat(opening).isNotNull();
        assertThat(opening.getJournalTypeCode()).isEqualTo(JournalEntryDomain.JOURNAL_TYPE_OPENING);
        assertThat(opening.getFiscalYearId())
            .isEqualTo(posted.fixture().successor().getFiscalYearPk());
        assertBalancedAndNonZero(opening, "opening entry");

        // "generated from the resulting balances": the asset keeps its own side, and Retained
        // Earnings carries forward exactly what the closing entry just put there.
        assertSingleLine(opening, posted.assetAccount(),
            REVENUE_AMOUNT.subtract(EXPENSE_AMOUNT), JournalEntryDomain.DIRECTION_DEBIT);
        assertSingleLine(opening, posted.retainedEarningsAccount(),
            REVENUE_AMOUNT.subtract(EXPENSE_AMOUNT), JournalEntryDomain.DIRECTION_CREDIT);
        assertThat(opening.getLines()).hasSize(2);

        // No result account may be carried forward — POL-FIN-010's other half.
        assertThat(opening.getLines())
            .noneMatch(line -> line.getAccountId()
                .equals(posted.revenueAccount().getAccountPk()))
            .noneMatch(line -> line.getAccountId()
                .equals(posted.expenseAccount().getAccountPk()));
    }

    // =========================================================================================
    // TC-FIN-056 — RULE-FIN-008's year-end carve-out, and its limit
    // =========================================================================================

    @Test
    void yearEndEntriesAreExemptFromThePeriodGate_butACallerSuppliedClosingTypeIsNot() {
        // Covers: TC-FIN-056, AC-FIN-036 / REQ-FIN-036, RULE-FIN-008 (year-end carve-out).
        PostedFixture posted = yearWithPostedActivity();
        FiscalPeriod lastPeriod = posted.fixture().periods().stream()
            .max(java.util.Comparator.comparing(FiscalPeriod::getPeriodNo))
            .orElseThrow();
        FiscalPeriod successorFirstPeriod = fiscalPeriodRepository
            .findByFiscalYearId(posted.fixture().successor().getFiscalYearPk()).stream()
            .min(java.util.Comparator.comparing(FiscalPeriod::getPeriodNo))
            .orElseThrow();

        // The period the closing entry is about to post into really is HARD_CLOSE — the whole
        // point of the carve-out. Asserted before the run so it cannot be read back after it.
        assertThat(lastPeriod.getStatusCode()).isEqualTo(FiscalPeriod.STATUS_HARD_CLOSE);

        setCloseApprover("tc-fin-056");
        ServiceResult<YearEndCloseResponse> result =
            fiscalYearService.yearEndClose(posted.fixture().year().getFiscalYearPk());

        // (c) neither generated entry was rejected — the call returned CREATED rather than
        // throwing FIN-409-PERIOD-NOT-OPEN.
        assertThat(result.getStatus()).isEqualTo(Status.CREATED);
        YearEndCloseResponse response = result.getData();

        // (a) the CLOSING entry posted INTO the year's last (hard-closed) period.
        assertThat(response.getClosingEntry().getPeriodId())
            .isEqualTo(lastPeriod.getFiscalPeriodPk());
        assertThat(response.getClosingEntry().getStatusCode())
            .isEqualTo(JournalEntry.STATUS_POSTED);

        // (b) the OPENING entry posted into the successor year's FIRST period.
        assertThat(response.getOpeningEntry().getPeriodId())
            .isEqualTo(successorFirstPeriod.getFiscalPeriodPk());
        assertThat(response.getOpeningEntry().getStatusCode())
            .isEqualTo(JournalEntry.STATUS_POSTED);

        // (d) the exemption cannot be bought by a caller-supplied journal type.
        //
        // Two manual attempts into that same hard-closed period, both rejected, and they fail at
        // DIFFERENT gates — which together is a stronger statement than the spec's single one:
        //
        //   d1. journalTypeCode="CLOSING" never even reaches RULE-FIN-008. "CLOSING" is not an
        //       active MDL JOURNAL_TYPE value (V26__fin_mdl_lookup_seed.sql seeds exactly five:
        //       EVENT_GENERATED, MANUAL, RECURRING, ALLOCATION, REVERSAL), so
        //       JournalEntryService.createManual's fail-fast XM-FIN-001 lookup check rejects it
        //       with FIN-400-INVALID-LOOKUP first. The spec line predicts 409
        //       FIN-409-PERIOD-NOT-OPEN here; that is asserted below as what the code ACTUALLY
        //       does, and the difference is reported rather than papered over — see this test's
        //       note in the run report. Either way the entry is refused and nothing is posted.
        //
        //   d2. the same entry with the only journal type a caller may legitimately send,
        //       "MANUAL", is rejected by RULE-FIN-008 itself with FIN-409-PERIOD-NOT-OPEN. This is
        //       the assertion that proves the carve-out is scoped to the generated entries: the
        //       manual path calls checkTargetPeriodOpen(...) — no journalTypeCode argument at all
        //       — so no caller-supplied value can reach the exemption.
        setEntryCreator("tc-fin-056-manual");

        JournalEntryCreateRequest closingTyped = manualEntryRequest(
            posted, lastPeriod, JournalEntryDomain.JOURNAL_TYPE_CLOSING);
        Throwable d1 = catchThrowable(() -> journalEntryService.createManual(closingTyped));
        assertThat(d1).isInstanceOf(LocalizedException.class);
        LocalizedException closingTypedRejection = (LocalizedException) d1;
        assertThat(closingTypedRejection.getErrorCode())
            .isEqualTo(FinErrorCodes.FIN_400_INVALID_LOOKUP);
        assertThat(closingTypedRejection.getStatus()).isEqualTo(Status.VALIDATION_ERROR);

        JournalEntryCreateRequest manualTyped = manualEntryRequest(posted, lastPeriod, "MANUAL");
        Throwable d2 = catchThrowable(() -> journalEntryService.createManual(manualTyped));
        assertThat(d2).isInstanceOf(LocalizedException.class);
        LocalizedException manualRejection = (LocalizedException) d2;
        assertThat(manualRejection.getStatus()).isEqualTo(Status.CONFLICT);
        assertThat(manualRejection.getErrorCode())
            .isEqualTo(FinErrorCodes.FIN_409_PERIOD_NOT_OPEN);
        assertThat(manualRejection.getErrors())
            .extracting(com.erp.common.exception.ErrorDetail::errorCode)
            .containsExactly(FinErrorCodes.FIN_409_PERIOD_NOT_OPEN);
    }

    // =========================================================================================
    // TC-FIN-086 — Retained Earnings resolved from the marker; its absence reported
    // =========================================================================================

    @Test
    void yearEndClose_resolvesRetainedEarningsFromTheMarkerAndReportsItsAbsence() {
        // Covers: TC-FIN-086 steps 1 and 2 (step 3 is the next test — a constraint violation
        // poisons this transaction, so it cannot share one with assertions that follow it).
        PostedFixture posted = yearWithPostedActivity(RetainedEarningsMarker.NONE);
        Long fiscalYearPk = posted.fixture().year().getFiscalYearPk();

        // ---- step 1: NO account marked → 404 FIN-404-ACCOUNT and nothing posted ----
        long entriesBefore = countEntriesIn(fiscalYearPk);

        setCloseApprover("tc-fin-086");
        Throwable thrown = catchThrowable(() -> fiscalYearService.yearEndClose(fiscalYearPk));

        assertThat(thrown).isInstanceOf(LocalizedException.class);
        LocalizedException absence = (LocalizedException) thrown;
        assertThat(absence.getErrorCode()).isEqualTo(FinErrorCodes.FIN_404_ACCOUNT);
        assertThat(absence.getStatus()).isEqualTo(Status.NOT_FOUND);

        // "and nothing posted" — no CLOSING/OPENING entry appeared in either year, and neither
        // the year nor its periods moved.
        assertThat(countEntriesIn(fiscalYearPk)).isEqualTo(entriesBefore);
        assertThat(countEntriesIn(posted.fixture().successor().getFiscalYearPk())).isZero();
        assertThat(fiscalYearRepository.findById(fiscalYearPk).orElseThrow().getStatusCode())
            .isEqualTo(FiscalYear.STATUS_OPEN);
        assertThat(fiscalPeriodRepository.findByFiscalYearId(fiscalYearPk))
            .isNotEmpty()
            .allSatisfy(period ->
                assertThat(period.getStatusCode()).isEqualTo(FiscalPeriod.STATUS_HARD_CLOSE));

        // ---- step 2: mark ONE EQUITY account and re-run → success ----
        Account retainedEarnings =
            fixtures.persistAccount("EQUITY", JournalEntryDomain.DIRECTION_CREDIT, true);

        ServiceResult<YearEndCloseResponse> result =
            fiscalYearService.yearEndClose(fiscalYearPk);
        assertThat(result.getStatus()).isEqualTo(Status.CREATED);

        JournalEntryResponse closing = result.getData().getClosingEntry();

        // "which is the only one credited/debited by the CLOSING entry": every other line on the
        // entry belongs to a RESULT account being zeroed, so the marked account is the single
        // non-result account the year's result lands on.
        List<Long> nonResultAccountIds = closing.getLines().stream()
            .map(JournalLineResponse::getAccountId)
            .filter(accountId -> !AccountDomain.isResultAccountType(
                accountRepository.findById(accountId).orElseThrow().getAccountTypeCode()))
            .distinct()
            .toList();
        assertThat(nonResultAccountIds).containsExactly(retainedEarnings.getAccountPk());

        // "every result account's balance closes into the marked account": the whole result net
        // sits on its single line.
        assertSingleLine(closing, retainedEarnings,
            REVENUE_AMOUNT.subtract(EXPENSE_AMOUNT), JournalEntryDomain.DIRECTION_CREDIT);

        // "and the result accounts stand at zero afterwards" — recomputed from the year's POSTED
        // lines, the same way FiscalYearService derives a balance (no stored balance column).
        Map<Long, BigDecimal> nets = signedNetsIn(fiscalYearPk);
        assertThat(nets.get(posted.revenueAccount().getAccountPk()))
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nets.get(posted.expenseAccount().getAccountPk()))
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nets.get(retainedEarnings.getAccountPk()))
            .isEqualByComparingTo(EXPENSE_AMOUNT.subtract(REVENUE_AMOUNT));
    }

    @Test
    void aSecondRetainedEarningsMarkerIsRejectedByTheUniqueIndex() {
        // Covers: TC-FIN-086 step 3 — "the index rejects the second marked row, so the account can
        // never be ambiguous". UQ_FIN_ACCOUNT_RETAINED_EARNINGS is a PARTIAL unique index, so the
        // refusal comes from the database at flush time, not from any Java guard: the failure is
        // asserted, never swallowed. It gets a test method of its own because a constraint
        // violation leaves the persistence context unusable for anything that would follow it.
        fixtures.clearRetainedEarningsMarkers();
        Account first =
            fixtures.persistAccount("EQUITY", JournalEntryDomain.DIRECTION_CREDIT, true);
        accountRepository.flush();
        assertThat(accountRepository
            .findFirstByIsRetainedEarningsFlTrueOrderByAccountPkAsc())
            .get()
            .extracting(Account::getAccountPk)
            .isEqualTo(first.getAccountPk());

        Account second = Account.builder()
            .code("A" + FinYearEndFixtures.uniqueSuffix())
            .nameAr("حساب اختبار ثانٍ")
            .nameEn("Second test account")
            .accountTypeCode("EQUITY")
            .natureCode(JournalEntryDomain.DIRECTION_CREDIT)
            .isLeafFl(Boolean.TRUE)
            .isActiveFl(Boolean.TRUE)
            .isRetainedEarningsFl(Boolean.TRUE)
            .build();

        Throwable rejection = catchThrowable(() -> accountRepository.saveAndFlush(second));

        assertThat(rejection)
            .as("UQ_FIN_ACCOUNT_RETAINED_EARNINGS must refuse a second marked account")
            .isInstanceOf(DataIntegrityViolationException.class);
        // Postgres reports the index by its own folded-lowercase name, so the identity of the
        // refusing constraint is matched case-insensitively — but it IS matched: a violation of
        // some other constraint would not satisfy this case.
        String message = rejection.getMessage().toUpperCase(Locale.ROOT);
        assertThat(message).contains("UQ_FIN_ACCOUNT_RETAINED_EARNINGS");
        assertThat(message).contains("DUPLICATE KEY VALUE VIOLATES UNIQUE CONSTRAINT");
    }

    // =========================================================================================
    // V34 seed — the deployed database itself satisfies DBF-FIN-147
    // =========================================================================================

    @Test
    void yearEndClose_resolvesTheSeededRetainedEarningsAccountWithNoFixtureMarker() {
        // Covers V34__fin_account_retained_earnings_seed.sql. Every other case here MARKS an
        // account first, so all of them would still pass on a database where nothing is marked —
        // which is what every deployment was until V34, and why API-FIN-027 could only ever answer
        // FIN-404-ACCOUNT there. This case marks nothing and clears nothing.
        assertThat(markedAccountCount())
            .as("V34 must leave exactly one marked account (UQ_FIN_ACCOUNT_RETAINED_EARNINGS)")
            .isEqualTo(1L);

        Account seeded = accountRepository
            .findFirstByIsRetainedEarningsFlTrueOrderByAccountPkAsc().orElseThrow();
        assertThat(seeded.getAccountTypeCode()).isEqualTo("EQUITY");
        assertThat(seeded.getNatureCode())
            .isEqualToIgnoringCase(JournalEntryDomain.DIRECTION_CREDIT);
        assertThat(seeded.getIsLeafFl()).isTrue();
        assertThat(seeded.getIsActiveFl()).isTrue();

        PostedFixture posted = yearWithPostedActivity(RetainedEarningsMarker.SEEDED);
        assertThat(posted.retainedEarningsAccount().getAccountPk())
            .isEqualTo(seeded.getAccountPk());

        setCloseApprover("v34-seed");
        ServiceResult<YearEndCloseResponse> result =
            fiscalYearService.yearEndClose(posted.fixture().year().getFiscalYearPk());

        assertThat(result.getStatus()).isEqualTo(Status.CREATED);
        assertSingleLine(result.getData().getClosingEntry(), seeded,
            REVENUE_AMOUNT.subtract(EXPENSE_AMOUNT), JournalEntryDomain.DIRECTION_CREDIT);
    }

    // =========================================================================================
    // TC-FIN-102 — a dormant year still closes, posting two EMPTY entries
    // =========================================================================================

    @Test
    void dormantYearEndClose_postsTwoEmptyEntriesAndStillClosesTheYear() {
        // Covers: TC-FIN-102 steps 1 and 2. No journal entry is ever posted into this year, so
        // neither a result nor a balance-sheet account carries a non-zero net and both line
        // builders skip everything — RULE-FIN-006 treats 0 = 0 as balanced.
        YearEndFixture fixture = fixtures.eligibleYearEndFixture(FIXTURE_YEAR_START);
        Long fiscalYearPk = fixture.year().getFiscalYearPk();
        Long successorPk = fixture.successor().getFiscalYearPk();
        assertThat(countEntriesIn(fiscalYearPk)).isZero();
        assertThat(countEntriesIn(successorPk)).isZero();

        setCloseApprover("tc-fin-102");
        ServiceResult<YearEndCloseResponse> result = fiscalYearService.yearEndClose(fiscalYearPk);

        // ---- step 1: 201 with BOTH entries, each with an EMPTY line set ----
        assertThat(result.getStatus()).isEqualTo(Status.CREATED);
        YearEndCloseResponse response = result.getData();
        JournalEntryResponse closing = response.getClosingEntry();
        JournalEntryResponse opening = response.getOpeningEntry();

        assertThat(closing).isNotNull();
        assertThat(closing.getJournalTypeCode()).isEqualTo(JournalEntryDomain.JOURNAL_TYPE_CLOSING);
        assertThat(closing.getLines()).isEmpty();
        assertThat(opening).isNotNull();
        assertThat(opening.getJournalTypeCode()).isEqualTo(JournalEntryDomain.JOURNAL_TYPE_OPENING);
        assertThat(opening.getLines()).isEmpty();

        // "each consuming a docNo from its OWN fiscal year's sequence" — JV-{fiscalYearCode}-NNNNNN
        // (JournalDocNoGenerator), so the prefix names the year the entry belongs to and the two
        // entries carry DIFFERENT year codes.
        assertThat(closing.getDocNo()).startsWith("JV-" + fixture.year().getCode() + "-");
        assertThat(closing.getFiscalYearId()).isEqualTo(fiscalYearPk);
        assertThat(opening.getDocNo()).startsWith("JV-" + fixture.successor().getCode() + "-");
        assertThat(opening.getFiscalYearId()).isEqualTo(successorPk);
        assertThat(closing.getDocNo()).isNotEqualTo(opening.getDocNo());

        // "the year becomes CLOSED and every period YEAR_END_CLOSE".
        assertThat(fiscalYearRepository.findById(fiscalYearPk).orElseThrow().getStatusCode())
            .isEqualTo(FiscalYear.STATUS_CLOSED);
        assertThat(fiscalPeriodRepository.findByFiscalYearId(fiscalYearPk))
            .isNotEmpty()
            .allSatisfy(period -> assertThat(period.getStatusCode())
                .isEqualTo(FiscalPeriod.STATUS_YEAR_END_CLOSE));

        // ---- step 2: both read back POSTED with zero lines ----
        // API-FIN-022's own read query (QR-FIN-037, JournalEntryRepository.findOneWithLines) —
        // the LEFT JOIN FETCH the read endpoint uses, so an entry with no lines is genuinely
        // returned rather than filtered away.
        for (JournalEntryResponse entry : List.of(closing, opening)) {
            JournalEntry readBack = journalEntryRepository
                .findOneWithLines(entry.getJournalEntryPk()).orElseThrow();
            assertThat(readBack.getStatusCode()).isEqualTo(JournalEntry.STATUS_POSTED);
            assertThat(readBack.getPostedAt()).isNotNull();
            assertThat(readBack.getLines()).isEmpty();
            assertThat(journalLineRepository.findByJournalEntryPk(entry.getJournalEntryPk()))
                .isEmpty();
        }
    }

    @Test
    void aManualEntryWithNoLinesIsRejectedByTheRequestContract() {
        // Covers: TC-FIN-102 step 3 — "400 {code: VALIDATION_ERROR} with a fieldErrors entry for
        // `lines` (the request contract's own @NotEmpty), which is what makes the empty-entry
        // shape reachable ONLY through the internal year-end path and never from a caller".
        //
        // @NotEmpty fires from @Valid at the MVC layer, NOT on a direct service call, so this
        // suite (which has no HTTP layer by design) exercises the constraint through the same
        // jakarta.validation.Validator Spring hands the MVC argument resolver. That is the same
        // constraint, evaluated by the same engine, on the same request object; what is NOT
        // asserted here is GlobalExceptionHandler's rendering of the resulting
        // MethodArgumentNotValidException into {code: "VALIDATION_ERROR", fieldErrors: [...]},
        // which is shared platform behaviour and not FIN's.
        JournalEntryCreateRequest request = JournalEntryCreateRequest.builder()
            .docDate(FIXTURE_YEAR_START)
            .fiscalYearId(1L)
            .periodId(1L)
            .journalTypeCode("MANUAL")
            .lines(List.of())
            .build();

        Set<ConstraintViolation<JournalEntryCreateRequest>> violations =
            validator.validate(request);

        assertThat(violations)
            .as("an empty line set must be refused by the request contract itself")
            .isNotEmpty();
        assertThat(violations)
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactly("lines");
        assertThat(violations)
            .allSatisfy(violation -> assertThat(violation.getConstraintDescriptor()
                .getAnnotation().annotationType())
                .isEqualTo(jakarta.validation.constraints.NotEmpty.class));

        // The same request with one line clears the contract, so the violation above is the empty
        // line set and nothing else about the request shape.
        request.setLines(List.of(JournalLineCreateRequest.builder()
            .accountId(1L)
            .amount(BigDecimal.ONE)
            .directionCode(JournalEntryDomain.DIRECTION_DEBIT)
            .build()));
        assertThat(validator.validate(request)).isEmpty();
    }

    // =========================================================================================
    // Fixture and assertion helpers
    // =========================================================================================

    private static final BigDecimal REVENUE_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal EXPENSE_AMOUNT = new BigDecimal("400.00");

    /**
     * The year-with-activity fixture TC-FIN-036 / 056 / 086 share. Real entries are posted through
     * API-FIN-019's own service while the periods are still OPEN — that is the only way a period's
     * balances can exist at all, since RULE-FIN-008 refuses a manual entry into a closed one — and
     * only then are the periods hard-closed.
     *
     * <p>The resulting year: revenue 1000 CREDIT / asset 1000 DEBIT, then expense 400 DEBIT /
     * asset 400 CREDIT. Signed nets: asset +600, revenue −1000, expense +400 — so the result set
     * nets to −600 and no line of either generated entry can be zero.
     */
    private PostedFixture yearWithPostedActivity() {
        return yearWithPostedActivity(RetainedEarningsMarker.FIXTURE);
    }

    /** Where {@link #yearWithPostedActivity(RetainedEarningsMarker)} gets DBF-FIN-147 from. */
    private enum RetainedEarningsMarker { SEEDED, FIXTURE, NONE }

    private PostedFixture yearWithPostedActivity(RetainedEarningsMarker marker) {
        YearEndFixture fixture = fixtures.openYearEndFixture(FIXTURE_YEAR_START);
        FiscalPeriod firstPeriod = fixture.periods().get(0);

        Account asset = fixtures.persistAccount("ASSET", JournalEntryDomain.DIRECTION_DEBIT);
        Account revenue = fixtures.persistAccount(
            AccountDomain.ACCOUNT_TYPE_REVENUE, JournalEntryDomain.DIRECTION_CREDIT);
        Account expense = fixtures.persistAccount(
            AccountDomain.ACCOUNT_TYPE_EXPENSE, JournalEntryDomain.DIRECTION_DEBIT);

        Account retainedEarnings;
        if (marker == RetainedEarningsMarker.SEEDED) {
            retainedEarnings = accountRepository
                .findFirstByIsRetainedEarningsFlTrueOrderByAccountPkAsc().orElse(null);
        } else {
            fixtures.clearRetainedEarningsMarkers();
            retainedEarnings = marker == RetainedEarningsMarker.FIXTURE
                ? fixtures.persistAccount("EQUITY", JournalEntryDomain.DIRECTION_CREDIT, true)
                : null;
        }

        setEntryCreator("tc-fin-fixture");
        postManualEntry(fixture.year(), firstPeriod,
            line(asset, REVENUE_AMOUNT, JournalEntryDomain.DIRECTION_DEBIT),
            line(revenue, REVENUE_AMOUNT, JournalEntryDomain.DIRECTION_CREDIT));
        postManualEntry(fixture.year(), firstPeriod,
            line(expense, EXPENSE_AMOUNT, JournalEntryDomain.DIRECTION_DEBIT),
            line(asset, EXPENSE_AMOUNT, JournalEntryDomain.DIRECTION_CREDIT));

        // The activity really did land, and the year is genuinely non-dormant.
        Map<Long, BigDecimal> nets = signedNetsIn(fixture.year().getFiscalYearPk());
        assertThat(nets.get(asset.getAccountPk()))
            .isEqualByComparingTo(REVENUE_AMOUNT.subtract(EXPENSE_AMOUNT));
        assertThat(nets.get(revenue.getAccountPk()))
            .isEqualByComparingTo(REVENUE_AMOUNT.negate());
        assertThat(nets.get(expense.getAccountPk())).isEqualByComparingTo(EXPENSE_AMOUNT);

        fixtures.hardClose(fixture.periods());
        SecurityContextHolder.clearContext();

        return new PostedFixture(fixture, asset, revenue, expense, retainedEarnings);
    }

    private void postManualEntry(FiscalYear year, FiscalPeriod period,
                                 JournalLineCreateRequest... lines) {
        ServiceResult<JournalEntryResponse> posted =
            journalEntryService.createManual(JournalEntryCreateRequest.builder()
                .docDate(period.getStartDate())
                .fiscalYearId(year.getFiscalYearPk())
                .periodId(period.getFiscalPeriodPk())
                .journalTypeCode("MANUAL")
                .descriptionEn("Year-end coverage fixture activity")
                .lines(List.of(lines))
                .build());
        assertThat(posted.getStatus()).isEqualTo(Status.CREATED);
        assertThat(posted.getData().getStatusCode()).isEqualTo(JournalEntry.STATUS_POSTED);
    }

    /** A balanced two-line manual entry aimed at the given period — used only to be refused. */
    private JournalEntryCreateRequest manualEntryRequest(PostedFixture posted,
                                                         FiscalPeriod period,
                                                         String journalTypeCode) {
        return JournalEntryCreateRequest.builder()
            .docDate(period.getStartDate())
            .fiscalYearId(posted.fixture().year().getFiscalYearPk())
            .periodId(period.getFiscalPeriodPk())
            .journalTypeCode(journalTypeCode)
            .descriptionEn("Attempt into a hard-closed period")
            .lines(List.of(
                line(posted.expenseAccount(), BigDecimal.TEN,
                    JournalEntryDomain.DIRECTION_DEBIT),
                line(posted.assetAccount(), BigDecimal.TEN,
                    JournalEntryDomain.DIRECTION_CREDIT)))
            .build();
    }

    private static JournalLineCreateRequest line(Account account, BigDecimal amount,
                                                 String directionCode) {
        return JournalLineCreateRequest.builder()
            .accountId(account.getAccountPk())
            .amount(amount)
            .directionCode(directionCode)
            .build();
    }

    /**
     * RULE-FIN-006 asserted for real: total debits equal total credits, AND the entry actually
     * moves money. TC-FIN-036 is the case that proves the close moves money, so a zero-line or
     * zero-amount entry must not satisfy it (that shape belongs to TC-FIN-102).
     */
    private void assertBalancedAndNonZero(JournalEntryResponse entry, String label) {
        assertThat(entry.getLines()).as(label + " must have lines").isNotEmpty();

        BigDecimal debits = sideTotal(entry, JournalEntryDomain.DIRECTION_DEBIT);
        BigDecimal credits = sideTotal(entry, JournalEntryDomain.DIRECTION_CREDIT);
        assertThat(debits).as(label + " debits must equal credits")
            .isEqualByComparingTo(credits);
        assertThat(debits).as(label + " must be non-zero")
            .isGreaterThan(BigDecimal.ZERO);
        assertThat(entry.getLines()).allSatisfy(line ->
            assertThat(line.getAmount()).isGreaterThan(BigDecimal.ZERO));
    }

    private static BigDecimal sideTotal(JournalEntryResponse entry, String directionCode) {
        return entry.getLines().stream()
            .filter(line -> directionCode.equalsIgnoreCase(line.getDirectionCode()))
            .map(JournalLineResponse::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * The entry must carry EXACTLY ONE line on the given account, for the given amount, on the
     * given side. Amount is compared by value ({@code isEqualByComparingTo}) so a scale difference
     * between {@code 600} and {@code 600.00} cannot make or break the assertion.
     */
    private static void assertSingleLine(JournalEntryResponse entry, Account account,
                                         BigDecimal amount, String directionCode) {
        List<JournalLineResponse> lines = entry.getLines().stream()
            .filter(line -> account.getAccountPk().equals(line.getAccountId()))
            .toList();
        assertThat(lines)
            .as("exactly one line on account " + account.getCode())
            .hasSize(1);
        JournalLineResponse line = lines.get(0);
        assertThat(line.getAmount())
            .as("amount on account " + account.getCode())
            .isEqualByComparingTo(amount);
        assertThat(line.getDirectionCode())
            .as("side on account " + account.getCode())
            .isEqualToIgnoringCase(directionCode);
    }

    /**
     * Every account's signed net (debits minus credits) over one fiscal year's POSTED lines — the
     * same derivation {@code FiscalYearService.balancesOf} performs, recomputed independently here
     * so the assertions do not borrow the code under test.
     */
    private Map<Long, BigDecimal> signedNetsIn(Long fiscalYearPk) {
        Specification<JournalLine> postedInYear = (root, query, cb) -> cb.and(
            cb.equal(root.get("journalEntry").get("fiscalYear").get("fiscalYearPk"), fiscalYearPk),
            cb.equal(root.get("journalEntry").get("statusCode"), JournalEntry.STATUS_POSTED));

        Map<Long, BigDecimal> nets = new LinkedHashMap<>();
        for (JournalLine line : journalLineRepository.findAll(postedInYear)) {
            BigDecimal signed = JournalEntryDomain.DIRECTION_DEBIT
                .equalsIgnoreCase(line.getDirectionCode())
                ? line.getAmount()
                : line.getAmount().negate();
            nets.merge(line.getAccount().getAccountPk(), signed, BigDecimal::add);
        }
        return nets;
    }

    private long markedAccountCount() {
        return accountRepository.count((Specification<Account>) (root, query, cb) ->
            cb.isTrue(root.get("isRetainedEarningsFl")));
    }

    private long countEntriesIn(Long fiscalYearPk) {
        return journalEntryRepository.count((Specification<JournalEntry>) (root, query, cb) ->
            cb.equal(root.get("fiscalYear").get("fiscalYearPk"), fiscalYearPk));
    }

    private void setCloseApprover(String usernamePrefix) {
        FinYearEndFixtures.setAuthenticatedPrincipal(
            usernamePrefix + "-" + FinYearEndFixtures.uniqueSuffix(),
            PermissionConstants.PERM_FIN_PERIODS_CLOSE_APPROVE);
    }

    private void setEntryCreator(String usernamePrefix) {
        FinYearEndFixtures.setAuthenticatedPrincipal(
            usernamePrefix + "-" + FinYearEndFixtures.uniqueSuffix(),
            PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE);
    }

    /** The fixture's year together with the accounts its posted activity used. */
    private record PostedFixture(YearEndFixture fixture, Account assetAccount,
                                 Account revenueAccount, Account expenseAccount,
                                 Account retainedEarningsAccount) {
    }
}
