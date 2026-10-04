package com.erp.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Every erp-core setting, bound from {@code erp.core.*}. The library ships no
 * {@code application.properties}: the defaults below are the only defaults, and the two secrets have
 * none, so an application that forgets one fails at startup with a message naming the property.
 *
 * <p>Property migration (step 03), old key → new key:
 * <ul>
 *   <li>{@code app.jwt.secret} → {@code erp.core.security.jwt.secret}</li>
 *   <li>{@code app.jwt.expiration-ms} → {@code erp.core.security.jwt.expiration-ms}</li>
 *   <li>{@code file.access-token.secret} → {@code erp.core.files.access-token-secret}</li>
 *   <li>{@code app.frontend-url} → {@code erp.core.frontend.base-url}</li>
 *   <li>{@code app.password-reset-path} → {@code erp.core.frontend.password-reset-path}</li>
 * </ul>
 */
@ConfigurationProperties("erp.core")
@Validated
@Getter
public class ErpCoreProperties {

    @Valid
    private final Security security = new Security();

    @Valid
    private final Files files = new Files();

    private final Frontend frontend = new Frontend();

    private final Tenant tenant = new Tenant();

    /** Authentication settings. */
    @Getter
    @Setter
    public static class Security {

        /**
         * The default unauthenticated paths: the four pre-authentication SEC endpoints, the
         * actuator health/info probes, the springdoc document and UI, and the servlet error page.
         */
        public static final List<String> DEFAULT_PUBLIC_PATHS = List.of(
            "/api/v1/sec/auth/login",
            "/api/v1/sec/auth/signup",
            "/api/v1/sec/auth/password-reset/**",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error");

        @Valid
        private final Jwt jwt = new Jwt();

        /**
         * Paths the core security filter chain permits without authentication; every other path
         * needs a valid bearer token. Setting this replaces the whole list.
         */
        private List<String> publicPaths = new ArrayList<>(DEFAULT_PUBLIC_PATHS);

        /**
         * Password for the seeded bootstrap {@code admin} account. The core seed ships that account
         * with no usable password (status {@code PENDING}); when this property is set, the
         * {@code BootstrapAdminPasswordRunner} sets it as the admin's password and activates the
         * account on the first start. Ignored once the account has been initialised (a later
         * change of the property never overwrites a password). Optional: without it the admin
         * account stays unusable.
         */
        private String bootstrapAdminPassword;

        // ── erp-core step 06: the CUSTOMER realm ────────────────────────────────────────────────

        /**
         * The default unauthenticated paths of the customer security chain
         * ({@code erpCoreCustomerSecurityFilterChain}, which serves {@code /api/v1/public/**} and
         * {@code /api/v1/customers/**}): customer self-registration, e-mail verification, login and
         * password reset. <b>The single place</b> later steps append their public storefront paths to
         * (e.g. step 07's {@code /api/v1/public/files/**}). Every other path of that chain needs a
         * CUSTOMER token; all of them require {@code X-Tenant-Code} when no token is sent.
         */
        public static final List<String> DEFAULT_CUSTOMER_PUBLIC_PATHS = List.of(
            "/api/v1/public/customers/register",
            "/api/v1/public/customers/verify",
            "/api/v1/public/customers/login",
            "/api/v1/public/customers/password-reset/**");

        /**
         * Paths the customer security filter chain permits without authentication. Setting this
         * replaces the whole list.
         */
        private List<String> customerPublicPaths = new ArrayList<>(DEFAULT_CUSTOMER_PUBLIC_PATHS);

        /** Brute-force protection of the customer login (bucket4j, keyed {@code tenant:realm:username}). */
        private final LoginRateLimit customerLoginRateLimit = new LoginRateLimit();
    }

    /** Login attempts allowed per key and period (erp-core step 06). */
    @Getter
    @Setter
    public static class LoginRateLimit {

        /** Attempts allowed per {@link #period} for one {@code tenant:realm:username} key. */
        private int capacity = 10;

        /** The refill period of {@link #capacity}. */
        private Duration period = Duration.ofMinutes(1);
    }

    /** Access-token (JWT) settings. */
    @Getter
    @Setter
    public static class Jwt {

        /** HMAC-SHA key that signs and verifies access tokens (at least 32 bytes). Required. */
        @NotBlank(message = "erp.core.security.jwt.secret must be set: it is the required HMAC key that "
            + "signs erp-core access tokens (at least 32 bytes), and erp-core has no default for it")
        private String secret;

        /** Access-token lifetime in milliseconds. */
        private long expirationMs = 3_600_000L;
    }

    /** FILE module settings. */
    @Getter
    @Setter
    public static class Files {

        /** Secret the AES-GCM file download token key is derived from (RULE-FILE-003). Required. */
        @NotBlank(message = "erp.core.files.access-token-secret must be set: it is the required secret "
            + "the FILE download-token key is derived from, and erp-core has no default for it")
        private String accessTokenSecret;

        /** Default per-file content limit in bytes (a FileCategory may override it). */
        private long maxContentBytes = 5_242_880L;

        /** Whole-request upload limit in bytes. */
        private long maxRequestBytes = 10_485_760L;
    }

    /** Multi-tenancy settings (erp-core step 05). */
    @Getter
    @Setter
    public static class Tenant {

        /**
         * The default tenant-exempt paths: the actuator, the springdoc document and UI, the servlet
         * error page, and the platform API (its callers are authenticated PLATFORM operators, whose
         * token carries the tenant anyway).
         */
        public static final List<String> DEFAULT_EXEMPT_PATHS = List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error",
            "/api/v1/platform/**");

        /**
         * Paths that may be served without a tenant: neither a token ({@code tid}) nor the
         * {@code X-Tenant-Code} header is required there. Every other public (unauthenticated) path
         * is refused 400 {@code TENANT_REQUIRED} without the header. Setting this replaces the list.
         */
        private List<String> exemptPaths = new ArrayList<>(DEFAULT_EXEMPT_PATHS);
    }

    /** Links that point into the frontend (e.g. the emailed password-reset link). */
    @Getter
    @Setter
    public static class Frontend {

        /** Origin of the frontend, e.g. {@code https://app.example.com}. */
        private String baseUrl;

        /** Route on {@link #baseUrl} that hosts the password-reset screen. */
        private String passwordResetPath = "/reset";

        /** Route on {@link #baseUrl} that the customer e-mail verification link opens (erp-core step 06). */
        private String customerVerifyPath = "/customer/verify";

        /** Route on {@link #baseUrl} that the customer password-reset link opens (erp-core step 06). */
        private String customerPasswordResetPath = "/customer/reset";
    }
}
