package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * erp-core 1.3.0 (TM-D) over HTTP: the staff profile {@code GET/PATCH /api/v1/sec/me} (REQ-SEC-086,
 * ADR-SEC-064: no roles) and the profile fields on user create / update / response (REQ-SEC-088,
 * RULE-SEC-062), including "absent keeps, empty clears".
 */
class StaffProfileIntegrationTest extends AbstractStaffAccountIntegrationTest {

    @Test
    void me_carriesTheAccountProfileAndTenant_butNoRolesOrPermissions() {
        String username = unique("me-");
        long id = createUser(username, false);
        String token = login(username, PASSWORD);

        HttpResponse<String> me = api.get(token, "/api/v1/sec/me");
        assertThat(me.statusCode()).as(me.body()).isEqualTo(200);
        Map<String, Object> body = JsonPath.read(me.body(), "$.data");
        assertThat(body).containsEntry("userPk", (int) id).containsEntry("username", username)
            .containsEntry("passwordChangeRequired", false).containsKeys("email", "fullNameAr", "fullNameEn", "phone",
                "jobTitleAr", "jobTitleEn", "preferredLocale", "photoUrl", "lastLoginAt", "tenant")
            .doesNotContainKeys("roles", "permissions", "authorities", "passwordHash", "photoFileId");
        assertThat(data(me, "tenant.code")).isEqualTo(tenant);
        assertThat(data(me, "tenant.nameEn")).isEqualTo("Tenant " + tenant);
        assertThat(data(me, "lastLoginAt")).isNotNull();
    }

    @Test
    void patchMe_changesOnlyTheSuppliedFields_emptyClears_andRejectsAnUnknownLocaleOrABlankName() {
        String username = unique("patch-");
        createUser(username, false);
        String token = login(username, PASSWORD);

        HttpResponse<String> patched = api.patch(token, "/api/v1/sec/me",
            "{\"phone\":\"+966 50 123 4567\",\"preferredLocale\":\"ar\",\"jobTitleEn\":\"Accountant\",\"jobTitleAr\":\"محاسب\"}");
        assertThat(patched.statusCode()).as(patched.body()).isEqualTo(200);
        assertThat(data(patched, "phone")).isEqualTo("+966 50 123 4567");
        assertThat(data(patched, "preferredLocale")).isEqualTo("ar");
        assertThat(data(patched, "fullNameEn")).isEqualTo("User " + username);

        HttpResponse<String> french = api.patch(token, "/api/v1/sec/me", "{\"preferredLocale\":\"fr\"}");
        assertThat(french.statusCode()).isEqualTo(400);
        assertThat(errorCode(french)).isEqualTo("VALIDATION_ERROR");
        assertThat((String) JsonPath.read(french.body(), "$.error.fieldErrors[0].field")).isEqualTo("preferredLocale");
        assertThat(api.patch(token, "/api/v1/sec/me", "{\"fullNameEn\":\"   \"}").statusCode()).isEqualTo(400);
        assertThat(api.patch(token, "/api/v1/sec/me", "{\"phone\":\"call me\"}").statusCode()).isEqualTo(400);

        HttpResponse<String> cleared = api.patch(token, "/api/v1/sec/me", "{\"phone\":\"\",\"fullNameEn\":\"Renamed\"}");
        assertThat(cleared.statusCode()).isEqualTo(200);
        assertThat(data(cleared, "phone")).isNull();
        assertThat(data(cleared, "preferredLocale")).as("absent keeps").isEqualTo("ar");
        assertThat(data(cleared, "jobTitleEn")).isEqualTo("Accountant");
        assertThat(data(cleared, "fullNameEn")).isEqualTo("Renamed");
    }

