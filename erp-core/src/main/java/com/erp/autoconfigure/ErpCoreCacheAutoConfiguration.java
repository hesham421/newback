package com.erp.autoconfigure;

import com.erp.cu.crossmodule.SettingsApi;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * erp-core step 09 — Spring caching for the core's one cache, {@value SettingsApi#CACHE_NAME}
 * (resolved settings, keyed {@code <tenantId>:<KEY>}).
 *
 * <ul>
 *   <li>{@code @EnableCaching}, so Boot's {@link CacheAutoConfiguration} provides the cache manager the
 *       application configures ({@code spring.cache.type}: {@code simple}, {@code redis}, ...; with
 *       {@code none} the settings are simply read uncached). Its advice runs <em>outside</em> the
 *       transaction advice ({@code order = LOWEST_PRECEDENCE - 1}), so a write's {@code @CacheEvict} happens
 *       after the write committed and a cache hit opens no transaction.</li>
 *   <li>For the {@code simple} cache manager, the cache name is registered: added to the fixed names when
 *       {@code spring.cache.cache-names} lists any (otherwise {@code ConcurrentMapCacheManager} would refuse
 *       an unknown cache), or created up front when the manager creates caches on demand — without
 *       switching an on-demand manager to fixed names, which would break the application's own caches.</li>
 * </ul>
 */
@AutoConfiguration(before = CacheAutoConfiguration.class)
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class ErpCoreCacheAutoConfiguration {

    @Bean
    public CacheManagerCustomizer<ConcurrentMapCacheManager> erpCoreSettingsCacheCustomizer() {
        return manager -> {
            Collection<String> names = manager.getCacheNames();
            if (names.isEmpty()) {
                manager.getCache(SettingsApi.CACHE_NAME);   // on-demand manager: create it now, stay dynamic
            } else if (!names.contains(SettingsApi.CACHE_NAME)) {
                LinkedHashSet<String> all = new LinkedHashSet<>(names);
                all.add(SettingsApi.CACHE_NAME);
                manager.setCacheNames(all);
            }
        };
    }
}
