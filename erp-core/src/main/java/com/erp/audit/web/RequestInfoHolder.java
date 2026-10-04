package com.erp.audit.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Captures the client IP and {@code User-Agent} of the current HTTP request for audit rows
 * (erp-core step 10). A servlet filter registered for every request (a filter bean, so Spring Boot
 * registers it ahead of the security chains); the values live in a thread-local for the duration of
 * the request and are removed in a {@code finally}.
 *
 * <p>The IP is {@link HttpServletRequest#getRemoteAddr()} — the same value the SEC services already
 * store in {@code SEC_AUDIT_LOG}; behind a proxy, configure the container's forwarded-header support
 * ({@code server.forward-headers-strategy}) rather than trusting {@code X-Forwarded-For} here. Code
 * running outside a request (jobs, asynchronous workers) sees {@link Optional#empty()}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestInfoHolder extends OncePerRequestFilter {

    /** Column widths of {@code CORE_AUDIT_EVENT.IP} / {@code USER_AGENT}. */
    static final int MAX_IP = 64;
    static final int MAX_USER_AGENT = 256;

    private static final ThreadLocal<RequestInfo> CURRENT = new ThreadLocal<>();

    /** The client of the current request. */
    public record RequestInfo(String ip, String userAgent) {
    }

    /** The current request's client, if this thread is serving a request. */
    public static Optional<RequestInfo> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RequestInfo previous = CURRENT.get();
        CURRENT.set(new RequestInfo(truncate(request.getRemoteAddr(), MAX_IP),
            truncate(request.getHeader(HttpHeaders.USER_AGENT), MAX_USER_AGENT)));
        try {
            chain.doFilter(request, response);
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
