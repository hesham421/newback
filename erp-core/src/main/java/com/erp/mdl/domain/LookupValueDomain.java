package com.erp.mdl.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.mdl.entity.LookupValue;
import com.erp.mdl.exception.MdlErrorCodes;

/**
 * Domain companion for ENT-MDL-002 (LookupValue): RULE-MDL-002 — a lookup value whose code
 * already exists under the same lookup type is rejected ({@code MDL-409-VALUE-DUP}). The
 * duplication fact ({@code codeAlreadyTakenInType}) is resolved by the service via QR-MDL-014
 * (also enforced structurally by {@code UQ_MDL_LOOKUP_VALUE_TYPE_CODE} — this pre-check exists
 * to return the friendly error before the DB would reject it, per DATA-DOM.md).
 */
public final class LookupValueDomain {

    private final Long lookupTypeId;
    private final String code;

    private LookupValueDomain(Long lookupTypeId, String code) {
        this.lookupTypeId = lookupTypeId;
        this.code = code;
    }

    /**
     * API-MDL-006 (create lookup value): RULE-MDL-002. The caller (service) resolves
     * {@code codeAlreadyTakenInType} via QR-MDL-014 before calling.
     *
     * @throws LocalizedException {@code MDL-409-VALUE-DUP}
     */
    public static LookupValueDomain create(Long lookupTypeId,
                                           String code,
                                           boolean codeAlreadyTakenInType) {
        if (codeAlreadyTakenInType) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                MdlErrorCodes.MDL_409_VALUE_DUP, code);
        }
        return new LookupValueDomain(lookupTypeId, code);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static LookupValueDomain from(LookupValue entity) {
        return new LookupValueDomain(
            entity.getLookupType() == null ? null : entity.getLookupType().getLookupTypePk(),
            entity.getCode());
    }

    public Long getLookupTypeId() {
        return lookupTypeId;
    }

    public String getCode() {
        return code;
    }
}
