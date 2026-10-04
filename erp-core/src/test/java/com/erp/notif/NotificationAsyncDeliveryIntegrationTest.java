package com.erp.notif;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.erp.events.NotificationDispatchedEvent;
import com.erp.events.NotificationFailedEvent;
import com.erp.notif.crossmodule.DispatchCommand;
import com.erp.notif.crossmodule.NotificationDispatchApi;
import com.erp.notif.domain.NotificationLogDomain;
import com.erp.notif.exception.NotifErrorCodes;
import com.erp.notif.repository.NotificationLogRepository;
import com.erp.notif.service.NotificationRequeueJob;
import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractAsyncIntegrationTest;
import com.erp.testsupport.DomainEventProbe;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 08 — event-driven NOTIF delivery, through the unchanged public
 * {@link NotificationDispatchApi}: dispatch persists {@code QUEUED} and returns at once; the
 * asynchronous worker delivers through the channel's provider (EMAIL with a Mockito
 * {@code JavaMailSender}), retries with Spring Retry, and ends {@code SENT}, {@code FAILED} or
 * {@code SKIPPED_NO_PROVIDER}; the requeue job re-dispatches stale rows; the worker runs as the
 * dispatching tenant. Every asynchronous outcome is awaited by polling {@code NOTIF_LOG}.
 */
class NotificationAsyncDeliveryIntegrationTest extends AbstractAsyncIntegrationTest {

    private static final long PLATFORM = 1L;
    private static final String TEMPLATE = "ACCOUNT_ACTIVATION";

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private NotificationDispatchApi dispatchApi;
    @Autowired
    private NotificationLogRepository logRepository;
    @Autowired
    private NotificationRequeueJob requeueJob;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private NotifTestFixtures fixtures;
    private long recipientId;

    @BeforeEach
    void recipientAndPrincipal() {
        fixtures = new NotifTestFixtures(port, jdbc, passwordEncoder);
        recipientId = fixtures.activeUser(PLATFORM, NotifTestFixtures.unique("notif-rcpt-"));
        authenticateAs("notif-dispatcher");
    }

    @Test
    void emailDispatch_isQueuedThenSentAsynchronously_andLogUpdated() throws Exception {
        List<Long> ids = dispatchApi.dispatch(emailCommand("someone@example.test"));

        assertThat(ids).hasSize(1);
        long id = ids.get(0);
        Map<String, Object> row = awaitStatus(id, NotificationLogDomain.STATUS_SENT);

        assertThat(((Number) row.get("attempts")).intValue()).isEqualTo(1);
        assertThat(row.get("sent_at")).isNotNull();
        assertThat(row.get("last_error")).isNull();
        assertThat(row.get("variables_json")).as("variables are cleared once final").isNull();
        verify(mailSender, times(1)).send(any(MimeMessage.class));

        DomainEventProbe.Received dispatched = awaitEvent(NotificationDispatchedEvent.class, id);
        assertThat(dispatched.event().getTenantId()).isEqualTo(PLATFORM);
        assertThat(((NotificationDispatchedEvent) dispatched.event()).getAttempts()).isEqualTo(1);
    }

    @Test
    void dispatch_returnsInUnder50ms_withoutWaitingForTheSend() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        // every send blocks until the end of the test: a synchronous send would block dispatch too
        doAnswer(invocation -> {
            release.await(30, TimeUnit.SECONDS);
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        List<Long> ids = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {   // warm-up (JIT, statement caches)
                ids.addAll(dispatchApi.dispatch(emailCommand("warmup" + i + "@example.test")));
            }
            long[] millis = new long[7];
            for (int i = 0; i < millis.length; i++) {
                long start = System.nanoTime();
                ids.addAll(dispatchApi.dispatch(emailCommand("timed" + i + "@example.test")));
                millis[i] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            }
            Arrays.sort(millis);
            System.out.println("[step08] NotificationDispatchApi.dispatch durations (ms, sorted): "
                + Arrays.toString(millis));
            assertThat(millis[millis.length / 2]).as("median dispatch time in ms").isLessThan(50L);
            for (Long id : ids) {
                assertThat(status(id)).as("row %s right after dispatch", id).isEqualTo(NotificationLogDomain.STATUS_QUEUED);
            }
        } finally {
            release.countDown();
        }
        for (Long id : ids) {
            awaitStatus(id, NotificationLogDomain.STATUS_SENT);
        }
    }

