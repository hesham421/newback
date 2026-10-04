package com.erp.tenant.security;

import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import com.erp.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the tenant of a web request (erp-core step 05) and refuses requests of unknown or
 * suspended tenants. It runs in the core security chain right after {@code JwtAuthenticationFilter}
 * and before authorization:
 * <ol>
 *   <li><b>Token</b> — when the JWT filter has authenticated the caller it has already set
 *       {@link TenantContext} from the token's {@code tid} claim; this filter only checks that the
 *       tenant still exists and is ACTIVE (a suspended tenant's tokens stop working at once).</li>
 *   <li><b>Header</b> — otherwise {@code X-Tenant-Code} is resolved (trimmed, upper-cased) via
 *       {@code CORE_TENANT}: unknown → 404 {@code TENANT_NOT_FOUND}, suspended → 403
 *       {@code TENANT_SUSPENDED}, else it becomes the request's tenant.</li>
 *   <li><b>Neither</b> — a path in {@code erp.core.tenant.exempt-paths} proceeds without a tenant; a
 *       public (unauthenticated) path is refused 400 {@code TENANT_REQUIRED}; any other path proceeds
 *       so the authorization layer answers 401 for the missing token.</li>
 * </ol>
 * <b>Path tenant</b> (erp-core step 07) — before all of the above, a path matching
 * {@code erp.core.tenant.path-tenant-paths} (default: the public file URLs
 * {@code /api/v1/public/files/{tenantCode}/**}) takes its tenant from the {@code {tenantCode}} path
 * variable, resolved like the header (unknown → 404, suspended → 403). The path's tenant always wins
 * there: a caller authenticated in a different tenant is treated as anonymous (its authentication is
 * dropped), so a token can never act in a foreign tenant through such a path.
 *
 * <p>The tenant this filter sets is removed in a {@code finally}. Not a {@code @Component}: built by
 * {@code ErpCoreSecurityAutoConfiguration} and registered only inside the security chain.
 *
 * <p>{@code CORE_TENANT} is global, but a Hibernate session always needs a tenant, so the lookups run
 * as the PLATFORM tenant. Repository use without {@code @PreAuthorize} is deliberate: this is
 * pre-authentication infrastructure, the same precedent as {@code JwtAuthenticationFilter}.
 */
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final Supplier<TenantRepository> tenantRepository;
    private final MessageSource messageSource;
    private final List<String> publicPaths;
    private final List<String> exemptPaths;
    private final List<String> pathTenantPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantResolutionFilter(Supplier<TenantRepository> tenantRepository, MessageSource messageSource,
                                  List<String> publicPaths, List<String> exemptPaths) {
        this(tenantRepository, messageSource, publicPaths, exemptPaths, List.of());
    }

    /**
     * @param pathTenantPaths patterns with a {@code {tenantCode}} variable whose tenant comes from the
     *                        path (erp-core step 07)
     */
    public TenantResolutionFilter(Supplier<TenantRepository> tenantRepository, MessageSource messageSource,
                                  List<String> publicPaths, List<String> exemptPaths, List<String> pathTenantPaths) {
        this.tenantRepository = tenantRepository;
        this.messageSource = messageSource;
        this.publicPaths = List.copyOf(publicPaths);
        this.exemptPaths = List.copyOf(exemptPaths);
        this.pathTenantPaths = List.copyOf(pathTenantPaths);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String pathTenantCode = pathTenantCode(pathOf(request));
        if (pathTenantCode != null) {
            resolveFromPath(pathTenantCode, request, response, chain);
            return;
        }

        Long tokenTenant = TenantContext.current();
        if (tokenTenant != null) {
            Optional<Tenant> tenant = findTenant(() -> tenantRepository.get().findById(tokenTenant));
            if (tenant.isEmpty() || !isActive(tenant.get())) {
                SecurityContextHolder.clearContext();
                reject(request, response, HttpServletResponse.SC_FORBIDDEN, TenantErrorCodes.TENANT_SUSPENDED);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        String code = request.getHeader(TenantConstants.TENANT_CODE_HEADER);
        if (StringUtils.hasText(code)) {
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            Optional<Tenant> tenant = findTenant(() -> tenantRepository.get().findByCode(normalized));
            if (tenant.isEmpty()) {
                reject(request, response, HttpServletResponse.SC_NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND);
                return;
            }
            if (!isActive(tenant.get())) {
                reject(request, response, HttpServletResponse.SC_FORBIDDEN, TenantErrorCodes.TENANT_SUSPENDED);
                return;
            }
            TenantContext.set(tenant.get().getId());
            try {
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
            return;
        }

        String path = pathOf(request);
        if (!matchesAny(exemptPaths, path) && matchesAny(publicPaths, path)) {
            reject(request, response, HttpServletResponse.SC_BAD_REQUEST, TenantErrorCodes.TENANT_REQUIRED);
            return;
        }
        chain.doFilter(request, response);
    }

    /** erp-core step 07 — the tenant of a path-tenant path is the one named in the path. */
    private void resolveFromPath(String code, HttpServletRequest request, HttpServletResponse response,
                                 FilterChain chain) throws ServletException, IOException {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        Optional<Tenant> tenant = findTenant(() -> tenantRepository.get().findByCode(normalized));
        if (tenant.isEmpty()) {
            reject(request, response, HttpServletResponse.SC_NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND);
            return;
        }
        if (!isActive(tenant.get())) {
            reject(request, response, HttpServletResponse.SC_FORBIDDEN, TenantErrorCodes.TENANT_SUSPENDED);
            return;
        }
        Long previous = TenantContext.current();
        if (previous != null && !previous.equals(tenant.get().getId())) {
            // A token of another tenant never authenticates inside this tenant.
            SecurityContextHolder.clearContext();
        }
        TenantContext.set(tenant.get().getId());
        try {
            chain.doFilter(request, response);
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }

    /** The {@code {tenantCode}} variable of the first matching path-tenant pattern, or {@code null}. */
    private String pathTenantCode(String path) {
        for (String pattern : pathTenantPaths) {
            if (pathMatcher.match(pattern, path)) {
                String code = pathMatcher.extractUriTemplateVariables(pattern, path).get("tenantCode");
                if (StringUtils.hasText(code)) {
                    return code;
                }
            }
        }
        return null;
    }

    private static Optional<Tenant> findTenant(Supplier<Optional<Tenant>> lookup) {
        return TenantContext.callAs(TenantConstants.PLATFORM_TENANT_ID, lookup);
    }

    private static boolean isActive(Tenant tenant) {
        return TenantConstants.STATUS_ACTIVE.equals(tenant.getStatusCode());
    }

    private boolean matchesAny(List<String> patterns, String path) {
        return patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return StringUtils.hasLength(contextPath) && uri.startsWith(contextPath)
            ? uri.substring(contextPath.length()) : uri;
    }

    /**
     * Same envelope as {@code SecSecurityErrorHandler}, written by hand: this runs before the
     * dispatcher, so neither {@code GlobalExceptionHandler} nor Jackson's converters are involved.
     */
    private void reject(HttpServletRequest request, HttpServletResponse response, int status,
                        String errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
            "{\"success\":false,\"error\":{\"code\":\"" + errorCode
                + "\",\"message\":\"" + escape(resolve(errorCode, request))
                + "\"},\"timestamp\":\"" + Instant.now() + "\"}");
    }

    private String resolve(String errorCode, HttpServletRequest request) {
        try {
            return messageSource.getMessage(errorCode, null, request.getLocale());
        } catch (NoSuchMessageException e) {
            return errorCode;
        }
    }

    private static String escape(String message) {
        return message.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
