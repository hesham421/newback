package com.erp.cu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.LocalizedException;
import com.erp.cu.crossmodule.NoSuchSettingException;
import com.erp.cu.crossmodule.SettingsApi;
import com.erp.cu.exception.CuErrorCodes;
import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractIntegrationTest;
import com.erp.testsupport.StaffApiClient;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 09 — {@code SettingsApi} against PostgreSQL in the full application context: resolution
 * order (tenant override beats platform default; deactivated rows count as absent), not-found, typed
 * reads and type mismatch, and the cache: a value changed behind the API's back stays cached until a
 * write through the configuration CRUD API evicts it. Keys are unique per test (the cache is shared by
 * the test context).
 */
class SettingsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SettingsApi settings;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @LocalServerPort
    private int port;

    @Test
    void theCacheIsRegistered_inTheSimpleCacheManager() {
        assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
        assertThat(cacheManager.getCacheNames()).contains(SettingsApi.CACHE_NAME);
    }

    @Test
    void tenantOverride_beatsThePlatformDefault_otherTenantsGetTheDefault() {
        String key = StaffApiClient.unique("RES_");
        long tenantX = newTenant();
        long tenantY = newTenant();
        insert(null, key, "platform-default", true);
        insert(tenantX, key, "x-override", true);

        assertThat(TenantContext.callAs(tenantX, () -> settings.get(key))).isEqualTo("x-override");
        assertThat(TenantContext.callAs(tenantY, () -> settings.get(key))).isEqualTo("platform-default");
        assertThat(settings.get(key)).isEqualTo("platform-default");               // PLATFORM (test listener)
        assertThat(settings.get(key.toLowerCase())).isEqualTo("platform-default");  // keys are case-insensitive
    }

    @Test
    void aDeactivatedOverride_fallsBackToTheDefault_andADeactivatedDefault_isNotFound() {
        String key = StaffApiClient.unique("DEA_");
        long tenantX = newTenant();
        insert(null, key, "default", true);
        insert(tenantX, key, "inactive-override", false);
        assertThat(TenantContext.callAs(tenantX, () -> settings.get(key))).isEqualTo("default");

        String onlyInactive = StaffApiClient.unique("DEB_");
        insert(null, onlyInactive, "x", false);
        assertThat(settings.find(onlyInactive)).isEmpty();
    }

    @Test
    void aMissingSetting_isNoSuchSettingException_orTheGivenDefault() {
        String key = StaffApiClient.unique("MIS_");

        assertThatThrownBy(() -> settings.get(key)).isInstanceOfSatisfying(NoSuchSettingException.class, e -> {
            assertThat(e.getErrorCode()).isEqualTo(CuErrorCodes.SETTING_NOT_FOUND);
            assertThat(e.getKey()).isEqualTo(key);
        });
        assertThatThrownBy(() -> settings.get(key, Integer.class)).isInstanceOf(NoSuchSettingException.class);
        assertThat(settings.find(key)).isEmpty();
        assertThat(settings.getOrDefault(key, "fallback")).isEqualTo("fallback");
        assertThat(settings.getOrDefault(key, Duration.class, Duration.ofMinutes(1))).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void typedReads_andTypeMismatch() {
        String prefix = StaffApiClient.unique("TYP_");
        insert(null, prefix + "_INT", "42", true);
        insert(null, prefix + "_LONG", "9000000000", true);
        insert(null, prefix + "_BOOL", "true", true);
        insert(null, prefix + "_DEC", "0.15", true);
        insert(null, prefix + "_DUR", "PT15M", true);
        insert(null, prefix + "_TXT", "not a number", true);

        assertThat(settings.get(prefix + "_INT", Integer.class)).isEqualTo(42);
        assertThat(settings.get(prefix + "_LONG", Long.class)).isEqualTo(9_000_000_000L);
        assertThat(settings.get(prefix + "_BOOL", Boolean.class)).isTrue();
        assertThat(settings.get(prefix + "_DEC", BigDecimal.class)).isEqualByComparingTo("0.15");
        assertThat(settings.find(prefix + "_DUR", Duration.class)).contains(Duration.ofMinutes(15));
        assertThat(settings.getOrDefault(prefix + "_INT", Integer.class, 0)).isEqualTo(42);

        assertThatThrownBy(() -> settings.get(prefix + "_TXT", Integer.class))
            .isInstanceOfSatisfying(LocalizedException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(CuErrorCodes.SETTING_TYPE_MISMATCH));
        // a default covers a missing setting, never a malformed one
        assertThatThrownBy(() -> settings.getOrDefault(prefix + "_TXT", Integer.class, 7))
            .isInstanceOfSatisfying(LocalizedException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(CuErrorCodes.SETTING_TYPE_MISMATCH));
    }

    @Test
    void aCachedValue_survivesAChangeBehindTheApi_untilACrudWriteEvictsIt() {
        String key = StaffApiClient.unique("CCH_");
        insert(null, key, "v1", true);
        assertThat(settings.get(key)).isEqualTo("v1");
        assertThat(cacheManager.getCache(SettingsApi.CACHE_NAME).get("1:" + key)).isNotNull();

        // changed directly in the database: the API still answers from the cache
        jdbcTemplate.update("update cu_app_configuration set config_value = 'v2' where tenant_id is null and config_key = ?", key);
        assertThat(settings.get(key)).isEqualTo("v1");

        // a write through the CRUD API (platform default, scope=PLATFORM) evicts
        StaffApiClient http = new StaffApiClient(port);
        String token = http.token("PLATFORM", StaffApiClient.platformOperator(jdbcTemplate, passwordEncoder));
        HttpResponse<String> updated = http.put(token, "/api/v1/common/configurations/" + key + "?scope=PLATFORM",
            "{\"configValue\":\"v3\"}");
        assertThat(updated.statusCode()).as(updated.body()).isEqualTo(200);

        assertThat(cacheManager.getCache(SettingsApi.CACHE_NAME).get("1:" + key)).isNull();
        assertThat(settings.get(key)).isEqualTo("v3");
    }

    // ------------------------------------------------------------------------------------------

    private void insert(Long tenantId, String key, String value, boolean active) {
        jdbcTemplate.update("insert into cu_app_configuration (id, tenant_id, config_key, config_value, is_active_fl,"
                + " created_by, created_at) values (nextval('seq_cu_app_configuration'), ?, ?, ?, ?, 'test', now())",
            tenantId, key.toUpperCase(), value, active ? 1 : 0);
    }

    private long newTenant() {
        return jdbcTemplate.queryForObject(
            "insert into core_tenant (id, code, name_ar, name_en, status_code, created_by, created_at)"
                + " values (nextval('seq_core_tenant'), ?, 'ت', 'T', 'ACTIVE', 'test', now()) returning id",
            Long.class, StaffApiClient.unique("SET"));
    }
}
