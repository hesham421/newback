package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * erp-core 1.3.0 (TM-D) over HTTP: profile photos (REQ-SEC-087, RULE-SEC-061) through FILE's image store
 * (XM-FILE-002, RULE-FILE-010, ADR-FILE-008): public URL served inline without a token, replacement and
 * removal withdraw the old URL, rejected types and sizes, another user's photo, audit rows. The multipart
 * limits mirror the reference application's (above the photo limit), so the size check is the photo rule's,
 * not Spring's default 1 MB multipart ceiling.
 */
@TestPropertySource(properties = {
    "spring.servlet.multipart.max-file-size=15MB",
    "spring.servlet.multipart.max-request-size=25MB"})
class UserPhotoIntegrationTest extends AbstractStaffAccountIntegrationTest {

    @Test
    void myPhoto_isPublicInline_replacementWithdrawsTheOldUrl_removalClearsIt() {
        String username = unique("photo-");
        long id = createUser(username, false);
        String token = login(username, PASSWORD);
        byte[] first = png("first-" + username);

        HttpResponse<String> set = api.putFile(token, "/api/v1/sec/me/photo", "me.png", first);
        assertThat(set.statusCode()).as(set.body()).isEqualTo(200);
        String firstUrl = (String) data(set, "photoUrl");
        assertThat(firstUrl).startsWith("/api/v1/public/files/" + tenant + "/");

        HttpResponse<byte[]> served = api.anonymousGet(firstUrl);
        assertThat(served.statusCode()).isEqualTo(200);
        assertThat(served.body()).isEqualTo(first);
        assertThat(served.headers().firstValue("Content-Type")).hasValue("image/png");
        assertThat(served.headers().firstValue("Content-Disposition")).hasValueSatisfying(v -> assertThat(v).startsWith("inline"));
        assertThat(data(api.get(token, "/api/v1/sec/me"), "photoUrl")).isEqualTo(firstUrl);

        HttpResponse<String> replaced = api.putFile(token, "/api/v1/sec/me/photo", "me2.png", png("second-" + username));
        String secondUrl = (String) data(replaced, "photoUrl");
        assertThat(secondUrl).isNotEqualTo(firstUrl);
        assertThat(api.anonymousGet(firstUrl).statusCode()).as("previous photo discarded").isEqualTo(404);
        assertThat(api.anonymousGet(secondUrl).statusCode()).isEqualTo(200);

        Map<String, Object> document = jdbcTemplate.queryForMap("SELECT d.OWNER_TYPE, d.OWNER_ID, d.MODULE_CODE,"
            + " d.FILE_CATEGORY_FK, d.VISIBILITY, d.FILE_STATUS_ID FROM FILE_DOCUMENT d JOIN SEC_USER u ON u.PHOTO_FILE_ID = d.ID"
            + " WHERE u.USER_PK = ?", id);
        assertThat(document).containsEntry("owner_type", "SEC_USER").containsEntry("owner_id", id)
            .containsEntry("module_code", "SEC").containsEntry("visibility", "PUBLIC").containsEntry("file_status_id", "ACTIVE");
        assertThat(document.get("file_category_fk")).isNull();

        HttpResponse<String> removed = api.delete(token, "/api/v1/sec/me/photo");
        assertThat(removed.statusCode()).isEqualTo(204);
        assertThat(data(api.get(token, "/api/v1/sec/me"), "photoUrl")).isNull();
        assertThat(api.anonymousGet(secondUrl).statusCode()).isEqualTo(404);
        assertThat(api.delete(token, "/api/v1/sec/me/photo").statusCode()).as("no photo is no error").isEqualTo(204);
        List<String> statuses = jdbcTemplate.queryForList("SELECT FILE_STATUS_ID || '/' || VISIBILITY FROM FILE_DOCUMENT"
            + " WHERE OWNER_TYPE = 'SEC_USER' AND OWNER_ID = ?", String.class, id);
        assertThat(statuses).containsExactly("DELETED/PRIVATE", "DELETED/PRIVATE");
        assertThat(auditRows("PROFILE_PHOTO_CHANGED", id)).isEqualTo(3);
    }

    @Test
    void rejectedPhotos_answer400_andKeepThePreviousOne() {
        String username = unique("bad-");
        createUser(username, false);
        String token = login(username, PASSWORD);
        String url = (String) data(api.putFile(token, "/api/v1/sec/me/photo", "ok.png", png("keep-" + username)), "photoUrl");

        byte[] exe = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0, (byte) 0xFF, (byte) 0xFF};
        assertPhotoInvalid(api.putFile(token, "/api/v1/sec/me/photo", "tool.exe", exe));
        assertPhotoInvalid(api.putFile(token, "/api/v1/sec/me/photo", "logo.svg",
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle r=\"1\"/></svg>".getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream big = new ByteArrayOutputStream();
        big.writeBytes(png("big"));
        big.writeBytes(new byte[1_048_577]);
        assertPhotoInvalid(api.putFile(token, "/api/v1/sec/me/photo", "big.png", big.toByteArray()));
        assertPhotoInvalid(api.putFile(token, "/api/v1/sec/me/photo", "empty.png", new byte[0]));

        assertThat(data(api.get(token, "/api/v1/sec/me"), "photoUrl")).isEqualTo(url);
        assertThat(api.anonymousGet(url).statusCode()).isEqualTo(200);
    }

    @Test
    void anAdministrator_setsAndRemovesAnotherUsersPhoto_andListsShowIt() {
        String username = unique("other-");
        long id = createUser(username, false);

        HttpResponse<String> set = api.putFile(adminToken, "/api/v1/sec/users/" + id + "/photo", "u.png", png("adm-" + username));
        assertThat(set.statusCode()).as(set.body()).isEqualTo(200);
        String url = (String) data(set, "photoUrl");
        assertThat(data(api.get(adminToken, "/api/v1/sec/users/" + id), "photoUrl")).isEqualTo(url);
        HttpResponse<String> search = api.post(adminToken, "/api/v1/sec/users/search",
            "{\"filters\":[{\"field\":\"username\",\"operator\":\"EQUALS\",\"value\":\"" + username + "\"}]}");
        assertThat(search.statusCode()).as(search.body()).isEqualTo(200);
        List<String> urls = JsonPath.read(search.body(), "$.data.content[*].photoUrl");
        assertThat(urls).containsExactly(url);

        assertThat(api.delete(adminToken, "/api/v1/sec/users/" + id + "/photo").statusCode()).isEqualTo(204);
        assertThat(data(api.get(adminToken, "/api/v1/sec/users/" + id), "photoUrl")).isNull();
        HttpResponse<String> unknown = api.putFile(adminToken, "/api/v1/sec/users/987654321/photo", "u.png", png("x"));
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(errorCode(unknown)).isEqualTo("SEC-404-USER");

        String plain = unique("plain-");
        createUser(plain, false);
        HttpResponse<String> denied = api.putFile(login(plain, PASSWORD), "/api/v1/sec/users/" + id + "/photo", "u.png", png("y"));
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(errorCode(denied)).isEqualTo("SEC-403-FORBIDDEN");
    }

    private static void assertPhotoInvalid(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(errorCode(response)).isEqualTo("SEC-400-PHOTO-INVALID");
        assertThat((String) JsonPath.read(response.body(), "$.error.fieldErrors[0].field")).isEqualTo("file");
    }

    /** PNG magic + IHDR, then a marker so every photo has its own content hash. */
    static byte[] png(String marker) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'});
        out.writeBytes(marker.getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
