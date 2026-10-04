package com.erp.notif.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.notif.entity.NotificationInboxItem;
import com.erp.notif.entity.NotificationLog;
import com.erp.notif.exception.NotifErrorCodes;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Unit test (erp-core step 08): the event-driven log state machine and the inbox ownership rule. */
class NotificationDomainsTest {

    @Test
    void logLifecycle_pendingToQueuedToAFinalState_andFinalStatesAreFinal() {
        assertThat(log("PENDING")).satisfies(d -> {
            d.assertCanTransitionTo(NotificationLogDomain.STATUS_QUEUED);
            d.assertCanTransitionTo(NotificationLogDomain.STATUS_CHANNEL_DISABLED);
        });
        NotificationLogDomain queued = log(NotificationLogDomain.STATUS_QUEUED);
        queued.assertCanTransitionTo(NotificationLogDomain.STATUS_SENT);
        queued.assertCanTransitionTo(NotificationLogDomain.STATUS_FAILED);
        queued.assertCanTransitionTo(NotificationLogDomain.STATUS_SKIPPED_NO_PROVIDER);
        assertThat(queued.isAwaitingDelivery()).isTrue();

        for (String done : new String[] {"SENT", "FAILED", "SKIPPED_NO_PROVIDER", "CHANNEL_DISABLED"}) {
            assertThat(log(done).isAwaitingDelivery()).isFalse();
            assertThatThrownBy(() -> log(done).assertCanTransitionTo(NotificationLogDomain.STATUS_QUEUED))
                .isInstanceOfSatisfying(LocalizedException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(Status.BUSINESS_RULE_VIOLATION);
                    assertThat(e.getErrorCode()).isEqualTo(NotifErrorCodes.NOTIF_LOG_INVALID_TRANSITION);
                });
        }
        // a pending row is never sent directly any more: it must be queued first
        assertThatThrownBy(() -> log("PENDING").assertCanTransitionTo(NotificationLogDomain.STATUS_SENT))
            .isInstanceOf(LocalizedException.class);
    }

    @Test
    void attemptsLeft_untilTheConfiguredCeiling() {
        assertThat(NotificationLogDomain.hasAttemptsLeft(4, 5)).isTrue();
        assertThat(NotificationLogDomain.hasAttemptsLeft(5, 5)).isFalse();
    }

    @Test
    void inboxItem_onlyItsRecipient_mayUseIt_andMarkReadIsIdempotent() {
        NotificationInboxItem item = NotificationInboxItem.builder().id(9L).recipientUserId(42L).build();
        NotificationInboxDomain domain = NotificationInboxDomain.from(item);

        domain.assertOwnedBy(42L);
        assertThat(domain.needsMarkRead()).isTrue();
        for (Long stranger : new Long[] {43L, null}) {
            assertThatThrownBy(() -> domain.assertOwnedBy(stranger))
                .isInstanceOfSatisfying(LocalizedException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
                    assertThat(e.getErrorCode()).isEqualTo(NotifErrorCodes.INBOX_ITEM_NOT_FOUND);
                });
        }

        item.markRead(Instant.now());
        assertThat(NotificationInboxDomain.from(item).needsMarkRead()).isFalse();
    }

    private static NotificationLogDomain log(String status) {
        return NotificationLogDomain.from(NotificationLog.builder().notificationStatusId(status).build());
    }
}
