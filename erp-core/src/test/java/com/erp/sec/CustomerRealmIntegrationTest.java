package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.erp.notif.crossmodule.DispatchCommand;
import com.erp.notif.crossmodule.NotificationDispatchApi;
import com.erp.tenant.TenantConstants;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * erp-core step 06 — the CUSTOMER realm end to end, over HTTP through both real security chains, in a
 * freshly provisioned tenant per test (so the templates seeded by V11 for PLATFORM are proven to reach
 * new tenants). NOTIF's {@link NotificationDispatchApi} is a spy: the raw verification / reset token
 * travels only in the dispatched e-mail, so the tests read it from the captured {@link DispatchCommand}
 * — and assert the NOTIF_LOG row the dispatch wrote. Since erp-core step 08 delivery is asynchronous: the
 * row is written (QUEUED) synchronously by the dispatch, then the delivery worker retries the send and,
 * because no SMTP server listens in tests, ends it FAILED after 5 attempts — awaited, never slept.
 */
class CustomerRealmIntegrationTest extends AbstractIntegrationTest {

    private static final String CUSTOMER_PASSWORD = "Cust0mer-Passw0rd!";

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoSpyBean
    private NotificationDispatchApi notificationDispatchApi;

    private RealmHttp http;
    private String tenantCode;
    private long tenantId;

    @BeforeEach
    void freshTenant() {
        http = new RealmHttp(port);
        String platformToken = http.staffToken(TenantConstants.PLATFORM_TENANT_CODE,
            RealmHttp.platformOperator(jdbcTemplate, passwordEncoder), RealmHttp.PASSWORD);
        tenantCode = RealmHttp.unique("CR");
        tenantId = http.provisionTenant(platformToken, tenantCode);
    }

    @Test
    void register_verify_login_me_happyPath() {
        String email = "jane." + RealmHttp.unique("x").toLowerCase() + "@shop.test";

        HttpResponse<String> registered = http.register(tenantCode, email, CUSTOMER_PASSWORD, "Jane Doe");
        assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);
        assertThat((String) JsonPath.read(registered.body(), "$.data.statusCode")).isEqualTo("PENDING_VERIFICATION");
        assertThat((String) JsonPath.read(registered.body(), "$.data.realm")).isEqualTo("CUSTOMER");
        assertThat(registered.body()).doesNotContain(CUSTOMER_PASSWORD);
        long customerId = ((Number) JsonPath.read(registered.body(), "$.data.id")).longValue();

        // the verification e-mail went through NOTIF: one NOTIF_LOG row of CUSTOMER_VERIFY_EMAIL in the tenant
        assertThat(notifLogRows(customerId, "CUSTOMER_VERIFY_EMAIL")).isEqualTo(1);
        awaitNotifLogFailedAfterRetries(customerId, "CUSTOMER_VERIFY_EMAIL");
        String verifyToken = capturedToken(customerId, "CUSTOMER_VERIFY_EMAIL");

        HttpResponse<String> beforeVerify = http.customerLogin(tenantCode, email, CUSTOMER_PASSWORD);
        assertThat(beforeVerify.statusCode()).isEqualTo(403);
        assertThat(RealmHttp.errorCode(beforeVerify)).isEqualTo("CUSTOMER_NOT_VERIFIED");

        HttpResponse<String> verified = http.verify(tenantCode, verifyToken);
        assertThat(verified.statusCode()).as(verified.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(verified.body(), "$.data.statusCode")).isEqualTo("ACTIVE");

        HttpResponse<String> verifiedAgain = http.verify(tenantCode, verifyToken);
        assertThat(verifiedAgain.statusCode()).isEqualTo(409);
        assertThat(RealmHttp.errorCode(verifiedAgain)).isEqualTo("VERIFY_TOKEN_INVALID");

        HttpResponse<String> login = http.customerLogin(tenantCode, email, CUSTOMER_PASSWORD);
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
        String token = JsonPath.read(login.body(), "$.data.accessToken");
        assertThat(RealmHttp.claim(token, "realm")).isEqualTo("CUSTOMER");
        assertThat(((Number) RealmHttp.claim(token, TenantConstants.TENANT_ID_CLAIM)).longValue()).isEqualTo(tenantId);

