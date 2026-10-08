package com.erp.tenant.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.tenant.exception.TenantErrorCodes;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** tenant-maturity E — RULE-TENANT-022: the public branding's per-address budget, counted for every code. */
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
    void capacityRequestsPass_thenEveryCode_unknownOnesIncluded_is429_whileAnotherAddressIsServed() throws Exception {
        PublicBrandingRateLimitFilter filter = filter(3);
        AtomicInteger passed = new AtomicInteger();
        for (String code : new String[] {"ACME", "NOPE", "acme"}) {
            assertThat(call(filter, "/api/v1/public/tenants/" + code + "/branding", "10.0.0.1", passed).getStatus())
                .isEqualTo(200);
        }
        MockHttpServletResponse limited = call(filter, "/api/v1/public/tenants/OTHER/branding", "10.0.0.1", passed);
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getContentAsString()).contains("\"code\":\"" + TenantErrorCodes.TENANT_BRANDING_RATE_LIMITED + "\"");
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
    void aNonPositiveCapacityOrPeriod_fallsBackToSafeDefaults() {
        PublicBrandingRateLimitFilter filter = new PublicBrandingRateLimitFilter(PATTERN, 0, Duration.ZERO,
            new StaticMessageSource());
        assertThat(filter.tryAcquire("10.0.0.4")).isTrue();
        assertThat(filter.tryAcquire("10.0.0.4")).isFalse();
    }
}
