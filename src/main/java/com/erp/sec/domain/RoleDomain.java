package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.Role;
import com.erp.sec.exception.SecErrorCodes;

/**
 * Domain companion for ENT-SEC-002 (Role): role-code uniqueness on create (API-SEC-013, fact =
 * QR-SEC-034) → {@code SEC-409-ROLE-DUP}. {@code code} is immutable after create, so there is no
 * update-time uniqueness guard, and no API declares a deactivation guard for Role.
 */
public final class RoleDomain {

    private final String code;
    private final boolean active;

    private RoleDomain(String code, boolean active) {
        this.code = code;
        this.active = active;
    }

    /**
     * Construction-time decision for API-SEC-013 (create role).
     *
     * @throws LocalizedException {@code SEC-409-ROLE-DUP} (409)
     */
    public static RoleDomain create(String code, boolean codeAlreadyTaken) {
        if (codeAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                SecErrorCodes.SEC_409_ROLE_DUP, code);
        }
        return new RoleDomain(code, true);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static RoleDomain from(Role entity) {
        return new RoleDomain(entity.getCode(), Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }
}
