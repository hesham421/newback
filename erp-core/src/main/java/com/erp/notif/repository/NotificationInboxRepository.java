package com.erp.notif.repository;

import com.erp.notif.entity.NotificationInboxItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@code NOTIF_INBOX} (erp-core step 08). Every query is tenant-filtered by Hibernate;
 * the recipient filter is explicit. Module-internal.
 */
@Repository
public interface NotificationInboxRepository
    extends JpaRepository<NotificationInboxItem, Long>,
            JpaSpecificationExecutor<NotificationInboxItem> {

    /** The recipient's items (read and unread), newest first per the pageable's sort. */
    Page<NotificationInboxItem> findByRecipientUserId(Long recipientUserId, Pageable pageable);

    /** The recipient's unread items only. */
    Page<NotificationInboxItem> findByRecipientUserIdAndReadAtIsNull(Long recipientUserId, Pageable pageable);

    /** Number of unread items of the recipient. */
    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);
}
