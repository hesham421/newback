package com.erp.notif;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.erp.events.NotificationRequestedEvent;
import com.erp.notif.crossmodule.DispatchCommand;
import com.erp.notif.crossmodule.NotificationDispatchApi;
import com.erp.notif.service.NotificationRequeueJob;
import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractAsyncIntegrationTest;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * tenant-maturity C12 (RULE-NOTIF-024) — a suspended tenant's queued notification is neither claimed (the after-commit
 * delivery of a dispatch made while it is suspended) nor requeued, keeps {@code QUEUED} with no attempt, and is sent once
 * {@code TenantActivatedEvent} re-dispatches it. The async context of the other NOTIF delivery tests.
 */
class NotificationSuspendedTenantIntegrationTest extends AbstractAsyncIntegrationTest {

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NotificationDispatchApi dispatchApi;
    @Autowired
    private NotificationRequeueJob requeueJob;

    @Test
    void aSuspendedTenantsQueuedNotification_isNeitherClaimedNorRequeued_andIsSentOnceTheTenantIsActive() {
        NotifTestFixtures fixtures = new NotifTestFixtures(port, jdbc, passwordEncoder);
        String platformToken = fixtures.token("PLATFORM", fixtures.platformOperator());
        String code = NotifTestFixtures.unique("HOLD").toUpperCase().replace("-", "_");
        long tenantId = fixtures.provisionTenant(platformToken, code);
        long recipient = jdbc.queryForObject("SELECT USER_PK FROM SEC_USER WHERE TENANT_ID = ? AND USERNAME = ?"
            + " AND REALM = 'STAFF'", Long.class, tenantId, NotifTestFixtures.TENANT_ADMIN);
        String status = "/api/v1/platform/tenants/" + tenantId + "/status";
        assertThat(fixtures.patch(platformToken, status, "{\"statusCode\":\"SUSPENDED\",\"reason\":\"Hold the mail\"}")
            .statusCode()).isEqualTo(200);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("notif-dispatcher", null, List.of()));

        long logId = TenantContext.callAs(tenantId, () -> dispatchApi.dispatch(new DispatchCommand(recipient,
            "ACCOUNT_ACTIVATION", List.of("EMAIL"), "TEST", null, null, Map.of("email", "held@example.test",
                "actionLink", "https://app.example.test/activate?token=t", "expiresAt", "soon")))).get(0);
        awaitExecutorIdle();

        Map<String, Object> held = row(tenantId, logId);
        assertThat(held.get("notification_status_id")).isEqualTo("QUEUED");
        assertThat(((Number) held.get("attempts")).intValue()).as("not claimed").isZero();
        assertThat(held.get("next_attempt_at")).isNull();
        verify(mailSender, never()).send(any(MimeMessage.class));

        jdbc.update("UPDATE NOTIF_LOG SET CREATED_AT = CREATED_AT - INTERVAL '1 hour' WHERE TENANT_ID = ? AND ID = ?",
            tenantId, logId);
        requeueJob.requeueStale();
        awaitExecutorIdle();
        assertThat(requestedEvents(logId)).as("the requeue job skipped the suspended tenant").isEqualTo(1);
        assertThat(((Number) row(tenantId, logId).get("attempts")).intValue()).isZero();

        assertThat(fixtures.patch(platformToken, status, "{\"statusCode\":\"ACTIVE\"}").statusCode()).isEqualTo(200);
        await().atMost(ASYNC_TIMEOUT).until(() -> "SENT".equals(row(tenantId, logId).get("notification_status_id")));
        assertThat(((Number) row(tenantId, logId).get("attempts")).intValue()).isEqualTo(1);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        assertThat(requestedEvents(logId)).as("re-dispatched once, on TenantActivatedEvent").isEqualTo(2);
    }

    private long requestedEvents(long logId) {
        return probe.all().stream()
            .filter(r -> r.event() instanceof NotificationRequestedEvent requested
                && requested.getNotificationLogId() == logId)
            .count();
    }

    private Map<String, Object> row(long tenantId, long logId) {
        return jdbc.queryForMap("SELECT * FROM NOTIF_LOG WHERE TENANT_ID = ? AND ID = ?", tenantId, logId);
    }
}
