package com.erp.fin.mapper;

import com.erp.fin.dto.AllocationRuleCreateRequest;
import com.erp.fin.dto.AllocationRuleResponse;
import com.erp.fin.dto.AllocationTargetResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.AllocationRule;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-013 (AllocationRule) — build-create-mapper, no MapStruct. The
 * resolved {@code sourceAccount} arrives as a compile-time-safe parameter (A.4.2, SH.3), and the
 * target responses are assembled by the service and passed in ({@code AllocationRule} declares no
 * {@code @OneToMany}, so there is no child collection to walk — A.1.19).
 */
@Component
public class AllocationRuleMapper {

    public AllocationRule toEntity(AllocationRuleCreateRequest request, Account sourceAccount) {
        if (request == null) {
            return null;
        }
        return AllocationRule.builder()
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .sourceAccount(sourceAccount)
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    public AllocationRuleResponse toResponse(AllocationRule entity,
                                             List<AllocationTargetResponse> targets) {
        if (entity == null) {
            return null;
        }
        List<AllocationTargetResponse> safeTargets = targets == null ? List.of() : targets;
        return AllocationRuleResponse.builder()
            .allocationRulePk(entity.getAllocationRulePk())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .sourceAccountId(entity.getSourceAccount() == null
                ? null : entity.getSourceAccount().getAccountPk())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .targetCount(safeTargets.size())
            .targets(safeTargets)
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
