package com.erp.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.tenant.TenantConstants;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

/**
 * erp-core step 11 — the reporting endpoints end to end, over HTTP through the real security chains:
 * definitions filtered by authority, parameter validation, CSV export (UTF-8 BOM, Arabic headers, RFC 4180
 * quoting), JSON export, the export cap ({@code erp.core.report.max-export-rows=5} in this context), the
 * customer realm locked out, and tenant isolation of the core reports' data.
 *
 * <p>A test-only provider {@code TST_CSV_SAMPLE} (module {@code TSTR}) returns {@code rows} rows of
 * awkward values (commas, quotes, line breaks, Arabic, a formula) and does not know its total, so the
 * export cap is decided on the rows it returned.
 */
@Import(ReportApiIntegrationTest.TestReports.class)
@TestPropertySource(properties = "erp.core.report.max-export-rows=5")
class ReportApiIntegrationTest extends AbstractIntegrationTest {

    static final String SAMPLE = "TST_CSV_SAMPLE";

    @TestConfiguration
    static class TestReports {

        @Bean
        ReportProvider testCsvSampleReport() {
            return new ReportProvider() {
                @Override
                public String code() {
                    return SAMPLE;
                }

                @Override
                public String moduleCode() {
                    return "TSTR";
                }

                @Override
                public String titleAr() {
                    return "عينة CSV";
                }

                @Override
                public String titleEn() {
                    return "CSV sample";
                }

                @Override
                public List<ReportParam> params() {
                    return List.of(ReportParam.of("rows", ParamType.INTEGER, true, "عدد الصفوف", "Row count"));
                }

                @Override
                public ReportResult run(Map<String, Object> params, Pageable page) {
                    long requested = (Long) params.get("rows");
                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (long i = 1; i <= Math.min(requested, page.getPageSize()); i++) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("no", i);
                        row.put("name", "Doe, \"J\" " + i);
                        row.put("nameAr", "شركة الأمل، فرع " + i);
                        row.put("formula", "=1+" + i);
                        rows.add(row);
                    }
                    return new ReportResult(List.of(
                        new ReportColumn("no", ColumnType.INTEGER, "الرقم", "No."),
                        new ReportColumn("name", ColumnType.STRING, "الاسم", "Name"),
                        new ReportColumn("nameAr", ColumnType.STRING, "الاسم العربي", "Arabic name"),
                        new ReportColumn("formula", ColumnType.STRING, "صيغة", "Formula")), rows, Map.of());
                }
            };
        }
    }

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private ReportHttp http;
    private String platformToken;

    @BeforeEach
    void operator() {
        http = new ReportHttp(port);
        platformToken = http.staffToken(TenantConstants.PLATFORM_TENANT_CODE,
            ReportHttp.platformOperator(jdbcTemplate, passwordEncoder));
    }

    @Test
    void definitions_listTheThreeCoreReports_forASuperRole() {
        HttpResponse<String> response = http.get(platformToken, "/api/v1/report/definitions");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        List<String> codes = JsonPath.read(response.body(), "$.data[*].code");
        assertThat(codes).contains("SEC_USER_LIST", "AUDIT_EVENT_LIST", "NOTIF_LOG_SUMMARY", SAMPLE);
        assertThat(codes).hasSize(4);
        assertThat((List<String>) JsonPath.read(response.body(), "$.data[?(@.code=='SEC_USER_LIST')].authority"))
            .containsExactly("SEC:REPORT:SEC_USER_LIST");
        assertThat((List<String>) JsonPath.read(response.body(),
            "$.data[?(@.code=='NOTIF_LOG_SUMMARY')].params[?(@.name=='channel')].lookupKey"))
            .containsExactly("NOTIF_CHANNEL");

        HttpResponse<String> one = http.get(platformToken, "/api/v1/report/definitions/AUDIT_EVENT_LIST");
        assertThat(one.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(one.body(), "$.data.moduleCode")).isEqualTo("AUDIT");
        assertThat((String) JsonPath.read(one.body(), "$.data.titleEn")).isEqualTo("Audit event list");

        // the report screen joins step 10's AUDIT module row (named by AuditPermissions), no second module row
        assertThat(jdbcTemplate.queryForList("SELECT name_en FROM sec_module_reg WHERE code = 'AUDIT'", String.class))
            .containsExactly("Audit Log");
        assertThat(jdbcTemplate.queryForList("SELECT s.page_code FROM sec_screen_reg s JOIN sec_module_reg m"
            + " ON m.module_reg_pk = s.module_id WHERE m.code = 'AUDIT' ORDER BY s.page_code", String.class))
            .containsExactly("AUDIT_EVENTS", "AUDIT_REPORTS");
        assertThat(jdbcTemplate.queryForObject("SELECT a.action_code FROM sec_action_reg a"
            + " WHERE a.permission_code = 'AUDIT:REPORT:AUDIT_EVENT_LIST'", String.class)).isEqualTo("AUDIT_EVENT_LIST");
    }

    @Test
    void definitions_areFilteredByTheCallersAuthorities_andRunNeedsTheReportsPermission() {
        String role = ReportHttp.unique("RV");
        jdbcTemplate.update("INSERT INTO SEC_ROLE (ROLE_PK, TENANT_ID, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, IS_SUPER,"
            + " CREATED_BY, CREATED_AT) VALUES (nextval('SEQ_SEC_ROLE'), 1, ?, 'مشاهد', 'Viewer', TRUE, FALSE, 'test', now())",
            role);
        for (String permission : List.of("PERM_SEC_REPORTS_VIEW", "SEC:REPORT:SEC_USER_LIST")) {
            jdbcTemplate.update("INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, TENANT_ID, ROLE_ID, ACTION_ID,"
                + " GRANTED_BY, GRANTED_AT) SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'), 1, r.ROLE_PK, a.ACTION_REG_PK,"
                + " 'test', now() FROM SEC_ROLE r, SEC_ACTION_REG a WHERE r.TENANT_ID = 1 AND r.CODE = ?"
                + " AND a.PERMISSION_CODE = ?", role, permission);
        }
        String viewer = ReportHttp.createUser(jdbcTemplate, passwordEncoder, 1L, "STAFF",
            "rv-" + UUID.randomUUID().toString().substring(0, 8), ReportHttp.unique("rv").toLowerCase() + "@x.test");
        ReportHttp.assignRole(jdbcTemplate, 1L, viewer, role);
        String nobody = ReportHttp.createUser(jdbcTemplate, passwordEncoder, 1L, "STAFF",
            "rn-" + UUID.randomUUID().toString().substring(0, 8), ReportHttp.unique("rn").toLowerCase() + "@x.test");
        String viewerToken = http.staffToken(TenantConstants.PLATFORM_TENANT_CODE, viewer);
        String nobodyToken = http.staffToken(TenantConstants.PLATFORM_TENANT_CODE, nobody);

        HttpResponse<String> viewerList = http.get(viewerToken, "/api/v1/report/definitions");
        assertThat(viewerList.statusCode()).isEqualTo(200);
        assertThat((List<String>) JsonPath.read(viewerList.body(), "$.data[*].code")).containsExactly("SEC_USER_LIST");
        assertThat((List<String>) JsonPath.read(http.get(nobodyToken, "/api/v1/report/definitions").body(),
            "$.data[*].code")).isEmpty();

        HttpResponse<String> allowed = http.post(viewerToken, "/api/v1/report/SEC_USER_LIST/run", "{}");
        assertThat(allowed.statusCode()).as(allowed.body()).isEqualTo(200);

        HttpResponse<String> definition = http.get(viewerToken, "/api/v1/report/definitions/NOTIF_LOG_SUMMARY");
        assertThat(definition.statusCode()).isEqualTo(403);
        assertThat(ReportHttp.errorCode(definition)).isEqualTo("ACCESS_DENIED");
        HttpResponse<String> run = http.post(viewerToken, "/api/v1/report/NOTIF_LOG_SUMMARY/run", "{}");
        assertThat(run.statusCode()).isEqualTo(403);
        assertThat(ReportHttp.errorCode(run)).isEqualTo("ACCESS_DENIED");
        HttpResponse<byte[]> export = http.postBytes(nobodyToken, "/api/v1/report/SEC_USER_LIST/export?format=csv", "{}", "en");
        assertThat(export.statusCode()).isEqualTo(403);
    }

    @Test
    void unknownReport_is404ReportNotFound() {
        HttpResponse<String> definition = http.get(platformToken, "/api/v1/report/definitions/NO_SUCH_REPORT");
        assertThat(definition.statusCode()).isEqualTo(404);
        assertThat(ReportHttp.errorCode(definition)).isEqualTo("REPORT_NOT_FOUND");
        assertThat((String) JsonPath.read(definition.body(), "$.error.message")).contains("NO_SUCH_REPORT");

        HttpResponse<String> run = http.post(platformToken, "/api/v1/report/NO_SUCH_REPORT/run", "{}");
        assertThat(run.statusCode()).isEqualTo(404);
        assertThat(ReportHttp.errorCode(run)).isEqualTo("REPORT_NOT_FOUND");
    }

    @Test
    void run_withAMissingRequiredOrInvalidParameter_is400ReportParamInvalid() {
        HttpResponse<String> missing = http.post(platformToken, "/api/v1/report/" + SAMPLE + "/run", "{\"params\":{}}");
        assertThat(missing.statusCode()).as(missing.body()).isEqualTo(400);
        assertThat(ReportHttp.errorCode(missing)).isEqualTo("REPORT_PARAM_INVALID");
        assertThat((List<String>) JsonPath.read(missing.body(), "$.error.fieldErrors[*].field")).containsExactly("rows");

        HttpResponse<String> arabic = http.post(platformToken, null, "/api/v1/report/" + SAMPLE + "/run", "{}", "ar");
        assertThat(arabic.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(arabic.body(), "$.error.message")).isEqualTo("معامل التقرير 'rows' مفقود أو غير صالح");

        HttpResponse<String> wrongType = http.post(platformToken, "/api/v1/report/" + SAMPLE + "/run",
            "{\"params\":{\"rows\":\"many\",\"extra\":1}}");
        assertThat(wrongType.statusCode()).isEqualTo(400);
        assertThat((List<String>) JsonPath.read(wrongType.body(), "$.error.fieldErrors[*].field"))
            .containsExactly("rows", "extra");

        HttpResponse<String> badLookup = http.post(platformToken, "/api/v1/report/NOTIF_LOG_SUMMARY/run",
            "{\"params\":{\"channel\":\"FAX\",\"dateFrom\":\"04-10-2026\"}}");
        assertThat(badLookup.statusCode()).isEqualTo(400);
        assertThat((List<String>) JsonPath.read(badLookup.body(), "$.error.fieldErrors[*].field"))
            .containsExactly("channel", "dateFrom");

        HttpResponse<String> ok = http.post(platformToken, "/api/v1/report/NOTIF_LOG_SUMMARY/run",
            "{\"params\":{\"channel\":\"EMAIL\",\"dateFrom\":\"2020-01-01\"}}");
        assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);

        HttpResponse<String> paged = http.post(platformToken, "/api/v1/report/" + SAMPLE + "/run",
            "{\"params\":{\"rows\":3},\"page\":0,\"size\":2}");
        assertThat(paged.statusCode()).isEqualTo(200);
        assertThat((List<Object>) JsonPath.read(paged.body(), "$.data.rows")).hasSize(2);
        assertThat((Integer) JsonPath.read(paged.body(), "$.data.size")).isEqualTo(2);
    }

    @Test
    void csvExport_hasTheUtf8Bom_arabicHeadersForAcceptLanguageAr_andRfc4180Quoting() {
        HttpResponse<byte[]> arabic = http.postBytes(platformToken, "/api/v1/report/" + SAMPLE + "/export?format=csv",
            "{\"params\":{\"rows\":2}}", "ar");

        assertThat(arabic.statusCode()).isEqualTo(200);
        assertThat(arabic.headers().firstValue("Content-Type").orElseThrow()).startsWith("text/csv");
        assertThat(arabic.headers().firstValue("Content-Disposition").orElseThrow())
            .contains("attachment").contains("TST_CSV_SAMPLE.csv");
        byte[] body = arabic.body();
        assertThat(Arrays.copyOf(body, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        String text = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        String[] lines = text.split("\r\n");
        assertThat(lines[0]).isEqualTo("الرقم,الاسم,الاسم العربي,صيغة");
        // the Arabic header row, byte for byte, right after the BOM
        byte[] header = "الرقم,الاسم,الاسم العربي,صيغة\r\n".getBytes(StandardCharsets.UTF_8);
        assertThat(Arrays.copyOfRange(body, 3, 3 + header.length)).isEqualTo(header);
        assertThat(lines).hasSize(3);
        // the Arabic comma (U+060C) is not a CSV separator, so that cell needs no quotes
        assertThat(lines[1]).isEqualTo("1,\"Doe, \"\"J\"\" 1\",شركة الأمل، فرع 1,'=1+1");

        HttpResponse<byte[]> english = http.postBytes(platformToken, "/api/v1/report/" + SAMPLE + "/export",
            "{\"params\":{\"rows\":1}}", "en");
        assertThat(english.statusCode()).isEqualTo(200);
        assertThat(new String(english.body(), StandardCharsets.UTF_8)).startsWith("﻿No.,Name,Arabic name,Formula\r\n");
    }

    @Test
    void jsonExport_returnsTheWholeResultAsAJsonFile() {
        HttpResponse<byte[]> response = http.postBytes(platformToken, "/api/v1/report/" + SAMPLE + "/export?format=json",
            "{\"params\":{\"rows\":3}}", "en");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        assertThat(response.headers().firstValue("Content-Disposition").orElseThrow()).contains("TST_CSV_SAMPLE.json");
        String json = new String(response.body(), StandardCharsets.UTF_8);
        assertThat((String) JsonPath.read(json, "$.code")).isEqualTo(SAMPLE);
        assertThat((List<Object>) JsonPath.read(json, "$.rows")).hasSize(3);
        assertThat((List<String>) JsonPath.read(json, "$.columns[*].labelAr")).contains("الاسم العربي");
        assertThat((String) JsonPath.read(json, "$.rows[0].nameAr")).isEqualTo("شركة الأمل، فرع 1");

        HttpResponse<String> badFormat = http.post(platformToken, "/api/v1/report/" + SAMPLE + "/export?format=pdf",
            "{\"params\":{\"rows\":1}}");
        assertThat(badFormat.statusCode()).isEqualTo(400);
        assertThat(ReportHttp.errorCode(badFormat)).isEqualTo("REPORT_PARAM_INVALID");
    }

    @Test
    void exportCap_isEnforced() {
        HttpResponse<byte[]> atCap = http.postBytes(platformToken, "/api/v1/report/" + SAMPLE + "/export",
            "{\"params\":{\"rows\":5}}", "en");
        assertThat(atCap.statusCode()).isEqualTo(200);
        assertThat(new String(atCap.body(), StandardCharsets.UTF_8).split("\r\n")).hasSize(6);

        HttpResponse<String> overCap = http.post(platformToken, "/api/v1/report/" + SAMPLE + "/export",
            "{\"params\":{\"rows\":6}}");
        assertThat(overCap.statusCode()).isEqualTo(422);
        assertThat(ReportHttp.errorCode(overCap)).isEqualTo("REPORT_EXPORT_TOO_LARGE");
        assertThat((String) JsonPath.read(overCap.body(), "$.error.message")).contains("5");
    }

    @Test
    void customerToken_isRejectedWith403() {
        String email = ReportHttp.unique("cust").toLowerCase() + "@shop.test";
        ReportHttp.createUser(jdbcTemplate, passwordEncoder, TenantConstants.PLATFORM_TENANT_ID, "CUSTOMER", email, email);
        String customerToken = http.customerToken(TenantConstants.PLATFORM_TENANT_CODE, email);

        HttpResponse<String> list = http.get(customerToken, "/api/v1/report/definitions");
        assertThat(list.statusCode()).isEqualTo(403);
        HttpResponse<String> run = http.post(customerToken, "/api/v1/report/SEC_USER_LIST/run", "{}");
        assertThat(run.statusCode()).isEqualTo(403);
        HttpResponse<byte[]> export = http.postBytes(customerToken, "/api/v1/report/SEC_USER_LIST/export", "{}", "en");
        assertThat(export.statusCode()).isEqualTo(403);
    }

    @Test
    void coreReports_seeOnlyTheCallersTenant() {
        String codeA = ReportHttp.unique("RA");
        String codeB = ReportHttp.unique("RB");
        long tenantA = http.provisionTenant(platformToken, codeA);
        long tenantB = http.provisionTenant(platformToken, codeB);
        String emailA = ReportHttp.unique("ua").toLowerCase() + "@a.test";
        String emailB = ReportHttp.unique("ub").toLowerCase() + "@b.test";
        ReportHttp.createUser(jdbcTemplate, passwordEncoder, tenantA, "STAFF", "user-a", emailA);
        ReportHttp.createUser(jdbcTemplate, passwordEncoder, tenantB, "STAFF", "user-b", emailB);
        insertNotificationLogs(tenantA, "EMAIL", "FAILED", 2);
        insertNotificationLogs(tenantA, "SMS", "SENT", 1);
        insertNotificationLogs(tenantB, "EMAIL", "FAILED", 5);
        String tokenA = http.staffToken(codeA, ReportHttp.TENANT_ADMIN);
        String tokenB = http.staffToken(codeB, ReportHttp.TENANT_ADMIN);

        // SEC_USER_LIST (JSON): A's accounts only
        HttpResponse<String> usersA = http.post(tokenA, "/api/v1/report/SEC_USER_LIST/run", "{\"params\":{\"realm\":\"staff\"}}");
        assertThat(usersA.statusCode()).as(usersA.body()).isEqualTo(200);
        List<String> emails = JsonPath.read(usersA.body(), "$.data.rows[*].email");
        assertThat(emails).containsExactlyInAnyOrder("admin@" + codeA.toLowerCase() + ".test", emailA);
        assertThat(((Number) JsonPath.read(usersA.body(), "$.data.totalRows")).longValue()).isEqualTo(2L);
        assertThat(usersA.body()).doesNotContain(emailB).doesNotContain("passwordHash");

        // SEC_USER_LIST (CSV): the same rows, quoted, never another tenant's
        HttpResponse<byte[]> csvA = http.postBytes(tokenA, "/api/v1/report/SEC_USER_LIST/export?format=csv", "{}", "en");
        assertThat(csvA.statusCode()).isEqualTo(200);
        String csv = new String(csvA.body(), StandardCharsets.UTF_8);
        assertThat(csv).contains(emailA).contains("\"Report, \"\"User\"\"\"").doesNotContain(emailB);

        // NOTIF_LOG_SUMMARY: grouped by channel/status/day, totals of A only
        HttpResponse<String> summaryA = http.post(tokenA, "/api/v1/report/NOTIF_LOG_SUMMARY/run", "{}");
        assertThat(summaryA.statusCode()).as(summaryA.body()).isEqualTo(200);
        assertThat((Integer) JsonPath.read(summaryA.body(), "$.data.totals.count")).isEqualTo(3);
        assertThat((List<Object>) JsonPath.read(summaryA.body(), "$.data.rows")).hasSize(2);
        assertThat((List<Integer>) JsonPath.read(summaryA.body(),
            "$.data.rows[?(@.channel=='EMAIL' && @.status=='FAILED')].count")).containsExactly(2);
        HttpResponse<String> emailOnly = http.post(tokenA, "/api/v1/report/NOTIF_LOG_SUMMARY/run",
            "{\"params\":{\"channel\":\"EMAIL\"}}");
        assertThat((Integer) JsonPath.read(emailOnly.body(), "$.data.totals.count")).isEqualTo(2);
        HttpResponse<String> summaryB = http.post(tokenB, "/api/v1/report/NOTIF_LOG_SUMMARY/run", "{}");
        assertThat((Integer) JsonPath.read(summaryB.body(), "$.data.totals.count")).isEqualTo(5);

        // AUDIT_EVENT_LIST (generic CORE_AUDIT_EVENT, step 10): A's admin LOGIN is there, B's never is
        HttpResponse<String> auditA = http.post(tokenA, "/api/v1/report/AUDIT_EVENT_LIST/run",
            "{\"params\":{\"action\":\"login\"},\"size\":200}");
        assertThat(auditA.statusCode()).as(auditA.body()).isEqualTo(200);
        assertThat((List<String>) JsonPath.read(auditA.body(), "$.data.rows[*].action")).isNotEmpty()
            .containsOnly("LOGIN");
        assertThat((List<String>) JsonPath.read(auditA.body(), "$.data.rows[*].actor")).contains(ReportHttp.TENANT_ADMIN);
        long loginRowsOfA = jdbcTemplate.queryForObject("SELECT count(*) FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ?"
            + " AND ACTION = 'LOGIN'", Long.class, tenantA);
        assertThat(((Number) JsonPath.read(auditA.body(), "$.data.totalRows")).longValue()).isEqualTo(loginRowsOfA);
        long allRowsOfA = jdbcTemplate.queryForObject("SELECT count(*) FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ?",
            Long.class, tenantA);
        HttpResponse<String> allOfA = http.post(tokenA, "/api/v1/report/AUDIT_EVENT_LIST/run", "{}");
        assertThat(((Number) JsonPath.read(allOfA.body(), "$.data.totalRows")).longValue()).isEqualTo(allRowsOfA);
        assertThat(allRowsOfA).isLessThan(jdbcTemplate.queryForObject("SELECT count(*) FROM CORE_AUDIT_EVENT",
            Long.class));
        HttpResponse<byte[]> auditCsvB = http.postBytes(tokenB, "/api/v1/report/AUDIT_EVENT_LIST/export?format=csv",
            "{\"params\":{\"action\":\"LOGIN\"}}", "en");
        assertThat(auditCsvB.statusCode()).isEqualTo(200);
        String csvB = new String(auditCsvB.body(), StandardCharsets.UTF_8);
        assertThat(csvB.lines().count()).isEqualTo(1L + jdbcTemplate.queryForObject(
            "SELECT count(*) FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ? AND ACTION = 'LOGIN'", Integer.class, tenantB));
    }

    private void insertNotificationLogs(long tenantId, String channel, String status, int count) {
        Long templateId = jdbcTemplate.queryForObject(
            "SELECT min(ID) FROM NOTIF_TEMPLATE WHERE TENANT_ID = ?", Long.class, tenantId);
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update("INSERT INTO NOTIF_LOG (ID, TENANT_ID, RECIPIENT_ID, CHANNEL_TYPE_ID, NOTIFICATION_STATUS_ID,"
                + " MODULE_CODE, RETRY_COUNT, TEMPLATE_FK, CREATED_BY, CREATED_AT) VALUES (nextval('SEQ_NOTIF_LOG'), ?, 1,"
                + " ?, ?, 'TEST', 0, ?, 'test', now())", tenantId, channel, status, templateId);
        }
    }
}
