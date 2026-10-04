package com.erp.sec.security;

import com.erp.autoconfigure.ErpCoreProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * erp-core step 06 — in-memory brute-force protection for logins (bucket4j token buckets). One bucket
 * per key; the key is {@code tenantId:realm:username} (step 05's rule: a login limiter is keyed by
 * tenant and username — and since step 06 by realm), so tenants and realms never throttle each other.
 * Every attempt consumes a token; when the bucket is empty the attempt is refused before the password
 * is checked. Limits come from {@code erp.core.security.customer-login-rate-limit}.
 *
 * <p>Per JVM: a clustered deployment gets one budget per node. The bucket map is cleared when it grows
 * past {@link #MAX_KEYS}, so an attacker cycling usernames cannot exhaust memory.
 */
@Component
public class LoginRateLimiter {

    /** Upper bound of distinct keys held at once. */
    static final int MAX_KEYS = 10_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final Duration period;

    public LoginRateLimiter(ErpCoreProperties properties) {
        ErpCoreProperties.LoginRateLimit limit = properties.getSecurity().getCustomerLoginRateLimit();
        this.capacity = Math.max(1, limit.getCapacity());
        this.period = limit.getPeriod() == null || limit.getPeriod().isNegative() || limit.getPeriod().isZero()
            ? Duration.ofMinutes(1) : limit.getPeriod();
    }

    /** {@code tenantId:realm:username}. */
    public static String key(Long tenantId, String realm, String username) {
        return tenantId + ":" + realm + ":" + (username == null ? "" : username.trim().toLowerCase());
    }

    /** Consumes one attempt for {@code key}; {@code false} when the key's budget is exhausted. */
    public boolean tryAcquire(String key) {
        if (buckets.size() > MAX_KEYS) {
            buckets.clear();
        }
        return buckets.computeIfAbsent(key, k -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, period).build())
            .build();
    }
}
