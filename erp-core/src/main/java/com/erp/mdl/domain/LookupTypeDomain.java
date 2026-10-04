package com.erp.mdl.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.mdl.entity.LookupType;
import com.erp.mdl.exception.MdlErrorCodes;

/**
 * Domain companion for ENT-MDL-001 (LookupType): RULE-MDL-001 — a lookup type whose owner
 * module code has no ModuleRegistry row in SEC is rejected
 * ({@code MDL-409-MODULE-NOT-REGISTERED}) — plus the key-uniqueness pre-check (QR-MDL-013)
 * runs alongside it ({@code MDL-409-TYPE-DUP}), mirroring
 * {@code ScreenRegistryDomain.create(...)}'s two-guard shape.
 *
 * <p>RULE-MDL-003 (key immutability) is enforced by DTO shape — {@code key} is absent from
 * the update request entirely (SVC-API-CRUD.md API-MDL-003) — so no update-time guard is added
 * here; that is the documented enforcement mechanism, not a gap. RULE-MDL-004 (excluding an
 * inactive type's values from consumer reads) is a query-time read filter owned by the service
 * (LookupConsumerService, API-MDL-011), not a construction/mutation decision on this entity, so
 * it is not modelled as a guard method here either.
 */
public final class LookupTypeDomain {

    private final String key;
    private final String ownerModuleCode;

    private LookupTypeDomain(String key, String ownerModuleCode) {
        this.key = key;
        this.ownerModuleCode = ownerModuleCode;
    }

    /**
     * API-MDL-002 (create lookup type): RULE-MDL-001 first, then key uniqueness. The caller
     * (service) resolves both facts — {@code ownerModuleRegistered} via XM-MDL-001 / QR-MDL-012,
     * {@code keyAlreadyTaken} via QR-MDL-013 — before calling.
     *
     * @throws LocalizedException {@code MDL-409-MODULE-NOT-REGISTERED} / {@code MDL-409-TYPE-DUP}
     */
    public static LookupTypeDomain create(String key,
                                          String ownerModuleCode,
                                          boolean ownerModuleRegistered,
                                          boolean keyAlreadyTaken) {
        if (!ownerModuleRegistered) {
            throw new LocalizedException(Status.CONFLICT,
                MdlErrorCodes.MDL_409_MODULE_NOT_REGISTERED);
        }
        if (keyAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                MdlErrorCodes.MDL_409_TYPE_DUP, key);
        }
        return new LookupTypeDomain(key, ownerModuleCode);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static LookupTypeDomain from(LookupType entity) {
        return new LookupTypeDomain(entity.getKey(), entity.getOwnerModuleCode());
    }

    public String getKey() {
        return key;
    }

    public String getOwnerModuleCode() {
        return ownerModuleCode;
    }
}
