package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.fin.domain.AllocationRuleDomain;
import com.erp.fin.domain.EventTypeRuleDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.AllocationRuleCreateRequest;
import com.erp.fin.dto.AllocationRuleResponse;
import com.erp.fin.dto.AllocationRuleSearchRequest;
import com.erp.fin.dto.AllocationTargetCreateRequest;
import com.erp.fin.dto.AllocationTargetResponse;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.AllocationRule;
import com.erp.fin.entity.AllocationTarget;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.JournalLine;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.AllocationRuleMapper;
import com.erp.fin.mapper.AllocationTargetMapper;
import com.erp.fin.mapper.JournalEntryMapper;
import com.erp.fin.mapper.JournalEntryMapper.BuiltLine;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.AllocationRuleRepository;
import com.erp.fin.repository.AllocationTargetRepository;
import com.erp.fin.repository.DimensionValueRepository;
import com.erp.fin.repository.JournalLineRepository;
import com.erp.fin.service.JournalPostingService.PostingRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for ENT-FIN-013 / ENT-FIN-014 (AllocationRule and its targets) —
 * API-FIN-016 (SVC-API-CRUD.md) and API-FIN-017, run an allocation rule (SVC-API-INT.md).
 *
 * <p>No caching annotations — FIN's approved cache register is empty. No
 * {@code ALLOWED_SORT_FIELDS} is present because SVC-API-SEARCH added API-FIN-015 here (A.5.6).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationRuleService {

    /**
     * A.5.6 — API-FIN-015's filter/sort whitelist. {@code sourceAccountId} is absent on purpose:
     * it is an association path and is ANDed in as an explicit join below, not through
     * {@code SpecBuilder}.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "allocationRulePk", "nameAr", "nameEn", "isActiveFl", "createdAt");

    private final AllocationRuleRepository repository;
    private final AllocationTargetRepository targetRepository;
    private final AccountRepository accountRepository;
    private final DimensionValueRepository dimensionValueRepository;
    private final JournalLineRepository journalLineRepository;
    private final AllocationRuleMapper mapper;
    private final AllocationTargetMapper targetMapper;
    private final JournalEntryMapper journalEntryMapper;
    private final FinLookupValidationService lookupValidation;
    private final JournalPostingService postingService;

    /**
     * API-FIN-016 — resolve the source account ({@code FIN-404-ACCOUNT}), validate every target's
     * {@code distributionTypeCode} against MDL (XM-FIN-001), build the prospective target set,
     * then delegate RULE-FIN-003 (reused for allocation targets, QR-FIN-016) to
     * {@link AllocationRuleDomain#create(Long, List)} — exactly one remainder target whenever any
     * sibling uses percentage distribution ({@code FIN-409-REMAINDER-COUNT}). Rule and targets are
     * persisted in ONE transaction (QR-FIN-021).
     *
     * <p>The targets are built against the not-yet-persisted rule and their parent FK is set after
     * the rule is saved: {@code AllocationRule} declares no {@code @OneToMany} cascade, so the
     * child rows are written explicitly through their own repository, inside this same
     * transaction.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ALLOCATION_RULES_CREATE)")
    public ServiceResult<AllocationRuleResponse> create(AllocationRuleCreateRequest request) {
        log.info("Creating AllocationRule from source account ID: {}", request.getSourceAccountId());

        Account sourceAccount = resolveAccount(request.getSourceAccountId());

        for (AllocationTargetCreateRequest target : request.getTargets()) {
            lookupValidation.assertValidCode(
                FinLookupValidationService.KEY_DISTRIBUTION_TYPE, target.getDistributionTypeCode());
        }

        AllocationRule rule = mapper.toEntity(request, sourceAccount);

        List<AllocationTarget> targets = new ArrayList<>(request.getTargets().size());
        int lineNo = 1;
        for (AllocationTargetCreateRequest targetRequest : request.getTargets()) {
            targets.add(targetMapper.toEntity(targetRequest, rule, lineNo,
                resolveAccount(targetRequest.getTargetAccountId()),
                resolveDimensionValue(targetRequest.getDimensionValueId())));
            lineNo++;
        }

        AllocationRuleDomain.create(request.getSourceAccountId(), targets);

        AllocationRule saved = repository.save(rule);
        for (AllocationTarget target : targets) {
            target.setAllocationRule(saved);
        }
        List<AllocationTarget> savedTargets = targetRepository.saveAll(targets);

        log.info("Created AllocationRule ID: {} with {} target(s)",
            saved.getAllocationRulePk(), savedTargets.size());

        List<AllocationTargetResponse> targetResponses =
            savedTargets.stream().map(targetMapper::toResponse).toList();

        return ServiceResult.success(mapper.toResponse(saved, targetResponses), Status.CREATED);
    }

    /**
     * API-FIN-017 — load the rule and its targets together with the source account's current
     * balance (QR-FIN-022) → distribute per each target's {@code distributionTypeCode}, the
     * remainder target absorbing the rounding difference (RULE-FIN-010, QR-FIN-028) → build the
     * entry (journalTypeCode=ALLOCATION) → validate and post (QR-FIN-029..033) → return.
     *
     * <p><b>Atomic unit: the entry.</b> A run produces exactly one entry, so "per entry" and "per
     * run" coincide; {@link JournalPostingService} joins this transaction, so CORE.md's "ONE
     * transaction per entry" holds and nothing is written before validation succeeds.
     *
     * <p><b>Aggregated.</b> The Validations line is "RULE-FIN-010 … then RULE-FIN-006/007/008/009",
     * the same post-time set as API-FIN-019, so those four are reported together by the shared
     * pipeline (REQ-FIN-015 / AC-FIN-015). The not-found preconditions (the rule itself, the target
     * period) stay fail-fast.
     *
     * <p><b>Rules delegated, none inlined</b> (A.5.18): the active gate →
     * {@code AllocationRuleDomain.assertCanRun()}, run first — before the target set is loaded,
     * before the source balance is computed and before the period is resolved — so a deactivated
     * rule is refused with {@code FIN-409-NOT-ACTIVE} and nothing is built or posted;
     * RULE-FIN-003 (the remainder guarantee
     * RULE-FIN-010 depends on) → {@code AllocationRuleDomain.assertRemainderTargetSetValid(...)};
     * each target's share and the remainder difference → {@code AllocationRuleDomain.targetAmount}
     * and {@code EventTypeRuleDomain.remainderAmount}, RULE-FIN-010's single implementation; the
     * posting side of each line → {@code JournalEntryDomain.directionOfNet} /
     * {@code reversalDirectionOf}; RULE-FIN-006/007/008/009 → the shared pipeline.
     *
     * <p><b>Lookup validation.</b> Nothing lookup-backed is submitted: {@code journalTypeCode} is
     * the system-assigned SRS A6 value {@code ALLOCATION}, every {@code distributionTypeCode} was
     * validated against MDL at API-FIN-016 time, and each line's {@code directionCode} is derived
     * from the source balance rather than supplied.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ALLOCATION_RULES_UPDATE)")
    public ServiceResult<JournalEntryResponse> run(Long id) {
        log.info("Running AllocationRule ID: {}", id);

        AllocationRule rule = repository.lockForRun(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ALLOCATION_RULE, id));

        AllocationRuleDomain domain = AllocationRuleDomain.from(rule);
        domain.assertCanRun();

        List<AllocationTarget> targets =
            targetRepository.findByAllocationRulePk(rule.getAllocationRulePk());
        domain.assertRemainderTargetSetValid(targets);

        Account sourceAccount = rule.getSourceAccount();
        BigDecimal sourceBalance = balanceOf(sourceAccount.getAccountPk());
        LocalDate runDate = LocalDate.now();
        FiscalPeriod period = postingService.resolvePeriodContaining(runDate);

        JournalEntry posted = postingService.buildValidateAndPost(new PostingRequest(
            period.getFiscalYear(), period, runDate,
            JournalPostingService.JOURNAL_TYPE_ALLOCATION, null,
            rule.getNameAr(), rule.getNameEn(),
            allocationLines(sourceAccount, sourceBalance, targets)));

        List<JournalLineResponse> lineResponses =
            posted.getLines().stream().map(journalEntryMapper::toLineResponse).toList();

        return ServiceResult.success(
            journalEntryMapper.toResponse(posted, lineResponses), Status.CREATED);
    }

    /**
     * API-FIN-037 — soft deactivation only: retire an allocation rule so it stops being run. No
     * SRS rule answers "may this rule be deactivated?" — RULE-FIN-001..017 were each read and none
     * constrains retiring an allocation rule. In particular RULE-FIN-003, the one rule this
     * entity's {@link AllocationRuleDomain} owns, governs the remainder-target SET at create and
     * run time; it says nothing about the rule's active flag, and no rule makes a rule referenced
     * by anything in flight undeactivatable (a run is a single transaction — there is no
     * long-running allocation to be caught mid-flight). So there is nothing to delegate before the
     * mutation: the same shape as {@code AccountService.deactivate} (API-FIN-004),
     * {@code EventTypeRuleService.deactivate} (API-FIN-034) and
     * {@code DimensionValueService.deactivate} (API-FIN-035). The flag moves through ENT-FIN-013's
     * own {@code deactivate()} helper, never a direct assignment.
     *
     * <p>Why this endpoint exists: srs-fin.md SCR-REQ-FIN-005 §B4 records the absence of a rule
     * deactivate as an OPEN DEFECT rather than a scope decision — {@code IS_ACTIVE_FL} is NOT NULL,
     * {@code activate()}/{@code deactivate()} shipped with zero callers,
     * {@link AllocationRuleDomain#isActive()} was dead code, and B2 advertises an
     * {@code isActiveFl(EXACT)} search filter over a column nothing could set to FALSE. A rule
     * created with the wrong source account or target split could be neither retired nor corrected,
     * so every subsequent API-FIN-017 run posted a wrong distribution.
     *
     * <p><b>This deactivate now gates the run</b>, by RECORDED HUMAN DECISION — it was not one
     * when API-FIN-037 shipped. {@link #run(Long)} calls
     * {@link AllocationRuleDomain#assertCanRun()} on the row it loads, so a deactivated rule is
     * refused with {@code FIN-409-NOT-ACTIVE} (HTTP 409) and posts nothing; that guard reads the
     * same {@code active} fact {@link AllocationRuleDomain#isActive()} exposes, which until now
     * had no callers. No RULE-FIN-* states this gate — RULE-FIN-001..017 were each read and none
     * constrains running a retired rule — so it rests on that decision, not on a rule that was
     * always there. Deactivation therefore stays unguarded on the way IN while being enforced on
     * the way OUT, at run time. The flag also still drives API-FIN-015's advertised
     * {@code isActiveFl} filter.
     *
     * <p>The response carries the rule's targets because {@code AllocationRuleResponse} derives its
     * {@code targetCount} from the list it is handed; passing an empty list would misreport the
     * aggregate as having none. Loading them is orchestration (load → map), not a rule.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ALLOCATION_RULES_UPDATE)")
    public ServiceResult<AllocationRuleResponse> deactivate(Long id) {
        log.info("Deactivating AllocationRule ID: {}", id);

        AllocationRule entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ALLOCATION_RULE, id));

        entity.deactivate();

        AllocationRule saved = repository.save(entity);
        log.info("Deactivated AllocationRule ID: {}", saved.getAllocationRulePk());

        List<AllocationTargetResponse> targetResponses =
            targetRepository.findByAllocationRulePk(saved.getAllocationRulePk()).stream()
                .map(targetMapper::toResponse)
                .toList();

        return ServiceResult.success(
            mapper.toResponse(saved, targetResponses), Status.UPDATED);
    }

    /**
     * QR-FIN-028 — the allocation entry's lines: one line relieving the source account of its
     * whole balance, plus one line per target taking its share on the opposite side, the
     * remainder target computed last as the difference so the target lines sum exactly to the
     * source balance (RULE-FIN-010, AC-FIN-026).
     *
     * <p>Balanced by construction: the target lines all sit on the balance's own side and total
     * exactly the balance, while the source line carries the same total on the opposite side.
     */
    private List<BuiltLine> allocationLines(Account sourceAccount,
                                            BigDecimal sourceBalance,
                                            List<AllocationTarget> targets) {
        String targetDirection = JournalEntryDomain.directionOfNet(sourceBalance);
        String sourceDirection = JournalEntryDomain.reversalDirectionOf(targetDirection);
        BigDecimal distributable = sourceBalance.abs();

        List<BuiltLine> lines = new ArrayList<>();
        lines.add(new BuiltLine(sourceAccount, distributable, sourceDirection, false, null, null,
            List.of()));

        List<AllocationTarget> remainderTargets = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO;
        for (AllocationTarget target : targets) {
            if (AllocationRuleDomain.isRemainderTarget(target.getIsRemainderFl())) {
                remainderTargets.add(target);
                continue;
            }
            BigDecimal amount = AllocationRuleDomain.targetAmount(
                target.getDistributionTypeCode(), target.getDistributionValue(), distributable);
            lines.add(toTargetLine(target, amount, targetDirection));
            assigned = assigned.add(amount);
        }
        for (AllocationTarget target : remainderTargets) {
            BigDecimal remainder = EventTypeRuleDomain.remainderAmount(distributable, assigned);
            lines.add(toTargetLine(target, remainder, targetDirection));
            assigned = assigned.add(remainder);
        }
        return lines;
    }

    private BuiltLine toTargetLine(AllocationTarget target, BigDecimal amount, String direction) {
        List<JournalEntryMapper.BuiltDimension> tags = target.getDimensionValue() == null
            ? List.of()
            : List.of(new JournalEntryMapper.BuiltDimension(
                target.getDimensionValue().getDimension(), target.getDimensionValue()));
        return new BuiltLine(target.getTargetAccount(), amount, direction,
            AllocationRuleDomain.isRemainderTarget(target.getIsRemainderFl()), null, null, tags);
    }

    /**
     * QR-FIN-022's fact-gathering half: the source account's current signed balance (debits minus
     * credits) over every POSTED line, live from {@code FIN_JOURNAL_LINE} — db-script-fin.md
     * declares no stored balance column anywhere, so a balance is only ever derived
     * (POL-FIN-009/011). A reversed entry and its mirror are both POSTED (classic reversal,
     * RULE-FIN-011) and contribute zero to the balance between them.
     *
     * <p>Read through the existing repository layer with no new query method: an explicit
     * {@code Specification} expresses the nested {@code account.accountPk} and
     * {@code journalEntry.statusCode} paths the shared {@code SpecBuilder}'s flat
     * {@code root.get(field)} cannot (the same A.5.17-sanctioned technique a child search uses).
     */
    private BigDecimal balanceOf(Long accountPk) {
        Specification<JournalLine> postedForAccount = (root, query, cb) -> cb.and(
            cb.equal(root.get("account").get("accountPk"), accountPk),
            cb.equal(root.get("journalEntry").get("statusCode"), JournalEntry.STATUS_POSTED));

        BigDecimal balance = BigDecimal.ZERO;
        for (JournalLine line : journalLineRepository.findAll(postedForAccount)) {
            balance = JournalEntryDomain.DIRECTION_DEBIT.equalsIgnoreCase(line.getDirectionCode())
                ? balance.add(line.getAmount())
                : balance.subtract(line.getAmount());
        }
        return balance;
    }

    /** FK resolution, not a rule: an unknown account id is {@code FIN-404-ACCOUNT}. */
    private Account resolveAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT, accountId));
    }

    /**
     * FK resolution for the optional dimension value (DBF-FIN-143, nullable). An id that resolves
     * to nothing is {@code FIN-409-INVALID-DIMENSION}.
     */
    private DimensionValue resolveDimensionValue(Long dimensionValueId) {
        if (dimensionValueId == null) {
            return null;
        }
        return dimensionValueRepository.findById(dimensionValueId)
            .orElseThrow(() -> new LocalizedException(
                Status.CONFLICT, FinErrorCodes.FIN_409_INVALID_DIMENSION, dimensionValueId));
    }

    /**
     * API-FIN-015 — QR-FIN-020, criteria search over ENT-FIN-013, read-only.
     *
     * <p>{@code sourceAccountId} (DBF-FIN-133) is the association {@code sourceAccount}, a nested
     * path {@code SpecBuilder}'s flat {@code root.get(field)} cannot resolve, so — when the caller
     * supplies it — it becomes an explicit {@code Specification} join ANDed with the generic
     * specification, the same mechanism A.5.17 mandates for a child search. Unlike a child search
     * it is OPTIONAL: the plan lists it as one filter among four, not as a required parent scope.
     *
     * <p>{@code AllocationRuleResponse} reports a {@code targetCount}, so the page's targets are
     * fetched in ONE batch read keyed by the page's ids and grouped in memory.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ALLOCATION_RULES_VIEW)")
    public ServiceResult<Page<AllocationRuleResponse>> search(
            AllocationRuleSearchRequest searchRequest) {
        log.debug("Searching AllocationRule");

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<AllocationRule> spec = SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(Set.of()));

        Long sourceAccountId = searchRequest.getSourceAccountId();
        if (sourceAccountId != null) {
            Specification<AllocationRule> sourceAccountSpec = (root, query, cb) ->
                cb.equal(root.get("sourceAccount").get("accountPk"), sourceAccountId);
            spec = sourceAccountSpec.and(spec);
        }

        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);
        Page<AllocationRule> page = repository.findAll(spec, pageable);

        List<Long> rulePks = page.getContent().stream()
            .map(AllocationRule::getAllocationRulePk)
            .toList();

        Map<Long, List<AllocationTargetResponse>> targetsByRule = rulePks.isEmpty()
            ? Map.of()
            : targetRepository.findByAllocationRulePkIn(rulePks).stream()
                .collect(Collectors.groupingBy(
                    target -> target.getAllocationRule().getAllocationRulePk(),
                    Collectors.mapping(targetMapper::toResponse, Collectors.toList())));

        return ServiceResult.success(page.map(rule -> mapper.toResponse(rule,
            targetsByRule.getOrDefault(rule.getAllocationRulePk(), List.of()))));
    }
}
