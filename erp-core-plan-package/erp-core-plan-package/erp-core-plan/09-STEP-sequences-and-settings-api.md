# Step 09 — Number series (sequences) and typed, tenant-aware settings API

**Branch:** `step/09-sequences-settings`
**Goal:** (a) any module or app can obtain gap-free, tenant-scoped, formatted document numbers (`INV-2026-000123`) from a configurable series; (b) `cu` settings are readable through a typed, cached, tenant-aware API with platform defaults and tenant overrides.
**Why:** the only numbering code was fin-specific (`JournalDocNoGenerator`); `cu` offers CRUD over `CU_APP_CONFIGURATION` but no typed read API, no cache and no tenant scope.

## Preconditions
- Step 05 merged.

## Design (fixed decisions)

### Sequences (`com.erp.sequence`)
- Table `CORE_NUMBER_SERIES`: ID, TENANT_ID, CODE (e.g. `SALES_INVOICE`), PREFIX, PATTERN (default `{PREFIX}-{YYYY}-{SEQ:6}`), RESET_POLICY ∈ {`NEVER`,`YEARLY`,`MONTHLY`}, PERIOD_KEY (e.g. `2026` / `2026-03` / `''`), NEXT_VALUE BIGINT, IS_ACTIVE, audit, version. Unique `(TENANT_ID, CODE, PERIOD_KEY)`.
- API: `NumberSeriesApi.next(String code)` → `String`; `NumberSeriesApi.preview(code)`; admin CRUD `/api/v1/sequence/series` (STAFF, permission `SEQUENCE:SERIES:MANAGE`).
- Concurrency: `SELECT ... FOR UPDATE` on the series row in a `REQUIRES_NEW` transaction so that numbers are allocated atomically; the caller's transaction rollback may leave gaps — accepted and documented (gap-free *within* the allocation, not across rolled-back business transactions; strict gap-free is a business-module concern).
- Pattern tokens: `{PREFIX}`, `{YYYY}`, `{YY}`, `{MM}`, `{SEQ:n}` (zero-padded), `{TENANT}` (tenant code). Unknown token → `SEQUENCE_PATTERN_INVALID` at save time.
- When a series does not exist for `code` the API throws `SEQUENCE_NOT_CONFIGURED` (no implicit creation) — apps seed their series in their own migration (`V1000+`).

### Settings (`cu`)
- `CU_APP_CONFIGURATION` gets `TENANT_ID` **nullable**: `NULL` = platform default, non-null = tenant override. (This is the one deliberate exception to "every scoped table has NOT NULL tenant"; the entity does **not** use `@TenantId`; it extends `GlobalAuditableEntity` and filters explicitly.) Unique `(COALESCE(TENANT_ID,0), CONFIG_KEY)` via a unique index on an expression.
- `SettingsApi` (`com.erp.cu.crossmodule`): `String get(key)`, `<T> T get(key, Class<T>)` for `String|Integer|Long|Boolean|BigDecimal|Duration`, `Optional<T> find(...)`, `T getOrDefault(...)`. Resolution: tenant override → platform default → `NoSuchSettingException` (`SETTING_NOT_FOUND`).
- Cache: Spring Cache `erpCoreSettings`, key `tenantId:key`, evicted on write through the existing CRUD. Works with `simple` and Redis cache types.
- Existing CRUD endpoints stay; add `scope` query param (`PLATFORM|TENANT`) — writing platform defaults requires `PLATFORM_SETTINGS_MANAGE` (platform tenant only).

## Tasks
1. Migration `V14__sequence_and_settings.sql`: create `CORE_NUMBER_SERIES`; alter `CU_APP_CONFIGURATION` (nullable tenant, expression unique index, drop old unique on key).
2. Implement `com.erp.sequence` (entity, repository with `@Lock(PESSIMISTIC_WRITE)` finder, `NumberSeriesService` with `Propagation.REQUIRES_NEW`, pattern formatter, controller, `SequencePermissions` contributor).
3. Implement `SettingsApi` + cache + scope handling in `cu`; `CuPermissions` adds `PLATFORM_SETTINGS_MANAGE`.
4. Register `erpCoreSettings` cache name in the auto-configuration (`CacheManagerCustomizer` adding the name when the manager is `simple`).
5. **i18n**: `SEQUENCE_NOT_CONFIGURED`, `SEQUENCE_PATTERN_INVALID`, `SETTING_NOT_FOUND`, `SETTING_TYPE_MISMATCH`.
6. **Tests**: 50 threads calling `next("T")` produce 50 unique consecutive numbers; `YEARLY` reset creates a new period row; pattern rendering table test; tenant A and B each start at 1; settings resolution order (override beats default), cache hit (repository mock invoked once), eviction on update, type conversion errors.

## Acceptance
- Tests green; concurrency test stable across 3 consecutive runs.
- `NumberSeriesApi` and `SettingsApi` are in `crossmodule` packages and allowed by the ArchUnit rules.

## Commit
`step(09): tenant-scoped number series with patterns and reset policies; typed cached SettingsApi with platform defaults and tenant overrides`

## Out of scope
Per-branch/per-warehouse series (apps compose the code, e.g. `SALES_INVOICE_BR01`), strict gap-free across rollbacks.
