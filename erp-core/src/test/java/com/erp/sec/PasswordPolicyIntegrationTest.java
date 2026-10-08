package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.util.TokenHasher;
import com.erp.testsupport.StaffApiClient;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * erp-core 1.3.0 (TM-D) over HTTP: the one STAFF password policy (REQ-SEC-082, RULE-SEC-056) on user
 * create, password-reset completion (which also satisfies a forced change) and a new tenant's first
 * administrator; nothing is written when the password is refused.
 */
class PasswordPolicyIntegrationTest extends AbstractStaffAccountIntegrationTest {

    @Test
    void createUser_refusesAWeakPassword_namingTheField_andCreatesNothing() {
        for (String weak : new String[] {"short1", "abcdefgh", "12345678"}) {
            String username = unique("weak-");
            HttpResponse<String> refused = api.post(adminToken, "/api/v1/sec/users", userBody(username, weak, false));
            assertThat(refused.statusCode()).as(weak).isEqualTo(400);
            assertThat(errorCode(refused)).isEqualTo("SEC-400-PASSWORD-POLICY");
            assertThat((String) JsonPath.read(refused.body(), "$.error.fieldErrors[0].field")).isEqualTo("password");
            assertThat((String) JsonPath.read(refused.body(), "$.error.message")).contains("8").contains("72").contains("72 bytes");
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_USER WHERE USERNAME = ?", Integer.class, username))
                .isZero();
        }
        assertThat(api.post(adminToken, "/api/v1/sec/users", userBody(unique("strong-"), "Passw0rd!Tc1", false)).statusCode())
            .isEqualTo(201);
    }

