package com.erp.autoconfigure;

import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.security.JwtAuthenticationFilter;
import com.erp.sec.security.JwtTokenValidator;
import com.erp.sec.security.SecSecurityErrorHandler;
import com.erp.sec.service.MenuService;
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
                                                          ErpCoreProperties properties) throws Exception {
        String[] publicPaths = properties.getSecurity().getPublicPaths().toArray(String[]::new);
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                if (publicPaths.length > 0) {
                    auth.requestMatchers(publicPaths).permitAll();
                }
                auth.anyRequest().authenticated();
            })
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
