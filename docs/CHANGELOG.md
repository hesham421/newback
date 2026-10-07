# Changelog

All notable changes to `com.erp:erp-core` (and the `erp-app-reference` consumer). Versioning policy:
`docs/RELEASE.md`. Per-step details: `docs/steps/NN-report.md`; deviations: `docs/DEVIATIONS.md`.

## [Unreleased]

### Added
- Shared helpers in `com.erp.common`, replacing copies that lived in two or more modules (no
  behaviour change):
  - `web.FilterErrorResponseWriter`: the hand-written error envelope of the SEC and tenant filters.
  - `search.InstantFieldValueConverter`: ISO-8601 filter values to `Instant` (AUDIT, SEC).
  - `util.SecurityContextHelper.currentCaller()` / `currentActorOrSystem()` / `currentRealm()` and the
    `REALM_*` / `CUSTOMER_AUTHORITY` constants, which `DomainEvent` and `AuditApi` now alias.
  - `lookup.LookupOptionResponse` and `lookup.OwnedLookups`: the MDL-backed lookup endpoints of FILE
    and NOTIF (same JSON shape).
  - `domain.StatusTransitions` (FILE document, NOTIF log) and `domain.DomainRules`
    (`assertUnique` / `assertNotBlank`, used by the Domain objects).
  - `util.Strings.truncate`, `util.UtcDates.startOfDay`, `util.PlainJson.MAPPER`,
    `util.TokenHasher.sha256Hex(byte[])`.
- `TenantContext.isPlatform()`.
- [TM-G] SEC: revoke a single grant of a role. `DELETE /api/v1/sec/roles/{id}/screens/{screenId}` removes the
  screen grant and the role's action grants on that screen (RULE-SEC-054) and answers
  `ScreenGrantRevokeResponse { revokedActionGrants }`; `DELETE /api/v1/sec/roles/{id}/actions/{actionId}` removes
  one action grant, and revoking a screen's `VIEW` also removes the role's other actions on that screen
  (RULE-SEC-055, ADR-SEC-062), answering `ActionGrantRevokeResponse { revokedActionGrants }`. Both need
  `PERM_SEC_ROLES_UPDATE`, answer 404 `SEC-404-ROLE` / `SEC-404-GRANT`, and write `SCREEN_REVOKED` /
  `ACTION_REVOKED` SEC audit entries. No migration; the module revoke is unchanged. Sessions are not ended (the
  next request sees the change); a super role keeps every authority, only its menu changes.
