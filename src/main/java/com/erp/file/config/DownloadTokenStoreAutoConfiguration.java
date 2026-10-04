package com.erp.file.config;

import com.erp.file.service.DownloadTokenStore;
import com.erp.file.service.InMemoryDownloadTokenStore;
import com.erp.file.service.RedisDownloadTokenStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Picks the {@link DownloadTokenStore}: {@link RedisDownloadTokenStore} when a
 * Redis template bean exists, otherwise {@link InMemoryDownloadTokenStore}. The import
 * order is significant — Redis first, then the in-memory fallback guarded by
 * {@code @ConditionalOnMissingBean(DownloadTokenStore.class)}.
 *
 * <p>Registered in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * (and therefore excluded from component scanning by {@code @SpringBootApplication}'s
 * {@code AutoConfigurationExcludeFilter}) so its bean conditions are evaluated after Boot's Redis
 * auto-configuration has registered — or not registered — the template. A component-scanned
 * {@code @ConditionalOnBean} would be evaluated before any auto-configured bean exists.
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@Import({RedisDownloadTokenStore.class, InMemoryDownloadTokenStore.class})
public class DownloadTokenStoreAutoConfiguration {
}
