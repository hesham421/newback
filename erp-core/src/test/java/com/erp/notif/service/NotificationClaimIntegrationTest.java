package com.erp.notif.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import com.erp.notif.channel.DeliveryResult;
import com.erp.notif.domain.NotificationLogDomain;
import com.erp.testsupport.AbstractAsyncIntegrationTest;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * erp-core 1.2.0 — the claim a delivery run writes is owned by that run:
 * <ul>
 *   <li>when recording an outcome fails (database error) after a successful send, the run's own retry
 *       recognises its claim and attempts again at once (at-least-once), instead of skipping its own
 *       lease and leaving the row {@code QUEUED} until a requeue;</li>
 * </ul>
 * Uses a Mockito spy on the delivery processor, hence its own context.
 */
class NotificationClaimIntegrationTest extends AbstractAsyncIntegrationTest {

    private static final long PLATFORM = 1L;
    private static final String TEMPLATE = "ACCOUNT_ACTIVATION";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private NotificationDeliveryWorker worker;
    @MockitoSpyBean
    private NotificationDeliveryProcessor processor;

    private long recipientId;

    @BeforeEach
    void recipient() {
        // the address comes from the row's variables; any PLATFORM account serves as the recipient id
        recipientId = jdbc.queryForObject("SELECT MIN(USER_PK) FROM SEC_USER WHERE TENANT_ID = 1", Long.class);
    }

    @Test
    void aFailedOutcomeRecord_afterASuccessfulSend_isRetriedByTheRunItself_andEndsSent() {
        String address = unique("record-fails-") + "@example.test";
        AtomicInteger sends = countSendsTo(address);
        long id = insertQueuedRow(address);
        AtomicBoolean failedOnce = new AtomicBoolean();
        doAnswer(invocation -> {
            if (failedOnce.compareAndSet(false, true)) {
                throw new DataAccessResourceFailureException("connection lost while recording");
            }
            return invocation.callRealMethod();
        }).when(processor).recordOutcome(eq(id), any(DeliveryResult.class), anyInt());

        worker.deliver(PLATFORM, id);   // the run itself, no requeue job involved

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM NOTIF_LOG WHERE ID = ?", id);
        assertThat(failedOnce).as("the first outcome record failed").isTrue();
        assertThat(row.get("notification_status_id")).isEqualTo(NotificationLogDomain.STATUS_SENT);
        assertThat(((Number) row.get("attempts")).intValue()).as("retried once after the lost record").isEqualTo(2);
        assertThat(row.get("next_attempt_at")).isNull();
        assertThat(sends.get()).as("at-least-once: the unrecorded send is repeated").isEqualTo(2);
    }

    // ---------------------------------------------------------------------------------------------

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private AtomicInteger countSendsTo(String address) {
        AtomicInteger sends = new AtomicInteger();
        doAnswer(invocation -> {
            MimeMessage message = invocation.getArgument(0);
            if (message.getAllRecipients() != null && Arrays.stream(message.getAllRecipients())
                .anyMatch(r -> address.equalsIgnoreCase(r.toString()))) {
                sends.incrementAndGet();
            }
            return null;
        }).when(mailSender).send(any(MimeMessage.class));
        return sends;
    }

    private long insertQueuedRow(String email) {
        long templateId = jdbc.queryForObject(
            "SELECT ID FROM NOTIF_TEMPLATE WHERE TENANT_ID = 1 AND TEMPLATE_CODE = ?", Long.class, TEMPLATE);
        return jdbc.queryForObject("INSERT INTO NOTIF_LOG (ID, TENANT_ID, RECIPIENT_ID, CHANNEL_TYPE_ID,"
                + " NOTIFICATION_STATUS_ID, MODULE_CODE, RETRY_COUNT, ATTEMPTS, TEMPLATE_FK, VARIABLES_JSON,"
                + " CREATED_BY, CREATED_AT, VERSION)"
                + " VALUES (nextval('SEQ_NOTIF_LOG'), 1, ?, 'EMAIL', 'QUEUED', 'TEST', 0, 0, ?, ?, 'test',"
                + " timezone('UTC', now()), 0) RETURNING ID",
            Long.class, recipientId, templateId, "{\"email\":\"" + email + "\"}");
    }
}