        HttpResponse<String> me = http.get(token, null, "/api/v1/customers/me");
        assertThat(me.statusCode()).as(me.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(me.body(), "$.data.email")).isEqualTo(email);
        assertThat(((Number) JsonPath.read(me.body(), "$.data.id")).longValue()).isEqualTo(customerId);

        HttpResponse<String> patched = http.patch(token, null, "/api/v1/customers/me", "{\"fullNameAr\":\"جين\"}");
        assertThat(patched.statusCode()).as(patched.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(patched.body(), "$.data.fullNameAr")).isEqualTo("جين");
        assertThat((String) JsonPath.read(patched.body(), "$.data.fullNameEn")).isEqualTo("Jane Doe");
    }

    @Test
    void duplicateEmailInTheSameTenant_is409_whileAnotherTenantMayReuseIt() {
        String email = "dup." + RealmHttp.unique("x").toLowerCase() + "@shop.test";
        assertThat(http.register(tenantCode, email, CUSTOMER_PASSWORD, "First").statusCode()).isEqualTo(201);

        HttpResponse<String> duplicate = http.register(tenantCode, email, CUSTOMER_PASSWORD, "Second");
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(RealmHttp.errorCode(duplicate)).isEqualTo("CUSTOMER_EMAIL_TAKEN");

        String platformToken = http.staffToken(TenantConstants.PLATFORM_TENANT_CODE,
            RealmHttp.platformOperator(jdbcTemplate, passwordEncoder), RealmHttp.PASSWORD);
        String otherTenant = RealmHttp.unique("CO");
        http.provisionTenant(platformToken, otherTenant);
        assertThat(http.register(otherTenant, email, CUSTOMER_PASSWORD, "Elsewhere").statusCode()).isEqualTo(201);
    }

    @Test
    void staffToken_onCustomerEndpoint_is403RealmMismatch() {
        String staffToken = http.staffToken(tenantCode, "admin", RealmHttp.PASSWORD);
        assertThat(RealmHttp.claim(staffToken, "realm")).isEqualTo("STAFF");

        HttpResponse<String> me = http.get(staffToken, null, "/api/v1/customers/me");
        assertThat(me.statusCode()).isEqualTo(403);
        assertThat(RealmHttp.errorCode(me)).isEqualTo("REALM_MISMATCH");

        HttpResponse<String> patch = http.patch(staffToken, null, "/api/v1/customers/me", "{\"fullNameEn\":\"X\"}");
        assertThat(patch.statusCode()).isEqualTo(403);
    }

    @Test
    void customerToken_onStaffEndpoints_is403() {
        String token = verifiedCustomerToken("cust." + RealmHttp.unique("x").toLowerCase() + "@shop.test");

        HttpResponse<String> search = http.post(token, null, "/api/v1/sec/users/search", "{\"size\":10}");
        assertThat(search.statusCode()).isEqualTo(403);
        assertThat(RealmHttp.errorCode(search)).isEqualTo("REALM_MISMATCH");

        HttpResponse<String> getUser = http.get(token, null, "/api/v1/sec/users/1");
        assertThat(getUser.statusCode()).isEqualTo(403);

        // even an endpoint gated on authentication alone (the staff menu) refuses the customer realm
        HttpResponse<String> menu = http.get(token, null, "/api/v1/sec/menu");
        assertThat(menu.statusCode()).isEqualTo(403);
    }

    @Test
    void theSameEmail_mayHoldAStaffAndACustomerAccount() {
        String email = "both." + RealmHttp.unique("x").toLowerCase() + "@shop.test";
        String adminToken = http.staffToken(tenantCode, "admin", RealmHttp.PASSWORD);
        HttpResponse<String> staff = http.post(adminToken, null, "/api/v1/sec/users", "{\"username\":\"" + email
            + "\",\"email\":\"" + email + "\",\"fullNameAr\":\"موظف\",\"fullNameEn\":\"Staff\",\"password\":\""
            + RealmHttp.PASSWORD + "\"}");
        assertThat(staff.statusCode()).as(staff.body()).isEqualTo(201);

        HttpResponse<String> customer = http.register(tenantCode, email, CUSTOMER_PASSWORD, "Customer");
        assertThat(customer.statusCode()).as(customer.body()).isEqualTo(201);

        assertThat(jdbcTemplate.queryForList("select realm from sec_user where tenant_id = ? and email = ? order by realm",
            String.class, tenantId, email)).containsExactly("CUSTOMER", "STAFF");
        // the staff account still logs in on the staff realm, with its own password
        assertThat(RealmHttp.claim(http.staffToken(tenantCode, email, RealmHttp.PASSWORD), "realm")).isEqualTo("STAFF");
        // and the staff password does not open the customer account
        assertThat(http.customerLogin(tenantCode, email, RealmHttp.PASSWORD).statusCode()).isIn(401, 403);
    }

    @Test
    void failurePaths_ofThePublicEndpoints() {
        HttpResponse<String> noTenant = http.register(null, "x@shop.test", CUSTOMER_PASSWORD, "X");
        assertThat(noTenant.statusCode()).isEqualTo(400);
        assertThat(RealmHttp.errorCode(noTenant)).isEqualTo("TENANT_REQUIRED");

        HttpResponse<String> badToken = http.verify(tenantCode, "not-a-token");
        assertThat(badToken.statusCode()).isEqualTo(409);
        assertThat(RealmHttp.errorCode(badToken)).isEqualTo("VERIFY_TOKEN_INVALID");

        HttpResponse<String> invalidBody = http.register(tenantCode, "not-an-email", "short", "");
        assertThat(invalidBody.statusCode()).isEqualTo(400);

        HttpResponse<String> unknown = http.customerLogin(tenantCode, "nobody@shop.test", CUSTOMER_PASSWORD);
        assertThat(unknown.statusCode()).isEqualTo(401);

        HttpResponse<String> anonymousMe = http.get(null, tenantCode, "/api/v1/customers/me");
        assertThat(anonymousMe.statusCode()).isEqualTo(401);
    }

    @Test
    void customerPasswordReset_requestThenComplete_thenLoginWithTheNewPassword() {
        String email = "reset." + RealmHttp.unique("x").toLowerCase() + "@shop.test";
        verifiedCustomerToken(email);
        long customerId = jdbcTemplate.queryForObject(
            "select user_pk from sec_user where tenant_id = ? and realm = 'CUSTOMER' and email = ?", Long.class, tenantId, email);

        HttpResponse<String> requested = http.post(null, tenantCode, "/api/v1/public/customers/password-reset/request",
            "{\"email\":\"" + email + "\"}");
        assertThat(requested.statusCode()).isEqualTo(200);
        assertThat(notifLogRows(customerId, "CUSTOMER_PASSWORD_RESET")).isEqualTo(1);
        awaitNotifLogFailedAfterRetries(customerId, "CUSTOMER_PASSWORD_RESET");
        String resetToken = capturedToken(customerId, "CUSTOMER_PASSWORD_RESET");

        // the staff completion endpoint refuses a customer's token
        HttpResponse<String> onStaff = http.post(null, tenantCode, "/api/v1/sec/auth/password-reset/complete",
            "{\"token\":\"" + resetToken + "\",\"newPassword\":\"N3w-Cust0mer-Pass!\"}");
        assertThat(onStaff.statusCode()).isEqualTo(409);

        HttpResponse<String> completed = http.post(null, tenantCode, "/api/v1/public/customers/password-reset/complete",
            "{\"token\":\"" + resetToken + "\",\"newPassword\":\"N3w-Cust0mer-Pass!\"}");
        assertThat(completed.statusCode()).as(completed.body()).isEqualTo(200);

        assertThat(http.customerLogin(tenantCode, email, CUSTOMER_PASSWORD).statusCode()).isEqualTo(401);
        assertThat(http.customerLogin(tenantCode, email, "N3w-Cust0mer-Pass!").statusCode()).isEqualTo(200);

        // unknown e-mail: same generic 200, nothing dispatched
        HttpResponse<String> unknown = http.post(null, tenantCode, "/api/v1/public/customers/password-reset/request",
            "{\"email\":\"ghost@shop.test\"}");
        assertThat(unknown.statusCode()).isEqualTo(200);
    }

    @Test
    void customerLogin_isRateLimitedPerTenantRealmAndUsername() {
        String email = "brute." + RealmHttp.unique("x").toLowerCase() + "@shop.test";
        for (int attempt = 1; attempt <= 10; attempt++) {
            assertThat(http.customerLogin(tenantCode, email, "wrong-password").statusCode()).as("attempt " + attempt)
                .isEqualTo(401);
        }
        HttpResponse<String> limited = http.customerLogin(tenantCode, email, "wrong-password");
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(RealmHttp.errorCode(limited)).isEqualTo("CUSTOMER_LOGIN_RATE_LIMITED");
        // another username is not affected
        assertThat(http.customerLogin(tenantCode, "other." + email, "wrong-password").statusCode()).isEqualTo(401);
    }

    @Test
    void openApi_listsTheCustomerEndpoints_underTheCustomersAndSecGroups() {
        for (String group : new String[] {"customers", "sec"}) {
            HttpResponse<String> doc = http.get(null, null, "/v3/api-docs/" + group);
            assertThat(doc.statusCode()).as(group).isEqualTo(200);
            assertThat(doc.body()).as(group)
                .contains("/api/v1/public/customers/register", "/api/v1/public/customers/verify",
                    "/api/v1/public/customers/login", "/api/v1/public/customers/password-reset/request",
                    "/api/v1/public/customers/password-reset/complete", "/api/v1/customers/me");
        }
        assertThat(http.get(null, null, "/v3/api-docs/customers").body()).doesNotContain("/api/v1/sec/users");
    }

    // ------------------------------------------------------------------------------------------

    private String verifiedCustomerToken(String email) {
        HttpResponse<String> registered = http.register(tenantCode, email, CUSTOMER_PASSWORD, "Customer");
        assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);
        long id = ((Number) JsonPath.read(registered.body(), "$.data.id")).longValue();
        assertThat(http.verify(tenantCode, capturedToken(id, "CUSTOMER_VERIFY_EMAIL")).statusCode()).isEqualTo(200);
        HttpResponse<String> login = http.customerLogin(tenantCode, email, CUSTOMER_PASSWORD);
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
        return JsonPath.read(login.body(), "$.data.accessToken");
    }

    /** The raw token of the last dispatch of {@code templateCode} to {@code recipientId}. */
    private String capturedToken(long recipientId, String templateCode) {
        ArgumentCaptor<DispatchCommand> captor = ArgumentCaptor.forClass(DispatchCommand.class);
        verify(notificationDispatchApi, atLeastOnce()).dispatchIndependently(captor.capture());
        List<DispatchCommand> matching = captor.getAllValues().stream()
            .filter(c -> c.recipientId() == recipientId && templateCode.equals(c.templateCode()))
            .toList();
        assertThat(matching).as("dispatches of %s to %s", templateCode, recipientId).isNotEmpty();
        DispatchCommand command = matching.get(matching.size() - 1);
        assertThat(command.channelHint()).containsExactly("EMAIL");
        assertThat(command.variables().get("actionLink")).contains("token=");
        return command.variables().get("token");
    }

    /**
     * erp-core step 08 — the row's asynchronous outcome: the EMAIL provider exists (the test profile
     * configures a mail host) but nothing listens on localhost:2525, so every attempt fails and the
     * row ends FAILED with 5 attempts and its variables (which carry the token) cleared.
     */
    private void awaitNotifLogFailedAfterRetries(long recipientId, String templateCode) {
        String sql = "select l.notification_status_id from notif_log l join notif_template t on t.id = l.template_fk"
            + " where l.tenant_id = ? and l.recipient_id = ? and t.template_code = ?";
        await().atMost(Duration.ofSeconds(30)).until(() -> "FAILED".equals(
            jdbcTemplate.queryForObject(sql, String.class, tenantId, recipientId, templateCode)));
        java.util.Map<String, Object> row = jdbcTemplate.queryForMap("select l.attempts, l.variables_json,"
                + " l.channel_type_id from notif_log l join notif_template t on t.id = l.template_fk"
                + " where l.tenant_id = ? and l.recipient_id = ? and t.template_code = ?",
            tenantId, recipientId, templateCode);
        assertThat(((Number) row.get("attempts")).intValue()).isEqualTo(5);
        assertThat(row.get("variables_json")).isNull();
        assertThat(row.get("channel_type_id")).isEqualTo("EMAIL");
    }

    private int notifLogRows(long recipientId, String templateCode) {
        return jdbcTemplate.queryForObject("select count(*) from notif_log l join notif_template t on t.id = l.template_fk"
                + " where l.tenant_id = ? and l.recipient_id = ? and t.template_code = ?",
            Integer.class, tenantId, recipientId, templateCode);
    }
}
