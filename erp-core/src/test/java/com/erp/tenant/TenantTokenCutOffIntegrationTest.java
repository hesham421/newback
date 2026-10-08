package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.erp.audit.crossmodule.AuditApi;
import com.erp.audit.crossmodule.AuditEntry;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.events.DomainEventPublisher;
import com.erp.file.crossmodule.FileDocumentLookupApi;
import com.erp.file.crossmodule.FileImageStoreApi;
import com.erp.notif.crossmodule.NotificationLogQueryApi;
import com.erp.sec.crossmodule.SecAdminRecoveryApi;
import com.erp.sec.crossmodule.SecUserDirectoryApi;
import com.erp.tenant.exception.TenantErrorCodes;
import com.erp.tenant.mapper.TenantMapper;
import com.erp.tenant.permission.TenantPermissions;
import com.erp.tenant.repository.TenantRepository;
import com.erp.tenant.service.TenantLogoUrls;
import com.erp.tenant.service.TenantService;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
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
 * tenant-maturity C12 — the per-tenant token cut-off (REQ-TENANT-034, RULE-TENANT-023, ADR-TENANT-002) over real HTTP for
 * both realms and {@code /api/v1/tenant/me}, its whole-second boundary, and {@code POST /{id}/revoke-tokens}
 * (REQ-TENANT-035, RULE-TENANT-024): sessions ended, audit in the tenant and in PLATFORM, refusals. Every tenant is fresh.
 */
class TenantTokenCutOffIntegrationTest extends AbstractIntegrationTest {

    private static final String TENANTS = "/api/v1/platform/tenants";
    private static final String STAFF_PROBE = "/api/v1/sec/menu";
    private static final String CUSTOMER_PROBE = "/api/v1/customers/me";
    private static final String TENANT_ME = "/api/v1/tenant/me";

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private SecAdminRecoveryApi adminRecovery;
    @Autowired
    private PlatformTransactionManager transactionManager;
    // review round 1: the collaborators of a TenantService built by hand with a failing SEC session step
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private ObjectProvider<TenantProvisioningContributor> contributors;
    @Autowired
    private DomainEventPublisher eventPublisher;
    @Autowired
    private SecUserDirectoryApi userDirectory;
    @Autowired
    private FileDocumentLookupApi fileDocuments;
    @Autowired
    private NotificationLogQueryApi notificationLog;
    @Autowired
    private AuditApi auditApi;
    @Autowired
    private FileImageStoreApi fileImageStore;
    @Autowired
    private TenantLogoUrls logoUrls;

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
        code = TenantHttp.unique("CUT");
        id = http.provisionTenant(platformToken, code);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aTokenIssuedBeforeTheCutOffSecond_is401TenantTokenRevoked_forBothRealmsAndTenantMe_whileItsSessionIsOpen() {
        String staff = http.token(code, "admin");
        String customer = customerToken();
        long firstSecond = Math.min(TenantHttp.issuedAtOf(staff), TenantHttp.issuedAtOf(customer));
        long lastSecond = Math.max(TenantHttp.issuedAtOf(staff), TenantHttp.issuedAtOf(customer));

        // boundary: a cut-off inside the tokens' own second serves them (whole-second comparison)
        setCutOff(Instant.ofEpochSecond(firstSecond).plusMillis(999));
        assertServed(staff, customer);

        setCutOff(Instant.ofEpochSecond(lastSecond + 1));
        for (HttpResponse<String> refused : List.of(http.get(staff, STAFF_PROBE), http.get(customer, CUSTOMER_PROBE),
                http.get(staff, TENANT_ME), http.get(customer, TENANT_ME))) {
            assertThat(refused.statusCode()).as(refused.body()).isEqualTo(401);
            assertThat(TenantHttp.errorCode(refused)).isEqualTo("TENANT_TOKEN_REVOKED");
        }
        assertThat(openSessions()).as("refused by the cut-off alone, the sessions are still open").isEqualTo(2);

        TenantHttp.awaitSecondAfterIssueOf(staff);
        TenantHttp.awaitSecondAfterIssueOf(customer);
        assertServed(http.token(code, "admin"), customerLogin());
        HttpResponse<String> loginWithStaleHeader = http.post(staff, code, "/api/v1/sec/auth/login",
            "{\"username\":\"admin\",\"password\":\"" + TenantHttp.PASSWORD + "\"}");
        assertThat(loginWithStaleHeader.statusCode()).as("a public path ignores a revoked token").isEqualTo(200);
    }

