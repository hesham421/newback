package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.UserRoleAssignment;
import com.erp.sec.exception.SecErrorCodes;

/**
 * Domain companion for ENT-SEC-003 (UserRoleAssignment): RULE-SEC-005 — a user must not hold,
 * through any combination of roles, both actions of a module-declared conflicting pair
 * (API-SEC-008, fact = QR-SEC-031 across ALL the user's roles) → {@code SEC-409-SOD-CONFLICT}.
 * No uniqueness guard: API-SEC-008 replaces the assignment set wholesale.
 */
public final class UserRoleAssignmentDomain {

    private final Long userId;
    private final Long roleId;

    private UserRoleAssignmentDomain(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    /**
     * API-SEC-008 (assign roles to user), evaluated per role in the requested set;
     * {@code conflictingActionHeld} is QR-SEC-031 across every role the user holds.
     *
     * @throws LocalizedException {@code SEC-409-SOD-CONFLICT} (Status.CONFLICT → 409)
     */
    public static UserRoleAssignmentDomain create(Long userId,
                                                  Long roleId,
                                                  boolean conflictingActionHeld) {
        if (conflictingActionHeld) {
            throw new LocalizedException(Status.CONFLICT, SecErrorCodes.SEC_409_SOD_CONFLICT);
        }
        return new UserRoleAssignmentDomain(userId, roleId);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static UserRoleAssignmentDomain from(UserRoleAssignment entity) {
        return new UserRoleAssignmentDomain(
            entity.getUser() == null ? null : entity.getUser().getUserPk(),
            entity.getRole() == null ? null : entity.getRole().getRolePk());
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRoleId() {
        return roleId;
    }
}
