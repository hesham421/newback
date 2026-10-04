package com.erp.fin.mapper;

import com.erp.fin.dto.AccountCreateRequest;
import com.erp.fin.dto.AccountResponse;
import com.erp.fin.dto.AccountUpdateRequest;
import com.erp.fin.entity.Account;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-001 (Account) — build-create-mapper, no MapStruct.
 *
 * <p>{@code toEntity} takes the already-resolved parent {@code Account} as a compile-time-safe
 * parameter (A.4.2); {@code null} means a root account. The mapper never looks a FK up itself
 * (SH.3) and never normalizes case (SH.2 — {@code Account}'s own {@code @PrePersist} owns that).
 */
@Component
public class AccountMapper {

    public Account toEntity(AccountCreateRequest request, Account parent) {
        if (request == null) {
            return null;
        }
        return Account.builder()
            .code(request.getCode())
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .accountTypeCode(request.getAccountTypeCode())
            .natureCode(request.getNatureCode())
            .parentAccount(parent)
            .isLeafFl(request.getIsLeafFl() != null ? request.getIsLeafFl() : Boolean.TRUE)
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /**
     * {@code code}, {@code accountTypeCode}, {@code natureCode} and {@code parentAccount} are
     * create-only (A.4.4), {@code isActiveFl} moves only through API-FIN-004 and
     * {@code isRetainedEarningsFl} (DBF-FIN-147) is never settable through the account APIs at
     * all — see {@code Account}'s own field javadoc — so none is touched here. {@code isLeafFl} is a plain field assignment — the decision about whether the
     * requested value is allowed was already delegated to
     * {@code AccountDomain.assertLeafFlagChangeAllowed(...)} by the service.
     */
    public void updateEntityFromRequest(Account entity, AccountUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setNameAr(request.getNameAr());
        entity.setNameEn(request.getNameEn());
        entity.setIsLeafFl(request.getIsLeafFl());
    }

    public AccountResponse toResponse(Account entity) {
        if (entity == null) {
            return null;
        }
        return AccountResponse.builder()
            .accountPk(entity.getAccountPk())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .accountTypeCode(entity.getAccountTypeCode())
            .natureCode(entity.getNatureCode())
            .parentAccountId(entity.getParentAccount() == null
                ? null : entity.getParentAccount().getAccountPk())
            .isLeafFl(Boolean.TRUE.equals(entity.getIsLeafFl()))
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .isRetainedEarningsFl(Boolean.TRUE.equals(entity.getIsRetainedEarningsFl()))
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
