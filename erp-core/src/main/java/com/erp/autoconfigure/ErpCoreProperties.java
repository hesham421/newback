package com.erp.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    /** Links that point into the frontend (e.g. the emailed password-reset link). */
    @Getter
    @Setter
    public static class Frontend {

        /** Origin of the frontend, e.g. {@code https://app.example.com}. */
        private String baseUrl;

        /** Route on {@link #baseUrl} that hosts the password-reset screen. */
        private String passwordResetPath = "/reset";
    }
}