    @Test
    void resetCompletion_appliesThePolicy_keepsTheTokenOnRefusal_andClearsAForcedChange() {
        String username = unique("reset-");
        long id = createUser(username, null);
        assertThat(jdbcTemplate.queryForObject("SELECT PASSWORD_CHANGE_REQUIRED_FL FROM SEC_USER WHERE USER_PK = ?",
            Boolean.class, id)).isTrue();
        String raw = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO SEC_PWD_RESET_TOKEN (PWD_RESET_TOKEN_PK, TENANT_ID, USER_ID, TOKEN_HASH, REQUESTED_AT,"
                + " EXPIRES_AT, CREATED_BY, CREATED_AT) SELECT nextval('SEQ_SEC_PWD_RESET_TOKEN'), u.TENANT_ID, u.USER_PK, ?,"
                + " now(), now() + interval '1 hour', 'test', now() FROM SEC_USER u WHERE u.USER_PK = ?",
            TokenHasher.sha256Hex(raw), id);

        HttpResponse<String> weak = api.postAnonymous(tenant, "/api/v1/sec/auth/password-reset/complete", "{\"token\":\"" + raw
            + "\",\"newPassword\":\"onlyletters\"}");
        assertThat(weak.statusCode()).isEqualTo(400);
        assertThat(errorCode(weak)).isEqualTo("SEC-400-PASSWORD-POLICY");
        assertThat((String) JsonPath.read(weak.body(), "$.error.fieldErrors[0].field")).isEqualTo("newPassword");

        HttpResponse<String> done = api.postAnonymous(tenant, "/api/v1/sec/auth/password-reset/complete", "{\"token\":\"" + raw
            + "\",\"newPassword\":\"Reset-Passw0rd9\"}");
        assertThat(done.statusCode()).as(done.body()).isEqualTo(200);
        HttpResponse<String> login = api.login(tenant, username, "Reset-Passw0rd9");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(login.body(), "$.data.passwordChangeRequired")).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT PASSWORD_CHANGED_AT IS NOT NULL FROM SEC_USER WHERE USER_PK = ?",
            Boolean.class, id)).isTrue();
    }

    /** Review round 1: BCrypt hashes at most 72 bytes — every password path answers 400, never 500. */
    @Test
    void everyPasswordPath_refusesMoreThanSeventyTwoBytes_with400_andAcceptsExactlySeventyTwo() {
        String ascii73 = "Aa1" + "x".repeat(70);
        String arabic122 = "س".repeat(60) + "12";
        String exactly72 = "Aa1" + "x".repeat(69);
        assertThat(ascii73.length()).isEqualTo(73);
        assertThat(exactly72.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(72);

        for (String tooLong : new String[] {ascii73, arabic122}) {
            assertPolicy(api.post(adminToken, "/api/v1/sec/users", userBody(unique("long-"), tooLong, false)), "password");
        }
        String username = unique("bytes-");
        HttpResponse<String> created = api.post(adminToken, "/api/v1/sec/users", userBody(username, exactly72, false));
        assertThat(created.statusCode()).as("create with exactly 72 bytes").isEqualTo(201);
        long id = ((Number) JsonPath.read(created.body(), "$.data.userPk")).longValue();

        assertPolicy(api.put(adminToken, "/api/v1/sec/users/" + id + "/password", "{\"newPassword\":\"" + ascii73 + "\"}"),
            "newPassword");
        assertPolicy(api.put(adminToken, "/api/v1/sec/users/" + id + "/password", "{\"newPassword\":\"" + arabic122 + "\"}"),
            "newPassword");
        assertThat(api.put(adminToken, "/api/v1/sec/users/" + id + "/password",
            "{\"newPassword\":\"" + exactly72 + "\",\"requireChangeAtNextLogin\":false}").statusCode()).isEqualTo(200);

        String token = login(username, exactly72);
        assertPolicy(api.put(token, "/api/v1/sec/me/password",
            "{\"currentPassword\":\"" + exactly72 + "\",\"newPassword\":\"" + ascii73 + "\"}"), "newPassword");
        assertThat(api.put(token, "/api/v1/sec/me/password",
            "{\"currentPassword\":\"" + exactly72 + "\",\"newPassword\":\"" + "Bb2" + "y".repeat(69) + "\"}").statusCode())
            .isEqualTo(200);

        String raw = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO SEC_PWD_RESET_TOKEN (PWD_RESET_TOKEN_PK, TENANT_ID, USER_ID, TOKEN_HASH, REQUESTED_AT,"
                + " EXPIRES_AT, CREATED_BY, CREATED_AT) SELECT nextval('SEQ_SEC_PWD_RESET_TOKEN'), u.TENANT_ID, u.USER_PK, ?,"
                + " now(), now() + interval '1 hour', 'test', now() FROM SEC_USER u WHERE u.USER_PK = ?",
            TokenHasher.sha256Hex(raw), id);
        assertPolicy(api.postAnonymous(tenant, "/api/v1/sec/auth/password-reset/complete",
            "{\"token\":\"" + raw + "\",\"newPassword\":\"" + arabic122 + "\"}"), "newPassword");
        assertThat(api.postAnonymous(tenant, "/api/v1/sec/auth/password-reset/complete",
            "{\"token\":\"" + raw + "\",\"newPassword\":\"" + exactly72 + "\"}").statusCode()).isEqualTo(200);

        String platformToken = api.token("PLATFORM", StaffApiClient.platformOperator(jdbcTemplate, passwordEncoder));
        String code = StaffApiClient.unique("LONG");
        assertPolicy(api.post(platformToken, "/api/v1/platform/tenants", "{\"code\":\"" + code
            + "\",\"nameAr\":\"م\",\"nameEn\":\"Long\",\"adminUsername\":\"admin\",\"adminEmail\":\"a@" + code.toLowerCase()
            + ".test\",\"adminPassword\":\"" + ascii73 + "\",\"adminFullNameAr\":\"م\",\"adminFullNameEn\":\"A\"}"),
            "adminPassword");

        assertPolicy(api.postAnonymous(tenant, "/api/v1/public/customers/register", "{\"email\":\"" + unique("cust-")
            + "@tmd.test\",\"password\":\"" + arabic122 + "\",\"fullName\":\"Buyer\"}"), "password");
    }

    private static void assertPolicy(HttpResponse<String> response, String field) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(errorCode(response)).isEqualTo("SEC-400-PASSWORD-POLICY");
        assertThat((String) JsonPath.read(response.body(), "$.error.fieldErrors[0].field")).isEqualTo(field);
    }

    @Test
    void tenantCreate_refusesAWeakFirstAdministratorPassword_andProvisionsNothing() {
        String platformToken = api.token("PLATFORM", StaffApiClient.platformOperator(jdbcTemplate, passwordEncoder));
        String code = StaffApiClient.unique("WEAK");
        HttpResponse<String> refused = api.post(platformToken, "/api/v1/platform/tenants", "{\"code\":\"" + code
            + "\",\"nameAr\":\"م\",\"nameEn\":\"Weak\",\"adminUsername\":\"admin\",\"adminEmail\":\"a@" + code.toLowerCase()
            + ".test\",\"adminPassword\":\"password\",\"adminFullNameAr\":\"م\",\"adminFullNameEn\":\"A\"}");
        assertThat(refused.statusCode()).as(refused.body()).isEqualTo(400);
        assertThat(errorCode(refused)).isEqualTo("SEC-400-PASSWORD-POLICY");
        assertThat((String) JsonPath.read(refused.body(), "$.error.fieldErrors[0].field")).isEqualTo("adminPassword");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CORE_TENANT WHERE CODE = ?", Integer.class, code)).isZero();

        Boolean adminFlag = jdbcTemplate.queryForObject("SELECT u.PASSWORD_CHANGE_REQUIRED_FL FROM SEC_USER u JOIN CORE_TENANT t"
            + " ON t.ID = u.TENANT_ID WHERE t.CODE = ? AND u.USERNAME = ?", Boolean.class, tenant, StaffApiClient.TENANT_ADMIN);
        assertThat(adminFlag).as("a provisioned first administrator is not forced to change").isFalse();
    }
}
