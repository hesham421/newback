package com.erp.notif.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.notif.entity.NotificationInboxItem;
import com.erp.notif.exception.NotifErrorCodes;
import java.util.Objects;

/**
 * Domain companion for {@code NOTIF_INBOX} items (erp-core step 08): decides who may read an item and
 * whether marking it read changes anything. No Spring/JPA annotations, no repository access;
 * constructed only via the static factory.
 */
public final class NotificationInboxDomain {

    private final Long id;
    private final Long recipientUserId;
    private final boolean read;

    private NotificationInboxDomain(Long id, Long recipientUserId, boolean read) {
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.read = read;
    }

    /** Reconstructs a Domain view over a persisted item — no validation. */
    public static NotificationInboxDomain from(NotificationInboxItem entity) {
        return new NotificationInboxDomain(entity.getId(), entity.getRecipientUserId(), entity.getReadAt() != null);
    }

    /**
     * Only the recipient may see or change an item. Anyone else gets the same 404 as for an unknown id
     * ({@code INBOX_ITEM_NOT_FOUND}), so item ids of other users are never confirmed.
     */
    public void assertOwnedBy(Long callerUserId) {
        if (callerUserId == null || !Objects.equals(recipientUserId, callerUserId)) {
            throw new LocalizedException(Status.NOT_FOUND, NotifErrorCodes.INBOX_ITEM_NOT_FOUND, id);
        }
    }

    /** Marking read is idempotent: an item already read keeps its first {@code READ_AT}. */
    public boolean needsMarkRead() {
        return !read;
    }
}
