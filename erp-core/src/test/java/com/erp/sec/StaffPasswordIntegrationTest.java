package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/**
 * erp-core 1.3.0 (TM-D) over HTTP: the administrator sets a staff password (REQ-SEC-083, RULE-SEC-057/058),
 * the forced-change gate (REQ-SEC-084, RULE-SEC-059, ADR-SEC-063) and the user's own change
 * (REQ-SEC-085, RULE-SEC-060): sessions, flags, audit rows and error codes.
 */
class StaffPasswordIntegrationTest extends AbstractStaffAccountIntegrationTest {

    private static final String NEW_PASSWORD = "Admin-Set-Passw0rd1";
    private static final String OWN_PASSWORD = "My-Own-Passw0rd2";

    @Test
    void adminSet_endsTheUsersSessions_forcesAChange_andTheGateLeavesOnlyThreeCalls() {
        String username = unique("pw-set-");
        long id = createUser(username, false);
        String oldToken = login(username, PASSWORD);

        HttpResponse<String> set = api.put(adminToken, "/api/v1/sec/users/" + id + "/password",
            "{\"newPassword\":\"" + NEW_PASSWORD + "\"}");
        assertThat(set.statusCode()).as(set.body()).isEqualTo(200);
        assertThat(data(set, "userPk")).isEqualTo((int) id);
        assertThat(data(set, "passwordChangeRequired")).isEqualTo(true);
        assertThat(data(set, "sessionsTerminated")).isEqualTo(1);
        assertThat(data(set, "passwordChangedAt")).isNotNull();
        assertThat(set.body()).doesNotContain(NEW_PASSWORD).doesNotContain("$2a$");

        assertThat(api.get(oldToken, "/api/v1/sec/me").statusCode()).as("old session ended").isEqualTo(401);
        assertThat(api.login(tenant, username, PASSWORD).statusCode()).isEqualTo(401);
        HttpResponse<String> loginNew = api.login(tenant, username, NEW_PASSWORD);
        assertThat(loginNew.statusCode()).isEqualTo(200);
        assertThat(data(loginNew, "passwordChangeRequired")).isEqualTo(true);
        String flagged = (String) data(loginNew, "accessToken");

        HttpResponse<String> menu = api.get(flagged, "/api/v1/sec/menu");
        assertThat(menu.statusCode()).isEqualTo(403);
        assertThat(errorCode(menu)).isEqualTo("SEC-403-PASSWORD-CHANGE-REQUIRED");
        assertThat(errorCode(api.patch(flagged, "/api/v1/sec/me", "{\"phone\":\"+966501234567\"}")))
            .isEqualTo("SEC-403-PASSWORD-CHANGE-REQUIRED");
        assertThat(errorCode(api.post(flagged, "/api/v1/sec/users/search", "{}"))).isEqualTo("SEC-403-PASSWORD-CHANGE-REQUIRED");
        HttpResponse<String> me = api.get(flagged, "/api/v1/sec/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(data(me, "passwordChangeRequired")).isEqualTo(true);

        HttpResponse<String> change = api.put(flagged, "/api/v1/sec/me/password",
            "{\"currentPassword\":\"" + NEW_PASSWORD + "\",\"newPassword\":\"" + OWN_PASSWORD + "\"}");
        assertThat(change.statusCode()).as(change.body()).isEqualTo(200);
        assertThat(data(change, "passwordChangeRequired")).isEqualTo(false);
        assertThat(api.get(flagged, "/api/v1/sec/menu").statusCode()).as("same token passes now").isEqualTo(200);

        assertThat(auditRows("PASSWORD_SET_BY_ADMIN", id)).isEqualTo(1);
        assertThat(auditRows("PASSWORD_CHANGED", id)).isEqualTo(1);
        Integer terminatedRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_AUDIT_LOG l JOIN SEC_ACTIVE_SESSION s"
            + " ON l.TARGET_REF = CAST(s.ACTIVE_SESSION_PK AS VARCHAR) WHERE l.EVENT_TYPE_CODE = 'SESSION_TERMINATED'"
            + " AND s.USER_ID = ?", Integer.class, id);
        assertThat(terminatedRows).isEqualTo(1);
        String secrets = jdbcTemplate.queryForObject("SELECT STRING_AGG(COALESCE(SUMMARY_EN, '') || COALESCE(CHANGES::text, ''), '|')"
            + " FROM CORE_AUDIT_EVENT WHERE ENTITY_TYPE = 'SEC_USER' AND ENTITY_ID = ?", String.class, String.valueOf(id));
        assertThat(secrets).doesNotContain(NEW_PASSWORD).doesNotContain(OWN_PASSWORD).doesNotContain("$2a$");
    }

    @Test
    void logout_staysAllowedWhileTheChangeIsPending() {
        String username = unique("pw-out-");
        createUser(username, null);
        HttpResponse<String> login = api.login(tenant, username, PASSWORD);
        assertThat(data(login, "passwordChangeRequired")).as("admin-created: default TRUE").isEqualTo(true);
        String token = (String) data(login, "accessToken");

        assertThat(api.post(token, "/api/v1/sec/auth/logout", "{}").statusCode()).isEqualTo(200);
        assertThat(api.get(token, "/api/v1/sec/me").statusCode()).isEqualTo(401);
    }

