package com.erp.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 10 over HTTP: the {@code @Audited} SEC_USER rows (CREATE without the password hash,
 * UPDATE with exactly the changed fields), SEC's LOGIN/LOGOUT entries, and the staff query API —
 * permission, tenant isolation, customer tokens refused.
 */
class AuditLogApiIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private AuditHttp http;
    private String platformToken;

    @BeforeEach
    void setUp() {
        http = new AuditHttp(port);
        platformToken = http.token(AuditHttp.PLATFORM, AuditHttp.platformOperator(jdbc, passwordEncoder));
    }

    @AfterEach
    void noSensitiveFieldInAnyChanges() {
        AuditRows.assertNoSensitiveFieldAnywhere(jdbc);
    }

    @Test
    void creatingAUser_writesOneCreateRow_withoutThePasswordHash() {
        String username = AuditHttp.unique("aud-c-").toLowerCase();
        long userId = http.createUser(platformToken, username);

        List<Map<String, Object>> creates = AuditRows.of(jdbc, "SEC_USER", userId, "CREATE");
        assertThat(creates).hasSize(1);
        Map<String, Object> row = creates.get(0);
        assertThat(row).containsEntry("tenant_id", 1L).containsEntry("actor_realm", "STAFF")
            .containsEntry("user_agent", AuditHttp.USER_AGENT);
        assertThat(row.get("actor")).isEqualTo(JsonPath.read(platformTokenPayload(), "$.sub"));
        assertThat(row.get("ip")).isNotNull();
        assertThat(AuditRows.changedFields(row))
            .contains("username", "email", "fullNameAr", "fullNameEn", "statusCode", "realm", "isActiveFl")
            .doesNotContain("passwordHash", "lastLoginAt", "createdBy", "createdAt", "updatedBy", "updatedAt",
                "version", "tenantId");
        assertThat((String) row.get("changes")).doesNotContain(AuditHttp.PASSWORD).doesNotContain("$2a$");
        assertThat(JsonPath.<List<String>>read((String) row.get("changes"), "$[?(@.field == 'username')].new"))
            .containsExactly(username);
        // no UPDATE row is written by the create itself
        assertThat(AuditRows.of(jdbc, "SEC_USER", userId, "UPDATE")).isEmpty();
    }

    @Test
    void updatingTwoFields_writesOneUpdateRow_withExactlyTwoChanges() {
        String username = AuditHttp.unique("aud-u-").toLowerCase();
        long userId = http.createUser(platformToken, username);

        HttpResponse<String> updated = http.put(platformToken, "/api/v1/sec/users/" + userId,
            "{\"email\":\"" + username + "@users.test\",\"fullNameAr\":\"اسم جديد\",\"fullNameEn\":\"New Name\"}");
        AuditHttp.expect(updated, 200, "update user");

        List<Map<String, Object>> updates = AuditRows.of(jdbc, "SEC_USER", userId, "UPDATE");
        assertThat(updates).hasSize(1);
        String changes = (String) updates.get(0).get("changes");
        assertThat(AuditRows.changedFields(updates.get(0))).containsExactlyInAnyOrder("fullNameAr", "fullNameEn");
        assertThat(JsonPath.<List<String>>read(changes, "$[?(@.field == 'fullNameEn')].old")).containsExactly("User");
        assertThat(JsonPath.<List<String>>read(changes, "$[?(@.field == 'fullNameEn')].new")).containsExactly("New Name");
    }

    @Test
    void loginAndLogout_areRecordedForTheAccount() {
        String username = AuditHttp.unique("aud-l-").toLowerCase();
        long userId = http.createUser(platformToken, username);
        String token = http.token(AuditHttp.PLATFORM, username);
        AuditHttp.expect(http.post(token, "/api/v1/sec/auth/logout", ""), 200, "logout");

        List<Map<String, Object>> logins = AuditRows.of(jdbc, "SEC_USER", userId, "LOGIN");
        assertThat(logins).hasSize(1);
        assertThat(logins.get(0)).containsEntry("actor", username).containsEntry("actor_realm", "STAFF")
            .containsEntry("actor_user_id", userId).containsEntry("tenant_id", 1L)
            .containsEntry("user_agent", AuditHttp.USER_AGENT);
        assertThat(AuditRows.of(jdbc, "SEC_USER", userId, "LOGOUT")).hasSize(1)
            .first().satisfies(row -> assertThat(row).containsEntry("actor", username));
        // lastLoginAt is ignored on SEC_USER: the login wrote a LOGIN row, not an UPDATE row
        assertThat(AuditRows.of(jdbc, "SEC_USER", userId, "UPDATE")).isEmpty();
    }

    @Test
    void query_returnsTheEntityHistory_newestFirst_forAHolderOfAuditEventRead() {
        String username = AuditHttp.unique("aud-q-").toLowerCase();
        long userId = http.createUser(platformToken, username);
        AuditHttp.expect(http.put(platformToken, "/api/v1/sec/users/" + userId,
            "{\"email\":\"" + username + "@users.test\",\"fullNameAr\":\"ق\",\"fullNameEn\":\"Q\"}"), 200, "update");

        HttpResponse<String> response = http.events(platformToken,
            Map.of("entityType", "SEC_USER", "entityId", String.valueOf(userId)));
        AuditHttp.expect(response, 200, "query");
        assertThat(JsonPath.<List<String>>read(response.body(), "$.data.content[*].action"))
            .containsExactly("UPDATE", "CREATE");
        assertThat(JsonPath.<String>read(response.body(), "$.data.content[1].changes[0].field")).isNotBlank();

        HttpResponse<String> byAction = http.events(platformToken, Map.of("entityType", "SEC_USER",
            "entityId", String.valueOf(userId), "action", "CREATE", "from", "2000-01-01T00:00:00Z"));
        assertThat(JsonPath.<List<String>>read(byAction.body(), "$.data.content[*].action")).containsExactly("CREATE");

        HttpResponse<String> future = http.events(platformToken, Map.of("entityType", "SEC_USER",
            "entityId", String.valueOf(userId), "from", "2999-01-01T00:00:00Z"));
        assertThat(JsonPath.<List<Object>>read(future.body(), "$.data.content")).isEmpty();
    }

    @Test
    void query_withAMalformedDate_is400() {
        HttpResponse<String> response = http.events(platformToken, Map.of("from", "yesterday"));
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void query_isTenantIsolated() {
        String code = AuditHttp.unique("AUD");
        long tenantB = http.provisionTenant(platformToken, code);
        String tokenB = http.token(code, AuditHttp.TENANT_ADMIN);
        long userOfB = http.createUser(tokenB, AuditHttp.unique("aud-b-").toLowerCase());
        long userOfPlatform = http.createUser(platformToken, AuditHttp.unique("aud-p-").toLowerCase());

        // B's row is stored under B
        assertThat(AuditRows.of(jdbc, "SEC_USER", userOfB, "CREATE")).singleElement()
            .satisfies(row -> assertThat(row).containsEntry("tenant_id", tenantB));

        HttpResponse<String> asB = http.events(tokenB, Map.of("entityType", "SEC_USER"));
        AuditHttp.expect(asB, 200, "query as B");
        List<String> idsSeenByB = JsonPath.read(asB.body(), "$.data.content[*].entityId");
        assertThat(idsSeenByB).contains(String.valueOf(userOfB)).doesNotContain(String.valueOf(userOfPlatform));

        HttpResponse<String> asPlatform = http.events(platformToken,
            Map.of("entityType", "SEC_USER", "entityId", String.valueOf(userOfB)));
        AuditHttp.expect(asPlatform, 200, "query as PLATFORM");
        assertThat(JsonPath.<List<Object>>read(asPlatform.body(), "$.data.content")).isEmpty();
    }

    @Test
    void query_customerToken_is403() {
        String customerToken = http.customerToken(jdbc, AuditHttp.PLATFORM);
        HttpResponse<String> response = http.events(customerToken, Map.of());
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(AuditHttp.errorCode(response)).isEqualTo("REALM_MISMATCH");

        // the customer's own login is in the generic log, in the CUSTOMER realm
        String customer = JsonPath.read(payloadOf(customerToken), "$.sub");
        assertThat(jdbc.queryForList("SELECT actor_realm FROM core_audit_event WHERE action = 'LOGIN' AND actor = ?",
            String.class, customer)).containsExactly("CUSTOMER");
    }

    @Test
    void query_withoutAuditEventRead_is403_andWithoutToken_is401() {
        String username = AuditHttp.unique("aud-n-").toLowerCase();
        http.createUser(platformToken, username);   // no role → no AUDIT:EVENT:READ
        String token = http.token(AuditHttp.PLATFORM, username);

        assertThat(http.events(token, Map.of()).statusCode()).isEqualTo(403);
        assertThat(http.events(null, Map.of()).statusCode()).isEqualTo(401);
    }

    private String platformTokenPayload() {
        return payloadOf(platformToken);
    }

    /** The JWT payload (decoded without verification — test only). */
    private static String payloadOf(String token) {
        return new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
            java.nio.charset.StandardCharsets.UTF_8);
    }
}
