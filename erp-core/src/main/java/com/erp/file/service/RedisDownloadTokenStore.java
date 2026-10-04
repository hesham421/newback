package com.erp.file.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed {@link DownloadTokenStore}: the token entry is a plain string key with a Redis TTL,
 * and single-use is enforced by {@code DEL} returning true only for the first consumer. Shared by
 * every application instance pointing at the same Redis.
 *
 * <p>Not a {@code @Component}: registered by {@link com.erp.file.config.DownloadTokenStoreAutoConfiguration}
 * (an auto-configuration ordered after Boot's Redis auto-configuration), so the
 * {@code @ConditionalOnBean} below sees the {@code StringRedisTemplate} bean when one exists.
 * This is the only class that touches Redis directly.
 */
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class RedisDownloadTokenStore implements DownloadTokenStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void put(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean consume(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }
}
