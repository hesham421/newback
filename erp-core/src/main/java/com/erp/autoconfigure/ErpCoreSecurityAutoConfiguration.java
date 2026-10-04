package com.erp.autoconfigure;

import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.security.JwtAuthenticationFilter;
import com.erp.sec.security.JwtTokenValidator;
import com.erp.sec.security.SecSecurityErrorHandler;
import com.erp.sec.service.MenuService;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import com.erp.tenant.repository.TenantRepository;
import com.erp.tenant.security.TenantResolutionFilter;
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

    /** The platform-level API (tenant provisioning). */
    public static final String PLATFORM_PATHS = "/api/v1/platform/**";

    /** Authority required on {@link #PLATFORM_PATHS} (seeded to the PLATFORM tenant's SYS_ADMIN by V10). */
    public static final String PLATFORM_TENANT_MANAGE_AUTHORITY = "PLATFORM_TENANT_MANAGE";

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
            .addFilterAfter(tenantResolutionFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * An authenticated caller whose request tenant is PLATFORM and who holds
     * {@value #PLATFORM_TENANT_MANAGE_AUTHORITY}. The tenant check is defence in depth: the
     * authority is only ever granted inside the PLATFORM tenant.
     */
    static boolean isPlatformOperator(org.springframework.security.core.Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
            && Long.valueOf(TenantConstants.PLATFORM_TENANT_ID).equals(TenantContext.current())
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
