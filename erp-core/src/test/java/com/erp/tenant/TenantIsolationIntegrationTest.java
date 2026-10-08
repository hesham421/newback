package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.erp.testsupport.AbstractIntegrationTest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 05 — cross-tenant access is impossible by construction. Two tenants A and B are
 * provisioned through the platform API, each gets an extra user, and everything below is plain HTTP
 * through the real security chain. Tenant-maturity C3 (AC-TENANT-024): one row per core module in each
 * tenant; each tenant's search sees only its own rows and the other tenant's id answers 404.
 */
class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private TenantHttp http;
    private String platformToken;
    private String codeA;
    private String codeB;
    private long tenantA;
    private long tenantB;
    private String tokenA;
    private String tokenB;
    private long userOfA;
    private long userOfB;

    @BeforeEach
    void twoTenantsWithOneExtraUserEach() {
        http = new TenantHttp(port);
        platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE,
            TenantHttp.platformOperator(jdbcTemplate, passwordEncoder));
        codeA = TenantHttp.unique("TA");
        codeB = TenantHttp.unique("TB");
        tenantA = http.provisionTenant(platformToken, codeA);
        tenantB = http.provisionTenant(platformToken, codeB);
        tokenA = http.token(codeA, "admin");
        tokenB = http.token(codeB, "admin");
        userOfA = http.createUser(tokenA, "alice");
        userOfB = http.createUser(tokenB, "bob");
    }

    @Test
    void userSearchAsA_neverReturnsBsRows() {
        HttpResponse<String> response = http.post(tokenA, "/api/v1/sec/users/search", "{\"size\":1000}");

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> usernames = JsonPath.read(response.body(), "$.data.content[*].username");
        List<Number> ids = JsonPath.read(response.body(), "$.data.content[*].userPk");
        assertThat(usernames).containsExactlyInAnyOrder("admin", "alice");
        assertThat(ids.stream().map(Number::longValue)).contains(userOfA).doesNotContain(userOfB);
        assertThat(ids.stream().map(Number::longValue).toList())
            .allSatisfy(id -> assertThat(tenantOfUser(id)).isEqualTo(tenantA));

        // a filter that would match B's user finds nothing
        HttpResponse<String> filtered = http.post(tokenA, "/api/v1/sec/users/search",
            "{\"filters\":[{\"field\":\"username\",\"operator\":\"EQUALS\",\"value\":\"bob\"}]}");
        assertThat(filtered.statusCode()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(filtered.body(), "$.data.content")).isEmpty();
    }

    @Test
    void getUserByIdOfB_asA_is404_whileBSeesIt() {
        HttpResponse<String> asA = http.get(tokenA, "/api/v1/sec/users/" + userOfB);
        assertThat(asA.statusCode()).isEqualTo(404);

        HttpResponse<String> asB = http.get(tokenB, "/api/v1/sec/users/" + userOfB);
        assertThat(asB.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(asB.body(), "$.data.username")).isEqualTo("bob");
    }

    @Test
    void usernamesAreUniquePerTenant_notGlobally() {
        // both provisioned administrators are called "admin", like the PLATFORM bootstrap admin
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from sec_user where username = 'admin' and tenant_id in (1, ?, ?)",
            Integer.class, tenantA, tenantB)).isEqualTo(3);

        http.createUser(tokenB, "alice");   // same username as A's user: allowed in another tenant

        HttpResponse<String> duplicateInA = http.post(tokenA, "/api/v1/sec/users", "{\"username\":\"alice\","
            + "\"email\":\"alice2@users.test\",\"fullNameAr\":\"م\",\"fullNameEn\":\"U\",\"password\":\""
            + TenantHttp.PASSWORD + "\"}");
        assertThat(duplicateInA.statusCode()).isEqualTo(409);
    }

    @Test
    void login_withoutXTenantCode_is400TenantRequired_andAnUnknownCodeIs404() {
        HttpResponse<String> noHeader = http.login(null, "admin", TenantHttp.PASSWORD);
        assertThat(noHeader.statusCode()).isEqualTo(400);
        assertThat(TenantHttp.errorCode(noHeader)).isEqualTo("TENANT_REQUIRED");

        HttpResponse<String> unknown = http.login("NO_SUCH_TENANT", "admin", TenantHttp.PASSWORD);
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(unknown)).isEqualTo("TENANT_NOT_FOUND");
    }

    @Test
    void login_isScopedToTheHeaderTenant() {
        // alice exists in A only
        assertThat(http.login(codeA, "alice", TenantHttp.PASSWORD).statusCode()).isEqualTo(200);
        assertThat(http.login(codeB, "alice", TenantHttp.PASSWORD).statusCode()).isEqualTo(401);
        // the header is case-insensitive and trimmed
        assertThat(http.login(" " + codeA.toLowerCase() + " ", "alice", TenantHttp.PASSWORD).statusCode()).isEqualTo(200);
    }

    @Test
    void aTokenOfA_ignoresAnXTenantCodeHeaderOfB() {
        // the token's tid wins over the header: A's admin still sees A's users only
        HttpResponse<String> response = http.post(tokenA, codeB, "/api/v1/sec/users/search", "{\"size\":1000}");
        assertThat(response.statusCode()).isEqualTo(200);
        List<String> usernames = JsonPath.read(response.body(), "$.data.content[*].username");
        assertThat(usernames).containsExactlyInAnyOrder("admin", "alice");
        assertThat(TenantHttp.tenantIdOf(tokenA)).isEqualTo(tenantA);
        assertThat(TenantHttp.tenantIdOf(tokenB)).isEqualTo(tenantB);
    }

    @Test
    void secRoles_searchAndGetById_stayInTheCallersTenant() {
        String codeOfA = TenantHttp.unique("ISO_A_");
        String codeOfB = TenantHttp.unique("ISO_B_");
        long roleOfA = created(http.post(tokenA, "/api/v1/sec/roles", role(codeOfA)), "$.data.rolePk");
        long roleOfB = created(http.post(tokenB, "/api/v1/sec/roles", role(codeOfB)), "$.data.rolePk");

        assertSearchSeesOnlyItsOwnRows(tokenA, tenantA, "/api/v1/sec/roles/search", "$.data.content[*].rolePk",
            "sec_role", "role_pk", "code", codeOfA, roleOfA, codeOfB, roleOfB);
        assertSearchSeesOnlyItsOwnRows(tokenB, tenantB, "/api/v1/sec/roles/search", "$.data.content[*].rolePk",
            "sec_role", "role_pk", "code", codeOfB, roleOfB, codeOfA, roleOfA);
        assertNotFound(http.get(tokenA, "/api/v1/sec/roles/" + roleOfB), "SEC-404-ROLE");
        assertThat(http.get(tokenB, "/api/v1/sec/roles/" + roleOfB).statusCode()).isEqualTo(200);
    }

    @Test
    void mdlLookupTypes_searchAndUpdateById_stayInTheCallersTenant() {
        String keyOfA = TenantHttp.unique("ISO_A_");
        String keyOfB = TenantHttp.unique("ISO_B_");
        long typeOfA = created(http.post(tokenA, "/api/v1/mdl/lookup-types", lookupType(keyOfA)), "$.data.lookupTypePk");
        long typeOfB = created(http.post(tokenB, "/api/v1/mdl/lookup-types", lookupType(keyOfB)), "$.data.lookupTypePk");

        assertSearchSeesOnlyItsOwnRows(tokenA, tenantA, "/api/v1/mdl/lookup-types/search",
            "$.data.content[*].lookupTypePk", "mdl_lookup_type", "lookup_type_pk", "key", keyOfA, typeOfA, keyOfB, typeOfB);
        assertSearchSeesOnlyItsOwnRows(tokenB, tenantB, "/api/v1/mdl/lookup-types/search",
            "$.data.content[*].lookupTypePk", "mdl_lookup_type", "lookup_type_pk", "key", keyOfB, typeOfB, keyOfA, typeOfA);
        // MDL has no read-by-id endpoint: the update by id is the by-id path, and it must not touch B's row
        assertNotFound(http.put(tokenA, "/api/v1/mdl/lookup-types/" + typeOfB,
            "{\"nameAr\":\"مخترق\",\"nameEn\":\"Hijacked\"}"), "MDL-404-TYPE");
        assertThat(jdbcTemplate.queryForObject("select name_en from mdl_lookup_type where lookup_type_pk = ?",
            String.class, typeOfB)).isEqualTo("Type " + keyOfB);
    }

    @Test
    void fileDocuments_listAndGetById_stayInTheCallersTenant() {
        long owner = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L);
        long fileOfA = created(http.uploadPng(tokenA, owner, "a.png"), "$.data.id");
        long fileOfB = created(http.uploadPng(tokenB, owner, "b.png"), "$.data.id");
        String list = "/api/v1/files?ownerType=PRODUCT&moduleCode=SHOP&size=200&ownerId=" + owner;

        assertThat(ids(http.get(tokenA, list), "$.data.content[*].id")).containsExactly(fileOfA);
        assertThat(ids(http.get(tokenB, list), "$.data.content[*].id")).containsExactly(fileOfB);
        assertThat(tenantOf("file_document", "id", fileOfA)).isEqualTo(tenantA);
        assertNotFound(http.get(tokenA, "/api/v1/files/" + fileOfB), "FILE_DOCUMENT_NOT_FOUND");
        assertThat(http.get(tokenB, "/api/v1/files/" + fileOfB).statusCode()).isEqualTo(200);
    }

    /** tenant-maturity E (RULE-TENANT-018): a tenant's logo is a document of that tenant only. */
    @Test
    void aTenantsLogo_livesInItsOwnRows_andIsInvisibleToAnotherTenant() {
        HttpResponse<String> set = http.putFile(platformToken, "/api/v1/platform/tenants/" + tenantA + "/logo", "a.png",
            TenantBrandingIntegrationTest.PNG);
        assertThat(set.statusCode()).as(set.body()).isEqualTo(200);
        long logoOfA = jdbcTemplate.queryForObject("select logo_file_id from core_tenant where id = ?", Long.class, tenantA);
        String list = "/api/v1/files?ownerType=CORE_TENANT&moduleCode=TENANT&ownerId=" + tenantA;

        assertThat(tenantOf("file_document", "id", logoOfA)).isEqualTo(tenantA);
        assertThat(ids(http.get(tokenA, list), "$.data.content[*].id")).containsExactly(logoOfA);
        assertThat(ids(http.get(tokenB, list), "$.data.content[*].id")).isEmpty();
        assertNotFound(http.get(tokenB, "/api/v1/files/" + logoOfA), "FILE_DOCUMENT_NOT_FOUND");
        String urlOfA = JsonPath.read(set.body(), "$.data.logoUrl");
        assertThat(http.getBytes(urlOfA.replace("/" + codeA + "/", "/" + codeB + "/")).statusCode())
            .as("the slug does not resolve under another tenant's code").isEqualTo(404);
        assertThat((Object) JsonPath.read(http.get(tokenB, "/api/v1/tenant/me").body(), "$.data.logoUrl")).isNull();
    }

    @Test
    void notifTemplates_searchAndGetById_stayInTheCallersTenant() {
        String codeOfA = TenantHttp.unique("ISO_A_");
        String codeOfB = TenantHttp.unique("ISO_B_");
        long templateOfA = created(http.post(tokenA, "/api/v1/notifications/templates", template(codeOfA)), "$.data.id");
        long templateOfB = created(http.post(tokenB, "/api/v1/notifications/templates", template(codeOfB)), "$.data.id");

        assertSearchSeesOnlyItsOwnRows(tokenA, tenantA, "/api/v1/notifications/templates/search", "$.data.content[*].id",
            "notif_template", "id", "templateCode", codeOfA, templateOfA, codeOfB, templateOfB);
        assertSearchSeesOnlyItsOwnRows(tokenB, tenantB, "/api/v1/notifications/templates/search", "$.data.content[*].id",
            "notif_template", "id", "templateCode", codeOfB, templateOfB, codeOfA, templateOfA);
        assertNotFound(http.get(tokenA, "/api/v1/notifications/templates/" + templateOfB), "NOTIF_TEMPLATE_NOT_FOUND");
        assertThat(http.get(tokenB, "/api/v1/notifications/templates/" + templateOfB).statusCode()).isEqualTo(200);
    }

    @Test
    void cuConfigurations_searchAndGetByKey_stayInTheCallersTenant() {
        String keyOfA = TenantHttp.unique("ISO_A_");
        String keyOfB = TenantHttp.unique("ISO_B_");
        created(http.post(tokenA, "/api/v1/common/configurations", configuration(keyOfA)), "$.data.id");
        created(http.post(tokenB, "/api/v1/common/configurations", configuration(keyOfB)), "$.data.id");

        // a provisioned tenant starts with no override: its search lists exactly the one it created
        HttpResponse<String> searchA = http.post(tokenA, "/api/v1/common/configurations/search", "{\"size\":200}");
        HttpResponse<String> searchB = http.post(tokenB, "/api/v1/common/configurations/search", "{\"size\":200}");
        assertThat(searchA.statusCode()).isEqualTo(200);
        assertThat(searchB.statusCode()).isEqualTo(200);
        List<String> keysOfA = JsonPath.read(searchA.body(), "$.data.content[*].configKey");
        List<String> keysOfB = JsonPath.read(searchB.body(), "$.data.content[*].configKey");
        assertThat(keysOfA).containsExactly(keyOfA);
        assertThat(keysOfB).containsExactly(keyOfB);
        assertThat(jdbcTemplate.queryForObject("select tenant_id from cu_app_configuration where config_key = ?",
            Long.class, keyOfA)).isEqualTo(tenantA);
        assertNotFound(http.get(tokenA, "/api/v1/common/configurations/" + keyOfB), "APP_CONFIGURATION_NOT_FOUND");
        assertThat(http.get(tokenB, "/api/v1/common/configurations/" + keyOfB).statusCode()).isEqualTo(200);
    }

    @Test
    void sequenceNumberSeries_searchAndGetById_stayInTheCallersTenant() {
        String codeOfA = TenantHttp.unique("ISO_A_");
        String codeOfB = TenantHttp.unique("ISO_B_");
        long seriesOfA = created(http.post(tokenA, "/api/v1/sequence/series", "{\"code\":\"" + codeOfA + "\"}"), "$.data.id");
        long seriesOfB = created(http.post(tokenB, "/api/v1/sequence/series", "{\"code\":\"" + codeOfB + "\"}"), "$.data.id");

        assertSearchSeesOnlyItsOwnRows(tokenA, tenantA, "/api/v1/sequence/series/search", "$.data.content[*].id",
            "core_number_series", "id", "code", codeOfA, seriesOfA, codeOfB, seriesOfB);
        assertSearchSeesOnlyItsOwnRows(tokenB, tenantB, "/api/v1/sequence/series/search", "$.data.content[*].id",
            "core_number_series", "id", "code", codeOfB, seriesOfB, codeOfA, seriesOfA);
        assertNotFound(http.get(tokenA, "/api/v1/sequence/series/" + seriesOfB), "NUMBER_SERIES_NOT_FOUND");
        assertThat(http.get(tokenB, "/api/v1/sequence/series/" + seriesOfB).statusCode()).isEqualTo(200);
    }

    @Test
    void auditEvents_ofAnActionInEachTenant_areSeenOnlyByThatTenant() {
        long roleOfA = created(http.post(tokenA, "/api/v1/sec/roles", role(TenantHttp.unique("ISO_A_"))), "$.data.rolePk");
        long roleOfB = created(http.post(tokenB, "/api/v1/sec/roles", role(TenantHttp.unique("ISO_B_"))), "$.data.rolePk");
        String byEntity = "/api/v1/audit/events?entityType=SEC_ROLE&size=200&entityId=";

        List<Long> eventsOfA = ids(http.get(tokenA, byEntity + roleOfA), "$.data.content[*].id");
        assertThat(eventsOfA).isNotEmpty()
            .allSatisfy(id -> assertThat(tenantOf("core_audit_event", "id", id)).isEqualTo(tenantA));
        assertThat(ids(http.get(tokenA, byEntity + roleOfB), "$.data.content[*].id")).isEmpty();
        assertThat(ids(http.get(tokenB, byEntity + roleOfB), "$.data.content[*].id")).isNotEmpty();
        assertThat(ids(http.get(tokenB, byEntity + roleOfA), "$.data.content[*].id")).isEmpty();
        // AUDIT has no read-by-id endpoint; the unfiltered log of A holds only A's events
        assertThat(ids(http.get(tokenA, "/api/v1/audit/events?size=200"), "$.data.content[*].id"))
            .contains(eventsOfA.get(0))
            .allSatisfy(id -> assertThat(tenantOf("core_audit_event", "id", id)).isEqualTo(tenantA));
    }

    /**
     * The unfiltered search returns only {@code tenant}'s rows and never {@code foreignId}; filtered on the
     * own code it finds exactly {@code ownId}; filtered on the other tenant's code it finds nothing.
     */
    private void assertSearchSeesOnlyItsOwnRows(String token, long tenant, String path, String idsPath, String table,
                                                String idColumn, String codeField, String ownCode, long ownId,
                                                String foreignCode, long foreignId) {
        List<Long> all = ids(http.post(token, path, "{\"size\":200}"), idsPath);
        assertThat(all).isNotEmpty().doesNotContain(foreignId)
            .allSatisfy(id -> assertThat(tenantOf(table, idColumn, id)).isEqualTo(tenant));
        assertThat(ids(http.post(token, path, codeFilter(codeField, ownCode)), idsPath)).containsExactly(ownId);
        assertThat(ids(http.post(token, path, codeFilter(codeField, foreignCode)), idsPath)).isEmpty();
    }

    private static void assertNotFound(HttpResponse<String> response, String errorCode) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(response)).isEqualTo(errorCode);
    }

    private static long created(HttpResponse<String> response, String idPath) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return ((Number) JsonPath.read(response.body(), idPath)).longValue();
    }

    private static List<Long> ids(HttpResponse<String> response, String idsPath) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        List<Number> ids = JsonPath.read(response.body(), idsPath);
        return ids.stream().map(Number::longValue).toList();
    }

    private long tenantOf(String table, String idColumn, long id) {
        return jdbcTemplate.queryForObject("select tenant_id from " + table + " where " + idColumn + " = ?", Long.class, id);
    }

    private static String codeFilter(String field, String value) {
        return "{\"size\":200,\"filters\":[{\"field\":\"" + field + "\",\"operator\":\"EQUALS\",\"value\":\"" + value + "\"}]}";
    }

    private static String role(String code) {
        return "{\"code\":\"" + code + "\",\"nameAr\":\"دور\",\"nameEn\":\"Role " + code + "\"}";
    }

    private static String lookupType(String key) {
        return "{\"key\":\"" + key + "\",\"ownerModuleCode\":\"MDL\",\"nameAr\":\"نوع\",\"nameEn\":\"Type " + key + "\"}";
    }

    private static String template(String code) {
        return "{\"templateCode\":\"" + code + "\",\"nameAr\":\"قالب\",\"nameEn\":\"Template " + code
            + "\",\"bodyAr\":\"نص\",\"bodyEn\":\"Body\"}";
    }

    private static String configuration(String key) {
        return "{\"configKey\":\"" + key + "\",\"configValue\":\"value-" + key + "\"}";
    }

    private long tenantOfUser(long userPk) {
        return jdbcTemplate.queryForObject("select tenant_id from sec_user where user_pk = ?", Long.class, userPk);
    }
}
