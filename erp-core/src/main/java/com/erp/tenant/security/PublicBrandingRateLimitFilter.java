package com.erp.tenant.security;

import com.erp.common.web.FilterErrorResponseWriter;
import com.erp.tenant.exception.TenantErrorCodes;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RULE-TENANT-022 (tenant-maturity E) — rate-limits the anonymous public branding per client address (IPv6 by /64) with
 * bucket4j buckets, <em>before</em> the tenant is resolved, so unknown and suspended codes count too; over it → 429
 * {@code TENANT_BRANDING_RATE_LIMITED} with {@code Retry-After}. Bounded: a bucket unused for {@code period} expires,
 * at most {@value #MAX_KEYS} keys (least recently used evicted). Per JVM. Not a {@code @Component}: built by
 * {@code ErpCoreSecurityAutoConfiguration}, first in the customer chain.
 */
public class PublicBrandingRateLimitFilter extends OncePerRequestFilter {

    /** Most keys held at once; beyond it the least recently used bucket is dropped. */
    static final int MAX_KEYS = 10_000;

    private final String pathPattern;
    private final int capacity;
    private final Duration period;
    private final MessageSource messageSource;
    private final LongSupplier nanoTime;
    private final LinkedHashMap<String, Entry> buckets = new LinkedHashMap<>(256, 0.75f, true);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private record Entry(Bucket bucket, long lastUsedNanos) {
    }

    /** The outcome of one request: allowed, or refused with the whole seconds to wait. */
    record Verdict(boolean allowed, long retryAfterSeconds) {
    }

    /**
     * @param pathPattern the public branding path ({@code /api/v1/public/tenants/*}{@code /branding}); other paths pass
     * @param capacity    requests per {@code period} and address (at least 1)
     * @param period      the refill period (one minute when null, zero or negative)
     */
    public PublicBrandingRateLimitFilter(String pathPattern, int capacity, Duration period, MessageSource messageSource) {
        this(pathPattern, capacity, period, messageSource, System::nanoTime);
    }

    /** As the public constructor, with an explicit clock (tests). */
    PublicBrandingRateLimitFilter(String pathPattern, int capacity, Duration period, MessageSource messageSource,
                                  LongSupplier nanoTime) {
        this.pathPattern = pathPattern;
        this.capacity = Math.max(1, capacity);
        this.period = period == null || period.isNegative() || period.isZero() ? Duration.ofMinutes(1) : period;
        this.messageSource = messageSource;
        this.nanoTime = nanoTime;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !pathMatcher.match(pathPattern, pathOf(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Verdict verdict = tryAcquire(request.getRemoteAddr());
        if (!verdict.allowed()) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(verdict.retryAfterSeconds()));
            FilterErrorResponseWriter.write(messageSource, request, response, HttpStatus.TOO_MANY_REQUESTS.value(),
                TenantErrorCodes.TENANT_BRANDING_RATE_LIMITED);
            return;
        }
        chain.doFilter(request, response);
    }

    /** Consumes one request of {@code address}'s budget (keyed by {@link #keyOf}). */
    Verdict tryAcquire(String address) {
        String key = keyOf(address);
        long now = nanoTime.getAsLong();
        Bucket bucket;
        synchronized (buckets) {
            evictExpired(now);
            Entry entry = buckets.get(key);
            bucket = entry != null ? entry.bucket() : newBucket();
            buckets.put(key, new Entry(bucket, now));
            if (buckets.size() > MAX_KEYS) {
                Iterator<String> eldest = buckets.keySet().iterator();
                eldest.next();
                eldest.remove();
            }
        }
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new Verdict(true, 0);
        }
        long nanos = probe.getNanosToWaitForRefill();
        return new Verdict(false, Math.max(1, (nanos + 999_999_999L) / 1_000_000_000L));
    }

    /** Keys held now (tests). */
    int size() {
        synchronized (buckets) {
            return buckets.size();
        }
    }

    /** The map is access-ordered, so the expired entries are at its head; a bucket unused for {@code period} is full. */
    private void evictExpired(long now) {
        long ttl = period.toNanos();
        Iterator<Map.Entry<String, Entry>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastUsedNanos() < ttl) {
                return;
            }
            it.remove();
        }
    }

    /** An IPv4 address as is; an IPv6 address by its /64 prefix; anything unparsable as given. */
    static String keyOf(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        try {
            InetAddress parsed = InetAddress.ofLiteral(address.strip());
            if (parsed instanceof Inet6Address) {
                byte[] prefix = new byte[8];
                System.arraycopy(parsed.getAddress(), 0, prefix, 0, 8);
                return HexFormat.of().formatHex(prefix) + "/64";
            }
            return parsed.getHostAddress();
        } catch (IllegalArgumentException e) {
            return address;
        }
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
