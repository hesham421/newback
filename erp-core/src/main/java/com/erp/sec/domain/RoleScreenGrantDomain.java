package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.RoleScreenGrant;
import com.erp.sec.exception.SecErrorCodes;

/**
 * Domain companion for ENT-SEC-008 (RoleScreenGrant). Check order follows API-SEC-016's Errors
 * line: RULE-SEC-001 first — the role must hold the screen's module grant (QR-SEC-028 →
 * {@code SEC-409-NO-MODULE-GRANT}) — duplication second
 * ({@code UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN} → {@code SEC-409-GRANT-DUP}).
 */
public final class RoleScreenGrantDomain {

    private final Long roleId;
    private final Long screenId;

    private RoleScreenGrantDomain(Long roleId, Long screenId) {
        this.roleId = roleId;
        this.screenId = screenId;
    }

    /**
     * Construction-time decision for API-SEC-016 (grant screen to role).
     *
     * @param roleHoldsModuleGrant QR-SEC-028 — the grant covering this screen's module
     * @throws LocalizedException {@code SEC-409-NO-MODULE-GRANT} / {@code SEC-409-GRANT-DUP} (409)
     */
    public static RoleScreenGrantDomain create(Long roleId,
                                               Long screenId,
                                               boolean roleHoldsModuleGrant,
                                               boolean grantAlreadyExists) {
        if (!roleHoldsModuleGrant) {
            throw new LocalizedException(Status.CONFLICT, SecErrorCodes.SEC_409_NO_MODULE_GRANT);
        }
        if (grantAlreadyExists) {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecErrorCodes.SEC_409_GRANT_DUP);
        }
        return new RoleScreenGrantDomain(roleId, screenId);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static RoleScreenGrantDomain from(RoleScreenGrant entity) {
        return new RoleScreenGrantDomain(
            entity.getRole() == null ? null : entity.getRole().getRolePk(),
            entity.getScreen() == null ? null : entity.getScreen().getScreenRegPk());
    }

    public Long getRoleId() {
        return roleId;
    }

    public Long getScreenId() {
        return screenId;
    }
}
