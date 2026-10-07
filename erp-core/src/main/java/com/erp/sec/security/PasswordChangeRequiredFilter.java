package com.erp.sec.security;

import com.erp.sec.exception.SecErrorCodes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RULE-SEC-059 (tenant-maturity D, ADR-SEC-063) — while the authenticated STAFF caller's
 * {@code PASSWORD_CHANGE_REQUIRED_FL} is set ({@link AuthRealm#passwordChangeRequired()}), every request
 * except {@code GET /api/v1/sec/me}, {@code PUT /api/v1/sec/me/password}, {@code POST /api/v1/sec/auth/logout}
 * and the chain's public paths answers 403 {@code SEC-403-PASSWORD-CHANGE-REQUIRED}. Placed after
 * {@link RealmEnforcementFilter} in the staff chain; not a {@code @Component}.
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    /** The three calls a caller with a pending forced change may still make. */
    static final List<Exemption> EXEMPTIONS = List.of(
        new Exemption(HttpMethod.GET, "/api/v1/sec/me"),
        new Exemption(HttpMethod.PUT, "/api/v1/sec/me/password"),
        new Exemption(HttpMethod.POST, "/api/v1/sec/auth/logout"));

    record Exemption(HttpMethod method, String path) {
    }

    private final List<String> publicPaths;
    private final SecSecurityErrorHandler errorHandler;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PasswordChangeRequiredFilter(List<String> publicPaths, SecSecurityErrorHandler errorHandler) {
        this.publicPaths = List.copyOf(publicPaths);
        this.errorHandler = errorHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (AuthRealm.passwordChangeRequired(SecurityContextHolder.getContext().getAuthentication())
            && !isAllowed(request)) {
            errorHandler.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                SecErrorCodes.SEC_403_PASSWORD_CHANGE_REQUIRED);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowed(HttpServletRequest request) {
        String path = pathOf(request);
        if (publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            return true;
        }
        return EXEMPTIONS.stream().anyMatch(exemption ->
            exemption.method().matches(request.getMethod()) && exemption.path().equals(trimSlash(path)));
    }

    private static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return StringUtils.hasLength(contextPath) && uri.startsWith(contextPath)
            ? uri.substring(contextPath.length()) : uri;
    }

    private static String trimSlash(String path) {
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
