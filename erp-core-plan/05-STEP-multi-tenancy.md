# Step 05 — Multi-tenancy (row-level, shared schema)

**Branch:** `step/05-multi-tenancy`
**Goal:** every core row belongs to a tenant; every query is automatically filtered by the current tenant; tenants can be provisioned; cross-tenant access is impossible by construction. SaaS deployment = one database, many tenants. Single-customer deployment = one tenant.
**Why:** the audit found no tenant concept anywhere (`AuditableEntity` has only created/updated columns). Adding it now touches 5 modules; adding it after business modules exist touches everything.

## Preconditions
- Step 04 merged (clean chain `V1..V9`).

## Design (fixed decisions)
- **Model:** shared schema, discriminator column `TENANT_ID BIGINT NOT NULL` on every tenant-scoped table.
- **Mechanism:** Hibernate discriminator multi-tenancy — `@TenantId` on a field of the base entity + a `CurrentTenantIdentifierResolver` bean. Hibernate then adds `WHERE tenant_id = ?` to every entity query and sets the value on insert. No manual `@Filter` enabling per request.
- **Context:** `com.erp.tenant.TenantContext` (ThreadLocal<Long>, plus `runAs(tenantId, Runnable)` for system jobs). Cleared in a `finally` by the resolving filter.
- **Resolution order (web):** (1) JWT claim `tid` when authenticated; (2) header `X-Tenant-Code` resolved to id via `CORE_TENANT` for public/unauthenticated endpoints; (3) otherwise request is rejected `400 TENANT_REQUIRED` unless the path is in `erp.core.tenant.exempt-paths` (defaults: actuator, api-docs, swagger, `/api/v1/platform/**`).
- **Platform tenant:** `CORE_TENANT.ID = 1, CODE = 'PLATFORM'` seeded; the bootstrap admin belongs to it. Platform-level operations (create tenant) are exposed under `/api/v1/platform/tenants` and require authority `PLATFORM_TENANT_MANAGE` (seeded to the bootstrap admin's role).
- **Uniqueness:** every unique constraint on tenant-scoped tables becomes composite with `TENANT_ID` (e.g. `SEC_USER(TENANT_ID, USERNAME)`, `SEC_USER(TENANT_ID, EMAIL)`, `MDL_LOOKUP_TYPE(TENANT_ID, KEY)`, `CU_APP_CONFIGURATION(TENANT_ID, CONFIG_KEY)`, `NOTIF_TEMPLATE(TENANT_ID, CODE)`, `SEC_ROLE(TENANT_ID, CODE)`).
- **Not tenant-scoped (global):** `SEC_MODULE_REG`, `SEC_SCREEN_REG`, `SEC_ACTION_REG` (the permission catalog is code-defined, same for all tenants). Everything else in core is tenant-scoped.
- **Optimistic locking:** add `VERSION BIGINT NOT NULL DEFAULT 0` + `@Version` to the base entity in the same migration (cheap now).

## Tasks
1. **Package `com.erp.tenant`** (add `com.erp.tenant` to `CORE_PACKAGES` in `ErpCoreAutoConfiguration`): `Tenant` entity (`CORE_TENANT`: ID, CODE unique, NAME_AR, NAME_EN, STATUS_CODE, audit columns — this table itself has no `TENANT_ID`; the entity extends `GlobalAuditableEntity` from task 3), `TenantRepository`, `TenantService` (create/activate/suspend, code validation `^[A-Z0-9_]{3,32}$`), `TenantContext`, `TenantResolutionFilter` (ordered before `JwtAuthenticationFilter`'s authorities resolution but after JWT parsing — implement by extending the JWT filter to set the context from the claim, and a separate `OncePerRequestFilter` for the header path), `CurrentTenantIdentifierResolver` impl (`resolveCurrentTenantIdentifier()` returns `TenantContext.require()`; `validateExistingCurrentSessions=false`), `PlatformTenantController` (`POST /api/v1/platform/tenants`, `GET`, `PATCH status`).
2. **Base entity**: rename `AuditableEntity` → keep the class, add fields `@TenantId @Column(name="TENANT_ID", nullable=false, updatable=false) Long tenantId;` and `@Version @Column(name="VERSION") Long version;`. Add `TenantAwareEntity` as an alias? **No** — single base class, keep the name `AuditableEntity` to avoid churn; document that it is tenant-aware.
3. **Global entities**: `SecModuleReg`, `SecScreenReg`, `SecActionReg` must **not** extend the tenant-aware base; introduce `GlobalAuditableEntity` (audit columns + version, no tenant) and switch these three.
4. **Migration `V10__tenant_schema.sql`** (additive): create `CORE_TENANT`, seed PLATFORM; add `TENANT_ID BIGINT NOT NULL DEFAULT 1` and `VERSION BIGINT NOT NULL DEFAULT 0` to every tenant-scoped table; add FK to `CORE_TENANT`; drop the `DEFAULT 1` after backfill (`ALTER COLUMN ... DROP DEFAULT`) so the application must always supply it; replace unique constraints with composite ones; add index `(TENANT_ID)` on every scoped table. Seed `PLATFORM_TENANT_MANAGE` action + grant.
5. **JWT**: add claim `tid` at token creation (`sec` login); `JwtAuthenticationFilter` sets `TenantContext` from the claim before loading the user (user lookup is itself tenant-filtered). Login endpoint: resolves tenant from header `X-Tenant-Code` (required for login) — the username is unique per tenant, not globally.
6. **Sessions / rate limiting**: `SEC_ACTIVE_SESSION` is tenant-scoped (gets `TENANT_ID` like the rest). bucket4j login buckets keyed by `tenantCode + ':' + username`.
7. **System jobs**: anywhere core runs code outside a request (none today; `notif` becomes async in step 08) must wrap with `TenantContext.runAs(...)`. Add `TenantContext.require()` that throws `TENANT_CONTEXT_MISSING` to catch mistakes early.
8. **Search/Spec**: no change needed (Hibernate applies the discriminator). Add a regression test that a `SpecBuilder` query cannot see another tenant's rows.
9. **i18n**: codes `TENANT_REQUIRED`, `TENANT_NOT_FOUND`, `TENANT_SUSPENDED`, `TENANT_CONTEXT_MISSING`, `TENANT_CODE_INVALID`.
10. **Tests** (`erp-core/src/test/java/com/erp/tenant/`): provisioning API happy/failure; isolation test — create users in tenant A and B, authenticate as A, `GET /api/v1/sec/users` never returns B's rows, `GET /api/v1/sec/users/{idOfB}` → 404; login without `X-Tenant-Code` → 400; suspended tenant login → 403; `runAs` works outside a request.
11. **Reference app**: `application.yml` adds nothing; smoke test creates a second tenant via the platform endpoint and logs in with `X-Tenant-Code`.

## Acceptance
- All tests green including the isolation tests.
- `grep -rn "extends AuditableEntity" erp-core/src/main` covers every `@Entity` except the three `Sec*Reg` (which extend `GlobalAuditableEntity`) and `Tenant`.
- Every scoped table has `TENANT_ID NOT NULL` without default (verify with a test that inspects `information_schema.columns`).
- No query path exists that bypasses Hibernate with raw SQL lacking `tenant_id` (grep for `nativeQuery = true` and `JdbcTemplate`; each occurrence must include the tenant predicate or be in `tenant`/platform code).

## Verification commands
```bash
mvn -q verify
grep -rn "nativeQuery = true\|JdbcTemplate" erp-core/src/main || echo OK-no-raw-sql
```

## Commit
`step(05): row-level multi-tenancy via Hibernate @TenantId, TenantContext, tenant provisioning API, composite uniqueness, optimistic locking`

## Out of scope
Schema-per-tenant, PostgreSQL RLS, tenant billing, per-tenant feature flags.
