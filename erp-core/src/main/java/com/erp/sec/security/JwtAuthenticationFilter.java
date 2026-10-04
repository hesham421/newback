package com.erp.sec.security;

import com.erp.sec.entity.ActiveSession;
import com.erp.sec.entity.User;
import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.service.MenuService;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * REQ-SEC-033's single entry point: it authenticates the bearer token and installs the caller's
 * effective permission codes as authorities, so every {@code @PreAuthorize} downstream is a plain
 * set lookup. An absent or rejected token leaves the context anonymous — the authorization layer,
 * not this filter, decides what that means. See
 * governance/project-artifacts/sec-implementation-notes.md for the authority model.
 *
 * <p>Tenant (erp-core step 05): a valid token's {@code tid} claim becomes the request's
 * {@link TenantContext} <em>before</em> the user is looked up — the lookup is itself tenant-filtered
 * and usernames are unique per tenant only — and stays set for the rest of the request; the
 * previous value (none, on a real request) is restored in a {@code finally} after the chain. A token without {@code tid}, or one whose user or
 * session is rejected, leaves no tenant behind, so the tenant module's {@code TenantResolutionFilter}
 * (next in the chain) can fall back to the {@code X-Tenant-Code} header.
 *
 * <p>Not a {@code @Component}: exposed as a bean by
 * {@code com.erp.autoconfigure.ErpCoreSecurityAutoConfiguration}, which also places it in the core
 * security filter chain.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /** USER_STATUS code a caller must carry to authenticate (REQ-SEC-001, CHK_SEC_USER_STATUS). */
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final JwtTokenValidator tokenValidator;
    private final UserRepository userRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final MenuService menuService;

    /**
     * {@code @Lazy} keeps the JPA and method-security infrastructure out of the security-config
     * bootstrap: this filter is built while the filter chain is, long before those are ready.
     */
    public JwtAuthenticationFilter(JwtTokenValidator tokenValidator,
                                   @Lazy UserRepository userRepository,
                                   @Lazy ActiveSessionRepository activeSessionRepository,
                                   @Lazy MenuService menuService) {
        this.tokenValidator = tokenValidator;
        this.userRepository = userRepository;
        this.activeSessionRepository = activeSessionRepository;
        this.menuService = menuService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Long previousTenant = TenantContext.current();
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
                boolean authenticated = tokenValidator.parse(header.substring(BEARER_PREFIX.length()))
                    .map(this::authenticate)
                    .orElse(false);
                if (!authenticated) {
                    restoreTenant(previousTenant);
                }
            }
            chain.doFilter(request, response);
        } finally {
            restoreTenant(previousTenant);
        }
    }

    private static void restoreTenant(Long tenantId) {
        if (tenantId == null) {
            TenantContext.clear();
        } else {
            TenantContext.set(tenantId);
        }
    }

    /**
     * REQ-SEC-028: the token's {@code jti} is the session's {@code tokenRef}, so a terminated or
     * unknown session makes the token unusable for every subsequent request, which is what
     * API-SEC-026 relies on. A non-ACTIVE or deactivated user is rejected for the same reason.
     *
     * @return whether the caller was authenticated. The token's tenant is set as the
     *         {@link TenantContext} before the lookups; on {@code false} the caller restores the
     *         previous tenant (none, on a real request)
     */
    private boolean authenticate(Claims claims) {
        String username = claims.getSubject();
        String tokenRef = claims.getId();
        Long tenantId = tenantIdOf(claims);
        if (username == null || username.isBlank() || tokenRef == null || tokenRef.isBlank()
            || tenantId == null) {
            return false;
        }

        TenantContext.set(tenantId);
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null || !STATUS_ACTIVE.equals(user.getStatusCode())
                || !Boolean.TRUE.equals(user.getIsActiveFl())) {
                return false;
            }

            ActiveSession session = activeSessionRepository.findByTokenRef(tokenRef).orElse(null);
            if (session == null || session.getTerminatedAt() != null) {
                log.debug("Rejecting a token whose session is unknown or terminated");
                return false;
            }

            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
            Set<String> codes = menuService.effectiveAuthorityCodes().getData();
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, toAuthorities(codes)));
            return true;
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            log.warn("Failed to resolve the effective grants of an otherwise valid caller", e);
            return false;
        }
    }

    /** The token's {@code tid} claim (a JSON number), or {@code null} when absent or malformed. */
    private static Long tenantIdOf(Claims claims) {
        Object value = claims.get(TenantConstants.TENANT_ID_CLAIM);
        return value instanceof Number number ? Long.valueOf(number.longValue()) : null;
    }

    /**
     * RULE-SEC-007 is already applied by {@link MenuService#effectiveAuthorityCodes()}, against the
     * registry's own screen rows. All that remains here is
     * {@link InternalCallerContext#INTERNAL_AUTHORITY}, stripped so no request can ever acquire it,
     * whatever a registry row might say.
     */
    private List<GrantedAuthority> toAuthorities(Set<String> codes) {
        return codes.stream()
            .filter(code -> !InternalCallerContext.INTERNAL_AUTHORITY.equals(code))
            .map(code -> (GrantedAuthority) new SimpleGrantedAuthority(code))
            .toList();
    }
}