    @Test
    void adminSet_refusals_self422_unknown404_weak400_andRequireChangeFalseIsHonoured() {
        Long adminId = jdbcTemplate.queryForObject("SELECT u.USER_PK FROM SEC_USER u JOIN CORE_TENANT t ON t.ID = u.TENANT_ID"
            + " WHERE t.CODE = ? AND u.USERNAME = 'tenant-admin' AND u.REALM = 'STAFF'", Long.class, tenant);
        HttpResponse<String> self = api.put(adminToken, "/api/v1/sec/users/" + adminId + "/password",
            "{\"newPassword\":\"" + NEW_PASSWORD + "\"}");
        assertThat(self.statusCode()).isEqualTo(422);
        assertThat(errorCode(self)).isEqualTo("SEC-422-PASSWORD-SELF");

        HttpResponse<String> unknown = api.put(adminToken, "/api/v1/sec/users/987654321/password",
            "{\"newPassword\":\"" + NEW_PASSWORD + "\"}");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(errorCode(unknown)).isEqualTo("SEC-404-USER");

        String username = unique("pw-opt-");
        long id = createUser(username, false);
        HttpResponse<String> weak = api.put(adminToken, "/api/v1/sec/users/" + id + "/password", "{\"newPassword\":\"abcdefgh\"}");
        assertThat(weak.statusCode()).isEqualTo(400);
        assertThat(errorCode(weak)).isEqualTo("SEC-400-PASSWORD-POLICY");
        assertThat((String) com.jayway.jsonpath.JsonPath.read(weak.body(), "$.error.fieldErrors[0].field")).isEqualTo("newPassword");

        HttpResponse<String> optOut = api.put(adminToken, "/api/v1/sec/users/" + id + "/password",
            "{\"newPassword\":\"" + NEW_PASSWORD + "\",\"requireChangeAtNextLogin\":false}");
        assertThat(optOut.statusCode()).isEqualTo(200);
        assertThat(data(optOut, "passwordChangeRequired")).isEqualTo(false);
        HttpResponse<String> login = api.login(tenant, username, NEW_PASSWORD);
        assertThat(data(login, "passwordChangeRequired")).isEqualTo(false);
        assertThat(api.get((String) data(login, "accessToken"), "/api/v1/sec/menu").statusCode()).isEqualTo(200);
    }

    @Test
    void adminSet_needsUsersUpdate() {
        String username = unique("pw-plain-");
        createUser(username, false);
        String plainToken = login(username, PASSWORD);
        long target = createUser(unique("pw-tgt-"), false);

        HttpResponse<String> denied = api.put(plainToken, "/api/v1/sec/users/" + target + "/password",
            "{\"newPassword\":\"" + NEW_PASSWORD + "\"}");
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(errorCode(denied)).isEqualTo("SEC-403-FORBIDDEN");
    }

    @Test
    void selfChange_needsTheCurrentPassword_meetsThePolicy_andEndsOnlyTheOtherSessions() {
        String username = unique("pw-own-");
        long id = createUser(username, false);
        String first = login(username, PASSWORD);
        String second = login(username, PASSWORD);

        HttpResponse<String> wrong = api.put(first, "/api/v1/sec/me/password",
            "{\"currentPassword\":\"Not-The-Passw0rd\",\"newPassword\":\"" + OWN_PASSWORD + "\"}");
        assertThat(wrong.statusCode()).isEqualTo(403);
        assertThat(errorCode(wrong)).isEqualTo("SEC-403-PASSWORD-CURRENT-INVALID");

        HttpResponse<String> weak = api.put(first, "/api/v1/sec/me/password",
            "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"12345678\"}");
        assertThat(weak.statusCode()).isEqualTo(400);
        assertThat(errorCode(weak)).isEqualTo("SEC-400-PASSWORD-POLICY");

        HttpResponse<String> ok = api.put(first, "/api/v1/sec/me/password",
            "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + OWN_PASSWORD + "\"}");
        assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);
        assertThat(data(ok, "sessionsTerminated")).isEqualTo(1);
        assertThat(api.get(first, "/api/v1/sec/me").statusCode()).as("the caller's session stays").isEqualTo(200);
        assertThat(api.get(second, "/api/v1/sec/me").statusCode()).as("the other session ended").isEqualTo(401);
        assertThat(api.login(tenant, username, OWN_PASSWORD).statusCode()).isEqualTo(200);
        assertThat(api.login(tenant, username, PASSWORD).statusCode()).isEqualTo(401);
        assertThat(auditRows("PASSWORD_CHANGED", id)).isEqualTo(1);
    }

    @Test
    void createUser_defaultsToAForcedChange_andRecordsWhenThePasswordWasSet() {
        HttpResponse<String> created = api.post(adminToken, "/api/v1/sec/users", userBody(unique("pw-new-"), PASSWORD, null));
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(data(created, "passwordChangeRequired")).isEqualTo(true);
        assertThat(data(created, "passwordChangedAt")).isNotNull();
        assertThat(created.body()).doesNotContain("passwordHash");
    }
}
