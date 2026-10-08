package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * tenant-maturity E — RULE-TENANT-022 over the real chain: {@code capacity} requests per client address and period,
 * unknown codes included (counted before the tenant filter answers 404), then 429 {@code TENANT_BRANDING_RATE_LIMITED}.
 * Own context (capacity 3, period 1 h: 127.0.0.1's bucket never throttles another class) with a two-connection pool,
 * closed after the class, so the cached contexts' pools still fit the database's 100 connections.
 */
@TestPropertySource(properties = {
    "erp.core.tenant.public-branding-rate-limit.capacity=3",
    "erp.core.tenant.public-branding-rate-limit.period=1h",
    "spring.datasource.hikari.maximum-pool-size=2",
    "spring.datasource.hikari.minimum-idle=1"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TenantPublicBrandingRateLimitIntegrationTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void theBudgetCountsEveryCode_thenAnswers429_andTheOtherPathsAreNotLimited() {
        TenantHttp http = new TenantHttp(port);
        String platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE,
            TenantHttp.platformOperator(jdbcTemplate, passwordEncoder));
        String code = TenantHttp.unique("RLB");
        http.provisionTenant(platformToken, code);
        String adminToken = http.token(code, "admin");

        assertThat(http.get(null, "/api/v1/public/tenants/" + code + "/branding").statusCode()).isEqualTo(200);
        assertThat(http.get(null, "/api/v1/public/tenants/NO_SUCH_" + code + "/branding").statusCode()).isEqualTo(404);
        assertThat(http.get(null, "/api/v1/public/tenants/" + code + "/branding").statusCode()).isEqualTo(200);

        for (String path : new String[] {code, "NO_SUCH_" + code}) {
            HttpResponse<String> limited = http.get(null, "/api/v1/public/tenants/" + path + "/branding");
            assertThat(limited.statusCode()).as(limited.body()).isEqualTo(429);
            assertThat(TenantHttp.errorCode(limited)).isEqualTo("TENANT_BRANDING_RATE_LIMITED");
            assertThat((String) JsonPath.read(limited.body(), "$.error.message")).startsWith("Too many tenant branding requests");
        }

        assertThat(http.get(adminToken, "/api/v1/tenant/me").statusCode()).as("not the public path").isEqualTo(200);
        assertThat(http.post(null, code, "/api/v1/public/customers/login",
            "{\"email\":\"nobody@shop.test\",\"password\":\"whatever-1\"}").statusCode()).as("other public paths").isEqualTo(401);
    }
}