- [TM-D] SEC: an administrator sets a staff user's password (`PUT /api/v1/sec/users/{id}/password`,
  `PERM_SEC_USERS_UPDATE`, never one's own: 422 `SEC-422-PASSWORD-SELF`); every session of the user ends and, by
  default, the user must change it at the next sign-in. While that change is pending every STAFF call except
  `GET /api/v1/sec/me`, `PUT /api/v1/sec/me/password` and logout answers 403 `SEC-403-PASSWORD-CHANGE-REQUIRED`
  (ADR-SEC-063). New `GET/PATCH /api/v1/sec/me` (own profile, no roles: ADR-SEC-064), `PUT /api/v1/sec/me/password`
  (current password required: 403 `SEC-403-PASSWORD-CURRENT-INVALID`; the user's other sessions end), and photos
  `PUT/DELETE /api/v1/sec/me/photo`, `PUT/DELETE /api/v1/sec/users/{id}/photo` (PNG/JPEG/WebP ≤ 1 MB, public URL;
  400 `SEC-400-PHOTO-INVALID`). Migration `V16__sec_user_profile.sql` (phone, job titles, preferred locale, photo
  reference, password-change flag and time). Audit actions `PASSWORD_SET_BY_ADMIN`, `PASSWORD_CHANGED`,
  `PROFILE_PHOTO_CHANGED`; new core event `UserPasswordChangedEvent`.
- [TM-D] SEC: one STAFF password policy, `erp.core.security.password-policy.*` (8..200 characters, a letter and a
  digit by default), on user create, reset completion, admin-set, own change and a new tenant's first
  administrator: 400 `SEC-400-PASSWORD-POLICY` naming the field.
- [TM-D] FILE: `FileImageStoreApi` (cross-module) stores a small public image for another module: type from the
  bytes, SVG only when allowed and free of active content, uncategorised, published under a random slug
  (ADR-FILE-008); `FileDocumentLookupApi.publicUrls(Collection)`.
- [TM-D] NOTIF: template `STAFF_PASSWORD_CHANGED` (`V17__notif_seed_password_changed.sql`, every tenant) e-mailed to
  a staff user whose password was set or changed.
- [TM-D] TENANT: `TenantLookupApi.summaryOf(tenantId)` (code and names).

### Changed
- [TM-D] SEC: users created by an administrator (`POST /api/v1/sec/users`) must change their password at the first
  sign-in unless the request says `requireChangeAtNextLogin: false`; the login response carries
  `passwordChangeRequired`; user requests and responses gain `phone`, `jobTitleAr`, `jobTitleEn`,
  `preferredLocale` (`ar` / `en`), responses also `photoUrl`, `passwordChangeRequired`, `passwordChangedAt`. On
  `PUT /api/v1/sec/users/{id}` an absent new field keeps its value. Completing a password reset clears a pending
  forced change. A new tenant's first administrator password must meet the policy.
- Java 25: `maven.compiler.release=25` and the enforcer now require JDK 25 or newer (was 21). The
  published jar is Java 25 bytecode, so a consuming application must also build and run on JDK 25+.
  CI, the reference app's Dockerfile and `.sdkmanrc` moved to 25 as well.

### Fixed
- `JwtAuthenticationFilter` no longer puts a tenant left on a reused worker thread back after the
  request: the thread leaves the filter with no tenant, so a container error dispatch after
  `sendError` (which skips the filter) can no longer run under the stale tenant.

## [1.2.0] — 2026-10-05

MINOR release. It is a MINOR, not a PATCH, because `com.erp.common` gained public members (see Added),
and `docs/RELEASE.md` allows additions only in a MINOR. Nothing in any `crossmodule` package or SPI
changed. Compared with 1.1.0 there are no migrations, no table or column changes and no `erp.core.*`
property changes. Three error responses visible to REST clients change from 500 (see Fixed). Details:
`docs/steps/15-report.md` and the `[15]` entries in `docs/DEVIATIONS.md`.

### Added
- `CommonErrorCodes.NOT_FOUND` in `com.erp.common.exception`: a generic 404 for an unknown path, with
  English and Arabic messages.
- `GlobalExceptionHandler.handleNoResource(NoResourceFoundException)` in `com.erp.common.web`: answers
  an unknown path with `404 NOT_FOUND`.

### Fixed
- NOTIF delivery claims a row before it sends, using `NOTIF_LOG.NEXT_ATTEMPT_AT` as a lease
  (now + `erp.core.notif.requeue.stale-after-minutes`, at least 1 minute). No column or property was
  added.
  - Under a backlog, the requeue job and a duplicate run never send a row that is in flight or waiting
    between retries, so a notification is not sent twice. Concurrent claims are serialized by the
    optimistic lock; the loser ends quietly.
  - When the event executor's queue is full, the rejection is caught. The row stays `QUEUED` and
    untouched, and the requeue job delivers it once it is stale. Before, the rejection escaped into the
    dispatching caller.
  - `ATTEMPTS` never exceeds `erp.core.notif.retry.max-attempts`, however often a row is requeued. The
    last failed attempt fails the row at once, and a requeued row with no attempts left is failed without
    another send. Before, a requeued row could reach twice the maximum.
  - When recording an outcome fails after a send (a database error), the delivery run retries at once
    (at-least-once). It no longer waits behind its own lease for the requeue job.
- An unknown path answers `404 NOT_FOUND` instead of `500 INTERNAL_ERROR` (authenticated, or anonymous
  under a permitted prefix; an anonymous request under a protected prefix still gets 401).
- A search whose `page` offset overflows `int` (e.g. `page=2147483647`) answers `400 VALIDATION_ERROR`
  with `fieldErrors[0].field = page` instead of `500 INTERNAL_ERROR`.
- An exception that wraps a `LocalizedException` is answered with that exception's own code and status
  instead of `500 INTERNAL_ERROR`. `TENANT_CONTEXT_MISSING` (e.g. inside
  `CannotCreateTransactionException`) is still a 500, now with its own code.
- The tenant resolver becomes strict before the web server starts accepting requests, not only on
  `ContextRefreshedEvent`.
- A `TenantContext` left on a request thread is logged and cleared when the request starts, and a
  rejected token clears the tenant.

### Docs
- `docs/CONSUMING.md` §7: in production, enable `erp.core.notif.requeue.enabled=true` and scheduling
  (`@EnableScheduling`), and why. The default stays `false`.

## [1.1.0] — 2026-10-05

MINOR release. It is a MINOR, not a PATCH, because the public NOTIF types gained additive members (see
Added), and `docs/RELEASE.md` allows additions only in a MINOR. Compared with 1.0.0 there are no
migrations, no table or column changes and no `erp.core.*` property changes. Two behaviour fixes are
visible to REST clients (see Security and Fixed below).

**Note:** `v1.0.0` was tagged but never published, because its CI `build-test` job failed (so `publish`
and `consume-published` were skipped). 1.1.0 is the first published version of erp-core. A published
version is never re-tagged (`docs/RELEASE.md`).

### Security
- The staff user, session and dashboard APIs are now limited to the STAFF realm. Before, a staff admin
  could deactivate and then reactivate an unverified customer, which bypassed customer verification, and
  could edit a customer account or assign roles to it.
  - A CUSTOMER account id on the staff by-id user endpoints (get, update, assign roles, deactivate,
    reactivate) answers `404 SEC-404-USER`, the same as an unknown id. A customer session id on
    terminate-session answers `404 SEC-404-SESSION`.
  - Staff user search and the active-sessions list never include customers.
  - The security dashboard's user and session counts cover STAFF accounts only, so the counts change
    for tenants that have customers.

### Added
- `DeliveryStatus.REJECTED` and `DeliveryResult.rejected(String)` in `com.erp.notif.channel`: a
  permanent failure that NOTIF never retries.
- `RecipientDirectory.emailOf(Long)` in `com.erp.notif.crossmodule`: the recipient's account e-mail.
  The method has no default implementation, so any implementation of `RecipientDirectory` outside
  erp-core must add it. erp-core's own implementation is `SecRecipientDirectory`.

### Fixed
- NOTIF EMAIL dispatch now uses the recipient's account e-mail from the user directory when
  `variables.email` is absent. `variables.email` still overrides it when present. An EMAIL that has no
  address at all is terminal: the provider returns `REJECTED`, and the row ends `FAILED` after one
  attempt (`attempts=1`) instead of being retried.
- `BootstrapAdminPasswordIntegrationTest` was order-dependent: its raw-SQL `SYS_ADMIN` role query saw
  the `admin` of every tenant other test classes had provisioned. The query is now scoped to the PLATFORM
  tenant; the assertion is unchanged (test-only).
- Surefire `runOrder` is pinned to `${surefire.runOrder}`, default `alphabetical`, so CI (Linux) runs
  the same order as Windows. The embedded test PostgreSQL uses `max_connections=100`, like the container.
- CI `build-test` publishes the root cause of a failure as public annotations
  (`.github/scripts/ci-annotate-failures.py`) and uploads `mvn-verify.log` with the surefire reports.

### Docs
- API documentation generated from the running application, under `docs/api-docs/`.
- `PublicFileController`: `@SecurityRequirements` opt-out, so the OpenAPI document shows the public-file
  endpoint as unauthenticated. OpenAPI metadata only; runtime security is unchanged.
- The governed core API test plan (`TC-CORE-*`), under `docs/test-api/`.
- The api-verify report (172/172), `docs/test-api/core-verify-report.md`. The api-docs were regenerated
  for the realm and NOTIF fixes.

## [1.0.0] — 2026-10-05

First release of erp-core as a versioned, auto-configured Spring Boot library. There are 12 plan steps.
The merge commit of each step is shown in brackets.

- **01 decouple-fin** (`e91e4e1`): removed the `fin` module completely: code, 18 migrations, seeds,
  permissions, the OpenAPI group, i18n and tests. The remaining core chain was renumbered `V1..V20`.
- **02 test-infrastructure** (`d143403`):
  - tests run against PostgreSQL 16 via Testcontainers, or an embedded PostgreSQL 16 fallback chosen
    with `ERP_TEST_DB`;
  - Redis and SMTP are optional in tests;
  - H2 was removed.
- **03 library-split-and-autoconfig** (`6e1e668`):
  - a multi-module Maven build: the `erp-core` library and the runnable `erp-app-reference`;
  - auto-configuration (`ErpCoreAutoConfiguration`, security, Flyway, OpenAPI);
  - `erp.core.*` properties bound by `ErpCoreProperties`;
  - Java 21, with Redis, SMTP and springdoc optional;
  - a test-jar for consumers.
- **04 flyway-core-chain** (`4a08319`):
  - the core chain was squashed into per-module schema and seed scripts `V2..V9`;
  - core owns `V1..V999` and applications own `V1000+`, and core scripts are additive only;
  - the bootstrap admin password comes from `erp.core.security.bootstrap-admin-password` (no more
    `admin/admin`).
- **05 multi-tenancy** (`eea2528`):
  - `TENANT_ID` on every core table, filtered by Hibernate `@TenantId`;
  - `TenantContext`, `CORE_TENANT` and tenant resolution from the token or `X-Tenant-Code`;
  - the tenant provisioning API and the `TenantProvisioningContributor` SPI;
  - composite uniqueness and optimistic locking.
- **06 auth-realms-and-permission-registry** (`3016338`):
  - STAFF and CUSTOMER realms, with customer self-registration, verification, login rate limiting and
    password reset;
  - the `PermissionContributor` catalog replaced the static constants, and `SYS_ADMIN` is a super role.
- **07 file-storage-and-public-urls** (`6c0eae1`):
  - the `StorageProvider` SPI (DB, LOCAL and S3), content hashes, and public files at stable URLs;
  - Redis-optional single-use download tokens.
- **08 events-and-async-notifications** (`de18fe9`):
  - the domain event bus (`com.erp.events`, 10 core events) with a tenant-propagating async executor;
  - event-driven, retried notification delivery, the `ChannelProvider` SPI and an in-app inbox.
- **09 sequences-and-settings-api** (`71542d3`):
  - tenant-scoped number series (`NumberSeriesApi`, patterns, reset policies);
  - the typed, cached `SettingsApi`, with platform defaults and tenant overrides.
- **10 generic-audit-log** (`779c012`):
  - the tenant-scoped `CORE_AUDIT_EVENT`, `AuditApi`, the `@Audited` entity listener (sensitive fields
    redacted), a query API and a retention job.
- **11 reporting-minimal** (`9e3efcf`):
  - the `ReportProvider` SPI and its registry, with automatic report permissions;
  - run and CSV/JSON export endpoints, and three core reference reports.
- **12 arch-rules-ci-and-release**:
  - ArchUnit rules for the library (`CoreLibraryRulesArchTest`) and an additive-only migration guard;
  - enforcer rules for dependency hygiene, and a JaCoCo gate of at least 60 % line coverage on erp-core;
  - CI that builds and tests with Testcontainers, builds the Docker image, publishes to GitHub Packages
    on a `v*` tag, and checks consumption of the published artifact;
  - `docs/RELEASE.md` and `docs/CONSUMING.md`, and the 1.0.0 release.
