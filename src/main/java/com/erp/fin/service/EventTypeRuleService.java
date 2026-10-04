package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.fin.domain.EventTypeRuleDomain;
import com.erp.fin.dto.EventTypeRuleCreateRequest;
import com.erp.fin.dto.EventTypeRuleSearchRequest;
import com.erp.fin.dto.EventTypeRuleResponse;
import com.erp.fin.dto.RuleLineResponse;
import com.erp.fin.entity.EventTypeRule;
import com.erp.fin.entity.RuleLine;
import com.erp.fin.entity.RuleLineDimension;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.EventTypeRuleMapper;
import com.erp.fin.mapper.RuleLineMapper;
import com.erp.fin.repository.EventTypeRuleRepository;
import com.erp.fin.repository.RuleLineDimensionRepository;
import com.erp.fin.repository.RuleLineRepository;
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
 * Orchestration layer for ENT-FIN-009 (EventTypeRule) — API-FIN-010 (SVC-API-CRUD.md).
 *
 * <p>Precondition recorded by the spec, not enforced per request: this endpoint is only reachable
 * once FIN's onboarding has completed — FIN's screens/actions registered into SEC (REQ-FIN-044)
 * and FIN's 13 lookup types, {@code ACCOUNTING_EVENT_TYPE} among them, registered into MDL
 * (REQ-FIN-045). Both run once at deployment.
 *
 * <p>No caching annotations — FIN's approved cache register is empty. No
 * {@code ALLOWED_SORT_FIELDS} is present because SVC-API-SEARCH added API-FIN-009 here (A.5.6).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventTypeRuleService {

    /**
     * A.5.6 — API-FIN-009's filter/sort whitelist: the plan's {@code eventTypeCode} and
     * {@code isActiveFl} filters, plus ENT-FIN-009's remaining flat columns for ordering.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "eventTypeRulePk", "eventTypeCode", "nameAr", "nameEn", "isActiveFl", "createdAt");

    private final EventTypeRuleRepository repository;
    private final RuleLineRepository lineRepository;
    private final RuleLineDimensionRepository ruleLineDimensionRepository;
    private final EventTypeRuleMapper mapper;
    private final RuleLineMapper lineMapper;
    private final FinLookupValidationService lookupValidation;

    /**
     * API-FIN-010 — validate {@code eventTypeCode} against MDL (XM-FIN-001), ask QR-FIN-014
     * whether the event type already carries a rule, then delegate the §6.4 one-rule-per-type
     * decision to {@link EventTypeRuleDomain#create(String, boolean)}
     * ({@code FIN-409-RULE-DUP}).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_RULES_CREATE)")
    public ServiceResult<EventTypeRuleResponse> create(EventTypeRuleCreateRequest request) {
        log.info("Creating EventTypeRule for event type: {}", request.getEventTypeCode());

        lookupValidation.assertValidCode(
            FinLookupValidationService.KEY_ACCOUNTING_EVENT_TYPE, request.getEventTypeCode());

        boolean ruleAlreadyExists = repository.existsByEventTypeCode(request.getEventTypeCode());

        EventTypeRuleDomain.create(request.getEventTypeCode(), ruleAlreadyExists);

        EventTypeRule saved = repository.save(mapper.toEntity(request));
        log.info("Created EventTypeRule ID: {}", saved.getEventTypeRulePk());

        // A rule is created before any of its lines (API-FIN-011 adds those), so the set is
        // empty here by construction rather than by a read that would always return nothing.
        return ServiceResult.success(mapper.toResponse(saved, List.of()), Status.CREATED);
    }

    /**
     * API-FIN-034 — soft deactivation only: retire an event-type rule so the event-entry build
     * stops resolving it. No SRS rule answers "may this rule be deactivated?", so there is nothing
     * to delegate to {@link EventTypeRuleDomain} — the same shape as {@code AccountService
     * .deactivate} (API-FIN-004) — and the flag moves through ENT-FIN-009's own
     * {@code deactivate()} helper, never a direct assignment.
     *
     * <p>Effect: {@code EventEntryService} resolves the rule through
     * {@code findByEventTypeCodeAndIsActiveFl(code, TRUE)}, so once no active rule remains for the
     * event type, API-FIN-020 answers {@code FIN-404-NO-ACTIVE-RULE} (RULE-FIN-005).
     *
     * <p>Stated plainly, because it is not what a reader would assume: deactivating does NOT free
     * the event type for a replacement rule. {@link #create(EventTypeRuleCreateRequest)} asks
     * {@code repository.existsByEventTypeCode(...)}, which is NOT scoped to the active flag, so
     * §6.4's one-rule-per-event-type check still sees the deactivated row and answers
     * {@code FIN-409-RULE-DUP}. Narrowing that check is a separate decision and is not made here.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_RULES_UPDATE)")
    public ServiceResult<EventTypeRuleResponse> deactivate(Long id) {
        log.info("Deactivating EventTypeRule ID: {}", id);

        EventTypeRule entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_RULE, id));

        entity.deactivate();

        EventTypeRule saved = repository.save(entity);
        log.info("Deactivated EventTypeRule ID: {}", saved.getEventTypeRulePk());

        return ServiceResult.success(
            mapper.toResponse(saved, linesOf(saved.getEventTypeRulePk())), Status.UPDATED);
    }

    /**
     * API-FIN-009 — QR-FIN-012, criteria search over ENT-FIN-009, read-only.
     *
     * <p>Each row carries its ENT-FIN-010 line set: SCR-FIN-003 is a Master/Detail screen and this
     * is the only endpoint that lists rules, so without the lines the Detail pane has no source at
     * all. They are fetched in ONE batch read keyed by the page's ids
     * ({@code findByEventTypeRulePkIn}) and grouped in memory — not one child query per row, the
     * same arrangement API-FIN-012 uses. v2: each line carries its ENT-FIN-016 dimension tags
     * (SCR-REQ-FIN-003 §B1), read in one further batch keyed by the lines' ids (QR-FIN-057).
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_RULES_VIEW)")
    public ServiceResult<Page<EventTypeRuleResponse>> search(
            EventTypeRuleSearchRequest searchRequest) {
        log.debug("Searching EventTypeRule");

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<EventTypeRule> spec = SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(Set.of()));
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<EventTypeRule> page = repository.findAll(spec, pageable);
        List<Long> rulePks = page.getContent().stream()
            .map(EventTypeRule::getEventTypeRulePk)
            .toList();

        List<RuleLine> lines = rulePks.isEmpty()
            ? List.of()
            : lineRepository.findByEventTypeRulePkIn(rulePks);
        Map<Long, List<RuleLineDimension>> tagsByLine = tagsByLine(lines);

        Map<Long, List<RuleLineResponse>> linesByRule = lines.stream()
            .collect(Collectors.groupingBy(
                line -> line.getEventTypeRule().getEventTypeRulePk(),
                Collectors.mapping(line -> lineMapper.toResponse(line,
                    tagsByLine.getOrDefault(line.getRuleLinePk(), List.of())),
                    Collectors.toList())));

        return ServiceResult.success(page.map(rule -> mapper.toResponse(rule,
            linesByRule.getOrDefault(rule.getEventTypeRulePk(), List.of()))));
    }

    /**
     * ENT-FIN-010's rows for one rule, ordered by {@code lineNo}, each with its ENT-FIN-016 tags,
     * already mapped (QR-FIN-016, QR-FIN-057).
     */
    private List<RuleLineResponse> linesOf(Long eventTypeRulePk) {
        List<RuleLine> lines = lineRepository.findByEventTypeRulePk(eventTypeRulePk);
        Map<Long, List<RuleLineDimension>> tagsByLine = tagsByLine(lines);
        return lines.stream()
            .map(line -> lineMapper.toResponse(line,
                tagsByLine.getOrDefault(line.getRuleLinePk(), List.of())))
            .toList();
    }

    /** The lines' tags in ONE read (QR-FIN-057), grouped by line pk; an empty map for no lines. */
    private Map<Long, List<RuleLineDimension>> tagsByLine(List<RuleLine> lines) {
        if (lines.isEmpty()) {
            return Map.of();
        }
        List<Long> linePks = lines.stream().map(RuleLine::getRuleLinePk).toList();
        return ruleLineDimensionRepository.findByRuleLine_RuleLinePkIn(linePks).stream()
            .collect(Collectors.groupingBy(tag -> tag.getRuleLine().getRuleLinePk()));
    }
}
