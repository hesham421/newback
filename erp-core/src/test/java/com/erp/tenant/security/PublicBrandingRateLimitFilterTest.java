package com.erp.tenant.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.tenant.exception.TenantErrorCodes;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** tenant-maturity E — RULE-TENANT-022: per-address budget (IPv6 by /64), counted for every code, bounded, Retry-After. */
class PublicBrandingRateLimitFilterTest {

    private static final String PATTERN = "/api/v1/public/tenants/*/branding";

    private static PublicBrandingRateLimitFilter filter(int capacity) {
        return new PublicBrandingRateLimitFilter(PATTERN, capacity, Duration.ofHours(1), new StaticMessageSource());
    }

    private static MockHttpServletResponse call(PublicBrandingRateLimitFilter filter, String path, String address,
                                                AtomicInteger passed) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                passed.incrementAndGet();
            }
        });
        return response;
    }

    @Test
    void capacityRequestsPass_thenEveryCode_is429WithRetryAfter_whileAnotherAddressIsServed() throws Exception {
        PublicBrandingRateLimitFilter filter = filter(3);
        AtomicInteger passed = new AtomicInteger();
        for (String code : new String[] {"ACME", "NOPE", "acme"}) {
            assertThat(call(filter, "/api/v1/public/tenants/" + code + "/branding", "10.0.0.1", passed).getStatus())
                .isEqualTo(200);
        }
        MockHttpServletResponse limited = call(filter, "/api/v1/public/tenants/OTHER/branding", "10.0.0.1", passed);
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getContentAsString()).contains("\"code\":\"" + TenantErrorCodes.TENANT_BRANDING_RATE_LIMITED + "\"");
        assertThat(Long.parseLong(limited.getHeader("Retry-After"))).as("one of three per hour refills in 20 min")
            .isBetween(1_190L, 1_200L);
        assertThat(passed).hasValue(3);

        assertThat(call(filter, "/api/v1/public/tenants/ACME/branding", "10.0.0.2", passed).getStatus()).isEqualTo(200);
        assertThat(passed).hasValue(4);
    }

    @Test
    void otherPaths_areNeverCounted() throws Exception {
        PublicBrandingRateLimitFilter filter = filter(1);
        AtomicInteger passed = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            assertThat(call(filter, "/api/v1/public/files/ACME/abc", "10.0.0.3", passed).getStatus()).isEqualTo(200);
            assertThat(call(filter, "/api/v1/public/customers/login", "10.0.0.3", passed).getStatus()).isEqualTo(200);
        }
        assertThat(passed).hasValue(10);
        assertThat(call(filter, "/api/v1/public/tenants/ACME/branding", "10.0.0.3", passed).getStatus()).isEqualTo(200);
        assertThat(call(filter, "/api/v1/public/tenants/ACME/branding", "10.0.0.3", passed).getStatus()).isEqualTo(429);
    }

    @Test
    void ipv6AddressesShareTheBudgetOfTheirSlash64_ipv4AddressesAreTheirOwnKey() {
        assertThat(PublicBrandingRateLimitFilter.keyOf("2001:db8:1:2::1"))
            .isEqualTo(PublicBrandingRateLimitFilter.keyOf("2001:0db8:0001:0002:ffff:eeee:dddd:cccc"))
            .isEqualTo("20010db800010002/64");
        assertThat(PublicBrandingRateLimitFilter.keyOf("2001:db8:1:3::1")).isEqualTo("20010db800010003/64");
        assertThat(PublicBrandingRateLimitFilter.keyOf("0:0:0:0:0:0:0:1")).isEqualTo("0000000000000000/64");
        assertThat(PublicBrandingRateLimitFilter.keyOf("10.1.2.3")).isEqualTo("10.1.2.3");
        assertThat(PublicBrandingRateLimitFilter.keyOf("::ffff:10.1.2.3")).as("IPv4-mapped").isEqualTo("10.1.2.3");
        assertThat(PublicBrandingRateLimitFilter.keyOf("not-an-address")).isEqualTo("not-an-address");

        PublicBrandingRateLimitFilter filter = filter(1);
        assertThat(filter.tryAcquire("2001:db8:1:2::1").allowed()).isTrue();
        assertThat(filter.tryAcquire("2001:db8:1:2::abcd").allowed()).as("same /64").isFalse();
        assertThat(filter.tryAcquire("2001:db8:1:3::1").allowed()).as("another /64").isTrue();
    }

    @Test
    void anUnusedBucketExpiresAfterThePeriod_andTheKeysAreBounded() {
        AtomicLong now = new AtomicLong();
        PublicBrandingRateLimitFilter filter = new PublicBrandingRateLimitFilter(PATTERN, 1, Duration.ofMinutes(1),
            new StaticMessageSource(), now::get);
        assertThat(filter.tryAcquire("10.0.0.5").allowed()).isTrue();
        now.addAndGet(Duration.ofSeconds(30).toNanos());
        assertThat(filter.tryAcquire("10.0.0.6").allowed()).isTrue();
        assertThat(filter.size()).isEqualTo(2);
        now.addAndGet(Duration.ofSeconds(31).toNanos());
        filter.tryAcquire("10.0.0.7");
        assertThat(filter.size()).as("10.0.0.5 unused for a period: expired").isEqualTo(2);

        for (int i = 0; i < PublicBrandingRateLimitFilter.MAX_KEYS + 50; i++) {
            filter.tryAcquire("10.1." + (i / 256) + "." + (i % 256));
        }
        assertThat(filter.size()).isEqualTo(PublicBrandingRateLimitFilter.MAX_KEYS);
    }

    @Test
    void aNonPositiveCapacityOrPeriod_fallsBackToSafeDefaults() {
        PublicBrandingRateLimitFilter filter = new PublicBrandingRateLimitFilter(PATTERN, 0, Duration.ZERO,
            new StaticMessageSource());
        assertThat(filter.tryAcquire("10.0.0.4").allowed()).isTrue();
        assertThat(filter.tryAcquire("10.0.0.4").allowed()).isFalse();
    }
}
