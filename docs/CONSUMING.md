# Consuming erp-core

`com.erp:erp-core` is an auto-configured Spring Boot 4 library (Java 25, PostgreSQL 16). An application
adds it as an ordinary Maven dependency; everything else arrives through
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. The application never
scans `com.erp.*` itself and never copies a core class. `erp-app-reference/` in this repository is the
working example, and CI builds it against the published artifact on every release tag.

## The three rules

1. **The core is never copied or modified by an application.** You depend on a released version. A change
   you need in core is a pull request to this repository and a new version, never a patched copy.
   `CoreLibraryRulesArchTest` and `CrossModuleBoundaryArchTest` enforce the library's structure in its
   own build.
2. **Application logic lives in the application's `domain/` package** (for example
   `com.acme.shop.domain..`), next to its own entities, services and controllers. Never put it under
   `com.erp.*`.
3. **A change needed by two applications goes into core.** Duplicated logic in two applications is a
   missing core feature. It is added to core additively (see `docs/RELEASE.md`) and both applications move
   to the new MINOR version.

## 1. Repository and dependency

The artifacts are published to this repository's GitHub Packages registry. GitHub Packages requires
authentication even for reads. Use a personal access token (classic) with `read:packages`, or the
workflow's `GITHUB_TOKEN` in GitHub Actions.

`~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USER</username>
      <password>${env.GITHUB_TOKEN}</password>   <!-- PAT with read:packages -->
    </server>
  </servers>
  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/hesham421/newback</url>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>
</settings>
```

Application `pom.xml` (Spring Boot parent 4.0.1, the version erp-core is built and tested with):

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.0.1</version>
</parent>

<properties>
  <java.version>25</java.version>
  <erp.core.version>1.3.0</erp.core.version>
</properties>

<dependencies>
  <dependency>
    <groupId>com.erp</groupId>
    <artifactId>erp-core</artifactId>
    <version>${erp.core.version}</version>
  </dependency>
  <!-- erp-core declares the driver optional: the application supplies it -->
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
  <!-- Optional: Swagger UI and erp-core's per-module OpenAPI groups -->
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.0</version>
  </dependency>

  <!-- Tests: the same Testcontainers-first / embedded-fallback PostgreSQL selection as core's own tests -->
  <dependency>
    <groupId>com.erp</groupId>
    <artifactId>erp-core</artifactId>
    <version>${erp.core.version}</version>
    <type>test-jar</type>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>io.zonky.test</groupId>
    <artifactId>embedded-postgres</artifactId>
    <version>2.1.0</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

The application class is a plain `@SpringBootApplication` in the application's own package. It must not
scan `com.erp`. The core's entities, repositories and components are registered by its
auto-configuration, next to the application's own (Boot's "application package" default is kept).

## 2. Configuration

All core properties are prefixed `erp.core.*` and bound and validated by `ErpCoreProperties`.
Never use `@Value` for them. The IDE completion comes from `META-INF/spring-configuration-metadata.json`.

### Required

| Property | What |
|---|---|
| `spring.datasource.url` / `username` / `password` | PostgreSQL 16 — the version the test suite runs against (Testcontainers and the embedded fallback); `erp-app-reference/docker/docker-compose.yml` pins `postgres:17`, a pre-existing drift — use 16. Flyway applies the core chain first. |
| `erp.core.security.jwt.secret` | HMAC key for access tokens, at least 32 bytes. Startup fails if it is blank. |
| `erp.core.files.access-token-secret` | Secret for the FILE download-token key. Startup fails if it is blank. |

`spring.jpa.hibernate.ddl-auto` must stay `none` (Flyway owns the schema), and `spring.jpa.open-in-view`
should be `false`.

### Usually set

