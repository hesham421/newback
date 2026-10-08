package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.erp.events.DomainEvent;
import com.erp.events.NotificationRequestedEvent;
import com.erp.events.TenantActivatedEvent;
import com.erp.events.TenantSuspendedEvent;
import com.erp.notif.crossmodule.DispatchCommand;
import com.erp.notif.crossmodule.NotificationDispatchApi;
import com.erp.notif.service.NotificationRequeueJob;
import com.erp.tenant.crossmodule.TenantLookupApi;
import com.erp.tenant.dto.TenantStatusUpdateRequest;
import com.erp.tenant.permission.TenantPermissions;
import com.erp.tenant.service.TenantService;
import com.erp.testsupport.AbstractAsyncIntegrationTest;
import com.erp.testsupport.DomainEventProbe;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.internet.MimeMessage;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * tenant-maturity C12 (REQ-TENANT-033) — the tenant lifecycle events and what reacts to them: {@code TenantSuspendedEvent}
 * / {@code TenantActivatedEvent} after commit on real transitions only; SEC ends the suspended tenant's sessions of both
 * realms (REQ-SEC-092); NOTIF holds a suspended tenant's queued notification and sends it after the activation
 * (RULE-NOTIF-024). The async context (mocked mail sender, event probe) of the NOTIF delivery tests.
 */
class TenantLifecycleEventsIntegrationTest extends AbstractAsyncIntegrationTest {

    private static final String TENANTS = "/api/v1/platform/tenants";

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private NotificationDispatchApi dispatchApi;
    @Autowired
    private NotificationRequeueJob requeueJob;
    @Autowired
    private TenantLookupApi tenantLookup;

    private TenantHttp http;
    private String operator;
    private String platformToken;
    private String code;
    private long id;

