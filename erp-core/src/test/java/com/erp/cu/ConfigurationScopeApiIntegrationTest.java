package com.erp.cu;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.cu.crossmodule.SettingsApi;
import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractIntegrationTest;
import com.erp.testsupport.StaffApiClient;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 09 — the configuration CRUD API's {@code scope} parameter over HTTP: platform defaults
 * ({@code scope=PLATFORM}) are managed only by the PLATFORM tenant with {@code PLATFORM_SETTINGS_MANAGE}
 * (its super role holds it, other tenants' super roles never do), {@code scope=TENANT} (the default)
 * addresses the caller's own overrides, and {@code SettingsApi} resolves override → default per tenant.
 */
class ConfigurationScopeApiIntegrationTest extends AbstractIntegrationTest {

    private static final String CONFIGS = "/api/v1/common/configurations";

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private SettingsApi settings;

    private StaffApiClient http;
    private String platformToken;

    @BeforeEach
    void platformToken() {
        http = new StaffApiClient(port);
        platformToken = http.token("PLATFORM", StaffApiClient.platformOperator(jdbcTemplate, passwordEncoder));
    }

    @Test
    void platformScope_create201_get200_duplicate409_andTheTenantScopeIsSeparate() {
        String key = StaffApiClient.unique("PLT_");

        HttpResponse<String> created = http.post(platformToken, CONFIGS + "?scope=PLATFORM", body(key, "default"));
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat((String) JsonPath.read(created.body(), "$.data.scope")).isEqualTo("PLATFORM");
        assertThat(jdbcTemplate.queryForObject("select count(*) from cu_app_configuration where config_key = ?"
            + " and tenant_id is null", Integer.class, key)).isEqualTo(1);

        assertThat(http.get(platformToken, CONFIGS + "/" + key + "?scope=PLATFORM").statusCode()).isEqualTo(200);
        assertThat(http.get(platformToken, CONFIGS + "/" + key).statusCode()).as("no PLATFORM-tenant override").isEqualTo(404);

        HttpResponse<String> duplicate = http.post(platformToken, CONFIGS + "?scope=PLATFORM", body(key, "again"));
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(StaffApiClient.errorCode(duplicate)).isEqualTo("APP_CONFIGURATION_KEY_DUPLICATE");

        // the same key as the PLATFORM tenant's own override (scope TENANT, the default) is a different row
        HttpResponse<String> override = http.post(platformToken, CONFIGS, body(key, "platform-tenant-override"));
        assertThat(override.statusCode()).as(override.body()).isEqualTo(201);
        assertThat((String) JsonPath.read(override.body(), "$.data.scope")).isEqualTo("TENANT");
        assertThat(settings.get(key)).isEqualTo("platform-tenant-override");
    }

    @Test
    void anotherTenant_cannotTouchPlatformDefaults_butManagesItsOwnOverrides_andResolvesThem() {
        String withOverride = StaffApiClient.unique("OVR_");
        String defaultOnly = StaffApiClient.unique("DEF_");
        assertThat(http.post(platformToken, CONFIGS + "?scope=PLATFORM", body(withOverride, "default-1")).statusCode()).isEqualTo(201);
        assertThat(http.post(platformToken, CONFIGS + "?scope=PLATFORM", body(defaultOnly, "default-2")).statusCode()).isEqualTo(201);

        String tenantCode = StaffApiClient.unique("CFG");
        long tenantId = http.provisionTenant(platformToken, tenantCode);
        String tenantToken = http.token(tenantCode, StaffApiClient.TENANT_ADMIN);

        // platform defaults: forbidden for another tenant's administrator (no PLATFORM_SETTINGS_MANAGE)
        assertThat(http.post(tenantToken, CONFIGS + "?scope=PLATFORM", body(StaffApiClient.unique("NO_"), "x")).statusCode())
            .isEqualTo(403);
        assertThat(http.get(tenantToken, CONFIGS + "/" + withOverride + "?scope=PLATFORM").statusCode()).isEqualTo(403);
        assertThat(http.put(tenantToken, CONFIGS + "/" + withOverride + "?scope=PLATFORM", "{\"configValue\":\"hijack\"}")
            .statusCode()).isEqualTo(403);
        assertThat(http.delete(tenantToken, CONFIGS + "/" + withOverride + "?scope=PLATFORM").statusCode()).isEqualTo(403);

        // its own override (scope TENANT): allowed
        HttpResponse<String> override = http.post(tenantToken, CONFIGS, body(withOverride, "tenant-value"));
        assertThat(override.statusCode()).as(override.body()).isEqualTo(201);

        // its search lists only its own rows — no platform default, no other tenant's override
        HttpResponse<String> search = http.post(tenantToken, CONFIGS + "/search", "{\"size\":1000}");
        assertThat(search.statusCode()).isEqualTo(200);
        List<String> keys = JsonPath.read(search.body(), "$.data.content[*].configKey");
        assertThat(keys).containsExactly(withOverride);

        // resolution for that tenant: its override, else the platform default
        assertThat(TenantContext.callAs(tenantId, () -> settings.get(withOverride))).isEqualTo("tenant-value");
        assertThat(TenantContext.callAs(tenantId, () -> settings.get(defaultOnly))).isEqualTo("default-2");
        assertThat(settings.get(withOverride)).isEqualTo("default-1");   // PLATFORM still sees the default
    }

    @Test
    void anUnknownScope_is400() {
        HttpResponse<String> response = http.post(platformToken, CONFIGS + "/search?scope=GLOBAL", "{}");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    private static String body(String key, String value) {
        return "{\"configKey\":\"" + key + "\",\"configValue\":\"" + value + "\"}";
    }
}
