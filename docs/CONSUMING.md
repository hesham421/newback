# Consuming erp-core

`com.erp:erp-core` is an auto-configured Spring Boot 4 library (Java 21, PostgreSQL 16). An application
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
  <java.version>21</java.version>
  <erp.core.version>1.0.0</erp.core.version>
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
| `spring.datasource.url` / `username` / `password` | PostgreSQL 16. Flyway applies the core chain first. |
| `erp.core.security.jwt.secret` | HMAC key for access tokens, at least 32 bytes. Startup fails if it is blank. |
| `erp.core.files.access-token-secret` | Secret for the FILE download-token key. Startup fails if it is blank. |

`spring.jpa.hibernate.ddl-auto` must stay `none` (Flyway owns the schema), and `spring.jpa.open-in-view`
should be `false`.

### Usually set

| Property | Default | What |
|---|---|---|
| `erp.core.security.bootstrap-admin-password` | empty | Sets the password of the seeded `admin` (SYS_ADMIN of tenant `PLATFORM`) once, on first start, and activates it. Without it nobody can log in as `admin`; there is no `admin/admin`. |
| `erp.core.security.jwt.expiration-ms` | `3600000` | Access-token lifetime. |
| `erp.core.frontend.base-url`, `password-reset-path`, `customer-verify-path`, `customer-password-reset-path` | — / `/reset` / `/customer/verify` / `/customer/reset` | Links in e-mails. |
| `erp.core.security.public-paths`, `customer-public-paths` | see `ErpCoreProperties.Security.DEFAULT_*` | Unauthenticated paths of the staff and customer chains. **Setting one replaces the whole list**, so start from the defaults. |
| `erp.core.tenant.exempt-paths`, `path-tenant-paths` | see `ErpCoreProperties.Tenant.DEFAULT_*` | Paths served without a tenant, and paths whose tenant comes from a `{tenantCode}` path variable. |
| `erp.core.files.storage` | `DB` | `DB`, `LOCAL` (`erp.core.files.local.root`) or `S3` (`erp.core.files.s3.*`). |
| `erp.core.files.max-content-bytes` / `max-request-bytes` / `public-base-url` | 5 MB / 10 MB / empty | Upload limits and the origin of public file URLs. |
| `erp.core.notif.retry.*` | 5 attempts, 2 s doubling, 32 s maximum | Asynchronous delivery retries. |
| `erp.core.notif.requeue.enabled` / `stale-after-minutes` / `interval-ms` | `false` / `10` / `60000` | Requeue job for stale `QUEUED` notifications. It runs only if the application enables scheduling. |
| `erp.core.events.executor.*` | 4 / 16 / 500 / `erp-event-` | Event worker pool. |
| `erp.core.security.customer-login-rate-limit.capacity` / `period` | `10` / `1m` | Customer login limit per `tenant:realm:username`. |
| `erp.core.audit.retention-days` / `retention-cron` | `0` (keep) / `-` (off) | Audit retention. The cron fires only if the application enables scheduling. |
| `erp.core.report.max-export-rows` | `100000` | Export cap. Above it the export answers 422 `REPORT_EXPORT_TOO_LARGE`. |

### Optional infrastructure

Each piece is off unless the application adds the dependency and its configuration.

| Infrastructure | Add | Effect without it |
|---|---|---|
| Redis | `spring-boot-starter-data-redis` + `spring.data.redis.*` | FILE download tokens use the in-memory store (single node only). `spring.cache.type=redis` also works for the settings cache. |
| SMTP | `spring-boot-starter-mail` + `spring.mail.host` (and credentials) | No `EMAIL` channel provider exists, so EMAIL notifications end `SKIPPED_NO_PROVIDER`. |
| S3 / S3-compatible | `software.amazon.awssdk:s3` + `erp.core.files.storage=S3`, `erp.core.files.s3.bucket` (`region`, `endpoint`, `access-key`, `secret-key`, `public-base-url`) | `S3` cannot be selected. Startup fails with a message naming the property if it is selected anyway. |
| Scheduling | `@EnableScheduling` on an application `@Configuration` | Core never enables scheduling. Without it the requeue and retention triggers never fire. You can call `NotificationRequeueJob.requeueStale()` and `AuditRetentionJob.run()` yourself. |

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
- **Reference data for new tenants.** If the application seeds reference data that every tenant needs,
  it implements `com.erp.tenant.TenantProvisioningContributor` (`order()`,
  `provision(TenantProvisioning)`). Use JDBC with explicit `TENANT_ID` and copy from the source tenant.
  Provisioning (`POST /api/v1/platform/tenants`) runs it in the same transaction.
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
`FileDocumentPublishedEvent`, `NotificationRequestedEvent`, `NotificationDispatchedEvent` and
`NotificationFailedEvent`. Each extends `DomainEvent`, which carries `id` (the idempotency key),
`occurredAt`, `tenantId`, `actor` and `realm`.

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

## 8. File storage — `StorageProvider`

The built-in providers are `DB`, `LOCAL` and `S3`, selected by `erp.core.files.storage`. A custom
provider implements `com.erp.file.storage.StorageProvider`: `key()`,
`put(StorageTarget, InputStream, size, contentType)`, which returns a `StoredObject`, `get(ref)`,
`delete(ref)` and `publicUrl(ref)`.

Supporting a new key also needs a core migration that widens `CHK_FILE_DOCUMENT_STORAGE_PROVIDER`, so
propose it to core. Other modules read files through
`com.erp.file.crossmodule.FileDocumentLookupApi` (`isAvailable`, `publicUrl`). Public files are served at
a stable URL once a category allows it (`allowPublic`) and the document's visibility is `PUBLIC`.

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
