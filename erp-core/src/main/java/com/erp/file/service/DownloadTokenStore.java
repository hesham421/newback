package com.erp.file.service;

import java.time.Duration;

/**
 * Single-use download-token store behind API-FILE-002/003 (RULE-FILE-003). {@link FileService}
 * binds each issued token (by its hashed key) to the issuing username for the token TTL and
 * atomically consumes it on the first successful download.
 *
 * <p>Two implementations, selected by {@link com.erp.autoconfigure.DownloadTokenStoreAutoConfiguration}:
 * {@link RedisDownloadTokenStore} when a Redis template bean exists (shared across
 * instances), otherwise {@link InMemoryDownloadTokenStore} (single JVM — tests and Redis-less runs).
 */
public interface DownloadTokenStore {

    /** Stores {@code value} under {@code key}; the entry disappears after {@code ttl}. */
    void put(String key, String value, Duration ttl);

    /** The live value under {@code key}, or {@code null} when absent or expired. */
    String get(String key);

    /**
     * Removes {@code key}. Returns {@code true} only for the caller that actually removed a live
     * entry — a second consumer, or a key that is absent/expired, gets {@code false}.
     */
    boolean consume(String key);
}