| Property | Default | What |
|---|---|---|
| `erp.core.security.bootstrap-admin-password` | empty | Sets the password of the seeded `admin` (SYS_ADMIN of tenant `PLATFORM`) once, on first start, and activates it. Without it nobody can log in as `admin`; there is no `admin/admin`. |
| `erp.core.security.jwt.expiration-ms` | `3600000` | Access-token lifetime. |
| `erp.core.security.password-policy.min-length` / `max-length` / `require-letter` / `require-digit` | `8` / `72` / `true` / `true` | The STAFF password policy (1.3.0); `max-length` above 72 fails startup and every password is also limited to 72 UTF-8 bytes (BCrypt; an Arabic letter takes 2): user create, reset completion, an administrator setting a password, the own change and a new tenant's first administrator answer 400 `SEC-400-PASSWORD-POLICY` otherwise. The message names the default composition; override the key in your bundle if you disable a requirement. Customer passwords get only the 72-byte limit. |
| `erp.core.frontend.base-url`, `password-reset-path`, `customer-verify-path`, `customer-password-reset-path` | — / `/reset` / `/customer/verify` / `/customer/reset` | Links in e-mails. |
| `erp.core.security.public-paths`, `customer-public-paths` | see `ErpCoreProperties.Security.DEFAULT_*` | Unauthenticated paths of the staff and customer chains. **Setting one replaces the whole list**, so start from the defaults. |
| `erp.core.tenant.exempt-paths`, `path-tenant-paths` | see `ErpCoreProperties.Tenant.DEFAULT_*` | Paths served without a tenant, and paths whose tenant comes from a `{tenantCode}` path variable (since 1.3.0 also the public branding `/api/v1/public/tenants/{tenantCode}/branding`: keep it when you replace the list). |
| `erp.core.files.storage` | `DB` | `DB`, `LOCAL` (`erp.core.files.local.root`) or `S3` (`erp.core.files.s3.*`). |
| `erp.core.files.max-content-bytes` / `max-request-bytes` / `public-base-url` | 5 MB / 10 MB / empty | Upload limits and the origin of public file URLs. |
| `erp.core.notif.retry.*` | 5 attempts, 2 s doubling, 32 s maximum | Asynchronous delivery retries. |
| `erp.core.notif.requeue.enabled` / `stale-after-minutes` / `interval-ms` | `false` / `10` / `60000` | Requeue job for stale `QUEUED` notifications. It runs only if the application enables scheduling. **Enable it in production** (see §7). |
| `erp.core.events.executor.*` | 4 / 16 / 500 / `erp-event-` | Event worker pool. |
| `erp.core.security.customer-login-rate-limit.capacity` / `period` | `10` / `1m` | Customer login limit per `tenant:realm:username`. |
| `erp.core.tenant.public-branding-rate-limit.capacity` / `period` | `60` / `1m` | (1.3.0) Requests to the anonymous `GET /api/v1/public/tenants/{tenantCode}/branding` per client address (`getRemoteAddr()`; an IPv6 address counts by its /64), counted before the tenant is looked up (unknown codes included); over it 429 `TENANT_BRANDING_RATE_LIMITED` with `Retry-After` (seconds). Idle buckets expire after `period`, at most 10 000 addresses are tracked. Per JVM. Behind a reverse proxy see "Client address behind a proxy" below. |
| `erp.core.audit.retention-days` / `retention-cron` | `0` (keep) / `-` (off) | Audit retention. The cron fires only if the application enables scheduling. |
| `erp.core.idempotency.enabled` / `retention` / `retention-cron` | `true` / `24h` / `-` (off) | (1.3.0) The `Idempotency-Key` mechanism (§3): `false` ignores the header; a stored answer is replayed for `retention` (must be positive; an older key counts as unused); the cron of `IdempotencyKeyRetentionJob`, which deletes older rows, fires only if the application enables scheduling. The request hash is keyed by a key derived from `erp.core.security.jwt.secret`: rotating the secret turns a retry within the retention into a 409. |
| `erp.core.report.max-export-rows` | `100000` | Export cap. Above it the export answers 422 `REPORT_EXPORT_TOO_LARGE`. |
| `erp.core.tenant.export.max-rows` / `max-concurrent` | `200000` / `2` | (1.3.0) The most rows one tenant data export (`POST /api/v1/platform/tenants/{id}/export`) may contain, all files together (more answers 422 `TENANT_EXPORT_TOO_LARGE`; it bounds the number of rows, not their width), and how many exports may run at once on a node (one more answers 429 `TENANT_EXPORT_BUSY`). Read on every export; must be positive. |

### Optional infrastructure

Each piece is off unless the application adds the dependency and its configuration.

