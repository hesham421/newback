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
}
