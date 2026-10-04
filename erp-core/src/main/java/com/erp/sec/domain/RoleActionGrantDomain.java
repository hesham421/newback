package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.RoleActionGrant;
import com.erp.sec.exception.SecErrorCodes;

/**
 * Domain companion for ENT-SEC-009 (RoleActionGrant). Check order is API-SEC-017's Orchestration
 * line verbatim: RULE-SEC-002 (QR-SEC-029, {@code SEC-409-NO-SCREEN-GRANT}) → RULE-SEC-007
 * (QR-SEC-030, {@code SEC-409-NO-VIEW-GRANT}, skipped when the granted action IS VIEW) →
 * RULE-SEC-005 (QR-SEC-031, {@code SEC-409-SOD-CONFLICT}) → {@code SEC-409-GRANT-DUP}.
 */
public final class RoleActionGrantDomain {

    /**
     * The screen-level gateway action code — {@code profile.conventions.security_model
     * .gateway_action = VIEW}, cited by RULE-SEC-007 (srs-sec.md §A5 Source line).
     */
    public static final String GATEWAY_ACTION_CODE = "VIEW";

    private final Long roleId;
    private final Long actionId;
    private final String actionCode;

    private RoleActionGrantDomain(Long roleId, Long actionId, String actionCode) {
        this.roleId = roleId;
        this.actionId = actionId;
        this.actionCode = actionCode;
    }

    /**
     * API-SEC-017 (grant action to role). {@code actionCode} = VIEW exempts the grant from
     * RULE-SEC-007; {@code conflictingActionHeld} is QR-SEC-031 across every user holding this role.
     *
     * @throws LocalizedException SEC-409-NO-SCREEN-GRANT / -NO-VIEW-GRANT / -SOD-CONFLICT / -GRANT-DUP
     */
    public static RoleActionGrantDomain create(Long roleId,
                                               Long actionId,
                                               String actionCode,
                                               boolean roleHoldsScreenGrant,
                                               boolean roleHoldsViewGrant,
                                               boolean conflictingActionHeld,
                                               boolean grantAlreadyExists) {
        if (!roleHoldsScreenGrant) {
            throw new LocalizedException(Status.CONFLICT, SecErrorCodes.SEC_409_NO_SCREEN_GRANT);
        }
        if (!isGatewayAction(actionCode) && !roleHoldsViewGrant) {
            throw new LocalizedException(Status.CONFLICT, SecErrorCodes.SEC_409_NO_VIEW_GRANT);
        }
        if (conflictingActionHeld) {
            throw new LocalizedException(Status.CONFLICT, SecErrorCodes.SEC_409_SOD_CONFLICT);
        }
        if (grantAlreadyExists) {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecErrorCodes.SEC_409_GRANT_DUP);
        }
        return new RoleActionGrantDomain(roleId, actionId, actionCode);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static RoleActionGrantDomain from(RoleActionGrant entity) {
        return new RoleActionGrantDomain(
            entity.getRole() == null ? null : entity.getRole().getRolePk(),
            entity.getAction() == null ? null : entity.getAction().getActionRegPk(),
            entity.getAction() == null ? null : entity.getAction().getActionCode());
    }

    /** RULE-SEC-007 exemption test: the gateway action itself needs no prior VIEW grant. */
    public static boolean isGatewayAction(String code) {
        return GATEWAY_ACTION_CODE.equalsIgnoreCase(code);
    }

    public Long getRoleId() {
        return roleId;
    }

    public Long getActionId() {
        return actionId;
    }

    public String getActionCode() {
        return actionCode;
    }
}