    @Test
    void userCreateAndUpdate_carryTheProfileFields_absentKeeps_andAnUnknownLocaleIs400() {
        String username = unique("prof-");
        HttpResponse<String> created = api.post(adminToken, "/api/v1/sec/users", "{\"username\":\"" + username
            + "\",\"email\":\"" + username + "@tmd.test\",\"fullNameAr\":\"م\",\"fullNameEn\":\"P\",\"password\":\"" + PASSWORD
            + "\",\"phone\":\"+966501234567\",\"jobTitleEn\":\"Clerk\",\"preferredLocale\":\"en\"}");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(data(created, "phone")).isEqualTo("+966501234567");
        assertThat(data(created, "jobTitleEn")).isEqualTo("Clerk");
        assertThat(data(created, "preferredLocale")).isEqualTo("en");
        assertThat(data(created, "photoUrl")).isNull();
        long id = ((Number) data(created, "userPk")).longValue();

        HttpResponse<String> keep = api.put(adminToken, "/api/v1/sec/users/" + id,
            "{\"email\":\"" + username + "@tmd.test\",\"fullNameAr\":\"م\",\"fullNameEn\":\"P2\"}");
        assertThat(keep.statusCode()).isEqualTo(200);
        assertThat(data(keep, "phone")).as("an older client never wipes them").isEqualTo("+966501234567");
        assertThat(data(keep, "preferredLocale")).isEqualTo("en");

        HttpResponse<String> change = api.put(adminToken, "/api/v1/sec/users/" + id, "{\"email\":\"" + username
            + "@tmd.test\",\"fullNameAr\":\"م\",\"fullNameEn\":\"P2\",\"preferredLocale\":\"ar\",\"jobTitleEn\":\"\"}");
        assertThat(change.statusCode()).isEqualTo(200);
        assertThat(data(change, "preferredLocale")).isEqualTo("ar");
        assertThat(data(change, "jobTitleEn")).isNull();

        HttpResponse<String> french = api.put(adminToken, "/api/v1/sec/users/" + id, "{\"email\":\"" + username
            + "@tmd.test\",\"fullNameAr\":\"م\",\"fullNameEn\":\"P2\",\"preferredLocale\":\"fr\"}");
        assertThat(french.statusCode()).isEqualTo(400);
        assertThat(errorCode(french)).isEqualTo("VALIDATION_ERROR");

        HttpResponse<String> read = api.get(adminToken, "/api/v1/sec/users/" + id);
        assertThat(data(read, "preferredLocale")).isEqualTo("ar");
        assertThat(data(read, "passwordChangeRequired")).isEqualTo(true);
        String stored = jdbcTemplate.queryForObject("SELECT PREFERRED_LOCALE FROM SEC_USER WHERE USER_PK = ?", String.class, id);
        assertThat(stored).isEqualTo("ar");
    }

    /**
     * Review round 1: a multipart request without the {@code file} part, a non-multipart request, and an
     * upload above Spring's default 1 MB multipart ceiling (this context keeps it) answer 400, not 500.
     */
    @Test
    void malformedOrOversizePhotoRequests_answer400ValidationError() {
        String username = unique("mp-");
        createUser(username, false);
        String token = login(username, PASSWORD);

        HttpResponse<String> missing = api.putFile(token, "/api/v1/sec/me/photo", "other", "x.png", new byte[] {1, 2, 3});
        assertThat(missing.statusCode()).as(missing.body()).isEqualTo(400);
        assertThat(errorCode(missing)).isEqualTo("VALIDATION_ERROR");
        assertThat((String) JsonPath.read(missing.body(), "$.error.fieldErrors[0].field")).isEqualTo("file");

        HttpResponse<String> notMultipart = api.put(token, "/api/v1/sec/me/photo", "{}");
        assertThat(notMultipart.statusCode()).as(notMultipart.body()).isEqualTo(400);
        assertThat(errorCode(notMultipart)).isEqualTo("VALIDATION_ERROR");

        HttpResponse<String> oversize = api.putFile(token, "/api/v1/sec/me/photo", "big.png", new byte[2 * 1024 * 1024]);
        assertThat(oversize.statusCode()).as(oversize.body()).isEqualTo(400);
        assertThat(errorCode(oversize)).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void theDatabaseRefusesAnUnknownLocale_too() {
        String username = unique("chk-");
        long id = createUser(username, false);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE SEC_USER SET PREFERRED_LOCALE = 'fr' WHERE USER_PK = ?", id))
            .hasMessageContaining("CHK_SEC_USER_LOCALE".toLowerCase());
    }
}