| Infrastructure | Add | Effect without it |
|---|---|---|
| Redis | `spring-boot-starter-data-redis` + `spring.data.redis.*` | FILE download tokens use the in-memory store (single node only). `spring.cache.type=redis` also works for the settings cache. |
| SMTP | `spring-boot-starter-mail` + `spring.mail.host` (and credentials) | No `EMAIL` channel provider exists, so EMAIL notifications end `SKIPPED_NO_PROVIDER`. |
| S3 / S3-compatible | `software.amazon.awssdk:s3` + `erp.core.files.storage=S3`, `erp.core.files.s3.bucket` (`region`, `endpoint`, `access-key`, `secret-key`, `public-base-url`) | `S3` cannot be selected. Startup fails with a message naming the property if it is selected anyway. |
| Scheduling | `@EnableScheduling` on an application `@Configuration` | Core never enables scheduling. Without it the requeue and retention triggers never fire. You can call `NotificationRequeueJob.requeueStale()`, `AuditRetentionJob.run()` and `IdempotencyKeyRetentionJob.run()` yourself. |

## 3. Database: migrations, tenants, number series, settings

- **Ranges.** Core ships `V1..V999` in `classpath:db/migration/core`, which erp-core prepends to
  `spring.flyway.locations`. The application's scripts are `V1000+` in its own location (default
  `classpath:db/migration`). Core scripts are additive only (`docs/RELEASE.md`).
- **Tenants.** Every application table that holds tenant data gets `TENANT_ID BIGINT NOT NULL` (no
  default, FK to `CORE_TENANT(ID)`, index), `VERSION BIGINT NOT NULL DEFAULT 0` and the four audit
  columns. Every unique constraint starts with `TENANT_ID`. Its entity extends
  `com.erp.common.domain.AuditableEntity`. Hibernate's `@TenantId` filters every query and fills
  `TENANT_ID`. Seed rows name `TENANT_ID` explicitly (`1` = `PLATFORM`).
- **Tenant context.** On a request it comes from the token's `tid` claim or the `X-Tenant-Code` header.
  Outside a request (jobs, listeners), wrap the work in
  `com.erp.tenant.TenantContext.runAs(tenantId, ...)` / `callAs`, around the `@Transactional` call.
- **Suspension and token cut-off (1.3.0).** Suspending a tenant ends its sessions; re-activating it, or
  `POST /api/v1/platform/tenants/{id}/revoke-tokens`, cuts off every token issued before (401
  `TENANT_TOKEN_REVOKED` on any non-public path, whole-second precision). A client treats it like any 401 and signs
  in again; the login itself ignores a stale `Authorization` header. Revoke-tokens refuses every token up to and
  including its own second (a login in that second signs in again a moment later); if it answers 500
  `TENANT_REVOKE_SESSIONS_FAILED`, the tokens are already refused but the sessions were not ended — call it again. A job that works per tenant can skip suspended
  tenants with `com.erp.tenant.crossmodule.TenantLookupApi.isActive(tenantId)` (uncached).
- **Reference data for new tenants.** If the application seeds reference data that every tenant needs,
  it implements `com.erp.tenant.TenantProvisioningContributor` (`order()`,
  `provision(TenantProvisioning)`). Use JDBC with explicit `TENANT_ID` and copy from the source tenant.
  Provisioning (`POST /api/v1/platform/tenants`) runs it in the same transaction.
