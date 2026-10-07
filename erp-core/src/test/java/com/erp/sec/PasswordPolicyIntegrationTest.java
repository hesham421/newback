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
            assertThat((String) JsonPath.read(refused.body(), "$.error.message")).contains("8").contains("200");
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