    @Test
    void providerThrowing_isRetriedFiveTimes_thenFailed_withNotificationFailedEvent() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        long id = dispatchApi.dispatch(emailCommand("fails@example.test")).get(0);
        Map<String, Object> row = awaitStatus(id, NotificationLogDomain.STATUS_FAILED);

        assertThat(((Number) row.get("attempts")).intValue()).isEqualTo(5);
        assertThat(((Number) row.get("retry_count")).intValue()).isEqualTo(4);
        assertThat((String) row.get("last_error")).contains("smtp down");
        assertThat((String) row.get("error_message")).contains("smtp down");
        assertThat(row.get("next_attempt_at")).isNull();
        assertThat(row.get("variables_json")).isNull();
        verify(mailSender, times(5)).send(any(MimeMessage.class));

        NotificationFailedEvent failed = (NotificationFailedEvent) awaitEvent(NotificationFailedEvent.class, id).event();
        assertThat(failed.getAttempts()).isEqualTo(5);
        assertThat(failed.getLastError()).contains("smtp down");
    }

    @Test
    void smsWithoutAProvider_endsSkippedNoProvider_withoutAnException() {
        jdbc.update("INSERT INTO NOTIF_CHANNEL_CONFIG (ID, TENANT_ID, CHANNEL_TYPE_ID, IS_ENABLED_FL, CREATED_BY, CREATED_AT)"
            + " SELECT nextval('SEQ_NOTIF_CHANNEL_CONFIG'), 1, 'SMS', 1, 'test', now()"
            + " WHERE NOT EXISTS (SELECT 1 FROM NOTIF_CHANNEL_CONFIG WHERE TENANT_ID = 1 AND CHANNEL_TYPE_ID = 'SMS')");
        try {
            List<Long> ids = dispatchApi.dispatch(new DispatchCommand(recipientId, TEMPLATE, List.of("SMS"), "TEST",
                null, null, Map.of("phone", "+100000000")));

            Map<String, Object> row = awaitStatus(ids.get(0), NotificationLogDomain.STATUS_SKIPPED_NO_PROVIDER);
            assertThat(((Number) row.get("attempts")).intValue()).isEqualTo(1);
            assertThat(row.get("error_message")).isEqualTo(NotifErrorCodes.NOTIF_CHANNEL_UNAVAILABLE);
            verify(mailSender, never()).send(any(MimeMessage.class));
        } finally {
            jdbc.update("DELETE FROM NOTIF_CHANNEL_CONFIG WHERE TENANT_ID = 1 AND CHANNEL_TYPE_ID = 'SMS'");
        }
    }

    @Test
    void disabledOrUnconfiguredChannel_isChannelDisabled_andNeverQueued() {
        long id = dispatchApi.dispatch(new DispatchCommand(recipientId, TEMPLATE, List.of("PUSH"), "TEST",
            null, null, Map.of())).get(0);

        assertThat(status(id)).isEqualTo(NotificationLogDomain.STATUS_CHANNEL_DISABLED);
        awaitExecutorIdle();
        assertThat(status(id)).isEqualTo(NotificationLogDomain.STATUS_CHANNEL_DISABLED);
    }

    @Test
    void requeueJob_redispatchesStaleQueuedRows_only() {
        long templateId = jdbc.queryForObject(
            "SELECT ID FROM NOTIF_TEMPLATE WHERE TENANT_ID = 1 AND TEMPLATE_CODE = ?", Long.class, TEMPLATE);
        long stale = insertQueuedRow(templateId, "timezone('UTC', now()) - interval '1 hour'");
        long fresh = insertQueuedRow(templateId, "timezone('UTC', now())");
        try {
            Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
            List<Long> staleIds = TenantContext.callAs(PLATFORM,
                () -> logRepository.findStale(NotificationLogDomain.STATUS_QUEUED, cutoff).stream()
                    .map(log -> log.getId()).toList());
            assertThat(staleIds).contains(stale).doesNotContain(fresh);

            assertThat(requeueJob.requeueStale()).isGreaterThanOrEqualTo(1);

            awaitStatus(stale, NotificationLogDomain.STATUS_SENT);
            awaitExecutorIdle();
            assertThat(status(fresh)).isEqualTo(NotificationLogDomain.STATUS_QUEUED);
        } finally {
            jdbc.update("DELETE FROM NOTIF_LOG WHERE ID = ?", fresh);
        }
    }

    @Test
    void theWorkerRunsAsTheDispatchingTenant() {
        String code = "NTF" + NotifTestFixtures.unique("").toUpperCase().substring(0, 8);
        long tenantId = fixtures.provisionTenant(
            fixtures.token(NotifTestFixtures.PLATFORM, fixtures.platformOperator()), code);
        long tenantAdmin = jdbc.queryForObject(
            "SELECT USER_PK FROM SEC_USER WHERE TENANT_ID = ? AND USERNAME = ?", Long.class, tenantId,
            NotifTestFixtures.TENANT_ADMIN);

        long id = TenantContext.callAs(tenantId, () -> dispatchApi.dispatch(new DispatchCommand(tenantAdmin,
            TEMPLATE, List.of("EMAIL"), "TEST", null, null, Map.of("email", "admin@tenant.test")))).get(0);

        Map<String, Object> row = awaitStatus(id, NotificationLogDomain.STATUS_SENT);
        assertThat(row.get("tenant_id")).isEqualTo(tenantId);

        DomainEventProbe.Received dispatched = awaitEvent(NotificationDispatchedEvent.class, id);
        assertThat(dispatched.event().getTenantId()).isEqualTo(tenantId);
        assertThat(dispatched.tenantInThread()).isEqualTo(tenantId);
    }

    // ---------------------------------------------------------------------------------------------

    private DispatchCommand emailCommand(String email) {
        return new DispatchCommand(recipientId, TEMPLATE, List.of("EMAIL"), "TEST", null, null,
            Map.of("email", email, "actionLink", "https://app.example.test/activate?token=t", "expiresAt", "soon"));
    }

    /** Audit timestamps are stored as UTC wall-clock time (Hibernate binds {@code Instant} in UTC). */
    private long insertQueuedRow(long templateId, String createdAtSql) {
        return jdbc.queryForObject("INSERT INTO NOTIF_LOG (ID, TENANT_ID, RECIPIENT_ID, CHANNEL_TYPE_ID,"
                + " NOTIFICATION_STATUS_ID, MODULE_CODE, RETRY_COUNT, ATTEMPTS, TEMPLATE_FK, VARIABLES_JSON,"
                + " CREATED_BY, CREATED_AT, VERSION)"
                + " VALUES (nextval('SEQ_NOTIF_LOG'), 1, ?, 'EMAIL', 'QUEUED', 'TEST', 0, 0, ?,"
                + " '{\"email\":\"requeue@example.test\"}', 'test', " + createdAtSql + ", 0) RETURNING ID",
            Long.class, recipientId, templateId);
    }

    private String status(long id) {
        return jdbc.queryForObject("SELECT NOTIFICATION_STATUS_ID FROM NOTIF_LOG WHERE ID = ?", String.class, id);
    }

    private Map<String, Object> awaitStatus(long id, String expected) {
        await().atMost(ASYNC_TIMEOUT).until(() -> expected.equals(status(id)));
        return jdbc.queryForMap("SELECT * FROM NOTIF_LOG WHERE ID = ?", id);
    }

    private DomainEventProbe.Received awaitEvent(Class<? extends com.erp.events.DomainEvent> type, long logId) {
        return await().atMost(ASYNC_TIMEOUT).until(() -> probe.all().stream()
            .filter(r -> type.isInstance(r.event()))
            .filter(r -> logId == logIdOf(r.event()))
            .findFirst().orElse(null), r -> r != null);
    }

    private static long logIdOf(com.erp.events.DomainEvent event) {
        if (event instanceof NotificationDispatchedEvent dispatched) {
            return dispatched.getNotificationLogId();
        }
        if (event instanceof NotificationFailedEvent failed) {
            return failed.getNotificationLogId();
        }
        return -1L;
    }

    private static void authenticateAs(String name) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(name, null, List.of()));
    }
}