- **Idempotent POSTs (1.3.0).** `POST /api/v1/platform/tenants` accepts an optional `Idempotency-Key` header
  (1 to 64 characters of `A-Z a-z 0-9 . _ : -`; otherwise 400 `IDEMPOTENCY_KEY_INVALID`). The first request runs and
  its 2xx answer is stored in `CORE_IDEMPOTENCY_KEY` in the same transaction (a failure stores nothing, so the key can be
  retried); a retry with the same key, the same body (compared as canonical JSON: property order and whitespace do not
  matter) and the same user answers the stored status and body with the response header `Idempotent-Replayed: true`
  and runs nothing; another body or another user under the key answers 409 `IDEMPOTENCY_KEY_CONFLICT`. A concurrent
  request with the same key waits for the first one and then replays it. Keys live `erp.core.idempotency.retention`
  (24 h). A client generates one key per logical submission (a UUID) and reuses it for every retry of that submission.
  An application can give its own expensive `POST` the same behaviour: inject
  `com.erp.common.idempotency.IdempotentResponses` and return
  `idempotentResponses.craftResponse(idempotencyKey, "POST /api/v1/my/things", request, MyResponse.class,
  () -> service.create(request))` with `@RequestHeader(name = IdempotentResponses.IDEMPOTENCY_KEY_HEADER, required =
  false) String idempotencyKey`; the service method must be `@Transactional` (it joins the key's transaction) and must
  not commit work in its own `REQUIRES_NEW` transactions. A replay is answered before the service method, so before
  its `@PreAuthorize`: authorize the path in the security chain as well, or the stored answer can be replayed to its
  user for up to the retention period after that user's permission was revoked. Behind another origin, allow the request header
  `Idempotency-Key` and expose `Idempotent-Replayed` in your CORS configuration (core configures no CORS).
- **Tenant data export (1.3.0).** `POST /api/v1/platform/tenants/{id}/export` (`PLATFORM_TENANT_MANAGE`) answers a
  ZIP of one CSV per table (UTF-8 with BOM, RFC 4180, a text starting with `= + - @` TAB or CR prefixed with `'`) and
  `manifest.json`, stored as a PRIVATE file document of the PLATFORM tenant, and a single-use download token for
  `GET /api/v1/files/download?token=` (10 minutes, same user; a new one with `POST /api/v1/files/{id}/access-token`).
  The archive is a **restricted** FILE document: only a user holding `PLATFORM_TENANT_MANAGE` sees it through the FILE
  API (list, metadata, token, download, delete); anyone else gets 404 `FILE_DOCUMENT_NOT_FOUND`, whatever FILE permission
  they hold. Deleting it (`DELETE /api/v1/files/{id}?action=DELETE`) removes its bytes and keeps a metadata tombstone.
  It is synchronous and bounded by `erp.core.tenant.export.max-rows`; one export per tenant at a time **per node**
  (409 `TENANT_EXPORT_IN_PROGRESS`). Proxies in front of the platform API must allow a request of tens of seconds near
  the limit; at most `erp.core.tenant.export.max-concurrent` exports run at once per node (429 `TENANT_EXPORT_BUSY`). Archives
  stay until deleted (no automatic retention yet: delete them once downloaded). To include an application's own tenant tables, implement
  `com.erp.tenant.TenantExportContributor` as a bean: `moduleCode()` (the ZIP folder, `^[A-Z][A-Z0-9_]{0,31}$`, unique —
  not one of core's `AUDIT CU FILE MDL NOTIF SEC SEQUENCE TENANT`), `countRows(tenantId)` (the rows `export` will write)
  and `export(TenantExport export)`, e.g.

  ```java
  @Component
  public class ShopTenantExportContributor implements TenantExportContributor {
      private static final List<String> ORDER_COLUMNS = List.of("ID", "ORDER_NO", "TOTAL", "CREATED_AT");
      private final JdbcTemplate jdbc;
      public ShopTenantExportContributor(DataSource dataSource) { this.jdbc = TenantExportJdbc.streaming(dataSource); }
      public String moduleCode() { return "SHOP"; }
      public long countRows(Long tenantId) { return TenantExportJdbc.countOfTenant(jdbc, tenantId, "SHOP_ORDER"); }
      public void export(TenantExport export) {
          export.csv("SHOP_ORDER", ORDER_COLUMNS, rows -> jdbc.query(
              TenantExportJdbc.selectOfTenant(ORDER_COLUMNS, "SHOP_ORDER", "ID"), rows::addRow, export.tenantId()));
      }
  }
  ```

  Both methods run inside `TenantContext.callAs(tenantId)` in one read-only snapshot transaction: never write, name
  `TENANT_ID` in every statement, stream (never load a table into memory), order by the primary key, and never export
  a secret (password or token hashes, credentials, file bytes).
- **Number series.** Inject `com.erp.sequence.crossmodule.NumberSeriesApi` (`next(code)`,
  `preview(code)`). Seed series in a `V1000+` script for `PLATFORM`; new tenants receive copies with the
  counter at 1. The pattern tokens are `{PREFIX} {YYYY} {YY} {MM} {SEQ:n} {TENANT}`, and the reset policy
  is `NEVER`, `YEARLY` or `MONTHLY`.

  ```sql
  INSERT INTO CORE_NUMBER_SERIES (ID, TENANT_ID, CODE, PREFIX, PATTERN, RESET_POLICY, CREATED_BY)
  VALUES (nextval('SEQ_CORE_NUMBER_SERIES'), 1, 'SALES_INVOICE', 'INV', '{PREFIX}-{YYYY}-{SEQ:6}', 'YEARLY', 'system');
  ```

  Series can also be managed over HTTP (`/api/v1/sequence/series`, `PERM_SEQUENCE_SERIES_MANAGE`). An
  unknown or inactive code raises `SEQUENCE_NOT_CONFIGURED`. `next()` runs in `REQUIRES_NEW`, so size the
  connection pool for it.
- **Settings.** Inject `com.erp.cu.crossmodule.SettingsApi`. It offers `get`, `find` and
  `getOrDefault`, typed as `String`, `Integer`, `Long`, `Boolean`, `BigDecimal` or `Duration`. A key is
  resolved from the tenant override first, then the platform default (`TENANT_ID` NULL), then
  `SETTING_NOT_FOUND`. The application owns its keys; core defines none that it requires. Write them
  through `/api/v1/common/configurations` (`?scope=PLATFORM` from the PLATFORM tenant for defaults) so
  the `erpCoreSettings` cache is evicted. A `V1000+` script may seed platform defaults, because the cache
  is empty at startup.

## 4. Permissions — `PermissionContributor`

Declare a `@Component` that implements `com.erp.sec.permission.PermissionContributor`. It holds the
authority constants that your own `@PreAuthorize` expressions reference:

```java
@Component
public class ShopPermissions implements PermissionContributor {
    public static final String PERM_SHOP_ORDERS_VIEW = "PERM_SHOP_ORDERS_VIEW";
    public static final String PERM_SHOP_ORDERS_CREATE = "PERM_SHOP_ORDERS_CREATE";
    private static final PermissionScreen ORDERS = new PermissionScreen("SHOP", "SHOP_ORDERS", "الطلبات", "Orders");

    @Override public List<PermissionModule> modules() { return List.of(new PermissionModule("SHOP", "المتجر", "Shop")); }
    @Override public List<PermissionScreen> screens() { return List.of(ORDERS); }
    @Override public List<PermissionDef> permissions() {
        return List.of(PermissionDef.of(ORDERS, "VIEW", "عرض"), PermissionDef.of(ORDERS, "CREATE", "إنشاء"));
    }
}

@PreAuthorize("hasAuthority(T(com.acme.shop.permission.ShopPermissions).PERM_SHOP_ORDERS_CREATE)")
```

At startup, `PermissionCatalogSynchronizer` upserts the catalog. Every super role (`SYS_ADMIN`) holds the
new permissions at once, so no grant migration is needed. Each screen needs a `VIEW` action. The authority
is `PERM_<SCREEN>_<ACTION>` unless you pass one explicitly.

**Realms and chains.** Staff endpoints are on `erpCoreSecurityFilterChain` (`@Order(100)`, any request).
Customer endpoints are on `erpCoreCustomerSecurityFilterChain` (`@Order(90)`, `/api/v1/public/**`,
`/api/v1/customers/**`; authority `ROLE_CUSTOMER`). A token from the other realm gets 403
`REALM_MISMATCH`. An application chain needs `@Order(<90)` and a `securityMatcher(...)`. Such a chain
gets neither `TenantResolutionFilter` nor `RealmEnforcementFilter`. To replace a core chain, declare a
bean with the same name and the same order.

## 5. Events — `@TransactionalEventListener`

Core publishes these events, all in `com.erp.events`: `UserCreatedEvent`, `UserStatusChangedEvent`,
`CustomerRegisteredEvent`, `CustomerVerifiedEvent`, `PasswordResetRequestedEvent`, `TenantCreatedEvent`,
`FileDocumentPublishedEvent`, `NotificationRequestedEvent`, `NotificationDispatchedEvent`,
`NotificationFailedEvent` and (1.3.0) `UserPasswordChangedEvent` (`userId`, `byAdmin`; NOTIF answers it with the
`STAFF_PASSWORD_CHANGED` e-mail), `TenantSuspendedEvent` (`tenantCode`, `reason`) and `TenantActivatedEvent`
(`tenantCode`) — 13 in all. Each extends `DomainEvent`, which carries `id` (the idempotency key),
`occurredAt`, `tenantId`, `actor` and `realm`. The two tenant events (1.3.0) are published by the platform's
`PATCH /api/v1/platform/tenants/{id}/status` on a real transition only; their `tenantId` is the tenant that changed
and their `actor` the platform operator. Core reacts to them itself — SEC ends the suspended tenant's sessions, NOTIF
holds a suspended tenant's queued notifications and sends them on activation — and an application may listen too
(e.g. to pause its own jobs for a suspended tenant).

```java
@Async(ErpCoreEvents.EXECUTOR)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
public void on(UserCreatedEvent event) {
    TenantContext.runAs(event.getTenantId(), () -> myService.welcome(event));   // @Transactional inside
}
```

The executor propagates the tenant and the security context to the worker thread. To publish your own
events, subclass `DomainEvent` (plain values only: no entities, no secrets) and call
`DomainEventPublisher.publish(...)` inside the writing transaction. A listener runs only after the
transaction commits.

## 6. Reports — `ReportProvider`

Expose a `@Component` that implements `com.erp.report.ReportProvider`: `code()` (upper snake case, at
most 40 characters), `moduleCode()` (at most 10), `titleAr/En`, `params()`, and
`run(params, Pageable)`, which returns a `ReportResult`. Read data through JPA/`SpecBuilder`. Native SQL
must include `TENANT_ID`. Honour `page.getPageSize()`.

The report then appears under `/api/v1/report/definitions`. It runs at `/api/v1/report/{code}/run` and
exports at `/api/v1/report/{code}/export?format=csv|json`. Its permission `<MODULE>:REPORT:<CODE>`
(`ReportAuthorities.of(module, code)`) enters the catalog automatically. Example:
`erp-app-reference/src/main/java/com/erp/app/report/AppSmokeReport.java`.

## 7. Notification channels — `ChannelProvider`

To add SMS or PUSH, or to replace EMAIL or IN_APP, define a `@Component` that implements
`com.erp.notif.channel.ChannelProvider`:
- `channel()` returns the channel name, for example `"SMS"`.
- `send(OutboundMessage)` returns a `DeliveryResult`: `sent()`, `failed(error)` (retried), or
  `skippedNoProvider(reason)`.

`send` runs on the event executor with the message's tenant set and no transaction open. An application
provider always wins over a core one for the same channel. To send, use
`com.erp.notif.crossmodule.NotificationDispatchApi`. Dispatch is asynchronous. Read the outcome from
`NotificationDispatchedEvent` / `NotificationFailedEvent` or from `NotificationLogQueryApi`.

**Production: enable the requeue job.** Set `erp.core.notif.requeue.enabled=true` and declare
`@EnableScheduling` (or call `NotificationRequeueJob.requeueStale()` from your own scheduler). The
default is `false`, and without the job a `QUEUED` notification is never retried after any of these:
- the JVM stopped while the row waited in the event executor's queue, was being sent, or waited
  between two retries;
- the event executor rejected the delivery because its queue was full (`erp.core.events.executor.queue-capacity`).
  The row stays `QUEUED` and untouched, and the dispatching call is not affected;
- a send succeeded but its outcome could not be recorded (delivery is at-least-once).

The job is safe to run while deliveries are in flight (since 1.2.0). An attempt claims its row by
setting `NEXT_ATTEMPT_AT` to now + `stale-after-minutes` while it sends. Between retries the column holds
the time the next retry is due. The job only picks rows whose `NEXT_ATTEMPT_AT` (or, before any
attempt, `CREATED_AT`) is older than `stale-after-minutes`. It also skips rows waiting in, or running
on, its own node's executor. A duplicate delivery skips a claimed row, and a row never exceeds
`erp.core.notif.retry.max-attempts` attempts, however often it is requeued. A crashed attempt is
therefore picked up between `stale-after-minutes` and twice that after it started. Keep
`stale-after-minutes` well above your slowest provider call.

## 8. File storage — `StorageProvider`

The built-in providers are `DB`, `LOCAL` and `S3`, selected by `erp.core.files.storage`. A custom
provider implements `com.erp.file.storage.StorageProvider`: `key()`,
`put(StorageTarget, InputStream, size, contentType)`, which returns a `StoredObject`, `get(ref)`,
`delete(ref)` and `publicUrl(ref)`.

Supporting a new key also needs a core migration that widens `CHK_FILE_DOCUMENT_STORAGE_PROVIDER`, so
propose it to core. Other modules read files through
`com.erp.file.crossmodule.FileDocumentLookupApi` (`isAvailable`, `publicUrl`, and since 1.3.0 `publicUrls`). Public
files are served at a stable URL once a category allows it (`allowPublic`) and the document's visibility is `PUBLIC`.
Since 1.3.0 `com.erp.file.crossmodule.FileImageStoreApi` stores a small public image for a core module
(`storePublicImage(ImageStoreRequest)` → `ImageStoreResult`, `discard(id)`): the type is detected from the bytes
(PNG, JPEG, WebP; SVG only when the request allows it and it passes a strict allow-list — logos must be plain /
optimised SVG: SVGO, Inkscape "Optimized SVG", Figma or Illustrator export, no editor metadata), the document is stored
without a category and published at once under a random slug (ADR-FILE-008). Keep
`spring.servlet.multipart.max-file-size` above the image limits (the reference app uses 15 MB): Spring's default
1 MB ceiling answers an over-size upload before the image rule can (400 `VALIDATION_ERROR` instead of the image error).

Tenant branding (1.3.0): the platform operator sets a tenant's logo (`PUT` / `DELETE
/api/v1/platform/tenants/{id}/logo`, ≤ 1 MB PNG / JPEG / WebP / plain SVG) and brand colour (`PATCH …/{id}/branding`);
the UI reads them through `GET /api/v1/tenant/me` (any signed-in user, staff or customer) and, before login,
`GET /api/v1/public/tenants/{tenantCode}/branding` (anonymous, rate-limited per address). `logoUrl` is a public file
URL: show it with `<img>` only — an SVG logo is served as an attachment with `nosniff` and a sandbox CSP. On 429
from the public branding show the platform mark alone (no error toast) and do not ask again before `Retry-After`.
Public files are cached for a day (`Cache-Control: max-age=86400, public`): every upload gets a new URL (new random
slug), but a removed or replaced logo's old URL can still be served from a browser or CDN cache for up to 24 h — always
take `logoUrl` from the branding answer, never from a remembered URL.

**Client address behind a proxy.** The branding rate limit (and the audit log's IP) use
`HttpServletRequest.getRemoteAddr()`. Behind a reverse proxy or load balancer that is the proxy's address, so every
visitor would share one budget. Set `server.forward-headers-strategy=native` together with
`server.tomcat.remoteip.internal-proxies` (a regular expression matching only your proxies' addresses): Tomcat then takes
the client from `X-Forwarded-For` only when the request comes from a listed proxy. Do not use
`server.forward-headers-strategy=framework` unless the proxy always overwrites `X-Forwarded-For`: Spring's
`ForwardedHeaderFilter` trusts the header from any client, so a caller could pick its own address and escape the limit.

## 9. Audit

Annotate an entity with `@com.erp.audit.crossmodule.Audited(entityType = "...", ignore = {...})` to record
field-level changes. Record explicit facts with
`AuditApi.record(AuditEntry.builder().action("APPROVE")...build())` inside the business transaction.
Staff query the log at `GET /api/v1/audit/events` (`AUDIT:EVENT:READ`).

## 10. Tests in an application

Depend on the erp-core `test-jar` (section 1). Then
`@Import(com.erp.testsupport.TestcontainersPostgresConfiguration.class)` and feed
`com.erp.testsupport.TestPostgres` into the datasource (see `ReferenceApplicationSmokeTest`).

The database backend is selected by `ERP_TEST_DB` (or `-Derp.test.db`):
- `testcontainers`: Docker required; CI forces it;
- `embedded`: zonky PostgreSQL 16, no Docker;
- `auto`: the default; Testcontainers when Docker is reachable, otherwise embedded.

Log in as `admin` only after setting `erp.core.security.bootstrap-admin-password`. Staff login needs the
header `X-Tenant-Code: PLATFORM`.
