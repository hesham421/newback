package com.erp.notif.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.notif.crossmodule.RecipientDirectory;
import com.erp.notif.domain.NotificationInboxDomain;
import com.erp.notif.dto.InboxItemResponse;
import com.erp.notif.entity.NotificationInboxItem;
import com.erp.notif.exception.NotifErrorCodes;
import com.erp.notif.mapper.NotificationInboxMapper;
import com.erp.notif.repository.NotificationInboxRepository;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The caller's own in-app inbox (erp-core step 08, IN_APP channel): list and mark read. Works for both
 * realms — gated on authentication alone, like dispatch (RULE-NOTIF-005: no screen, so no page
 * permission) — because every caller sees only items addressed to its own {@code SEC_USER} id, resolved
 * through {@link RecipientDirectory#currentRecipientId()}. A principal that is no user account gets
 * 403 {@code NOTIF_CHANNEL_UNAVAILABLE}.
 *
 * <p>No caching annotations — NOTIF is absent from the caching approved-register.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationInboxService {

    private static final String SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(SORT_FIELD);

    private final NotificationInboxRepository repository;
    private final NotificationInboxMapper mapper;
    private final RecipientDirectory recipientDirectory;

    /** {@code GET /api/v1/notif/inbox}: the caller's items, newest first; only unread ones on request. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<Page<InboxItemResponse>> list(boolean unreadOnly, int page, int size) {
        Long me = currentRecipient();
        log.debug("Listing inbox of recipient {} (unreadOnly={}, page={}, size={})", me, unreadOnly, page, size);

        Pageable pageable = PageableBuilder.from(SearchRequest.builder()
            .sortField(SORT_FIELD).sortDirection(Sort.Direction.DESC).page(page).size(size).build(),
            ALLOWED_SORT_FIELDS);
        Page<NotificationInboxItem> items = unreadOnly
            ? repository.findByRecipientUserIdAndReadAtIsNull(me, pageable)
            : repository.findByRecipientUserId(me, pageable);
        return ServiceResult.success(items.map(mapper::toResponse));
    }

    /** {@code PATCH /api/v1/notif/inbox/{id}/read}: idempotent; another user's item is a 404. */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<InboxItemResponse> markRead(Long id) {
        Long me = currentRecipient();
        log.info("Marking inbox item {} read for recipient {}", id, me);

        NotificationInboxItem item = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, NotifErrorCodes.INBOX_ITEM_NOT_FOUND, id));
        NotificationInboxDomain domain = NotificationInboxDomain.from(item);
        domain.assertOwnedBy(me);

        if (domain.needsMarkRead()) {
            item.markRead(Instant.now());
            item = repository.save(item);
        }
        return ServiceResult.success(mapper.toResponse(item), Status.UPDATED);
    }

    private Long currentRecipient() {
        return recipientDirectory.currentRecipientId()
            .orElseThrow(() -> new LocalizedException(Status.FORBIDDEN, NotifErrorCodes.NOTIF_CHANNEL_UNAVAILABLE));
    }
}
