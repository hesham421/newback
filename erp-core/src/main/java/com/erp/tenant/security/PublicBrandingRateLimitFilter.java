package com.erp.tenant.security;

import com.erp.common.web.FilterErrorResponseWriter;
import com.erp.tenant.exception.TenantErrorCodes;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RULE-TENANT-022 (tenant-maturity E) — rate-limits the anonymous public branding per client address
 * ({@code getRemoteAddr()}) with bucket4j token buckets, <em>before</em> the tenant is resolved, so unknown and suspended
 * codes count too and the endpoint cannot enumerate tenant codes faster than the limit; over it → 429
 * {@code TENANT_BRANDING_RATE_LIMITED}. Per JVM; the map is cleared above {@value #MAX_KEYS} addresses. Not a
 * {@code @Component}: built by {@code ErpCoreSecurityAutoConfiguration}, first in the customer chain.
 */
public class PublicBrandingRateLimitFilter extends OncePerRequestFilter {

    /** Upper bound of distinct addresses held at once (the {@code LoginRateLimiter} precedent). */
    static final int MAX_KEYS = 10_000;

    private final String pathPattern;
    private final int capacity;
    private final Duration period;
    private final MessageSource messageSource;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * @param pathPattern the public branding path ({@code /api/v1/public/tenants/*}{@code /branding}); other paths pass
     * @param capacity    requests per {@code period} and address (at least 1)
     * @param period      the refill period (one minute when null, zero or negative)
     */
    public PublicBrandingRateLimitFilter(String pathPattern, int capacity, Duration period, MessageSource messageSource) {
        this.pathPattern = pathPattern;
        this.capacity = Math.max(1, capacity);
        this.period = period == null || period.isNegative() || period.isZero() ? Duration.ofMinutes(1) : period;
        this.messageSource = messageSource;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !pathMatcher.match(pathPattern, pathOf(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!tryAcquire(request.getRemoteAddr())) {
            FilterErrorResponseWriter.write(messageSource, request, response, HttpStatus.TOO_MANY_REQUESTS.value(),
                TenantErrorCodes.TENANT_BRANDING_RATE_LIMITED);
            return;
        }
        chain.doFilter(request, response);
    }

    /** Consumes one request of {@code address}'s budget; {@code false} when it is exhausted. */
    boolean tryAcquire(String address) {
        if (buckets.size() > MAX_KEYS) {
            buckets.clear();
        }
        return buckets.computeIfAbsent(address == null ? "" : address, key -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, period).build())
            .build();
    }

    private static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return StringUtils.hasLength(contextPath) && uri.startsWith(contextPath)
            ? uri.substring(contextPath.length()) : uri;
    }
}
