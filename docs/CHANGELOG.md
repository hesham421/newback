# Changelog

All notable changes to `com.erp:erp-core` (and the `erp-app-reference` consumer). Versioning policy:
`docs/RELEASE.md`. Per-step details: `docs/steps/NN-report.md`; deviations: `docs/DEVIATIONS.md`.

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
