package com.erp.sec.crossmodule;

import java.util.List;
import java.util.Optional;

/**
 * SEC's user-directory cross-module surface (REQ-SEC-034, REQ-SEC-035): direct Spring interface
 * injection, never loopback HTTP. (Module-registry reads are the sibling {@link SecModuleRegistryApi}.) Both methods are reads and return narrow read-models only —
 * never a JPA entity, an internal DTO, a credential, a session or a grant row.
 */
public interface SecUserDirectoryApi {

    /**
     * REQ-SEC-034 — a user's contact details, for a consumer that must reach that user out of
     * band. Consumed by NOTIF's {@code SecRecipientDirectory} (XM-NOTIF-001) for the
     * recipient-active check, and by any module that needs the email to pass to NOTIF. Empty when the id is unknown.
     */
    Optional<UserContact> findContact(Long userPk);

    /**
     * REQ-SEC-035 / QR-SEC-039 — the user ids currently holding {@code permissionCode} through an
     * active role. Available to a consumer module that needs the holders of one of its own
     * permission codes; no module consumes it today.
     */
    List<Long> findUserIdsHoldingPermission(String permissionCode);

    /**
     * erp-core step 08 — the {@code SEC_USER} id of the authenticated caller (either realm), for a
     * consumer that keys its own rows by user id (NOTIF's in-app inbox). Empty when the principal is
     * not a user account.
     */
    Optional<Long> findCurrentUserId();

    /** REQ-SEC-090 (tenant-maturity B) — the current tenant's STAFF users, any status (TENANT usage figures). */
    int countStaff();

    /** REQ-SEC-090 — the current tenant's CUSTOMER accounts, any status. */
    int countCustomers();

    /** REQ-SEC-090 — the current tenant's open sessions ({@code TERMINATED_AT} NULL), either realm. */
    int countActiveSessions();
}