    @BeforeEach
    void aFreshTenant() {
        http = new TenantHttp(port);
        operator = TenantHttp.platformOperator(jdbcTemplate, passwordEncoder);
        platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE, operator);
        code = TenantHttp.unique("LIFE");
        id = http.provisionTenant(platformToken, code);
    }

    @Test
    void realTransitions_publishTheirEventsAfterCommit_reAppliedAndRefusedChangesPublishNothing() {
        assertThat(status("{\"statusCode\":\"SUSPENDED\",\"reason\":\"  Unpaid invoice  \"}")).isEqualTo(200);
        awaitExecutorIdle();
        assertThat(tenantLookup.isActive(id)).isFalse();

        TenantSuspendedEvent suspended = (TenantSuspendedEvent) single(TenantSuspendedEvent.class).event();
        assertThat(suspended.getTenantId()).isEqualTo(id);
        assertThat(suspended.getTenantCode()).isEqualTo(code);
        assertThat(suspended.getReason()).isEqualTo("Unpaid invoice");
        assertThat(suspended.getActor()).isEqualTo(operator);
        assertThat(suspended.getRealm()).isEqualTo(DomainEvent.REALM_STAFF);

        assertThat(status("{\"statusCode\":\"SUSPENDED\",\"reason\":\"Another reason\"}")).as("re-applied").isEqualTo(200);
        assertThat(http.patch(platformToken, TENANTS + "/" + TenantConstants.PLATFORM_TENANT_ID + "/status",
            "{\"statusCode\":\"SUSPENDED\",\"reason\":\"Never\"}").statusCode()).isEqualTo(422);
        assertThat(http.patch(platformToken, TENANTS + "/987654321/status", "{\"statusCode\":\"ACTIVE\"}").statusCode())
            .isEqualTo(404);
        awaitExecutorIdle();
        assertThat(events(TenantSuspendedEvent.class)).hasSize(1);
        assertThat(events(TenantActivatedEvent.class)).isEmpty();

        assertThat(status("{\"statusCode\":\"ACTIVE\"}")).isEqualTo(200);
        assertThat(status("{\"statusCode\":\"ACTIVE\"}")).as("re-applied").isEqualTo(200);
        assertThat(tenantLookup.isActive(id)).isTrue();
        awaitExecutorIdle();
        TenantActivatedEvent activated = (TenantActivatedEvent) single(TenantActivatedEvent.class).event();
        assertThat(activated.getTenantId()).isEqualTo(id);
        assertThat(activated.getTenantCode()).isEqualTo(code);
        assertThat(activated.getActor()).isEqualTo(operator);
        assertThat(events(TenantSuspendedEvent.class)).hasSize(1);

        assertThat(status("{\"statusCode\":\"SUSPENDED\"}")).as("no reason").isEqualTo(400);
        awaitExecutorIdle();
        assertThat(events(TenantSuspendedEvent.class)).hasSize(1);
        assertThat(tenantLookup.isActive(null)).isFalse();
        assertThat(tenantLookup.isActive(987654321L)).isFalse();
    }

    @Test
    void aStatusChangeRolledBackByItsCaller_publishesNothing_andEndsNoSession() {
        String staff = http.token(code, "admin");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(operator, null,
            List.of(new SimpleGrantedAuthority(TenantPermissions.PLATFORM_TENANT_MANAGE))));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            tenantService.updateStatus(id, TenantStatusUpdateRequest.builder()
                .statusCode(TenantConstants.STATUS_SUSPENDED).reason("Rolled back").build());
            status.setRollbackOnly();
        });
        awaitExecutorIdle();

        assertThat(events(TenantSuspendedEvent.class)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT STATUS_CODE FROM CORE_TENANT WHERE ID = ?", String.class, id))
            .isEqualTo(TenantConstants.STATUS_ACTIVE);
        assertThat(openSessions(id)).isEqualTo(1);
        assertThat(http.get(staff, "/api/v1/sec/menu").statusCode()).isEqualTo(200);
    }

    @Test
    void suspension_endsEverySessionOfTheTenant_bothRealms_andNoOtherTenantsSession() {
        String firstStaff = http.token(code, "admin");
        String secondStaff = http.token(code, "admin");
        String customer = customerToken();
        String otherCode = TenantHttp.unique("LIFEO");
        long otherId = http.provisionTenant(platformToken, otherCode);
        String otherTenantsToken = http.token(otherCode, "admin");
        assertThat(openSessions(id)).isEqualTo(3);

        assertThat(status("{\"statusCode\":\"SUSPENDED\",\"reason\":\"End the sessions\"}")).isEqualTo(200);

        assertThat(openSessions(id)).as("closed when the PATCH answers").isZero();
        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT TERMINATED_BY FROM SEC_ACTIVE_SESSION WHERE TENANT_ID = ?",
            String.class, id)).containsExactly(operator);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_AUDIT_LOG WHERE TENANT_ID = ?"
            + " AND EVENT_TYPE_CODE = 'SESSION_TERMINATED' AND ACTOR_USER_ID IS NULL AND DETAILS_EN = ?", Integer.class, id,
            "Session terminated: the tenant was suspended by the platform operator " + operator)).isEqualTo(3);
        assertThat(openSessions(otherId)).isEqualTo(1);
        assertThat(http.get(otherTenantsToken, "/api/v1/sec/menu").statusCode()).isEqualTo(200);
        for (HttpResponse<String> refused : List.of(http.get(firstStaff, "/api/v1/sec/menu"),
                http.get(secondStaff, "/api/v1/tenant/me"), http.get(customer, "/api/v1/customers/me"),
                http.get(customer, "/api/v1/tenant/me"))) {
            assertThat(refused.statusCode()).as(refused.body()).isEqualTo(403);
            assertThat(TenantHttp.errorCode(refused)).isEqualTo("TENANT_SUSPENDED");
        }
    }

    @Test
    void aSuspendedTenantsQueuedNotification_isNeitherClaimedNorRequeued_andIsSentOnceTheTenantIsActive() throws Exception {
        long recipient = jdbcTemplate.queryForObject("SELECT USER_PK FROM SEC_USER WHERE TENANT_ID = ? AND USERNAME = 'admin'"
            + " AND REALM = 'STAFF'", Long.class, id);
        assertThat(status("{\"statusCode\":\"SUSPENDED\",\"reason\":\"Hold the mail\"}")).isEqualTo(200);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("notif-dispatcher", null, List.of()));

        long logId = TenantContext.callAs(id, () -> dispatchApi.dispatch(new DispatchCommand(recipient,
            "ACCOUNT_ACTIVATION", List.of("EMAIL"), "TEST", null, null, Map.of("email", "held@example.test",
                "actionLink", "https://app.example.test/activate?token=t", "expiresAt", "soon")))).get(0);
        awaitExecutorIdle();

        Map<String, Object> held = row(logId);
        assertThat(held.get("notification_status_id")).isEqualTo("QUEUED");
        assertThat(((Number) held.get("attempts")).intValue()).as("not claimed").isZero();
        assertThat(held.get("next_attempt_at")).isNull();
        verify(mailSender, never()).send(any(MimeMessage.class));

        jdbcTemplate.update("UPDATE NOTIF_LOG SET CREATED_AT = CREATED_AT - INTERVAL '1 hour' WHERE TENANT_ID = ? AND ID = ?",
            id, logId);
        requeueJob.requeueStale();
        awaitExecutorIdle();
        assertThat(requestedEvents(logId)).as("the requeue job skipped the suspended tenant").isEqualTo(1);
        assertThat(((Number) row(logId).get("attempts")).intValue()).isZero();

        assertThat(status("{\"statusCode\":\"ACTIVE\"}")).isEqualTo(200);
        await().atMost(ASYNC_TIMEOUT).until(() -> "SENT".equals(row(logId).get("notification_status_id")));
        assertThat(((Number) row(logId).get("attempts")).intValue()).isEqualTo(1);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        assertThat(requestedEvents(logId)).as("re-dispatched once on TenantActivatedEvent").isEqualTo(2);
    }

    private int status(String body) {
        return http.patch(platformToken, TENANTS + "/" + id + "/status", body).statusCode();
    }

    private List<DomainEventProbe.Received> events(Class<? extends DomainEvent> type) {
        return probe.all().stream()
            .filter(r -> type.isInstance(r.event()) && Long.valueOf(id).equals(r.event().getTenantId()))
            .toList();
    }

    private DomainEventProbe.Received single(Class<? extends DomainEvent> type) {
        List<DomainEventProbe.Received> received = events(type);
        assertThat(received).hasSize(1);
        assertThat(received.get(0).transactionActive()).as("delivered after commit").isFalse();
        return received.get(0);
    }

    private long requestedEvents(long logId) {
        return probe.all().stream()
            .filter(r -> r.event() instanceof NotificationRequestedEvent requested
                && requested.getNotificationLogId() == logId)
            .count();
    }

    private Map<String, Object> row(long logId) {
        return jdbcTemplate.queryForMap("SELECT * FROM NOTIF_LOG WHERE TENANT_ID = ? AND ID = ?", id, logId);
    }

    private int openSessions(long tenantId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_ACTIVE_SESSION WHERE TENANT_ID = ?"
            + " AND TERMINATED_AT IS NULL", Integer.class, tenantId);
    }

    private String customerToken() {
        String email = "c-" + code.toLowerCase() + "@shop.test";
        assertThat(http.post(null, code, "/api/v1/public/customers/register", "{\"email\":\"" + email + "\",\"password\":\""
            + TenantHttp.PASSWORD + "\",\"fullName\":\"Customer\"}").statusCode()).isEqualTo(201);
        jdbcTemplate.update("UPDATE SEC_USER SET STATUS_CODE = 'ACTIVE' WHERE TENANT_ID = ? AND USERNAME = ?"
            + " AND REALM = 'CUSTOMER'", id, email);
        HttpResponse<String> login = http.post(null, code, "/api/v1/public/customers/login",
            "{\"email\":\"" + email + "\",\"password\":\"" + TenantHttp.PASSWORD + "\"}");
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
        return JsonPath.read(login.body(), "$.data.accessToken");
    }
}
