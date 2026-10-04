package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.domain.EventTypeRuleDomain;
import com.erp.fin.dto.RuleLineCreateRequest;
import com.erp.fin.dto.RuleLineDimensionTagRequest;
import com.erp.fin.dto.RuleLineResponse;
import com.erp.fin.entity.Dimension;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.EventTypeRule;
import com.erp.fin.entity.RuleLine;
import com.erp.fin.entity.RuleLineDimension;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.RuleLineMapper;
import com.erp.fin.repository.DimensionRepository;
import com.erp.fin.repository.DimensionValueRepository;
import com.erp.fin.repository.EventTypeRuleRepository;
import com.erp.fin.repository.RuleLineDimensionRepository;
import com.erp.fin.repository.RuleLineRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for ENT-FIN-010 (RuleLine) and its ENT-FIN-016 tags — API-FIN-011
 * (SVC-API-CRUD.md, MODIFIED v2). The endpoint is a method on {@code EventTypeRuleController}
 * (A.6.9); only the service is separate. No caching annotations — FIN's approved cache register
 * is empty. Every "is this line allowed?" decision belongs to {@link EventTypeRuleDomain}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuleLineService {

    private final RuleLineRepository repository;
    private final RuleLineDimensionRepository ruleLineDimensionRepository;
    private final EventTypeRuleRepository eventTypeRuleRepository;
    private final DimensionRepository dimensionRepository;
    private final DimensionValueRepository dimensionValueRepository;
    private final RuleLineMapper mapper;
    private final FinLookupValidationService lookupValidation;

    /** A tag's request together with the QR-FIN-061 rows it names, resolved before any decision. */
    private record ResolvedTag(RuleLineDimensionTagRequest request, Dimension dimension,
                               DimensionValue value) {
    }

    /**
     * API-FIN-011 — one transaction, the block's Orchestration order: lock the parent rule
     * (PESSIMISTIC_WRITE, the only guard on lineNo and RULE-FIN-003's sibling set) → lookup codes
     * (XM-FIN-001) → RULE-FIN-018 → RULE-FIN-019 → RULE-FIN-003 over the prospective line set →
     * QR-FIN-061 → RULE-FIN-025 → RULE-FIN-026 → persist the line (QR-FIN-015) and its tags
     * (QR-FIN-056). {@code lineNo} is the rule's highest lineNo + 1.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_RULES_UPDATE)")
    public ServiceResult<RuleLineResponse> create(Long eventTypeRuleId,
                                                   RuleLineCreateRequest request) {
        log.info("Adding RuleLine to EventTypeRule ID: {}", eventTypeRuleId);

        EventTypeRule parent = eventTypeRuleRepository.lockForLineAppend(eventTypeRuleId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_RULE, eventTypeRuleId));

        List<RuleLineDimensionTagRequest> tagRequests =
            request.getDimensionTags() == null ? List.of() : request.getDimensionTags();

        lookupValidation.assertValidCode(FinLookupValidationService.KEY_AMOUNT_SOURCE_TYPE,
            request.getAmountSourceTypeCode());
        lookupValidation.assertValidCode(FinLookupValidationService.KEY_DEBIT_CREDIT,
            request.getDirectionCode());
        lookupValidation.assertValidCode(FinLookupValidationService.KEY_DISTRIBUTION_TYPE,
            request.getDistributionTypeCode());
        lookupValidation.assertValidCodeIfPresent(
            FinLookupValidationService.KEY_FIN_EVENT_BUSINESS_FIELD,
            request.getAccountBusinessFieldCode());
        for (RuleLineDimensionTagRequest tag : tagRequests) {
            lookupValidation.assertValidCode(
                FinLookupValidationService.KEY_FIN_DIMENSION_VALUE_SOURCE, tag.getValueSourceCode());
            lookupValidation.assertValidCodeIfPresent(
                FinLookupValidationService.KEY_FIN_EVENT_BUSINESS_FIELD, tag.getBusinessFieldCode());
        }

        EventTypeRuleDomain.assertDerivationSupported(request.getAccountDerivationTypeCode(),
            lookupValidation.isActiveCode(FinLookupValidationService.KEY_ACCOUNT_DERIVATION_TYPE,
                request.getAccountDerivationTypeCode()));
        EventTypeRuleDomain.assertDerivationSpec(request.getAccountDerivationTypeCode(),
            request.getAccountDerivationValue(), request.getAccountBusinessFieldCode());

        List<RuleLine> existingLines = repository.findByEventTypeRulePk(eventTypeRuleId);
        int nextLineNo = existingLines.stream()
            .map(RuleLine::getLineNo)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0) + 1;
        RuleLine newLine = mapper.toEntity(request, parent, nextLineNo);

        List<RuleLine> prospectiveLines = new ArrayList<>(existingLines);
        prospectiveLines.add(newLine);
        EventTypeRuleDomain.from(parent).assertRemainderLineSetValid(prospectiveLines);

        List<ResolvedTag> resolvedTags = new ArrayList<>(tagRequests.size());
        for (RuleLineDimensionTagRequest tag : tagRequests) {
            resolvedTags.add(new ResolvedTag(tag, resolveDimension(tag.getDimensionId()),
                resolveDimensionValue(tag.getDimensionValueId())));
        }

        EventTypeRuleDomain.assertTagsDistinct(
            tagRequests.stream().map(RuleLineDimensionTagRequest::getDimensionId).toList());
        for (ResolvedTag resolved : resolvedTags) {
            RuleLineDimensionTagRequest tag = resolved.request();
            EventTypeRuleDomain.assertTagMatchesSource(tag.getValueSourceCode(),
                tag.getDimensionId(), tag.getDimensionValueId(),
                resolved.value() == null ? null : resolved.value().getDimension().getDimensionPk(),
                tag.getBusinessFieldCode());
        }

        RuleLine saved = repository.save(newLine);

        List<RuleLineDimension> tagEntities = new ArrayList<>(resolvedTags.size());
        for (ResolvedTag resolved : resolvedTags) {
            tagEntities.add(mapper.toTagEntity(resolved.request(), saved, resolved.dimension(),
                resolved.value()));
        }
        List<RuleLineDimension> savedTags = ruleLineDimensionRepository.saveAll(tagEntities);
        log.info("Added RuleLine ID: {} with {} tag(s) to EventTypeRule ID: {}",
            saved.getRuleLinePk(), savedTags.size(), eventTypeRuleId);

        return ServiceResult.success(mapper.toResponse(saved, savedTags), Status.CREATED);
    }

    /** QR-FIN-061, the dimension half — FK resolution, not a rule: unknown is FIN-404-DIMENSION. */
    private Dimension resolveDimension(Long dimensionId) {
        return dimensionRepository.findById(dimensionId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_DIMENSION, dimensionId));
    }

    /**
     * QR-FIN-061, the value half — FK resolution, not a rule: a {@code null} id means no constant
     * value was submitted (a BUSINESS_FIELD tag), a non-null id that resolves to nothing is
     * FIN-404-DIMVALUE. Whether the value may be absent is RULE-FIN-026's decision, in the Domain.
     */
    private DimensionValue resolveDimensionValue(Long dimensionValueId) {
        if (dimensionValueId == null) {
            return null;
        }
        return dimensionValueRepository.findById(dimensionValueId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_DIMVALUE, dimensionValueId));
    }
}
