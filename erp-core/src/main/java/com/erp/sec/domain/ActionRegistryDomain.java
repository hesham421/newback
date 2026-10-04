package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.exception.SecErrorCodes;

/**
 * Domain companion for ENT-SEC-006 (ActionRegistry): API-SEC-020's two guards, in the
 * Orchestration line's order — screen must already be registered
 * ({@code SEC-409-SCREEN-NOT-REGISTERED}), then derived-permissionCode uniqueness (QR-SEC-037 →
 * {@code SEC-409-ACTION-DUP}). The permissionCode derivation itself is the service's.
 */
public final class ActionRegistryDomain {

    private final String permissionCode;
    private final boolean active;

    private ActionRegistryDomain(String permissionCode, boolean active) {
        this.permissionCode = permissionCode;
        this.active = active;
    }

    /**
     * Construction-time decision for API-SEC-020 (register action).
     *
     * @param permissionCode the server-derived PERM_&lt;pageCode&gt;_&lt;actionCode&gt;
     * @throws LocalizedException {@code SEC-409-SCREEN-NOT-REGISTERED} / {@code SEC-409-ACTION-DUP}
     */
    public static ActionRegistryDomain create(String permissionCode,
                                              boolean screenRegistered,
                                              boolean permissionCodeAlreadyTaken) {
        if (!screenRegistered) {
            throw new LocalizedException(Status.CONFLICT,
                SecErrorCodes.SEC_409_SCREEN_NOT_REGISTERED);
        }
        if (permissionCodeAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                SecErrorCodes.SEC_409_ACTION_DUP, permissionCode);
        }
        return new ActionRegistryDomain(permissionCode, true);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static ActionRegistryDomain from(ActionRegistry entity) {
        return new ActionRegistryDomain(entity.getPermissionCode(),
            Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public boolean isActive() {
        return active;
    }
}
