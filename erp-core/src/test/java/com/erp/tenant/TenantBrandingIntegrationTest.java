package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * tenant-maturity E — tenant branding (REQ-TENANT-029 … 032, RULE-TENANT-018 … 021, ADR-TENANT-005): the platform
 * operator sets, replaces and removes a tenant's logo (a PUBLIC document in that tenant's own rows) and its brand
 * colour; every user of the tenant reads it through {@code /api/v1/tenant/me}, an anonymous visitor through the public
 * branding by code, rate-limited per client address. Multipart limits as the reference application's (1 MB = logo rule).
 */
@TestPropertySource(properties = {
    "spring.servlet.multipart.max-file-size=15MB",
    "spring.servlet.multipart.max-request-size=25MB"})
class TenantBrandingIntegrationTest extends AbstractIntegrationTest {

    private static final String TENANTS = "/api/v1/platform/tenants";

    /** A real 1×1 PNG. */
    static final byte[] PNG = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
    static final byte[] PLAIN_SVG = ("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\">"
        + "<rect width=\"10\" height=\"10\" fill=\"#1A2B3C\"/></svg>").getBytes(StandardCharsets.UTF_8);
    static final byte[] SCRIPT_SVG = ("<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script>"
        + "<rect width=\"1\" height=\"1\"/></svg>").getBytes(StandardCharsets.UTF_8);
    static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0};

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ErpCoreProperties properties;
    @Autowired
    private WebApplicationContext webContext;
    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilterChain;

    private TenantHttp http;
    private String platformToken;
    private String operator;
    private String code;
    private long id;
    private String adminToken;

    @BeforeEach
    void aFreshTenantAndItsAdministrator() {
        http = new TenantHttp(port);
        operator = TenantHttp.platformOperator(jdbcTemplate, passwordEncoder);
        platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE, operator);
        code = TenantHttp.unique("BRD");
        id = http.provisionTenant(platformToken, code);
        adminToken = http.token(code, "admin");
    }

    @Test
    void aLogo_isStoredInTheTenantsOwnRows_shownEverywhere_andServedUnderItsCode() {
        HttpResponse<String> set = http.putFile(platformToken, TENANTS + "/" + id + "/logo", "brand.png", PNG);

        assertThat(set.statusCode()).as(set.body()).isEqualTo(200);
        String logoUrl = JsonPath.read(set.body(), "$.data.logoUrl");
        assertThat(logoUrl).startsWith("/api/v1/public/files/" + code + "/");
        assertThat((String) JsonPath.read(http.get(platformToken, TENANTS + "/" + id).body(), "$.data.logoUrl"))
            .isEqualTo(logoUrl);
        HttpResponse<String> search = http.post(platformToken, TENANTS + "/search",
            "{\"filters\":[{\"field\":\"code\",\"operator\":\"EQUALS\",\"value\":\"" + code + "\"}]}");
        assertThat((List<String>) JsonPath.read(search.body(), "$.data.content[*].logoUrl")).containsExactly(logoUrl);

        HttpResponse<String> me = http.get(adminToken, "/api/v1/tenant/me");
        assertThat(me.statusCode()).as(me.body()).isEqualTo(200);
        Map<String, Object> branding = JsonPath.read(me.body(), "$.data");
        assertThat(branding).containsOnlyKeys("code", "nameAr", "nameEn", "logoUrl", "brandColor", "defaultLocale");
        assertThat(branding).containsEntry("code", code).containsEntry("logoUrl", logoUrl);

        HttpResponse<byte[]> served = http.getBytes(logoUrl);
        assertThat(served.statusCode()).isEqualTo(200);
        assertThat(served.body()).isEqualTo(PNG);
        assertThat(served.headers().firstValue("Content-Type")).hasValue("image/png");
        assertThat(served.headers().firstValue("Content-Disposition")).hasValueSatisfying(v -> assertThat(v).startsWith("inline"));

        Long logoId = jdbcTemplate.queryForObject("SELECT LOGO_FILE_ID FROM CORE_TENANT WHERE ID = ?", Long.class, id);
        Map<String, Object> document = jdbcTemplate.queryForMap("SELECT TENANT_ID, OWNER_TYPE, OWNER_ID, MODULE_CODE,"
            + " VISIBILITY, FILE_STATUS_ID, FILE_NAME, FILE_CATEGORY_FK FROM FILE_DOCUMENT WHERE ID = ?", logoId);
        assertThat(document).containsEntry("tenant_id", id).containsEntry("owner_type", "CORE_TENANT")
            .containsEntry("owner_id", id).containsEntry("module_code", "TENANT").containsEntry("visibility", "PUBLIC")
            .containsEntry("file_status_id", "ACTIVE").containsEntry("file_name", "logo.png");
        assertThat(document.get("file_category_fk")).isNull();
    }

    @Test
    void replacingTheLogo_discardsThePrevious_aPlainSvgIsAnAttachment_andRefusalsKeepTheCurrentOne() {
        String pngUrl = logoUrl(http.putFile(platformToken, TENANTS + "/" + id + "/logo", "a.png", PNG));
        Long pngId = logoFileId();
        String svgUrl = logoUrl(http.putFile(platformToken, TENANTS + "/" + id + "/logo", "b.svg", PLAIN_SVG));

        assertThat(svgUrl).isNotEqualTo(pngUrl).startsWith("/api/v1/public/files/" + code + "/");
        assertThat(http.getBytes(pngUrl).statusCode()).isEqualTo(404);
        assertThat(jdbcTemplate.queryForObject("SELECT FILE_STATUS_ID || '/' || VISIBILITY FROM FILE_DOCUMENT WHERE ID = ?",
            String.class, pngId)).isEqualTo("DELETED/PRIVATE");
        HttpResponse<byte[]> svg = http.getBytes(svgUrl);
        assertThat(svg.statusCode()).isEqualTo(200);
        assertThat(svg.headers().firstValue("Content-Type")).hasValue("image/svg+xml");
        assertThat(svg.headers().firstValue("Content-Disposition")).hasValueSatisfying(v -> assertThat(v).startsWith("attachment"));
        assertThat(svg.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
        assertThat(svg.headers().firstValue("Content-Security-Policy")).hasValueSatisfying(v -> assertThat(v).contains("sandbox"));

        byte[] tooLarge = Arrays.copyOf(PNG, 1_048_577);
        byte[] duplicateIds = ("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect id=\"a\"/><circle id=\"a\" r=\"1\"/></svg>")
            .getBytes(StandardCharsets.UTF_8);
        for (byte[] refused : List.of(SCRIPT_SVG, EXE, tooLarge, duplicateIds, new byte[0])) {
            HttpResponse<String> answer = http.putFile(platformToken, TENANTS + "/" + id + "/logo", "x.svg", refused);
            assertThat(answer.statusCode()).as(answer.body()).isEqualTo(400);
            assertThat(TenantHttp.errorCode(answer)).isEqualTo("TENANT_LOGO_INVALID");
            assertThat((String) JsonPath.read(answer.body(), "$.error.fieldErrors[0].field")).isEqualTo("file");
        }
        HttpResponse<String> noPart = http.put(platformToken, TENANTS + "/" + id + "/logo", "{}");
        assertThat(noPart.statusCode()).isEqualTo(400);
        assertThat(TenantHttp.errorCode(noPart)).isEqualTo("VALIDATION_ERROR");

        assertThat((String) JsonPath.read(http.get(platformToken, TENANTS + "/" + id).body(), "$.data.logoUrl"))
            .isEqualTo(svgUrl);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FILE_DOCUMENT WHERE TENANT_ID = ? AND OWNER_TYPE ="
            + " 'CORE_TENANT' AND FILE_STATUS_ID <> 'DELETED'", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void removingTheLogo_clearsItEverywhere_isIdempotent_andUnknownTenantsAre404() {
        String url = logoUrl(http.putFile(platformToken, TENANTS + "/" + id + "/logo", "a.png", PNG));

        assertThat(http.delete(platformToken, TENANTS + "/" + id + "/logo").statusCode()).isEqualTo(204);

        assertThat((Object) JsonPath.read(http.get(platformToken, TENANTS + "/" + id).body(), "$.data.logoUrl")).isNull();
        assertThat((Object) JsonPath.read(http.get(adminToken, "/api/v1/tenant/me").body(), "$.data.logoUrl")).isNull();
        assertThat(http.getBytes(url).statusCode()).isEqualTo(404);
        assertThat(logoFileId()).isNull();
        assertThat(http.delete(platformToken, TENANTS + "/" + id + "/logo").statusCode()).isEqualTo(204);

        for (HttpResponse<String> unknown : List.of(
                http.putFile(platformToken, TENANTS + "/987654321/logo", "a.png", PNG),
                http.delete(platformToken, TENANTS + "/987654321/logo"),
                http.patch(platformToken, TENANTS + "/987654321/branding", "{\"brandColor\":\"#000000\"}"))) {
            assertThat(unknown.statusCode()).as(unknown.body()).isEqualTo(404);
            assertThat(TenantHttp.errorCode(unknown)).isEqualTo("TENANT_NOT_FOUND");
        }
    }

    @Test
    void theBrandColour_isValidated_storedUpperCase_andCleared() {
        HttpResponse<String> set = http.patch(platformToken, TENANTS + "/" + id + "/branding", "{\"brandColor\":\" #1a2b3c \"}");
        assertThat(set.statusCode()).as(set.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(set.body(), "$.data.brandColor")).isEqualTo("#1A2B3C");
        assertThat((String) JsonPath.read(http.get(adminToken, "/api/v1/tenant/me").body(), "$.data.brandColor"))
            .isEqualTo("#1A2B3C");

        for (String wrong : List.of("red", "#12345", "#1234567", "1A2B3C")) {
            HttpResponse<String> refused = http.patch(platformToken, TENANTS + "/" + id + "/branding",
                "{\"brandColor\":\"" + wrong + "\"}");
            assertThat(refused.statusCode()).as(wrong).isEqualTo(400);
            assertThat(TenantHttp.errorCode(refused)).isEqualTo("TENANT_BRAND_COLOR_INVALID");
            assertThat((String) JsonPath.read(refused.body(), "$.error.fieldErrors[0].field")).isEqualTo("brandColor");
        }
        assertThat(jdbcTemplate.queryForObject("SELECT BRAND_COLOR FROM CORE_TENANT WHERE ID = ?", String.class, id))
            .isEqualTo("#1A2B3C");

        HttpResponse<String> cleared = http.patch(platformToken, TENANTS + "/" + id + "/branding", "{\"brandColor\":null}");
        assertThat(cleared.statusCode()).isEqualTo(200);
        assertThat((Object) JsonPath.read(cleared.body(), "$.data.brandColor")).isNull();
        http.patch(platformToken, TENANTS + "/" + id + "/branding", "{\"brandColor\":\"#ABCDEF\"}");
        assertThat((Object) JsonPath.read(http.patch(platformToken, TENANTS + "/" + id + "/branding", "{}").body(),
            "$.data.brandColor")).isNull();
    }

    @Test
    void theWritePaths_arePlatformOnly_andATenantAdministratorChangesNothing() {
        for (HttpResponse<String> refused : List.of(
                http.putFile(adminToken, TENANTS + "/" + id + "/logo", "a.png", PNG),
                http.delete(adminToken, TENANTS + "/" + id + "/logo"),
                http.patch(adminToken, TENANTS + "/" + id + "/branding", "{\"brandColor\":\"#000000\"}"))) {
            assertThat(refused.statusCode()).as(refused.body()).isEqualTo(403);
            assertThat(TenantHttp.errorCode(refused)).isEqualTo("SEC-403-FORBIDDEN");
        }
        assertThat(http.putFile(null, TENANTS + "/" + id + "/logo", "a.png", PNG).statusCode()).isEqualTo(401);
        assertThat(logoFileId()).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FILE_DOCUMENT WHERE TENANT_ID = ?", Integer.class, id))
            .isZero();
    }

    @Test
    void tenantMe_answersTheTokensTenant_forStaff_customers_aPendingForcedChange_andPlatform() {
        http.patch(platformToken, TENANTS + "/" + id + "/branding", "{\"brandColor\":\"#102030\"}");
        String email = "c-" + code.toLowerCase() + "@shop.test";
        assertThat(http.post(null, code, "/api/v1/public/customers/register", "{\"email\":\"" + email + "\",\"password\":\""
            + TenantHttp.PASSWORD + "\",\"fullName\":\"Customer\"}").statusCode()).isEqualTo(201);
        jdbcTemplate.update("UPDATE SEC_USER SET STATUS_CODE = 'ACTIVE' WHERE TENANT_ID = ? AND USERNAME = ?"
            + " AND REALM = 'CUSTOMER'", id, email);
        HttpResponse<String> customerLogin = http.post(null, code, "/api/v1/public/customers/login",
            "{\"email\":\"" + email + "\",\"password\":\"" + TenantHttp.PASSWORD + "\"}");
        assertThat(customerLogin.statusCode()).as(customerLogin.body()).isEqualTo(200);
        String customerToken = JsonPath.read(customerLogin.body(), "$.data.accessToken");

        HttpResponse<String> asCustomer = http.get(customerToken, "/api/v1/tenant/me");
        assertThat(asCustomer.statusCode()).as(asCustomer.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(asCustomer.body(), "$.data.code")).isEqualTo(code);
        assertThat((String) JsonPath.read(asCustomer.body(), "$.data.brandColor")).isEqualTo("#102030");
        assertThat(http.get(customerToken, "/api/v1/sec/me").statusCode()).as("other core paths stay STAFF-only").isEqualTo(403);
        for (HttpResponse<String> otherMethod : List.of(http.post(customerToken, "/api/v1/tenant/me", "{}"),
                http.put(customerToken, "/api/v1/tenant/me", "{}"), http.patch(customerToken, "/api/v1/tenant/me", "{}"))) {
            assertThat(otherMethod.statusCode()).as("only GET is realm-neutral").isEqualTo(403);
            assertThat(TenantHttp.errorCode(otherMethod)).isEqualTo("REALM_MISMATCH");
        }

        http.createUser(adminToken, "clerk");
        String clerkToken = http.token(code, "clerk");
        assertThat(http.get(clerkToken, "/api/v1/sec/menu").statusCode()).as("forced change pending").isEqualTo(403);
        assertThat((String) JsonPath.read(http.get(clerkToken, "/api/v1/tenant/me").body(), "$.data.code")).isEqualTo(code);

        assertThat((String) JsonPath.read(http.get(platformToken, "/api/v1/tenant/me").body(), "$.data.code"))
            .isEqualTo(TenantConstants.PLATFORM_TENANT_CODE);
        assertThat(http.get(null, "/api/v1/tenant/me").statusCode()).isEqualTo(401);
    }

    @Test
    void publicBranding_takesTheTenantFromThePath_unknownIs404_suspendedIs403() {
        String url = logoUrl(http.putFile(platformToken, TENANTS + "/" + id + "/logo", "a.png", PNG));
        http.patch(platformToken, TENANTS + "/" + id + "/branding", "{\"brandColor\":\"#0A0B0C\"}");

        HttpResponse<String> branding = http.get(null, "/api/v1/public/tenants/" + code.toLowerCase() + "/branding");
        assertThat(branding.statusCode()).as(branding.body()).isEqualTo(200);
        Map<String, Object> data = JsonPath.read(branding.body(), "$.data");
        assertThat(data).containsOnlyKeys("code", "nameAr", "nameEn", "logoUrl", "brandColor", "defaultLocale")
            .containsEntry("code", code).containsEntry("logoUrl", url).containsEntry("brandColor", "#0A0B0C");
        assertThat(http.get(adminToken, "/api/v1/public/tenants/" + code + "/branding").statusCode())
            .as("a token is irrelevant there").isEqualTo(200);

        HttpResponse<String> unknown = http.get(null, "/api/v1/public/tenants/NO_SUCH_" + code + "/branding");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(unknown)).isEqualTo("TENANT_NOT_FOUND");

        assertThat(http.patch(platformToken, TENANTS + "/" + id + "/status",
            "{\"statusCode\":\"SUSPENDED\",\"reason\":\"Branding test\"}").statusCode()).isEqualTo(200);
        HttpResponse<String> suspended = http.get(null, "/api/v1/public/tenants/" + code + "/branding");
        assertThat(suspended.statusCode()).isEqualTo(403);
        assertThat(TenantHttp.errorCode(suspended)).isEqualTo("TENANT_SUSPENDED");
        assertThat(http.getBytes(url).statusCode()).isEqualTo(403);
        assertThat(http.get(adminToken, "/api/v1/tenant/me").statusCode()).isEqualTo(403);
        assertThat((String) JsonPath.read(http.get(platformToken, TENANTS + "/" + id).body(), "$.data.logoUrl"))
            .as("the operator still sees it").isEqualTo(url);

        http.patch(platformToken, TENANTS + "/" + id + "/status", "{\"statusCode\":\"ACTIVE\"}");
        assertThat(http.get(null, "/api/v1/public/tenants/" + code + "/branding").statusCode()).isEqualTo(200);
    }

    @Test
    void platformMayCarryALogo_andEveryChangeIsAuditedInTheTenantAndInPlatform() {
        http.putFile(platformToken, TENANTS + "/" + id + "/logo", "a.png", PNG);
        http.putFile(platformToken, TENANTS + "/" + id + "/logo", "b.png", PNG);
        http.delete(platformToken, TENANTS + "/" + id + "/logo");

        String rows = "SELECT ACTOR || '|' || ACTOR_REALM || '|' || ENTITY_TYPE FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ?"
            + " AND ACTION = 'TENANT_LOGO_CHANGED' AND ENTITY_ID = ? ORDER BY ID";
        List<String> inTenant = jdbcTemplate.queryForList(rows, String.class, id, String.valueOf(id));
        List<String> inPlatform = jdbcTemplate.queryForList(rows, String.class, TenantConstants.PLATFORM_TENANT_ID,
            String.valueOf(id));
        assertThat(inTenant).hasSize(3).allSatisfy(row -> assertThat(row).isEqualTo(operator + "|STAFF|CORE_TENANT"));
        assertThat(inPlatform).isEqualTo(inTenant);
        assertThat(jdbcTemplate.queryForList("SELECT SUMMARY_EN FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ? AND ACTION ="
            + " 'TENANT_LOGO_CHANGED' AND ENTITY_ID = ? ORDER BY ID", String.class, id, String.valueOf(id)))
            .allSatisfy(summary -> assertThat(summary).contains(code))
            .last().satisfies(summary -> assertThat(summary).contains("removed"));

        String platformId = String.valueOf(TenantConstants.PLATFORM_TENANT_ID);
        int before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CORE_AUDIT_EVENT WHERE ACTION = 'TENANT_LOGO_CHANGED'"
            + " AND ENTITY_ID = ?", Integer.class, platformId);
        HttpResponse<String> platformLogo = http.putFile(platformToken, TENANTS + "/1/logo", "p.png", PNG);
        assertThat(platformLogo.statusCode()).as(platformLogo.body()).isEqualTo(200);
        assertThat(logoUrl(platformLogo)).startsWith("/api/v1/public/files/PLATFORM/");
        assertThat(http.delete(platformToken, TENANTS + "/1/logo").statusCode()).isEqualTo(204);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CORE_AUDIT_EVENT WHERE ACTION = 'TENANT_LOGO_CHANGED'"
            + " AND ENTITY_ID = ?", Integer.class, platformId)).as("one row per change for PLATFORM").isEqualTo(before + 2);
    }

    /**
     * RULE-TENANT-022 through the real security chain (MockMvc over the context's {@code springSecurityFilterChain}): each
     * call carries its own client address, so no other test's bucket is touched and no host network feature is needed.
     */
    @Test
    void publicBranding_isRateLimitedPerClientAddress_unknownCodesIncluded() throws Exception {
        int capacity = properties.getTenant().getPublicBrandingRateLimit().getCapacity();
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webContext).addFilters(securityFilterChain).build();
        String address = "203.0.113." + (1 + Math.abs(code.hashCode() % 250));
        for (int call = 1; call <= capacity; call++) {
            String path = "/api/v1/public/tenants/" + (call % 2 == 0 ? "NO_SUCH_" : "") + code + "/branding";
            assertThat(mvc.perform(get(path).with(from(address))).andReturn().getResponse().getStatus())
                .as("call " + call).isIn(200, 404);
        }
        for (String path : List.of(code, "NO_SUCH_" + code)) {
            MockHttpServletResponse limited = mvc.perform(get("/api/v1/public/tenants/" + path + "/branding")
                .with(from(address))).andReturn().getResponse();
            assertThat(limited.getStatus()).as(limited.getContentAsString()).isEqualTo(429);
            assertThat((String) JsonPath.read(limited.getContentAsString(), "$.error.code"))
                .isEqualTo("TENANT_BRANDING_RATE_LIMITED");
            assertThat(Long.parseLong(limited.getHeader("Retry-After"))).isPositive();
        }
        assertThat(mvc.perform(get("/api/v1/public/tenants/" + code + "/branding").with(from("198.51.100.9")))
            .andReturn().getResponse().getStatus()).as("another address is still served").isEqualTo(200);
        assertThat(mvc.perform(get("/api/v1/tenant/me").with(from(address))).andReturn().getResponse().getStatus())
            .as("other paths are not counted").isEqualTo(401);
    }

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private Long logoFileId() {
        return jdbcTemplate.queryForObject("SELECT LOGO_FILE_ID FROM CORE_TENANT WHERE ID = ?", Long.class, id);
    }

    private static String logoUrl(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.data.logoUrl");
    }
}
