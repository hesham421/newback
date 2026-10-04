package com.erp.fin.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.entity.AccountMapping;
import com.erp.fin.exception.FinErrorCodes;
import java.util.Optional;

/**
 * Domain companion for ENT-FIN-015 (AccountMapping, v2): RULE-FIN-020 ({@code assertResolved}),
 * RULE-FIN-022 ({@code assertKeyFree}), RULE-FIN-023 ({@code assertAccountEligible}) and
 * RULE-FIN-024 ({@code assertBusinessValueDefined}). Every fact — the QR-FIN-052 existence
 * answer, QR-FIN-060's account flags, MDL's PAYMENT_METHOD membership and the
 * QR-FIN-055 lookup — is fetched by the service and passed in (A.0.3, A.0.6).
 */
public final class AccountMappingDomain {

    /**
     * The FIN_EVENT_BUSINESS_FIELD code whose business value must be an active PAYMENT_METHOD
     * code (RULE-FIN-024, SRS A6); any other field stores the host's code as-is.
     */
    public static final String BUSINESS_FIELD_PAYMENT_METHOD = "PAYMENT_METHOD";

    private final Long accountMappingPk;
    private final String eventTypeCode;
    private final String businessFieldCode;
    private final String businessValue;
    private final Long accountPk;
    private final boolean active;

    private AccountMappingDomain(Long accountMappingPk, String eventTypeCode,
                                 String businessFieldCode, String businessValue,
                                 Long accountPk, boolean active) {
        this.accountMappingPk = accountMappingPk;
        this.eventTypeCode = eventTypeCode;
        this.businessFieldCode = businessFieldCode;
        this.businessValue = businessValue;
        this.accountPk = accountPk;
        this.active = active;
    }

    /**
     * API-FIN-039 (create account mapping): RULE-FIN-022 at construction, over the key as the
     * service will store it (blanks trimmed, ADR-FIN-039).
     *
     * @param activeMappingExists QR-FIN-052's answer, resolved by the service
     * @throws LocalizedException {@code FIN-409-MAPPING-DUP}
     */
    public static AccountMappingDomain create(String eventTypeCode, String businessFieldCode,
                                              String businessValue, Long accountPk,
                                              boolean activeMappingExists) {
        assertKeyFree(activeMappingExists, eventTypeCode, businessFieldCode, businessValue);
        return new AccountMappingDomain(null, eventTypeCode, businessFieldCode,
            businessValue == null ? null : businessValue.trim(), accountPk, true);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static AccountMappingDomain from(AccountMapping entity) {
        return new AccountMappingDomain(entity.getAccountMappingPk(),
            entity.getEventTypeCode(),
            entity.getBusinessFieldCode(),
            entity.getBusinessValue(),
            entity.getAccount() == null ? null : entity.getAccount().getAccountPk(),
            Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    /**
     * RULE-FIN-022 — one active mapping per (event type, business field, business value).
     * {@code Status.ALREADY_EXISTS} (409), the module's duplicate-key precedent
     * ({@code DimensionValueDomain.create}); the store's own refusal on
     * {@code UQ_FIN_ACCOUNT_MAPPING_ACTIVE_KEY} answers the same code (ADR-FIN-030).
     *
     * @throws LocalizedException {@code FIN-409-MAPPING-DUP}
     */
    public static void assertKeyFree(boolean activeMappingExists, String eventTypeCode,
                                     String businessFieldCode, String businessValue) {
        if (activeMappingExists) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FinErrorCodes.FIN_409_MAPPING_DUP, eventTypeCode, businessFieldCode, businessValue);
        }
    }

    /**
     * RULE-FIN-023 — API-FIN-039/040: the mapped account must be an active leaf on save
     * (QR-FIN-060's flags). Does not replace RULE-FIN-007 at post time.
     *
     * @throws LocalizedException {@code FIN-422-MAPPED-ACCOUNT-INELIGIBLE}
     */
    public static void assertAccountEligible(boolean accountIsLeaf, boolean accountIsActive) {
        if (!accountIsLeaf || !accountIsActive) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_MAPPED_ACCOUNT_INELIGIBLE);
        }
    }

    /**
     * RULE-FIN-024 — API-FIN-039: only when the business field is PAYMENT_METHOD must the value
     * be an active PAYMENT_METHOD code; the membership fact comes from MDL via the service.
     *
     * @throws LocalizedException {@code FIN-422-INVALID-PAYMENT-METHOD}
     */
    public static void assertBusinessValueDefined(String businessFieldCode,
                                                  boolean valueIsActivePaymentMethodCode) {
        if (BUSINESS_FIELD_PAYMENT_METHOD.equalsIgnoreCase(businessFieldCode)
            && !valueIsActivePaymentMethodCode) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_INVALID_PAYMENT_METHOD, businessFieldCode);
        }
    }

    /**
     * RULE-FIN-020 — API-FIN-020 build: the account a MAPPING line resolves to is the one active
     * mapping QR-FIN-055 found; none means the event is rejected, never a default account. An
     * inactive mapping counts as none.
     *
     * @param activeMapping QR-FIN-055's answer, as returned by the repository
     * @return the resolved mapping, for its {@link #getAccountPk()}
     * @throws LocalizedException {@code FIN-422-UNMAPPED-VALUE}
     */
    public static AccountMappingDomain assertResolved(Optional<AccountMapping> activeMapping,
                                                      String businessFieldCode,
                                                      String businessValue) {
        return activeMapping
            .map(AccountMappingDomain::from)
            .filter(AccountMappingDomain::isActive)
            .orElseThrow(() -> new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_UNMAPPED_VALUE, businessFieldCode, businessValue));
    }

    public Long getAccountMappingPk() {
        return accountMappingPk;
    }

    public String getEventTypeCode() {
        return eventTypeCode;
    }

    public String getBusinessFieldCode() {
        return businessFieldCode;
    }

    public String getBusinessValue() {
        return businessValue;
    }

    public Long getAccountPk() {
        return accountPk;
    }

    public boolean isActive() {
        return active;
    }
}
