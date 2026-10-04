package com.erp.fin.mapper;

import com.erp.fin.dto.AllocationTargetCreateRequest;
import com.erp.fin.dto.AllocationTargetResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.AllocationRule;
import com.erp.fin.entity.AllocationTarget;
import com.erp.fin.entity.DimensionValue;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-014 (AllocationTarget) — build-create-mapper, no MapStruct. Child
 * shape: parent rule, target account and the optional dimension value all arrive already resolved
 * (A.4.2, SH.3).
 */
@Component
public class AllocationTargetMapper {

    public AllocationTarget toEntity(AllocationTargetCreateRequest request,
                                     AllocationRule parent,
                                     Integer lineNo,
                                     Account targetAccount,
                                     DimensionValue dimensionValue) {
        if (request == null) {
            return null;
        }
        return AllocationTarget.builder()
            .allocationRule(parent)
            .lineNo(lineNo)
            .targetAccount(targetAccount)
            .dimensionValue(dimensionValue)
            .distributionTypeCode(request.getDistributionTypeCode())
            .distributionValue(request.getDistributionValue())
            .isRemainderFl(request.getIsRemainderFl() != null
                ? request.getIsRemainderFl() : Boolean.FALSE)
            .build();
    }

    public AllocationTargetResponse toResponse(AllocationTarget entity) {
        if (entity == null) {
            return null;
        }
        return AllocationTargetResponse.builder()
            .allocationTargetPk(entity.getAllocationTargetPk())
            .allocationRuleId(entity.getAllocationRule() == null
                ? null : entity.getAllocationRule().getAllocationRulePk())
            .lineNo(entity.getLineNo())
            .targetAccountId(entity.getTargetAccount() == null
                ? null : entity.getTargetAccount().getAccountPk())
            .dimensionValueId(entity.getDimensionValue() == null
                ? null : entity.getDimensionValue().getDimensionValuePk())
            .distributionTypeCode(entity.getDistributionTypeCode())
            .distributionValue(entity.getDistributionValue())
            .isRemainderFl(Boolean.TRUE.equals(entity.getIsRemainderFl()))
            .build();
    }
}
