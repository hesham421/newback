package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.exception.SecErrorCodes;

/**
 * Domain companion for ENT-SEC-004 (ModuleRegistry): module-code uniqueness on create
 * (API-SEC-018, fact = QR-SEC-035) → {@code SEC-409-MODULE-DUP}. There is no field-level update
 * endpoint for this entity ("update: deactivate only"), so no update-time guard exists.
 */
public final class ModuleRegistryDomain {

    private final String code;
    private final boolean active;

    private ModuleRegistryDomain(String code, boolean active) {
        this.code = code;
        this.active = active;
    }

    /**
     * Construction-time decision for API-SEC-018 (register module).
     *
     * @throws LocalizedException {@code SEC-409-MODULE-DUP} (409)
     */
    public static ModuleRegistryDomain create(String code, boolean codeAlreadyTaken) {
        if (codeAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                SecErrorCodes.SEC_409_MODULE_DUP, code);
        }
        return new ModuleRegistryDomain(code, true);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static ModuleRegistryDomain from(ModuleRegistry entity) {
        return new ModuleRegistryDomain(entity.getCode(),
            Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }
}
