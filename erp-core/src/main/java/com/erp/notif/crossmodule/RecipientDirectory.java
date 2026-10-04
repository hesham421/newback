package com.erp.notif.crossmodule;

import java.util.Optional;

/**
 * NOTIF-owned port for the recipient-active check (RULE-NOTIF-007, XM-NOTIF-001). Dispatch resolves
 * a recipient's account status through this narrow interface rather than reaching into SEC directly;
 * {@link SecRecipientDirectory} backs it with SEC's crossmodule {@code SecUserDirectoryApi}.
 */
public interface RecipientDirectory {

    /**
     * True when a user account carries this id and is active, so it may receive notifications
     * (RULE-NOTIF-007). An unknown id is not active.
     */
    boolean isActive(Long recipientId);

    /**
     * erp-core step 08 — the recipient id of the authenticated caller, for the in-app inbox
     * ({@code /api/v1/notif/inbox}). Recipients of both realms (staff and customer) are
     * {@code SEC_USER} rows, so one id space serves both; empty when the caller is not a user account.
     */
    Optional<Long> currentRecipientId();
}
