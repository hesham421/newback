package com.erp.fin.mapper;

import com.erp.fin.dto.RuleLineCreateRequest;
import com.erp.fin.dto.RuleLineDimensionTagRequest;
import com.erp.fin.dto.RuleLineDimensionTagResponse;
import com.erp.fin.dto.RuleLineResponse;
import com.erp.fin.entity.Dimension;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.EventTypeRule;
import com.erp.fin.entity.RuleLine;
import com.erp.fin.entity.RuleLineDimension;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-010 (RuleLine) and its ENT-FIN-016 tags — build-create-mapper, no
 * MapStruct. Child shape throughout (A.4.2): {@code toEntity} takes the resolved parent
 * {@code EventTypeRule} and the service-assigned {@code lineNo}; {@code toTagEntity} takes the
 * saved line and the service-resolved {@code Dimension} / {@code DimensionValue} (SH.3).
 */
@Component
public class RuleLineMapper {

    public RuleLine toEntity(RuleLineCreateRequest request, EventTypeRule parent, Integer lineNo) {
        if (request == null) {
            return null;
        }
        return RuleLine.builder()
            .eventTypeRule(parent)
            .lineNo(lineNo)
            .accountDerivationTypeCode(request.getAccountDerivationTypeCode())
            .accountDerivationValue(request.getAccountDerivationValue())
            .accountBusinessFieldCode(request.getAccountBusinessFieldCode())
            .amountSourceTypeCode(request.getAmountSourceTypeCode())
            .amountSourceValue(request.getAmountSourceValue())
            .directionCode(request.getDirectionCode())
            .distributionTypeCode(request.getDistributionTypeCode())
            .isRemainderFl(request.getIsRemainderFl() != null
                ? request.getIsRemainderFl() : Boolean.FALSE)
            .build();
    }

    /** {@code dimensionValue} is {@code null} for a BUSINESS_FIELD tag (DBF-FIN-164). */
    public RuleLineDimension toTagEntity(RuleLineDimensionTagRequest request, RuleLine parentLine,
                                         Dimension dimension, DimensionValue dimensionValue) {
        if (request == null) {
            return null;
        }
        return RuleLineDimension.builder()
            .ruleLine(parentLine)
            .dimension(dimension)
            .dimensionValue(dimensionValue)
            .valueSourceCode(request.getValueSourceCode())
            .businessFieldCode(request.getBusinessFieldCode())
            .build();
    }

    public RuleLineDimensionTagResponse toTagResponse(RuleLineDimension entity) {
        if (entity == null) {
            return null;
        }
        return RuleLineDimensionTagResponse.builder()
            .ruleLineDimensionPk(entity.getRuleLineDimensionPk())
            .ruleLineId(entity.getRuleLine() == null
                ? null : entity.getRuleLine().getRuleLinePk())
            .dimensionId(entity.getDimension() == null
                ? null : entity.getDimension().getDimensionPk())
            .valueSourceCode(entity.getValueSourceCode())
            .dimensionValueId(entity.getDimensionValue() == null
                ? null : entity.getDimensionValue().getDimensionValuePk())
            .businessFieldCode(entity.getBusinessFieldCode())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    public RuleLineResponse toResponse(RuleLine entity, List<RuleLineDimension> tags) {
        if (entity == null) {
            return null;
        }
        List<RuleLineDimension> safeTags = tags == null ? List.of() : tags;
        return RuleLineResponse.builder()
            .ruleLinePk(entity.getRuleLinePk())
            .eventTypeRuleId(entity.getEventTypeRule() == null
                ? null : entity.getEventTypeRule().getEventTypeRulePk())
            .lineNo(entity.getLineNo())
            .accountDerivationTypeCode(entity.getAccountDerivationTypeCode())
            .accountDerivationValue(entity.getAccountDerivationValue())
            .accountBusinessFieldCode(entity.getAccountBusinessFieldCode())
            .amountSourceTypeCode(entity.getAmountSourceTypeCode())
            .amountSourceValue(entity.getAmountSourceValue())
            .directionCode(entity.getDirectionCode())
            .distributionTypeCode(entity.getDistributionTypeCode())
            .isRemainderFl(Boolean.TRUE.equals(entity.getIsRemainderFl()))
            .dimensionTags(safeTags.stream().map(this::toTagResponse).toList())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
