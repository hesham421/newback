package com.erp.main.config;

import com.erp.sec.security.JwtAuthenticationFilter;
import com.erp.sec.security.SecSecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
 * Authentication and authorization are ON (SEC-BE). Only the four pre-authentication endpoints and
 * the infrastructure paths listed below are open; everything else needs a valid bearer token, and
 * {@code @EnableMethodSecurity} makes every {@code @PreAuthorize} in the codebase live.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecSecurityErrorHandler securityErrorHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // The four pre-authentication endpoints (SEC-BE.md: SEC_LOGIN, SEC_SIGNUP and
                // SEC_PWD_RESET are public screens; CORE.md exempts API-SEC-001..004 by name).
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/sec/auth/login",
                    "/api/v1/sec/auth/signup",
                    "/api/v1/sec/auth/password-reset/request",
                    "/api/v1/sec/auth/password-reset/complete").permitAll()
                // Docker's healthcheck and the TestSprite harness poll these unauthenticated
                // (application.properties exposes exactly health + info).
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                // springdoc's own two paths; without them the API contract cannot be read at all,
                // since the browser fetches the document before any token exists.
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**",
                    "/swagger-ui.html", "/swagger-ui/**").permitAll()
                // The container's ERROR dispatch, which is authorized like any other request in
                // Spring Security 6+; blocking it would replace every error body with a 403.
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
