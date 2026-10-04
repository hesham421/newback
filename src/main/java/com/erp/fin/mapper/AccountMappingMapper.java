package com.erp.fin.mapper;

import com.erp.fin.dto.AccountMappingCreateRequest;
import com.erp.fin.dto.AccountMappingResponse;
import com.erp.fin.dto.AccountMappingUpdateRequest;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.AccountMapping;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-015 (AccountMapping, v2) — build-create-mapper, no MapStruct. Both
 * write methods take the already-resolved {@code Account} as a compile-time-safe parameter (SH.3):
 * the mapper never looks a FK up. {@code toResponse} reads the account's display columns through
 * the association, which the service always does inside its own transaction.
 */
@Component
public class AccountMappingMapper {

    public AccountMapping toEntity(AccountMappingCreateRequest request, Account account) {
        if (request == null) {
            return null;
        }
        return AccountMapping.builder()
            .eventTypeCode(request.getEventTypeCode())
            .businessFieldCode(request.getBusinessFieldCode())
            .businessValue(request.getBusinessValue())
            .account(account)
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /**
     * The key (eventTypeCode, businessFieldCode, businessValue) is fixed after create
     * (ADR-FIN-024, A.4.4) and {@code isActiveFl} moves only through API-FIN-041 — only the
     * account changes.
     */
    public void updateEntityFromRequest(AccountMapping entity, AccountMappingUpdateRequest request,
                                        Account account) {
        if (entity == null || request == null) {
            return;
        }
        entity.setAccount(account);
    }

    public AccountMappingResponse toResponse(AccountMapping entity) {
        if (entity == null) {
            return null;
        }
        Account account = entity.getAccount();
        return AccountMappingResponse.builder()
            .accountMappingPk(entity.getAccountMappingPk())
            .eventTypeCode(entity.getEventTypeCode())
            .businessFieldCode(entity.getBusinessFieldCode())
            .businessValue(entity.getBusinessValue())
            .accountId(account == null ? null : account.getAccountPk())
            .accountCode(account == null ? null : account.getCode())
            .accountNameAr(account == null ? null : account.getNameAr())
            .accountNameEn(account == null ? null : account.getNameEn())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .createdBy(entity.getCreatedBy())
            .createdAt(entity.getCreatedAt())
            .updatedBy(entity.getUpdatedBy())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
