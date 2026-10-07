package com.erp.sec.security;

import com.erp.sec.entity.ActiveSession;
import com.erp.sec.entity.User;
import com.erp.sec.permission.SecPermissions;
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
 * and usernames are unique per tenant only — and stays set for the rest of the request; it is
 * cleared in a {@code finally} after the chain. A token without {@code tid}, or one whose user or
 * session is rejected, leaves no tenant behind, so the tenant module's {@code TenantResolutionFilter}
 * (next in the chain) can fall back to the {@code X-Tenant-Code} header.
 *
 * <p>erp-core 1.2.0: the filter starts every request with no tenant. A value already present on the
 * thread (a leak from earlier work on a reused worker thread) is logged and cleared before anything
 * else runs, so it can never be used by the request, and it is not put back: the thread leaves the
 * filter with no tenant, so nothing that runs on it later outside this filter (e.g. a container error
 * dispatch after {@code sendError}) can pick the leak up.
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

    /** erp-core step 06 — the single authority of a CUSTOMER caller ({@code hasRole('CUSTOMER')}). */
    public static final String ROLE_CUSTOMER = SecPermissions.ROLE_CUSTOMER;

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
        if (previousTenant != null) {
            // erp-core 1.2.0: a tenant left on a reused worker thread is never used by this request,
            // and is not restored afterwards either (the finally below clears it for good).
            log.warn("Tenant {} was already set on thread {} when request {} {} arrived; cleared for the request",
                previousTenant, Thread.currentThread().getName(), request.getMethod(), request.getRequestURI());
            TenantContext.clear();
        }
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
                boolean authenticated = tokenValidator.parse(header.substring(BEARER_PREFIX.length()))
                    .map(this::authenticate)
                    .orElse(false);
                if (!authenticated) {
                    // no tenant behind: TenantResolutionFilter may fall back to X-Tenant-Code
                    TenantContext.clear();
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * REQ-SEC-028: the token's {@code jti} is the session's {@code tokenRef}, so a terminated or
     * unknown session makes the token unusable for every subsequent request, which is what
     * API-SEC-026 relies on. A non-ACTIVE or deactivated user is rejected for the same reason.
     *
     * <p>erp-core step 06 — the token's {@code realm} claim selects the account (usernames are unique
     * per tenant and realm) and the authorities: a {@code STAFF} caller gets its RBAC permission codes
     * as today ({@link MenuService#effectiveAuthorityCodes()}); a {@code CUSTOMER} caller gets exactly
     * {@value #ROLE_CUSTOMER} and no grant is queried. The realm is attached as {@link AuthRealm}
     * details, which {@link RealmEnforcementFilter} checks against the chain's realm. A token without a
     * known realm (e.g. issued before step 06) does not authenticate.
     *
     * @return whether the caller was authenticated. The token's tenant is set as the
     *         {@link TenantContext} before the lookups; on {@code false} the caller clears it
     */
    private boolean authenticate(Claims claims) {
        String username = claims.getSubject();
        String tokenRef = claims.getId();
        Long tenantId = tenantIdOf(claims);
        String realm = claims.get(JwtTokenIssuer.REALM_CLAIM, String.class);
        if (username == null || username.isBlank() || tokenRef == null || tokenRef.isBlank()
            || tenantId == null || !(User.REALM_STAFF.equals(realm) || User.REALM_CUSTOMER.equals(realm))) {
            return false;
        }

        TenantContext.set(tenantId);
        try {
            User user = userRepository.findByUsernameAndRealm(username, realm).orElse(null);
            if (user == null || !STATUS_ACTIVE.equals(user.getStatusCode())
                || !Boolean.TRUE.equals(user.getIsActiveFl())) {
                return false;
            }

            ActiveSession session = activeSessionRepository.findByTokenRef(tokenRef).orElse(null);
            if (session == null || session.getTerminatedAt() != null) {
                log.debug("Rejecting a token whose session is unknown or terminated");
                return false;
            }

            if (User.REALM_CUSTOMER.equals(realm)) {
                install(username, realm, List.of(new SimpleGrantedAuthority(ROLE_CUSTOMER)));
                return true;
            }
            install(username, realm, List.of());
            Set<String> codes = menuService.effectiveAuthorityCodes().getData();
            install(username, realm, toAuthorities(codes));
            return true;
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            log.warn("Failed to resolve the effective grants of an otherwise valid caller", e);
            return false;
        }
    }

    private static void install(String username, String realm, List<GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new AuthRealm(realm));
        SecurityContextHolder.getContext().setAuthentication(authentication);
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
    private static List<GrantedAuthority> toAuthorities(Set<String> codes) {
        return codes.stream()
            .filter(code -> !InternalCallerContext.INTERNAL_AUTHORITY.equals(code) && !ROLE_CUSTOMER.equals(code))
            .map(code -> (GrantedAuthority) new SimpleGrantedAuthority(code))
            .toList();
    }
}
