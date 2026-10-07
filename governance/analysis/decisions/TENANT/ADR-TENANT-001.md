# ADR-TENANT-001 — Discriminator (row-level) multi-tenancy over schema-per-tenant

Module  : TENANT     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P2 (Database) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
erp-core is one library serving many organisations. Its audit before step 05 found no tenant concept at
all (`AuditableEntity` carried only the created/updated columns); adding one after business modules exist
would touch every table, so the erp-core plan made it step 05, while only the core modules existed
(`erp-core-plan/05-STEP-multi-tenancy.md` "Why"). The goal set there: "every core row belongs to a tenant;
every query is automatically filtered by the current tenant; tenants can be provisioned; cross-tenant
access is impossible by construction. SaaS deployment = one database, many tenants. Single-customer
deployment = one tenant."

Three models were available:
- **Database per tenant** — strongest isolation, but one connection pool and one Flyway run per tenant,
  and provisioning a tenant means creating a database.
- **Schema per tenant** — one database, a schema per tenant; Hibernate's schema multi-tenancy needs a
  connection provider that switches `search_path`, Flyway must migrate every schema on every release, and
  the shared, code-defined permission catalog would be copied into every schema or kept in a separate one.
- **Shared schema with a discriminator column** — one schema, a `TENANT_ID` column on every tenant-scoped
  table; Hibernate's `@TenantId` adds the tenant predicate to every query, join and load by id and fills
  the column on insert.

The plan lists "multi-DB/schema-per-tenant" and "PostgreSQL RLS" as out of scope for the whole erp-core
plan (`erp-core-plan/00-README-EXECUTION-PLAN.md` §6) and as the step's own out-of-scope items.

## Decision
**Shared schema, discriminator column** (as built in V10 and `com.erp.tenant`):
- every tenant-scoped table carries `TENANT_ID BIGINT NOT NULL` without default, a FK to
  `CORE_TENANT(ID)` and an index, and every unique constraint leads with `TENANT_ID`
  (`V10__tenant_schema.sql:63-138`, `:174-203`; 22 columns today, `../../modules/TENANT/P2/db-script-tenant.md`);
- the tenant-aware base entity `AuditableEntity` carries `@TenantId tenantId` (Hibernate discriminator
  multi-tenancy) and the `CurrentTenantIdentifierResolver` returns the request tenant from `TenantContext`
  (`erp-core/src/main/java/com/erp/common/domain/AuditableEntity.java:35-37`,
  `erp-core/src/main/java/com/erp/tenant/config/TenantIdentifierResolver.java:36-42`);
- `isRoot` stays `false`: no tenant, PLATFORM included, reads another tenant's rows;
- the permission catalog (`SEC_MODULE_REG`, `SEC_SCREEN_REG`, `SEC_ACTION_REG`) and `CORE_TENANT` are
  global tables, one copy for all tenants (05-STEP "Not tenant-scoped (global)");
- the few raw-SQL paths (tenant provisioning contributors, sequence, audit, the NOTIF requeue job) name
  `TENANT_ID` in every statement, enforced by the ArchUnit raw-JDBC allow-list (docs/DEVIATIONS.md [12]
  rule 7).

Reasons, from the step-05 file (fixed decisions, "Why", "Out of scope"), its report
(`docs/steps/05-report.md`) and the code they produced — the step file states the model as a fixed
decision and gives no longer comparison of the alternatives above:
1. **No manual filtering.** Hibernate applies the discriminator to every HQL/criteria query, every join
   and every load by id (`_tenantId` filter with `applyToLoadByKey`); search specifications need no change
   (05-STEP task 8) — isolation holds by construction, not by each query remembering it.
2. **One schema, one migration chain.** A single Flyway chain `V1..V999` migrates all tenants at once; a
   new tenant is a row plus copied reference rows (`TenantProvisioningContributor`), not a new schema.
3. **Global catalog stays global.** The code-defined permission catalog is one set of rows joined by every
   tenant, never copied (05-STEP; SEC `SecTenantProvisioningContributor` joins it).
4. **Cheap now, expensive later.** Five core modules existed; the change was one additive migration (two
   plan-sanctioned exceptions: `DROP DEFAULT` and composite uniques under the same names) and one base-class
   field.
5. **Both deployment shapes.** SaaS (many tenants, one database) and a single-customer installation (only
   PLATFORM or one tenant) run the same code.

## Consequences
- Isolation depends on every access going through Hibernate with a tenant set: a session without a tenant
  fails fast with `TENANT_CONTEXT_MISSING` after start-up (REQ-TENANT-017); raw SQL must name `TENANT_ID`
  (RULE-TENANT-008); a cache over tenant data must include the tenant in its key
  (docs/steps/05-report.md "Notes for later steps").
- Cross-tenant foreign keys (a row pointing at another tenant's row) are prevented by the application,
  because loads are tenant-filtered — not by composite FKs; there is no database-level RLS.
- Per-tenant backup, restore or export is a filtered copy by `TENANT_ID`, not a schema dump (relevant to
  the tenant-maturity plan item C.5).
- A noisy tenant shares the same tables and indexes with all others; every tenant-scoped table is indexed
  on `TENANT_ID`.
- Revisiting this choice (schema- or database-per-tenant, or RLS on top) is a MAJOR change of the core
  tables and is not planned.

## Traces
ENT-TENANT-001 · REQ-TENANT-016, REQ-TENANT-017, REQ-TENANT-020 · RULE-TENANT-008 · POL-TENANT-007 ·
DBF-TENANT-011 … DBF-TENANT-032
