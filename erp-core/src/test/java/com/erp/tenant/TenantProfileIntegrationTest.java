package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.tenant.dto.TenantUpdateRequest;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.mapper.TenantMapper;
import com.erp.tenant.repository.TenantRepository;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * tenant-maturity B — the tenant's editable names and profile ({@code PUT /{id}}, REQ-TENANT-025) and the
 * suspension facts (REQ-TENANT-026, RULE-TENANT-016) over real HTTP, plus the new search fields. Not
 * transactional: every request commits, so each test provisions its own tenant.
 */
class TenantProfileIntegrationTest extends AbstractIntegrationTest {

    private static final String TENANTS = "/api/v1/platform/tenants";

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private TenantMapper tenantMapper;

    private TenantHttp http;
    private String operator;
    private String platformToken;

    @BeforeEach
    void platformOperatorLogsIn() {
        http = new TenantHttp(port);
        operator = TenantHttp.platformOperator(jdbcTemplate, passwordEncoder);
        platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE, operator);
    }

    @Test
    void update_replacesTheNamesAndProfile_keepsCodeAndStatus_andTheNewFieldsAreSearchable() {
        String code = TenantHttp.unique("PROF");
        long id = http.provisionTenant(platformToken, code);
        String email = "ops-" + code.toLowerCase() + "@tenant.test";

        HttpResponse<String> updated = http.put(platformToken, TENANTS + "/" + id, "{\"nameAr\":\"اسم جديد\","
            + "\"nameEn\":\"New name\",\"contactEmail\":\"" + email + "\",\"contactPhone\":\"+966 11 555 0100\","
            + "\"countryCode\":\"SA\",\"defaultLocale\":\"ar\",\"timezone\":\"Asia/Riyadh\",\"notes\":\"Pilot\","
            + "\"code\":\"OTHER_CODE\",\"statusCode\":\"SUSPENDED\"}");

        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(updated.body(), "$.data.code")).isEqualTo(code);
        assertThat((String) JsonPath.read(updated.body(), "$.data.statusCode")).isEqualTo("ACTIVE");
        assertThat((String) JsonPath.read(updated.body(), "$.data.nameAr")).isEqualTo("اسم جديد");
        assertThat((String) JsonPath.read(updated.body(), "$.data.nameEn")).isEqualTo("New name");
        assertThat((String) JsonPath.read(updated.body(), "$.data.contactEmail")).isEqualTo(email);
        assertThat((String) JsonPath.read(updated.body(), "$.data.contactPhone")).isEqualTo("+966 11 555 0100");
        assertThat((String) JsonPath.read(updated.body(), "$.data.countryCode")).isEqualTo("SA");
        assertThat((String) JsonPath.read(updated.body(), "$.data.defaultLocale")).isEqualTo("ar");
        assertThat((String) JsonPath.read(updated.body(), "$.data.timezone")).isEqualTo("Asia/Riyadh");
        assertThat((String) JsonPath.read(updated.body(), "$.data.notes")).isEqualTo("Pilot");
        assertThat(updated.body()).doesNotContain("tokensInvalidBefore");

        Map<String, Object> row = jdbcTemplate.queryForMap("select code, status_code, name_en, country_code"
            + " from core_tenant where id = ?", id);
        assertThat(row).containsEntry("code", code).containsEntry("status_code", "ACTIVE")
            .containsEntry("name_en", "New name").containsEntry("country_code", "SA");
        assertThat((String) JsonPath.read(http.get(platformToken, TENANTS + "/" + id).body(), "$.data.timezone"))
            .isEqualTo("Asia/Riyadh");

        List<String> byCountry = JsonPath.read(http.post(platformToken, TENANTS + "/search",
            "{\"filters\":[{\"field\":\"countryCode\",\"operator\":\"EQUALS\",\"value\":\"SA\"},"
                + "{\"field\":\"code\",\"operator\":\"EQUALS\",\"value\":\"" + code + "\"}]}").body(), "$.data.content[*].code");
        assertThat(byCountry).containsExactly(code);
        List<String> byEmail = JsonPath.read(http.post(platformToken, TENANTS + "/search",
            "{\"filters\":[{\"field\":\"contactEmail\",\"operator\":\"EQUALS\",\"value\":\"" + email + "\"}],"
                + "\"sortField\":\"countryCode\"}").body(), "$.data.content[*].code");
        assertThat(byEmail).containsExactly(code);
    }

    @Test
    void update_withoutTheOptionalFields_clearsThem_andEmptyValuesAreStoredAsNull() {
        long id = http.provisionTenant(platformToken, TenantHttp.unique("CLR"));
        assertThat(http.put(platformToken, TENANTS + "/" + id, "{\"nameAr\":\"أ\",\"nameEn\":\"A\","
            + "\"contactEmail\":\"a@tenant.test\",\"countryCode\":\"EG\",\"notes\":\"n\"}").statusCode()).isEqualTo(200);

        HttpResponse<String> cleared = http.put(platformToken, TENANTS + "/" + id,
            "{\"nameAr\":\"ب\",\"nameEn\":\"B\",\"countryCode\":\"\",\"notes\":\"   \"}");

        assertThat(cleared.statusCode()).isEqualTo(200);
        assertThat((Object) JsonPath.read(cleared.body(), "$.data.contactEmail")).isNull();
        assertThat((Object) JsonPath.read(cleared.body(), "$.data.countryCode")).isNull();
        assertThat((Object) JsonPath.read(cleared.body(), "$.data.notes")).isNull();
        assertThat(jdbcTemplate.queryForMap("select contact_email, country_code, notes from core_tenant where id = ?", id))
            .containsEntry("contact_email", null).containsEntry("country_code", null).containsEntry("notes", null);
    }

    @Test
    void update_refusesAnUnknownTenantAndMalformedFields_andChangesNothing() {
        long id = http.provisionTenant(platformToken, TenantHttp.unique("BAD"));

        HttpResponse<String> unknown = http.put(platformToken, TENANTS + "/987654321", "{\"nameAr\":\"أ\",\"nameEn\":\"A\"}");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(unknown)).isEqualTo("TENANT_NOT_FOUND");

        for (String[] bad : new String[][] {
            {"defaultLocale", "\"fr\""}, {"countryCode", "\"sau\""}, {"timezone", "\"Asia/ Riyadh\""},
            {"contactPhone", "\"call me\""}, {"contactEmail", "\"not-an-email\""}, {"nameEn", "\"  \""}}) {
            HttpResponse<String> refused = http.put(platformToken, TENANTS + "/" + id,
                "{\"nameAr\":\"أ\",\"nameEn\":\"A\",\"" + bad[0] + "\":" + bad[1] + "}");
            assertThat(refused.statusCode()).as(bad[0]).isEqualTo(400);
            assertThat(TenantHttp.errorCode(refused)).as(bad[0]).isEqualTo("VALIDATION_ERROR");
            List<String> fields = JsonPath.read(refused.body(), "$.error.fieldErrors[*].field");
            assertThat(fields).as(bad[0]).contains(bad[0]);
        }
        assertThat(jdbcTemplate.queryForObject("select name_en from core_tenant where id = ?", String.class, id))
            .startsWith("Tenant ");
    }

    @Test
    void suspension_needsAReason_recordsItsFacts_andActivationClearsThem_andSetsTheTokenCutOff() {
        String code = TenantHttp.unique("SUSR");
        long id = http.provisionTenant(platformToken, code);
        Instant before = Instant.now().minusSeconds(1);

        for (String body : new String[] {"{\"statusCode\":\"SUSPENDED\"}", "{\"statusCode\":\"SUSPENDED\",\"reason\":\" ab \"}",
            "{\"statusCode\":\"SUSPENDED\",\"reason\":\"" + "x".repeat(501) + "\"}"}) {
            HttpResponse<String> refused = http.patch(platformToken, TENANTS + "/" + id + "/status", body);
            assertThat(refused.statusCode()).isEqualTo(400);
            assertThat(TenantHttp.errorCode(refused)).isEqualTo("TENANT_SUSPENSION_REASON_REQUIRED");
        }
        assertThat(jdbcTemplate.queryForObject("select status_code from core_tenant where id = ?", String.class, id))
            .isEqualTo("ACTIVE");

        HttpResponse<String> suspended = http.patch(platformToken, TENANTS + "/" + id + "/status",
            "{\"statusCode\":\"SUSPENDED\",\"reason\":\"  Unpaid invoice  \"}");
        assertThat(suspended.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(suspended.body(), "$.data.statusCode")).isEqualTo("SUSPENDED");
        assertThat((String) JsonPath.read(suspended.body(), "$.data.suspendedBy")).isEqualTo(operator);
        assertThat((String) JsonPath.read(suspended.body(), "$.data.suspensionReason")).isEqualTo("Unpaid invoice");
        String suspendedAt = JsonPath.read(suspended.body(), "$.data.suspendedAt");
        assertThat(Instant.parse(suspendedAt)).isAfter(before);
        assertThat(TenantHttp.errorCode(http.login(code, "admin", TenantHttp.PASSWORD))).isEqualTo("TENANT_SUSPENDED");

        // re-applying the status changes nothing — not even the reason
        HttpResponse<String> again = http.patch(platformToken, TENANTS + "/" + id + "/status",
            "{\"statusCode\":\"SUSPENDED\",\"reason\":\"Another reason\"}");
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(again.body(), "$.data.suspensionReason")).isEqualTo("Unpaid invoice");
        assertThat((String) JsonPath.read(again.body(), "$.data.suspendedAt")).isEqualTo(suspendedAt);

        List<String> found = JsonPath.read(http.post(platformToken, TENANTS + "/search",
            "{\"filters\":[{\"field\":\"suspendedAt\",\"operator\":\"GREATER_THAN_OR_EQUAL\",\"value\":\"" + before + "\"},"
                + "{\"field\":\"code\",\"operator\":\"EQUALS\",\"value\":\"" + code + "\"}]}").body(), "$.data.content[*].code");
        assertThat(found).containsExactly(code);
        HttpResponse<String> malformed = http.post(platformToken, TENANTS + "/search",
            "{\"filters\":[{\"field\":\"suspendedAt\",\"operator\":\"GREATER_THAN\",\"value\":\"yesterday\"}]}");
        assertThat(malformed.statusCode()).isEqualTo(400);

        HttpResponse<String> activated = http.patch(platformToken, TENANTS + "/" + id + "/status",
            "{\"statusCode\":\"ACTIVE\",\"reason\":\"ignored\"}");
        assertThat(activated.statusCode()).isEqualTo(200);
        assertThat((Object) JsonPath.read(activated.body(), "$.data.suspendedAt")).isNull();
        assertThat((Object) JsonPath.read(activated.body(), "$.data.suspendedBy")).isNull();
        assertThat((Object) JsonPath.read(activated.body(), "$.data.suspensionReason")).isNull();
        Timestamp cutOff = jdbcTemplate.queryForObject("select tokens_invalid_before from core_tenant where id = ?",
            Timestamp.class, id);
        assertThat(cutOff).isNotNull();
        assertThat(cutOff.toInstant()).isAfterOrEqualTo(Instant.parse(suspendedAt));
        assertThat(http.login(code, "admin", TenantHttp.PASSWORD).statusCode()).isEqualTo(200);

        // re-activating an ACTIVE tenant moves no cut-off
        assertThat(http.patch(platformToken, TENANTS + "/" + id + "/status", "{\"statusCode\":\"ACTIVE\"}").statusCode())
            .isEqualTo(200);
        assertThat(jdbcTemplate.queryForObject("select tokens_invalid_before from core_tenant where id = ?",
            Timestamp.class, id)).isEqualTo(cutOff);
    }

    /**
     * The 409 {@code CONCURRENT_MODIFICATION} of {@code PUT /{id}} is the shared handler's answer to this lock failure:
     * a write from a copy read before another write committed (the {@code TenantScopedQueryIntegrationTest} pattern).
     */
    @Test
    void anUpdateFromAStaleCopy_failsTheOptimisticLock() {
        long id = http.provisionTenant(platformToken, TenantHttp.unique("LOCK"));
        Tenant first = tenantRepository.findById(id).orElseThrow();
        Tenant stale = tenantRepository.findById(id).orElseThrow();

        tenantMapper.updateEntityFromRequest(first, TenantUpdateRequest.builder().nameAr("أ").nameEn("First").build());
        tenantRepository.saveAndFlush(first);
        tenantMapper.updateEntityFromRequest(stale, TenantUpdateRequest.builder().nameAr("ب").nameEn("Stale").build());

        assertThatThrownBy(() -> tenantRepository.saveAndFlush(stale))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(jdbcTemplate.queryForObject("select name_en from core_tenant where id = ?", String.class, id))
            .isEqualTo("First");
    }

    @Test
    void thePlatformTenant_isStillProtected_beforeAnyReasonIsAskedFor() {
        HttpResponse<String> platform = http.patch(platformToken,
            TENANTS + "/" + TenantConstants.PLATFORM_TENANT_ID + "/status", "{\"statusCode\":\"SUSPENDED\"}");

        assertThat(platform.statusCode()).isEqualTo(422);
        assertThat(TenantHttp.errorCode(platform)).isEqualTo("TENANT_PLATFORM_PROTECTED");
    }

    @Test
    void theNewEndpoints_areClosedToATenantAdministrator() {
        String code = TenantHttp.unique("CLOSED");
        long id = http.provisionTenant(platformToken, code);
        String tenantToken = http.token(code, "admin");

        assertThat(http.put(tenantToken, TENANTS + "/" + id, "{\"nameAr\":\"أ\",\"nameEn\":\"A\"}").statusCode()).isEqualTo(403);
        assertThat(http.post(tenantToken, TENANTS + "/" + id + "/admin-reset",
            "{\"username\":\"admin\",\"newPassword\":\"Passw0rd-New1\"}").statusCode()).isEqualTo(403);
        assertThat(http.get(tenantToken, TENANTS + "/" + id + "/usage").statusCode()).isEqualTo(403);
        assertThat(http.put(null, TENANTS + "/" + id, "{\"nameAr\":\"أ\",\"nameEn\":\"A\"}").statusCode()).isEqualTo(401);
    }
}
