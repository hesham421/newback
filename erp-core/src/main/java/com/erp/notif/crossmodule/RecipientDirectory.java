package com.erp.notif.crossmodule;

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
}
