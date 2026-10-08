package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.LocalizedException;
import com.erp.sec.crossmodule.SecAdminRecoveryApi;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    private void setCutOff(Instant cutOff) {
        jdbcTemplate.update("UPDATE CORE_TENANT SET TOKENS_INVALID_BEFORE = ? WHERE ID = ?", Timestamp.from(cutOff), id);
    }

    private int openSessions() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_ACTIVE_SESSION WHERE TENANT_ID = ?"
            + " AND TERMINATED_AT IS NULL", Integer.class, id);
    }

    /** A verified customer of the tenant (registered, activated directly) and its first access token. */
    private String customerToken() {
        HttpResponse<String> registered = http.post(null, code, "/api/v1/public/customers/register", "{\"email\":\""
            + customerEmail() + "\",\"password\":\"" + TenantHttp.PASSWORD + "\",\"fullName\":\"Customer\"}");
        assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);
        jdbcTemplate.update("UPDATE SEC_USER SET STATUS_CODE = 'ACTIVE' WHERE TENANT_ID = ? AND USERNAME = ?"
            + " AND REALM = 'CUSTOMER'", id, customerEmail());
        return customerLogin();
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
