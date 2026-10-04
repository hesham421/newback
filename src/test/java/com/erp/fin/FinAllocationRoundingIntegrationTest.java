package com.erp.fin;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.fin.FinYearEndFixtures.YearEndFixture;
import com.erp.fin.domain.AccountDomain;
import com.erp.fin.domain.AllocationRuleDomain;
import com.erp.fin.domain.EventTypeRuleDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.JournalEntryCreateRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineCreateRequest;
import com.erp.fin.dto.JournalLineResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.AllocationRule;
import com.erp.fin.entity.AllocationTarget;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.JournalLine;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.AllocationRuleRepository;
import com.erp.fin.repository.AllocationTargetRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.FiscalYearRepository;
import com.erp.fin.repository.JournalLineRepository;
import com.erp.fin.service.AllocationRuleService;
import com.erp.fin.service.JournalEntryService;
import com.erp.fin.service.JournalPostingService;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * TC-FIN-026 / AC-FIN-026 — an allocation run over a source balance of 1,000.00 whose target
 * lines must sum <i>exactly</i> to it, the REMAINDER target absorbing the rounding difference.
 * The figure is AC prose only, so `api-verify` may not assert it. Same conventions as
 * {@link FinYearEndCoverageIntegrationTest}: real dev Postgres, class-level {@link Transactional}.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class FinAllocationRoundingIntegrationTest {

    private static final LocalDate FIXTURE_YEAR_START = LocalDate.of(2230, 1, 1);

    /** AC-FIN-026's own source balance. */
    private static final BigDecimal SOURCE_BALANCE = new BigDecimal("1000.00");

    /**
     * The two PERCENTAGE targets' share. AC-FIN-026 fixes the shape ("two PERCENTAGE targets and
     * one REMAINDER target") and the balance, but not the percentages; this value is chosen — not
     * taken from any artifact — because it is the largest the column holds (DBF-FIN-145,
     * NUMERIC(18,4)) that leaves a genuine residue, so the remainder really has a rounding
     * difference to absorb rather than a clean third.
     */
    private static final BigDecimal PERCENT_EACH = new BigDecimal("33.3333");
    private static final BigDecimal PERCENT_TARGET_AMOUNT = new BigDecimal("333.3330");
    private static final BigDecimal REMAINDER_TARGET_AMOUNT = new BigDecimal("333.3340");

    @Autowired private AccountRepository accountRepository;
    @Autowired private AllocationRuleRepository allocationRuleRepository;
    @Autowired private AllocationTargetRepository allocationTargetRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private JournalLineRepository journalLineRepository;

    @Autowired private AllocationRuleService allocationRuleService;
    @Autowired private JournalEntryService journalEntryService;
    @Autowired private JournalPostingService postingService;

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
    void anAllocationRunDistributesTheWholeSourceBalanceToTheLastUnit() {
        // Covers: TC-FIN-026, AC-FIN-026 / REQ-FIN-026, API-FIN-017, RULE-FIN-010 + RULE-FIN-003.
        //
        // AC-FIN-026 verbatim: "an allocation rule with two PERCENTAGE targets and one REMAINDER
        // target, and a source balance of 1,000.00 … one entry whose target lines sum exactly to
        // 1,000.00, the remainder target absorbing the rounding difference".
        Account source = fixtures.persistAccount(
            AccountDomain.ACCOUNT_TYPE_EXPENSE, JournalEntryDomain.DIRECTION_DEBIT);
        Account firstTarget = expenseAccount();
        Account secondTarget = expenseAccount();
        Account remainderTarget = expenseAccount();

        giveSourceItsBalance(source);
        assertThat(postedBalanceOf(source))
            .as("the precondition balance of 1,000.00 (AC-FIN-026)")
            .isEqualByComparingTo(SOURCE_BALANCE);

        AllocationRule rule = persistRule(source);
        persistPercentageTarget(rule, 1, firstTarget);
        persistPercentageTarget(rule, 2, secondTarget);
        persistRemainderTarget(rule, 3, remainderTarget);

        FinYearEndFixtures.setAuthenticatedPrincipal("tc-fin-026-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_ALLOCATION_RULES_UPDATE);

        // API-FIN-017 posts into the period containing LocalDate.now() (the service's own
        // resolvePeriodContaining), so the run needs a currently OPEN period — asserted rather
        // than assumed, because a HARD_CLOSE one would fail at RULE-FIN-008 and say nothing about
        // the distribution.
        FiscalPeriod today = postingService.resolvePeriodContaining(LocalDate.now());
        assertThat(today.getStatusCode()).isEqualTo(FiscalPeriod.STATUS_OPEN);
        ServiceResult<JournalEntryResponse> result =
            allocationRuleService.run(rule.getAllocationRulePk());

        assertThat(result.getStatus()).isEqualTo(Status.CREATED);
        JournalEntryResponse entry = result.getData();
        assertThat(entry.getJournalTypeCode())
            .isEqualTo(JournalPostingService.JOURNAL_TYPE_ALLOCATION);
        assertThat(entry.getStatusCode()).isEqualTo(JournalEntry.STATUS_POSTED);

        BigDecimal firstAmount = amountOn(entry, firstTarget);
        BigDecimal secondAmount = amountOn(entry, secondTarget);
        BigDecimal remainderAmount = amountOn(entry, remainderTarget);

        assertThat(firstAmount).isEqualByComparingTo(PERCENT_TARGET_AMOUNT);
        assertThat(secondAmount).isEqualByComparingTo(PERCENT_TARGET_AMOUNT);
        assertThat(remainderAmount)
            .as("the remainder target absorbs the residue 1,000.00 − 666.6660 (AC-FIN-026)")
            .isEqualByComparingTo(REMAINDER_TARGET_AMOUNT);
        assertThat(remainderAmount)
            .as("the remainder is a difference, never a percentage of the balance")
            .isNotEqualByComparingTo(PERCENT_TARGET_AMOUNT);

        assertThat(firstAmount.add(secondAmount).add(remainderAmount))
            .as("the target lines sum EXACTLY to 1,000.00 (AC-FIN-026)")
            .isEqualByComparingTo(SOURCE_BALANCE);

        // The exactly-one REMAINDER marker really is the line that absorbed it (RULE-FIN-003).
        List<JournalLineResponse> marked = entry.getLines().stream()
            .filter(line -> Boolean.TRUE.equals(line.getIsRemainderFl()))
            .toList();
        assertThat(marked).hasSize(1);
        assertThat(marked.get(0).getAccountId()).isEqualTo(remainderTarget.getAccountPk());

        // "the entry balances, so RULE-FIN-006 does not fire": the source line carries the whole
        // 1,000.00 on the opposite side.
        assertThat(amountOn(entry, source)).isEqualByComparingTo(SOURCE_BALANCE);
        assertThat(sideTotal(entry, JournalEntryDomain.DIRECTION_DEBIT))
            .isEqualByComparingTo(sideTotal(entry, JournalEntryDomain.DIRECTION_CREDIT));
        assertThat(entry.getLines()).hasSize(4);

        // The distribution really came off the balance: the source nets to zero afterwards.
        assertThat(postedBalanceOf(source)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private void giveSourceItsBalance(Account source) {
        YearEndFixture fixture = fixtures.openYearEndFixture(FIXTURE_YEAR_START);
        FiscalPeriod period = fixture.periods().get(0);
        Account funding = fixtures.persistAccount("ASSET", JournalEntryDomain.DIRECTION_DEBIT);

        FinYearEndFixtures.setAuthenticatedPrincipal("tc-fin-026-seed-" + uniqueSuffix(),
            PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE);
        ServiceResult<JournalEntryResponse> posted =
            journalEntryService.createManual(JournalEntryCreateRequest.builder()
                .docDate(period.getStartDate())
                .fiscalYearId(fixture.year().getFiscalYearPk())
                .periodId(period.getFiscalPeriodPk())
                .journalTypeCode("MANUAL")
                .descriptionEn("AC-FIN-026 source balance")
                .lines(List.of(
                    JournalLineCreateRequest.builder()
                        .accountId(source.getAccountPk()).amount(SOURCE_BALANCE)
                        .directionCode(JournalEntryDomain.DIRECTION_DEBIT).build(),
                    JournalLineCreateRequest.builder()
                        .accountId(funding.getAccountPk()).amount(SOURCE_BALANCE)
                        .directionCode(JournalEntryDomain.DIRECTION_CREDIT).build()))
                .build());
        assertThat(posted.getStatus()).isEqualTo(Status.CREATED);
        SecurityContextHolder.clearContext();
    }

    private AllocationRule persistRule(Account source) {
        return allocationRuleRepository.save(AllocationRule.builder()
            .nameAr("قاعدة توزيع اختبار").nameEn("Coverage allocation rule")
            .sourceAccount(source)
            .isActiveFl(Boolean.TRUE)
            .build());
    }

    private AllocationTarget persistPercentageTarget(AllocationRule rule, int lineNo,
                                                     Account target) {
        return allocationTargetRepository.save(AllocationTarget.builder()
            .allocationRule(rule)
            .lineNo(lineNo)
            .targetAccount(target)
            .distributionTypeCode(EventTypeRuleDomain.DISTRIBUTION_TYPE_PERCENTAGE)
            .distributionValue(PERCENT_EACH)
            .isRemainderFl(Boolean.FALSE)
            .build());
    }

    private AllocationTarget persistRemainderTarget(AllocationRule rule, int lineNo,
                                                    Account target) {
        return allocationTargetRepository.save(AllocationTarget.builder()
            .allocationRule(rule)
            .lineNo(lineNo)
            .targetAccount(target)
            .distributionTypeCode(AllocationRuleDomain.DISTRIBUTION_TYPE_REMAINDER)
            .isRemainderFl(Boolean.TRUE)
            .build());
    }

    private Account expenseAccount() {
        return fixtures.persistAccount(
            AccountDomain.ACCOUNT_TYPE_EXPENSE, JournalEntryDomain.DIRECTION_DEBIT);
    }

    /** Recomputed here rather than borrowed from the service under test. */
    private BigDecimal postedBalanceOf(Account account) {
        BigDecimal balance = BigDecimal.ZERO;
        for (JournalLine line : allLinesOf(account)) {
            balance = JournalEntryDomain.DIRECTION_DEBIT
                .equalsIgnoreCase(line.getDirectionCode())
                ? balance.add(line.getAmount())
                : balance.subtract(line.getAmount());
        }
        return balance;
    }

    private List<JournalLine> allLinesOf(Account account) {
        return journalLineRepository.findAll(
            (Specification<JournalLine>)
                (root, query, cb) -> cb.and(
                    cb.equal(root.get("account").get("accountPk"), account.getAccountPk()),
                    cb.equal(root.get("journalEntry").get("statusCode"),
                        JournalEntry.STATUS_POSTED)));
    }

    private static BigDecimal amountOn(JournalEntryResponse entry, Account account) {
        List<JournalLineResponse> lines = entry.getLines().stream()
            .filter(line -> account.getAccountPk().equals(line.getAccountId()))
            .toList();
        assertThat(lines).as("exactly one line on account " + account.getCode()).hasSize(1);
        return lines.get(0).getAmount();
    }

    private static BigDecimal sideTotal(JournalEntryResponse entry, String directionCode) {
        return entry.getLines().stream()
            .filter(line -> directionCode.equalsIgnoreCase(line.getDirectionCode()))
            .map(JournalLineResponse::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String uniqueSuffix() {
        return FinYearEndFixtures.uniqueSuffix();
    }
}
