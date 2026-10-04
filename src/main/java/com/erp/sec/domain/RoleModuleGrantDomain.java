package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.RoleModuleGrant;
import com.erp.sec.exception.SecErrorCodes;

/**
 * Domain companion for ENT-SEC-007 (RoleModuleGrant): API-SEC-014's duplication guard
 * ({@code UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE}) → {@code SEC-409-GRANT-DUP}. RULE-SEC-003's
 * cascade revoke is a service action, not a decision, and the "must be active" half resolves as
 * 404 at load time — see governance/project-artifacts/sec-implementation-notes.md.
 */
public final class RoleModuleGrantDomain {

    private final Long roleId;
    private final Long moduleId;

    private RoleModuleGrantDomain(Long roleId, Long moduleId) {
        this.roleId = roleId;
        this.moduleId = moduleId;
    }

    /**
     * Construction-time decision for API-SEC-014 (grant module to role).
     *
     * @param grantAlreadyExists UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE already pairs role + module
     * @throws LocalizedException {@code SEC-409-GRANT-DUP} (Status.ALREADY_EXISTS → 409)
     */
    public static RoleModuleGrantDomain create(Long roleId,
                                               Long moduleId,
                                               boolean grantAlreadyExists) {
        if (grantAlreadyExists) {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecErrorCodes.SEC_409_GRANT_DUP);
        }
        return new RoleModuleGrantDomain(roleId, moduleId);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static RoleModuleGrantDomain from(RoleModuleGrant entity) {
        return new RoleModuleGrantDomain(
            entity.getRole() == null ? null : entity.getRole().getRolePk(),
            entity.getModule() == null ? null : entity.getModule().getModuleRegPk());
    }

    public Long getRoleId() {
        return roleId;
    }

    public Long getModuleId() {
        return moduleId;
    }
}
