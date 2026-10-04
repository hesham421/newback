package com.erp.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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

    // erp-core step 08 — event bus executor and NOTIF asynchronous delivery
    private final Events events = new Events();

    private final Notif notif = new Notif();

    // erp-core step 10 — generic audit log
    private final Audit audit = new Audit();

    /** erp-core step 11 — reporting. */
    @Valid
    private final Report report = new Report();

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

        // --- erp-core step 07: storage provider selection and public file URLs ---------------

        /**
         * Where new file content is written: {@code DB} (default, the document row itself),
         * {@code LOCAL} (needs {@link Local#getRoot() local.root}) or {@code S3} (needs
         * {@link S3#getBucket() s3.bucket} and {@code software.amazon.awssdk:s3} on the classpath).
         * Existing documents are always read from the provider recorded on them.
         */
        private String storage = "DB";

        /**
         * Optional origin prepended to public file URLs (e.g. {@code https://api.example.com});
         * empty = the URL is the path {@code /api/v1/public/files/{tenantCode}/{publicSlug}}.
         */
        private String publicBaseUrl;

        private final Local local = new Local();

        private final S3 s3 = new S3();
    }

    /** {@code erp.core.files.local.*} — the LOCAL storage provider (erp-core step 07). */
    @Getter
    @Setter
    public static class Local {

        /**
         * Root directory of the LOCAL provider; must exist and be writable when
         * {@code erp.core.files.storage=LOCAL}. Content is stored under
         * {@code <root>/<tenantId>/<category>/<yyyy>/<MM>/<documentId>_<filename>}.
         */
        private String root;
    }

    /** {@code erp.core.files.s3.*} — the optional S3-compatible storage provider (erp-core step 07). */
    @Getter
    @Setter
    public static class S3 {

        /** Bucket name; required when {@code erp.core.files.storage=S3}. */
        private String bucket;

        /** Region, e.g. {@code eu-central-1} (S3-compatible servers accept any value). */
        private String region = "us-east-1";

        /** Optional endpoint override for S3-compatible servers (MinIO, R2, ...); enables path-style access. */
        private String endpoint;

        /** Optional static access key; empty = the AWS default credentials chain. */
        private String accessKey;

        /** Optional static secret key (with {@link #accessKey}). */
        private String secretKey;

        /**
         * Optional base URL under which objects are publicly readable (bucket website, CDN). When set,
         * a PUBLIC document's public URL redirects (302) there instead of being streamed by the app.
         */
        private String publicBaseUrl;
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

        /**
         * erp-core step 07: the default paths whose tenant comes from the path itself — the public
         * file URLs, which must work in a plain browser/curl without a header or token.
         */
        public static final List<String> DEFAULT_PATH_TENANT_PATHS = List.of(
            "/api/v1/public/files/{tenantCode}/**");

        /**
         * Paths whose tenant is resolved from the {@code {tenantCode}} path variable instead of the
         * token or the {@code X-Tenant-Code} header (unknown → 404, suspended → 403). On such a path
         * the path's tenant always wins; a caller authenticated in another tenant is treated as
         * anonymous there. Setting this replaces the list.
         */
        private List<String> pathTenantPaths = new ArrayList<>(DEFAULT_PATH_TENANT_PATHS);
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

    /** Domain event bus settings (erp-core step 08). */
    @Getter
    public static class Events {

        private final Executor executor = new Executor();

        /** The core event executor ({@code erpCoreEventExecutor}) that runs asynchronous listeners. */
        @Getter
        @Setter
        public static class Executor {

            /** Threads kept alive. */
            private int corePoolSize = 4;

            /** Upper bound of threads, reached only once the queue is full. */
            private int maxPoolSize = 16;

            /** Tasks waiting for a thread before the pool grows beyond the core size. */
            private int queueCapacity = 500;

            /** Prefix of the worker thread names. */
            private String threadNamePrefix = "erp-event-";
        }
    }

    /** NOTIF asynchronous delivery settings (erp-core step 08). */
    @Getter
    public static class Notif {

        private final Retry retry = new Retry();

        private final Requeue requeue = new Requeue();

        /**
         * Delivery retry policy (Spring Retry, exponential backoff): the delays between attempts are
         * {@code initialDelayMs}, ×{@code multiplier} each time, capped at {@code maxDelayMs} — by
         * default 5 attempts, 2 s → 4 s → 8 s → 16 s (cap 32 s). After the last failed attempt the
         * {@code NOTIF_LOG} row is {@code FAILED}. Read by the {@code @Retryable} placeholders too.
         */
        @Getter
        @Setter
        public static class Retry {

            /** Total delivery attempts, the first included. */
            private int maxAttempts = 5;

            /** Delay before the second attempt, in milliseconds. */
            private long initialDelayMs = 2_000L;

            /** Factor applied to the delay after each failed attempt. */
            private double multiplier = 2.0d;

            /** Upper bound of the delay between two attempts, in milliseconds. */
            private long maxDelayMs = 32_000L;
        }

        /**
         * Crash recovery: {@code NotificationRequeueJob} re-dispatches {@code QUEUED} rows whose
         * delivery stalled (e.g. the JVM stopped mid-queue). Core never schedules it; an application
         * enables it here and turns on {@code @EnableScheduling} (or calls the job itself).
         */
        @Getter
        @Setter
        public static class Requeue {

            /** Registers the {@code NotificationRequeueJob} bean. */
            private boolean enabled = false;

            /** A QUEUED row is stale once its next attempt (or creation) is this many minutes old. */
            private long staleAfterMinutes = 10L;

            /** Delay between two runs when the application has scheduling enabled, in milliseconds. */
            private long intervalMs = 60_000L;
        }
    }

    /** Generic audit log settings (erp-core step 10). */
    @Getter
    @Setter
    public static class Audit {

        /**
         * {@code AuditRetentionJob} deletes {@code CORE_AUDIT_EVENT} rows older than this many days;
         * {@code 0} (the default) keeps every row forever.
         */
        private int retentionDays = 0;

        /**
         * Cron of the job's own {@code @Scheduled} trigger, which fires only in an application that
         * enables scheduling; {@code -} (the default) disables it.
         */
        private String retentionCron = "-";
    }

    /** Reporting settings (erp-core step 11). */
    @Getter
    @Setter
    public static class Report {

        /** Most rows one export may contain; a larger result answers 422 {@code REPORT_EXPORT_TOO_LARGE}. */
        @Positive
        private int maxExportRows = 100_000;
    }
}
