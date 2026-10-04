package com.erp.file.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * In-process {@link DownloadTokenStore} used when no Redis is configured (tests, single-instance
 * runs). Entries live in a {@link ConcurrentHashMap} with an absolute expiry; expired entries are
 * treated as absent and purged lazily on access and on every {@link #put}. Tokens are not shared
 * between JVMs — a multi-instance deployment must provide Redis.
 *
 * <p>Not a {@code @Component}: registered by {@link com.erp.file.config.DownloadTokenStoreAutoConfiguration}
 * after {@link RedisDownloadTokenStore}, so it only applies when no other store was defined.
 */
@ConditionalOnMissingBean(DownloadTokenStore.class)
public class InMemoryDownloadTokenStore implements DownloadTokenStore {

    private record Entry(String value, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryDownloadTokenStore() {
        this(Clock.systemUTC());
    }

    InMemoryDownloadTokenStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        Instant now = clock.instant();
        entries.values().removeIf(entry -> entry.isExpired(now));
        entries.put(key, new Entry(value, now.plus(ttl)));
    }

    @Override
    public String get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(clock.instant())) {
            entries.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    @Override
    public boolean consume(String key) {
        // remove() is atomic: exactly one concurrent caller receives the entry.
        Entry removed = entries.remove(key);
        return removed != null && !removed.isExpired(clock.instant());
    }
}
