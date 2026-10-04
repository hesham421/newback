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
 * erp-core step 10 acceptance — every {@code @Audited} core entity has its audit rows: each is created
 * (and, where its API allows, updated) over HTTP and its {@code CREATE} (and {@code UPDATE}) row is
 * asserted. After each test the whole table is scanned for sensitive field names.
 *
 * <p>{@code NumberSeries} (named by the step) belongs to step 09, which is not merged on this branch;
 * it gets its annotation and its row here when step 10 is rebased onto step 09.
 */
class AuditedEntitiesCoverageIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private AuditHttp http;
    private String token;

    /**
     * A tenant of this class's own for the MDL and NOTIF cases: tenant provisioning copies PLATFORM's
     * lookup types and templates, so creating them in PLATFORM would change what other tests' new
     * tenants receive (PlatformTenantApiIntegrationTest counts them).
     */
    private static String ownTenantCode;
    private static long ownTenantId;

    @BeforeEach
    void setUp() {
        http = new AuditHttp(port);
        token = http.token(AuditHttp.PLATFORM, AuditHttp.platformOperator(jdbc, passwordEncoder));
    }

    @AfterEach
    void noSensitiveFieldInAnyChanges() {
        AuditRows.assertNoSensitiveFieldAnywhere(jdbc);
    }

    @Test
    void secUser() {
        String username = AuditHttp.unique("cov-u-").toLowerCase();
        long id = http.createUser(token, username);
        AuditHttp.expect(http.put(token, "/api/v1/sec/users/" + id,
            "{\"email\":\"" + username + "@users.test\",\"fullNameAr\":\"م\",\"fullNameEn\":\"Changed\"}"), 200, "update");
        assertCreateAndUpdate("SEC_USER", id, List.of("fullNameAr", "fullNameEn"));
    }

    @Test
    void secRole() {
        String code = AuditHttp.unique("COVR");
        HttpResponse<String> created = http.post(token, "/api/v1/sec/roles",
            "{\"code\":\"" + code + "\",\"nameAr\":\"دور\",\"nameEn\":\"Role\"}");
        AuditHttp.expect(created, 201, "create role");
        long id = AuditHttp.id(created, "$.data.rolePk");
        AuditHttp.expect(http.put(token, "/api/v1/sec/roles/" + id,
            "{\"nameAr\":\"دور\",\"nameEn\":\"Role renamed\"}"), 200, "update role");
        assertCreateAndUpdate("SEC_ROLE", id, List.of("nameEn"));
    }

    @Test
    void coreTenant() {
        String code = AuditHttp.unique("COVT");
        long id = http.provisionTenant(token, code);
        AuditHttp.expect(http.patch(token, "/api/v1/platform/tenants/" + id + "/status",
            "{\"statusCode\":\"SUSPENDED\"}"), 200, "suspend tenant");
        // CORE_TENANT is global: its rows belong to the tenant that changed it (PLATFORM)
        assertCreateAndUpdate("CORE_TENANT", id, List.of("statusCode"));
        assertThat(AuditRows.of(jdbc, "CORE_TENANT", id)).allSatisfy(row -> assertThat(row).containsEntry("tenant_id", 1L));
    }

    @Test
    void fileCategoryAndFileDocument() {
        String code = AuditHttp.unique("COVC");
        HttpResponse<String> created = http.post(token, "/api/v1/files/categories", "{\"categoryCode\":\"" + code
            + "\",\"nameAr\":\"فئة\",\"nameEn\":\"Category\",\"allowPublic\":true}");
        AuditHttp.expect(created, 201, "create category");
        long categoryId = AuditHttp.id(created, "$.data.id");
        AuditHttp.expect(http.put(token, "/api/v1/files/categories/" + categoryId,
            "{\"nameAr\":\"فئة\",\"nameEn\":\"Category renamed\"}"), 200, "update category");
        assertCreateAndUpdate("FILE_CATEGORY", categoryId, List.of("nameEn"));

        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R', 'a', 'u'};
        long documentId = http.upload(token, categoryId, "cover.png", png);
        AuditHttp.expect(http.patch(token, "/api/v1/files/" + documentId + "/visibility",
            "{\"visibility\":\"PUBLIC\"}"), 200, "publish");
        List<Map<String, Object>> creates = AuditRows.of(jdbc, "FILE_DOCUMENT", documentId, "CREATE");
        assertThat(creates).hasSize(1);
        assertThat(AuditRows.changedFields(creates.get(0))).contains("fileName", "contentType", "fileSize")
            .doesNotContain("contentHash");
        assertThat(AuditRows.of(jdbc, "FILE_DOCUMENT", documentId, "UPDATE"))
            .anySatisfy(row -> assertThat(AuditRows.changedFields(row)).contains("visibility", "publicSlug"));
    }

    @Test
    void notifTemplate() {
        String tenantToken = ownTenantToken();
        String code = AuditHttp.unique("COV_TPL_");
        HttpResponse<String> created = http.post(tenantToken, "/api/v1/notifications/templates", "{\"templateCode\":\""
            + code + "\",\"nameAr\":\"قالب\",\"nameEn\":\"Template\",\"bodyAr\":\"نص\",\"bodyEn\":\"Body\"}");
        AuditHttp.expect(created, 201, "create template");
        long id = AuditHttp.id(created, "$.data.id");
        AuditHttp.expect(http.put(tenantToken, "/api/v1/notifications/templates/" + id,
            "{\"nameAr\":\"قالب\",\"nameEn\":\"Template\",\"bodyAr\":\"نص\",\"bodyEn\":\"Body changed\"}"),
            200, "update template");
        assertCreateAndUpdate("NOTIF_TEMPLATE", id, List.of("bodyEn"));
        assertThat(AuditRows.of(jdbc, "NOTIF_TEMPLATE", id)).allSatisfy(row -> assertThat(row).containsEntry("tenant_id", ownTenantId));
    }

    @Test
    void mdlLookupTypeAndValue() {
        String tenantToken = ownTenantToken();
        String key = AuditHttp.unique("COV_TYPE_");
        HttpResponse<String> type = http.post(tenantToken, "/api/v1/mdl/lookup-types", "{\"key\":\"" + key
            + "\",\"ownerModuleCode\":\"SEC\",\"nameAr\":\"نوع\",\"nameEn\":\"Type\"}");
        AuditHttp.expect(type, 201, "create lookup type");
        long typeId = AuditHttp.id(type, "$.data.lookupTypePk");
        AuditHttp.expect(http.put(tenantToken, "/api/v1/mdl/lookup-types/" + typeId,
            "{\"nameAr\":\"نوع\",\"nameEn\":\"Type renamed\"}"), 200, "update lookup type");
        assertCreateAndUpdate("MDL_LOOKUP_TYPE", typeId, List.of("nameEn"));

        HttpResponse<String> value = http.post(tenantToken, "/api/v1/mdl/lookup-types/" + typeId + "/values",
            "{\"code\":\"V1\",\"nameAr\":\"قيمة\",\"nameEn\":\"Value\",\"sortOrder\":1}");
        AuditHttp.expect(value, 201, "create lookup value");
        long valueId = AuditHttp.id(value, "$.data.lookupValuePk");
        AuditHttp.expect(http.put(tenantToken, "/api/v1/mdl/lookup-values/" + valueId,
            "{\"nameAr\":\"قيمة\",\"nameEn\":\"Value renamed\",\"sortOrder\":1}"), 200, "update lookup value");
        assertCreateAndUpdate("MDL_LOOKUP_VALUE", valueId, List.of("nameEn"));
        // the association is recorded as the referenced id
        String changes = (String) AuditRows.of(jdbc, "MDL_LOOKUP_VALUE", valueId, "CREATE").get(0).get("changes");
        assertThat(JsonPath.<List<Number>>read(changes, "$[?(@.field == 'lookupType')].new"))
            .singleElement().satisfies(referenced -> assertThat(referenced.longValue()).isEqualTo(typeId));
    }

    @Test
    void cuAppConfiguration() {
        String key = "audit.coverage." + AuditHttp.unique("k").toLowerCase();
        HttpResponse<String> created = http.post(token, "/api/v1/common/configurations",
            "{\"configKey\":\"" + key + "\",\"configValue\":\"one\"}");
        AuditHttp.expect(created, 201, "create configuration");
        long id = AuditHttp.id(created, "$.data.id");
        AuditHttp.expect(http.put(token, "/api/v1/common/configurations/" + key,
            "{\"configValue\":\"two\"}"), 200, "update configuration");
        assertCreateAndUpdate("CU_APP_CONFIGURATION", id, List.of("configValue"));
    }

    private String ownTenantToken() {
        if (ownTenantCode == null) {
            String code = AuditHttp.unique("COVN");
            ownTenantId = http.provisionTenant(token, code);
            ownTenantCode = code;
        }
        return http.token(ownTenantCode, AuditHttp.TENANT_ADMIN);
    }

    /** One CREATE row, and one UPDATE row whose changes are exactly {@code updatedFields}. */
    private void assertCreateAndUpdate(String entityType, long id, List<String> updatedFields) {
        List<Map<String, Object>> creates = AuditRows.of(jdbc, entityType, id, "CREATE");
        assertThat(creates).as("%s #%s CREATE rows", entityType, id).hasSize(1);
        assertThat(AuditRows.changedFields(creates.get(0))).as("%s CREATE changes", entityType).isNotEmpty();
        List<Map<String, Object>> updates = AuditRows.of(jdbc, entityType, id, "UPDATE");
        assertThat(updates).as("%s #%s UPDATE rows", entityType, id).hasSize(1);
        assertThat(AuditRows.changedFields(updates.get(0))).as("%s UPDATE changes", entityType)
            .containsExactlyInAnyOrderElementsOf(updatedFields);
    }
}
