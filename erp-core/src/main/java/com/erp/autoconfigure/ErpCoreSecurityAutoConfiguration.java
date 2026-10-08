package com.erp.autoconfigure;

import com.erp.sec.entity.User;
import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.security.JwtAuthenticationFilter;
import com.erp.sec.security.JwtTokenValidator;
import com.erp.sec.security.PasswordChangeRequiredFilter;
import com.erp.sec.security.RealmEnforcementFilter;
import com.erp.sec.security.SecSecurityErrorHandler;
import com.erp.sec.service.MenuService;
import com.erp.tenant.TenantContext;
import com.erp.tenant.permission.TenantPermissions;
import com.erp.tenant.repository.TenantRepository;
import com.erp.tenant.security.PublicBrandingRateLimitFilter;
import com.erp.tenant.security.TenantResolutionFilter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless bearer-token security for every core endpoint. Only {@code erp.core.security.public-paths}
 * are open; everything else needs a valid access token, and {@code @EnableMethodSecurity} makes every
 * {@code @PreAuthorize} in the core live.
 *
 * <p>The core chain ({@value #FILTER_CHAIN_BEAN_NAME}, {@code @Order(}{@value #FILTER_CHAIN_ORDER}{@code )})
 * matches every request. An application adds its own chain for its own paths with a lower order and
 * a {@code securityMatcher}, or replaces the core chain by defining a bean with the same name.
 * Ordered before Boot's web-security auto-configurations so their default chains back off.
 *
 * <p>Tenant (erp-core step 05): {@code JwtAuthenticationFilter} sets the request's tenant from the
 * token's {@code tid} claim, then {@link TenantResolutionFilter} (right after it) falls back to the
 * {@code X-Tenant-Code} header and refuses unknown / suspended tenants. {@value #PLATFORM_PATHS}
 * requires an authenticated caller of the PLATFORM tenant holding
 * {@value #PLATFORM_TENANT_MANAGE_AUTHORITY}.
 *
 * <p>Realms (erp-core step 06): a second chain, {@value #CUSTOMER_FILTER_CHAIN_BEAN_NAME}
 * ({@code @Order(}{@value #CUSTOMER_FILTER_CHAIN_ORDER}{@code )}), serves exactly
 * {@value #CUSTOMER_PUBLIC_API} and {@value #CUSTOMER_API} for the CUSTOMER realm: its
 * {@code erp.core.security.customer-public-paths} are open, everything else needs a customer token
 * ({@code ROLE_CUSTOMER}). Both chains run the same JWT and tenant filters plus a
 * {@link RealmEnforcementFilter} for their own realm, so a token of the other realm is refused with 403
 * {@code REALM_MISMATCH}. Each chain backs off by bean name.
 */
@AutoConfiguration(
    before = {SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class},
    beforeName = "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableMethodSecurity
public class ErpCoreSecurityAutoConfiguration {

    public static final String FILTER_CHAIN_BEAN_NAME = "erpCoreSecurityFilterChain";
    public static final int FILTER_CHAIN_ORDER = 100;

    public static final String CUSTOMER_FILTER_CHAIN_BEAN_NAME = "erpCoreCustomerSecurityFilterChain";
    public static final int CUSTOMER_FILTER_CHAIN_ORDER = 90;

    /** The unauthenticated storefront API (customer self-service, later steps' public resources). */
    public static final String CUSTOMER_PUBLIC_API = "/api/v1/public/**";

    /** The authenticated customer API. */
    public static final String CUSTOMER_API = "/api/v1/customers/**";

    /** The platform-level API (tenant provisioning). */
    public static final String PLATFORM_PATHS = "/api/v1/platform/**";

    /** Authority required on {@link #PLATFORM_PATHS} (seeded to the PLATFORM tenant's SYS_ADMIN by V10). */
    public static final String PLATFORM_TENANT_MANAGE_AUTHORITY = TenantPermissions.PLATFORM_TENANT_MANAGE;

    /**
     * erp-core step 07 — the public file URLs ({@code PublicFileController}). Served by the customer
     * chain (they lie under {@value #CUSTOMER_PUBLIC_API}), which permits them for {@code GET} and
     * {@code HEAD} only, treats them as public for realm enforcement (any token, staff or customer, is
     * irrelevant there) and resolves their tenant from the path ({@code erp.core.tenant.path-tenant-paths}).
     * Not part of {@code erp.core.security.customer-public-paths}, because that list is permitted for
     * every method.
     */
    public static final String PUBLIC_FILE_PATHS = "/api/v1/public/files/**";

    /**
     * tenant-maturity E (REQ-TENANT-032) — the anonymous public branding, {@code GET} only, on the customer chain like
     * {@link #PUBLIC_FILE_PATHS}: public for the realm and tenant filters, tenant from the path
     * ({@code erp.core.tenant.path-tenant-paths}), rate-limited per client address first (RULE-TENANT-022).
     */
    public static final String PUBLIC_TENANT_BRANDING_PATHS = "/api/v1/public/tenants/*/branding";

    /**
     * tenant-maturity E (REQ-TENANT-031) — the branding of the caller's tenant: authenticated, but its {@code GET} is
     * realm-neutral on the core chain (a CUSTOMER token is not refused with {@code REALM_MISMATCH} there) and allowed
     * during a pending forced password change (RULE-SEC-059).
     */
    public static final String TENANT_ME_PATH = "/api/v1/tenant/me";

    /**
     * {@code @Lazy} keeps the JPA and method-security infrastructure out of the security-config
     * bootstrap: the filter is built while the filter chain is, long before those are ready.
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenValidator tokenValidator,
                                                           @Lazy UserRepository userRepository,
                                                           @Lazy ActiveSessionRepository activeSessionRepository,
                                                           @Lazy MenuService menuService) {
        return new JwtAuthenticationFilter(tokenValidator, userRepository, activeSessionRepository, menuService);
    }

    @Bean(name = FILTER_CHAIN_BEAN_NAME)
    @ConditionalOnMissingBean(name = FILTER_CHAIN_BEAN_NAME)
    @Order(FILTER_CHAIN_ORDER)
    public SecurityFilterChain erpCoreSecurityFilterChain(HttpSecurity http,
                                                          JwtAuthenticationFilter jwtAuthenticationFilter,
                                                          SecSecurityErrorHandler securityErrorHandler,
                                                          ErpCoreProperties properties,
                                                          ObjectProvider<TenantRepository> tenantRepository,
                                                          MessageSource messageSource) throws Exception {
        String[] publicPaths = properties.getSecurity().getPublicPaths().toArray(String[]::new);
        TenantResolutionFilter tenantResolutionFilter = new TenantResolutionFilter(
            tenantRepository::getObject, messageSource,
            properties.getSecurity().getPublicPaths(), properties.getTenant().getExemptPaths());
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(PLATFORM_PATHS).access((authentication, context) ->
                    new AuthorizationDecision(isPlatformOperator(authentication.get())));
                if (publicPaths.length > 0) {
                    auth.requestMatchers(publicPaths).permitAll();
                }
                auth.anyRequest().authenticated();
            })
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(tenantResolutionFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(new RealmEnforcementFilter(User.REALM_STAFF, properties.getSecurity().getPublicPaths(),
                List.of(TENANT_ME_PATH), securityErrorHandler), TenantResolutionFilter.class)
            // tenant-maturity D (RULE-SEC-059): a pending forced password change blocks all but three calls
            .addFilterAfter(new PasswordChangeRequiredFilter(properties.getSecurity().getPublicPaths(),
                securityErrorHandler), RealmEnforcementFilter.class);
        return http.build();
    }

    /**
     * erp-core step 06 — the CUSTOMER realm's chain: {@value #CUSTOMER_PUBLIC_API} and
     * {@value #CUSTOMER_API} only. {@code erp.core.security.customer-public-paths} are open (they still
     * need {@code X-Tenant-Code}); every other path of the chain needs an authenticated CUSTOMER
     * ({@value JwtAuthenticationFilter#ROLE_CUSTOMER}). A staff token is refused by the chain's
     * {@link RealmEnforcementFilter} (403 {@code REALM_MISMATCH}).
     */
    @Bean(name = CUSTOMER_FILTER_CHAIN_BEAN_NAME)
    @ConditionalOnMissingBean(name = CUSTOMER_FILTER_CHAIN_BEAN_NAME)
    @Order(CUSTOMER_FILTER_CHAIN_ORDER)
    public SecurityFilterChain erpCoreCustomerSecurityFilterChain(HttpSecurity http,
                                                                  JwtAuthenticationFilter jwtAuthenticationFilter,
                                                                  SecSecurityErrorHandler securityErrorHandler,
                                                                  ErpCoreProperties properties,
                                                                  ObjectProvider<TenantRepository> tenantRepository,
                                                                  MessageSource messageSource) throws Exception {
        List<String> customerPublicPaths = properties.getSecurity().getCustomerPublicPaths();
        String[] publicPaths = customerPublicPaths.toArray(String[]::new);
        // erp-core step 07: the public file URLs are public for the filters too (no realm check, no
        // TENANT_REQUIRED) and take their tenant from the path.
        List<String> unauthenticatedPaths = new ArrayList<>(customerPublicPaths);
        unauthenticatedPaths.add(PUBLIC_FILE_PATHS);
        unauthenticatedPaths.add(PUBLIC_TENANT_BRANDING_PATHS); // tenant-maturity E
        ErpCoreProperties.PublicBrandingRateLimit brandingLimit = properties.getTenant().getPublicBrandingRateLimit();
        TenantResolutionFilter tenantResolutionFilter = new TenantResolutionFilter(
            tenantRepository::getObject, messageSource, unauthenticatedPaths, properties.getTenant().getExemptPaths(),
            properties.getTenant().getPathTenantPaths());
        http
            .securityMatcher(CUSTOMER_PUBLIC_API, CUSTOMER_API)
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                if (publicPaths.length > 0) {
                    auth.requestMatchers(publicPaths).permitAll();
                }
                auth.requestMatchers(HttpMethod.GET, PUBLIC_FILE_PATHS).permitAll(); // erp-core step 07
                auth.requestMatchers(HttpMethod.HEAD, PUBLIC_FILE_PATHS).permitAll();
                auth.requestMatchers(HttpMethod.GET, PUBLIC_TENANT_BRANDING_PATHS).permitAll(); // tenant-maturity E
                auth.anyRequest().hasAuthority(JwtAuthenticationFilter.ROLE_CUSTOMER);
            })
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // tenant-maturity E (RULE-TENANT-022): counted before the token and the tenant are looked at
            .addFilterBefore(new PublicBrandingRateLimitFilter(PUBLIC_TENANT_BRANDING_PATHS, brandingLimit.getCapacity(),
                brandingLimit.getPeriod(), messageSource), JwtAuthenticationFilter.class)
            .addFilterAfter(tenantResolutionFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(new RealmEnforcementFilter(User.REALM_CUSTOMER, unauthenticatedPaths, securityErrorHandler),
                TenantResolutionFilter.class);
        return http.build();
    }

    /**
     * An authenticated caller whose request tenant is PLATFORM and who holds
     * {@value #PLATFORM_TENANT_MANAGE_AUTHORITY}. The tenant check is defence in depth: the
     * authority is only ever granted inside the PLATFORM tenant.
     */
    static boolean isPlatformOperator(org.springframework.security.core.Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
            && TenantContext.isPlatform()
            && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PLATFORM_TENANT_MANAGE_AUTHORITY::equals);
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
