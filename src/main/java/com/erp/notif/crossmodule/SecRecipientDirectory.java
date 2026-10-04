package com.erp.notif.crossmodule;

import com.erp.sec.crossmodule.SecUserDirectoryApi;
import com.erp.sec.crossmodule.UserContact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link RecipientDirectory} backed by SEC's crossmodule user directory (XM-NOTIF-001 — REQ-SEC-034).
 * {@code SecUserDirectoryApi.findContact} is gated {@code isAuthenticated()}, the same gate every
 * dispatch entry point already carries, so the dispatching principal satisfies it.
 */
@Component
@RequiredArgsConstructor
public class SecRecipientDirectory implements RecipientDirectory {

    private final SecUserDirectoryApi secUserDirectoryApi;

    @Override
    public boolean isActive(Long recipientId) {
        return secUserDirectoryApi.findContact(recipientId).map(UserContact::active).orElse(false);
    }
}
