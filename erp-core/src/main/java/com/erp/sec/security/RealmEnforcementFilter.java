package com.erp.sec.security;

import com.erp.sec.exception.SecErrorCodes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * erp-core step 06 — keeps the two auth realms apart. Each core security chain carries one instance
 * configured with the realm it serves ({@code STAFF} for {@code erpCoreSecurityFilterChain},
 * {@code CUSTOMER} for {@code erpCoreCustomerSecurityFilterChain}). Placed after
 * {@link JwtAuthenticationFilter}, it compares the authenticated caller's realm — the token's
 * {@code realm} claim, attached as {@link AuthRealm} details — with the chain's realm and refuses a
 * mismatch with 403 {@code REALM_MISMATCH} before authorization runs. The check is by claim, not by
 * path convention: a staff token never reaches a customer endpoint and vice versa, whatever the path.
 *
 * <p>The chain's public (permit-all) paths are not checked: they need no identity, so a stray token of
 * the other realm on, e.g., the login endpoint is simply irrelevant there. Neither are the chain's realm-neutral
 * {@code GET} paths (tenant-maturity E: {@code GET /api/v1/tenant/me}), which still need an authenticated caller.
 *
 * <p>Not a {@code @Component}: built by {@code ErpCoreSecurityAutoConfiguration} per chain, so it is
 * never registered as a servlet filter of its own.
 */
public class RealmEnforcementFilter extends OncePerRequestFilter {

    private final String realm;
    private final List<String> publicPaths;
    private final List<String> realmNeutralGetPaths;
    private final SecSecurityErrorHandler errorHandler;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RealmEnforcementFilter(String realm, List<String> publicPaths, SecSecurityErrorHandler errorHandler) {
        this(realm, publicPaths, List.of(), errorHandler);
    }

    /** @param realmNeutralGetPaths paths a token of either realm may {@code GET} (any other method is checked) */
    public RealmEnforcementFilter(String realm, List<String> publicPaths, List<String> realmNeutralGetPaths,
                                  SecSecurityErrorHandler errorHandler) {
        this.realm = realm;
        this.publicPaths = List.copyOf(publicPaths);
        this.realmNeutralGetPaths = List.copyOf(realmNeutralGetPaths);
        this.errorHandler = errorHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String callerRealm = AuthRealm.of(authentication);
        if (callerRealm != null && !realm.equals(callerRealm) && !isPublic(request) && !isRealmNeutralGet(request)) {
            SecurityContextHolder.clearContext();
            errorHandler.write(request, response, HttpServletResponse.SC_FORBIDDEN, SecErrorCodes.REALM_MISMATCH);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = pathOf(request);
        return publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isRealmNeutralGet(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) {
            return false;
        }
        String path = pathOf(request);
        return realmNeutralGetPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return StringUtils.hasLength(contextPath) && uri.startsWith(contextPath)
            ? uri.substring(contextPath.length()) : uri;
    }
}