    @Test
    void revokeTokens_cutsOffEveryEarlierToken_endsTheSessions_andAuditsInTheTenantAndInPlatform() {
        String staff = http.token(code, "admin");
        String customer = customerToken();
        String otherCode = TenantHttp.unique("CUTO");
        http.provisionTenant(platformToken, otherCode);
        String otherTenantsToken = http.token(otherCode, "admin");
        TenantHttp.awaitSecondAfterIssueOf(staff);
        TenantHttp.awaitSecondAfterIssueOf(customer);

        HttpResponse<String> revoked = http.post(platformToken, TENANTS + "/" + id + "/revoke-tokens", "");

        assertThat(revoked.statusCode()).as(revoked.body()).isEqualTo(200);
        Map<String, Object> data = JsonPath.read(revoked.body(), "$.data");
        assertThat(data).containsOnlyKeys("id", "code", "sessionsTerminated")
            .containsEntry("id", (int) id).containsEntry("code", code).containsEntry("sessionsTerminated", 2);
        Timestamp cutOff = jdbcTemplate.queryForObject("SELECT TOKENS_INVALID_BEFORE FROM CORE_TENANT WHERE ID = ?",
            Timestamp.class, id);
        assertThat(cutOff).isNotNull();
        assertThat(cutOff.toInstant().getNano()).as("the start of a whole second").isZero();
        assertThat(cutOff.toInstant()).as("the next whole second").isAfter(Instant.now().minusSeconds(5));
        assertThat(openSessions()).isZero();
        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT TERMINATED_BY FROM SEC_ACTIVE_SESSION WHERE TENANT_ID = ?",
            String.class, id)).containsExactly(operator);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_AUDIT_LOG WHERE TENANT_ID = ?"
            + " AND EVENT_TYPE_CODE = 'SESSION_TERMINATED' AND ACTOR_USER_ID IS NULL AND DETAILS_EN LIKE ?",
            Integer.class, id, "%revoked by the platform operator " + operator)).isEqualTo(2);

        for (HttpResponse<String> refused : List.of(http.get(staff, STAFF_PROBE), http.get(customer, CUSTOMER_PROBE),
                http.get(staff, TENANT_ME), http.get(customer, TENANT_ME))) {
            assertThat(refused.statusCode()).as(refused.body()).isEqualTo(401);
            assertThat(TenantHttp.errorCode(refused)).as("also after the session ended").isEqualTo("TENANT_TOKEN_REVOKED");
        }
        assertThat(http.get(otherTenantsToken, STAFF_PROBE).statusCode()).as("another tenant is untouched").isEqualTo(200);
        awaitInstant(cutOff.toInstant());
        assertServed(http.token(code, "admin"), customerLogin());
        assertThat(http.post(staff, code, "/api/v1/sec/auth/login",
            "{\"username\":\"admin\",\"password\":\"" + TenantHttp.PASSWORD + "\"}").statusCode()).isEqualTo(200);

        String rows = "SELECT ACTOR || '|' || ACTOR_REALM || '|' || ENTITY_TYPE || '|' || SUMMARY_EN FROM CORE_AUDIT_EVENT"
            + " WHERE TENANT_ID = ? AND ACTION = 'TOKENS_REVOKED' AND ENTITY_ID = ?";
        String summary = "Tokens of tenant " + code + " revoked; sessions terminated: 2";
        assertThat(jdbcTemplate.queryForList(rows, String.class, id, String.valueOf(id)))
            .containsExactly(operator + "|STAFF|CORE_TENANT|" + summary);
        assertThat(jdbcTemplate.queryForList(rows, String.class, TenantConstants.PLATFORM_TENANT_ID, String.valueOf(id)))
            .containsExactly(operator + "|STAFF|CORE_TENANT|" + summary);
        String cutOffDay = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(cutOff.toInstant());
        assertThat(jdbcTemplate.queryForList("SELECT COALESCE(CAST(CHANGES AS TEXT), '') || SUMMARY_EN FROM CORE_AUDIT_EVENT"
            + " WHERE ENTITY_TYPE = 'CORE_TENANT' AND ENTITY_ID = ? AND SUMMARY_EN IS NOT NULL", String.class,
            String.valueOf(id))).noneMatch(text -> text.contains(cutOffDay) || text.contains("tokensInvalidBefore"));
        assertThat(revoked.body()).doesNotContain("tokensInvalidBefore");
        assertThat(data.values()).noneMatch(value -> String.valueOf(value).contains(cutOffDay));
    }

    /**
     * Review round 1: the cut-off alone refuses a token of the revoke's own second — the reviewer's "session step failed"
     * and "login racing the termination query" cases, simulated by re-opening the token's session afterwards.
     */
    @Test
    void revokeTokens_refusesATokenOfTheRevokesOwnSecond_evenWhenItsSessionIsReopened() {
        registerCustomer();
        awaitEarlyInASecond();
        String staff = http.token(code, "admin");
        String customer = customerLogin();
        assertThat(http.post(platformToken, TENANTS + "/" + id + "/revoke-tokens", "").statusCode()).isEqualTo(200);
        jdbcTemplate.update("UPDATE SEC_ACTIVE_SESSION SET TERMINATED_AT = NULL, TERMINATED_BY = NULL WHERE TENANT_ID = ?", id);
        assertThat(openSessions()).isEqualTo(2);

        for (HttpResponse<String> refused : List.of(http.get(staff, STAFF_PROBE), http.get(customer, CUSTOMER_PROBE),
                http.get(staff, TENANT_ME), http.get(customer, TENANT_ME))) {
            assertThat(refused.statusCode()).as(refused.body()).isEqualTo(401);
            assertThat(TenantHttp.errorCode(refused)).isEqualTo("TENANT_TOKEN_REVOKED");
        }
    }

    /**
     * Review round 1: SEC's session step fails after the cut-off committed (a mocked {@code SecAdminRecoveryApi} in a
     * {@code TenantService} built by hand, so no new Spring context): the tokens are refused anyway, PLATFORM records it,
     * the call answers 500 {@code TENANT_REVOKE_SESSIONS_FAILED}, and a repeated call ends the sessions.
     */
    @Test
    void revokeTokens_whoseSessionStepFails_keepsTheCutOff_recordsItInPlatform_answers500_andARetryEndsTheSessions() {
        String staff = http.token(code, "admin");
        TenantHttp.awaitSecondAfterIssueOf(staff);
        SecAdminRecoveryApi failing = mock(SecAdminRecoveryApi.class);
        when(failing.terminateAllSessions()).thenThrow(new IllegalStateException("database down"));
        TenantService withFailingSessions = new TenantService(tenantRepository, tenantMapper, contributors,
            eventPublisher, userDirectory, failing, fileDocuments, notificationLog, transactionManager, auditApi,
            fileImageStore, logoUrls);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(operator, null,
            List.of(new SimpleGrantedAuthority(TenantPermissions.PLATFORM_TENANT_MANAGE))));

        assertThatThrownBy(() -> withFailingSessions.revokeTokens(id))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_REVOKE_SESSIONS_FAILED);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.INTERNAL_ERROR);
            });
        SecurityContextHolder.clearContext();

        Timestamp firstCutOff = jdbcTemplate.queryForObject("SELECT TOKENS_INVALID_BEFORE FROM CORE_TENANT WHERE ID = ?",
            Timestamp.class, id);
        assertThat(firstCutOff).as("the cut-off committed first").isNotNull();
        assertThat(openSessions()).as("no session ended").isEqualTo(1);
        HttpResponse<String> refused = http.get(staff, STAFF_PROBE);
        assertThat(refused.statusCode()).as("refused by the cut-off alone").isEqualTo(401);
        assertThat(TenantHttp.errorCode(refused)).isEqualTo("TENANT_TOKEN_REVOKED");
        String rows = "SELECT SUMMARY_EN FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ? AND ACTION = 'TOKENS_REVOKED' AND ENTITY_ID = ?";
        assertThat(jdbcTemplate.queryForList(rows, String.class, TenantConstants.PLATFORM_TENANT_ID, String.valueOf(id)))
            .containsExactly("Tokens of tenant " + code + " revoked; the sessions were NOT terminated: call again");
        assertThat(jdbcTemplate.queryForList(rows, String.class, id, String.valueOf(id))).isEmpty();

        awaitInstant(firstCutOff.toInstant());
        HttpResponse<String> retried = http.post(platformToken, TENANTS + "/" + id + "/revoke-tokens", "");
        assertThat(retried.statusCode()).as(retried.body()).isEqualTo(200);
        assertThat((Integer) JsonPath.read(retried.body(), "$.data.sessionsTerminated")).isEqualTo(1);
        assertThat(openSessions()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT TOKENS_INVALID_BEFORE FROM CORE_TENANT WHERE ID = ?",
            Timestamp.class, id)).as("the retry moved the cut-off forward").isAfter(firstCutOff);
    }

    /** tenant-maturity C4 (C12 follow-up): a failing PLATFORM audit write no longer replaces the 500 answer. */
    @Test
    void revokeTokens_whoseSessionStepAndPlatformAuditFail_stillAnswersSessionsFailed_withTheSessionFailureAsCause() {
        SecAdminRecoveryApi failing = mock(SecAdminRecoveryApi.class);
        IllegalStateException sessionFailure = new IllegalStateException("database down");
        when(failing.terminateAllSessions()).thenThrow(sessionFailure);
        AuditApi failingAudit = mock(AuditApi.class);
        IllegalStateException auditFailure = new IllegalStateException("audit store down");
        doThrow(auditFailure).when(failingAudit).record(any(AuditEntry.class));
        TenantService withFailures = new TenantService(tenantRepository, tenantMapper, contributors,
            eventPublisher, userDirectory, failing, fileDocuments, notificationLog, transactionManager, failingAudit,
            fileImageStore, logoUrls);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(operator, null,
            List.of(new SimpleGrantedAuthority(TenantPermissions.PLATFORM_TENANT_MANAGE))));

        assertThatThrownBy(() -> withFailures.revokeTokens(id))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_REVOKE_SESSIONS_FAILED);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.INTERNAL_ERROR);
                assertThat(e.getCause()).isSameAs(sessionFailure);
                assertThat(e.getCause().getSuppressed()).containsExactly(auditFailure);
            });
        assertThat(jdbcTemplate.queryForObject("SELECT TOKENS_INVALID_BEFORE FROM CORE_TENANT WHERE ID = ?",
            Timestamp.class, id)).as("the cut-off committed first").isNotNull();
    }

    @Test
    void revokeTokens_refusesPlatform_anUnknownTenant_aTenantAdministrator_andAnonymous_butRevokesASuspendedTenant() {
        HttpResponse<String> platform = http.post(platformToken,
            TENANTS + "/" + TenantConstants.PLATFORM_TENANT_ID + "/revoke-tokens", "");
        assertThat(platform.statusCode()).isEqualTo(422);
        assertThat(TenantHttp.errorCode(platform)).isEqualTo("TENANT_REVOKE_TOKENS_PLATFORM");
        assertThat(jdbcTemplate.queryForObject("SELECT TOKENS_INVALID_BEFORE FROM CORE_TENANT WHERE ID = 1",
            Timestamp.class)).isNull();
        assertThat(http.get(platformToken, TENANTS + "/" + id).statusCode()).as("the operator is still signed in")
            .isEqualTo(200);

        HttpResponse<String> unknown = http.post(platformToken, TENANTS + "/987654321/revoke-tokens", "");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(unknown)).isEqualTo("TENANT_NOT_FOUND");
        assertThat(http.post(http.token(code, "admin"), TENANTS + "/" + id + "/revoke-tokens", "").statusCode())
            .isEqualTo(403);
        assertThat(http.post(null, TENANTS + "/" + id + "/revoke-tokens", "").statusCode()).isEqualTo(401);
        assertThat(jdbcTemplate.queryForObject("SELECT TOKENS_INVALID_BEFORE FROM CORE_TENANT WHERE ID = ?",
            Timestamp.class, id)).as("nothing changed").isNull();

        assertThat(http.patch(platformToken, TENANTS + "/" + id + "/status",
            "{\"statusCode\":\"SUSPENDED\",\"reason\":\"Revoke while suspended\"}").statusCode()).isEqualTo(200);
        HttpResponse<String> suspended = http.post(platformToken, TENANTS + "/" + id + "/revoke-tokens", "");
        assertThat(suspended.statusCode()).as(suspended.body()).isEqualTo(200);
        assertThat((Integer) JsonPath.read(suspended.body(), "$.data.sessionsTerminated")).isZero();
    }

    @Test
    void terminateAllSessions_needsThePlatformAuthority() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("someone", null, List.of()));

        assertThatThrownBy(() -> TenantContext.runAs(id, () -> new TransactionTemplate(transactionManager)
            .executeWithoutResult(status -> adminRecovery.terminateAllSessions())))
            .isInstanceOf(LocalizedException.class)
            .extracting(e -> ((LocalizedException) e).getErrorCode()).isEqualTo("SEC-403-FORBIDDEN");
    }

    private void assertServed(String staff, String customer) {
        for (HttpResponse<String> served : List.of(http.get(staff, STAFF_PROBE), http.get(customer, CUSTOMER_PROBE),
                http.get(staff, TENANT_ME), http.get(customer, TENANT_ME))) {
            assertThat(served.statusCode()).as(served.body()).isEqualTo(200);
        }
    }

    /** Waits until the clock has reached {@code instant} (a revoke-tokens cut-off lies in the next second). */
    private static void awaitInstant(Instant instant) {
        await().atMost(Duration.ofSeconds(5)).until(() -> !Instant.now().isBefore(instant));
    }

    /** Waits for the first 300 ms of a second, so a login and a revoke made right after share that second. */
    private static void awaitEarlyInASecond() {
        await().atMost(Duration.ofSeconds(3)).pollInterval(Duration.ofMillis(10))
            .until(() -> Instant.now().getNano() < 300_000_000);
    }

    private void setCutOff(Instant cutOff) {
        jdbcTemplate.update("UPDATE CORE_TENANT SET TOKENS_INVALID_BEFORE = ? WHERE ID = ?", Timestamp.from(cutOff), id);
    }

    private int openSessions() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_ACTIVE_SESSION WHERE TENANT_ID = ?"
            + " AND TERMINATED_AT IS NULL", Integer.class, id);
    }

    /** A verified customer of the tenant (registered, activated directly) and its first access token. */
    private String customerToken() {
        registerCustomer();
        return customerLogin();
    }

    /** Registers the customer and activates it directly, without signing in. */
    private void registerCustomer() {
        HttpResponse<String> registered = http.post(null, code, "/api/v1/public/customers/register", "{\"email\":\""
            + customerEmail() + "\",\"password\":\"" + TenantHttp.PASSWORD + "\",\"fullName\":\"Customer\"}");
        assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);
        jdbcTemplate.update("UPDATE SEC_USER SET STATUS_CODE = 'ACTIVE' WHERE TENANT_ID = ? AND USERNAME = ?"
            + " AND REALM = 'CUSTOMER'", id, customerEmail());
    }

    private String customerLogin() {
        HttpResponse<String> login = http.post(null, code, "/api/v1/public/customers/login",
            "{\"email\":\"" + customerEmail() + "\",\"password\":\"" + TenantHttp.PASSWORD + "\"}");
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
        return JsonPath.read(login.body(), "$.data.accessToken");
    }

    private String customerEmail() {
        return "c-" + code.toLowerCase() + "@shop.test";
    }
}
