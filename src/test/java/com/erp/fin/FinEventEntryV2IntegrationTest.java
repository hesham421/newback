package com.erp.fin;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.fin.FinYearEndFixtures.YearEndFixture;
import com.erp.fin.domain.AccountDomain;
import com.erp.fin.domain.AllocationRuleDomain;
import com.erp.fin.domain.EventTypeRuleDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.DimensionReportResponse;
import com.erp.fin.dto.DimensionReportRowResponse;
import com.erp.fin.dto.EventEntryBuildRequest;
import com.erp.fin.dto.JournalEntryCreateRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineCreateRequest;
import com.erp.fin.dto.JournalLineDimensionCreateRequest;
import com.erp.fin.dto.JournalLineResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.AccountMapping;
import com.erp.fin.entity.Dimension;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.EventTypeRule;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.JournalLine;
import com.erp.fin.entity.JournalLineDimension;
import com.erp.fin.entity.RuleLine;
import com.erp.fin.entity.RuleLineDimension;
import com.erp.fin.repository.AccountMappingRepository;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.DimensionRepository;
import com.erp.fin.repository.DimensionValueRepository;
import com.erp.fin.repository.EventTypeRuleRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.repository.JournalEntryRepository;
import com.erp.fin.repository.JournalLineDimensionRepository;
import com.erp.fin.repository.JournalLineRepository;
import com.erp.fin.repository.RuleLineDimensionRepository;
import com.erp.fin.repository.RuleLineRepository;
import com.erp.fin.service.EventEntryService;
import com.erp.fin.service.JournalEntryService;
import com.erp.fin.service.JournalPostingService;
import com.erp.fin.service.ReportService;
import com.erp.main.ErpMainApplication;
import com.erp.sec.permission.PermissionConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * FIN v2 cases whose acceptance criteria are stated as literal money — TC-FIN-012, TC-FIN-128,
 * TC-FIN-122 and TC-FIN-129. The figures (100.00 · 33% · 66.00 · 34.00 · 200.00 · "BR-07") live
 * only in the AC prose, so `api-verify` may not assert them; here they are asserted as written.
 * Same conventions as {@link FinYearEndCoverageIntegrationTest}: the real dev Postgres under the
 * {@code dev} profile and a class-level {@link Transactional} that rolls every write back.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class FinEventEntryV2IntegrationTest {

    /** Far enough ahead of every fiscal year the dev database and the sibling suites carry. */
    private static final LocalDate FIXTURE_YEAR_START = LocalDate.of(2220, 1, 1);

    private static final BigDecimal EVENT_AMOUNT = new BigDecimal("100.00");
    private static final String PERCENT_33 = "33";
    private static final BigDecimal PERCENT_LINE_AMOUNT = new BigDecimal("33.00");
    private static final BigDecimal PERCENT_LINES_TOTAL = new BigDecimal("66.00");
    private static final BigDecimal REMAINDER_AMOUNT = new BigDecimal("34.00");

    private static final String BRANCH_DIMENSION_CODE = "BRANCH";
    private static final String BRANCH_VALUE_CODE = "BR-07";
    private static final String TAGGED_ACCOUNT_CODE = "5100";
    private static final BigDecimal TAGGED_LINE_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal TAGGED_DEBIT_TOTAL = new BigDecimal("200.00");

    /**
     * DISTRIBUTION_TYPE (SRS A6: FIXED, PERCENTAGE, REMAINDER). A line set with no remainder line
     * may not declare PERCENTAGE distribution — RULE-FIN-003 then demands exactly one remainder
     * (FIN-409-REMAINDER-COUNT), so the two-line rules below distribute FIXED while still sourcing
     * their amount as a percentage of the event (AMOUNT_SOURCE_TYPE, a different code column).
     */
    private static final String DISTRIBUTION_TYPE_FIXED = "FIXED";

    private static final String PAYMENT_METHOD_FIELD = "PAYMENT_METHOD";
    private static final String CHEQUE = "CHEQUE";

    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountMappingRepository accountMappingRepository;
    @Autowired private DimensionRepository dimensionRepository;
    @Autowired private DimensionValueRepository dimensionValueRepository;
    @Autowired private EventTypeRuleRepository eventTypeRuleRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private JournalLineRepository journalLineRepository;
    @Autowired private JournalLineDimensionRepository journalLineDimensionRepository;
    @Autowired private RuleLineRepository ruleLineRepository;
    @Autowired private RuleLineDimensionRepository ruleLineDimensionRepository;

    @Autowired private EventEntryService eventEntryService;
    @Autowired private JournalEntryService journalEntryService;
    @Autowired private ReportService reportService;

    private FinYearEndFixtures fixtures;
    private YearEndFixture fixture;
    private FiscalPeriod openPeriod;

    @BeforeEach
    void setUp() {
        fixtures = new FinYearEndFixtures(
            fiscalYearRepository, fiscalPeriodRepository, accountRepository);
        fixture = fixtures.openYearEndFixture(FIXTURE_YEAR_START);
        openPeriod = fixture.periods().get(0);
        assertThat(openPeriod.getStatusCode()).isEqualTo(FiscalPeriod.STATUS_OPEN);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aThirtyThreePercentPairLeavesExactlyThirtyFourOnTheRemainderLine() {
        // Covers: TC-FIN-012, AC-FIN-012 / REQ-FIN-012, API-FIN-020, RULE-FIN-010.
        //
        // AC-FIN-012 verbatim: "a rule distributing 100.00 as 33% + 33% + remainder … the two
        // percentage lines total 66.00 … and the remainder line is exactly 34.00". A 25/75 split
        // has no residue at all and proves nothing about RULE-FIN-010, which is why these three
        // numbers are asserted literally rather than as "the entry balances".
        Account funding = fixtures.persistAccount("ASSET", JournalEntryDomain.DIRECTION_DEBIT);
        Account firstShare = expenseAccount();
        Account secondShare = expenseAccount();
        Account remainderShare = expenseAccount();

        EventTypeRule rule = persistRule();
        persistConstantLine(rule, 1, funding, JournalEntryDomain.DIRECTION_CREDIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, "100");
        RuleLine first = persistConstantLine(rule, 2, firstShare,
            JournalEntryDomain.DIRECTION_DEBIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, PERCENT_33);
        RuleLine second = persistConstantLine(rule, 3, secondShare,
            JournalEntryDomain.DIRECTION_DEBIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, PERCENT_33);
        persistRemainderLine(rule, 4, remainderShare);

        setEventConsumer();
        JournalEntryResponse entry = buildEvent(rule, "TCFIN012-" + uniqueSuffix(), null);

        assertThat(entry.getStatusCode()).isEqualTo(JournalEntry.STATUS_POSTED);

        BigDecimal firstAmount = amountOn(entry, firstShare);
        BigDecimal secondAmount = amountOn(entry, secondShare);
        assertThat(firstAmount).isEqualByComparingTo(PERCENT_LINE_AMOUNT);
        assertThat(secondAmount).isEqualByComparingTo(PERCENT_LINE_AMOUNT);
        assertThat(firstAmount.add(secondAmount))
            .as("the two percentage lines total 66.00 (AC-FIN-012)")
            .isEqualByComparingTo(PERCENT_LINES_TOTAL);

        assertThat(amountOn(entry, remainderShare))
            .as("the remainder line is exactly 34.00 (AC-FIN-012)")
            .isEqualByComparingTo(REMAINDER_AMOUNT);
        assertThat(remainderMarkedLine(entry).getAccountId())
            .isEqualTo(remainderShare.getAccountPk());

        assertBalancedAt(entry, EVENT_AMOUNT);
        // The rule lines really did carry the AC's percentages, so the 33.00s are computed and
        // not accidental equality with some other source value.
        assertThat(first.getAmountSourceValue()).isEqualTo(PERCENT_33);
        assertThat(second.getAmountSourceValue()).isEqualTo(PERCENT_33);
    }

    @Test
    void aMappedRemainderLineStillLeavesExactlyThirtyFourOnTheMappedAccount() {
        // Covers: TC-FIN-128, AC-FIN-082 / REQ-FIN-076, API-FIN-020, RULE-FIN-010 + RULE-FIN-020.
        //
        // AC-FIN-082 verbatim: "the two percentage lines total 66.00, the remainder line is
        // exactly 34.00 on the mapped account, and the posted entry's debits equal its credits at
        // 100.00". The v2 half is "on the mapped account": the remainder is MAPPING-derived here,
        // so the assertion names the account the AccountMapping resolves, never a constant.
        Account funding = fixtures.persistAccount("ASSET", JournalEntryDomain.DIRECTION_DEBIT);
        Account firstShare = expenseAccount();
        Account secondShare = expenseAccount();
        Account mappedAccount = expenseAccount();

        EventTypeRule rule = persistRule();
        persistConstantLine(rule, 1, funding, JournalEntryDomain.DIRECTION_CREDIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, "100");
        persistConstantLine(rule, 2, firstShare, JournalEntryDomain.DIRECTION_DEBIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, PERCENT_33);
        persistConstantLine(rule, 3, secondShare, JournalEntryDomain.DIRECTION_DEBIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, PERCENT_33);
        persistMappedRemainderLine(rule, 4);
        persistMapping(rule, mappedAccount);

        setEventConsumer();
        JournalEntryResponse entry =
            buildEvent(rule, "EVT-4005", Map.of(PAYMENT_METHOD_FIELD, CHEQUE));

        assertThat(entry.getStatusCode()).isEqualTo(JournalEntry.STATUS_POSTED);
        assertThat(amountOn(entry, firstShare).add(amountOn(entry, secondShare)))
            .as("the two percentage lines total 66.00 (AC-FIN-082)")
            .isEqualByComparingTo(PERCENT_LINES_TOTAL);

        JournalLineResponse remainder = remainderMarkedLine(entry);
        assertThat(remainder.getAccountId())
            .as("the remainder line sits on the MAPPING-resolved account (AC-FIN-082)")
            .isEqualTo(mappedAccount.getAccountPk());
        assertThat(remainder.getAmount())
            .as("the remainder line is exactly 34.00 on the mapped account (AC-FIN-082)")
            .isEqualByComparingTo(REMAINDER_AMOUNT);

        assertBalancedAt(entry, EVENT_AMOUNT);
    }

    @Test
    void theDimensionReportSumsEventBuiltAndManualLinesIntoOneRow() {
        // Covers: TC-FIN-122, AC-FIN-076 / REQ-FIN-071, API-FIN-032.
        //
        // AC-FIN-076 verbatim: "the two entries of AC-FIN-075, each debiting account 5100 with
        // 100.00 under BRANCH 'BR-07' … the system shows 1 row for (5100, 'BR-07') with a debit
        // total of 200.00". One entry is event-built, the other manual — the point is that the
        // report cannot tell them apart.
        Dimension branch = branchDimension();
        DimensionValue br07 = branchValue(branch);
        Account tagged = taggedAccount();
        Account funding = fixtures.persistAccount("ASSET", JournalEntryDomain.DIRECTION_DEBIT);

        setReportViewer();
        assertThat(reportRowFor(branch, tagged, br07))
            .as("(5100, BR-07) must carry no prior posting, or 200.00 would not be the whole total")
            .isNull();

        EventTypeRule rule = persistRule();
        RuleLine debitLine = persistConstantLine(rule, 1, tagged,
            JournalEntryDomain.DIRECTION_DEBIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, "100",
            DISTRIBUTION_TYPE_FIXED);
        persistConstantTag(debitLine, br07);
        persistConstantLine(rule, 2, funding, JournalEntryDomain.DIRECTION_CREDIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, "100",
            DISTRIBUTION_TYPE_FIXED);

        setEventConsumer();
        JournalEntryResponse eventBuilt =
            buildEvent(rule, "EVT-3016", null);
        assertThat(amountOn(eventBuilt, tagged)).isEqualByComparingTo(TAGGED_LINE_AMOUNT);

        postManualTaggedEntry(tagged, funding, br07);

        setReportViewer();
        List<DimensionReportRowResponse> matching = reportRowsFor(branch, tagged, br07);
        assertThat(matching)
            .as("1 row for (5100, BR-07), never one row per journal type (AC-FIN-076)")
            .hasSize(1);
        assertThat(matching.get(0).getDebitTotal())
            .as("a debit total of 200.00 (AC-FIN-076)")
            .isEqualByComparingTo(TAGGED_DEBIT_TOTAL);
        assertThat(matching.get(0).getAccountCode()).isEqualTo(TAGGED_ACCOUNT_CODE);
        assertThat(matching.get(0).getDimensionValueCode()).isEqualTo(BRANCH_VALUE_CODE);
    }

    @Test
    void aMappedAndTaggedEntryReversesAccountsAmountsAndDimensionValuesToZero() {
        // Covers: TC-FIN-129, AC-FIN-083 / REQ-FIN-077, API-FIN-021 (+ API-FIN-028 / API-FIN-032
        // read as balances).
        //
        // AC-FIN-083 verbatim: "the system posts a reversal whose lines carry the same accounts,
        // amounts and dimension values with opposite direction, the original stays POSTED, and
        // every account and BRANCH balance nets to 0.00". The original here carries BOTH a mapped
        // account AND a BRANCH tag — a mapping-only entry has no tags, so it leaves the
        // "and dimension values" half of the Then untested.
        Dimension branch = branchDimension();
        DimensionValue br07 = branchValue(branch);
        Account mappedAccount = expenseAccount();
        Account funding = fixtures.persistAccount("ASSET", JournalEntryDomain.DIRECTION_DEBIT);

        EventTypeRule rule = persistRule();
        RuleLine mappedDebit = persistMappedLine(rule, 1, JournalEntryDomain.DIRECTION_DEBIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, "100",
            DISTRIBUTION_TYPE_FIXED);
        persistConstantTag(mappedDebit, br07);
        RuleLine creditLine = persistConstantLine(rule, 2, funding,
            JournalEntryDomain.DIRECTION_CREDIT,
            EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_PERCENTAGE, "100",
            DISTRIBUTION_TYPE_FIXED);
        persistConstantTag(creditLine, br07);
        persistMapping(rule, mappedAccount);

        setEventConsumer();
        JournalEntryResponse original =
            buildEvent(rule, "EVT-4006", Map.of(PAYMENT_METHOD_FIELD, CHEQUE));
        assertThat(original.getLines())
            .extracting(JournalLineResponse::getAccountId)
            .contains(mappedAccount.getAccountPk());

        FinYearEndFixtures.setAuthenticatedPrincipal("tc-fin-129-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_REVERSE);
        ServiceResult<JournalEntryResponse> reversalResult =
            journalEntryService.reverse(original.getJournalEntryPk());

        assertThat(reversalResult.getStatus()).isEqualTo(Status.CREATED);
        JournalEntryResponse reversal = reversalResult.getData();
        assertThat(reversal.getStatusCode()).isEqualTo(JournalEntry.STATUS_POSTED);
        assertThat(reversal.getJournalTypeCode())
            .isEqualTo(JournalPostingService.JOURNAL_TYPE_REVERSAL);
        assertThat(reversal.getOriginalEntryId()).isEqualTo(original.getJournalEntryPk());

        JournalEntry reloadedOriginal = journalEntryRepository
            .findOneWithLines(original.getJournalEntryPk()).orElseThrow();
        assertThat(reloadedOriginal.getStatusCode()).isEqualTo(JournalEntry.STATUS_POSTED);
        assertThat(reloadedOriginal.getReversalEntry()).isNotNull();
        assertThat(reloadedOriginal.getReversalEntry().getJournalEntryPk())
            .isEqualTo(reversal.getJournalEntryPk());

        // "the same accounts, amounts and dimension values with the opposite direction", asserted
        // line for line rather than by the pair's totals alone.
        Map<Long, BigDecimal> originalSigned = signedNetsOf(original.getJournalEntryPk());
        Map<Long, BigDecimal> reversalSigned = signedNetsOf(reversal.getJournalEntryPk());
        assertThat(reversalSigned.keySet()).isEqualTo(originalSigned.keySet());
        originalSigned.forEach((accountPk, net) ->
            assertThat(reversalSigned.get(accountPk))
                .as("account " + accountPk + " mirrored with the opposite sign")
                .isEqualByComparingTo(net.negate()));

        Map<Long, List<Long>> originalTags = dimensionValueIdsByAccount(
            original.getJournalEntryPk());
        Map<Long, List<Long>> reversalTags = dimensionValueIdsByAccount(
            reversal.getJournalEntryPk());
        assertThat(reversalTags)
            .as("the reversal's lines carry the same dimensionValueIds (AC-FIN-083)")
            .isEqualTo(originalTags);
        assertThat(originalTags.values())
            .allSatisfy(values -> assertThat(values)
                .containsExactly(br07.getDimensionValuePk()));

        // "every account and BRANCH balance nets to 0.00" across the pair.
        Map<Long, BigDecimal> pairAccountNets = new LinkedHashMap<>(originalSigned);
        reversalSigned.forEach((accountPk, net) ->
            pairAccountNets.merge(accountPk, net, BigDecimal::add));
        assertThat(pairAccountNets.values())
            .isNotEmpty()
            .allSatisfy(net -> assertThat(net).isEqualByComparingTo(BigDecimal.ZERO));

        Map<Long, BigDecimal> branchNets = signedBranchNetsOf(
            branch, original.getJournalEntryPk(), reversal.getJournalEntryPk());
        assertThat(branchNets)
            .as("the BRANCH balances the pair touched")
            .containsKey(br07.getDimensionValuePk());
        assertThat(branchNets.values())
            .allSatisfy(net -> assertThat(net).isEqualByComparingTo(BigDecimal.ZERO));
    }

    private JournalEntryResponse buildEvent(EventTypeRule rule, String eventReference,
                                            Map<String, String> fields) {
        ServiceResult<JournalEntryResponse> result =
            eventEntryService.build(EventEntryBuildRequest.builder()
                .eventReference(eventReference)
                .eventTypeCode(rule.getEventTypeCode())
                .docDate(openPeriod.getStartDate())
                .baseAmount(EVENT_AMOUNT)
                .fields(fields)
                .descriptionEn("FIN v2 coverage event")
                .build());
        assertThat(result.getStatus()).isEqualTo(Status.CREATED);
        return result.getData();
    }

    private void postManualTaggedEntry(Account debitAccount, Account creditAccount,
                                       DimensionValue value) {
        FinYearEndFixtures.setAuthenticatedPrincipal("tc-fin-122-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE);
        JournalLineDimensionCreateRequest tag = JournalLineDimensionCreateRequest.builder()
            .dimensionId(value.getDimension().getDimensionPk())
            .dimensionValueId(value.getDimensionValuePk())
            .build();
        ServiceResult<JournalEntryResponse> posted =
            journalEntryService.createManual(JournalEntryCreateRequest.builder()
                .docDate(openPeriod.getStartDate())
                .fiscalYearId(fixture.year().getFiscalYearPk())
                .periodId(openPeriod.getFiscalPeriodPk())
                .journalTypeCode("MANUAL")
                .descriptionEn("AC-FIN-075 manual entry B")
                .lines(List.of(
                    JournalLineCreateRequest.builder()
                        .accountId(debitAccount.getAccountPk())
                        .amount(TAGGED_LINE_AMOUNT)
                        .directionCode(JournalEntryDomain.DIRECTION_DEBIT)
                        .dimensions(List.of(tag)).build(),
                    JournalLineCreateRequest.builder()
                        .accountId(creditAccount.getAccountPk())
                        .amount(TAGGED_LINE_AMOUNT)
                        .directionCode(JournalEntryDomain.DIRECTION_CREDIT)
                        .build()))
                .build());
        assertThat(posted.getStatus()).isEqualTo(Status.CREATED);
    }

    private EventTypeRule persistRule() {
        return eventTypeRuleRepository.save(EventTypeRule.builder()
            .eventTypeCode("EVT_TYPE_" + uniqueSuffix())
            .nameAr("قاعدة اختبار").nameEn("Coverage rule")
            .isActiveFl(Boolean.TRUE)
            .build());
    }

    private RuleLine persistConstantLine(EventTypeRule rule, int lineNo, Account account,
                                         String directionCode, String amountSourceTypeCode,
                                         String amountSourceValue) {
        return persistConstantLine(rule, lineNo, account, directionCode, amountSourceTypeCode,
            amountSourceValue, EventTypeRuleDomain.DISTRIBUTION_TYPE_PERCENTAGE);
    }

    private RuleLine persistConstantLine(EventTypeRule rule, int lineNo, Account account,
                                         String directionCode, String amountSourceTypeCode,
                                         String amountSourceValue, String distributionTypeCode) {
        return ruleLineRepository.save(RuleLine.builder()
            .eventTypeRule(rule)
            .lineNo(lineNo)
            .accountDerivationTypeCode(EventTypeRuleDomain.ACCOUNT_DERIVATION_TYPE_CONSTANT)
            .accountDerivationValue(account.getCode())
            .amountSourceTypeCode(amountSourceTypeCode)
            .amountSourceValue(amountSourceValue)
            .directionCode(directionCode)
            .distributionTypeCode(distributionTypeCode)
            .isRemainderFl(Boolean.FALSE)
            .build());
    }

    private RuleLine persistMappedLine(EventTypeRule rule, int lineNo, String directionCode,
                                       String amountSourceTypeCode, String amountSourceValue,
                                       String distributionTypeCode) {
        return ruleLineRepository.save(RuleLine.builder()
            .eventTypeRule(rule)
            .lineNo(lineNo)
            .accountDerivationTypeCode(EventTypeRuleDomain.ACCOUNT_DERIVATION_TYPE_MAPPING)
            .accountBusinessFieldCode(PAYMENT_METHOD_FIELD)
            .amountSourceTypeCode(amountSourceTypeCode)
            .amountSourceValue(amountSourceValue)
            .directionCode(directionCode)
            .distributionTypeCode(distributionTypeCode)
            .isRemainderFl(Boolean.FALSE)
            .build());
    }

    private RuleLine persistRemainderLine(EventTypeRule rule, int lineNo, Account account) {
        return ruleLineRepository.save(RuleLine.builder()
            .eventTypeRule(rule)
            .lineNo(lineNo)
            .accountDerivationTypeCode(EventTypeRuleDomain.ACCOUNT_DERIVATION_TYPE_CONSTANT)
            .accountDerivationValue(account.getCode())
            .amountSourceTypeCode(EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_REMAINDER)
            .directionCode(JournalEntryDomain.DIRECTION_DEBIT)
            .distributionTypeCode(AllocationRuleDomain.DISTRIBUTION_TYPE_REMAINDER)
            .isRemainderFl(Boolean.TRUE)
            .build());
    }

    private RuleLine persistMappedRemainderLine(EventTypeRule rule, int lineNo) {
        return ruleLineRepository.save(RuleLine.builder()
            .eventTypeRule(rule)
            .lineNo(lineNo)
            .accountDerivationTypeCode(EventTypeRuleDomain.ACCOUNT_DERIVATION_TYPE_MAPPING)
            .accountBusinessFieldCode(PAYMENT_METHOD_FIELD)
            .amountSourceTypeCode(EventTypeRuleDomain.AMOUNT_SOURCE_TYPE_REMAINDER)
            .directionCode(JournalEntryDomain.DIRECTION_DEBIT)
            .distributionTypeCode(AllocationRuleDomain.DISTRIBUTION_TYPE_REMAINDER)
            .isRemainderFl(Boolean.TRUE)
            .build());
    }

    private RuleLineDimension persistConstantTag(RuleLine ruleLine, DimensionValue value) {
        return ruleLineDimensionRepository.save(RuleLineDimension.builder()
            .ruleLine(ruleLine)
            .dimension(value.getDimension())
            .dimensionValue(value)
            .valueSourceCode(EventTypeRuleDomain.VALUE_SOURCE_CONSTANT)
            .build());
    }

    private AccountMapping persistMapping(EventTypeRule rule, Account account) {
        return accountMappingRepository.save(AccountMapping.builder()
            .eventTypeCode(rule.getEventTypeCode())
            .businessFieldCode(PAYMENT_METHOD_FIELD)
            .businessValue(CHEQUE)
            .account(account)
            .isActiveFl(Boolean.TRUE)
            .build());
    }

    private Account expenseAccount() {
        return fixtures.persistAccount(
            AccountDomain.ACCOUNT_TYPE_EXPENSE, JournalEntryDomain.DIRECTION_DEBIT);
    }

    /** Account "5100" is AC-FIN-076's own code; reused when the database already carries it. */
    private Account taggedAccount() {
        Specification<Account> ofCode =
            (root, query, cb) -> cb.equal(root.get("code"), TAGGED_ACCOUNT_CODE);
        return accountRepository.findBy(ofCode, fluent -> fluent.first())
            .orElseGet(() -> accountRepository.save(Account.builder()
                .code(TAGGED_ACCOUNT_CODE)
                .nameAr("حساب اختبار ٥١٠٠").nameEn("Account 5100")
                .accountTypeCode(AccountDomain.ACCOUNT_TYPE_EXPENSE)
                .natureCode(JournalEntryDomain.DIRECTION_DEBIT)
                .isLeafFl(Boolean.TRUE).isActiveFl(Boolean.TRUE)
                .isRetainedEarningsFl(false)
                .build()));
    }

    private Dimension branchDimension() {
        Specification<Dimension> ofCode =
            (root, query, cb) -> cb.equal(root.get("code"), BRANCH_DIMENSION_CODE);
        return dimensionRepository.findBy(ofCode, fluent -> fluent.first())
            .orElseGet(() -> dimensionRepository.save(Dimension.builder()
                .code(BRANCH_DIMENSION_CODE)
                .nameAr("الفرع").nameEn("Branch")
                .isActiveFl(Boolean.TRUE)
                .build()));
    }

    private DimensionValue branchValue(Dimension branch) {
        return dimensionValueRepository
            .findByDimension_DimensionPkAndCode(branch.getDimensionPk(), BRANCH_VALUE_CODE)
            .orElseGet(() -> dimensionValueRepository.save(DimensionValue.builder()
                .dimension(branch)
                .code(BRANCH_VALUE_CODE)
                .nameAr("فرع ٠٧").nameEn("Branch 07")
                .isActiveFl(Boolean.TRUE)
                .build()));
    }

    private void setEventConsumer() {
        FinYearEndFixtures.setAuthenticatedPrincipal("fin-v2-event-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE);
    }

    private void setReportViewer() {
        FinYearEndFixtures.setAuthenticatedPrincipal("fin-v2-report-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_DIMENSION_REPORTS_VIEW);
    }

    private List<DimensionReportRowResponse> reportRowsFor(Dimension dimension, Account account,
                                                           DimensionValue value) {
        DimensionReportResponse report = reportService
            .dimensionReport(dimension.getDimensionPk(), null, null).getData();
        return report.getRows().stream()
            .filter(row -> account.getAccountPk().equals(row.getAccountId()))
            .filter(row -> value.getDimensionValuePk().equals(row.getDimensionValueId()))
            .toList();
    }

    private DimensionReportRowResponse reportRowFor(Dimension dimension, Account account,
                                                    DimensionValue value) {
        List<DimensionReportRowResponse> rows = reportRowsFor(dimension, account, value);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static BigDecimal amountOn(JournalEntryResponse entry, Account account) {
        List<JournalLineResponse> lines = entry.getLines().stream()
            .filter(line -> account.getAccountPk().equals(line.getAccountId()))
            .toList();
        assertThat(lines).as("exactly one line on account " + account.getCode()).hasSize(1);
        return lines.get(0).getAmount();
    }

    private static JournalLineResponse remainderMarkedLine(JournalEntryResponse entry) {
        List<JournalLineResponse> marked = entry.getLines().stream()
            .filter(line -> Boolean.TRUE.equals(line.getIsRemainderFl()))
            .toList();
        assertThat(marked).as("exactly one line carries isRemainderFl").hasSize(1);
        return marked.get(0);
    }

    private static void assertBalancedAt(JournalEntryResponse entry, BigDecimal expectedSide) {
        BigDecimal debits = sideTotal(entry, JournalEntryDomain.DIRECTION_DEBIT);
        BigDecimal credits = sideTotal(entry, JournalEntryDomain.DIRECTION_CREDIT);
        assertThat(debits).as("total debits").isEqualByComparingTo(expectedSide);
        assertThat(credits).as("total credits").isEqualByComparingTo(expectedSide);
    }

    private static BigDecimal sideTotal(JournalEntryResponse entry, String directionCode) {
        return entry.getLines().stream()
            .filter(line -> directionCode.equalsIgnoreCase(line.getDirectionCode()))
            .map(JournalLineResponse::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, BigDecimal> signedNetsOf(Long journalEntryPk) {
        Map<Long, BigDecimal> nets = new LinkedHashMap<>();
        for (JournalLine line : journalLineRepository.findByJournalEntryPk(journalEntryPk)) {
            nets.merge(line.getAccount().getAccountPk(), signed(line), BigDecimal::add);
        }
        return nets;
    }

    private Map<Long, List<Long>> dimensionValueIdsByAccount(Long journalEntryPk) {
        Map<Long, List<Long>> tags = new LinkedHashMap<>();
        for (JournalLineDimension tag
                : journalLineDimensionRepository.findByJournalEntryPk(journalEntryPk)) {
            tags.computeIfAbsent(tag.getJournalLine().getAccount().getAccountPk(),
                    key -> new ArrayList<>())
                .add(tag.getDimensionValue().getDimensionValuePk());
        }
        return tags;
    }

    private Map<Long, BigDecimal> signedBranchNetsOf(Dimension branch, Long... journalEntryPks) {
        Map<Long, BigDecimal> nets = new LinkedHashMap<>();
        for (Long journalEntryPk : journalEntryPks) {
            for (JournalLineDimension tag
                    : journalLineDimensionRepository.findByJournalEntryPk(journalEntryPk)) {
                if (!branch.getDimensionPk().equals(tag.getDimension().getDimensionPk())) {
                    continue;
                }
                nets.merge(tag.getDimensionValue().getDimensionValuePk(),
                    signed(tag.getJournalLine()), BigDecimal::add);
            }
        }
        return nets;
    }

    private static BigDecimal signed(JournalLine line) {
        return JournalEntryDomain.DIRECTION_DEBIT.equalsIgnoreCase(line.getDirectionCode())
            ? line.getAmount()
            : line.getAmount().negate();
    }

    private static String uniqueSuffix() {
        return FinYearEndFixtures.uniqueSuffix();
    }
}
