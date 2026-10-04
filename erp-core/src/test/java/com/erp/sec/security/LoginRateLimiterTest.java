package com.erp.sec.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.autoconfigure.ErpCoreProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** erp-core step 06 — the bucket4j login limiter, keyed tenant:realm:username. */
class LoginRateLimiterTest {

    private static LoginRateLimiter limiter(int capacity) {
        ErpCoreProperties properties = new ErpCoreProperties();
        properties.getSecurity().getCustomerLoginRateLimit().setCapacity(capacity);
        properties.getSecurity().getCustomerLoginRateLimit().setPeriod(Duration.ofHours(1));
        return new LoginRateLimiter(properties);
    }

    @Test
    void allowsCapacityAttempts_thenRefuses() {
        LoginRateLimiter limiter = limiter(3);
        String key = LoginRateLimiter.key(7L, "CUSTOMER", "Jane@Shop.test");
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isFalse();
    }

    @Test
    void keysOfOtherTenantsRealmsAndUsers_areIndependent() {
        LoginRateLimiter limiter = limiter(1);
        assertThat(limiter.tryAcquire(LoginRateLimiter.key(1L, "CUSTOMER", "a"))).isTrue();
        assertThat(limiter.tryAcquire(LoginRateLimiter.key(1L, "CUSTOMER", "a"))).isFalse();
        assertThat(limiter.tryAcquire(LoginRateLimiter.key(2L, "CUSTOMER", "a"))).isTrue();
        assertThat(limiter.tryAcquire(LoginRateLimiter.key(1L, "STAFF", "a"))).isTrue();
        assertThat(limiter.tryAcquire(LoginRateLimiter.key(1L, "CUSTOMER", "b"))).isTrue();
    }

    @Test
    void theKey_isTenantRealmAndCaseInsensitiveUsername() {
        assertThat(LoginRateLimiter.key(5L, "CUSTOMER", " Jane@Shop.TEST ")).isEqualTo("5:CUSTOMER:jane@shop.test");
    }
}
