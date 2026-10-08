package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.tenant.export.TenantExportGuard;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * tenant-maturity C5 — {@code POST /api/v1/platform/tenants/{id}/export} (REQ-TENANT-037 / AC-TENANT-037, RULE-TENANT-027,
 * -028; FILE XM-FILE-003, RULE-FILE-011): every module's rows of that tenant only, no secret, one snapshot, the archive a
 * PRIVATE PLATFORM document downloaded once by the operator, the limit and the per-node guard, audit in both tenants.
 */
class TenantExportIntegrationTest extends AbstractIntegrationTest {

    private static final String TENANTS = "/api/v1/platform/tenants";
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /** The archive's files and the count of the exported tenant's rows behind each (srs-tenant.md X7). */
    private static final Map<String, String> FILES = files();

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ErpCoreProperties properties;
    @Autowired
    private TenantExportGuard guard;

    private TenantHttp http;
    private String platformToken;

    @BeforeEach
    void platformOperatorLogsIn() {
        http = new TenantHttp(port);
        platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE,
            TenantHttp.platformOperator(jdbcTemplate, passwordEncoder));
    }

    @Test
    void anExport_holdsEveryModulesRowsOfThatTenantOnly_withoutSecrets_asAPrivatePlatformDocumentDownloadedOnce()
            throws IOException {
        String codeA = TenantHttp.unique("EXA");
        long tenantA = http.provisionTenant(platformToken, codeA);
        String channelSecret = "smtp-secret-" + UUID.randomUUID();
        seedEveryModule(codeA, tenantA, channelSecret);
        String codeB = TenantHttp.unique("EXB");
        long tenantB = http.provisionTenant(platformToken, codeB);
        String userOfB = "bonly-" + codeB.toLowerCase();
        http.createUser(http.token(codeB, "admin"), userOfB);
        Map<String, Long> expected = countsOf(tenantA);
        List<String> secrets = secretsOf(tenantA);
        secrets.add(channelSecret);
        secrets.add("raw-" + codeA);
        Set<Path> temporaryBefore = temporaryArchives();

        HttpResponse<String> exported = http.post(platformToken, TENANTS + "/" + tenantA + "/export", "");

        assertThat(exported.statusCode()).as(exported.body()).isEqualTo(200);
        assertThat(((Number) JsonPath.read(exported.body(), "$.data.tenantId")).longValue()).isEqualTo(tenantA);
        assertThat((String) JsonPath.read(exported.body(), "$.data.tenantCode")).isEqualTo(codeA);
        assertThat((String) JsonPath.read(exported.body(), "$.data.fileName"))
            .startsWith("tenant-export-" + codeA + "-").endsWith("Z.zip");
        long fileId = ((Number) JsonPath.read(exported.body(), "$.data.fileId")).longValue();
        long size = ((Number) JsonPath.read(exported.body(), "$.data.sizeBytes")).longValue();
        long rowCount = ((Number) JsonPath.read(exported.body(), "$.data.rowCount")).longValue();
        String downloadToken = JsonPath.read(exported.body(), "$.data.downloadToken");
        assertThat((String) JsonPath.read(exported.body(), "$.data.downloadTokenExpiresAt")).isNotBlank();
        assertThat(temporaryArchives()).as("no temporary archive is left behind").isSubsetOf(temporaryBefore);

        Map<String, Object> document = jdbcTemplate.queryForMap("SELECT TENANT_ID, OWNER_TYPE, OWNER_ID, MODULE_CODE,"
            + " VISIBILITY, PUBLIC_SLUG, FILE_STATUS_ID, FILE_TYPE_ID, FILE_CATEGORY_FK, CONTENT_TYPE, FILE_SIZE"
            + " FROM FILE_DOCUMENT WHERE ID = ?", fileId);
        assertThat(((Number) document.get("tenant_id")).longValue()).as("stored in PLATFORM").isEqualTo(1L);
        assertThat(document.get("owner_type")).isEqualTo("CORE_TENANT");
        assertThat(((Number) document.get("owner_id")).longValue()).isEqualTo(tenantA);
        assertThat(document.get("module_code")).isEqualTo("TENANT");
        assertThat(document.get("visibility")).isEqualTo("PRIVATE");
        assertThat(document.get("public_slug")).isNull();
        assertThat(document.get("file_status_id")).isEqualTo("ACTIVE");
        assertThat(document.get("file_type_id")).isEqualTo("ARCHIVE");
        assertThat(document.get("file_category_fk")).isNull();
        assertThat(document.get("content_type")).isEqualTo("application/zip");
        assertThat(((Number) document.get("file_size")).longValue()).isEqualTo(size);

        String download = "/api/v1/files/download?token=" + URLEncoder.encode(downloadToken, StandardCharsets.UTF_8);
        String otherOperator = http.token(TenantConstants.PLATFORM_TENANT_CODE,
            TenantHttp.anotherPlatformOperator(jdbcTemplate, passwordEncoder));
        assertThat(http.getBytes(otherOperator, download).statusCode()).as("bound to the issuing operator").isEqualTo(401);
        HttpResponse<byte[]> zip = http.getBytes(platformToken, download);
        assertThat(zip.statusCode()).isEqualTo(200);
        assertThat(zip.headers().firstValue("Content-Type")).hasValue("application/zip");
        assertThat(zip.body()).hasSize((int) size);
        assertThat(http.getBytes(platformToken, download).statusCode()).as("single use").isEqualTo(401);

        Map<String, byte[]> entries = unzip(zip.body());
        Set<String> names = new HashSet<>(FILES.keySet());
        names.add("manifest.json");
        assertThat(entries.keySet()).containsExactlyInAnyOrderElementsOf(names);
        String manifest = new String(entries.get("manifest.json"), StandardCharsets.UTF_8);
        assertThat((String) JsonPath.read(manifest, "$.format")).isEqualTo("erp-tenant-export");
        assertThat((String) JsonPath.read(manifest, "$.tenantCode")).isEqualTo(codeA);
        assertThat(((Number) JsonPath.read(manifest, "$.rowCount")).longValue()).isEqualTo(rowCount);
        long sum = 0;
        for (Map.Entry<String, Long> file : expected.entrySet()) {
            byte[] content = entries.get(file.getKey());
            assertThat(Arrays.copyOf(content, 3)).as(file.getKey() + " starts with a BOM").isEqualTo(BOM);
            List<List<String>> records = parse(new String(content, 3, content.length - 3, StandardCharsets.UTF_8));
            assertThat(records.size() - 1L).as(file.getKey() + " records").isEqualTo(file.getValue());
            List<Integer> manifestRows = JsonPath.read(manifest, "$.files[?(@.path == '" + file.getKey() + "')].rows");
            assertThat(manifestRows).containsExactly(file.getValue().intValue());
            assertThat(records.get(0)).doesNotContain("TENANT_ID", "VERSION");
            sum += file.getValue();
        }
        assertThat(rowCount).isEqualTo(sum);

        List<String> userHeader = header(entries, "SEC/SEC_USER.csv");
        assertThat(userHeader).containsExactly("USER_PK", "USERNAME", "EMAIL", "REALM", "FULL_NAME_AR", "FULL_NAME_EN",
            "STATUS_CODE", "IS_ACTIVE_FL", "LAST_LOGIN_AT", "PHONE", "JOB_TITLE_AR", "JOB_TITLE_EN", "PREFERRED_LOCALE",
            "PHOTO_FILE_ID", "PASSWORD_CHANGE_REQUIRED_FL", "PASSWORD_CHANGED_AT", "CREATED_BY", "CREATED_AT", "UPDATED_BY",
            "UPDATED_AT");
        assertThat(header(entries, "FILE/FILE_DOCUMENT.csv")).doesNotContain("FILE_CONTENT", "STORAGE_REF", "PUBLIC_SLUG");
        assertThat(header(entries, "SEC/SEC_ACTIVE_SESSION.csv")).doesNotContain("TOKEN_REF");
        assertThat(header(entries, "NOTIF/NOTIF_CHANNEL_CONFIG.csv")).doesNotContain("CONFIG_JSON");
        assertThat(header(entries, "NOTIF/NOTIF_LOG.csv")).doesNotContain("VARIABLES_JSON");
        assertThat(header(entries, "TENANT/CORE_TENANT.csv")).doesNotContain("TOKENS_INVALID_BEFORE");

        String everything = entries.values().stream()
            .map(bytes -> new String(bytes, StandardCharsets.ISO_8859_1) + new String(bytes, StandardCharsets.UTF_8))
            .collect(Collectors.joining("\n"));
        assertThat(everything).doesNotContain("$2a$", "$2b$", "$2y$", "\u0089PNG");
        assertThat(secrets).isNotEmpty().allSatisfy(secret -> assertThat(everything).doesNotContain(secret));
        assertThat(everything).as("nothing of tenant B").doesNotContain(codeB, userOfB);
        assertThat(rows(entries, "CU/CU_APP_CONFIGURATION.csv")).anySatisfy(row ->
            assertThat(row).contains("EXPORT_FORMULA", "'=SUM(A1)"));
        assertThat(rows(entries, "NOTIF/NOTIF_INBOX.csv")).anySatisfy(row ->
            assertThat(row).contains("Hello, \"export\"\r\nsecond line"));
        assertThat(rows(entries, "TENANT/CORE_TENANT.csv")).singleElement().satisfies(row ->
            assertThat(row.get(1)).isEqualTo(codeA));

        assertThat(auditRows(tenantA, tenantA)).as("TENANT_EXPORTED in the tenant").isEqualTo(1);
        assertThat(auditRows(1L, tenantA)).as("TENANT_EXPORTED in PLATFORM").isEqualTo(1);
        assertThat(auditRows(tenantB, tenantB) + auditRows(1L, tenantB)).isZero();
    }

    @Test
    void anExportOverTheRowLimit_is422_storesNothing_andFreesTheTenantForTheNextExport() {
        long tenant = http.provisionTenant(platformToken, TenantHttp.unique("EXL"));
        ErpCoreProperties.Export export = properties.getTenant().getExport();
        long limit = export.getMaxRows();
        export.setMaxRows(5);
        try {
            HttpResponse<String> tooLarge = http.post(platformToken, TENANTS + "/" + tenant + "/export", "");

            assertThat(tooLarge.statusCode()).as(tooLarge.body()).isEqualTo(422);
            assertThat(TenantHttp.errorCode(tooLarge)).isEqualTo("TENANT_EXPORT_TOO_LARGE");
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FILE_DOCUMENT WHERE TENANT_ID = 1"
                + " AND OWNER_TYPE = 'CORE_TENANT' AND OWNER_ID = ?", Long.class, tenant)).isZero();
            assertThat(auditRows(1L, tenant)).isZero();
        } finally {
            export.setMaxRows(limit);
        }
        assertThat(guard.isRunning(tenant)).isFalse();
        assertThat(http.post(platformToken, TENANTS + "/" + tenant + "/export", "").statusCode()).isEqualTo(200);
    }

    @Test
    void aSecondExportOfTheSameTenant_is409_whileAnotherTenantIsExported() {
        String code = TenantHttp.unique("EXR");
        long running = http.provisionTenant(platformToken, code);
        long other = http.provisionTenant(platformToken, TenantHttp.unique("EXO"));
        assertThat(guard.tryStart(running)).isTrue();
        try {
            HttpResponse<String> second = http.post(platformToken, TENANTS + "/" + running + "/export", "");

            assertThat(second.statusCode()).as(second.body()).isEqualTo(409);
            assertThat(TenantHttp.errorCode(second)).isEqualTo("TENANT_EXPORT_IN_PROGRESS");
            assertThat((String) JsonPath.read(second.body(), "$.error.message")).contains(code);
            assertThat(http.post(platformToken, TENANTS + "/" + other + "/export", "").statusCode()).isEqualTo(200);
            assertThat(guard.isRunning(running)).as("a refused export never frees another's slot").isTrue();
        } finally {
            guard.finish(running);
        }
        assertThat(http.post(platformToken, TENANTS + "/" + running + "/export", "").statusCode()).isEqualTo(200);
    }

    @Test
    void platformAndASuspendedTenant_areExported_andOnlyAPlatformOperatorMayExport() {
        long platformRowsBefore = auditRows(1L, 1L);
        HttpResponse<String> platform = http.post(platformToken, TENANTS + "/1/export", "");
        assertThat(platform.statusCode()).as(platform.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(platform.body(), "$.data.tenantCode")).isEqualTo("PLATFORM");
        assertThat(auditRows(1L, 1L)).as("one row when the tenant is PLATFORM").isEqualTo(platformRowsBefore + 1);

        String code = TenantHttp.unique("EXS");
        long suspended = http.provisionTenant(platformToken, code);
        String tenantAdmin = http.token(code, "admin");
        assertThat(http.post(tenantAdmin, TENANTS + "/" + suspended + "/export", "").statusCode()).isEqualTo(403);
        assertThat(http.post(null, TENANTS + "/" + suspended + "/export", "").statusCode()).isEqualTo(401);
        assertThat(http.patch(platformToken, TENANTS + "/" + suspended + "/status",
            "{\"statusCode\":\"SUSPENDED\",\"reason\":\"Export while suspended\"}").statusCode()).isEqualTo(200);
        HttpResponse<String> exported = http.post(platformToken, TENANTS + "/" + suspended + "/export", "");
        assertThat(exported.statusCode()).as(exported.body()).isEqualTo(200);
        assertThat(auditRows(suspended, suspended)).isEqualTo(1);

        HttpResponse<String> missing = http.post(platformToken, TENANTS + "/987654321/export", "");
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(missing)).isEqualTo("TENANT_NOT_FOUND");
    }

    /** A second staff user, a session, a customer, a file, a notification, an inbox row, a CU override, a number series. */
    private void seedEveryModule(String code, long tenant, String channelSecret) {
        String token = http.token(code, "admin");
        http.createUser(token, "clerk");
        assertThat(http.uploadPng(token, 4711, "export.png").statusCode()).isEqualTo(201);
        assertThat(http.post(null, code, "/api/v1/public/customers/register", "{\"email\":\"c-" + code.toLowerCase()
            + "@shop.test\",\"password\":\"" + TenantHttp.PASSWORD + "\",\"fullName\":\"Customer\"}").statusCode())
            .isEqualTo(201);
        long admin = ((Number) JsonPath.read(http.get(token, "/api/v1/sec/me").body(), "$.data.userPk")).longValue();
        assertThat(http.post(token, "/api/v1/notifications/dispatch", "{\"recipientId\":" + admin
            + ",\"templateCode\":\"PASSWORD_RESET\",\"channelHint\":[\"EMAIL\"],\"moduleCode\":\"TEST\","
            + "\"referenceType\":\"TC_REF\",\"referenceId\":1,\"variables\":{\"actionLink\":\"http://x/reset?t=raw-"
            + code + "\",\"expiresAt\":\"soon\"}}").statusCode()).isEqualTo(200);
        jdbcTemplate.update("INSERT INTO NOTIF_INBOX (ID, TENANT_ID, RECIPIENT_USER_ID, TITLE_AR, TITLE_EN, BODY_AR, BODY_EN,"
            + " CREATED_BY, CREATED_AT, VERSION) VALUES (nextval('SEQ_NOTIF_INBOX'), ?, ?, 'عنوان', 'Title', 'نص',"
            + " ?, 'test', now(), 0)", tenant, admin, "Hello, \"export\"\r\nsecond line");
        jdbcTemplate.update("INSERT INTO CU_APP_CONFIGURATION (ID, TENANT_ID, CONFIG_KEY, CONFIG_VALUE, IS_ACTIVE_FL,"
            + " CREATED_BY, CREATED_AT, VERSION) VALUES (nextval('SEQ_CU_APP_CONFIGURATION'), ?, 'EXPORT_FORMULA',"
            + " '=SUM(A1)', 1, 'test', now(), 0)", tenant);
        jdbcTemplate.update("INSERT INTO CORE_NUMBER_SERIES (ID, TENANT_ID, CODE, PREFIX, CREATED_BY)"
            + " VALUES (nextval('SEQ_CORE_NUMBER_SERIES'), ?, 'EXPORT_SERIES', 'EX', 'test')", tenant);
        jdbcTemplate.update("UPDATE NOTIF_CHANNEL_CONFIG SET CONFIG_JSON = ? WHERE TENANT_ID = ?",
            "{\"password\":\"" + channelSecret + "\"}", tenant);
    }

    private Map<String, Long> countsOf(long tenant) {
        Map<String, Long> counts = new LinkedHashMap<>();
        FILES.forEach((path, table) -> counts.put(path, "CORE_TENANT".equals(table) ? 1L
            : jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE TENANT_ID = ?", Long.class, tenant)));
        Stream.of("SEC/SEC_USER.csv", "SEC/SEC_ACTIVE_SESSION.csv", "SEC/SEC_ROLE_ACTION_GRANT.csv", "MDL/MDL_LOOKUP_VALUE.csv",
                "CU/CU_APP_CONFIGURATION.csv", "FILE/FILE_DOCUMENT.csv", "NOTIF/NOTIF_LOG.csv", "NOTIF/NOTIF_INBOX.csv",
                "SEQUENCE/CORE_NUMBER_SERIES.csv", "AUDIT/CORE_AUDIT_EVENT.csv")
            .forEach(path -> assertThat(counts.get(path)).as("seeded rows in " + path).isPositive());
        return counts;
    }

    /** Password and token hashes, session token references and queued notification variables of the tenant. */
    private List<String> secretsOf(long tenant) {
        List<String> secrets = new ArrayList<>();
        Stream.of("SELECT PASSWORD_HASH FROM SEC_USER WHERE TENANT_ID = ?",
                "SELECT TOKEN_HASH FROM SEC_CUSTOMER_VERIFY_TOKEN WHERE TENANT_ID = ?",
                "SELECT TOKEN_HASH FROM SEC_PWD_RESET_TOKEN WHERE TENANT_ID = ?",
                "SELECT TOKEN_REF FROM SEC_ACTIVE_SESSION WHERE TENANT_ID = ?",
                "SELECT STORAGE_REF FROM FILE_DOCUMENT WHERE TENANT_ID = ? AND STORAGE_PROVIDER <> 'DB'",
                "SELECT VARIABLES_JSON FROM NOTIF_LOG WHERE TENANT_ID = ? AND VARIABLES_JSON IS NOT NULL")
            .forEach(sql -> secrets.addAll(jdbcTemplate.queryForList(sql, String.class, tenant)));
        assertThat(secrets).as("the tenant has hashes and session references").hasSizeGreaterThanOrEqualTo(4);
        return secrets;
    }

    private long auditRows(long inTenant, long exportedTenant) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ?"
            + " AND ACTION = 'TENANT_EXPORTED' AND ENTITY_TYPE = 'CORE_TENANT' AND ENTITY_ID = ?", Long.class,
            inTenant, String.valueOf(exportedTenant));
    }

    private static Set<Path> temporaryArchives() throws IOException {
        try (Stream<Path> files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return files.filter(file -> file.getFileName().toString().startsWith("erp-tenant-export-"))
                .collect(Collectors.toSet());
        }
    }

    private static Map<String, byte[]> unzip(byte[] archive) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.put(entry.getName(), zip.readAllBytes());
            }
        }
        return entries;
    }

    private static List<String> header(Map<String, byte[]> entries, String path) {
        return parse(new String(entries.get(path), StandardCharsets.UTF_8).substring(1)).get(0);
    }

    private static List<List<String>> rows(Map<String, byte[]> entries, String path) {
        List<List<String>> records = parse(new String(entries.get(path), StandardCharsets.UTF_8).substring(1));
        return records.subList(1, records.size());
    }

    /** RFC 4180: CRLF records, quoted fields with doubled quotes (a quoted field may hold CR LF). */
    static List<List<String>> parse(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                record.add(field.toString());
                field.setLength(0);
            } else if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                record.add(field.toString());
                field.setLength(0);
                records.add(record);
                record = new ArrayList<>();
                i++;
            } else {
                field.append(c);
            }
        }
        return records;
    }

    private static Map<String, String> files() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("AUDIT/CORE_AUDIT_EVENT.csv", "CORE_AUDIT_EVENT");
        files.put("CU/CU_APP_CONFIGURATION.csv", "CU_APP_CONFIGURATION");
        files.put("FILE/FILE_CATEGORY.csv", "FILE_CATEGORY");
        files.put("FILE/FILE_DOCUMENT.csv", "FILE_DOCUMENT");
        files.put("MDL/MDL_LOOKUP_TYPE.csv", "MDL_LOOKUP_TYPE");
        files.put("MDL/MDL_LOOKUP_VALUE.csv", "MDL_LOOKUP_VALUE");
        files.put("NOTIF/NOTIF_TEMPLATE.csv", "NOTIF_TEMPLATE");
        files.put("NOTIF/NOTIF_CHANNEL_CONFIG.csv", "NOTIF_CHANNEL_CONFIG");
        files.put("NOTIF/NOTIF_LOG.csv", "NOTIF_LOG");
        files.put("NOTIF/NOTIF_INBOX.csv", "NOTIF_INBOX");
        files.put("SEC/SEC_USER.csv", "SEC_USER");
        files.put("SEC/SEC_ROLE.csv", "SEC_ROLE");
        files.put("SEC/SEC_USER_ROLE.csv", "SEC_USER_ROLE");
        files.put("SEC/SEC_ROLE_MODULE_GRANT.csv", "SEC_ROLE_MODULE_GRANT");
        files.put("SEC/SEC_ROLE_SCREEN_GRANT.csv", "SEC_ROLE_SCREEN_GRANT");
        files.put("SEC/SEC_ROLE_ACTION_GRANT.csv", "SEC_ROLE_ACTION_GRANT");
        files.put("SEC/SEC_ACTIVE_SESSION.csv", "SEC_ACTIVE_SESSION");
        files.put("SEC/SEC_AUDIT_LOG.csv", "SEC_AUDIT_LOG");
        files.put("SEC/SEC_SIGNUP_REQUEST.csv", "SEC_SIGNUP_REQUEST");
        files.put("SEQUENCE/CORE_NUMBER_SERIES.csv", "CORE_NUMBER_SERIES");
        files.put("TENANT/CORE_TENANT.csv", "CORE_TENANT");
        return files;
    }
}
