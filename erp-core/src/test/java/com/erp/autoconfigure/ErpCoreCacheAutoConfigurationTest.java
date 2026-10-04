package com.erp.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.cu.crossmodule.SettingsApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;

/**
 * erp-core step 09, task 4 — {@link ErpCoreCacheAutoConfiguration} enables caching and registers
 * {@code erpCoreSettings} in the {@code simple} cache manager without breaking the application's caches.
 */
class ErpCoreCacheAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class, ErpCoreCacheAutoConfiguration.class));

    @Test
    void simple_onDemand_registersTheSettingsCache_andStaysOnDemand() {
        runner.withPropertyValues("spring.cache.type=simple").run(context -> {
            assertThat(context).hasNotFailed();
            CacheManager manager = context.getBean(CacheManager.class);
            assertThat(manager).isInstanceOf(ConcurrentMapCacheManager.class);
            assertThat(manager.getCacheNames()).containsExactly(SettingsApi.CACHE_NAME);
            assertThat(manager.getCache("applicationCache")).as("app caches are still created on demand").isNotNull();
        });
    }

    @Test
    void simple_withFixedCacheNames_addsTheSettingsCacheToThem() {
        runner.withPropertyValues("spring.cache.type=simple", "spring.cache.cache-names=orders,customers").run(context -> {
            CacheManager manager = context.getBean(CacheManager.class);
            assertThat(manager.getCacheNames()).containsExactlyInAnyOrder("orders", "customers", SettingsApi.CACHE_NAME);
            assertThat(manager.getCache(SettingsApi.CACHE_NAME)).isNotNull();
        });
    }

    @Test
    void cacheTypeNone_isRespected() {
        runner.withPropertyValues("spring.cache.type=none").run(context ->
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(NoOpCacheManager.class));
    }
}
