package com.erp.sec.crossmodule;

import java.util.Optional;

/**
 * tenant-maturity B (REQ-SEC-091) — SEC's surface for the platform's recovery of a tenant administrator
 * ({@code POST /api/v1/platform/tenants/{id}/admin-reset}). Runs in the <em>current</em> tenant: the caller wraps
 * both calls in {@code TenantContext.callAs(tenantId)} and one transaction, so the check and the write are atomic.
 * Gated by the authority {@code PLATFORM_TENANT_MANAGE}. Raw passwords are never logged, returned or audited.
 */
public interface SecAdminRecoveryApi {

    /** The STAFF user {@code username} of the current tenant, with whether it holds an active super role; empty if none. */
    Optional<RecoveryTarget> findRecoveryTarget(String username);

    /**
     * Sets the password of the STAFF super user {@code username} (policy RULE-SEC-056, field {@code newPassword}),
     * flags a change at the next sign-in unless {@code requireChangeAtNextLogin} is FALSE (RULE-SEC-058), ends every
     * session of the user, records {@code ADMIN_PASSWORD_RESET} and answers the number of sessions ended. A name that
     * is not a STAFF user holding an active super role answers {@code SEC-404-USER}.
     */
    int resetSuperUserPassword(String username, String rawPassword, Boolean requireChangeAtNextLogin);
}
