package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.domain.AccountMappingDomain;
import com.erp.fin.domain.DimensionValueDomain;
import com.erp.fin.domain.EventTypeRuleDomain;
import com.erp.fin.domain.FiscalPeriodDomain;
import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.EventEntryBuildRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.AccountMapping;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.EventTypeRule;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.RuleLine;
import com.erp.fin.entity.RuleLineDimension;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.JournalEntryMapper;
import com.erp.fin.mapper.JournalEntryMapper.BuiltDimension;
import com.erp.fin.mapper.JournalEntryMapper.BuiltLine;
import com.erp.fin.repository.AccountMappingRepository;
import com.erp.fin.repository.AccountRepository;
import com.erp.fin.repository.DimensionValueRepository;
import com.erp.fin.repository.EventTypeRuleRepository;
import com.erp.fin.repository.FiscalPeriodRepository;
import com.erp.fin.repository.JournalEntryRepository;
import com.erp.fin.repository.RuleLineDimensionRepository;
import com.erp.fin.repository.RuleLineRepository;
import com.erp.fin.service.JournalPostingService.PostingRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-FIN-020 (SVC-API-INT, v2) — build and post a journal entry from a canonical accounting
 * event: mapped accounts (RULE-FIN-020/021), event-resolved dimension tags (RULE-FIN-027) and
 * the period covering the event date (RULE-FIN-028), then the shared posting pipeline. No
 * caching annotations (FIN's register is empty); no search, so no {@code ALLOWED_SORT_FIELDS}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final EventTypeRuleRepository eventTypeRuleRepository;
    private final RuleLineRepository ruleLineRepository;
    private final RuleLineDimensionRepository ruleLineDimensionRepository;
    private final AccountRepository accountRepository;
    private final AccountMappingRepository accountMappingRepository;
    private final DimensionValueRepository dimensionValueRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalPostingService postingService;
    private final JournalEntryMapper mapper;

    /**
     * API-FIN-020's Orchestration steps 1–10 in ONE transaction: RULE-FIN-004 pre-check → active
     * rule → covering period (QR-FIN-059, RULE-FIN-028) → lines and their tags (QR-FIN-057) →
     * per line the account, the amount and the dimensions → validate, number and post through
     * {@link JournalPostingService}. Every decision is a Domain call; nothing here reads MDL, the
     * rule's own codes having been validated when it was written (API-FIN-011/039).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_JOURNAL_ENTRIES_CREATE)")
    public ServiceResult<JournalEntryResponse> build(EventEntryBuildRequest request) {
        log.info("Building event entry for event reference: {} type: {}",
            request.getEventReference(), request.getEventTypeCode());

        boolean alreadyProcessed =
            journalEntryRepository.existsByEventReference(request.getEventReference());
        JournalEntryDomain.create(request.getEventReference(), alreadyProcessed);

        EventTypeRule rule = eventTypeRuleRepository
            .findByEventTypeCodeAndIsActiveFl(request.getEventTypeCode(), Boolean.TRUE)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                FinErrorCodes.FIN_404_NO_ACTIVE_RULE, request.getEventTypeCode()));

        Optional<FiscalPeriod> covering =
            fiscalPeriodRepository.findCoveringDate(request.getDocDate(), Limit.of(1));
        FiscalPeriodDomain.assertDateCovered(covering, request.getDocDate());
        FiscalPeriod period = covering.orElseThrow();

        List<RuleLine> ruleLines =
            ruleLineRepository.findByEventTypeRulePk(rule.getEventTypeRulePk());
        EventTypeRuleDomain.from(rule).assertRemainderLineSetValid(ruleLines);
        Map<Long, List<RuleLineDimension>> tagsByLine = tagsByLine(ruleLines);

        JournalEntry posted = postingService.buildValidateAndPost(new PostingRequest(
            period.getFiscalYear(), period, request.getDocDate(),
            JournalPostingService.JOURNAL_TYPE_EVENT_GENERATED, request.getEventReference(),
            request.getDescriptionAr(), request.getDescriptionEn(),
            buildLines(rule, ruleLines, tagsByLine, request)));

        List<JournalLineResponse> lineResponses =
            posted.getLines().stream().map(mapper::toLineResponse).toList();

        return ServiceResult.success(mapper.toResponse(posted, lineResponses), Status.CREATED);
    }

    /** QR-FIN-057 — every tag of the rule's lines in one read, grouped by rule line. */
    private Map<Long, List<RuleLineDimension>> tagsByLine(List<RuleLine> ruleLines) {
        Map<Long, List<RuleLineDimension>> tagsByLine = new HashMap<>();
        List<Long> lineIds = ruleLines.stream().map(RuleLine::getRuleLinePk).toList();
        if (lineIds.isEmpty()) {
            return tagsByLine;
        }
        for (RuleLineDimension tag : ruleLineDimensionRepository.findByRuleLine_RuleLinePkIn(lineIds)) {
            tagsByLine.computeIfAbsent(tag.getRuleLine().getRuleLinePk(), key -> new ArrayList<>())
                .add(tag);
        }
        return tagsByLine;
    }

    /**
     * Steps 5–7 in the block's order: every line's account, then every amount with the remainder
     * lines last per side (RULE-FIN-010, {@code EventTypeRuleDomain}), then every tag's dimension
     * value. Pure assembly — each branch on a business code is a Domain predicate, and the built
     * lines carry their {@link BuiltDimension}s so the pipeline judges RULE-FIN-009 exactly as for
     * a manual line (REQ-FIN-066/067) and writes one row per tag (REQ-FIN-070).
     */
    private List<BuiltLine> buildLines(EventTypeRule rule, List<RuleLine> ruleLines,
                                       Map<Long, List<RuleLineDimension>> tagsByLine,
                                       EventEntryBuildRequest request) {
        Map<Long, Account> accounts = new HashMap<>();
        for (RuleLine ruleLine : ruleLines) {
            accounts.put(ruleLine.getRuleLinePk(),
                resolveAccount(rule, ruleLine, request.getFields()));
        }

        List<RuleLine> orderedLines = new ArrayList<>(ruleLines.size());
        Map<Long, BigDecimal> amounts = new HashMap<>();
        List<RuleLine> remainderLines = new ArrayList<>();
        JournalEntryDomain.SideTotals totals = JournalEntryDomain.SideTotals.zero();

        for (RuleLine ruleLine : ruleLines) {
            if (EventTypeRuleDomain.isRemainderLine(ruleLine.getIsRemainderFl())) {
                remainderLines.add(ruleLine);
                continue;
            }
            BigDecimal amount = EventTypeRuleDomain.sourcedAmount(
                ruleLine.getAmountSourceTypeCode(), ruleLine.getAmountSourceValue(),
                request.getBaseAmount(), request.getAmounts());
            if (EventTypeRuleDomain.isOmittedZeroLine(amount)) {
                continue;   // POL-FIN-005: a zero line carries nothing (e.g. remainingAmount = 0)
            }
            amounts.put(ruleLine.getRuleLinePk(), amount);
            orderedLines.add(ruleLine);
            totals = totals.plus(ruleLine.getDirectionCode(), amount);
        }

        for (RuleLine remainderLine : remainderLines) {
            String direction = remainderLine.getDirectionCode();
            BigDecimal remainder = EventTypeRuleDomain.remainderAmount(
                totals.opposingTotalOf(direction), totals.ownTotalOf(direction));
            amounts.put(remainderLine.getRuleLinePk(), remainder);
            orderedLines.add(remainderLine);
            totals = totals.plus(direction, remainder);
        }

        List<BuiltLine> built = new ArrayList<>(orderedLines.size());
        for (RuleLine ruleLine : orderedLines) {
            Long ruleLinePk = ruleLine.getRuleLinePk();
            built.add(new BuiltLine(accounts.get(ruleLinePk), amounts.get(ruleLinePk),
                ruleLine.getDirectionCode(),
                EventTypeRuleDomain.isRemainderLine(ruleLine.getIsRemainderFl()),
                request.getDescriptionAr(), request.getDescriptionEn(),
                resolveDimensions(tagsByLine.getOrDefault(ruleLinePk, List.of()),
                    request.getFields())));
        }
        return built;
    }

    /**
     * Step 5 — CONSTANT: the account whose code is the line's {@code accountDerivationValue};
     * MAPPING: the event's value for the line's business field (RULE-FIN-021) selects the one
     * active mapping (QR-FIN-055, RULE-FIN-020), whose account is already fetched. Nothing in
     * {@code fields} is ever read as an account code (REQ-FIN-047).
     */
    private Account resolveAccount(EventTypeRule rule, RuleLine ruleLine,
                                   Map<String, String> fields) {
        if (!EventTypeRuleDomain.isMappingDerivation(ruleLine.getAccountDerivationTypeCode())) {
            return resolveAccountByCode(ruleLine.getAccountDerivationValue());
        }
        String businessFieldCode = ruleLine.getAccountBusinessFieldCode();
        String value = EventTypeRuleDomain.assertFieldPresent(businessFieldCode, fields);
        Optional<AccountMapping> mapping = accountMappingRepository.findActiveByKey(
            rule.getEventTypeCode(), businessFieldCode, value);
        AccountMappingDomain.assertResolved(mapping, businessFieldCode, value);
        return mapping.orElseThrow().getAccount();
    }

    /** Step 7 — one {@link BuiltDimension} per tag; a line with no tag builds none. */
    private List<BuiltDimension> resolveDimensions(List<RuleLineDimension> tags,
                                                   Map<String, String> fields) {
        List<BuiltDimension> resolved = new ArrayList<>(tags.size());
        for (RuleLineDimension tag : tags) {
            resolved.add(new BuiltDimension(tag.getDimension(), resolveDimensionValue(tag, fields)));
        }
        return resolved;
    }

    /**
     * CONSTANT: the tag's own value; BUSINESS_FIELD: the event's value for the tag's business
     * field (RULE-FIN-021) resolved by exact code within the tagged dimension (QR-FIN-058,
     * RULE-FIN-027, ADR-FIN-026). An inactive match is RULE-FIN-009's rejection at post.
     */
    private DimensionValue resolveDimensionValue(RuleLineDimension tag,
                                                 Map<String, String> fields) {
        if (!EventTypeRuleDomain.isBusinessFieldSource(tag.getValueSourceCode())) {
            return tag.getDimensionValue();
        }
        String value = EventTypeRuleDomain.assertFieldPresent(tag.getBusinessFieldCode(), fields);
        Long dimensionPk = tag.getDimension().getDimensionPk();
        Optional<DimensionValue> match =
            dimensionValueRepository.findByDimension_DimensionPkAndCode(dimensionPk, value);
        DimensionValueDomain.assertResolvedByCode(dimensionPk, value, match);
        return match.orElseThrow();
    }

    /**
     * FK resolution by natural key, not a rule: a CONSTANT line's account code that matches no
     * account is {@code FIN-404-ACCOUNT}. An explicit {@code Specification} plus the fluent
     * {@code first()}, the same A.5.17-sanctioned technique the posting pipeline uses.
     */
    private Account resolveAccountByCode(String accountCode) {
        Specification<Account> ofCode = (root, query, cb) -> cb.equal(root.get("code"),
            accountCode);

        return accountRepository.findBy(ofCode, fluent -> fluent.first())
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT, accountCode));
    }
}
