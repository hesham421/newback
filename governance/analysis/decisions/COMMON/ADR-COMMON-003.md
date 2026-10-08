# ADR-COMMON-003 — Base-entity hierarchy `GlobalAuditableEntity` / `AuditableEntity` with `@Version` and the global-entity allow-list

Module  : COMMON     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P2 (Database) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
Before erp-core, every entity extended one `AuditableEntity` that carried only the four audit columns
filled by `AuditEntityListener`. erp-core plan step 05 then required row-level multi-tenancy
(`TENANT_ID` on every core table, filtered by Hibernate — ADR-TENANT-001) and optimistic locking
(`VERSION`), while a few tables had to stay global: the tenant registry itself and the code-defined
permission catalog, one copy for all tenants (`erp-core-plan/05-STEP-multi-tenancy.md`;
`docs/DEVIATIONS.md` [05] "Task 3 `GlobalAuditableEntity`"). Step 09 added a fourth kind, a global
entity with its own nullable `TENANT_ID` (`AppConfiguration`: NULL = platform default). The options for
the base classes were: one base with an optional tenant (every entity would carry a nullable
`tenantId`, and Hibernate's `@TenantId` filter could not be applied selectively); a `TenantAwareEntity`
sibling next to the old `AuditableEntity` (every existing entity would have to be edited); or a parent
`GlobalAuditableEntity` (audit + version) with `AuditableEntity` (+ tenant) as its child, keeping the
name every entity already extended. Which entities may be global then had to be made explicit rather
than left to convention.

## Decision
**`GlobalAuditableEntity` (audit columns + `@Version`) is the parent of `AuditableEntity` (+ `@TenantId`);
only an explicit allow-list may extend the parent directly; the build enforces it** (as built):
- `GlobalAuditableEntity` (`@MappedSuperclass`, `@EntityListeners(AuditEntityListener)`): `CREATED_BY`
  (`updatable = false`, `length = 100`), `CREATED_AT` (`updatable = false`), `UPDATED_BY` (`length = 100`),
  `UPDATED_AT`, `@Version VERSION NOT NULL` — 0 on insert, +1 per update, never set by hand; a stale
  update is answered 409 `CONCURRENT_MODIFICATION` by the global handler
  (`erp-core/src/main/java/com/erp/common/domain/GlobalAuditableEntity.java:22-53`;
  `common/web/GlobalExceptionHandler.java:152-161`); the JPA length 100 mirrors the narrowest live
  physical width (SEC/MDL/CORE `VARCHAR(100)`; CU/NOTIF/FILE `VARCHAR(255)`) and is the true upper
  bound of a SEC username (`GlobalAuditableEntity.java:30-33`);
- `AuditableEntity extends GlobalAuditableEntity` adds `@TenantId @Column(name = "TENANT_ID",
  nullable = false, updatable = false)`; Hibernate fills it from the session tenant on insert and adds
  the predicate to every query, join and load by id; the name stayed `AuditableEntity` so every
  existing entity became tenant-scoped without an edit (`common/domain/AuditableEntity.java:12-37`);
- the physical conventions follow: `VERSION BIGINT NOT NULL DEFAULT 0` on every core table;
  `TENANT_ID BIGINT NOT NULL` (no default), `FK_<TABLE>_TENANT`, `IDX_<TABLE>_TENANT`, uniques leading
  with `TENANT_ID` (`V10__tenant_schema.sql:7-12`, `:63-147`; `db/migration/core/README.md` "Tenant
  columns (since V10)");
- the global allow-list is explicit and enforced by ArchUnit rule 2: every `@Entity` extends one of the
  two bases; only `com.erp.sec.entity.ModuleRegistry`, `ScreenRegistry`, `ActionRegistry`,
  `com.erp.tenant.entity.Tenant` and `com.erp.cu.entity.AppConfiguration` may extend
  `GlobalAuditableEntity` directly, and none of them may extend `AuditableEntity`
  (`erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:67-75`, `:110-133`;
  `docs/DEVIATIONS.md` [12] rule 2); `AppConfiguration` is the one global entity with its own nullable
  `TENANT_ID`, filtered explicitly in its queries (`docs/DEVIATIONS.md` [09]).

Reasons, from the step reports and the deviations:
1. **No edit to existing entities**: keeping the child's name made the whole code base tenant-scoped
   in one migration and one base-class field (`AuditableEntity.java:24-26`).
2. **Global stays global by declaration, not by accident**: the step files name the global tables;
   a new global entity needs a step that names it and an allow-list entry, and the build fails
   otherwise (`GlobalAuditableEntity.java:16-20`; `docs/steps/12-report.md`).
3. **One lock, one place**: `@Version` on the parent covers global and tenant-scoped rows alike, and
   one handler mapping covers every module (`docs/DEVIATIONS.md` [05] error codes entry).
4. **The listener is typed on the parent**, so the audit columns are filled for both kinds
   (`common/audit/AuditEntityListener.java:10-27`).

## Consequences
- Every new table carries six foundation columns (`CREATED_BY/AT`, `UPDATED_BY/AT`, `VERSION`,
  `TENANT_ID` unless global) and the matching FK and index; `build-create-entity` generates them.
- The two physical audit-column families (100 / 255, TIMESTAMPTZ / TIMESTAMP) are kept as recorded —
  renames and type changes are not additive (`docs/DEVIATIONS.md` [05]); the JPA mapping is one.
- A `@Version` field means every write re-reads the version; clients that update a row must send the
  current state and handle 409.
- A global entity (catalog, tenant registry, settings defaults) is readable from any tenant; its
  writers decide the tenant of its audit entry (`docs/DEVIATIONS.md` [10] "Tenant of a row").
- The tenant-maturity plan's `TenantScopedEntityTest` (C.3) restates this rule in ArchUnit; the 1.3.0
  `CORE_IDEMPOTENCY_KEY` table extends `AuditableEntity` (tenant-scoped), and a global entity would
  need an allow-list entry.

## Traces
ENT-COMMON-001, ENT-COMMON-002 · REQ-COMMON-005, REQ-COMMON-013, REQ-COMMON-014, REQ-COMMON-015 ·
POL-COMMON-005, POL-COMMON-006 · docs/DEVIATIONS.md [05] (GlobalAuditableEntity, V10 exceptions, error
codes), [09] (AppConfiguration global), [12] (rule 2); ADR-TENANT-001; DBF-TENANT-011 … DBF-TENANT-032
