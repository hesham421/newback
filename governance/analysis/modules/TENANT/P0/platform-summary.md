# PLATFORM SUMMARY — المستأجرون / Tenant (TENANT)
══════════════════════════════════════════════════════════════════
Profile : erp   Source version : erp-core 1.2.0 (as built)   Analysis : v1 (as-built baseline)
Written : 2026-10-07, from the code at main @ 2274f86 (1.3.0-SNAPSHOT; the tenant code is unchanged
          since tag v1.2.0 apart from the helper move of 6b01816, no behaviour change)
══════════════════════════════════════════════════════════════════

Paths cited below are relative to the repository root, except: Java sources are relative to
`erp-core/src/main/java/com/erp/`, and core migrations (`V<N>__*.sql`) to
`erp-core/src/main/resources/db/migration/core/`. `file:line` points at main @ 2274f86.

This analysis was NOT produced before the code: the tenant module was built by erp-core plan step 05
(`erp-core-plan/05-STEP-multi-tenancy.md`, report `docs/steps/05-report.md`) and extended by steps 07,
09 and 15. It records what exists, so that every later tenant change (the tenant-maturity plan,
`docs/plans/tenant-maturity-plan.md`, packages B–E) is written as an "Implementation Addendum" on top
of it, like the other modules' 1.2.0 addenda. The full description of the platform as a whole stays in
[`../../SEC/P0/platform-summary.md`](../../SEC/P0/platform-summary.md) → "Implementation Addendum —
erp-core 1.2.0" (the single-copy decision recorded there); this file describes only the tenant's place
in it.

## OVERVIEW
المنصة متعددة المستأجرين بنمط الصفوف (row-level) في مخطط واحد مشترك: كل صف في جدول خاص بمستأجر
يحمل عمود `TENANT_ID`، ويضيف Hibernate شرط المستأجر الحالي إلى كل استعلام وكل ربط وكل تحميل بالمعرّف،
ويملأ العمود عند الإدراج. المستأجر `PLATFORM` (المعرّف 1) هو مستأجر المنصة نفسه: يملك المدير
الأوّلي وكل عمليات إدارة المستأجرين. نشر SaaS = قاعدة بيانات واحدة ومستأجرون كثيرون؛ نشر لعميل واحد
= مستأجر واحد. [`erp-core-plan/05-STEP-multi-tenancy.md` Goal; ADR-TENANT-001]

The platform is multi-tenant by discriminator column in one shared schema. Every tenant-scoped row
carries `TENANT_ID`; Hibernate's `@TenantId` restricts every query, join and load by id to the request
tenant and fills the column on insert. The `PLATFORM` tenant (id 1) is the platform itself: it owns
the bootstrap administrator and every tenant-management operation.

## THE TENANT IN THE PLATFORM (as built)
| Aspect | As built | Code location |
|---|---|---|
| Model | shared schema, discriminator `TENANT_ID BIGINT` on every tenant-scoped table, FK `FK_<TABLE>_TENANT` → `CORE_TENANT(ID)`, index `IDX_<TABLE>_TENANT`; every unique constraint of a tenant-scoped table leads with `TENANT_ID` | `V10__tenant_schema.sql:63-138`, `:174-203`; `erp-core/src/main/resources/db/migration/core/README.md` |
| Discriminator | `AuditableEntity.tenantId` — `@TenantId @Column(name = "TENANT_ID", nullable = false, updatable = false)`; Hibernate resolves the tenant when a session opens through `TenantIdentifierResolver` (registered by `TenantHibernateConfiguration`); `isRoot` stays `false`, so no tenant — PLATFORM included — reads another tenant's rows | `common/domain/AuditableEntity.java:35-37`; `tenant/config/TenantIdentifierResolver.java:28-42`; `tenant/config/TenantHibernateConfiguration.java:39-41` |
| Tenant-scoped tables | 22 `TENANT_ID` columns today: 18 added by V10 (CU 1, MDL 2, SEC 10, FILE 2, NOTIF 3) and 4 created with their table later (`SEC_CUSTOMER_VERIFY_TOKEN` V11, `NOTIF_INBOX` V13, `CORE_NUMBER_SERIES` V14, `CORE_AUDIT_EVENT` V15). 21 are `NOT NULL` without default; `CU_APP_CONFIGURATION.TENANT_ID` is nullable since V14 (NULL = platform default). Full list: `../P2/db-script-tenant.md` §1 (DBF-TENANT-011…032) | `V10__tenant_schema.sql:60-80`; `V11__sec_realms.sql:68`; `V13__notif_async_inbox.sql:40`; `V14__sequence_and_settings.sql:33`, `:66`; `V15__audit_schema.sql:18`; `erp-core/src/test/java/com/erp/tenant/TenantSchemaIntegrationTest.java:51` |
| Global (no tenant) | `CORE_TENANT` itself and the permission catalog `SEC_MODULE_REG`, `SEC_SCREEN_REG`, `SEC_ACTION_REG`; their entities extend `GlobalAuditableEntity`. `AppConfiguration` (`CU_APP_CONFIGURATION`) is a global entity with its own nullable `TENANT_ID` (platform default / tenant override) | `V10__tenant_schema.sql:44`, `:145-147`; `tenant/entity/Tenant.java:40`; `cu/entity/AppConfiguration.java:41`, `:50`; `erp-core/src/test/java/com/erp/tenant/TenantSchemaIntegrationTest.java:28-36` |
| PLATFORM tenant | `CORE_TENANT.ID = 1`, `CODE = 'PLATFORM'`, seeded by V10; owns every row that existed before V10, the bootstrap `admin`, and the platform-level screens (`PLATFORM_TENANTS`; `PLATFORM_SETTINGS` of CU) | `V10__tenant_schema.sql:55-57`; `tenant/TenantConstants.java:11`, `:14` |
| Tenant of a request | resolved by the security chain: path tenant (customer chain, public file URLs) → token claim `tid` → header `X-Tenant-Code` → none (exempt path proceeds, public path 400). Held in `TenantContext` (ThreadLocal) for the request and cleared in a `finally` | `tenant/security/TenantResolutionFilter.java:81-128`; `sec/security/JwtAuthenticationFilter.java:129-136`; `tenant/TenantContext.java:25-27` |
| Outside a request | system code (start-up runners, schedulers, async listeners) wraps its work in `TenantContext.runAs` / `callAs`; a session opened without a tenant fails fast with `TENANT_CONTEXT_MISSING` once the context has started | `tenant/TenantContext.java:47-53`, `:71-92`; `tenant/config/TenantHibernateConfiguration.java:34`, `:50-52` |
| Tenant management | `/api/v1/platform/tenants` (provision, list, get, search, change status), only for an authenticated PLATFORM-tenant caller holding `PLATFORM_TENANT_MANAGE` | `tenant/controller/PlatformTenantController.java:33`; `autoconfigure/ErpCoreSecurityAutoConfiguration.java:130-131`, `:199-205` |
| Provisioning | a new tenant is set up by every `TenantProvisioningContributor` (SEC 0, MDL 10, NOTIF 20, SEQUENCE 40) inside the transaction that inserts the `CORE_TENANT` row | `tenant/service/TenantService.java:68-103`; `tenant/TenantProvisioningContributor.java:22-31` |

## REALMS INTERPLAY
| Realm | Tenant handling | Code location |
|---|---|---|
| STAFF (core chain, every administrative path) | Login, sign-up and password reset are public paths: they need `X-Tenant-Code` (400 `TENANT_REQUIRED` without it). The issued token carries `tid` (and `realm`); usernames are unique per (tenant, realm). `/api/v1/platform/**` is tenant-exempt but needs a PLATFORM-tenant operator. No path-tenant paths on this chain. | `autoconfigure/ErpCoreSecurityAutoConfiguration.java:123-125`, `:141`; `sec/security/JwtTokenIssuer.java:46`; `autoconfigure/ErpCoreProperties.java:233-239` |
| CUSTOMER (customer chain, `/api/v1/public/**`, `/api/v1/customers/**`) | The same filter runs on this chain; customer register / verify / login / reset need `X-Tenant-Code`; customer tokens carry `tid`; the public file URLs `/api/v1/public/files/{tenantCode}/**` take the tenant from the path, and a token of another tenant is dropped there. | `autoconfigure/ErpCoreSecurityAutoConfiguration.java:169-171`, `:188`; `autoconfigure/ErpCoreProperties.java:252-261`; `tenant/security/TenantResolutionFilter.java:131-158` |
| Both | A suspended tenant's tokens are refused on the next request (403 `TENANT_SUSPENDED`), whatever the realm. The PLATFORM-module permissions are effective only inside the PLATFORM tenant (a super role elsewhere never receives them). | `tenant/security/TenantResolutionFilter.java:89-99`; `sec/service/MenuService.java:142-143` |

## DEPENDENCY MAP
```
every core module ──HARD (FK TENANT_ID → CORE_TENANT, 22 tables)──▶ TENANT
SEC, MDL, NOTIF, SEQUENCE ──SPI (TenantProvisioningContributor, XM-TENANT-002)──▶ TENANT
FILE, SEQUENCE ──crossmodule (TenantLookupApi.codeOf, XM-TENANT-001)──▶ TENANT
TENANT ──SPI (PermissionContributor: TenantPermissions)──▶ SEC
TENANT ──publishes (TenantCreatedEvent)──▶ events
TENANT ──@Audited (CORE_TENANT)──▶ audit
SEC (JwtAuthenticationFilter) ──sets TenantContext from tid──▶ TENANT (root package)
```
Build order: TENANT is created by V10, after the V2..V9 module chain, and every later core table is
born tenant-scoped. Tier: foundation (L1), beside SEC.

## DEFERRED (not in scope of the as-built module)
| Item | Reason / activation trigger |
|---|---|
| Schema-per-tenant, database-per-tenant, PostgreSQL RLS | out of scope of the erp-core plan (`erp-core-plan/00-README-EXECUTION-PLAN.md` §6; 05-STEP "Out of scope"); ADR-TENANT-001 |
| Tenant billing, per-tenant feature flags | 05-STEP "Out of scope" |
| Editing a tenant's name, deleting a tenant, a usage endpoint | not built: the code is immutable, tenants are never deleted (`PlatformTenantController` Javadoc, DEVIATIONS [05]); the tenant-maturity plan package B adds edit and usage as a 1.3.0 addendum |
| Tenant profile, suspension facts, token cut-off, lifecycle events, branding, idempotent provisioning, export | tenant-maturity plan packages B, C, E (1.3.0) |

## OPEN ITEMS
None — this file describes what exists. Behaviour the code does not have is listed under DEFERRED,
never described as present.

## NEXT STEP
Module registry and policies: `module-registry-tenant.md`, `business-policies-tenant.md`.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package B — tenant level 1 (edit, suspension facts, admin-reset, usage)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

| Kind | Aspect | Delta | Source |
|---|---|---|---|
| CHANGED | Tenant record | `CORE_TENANT` gains a profile (contact e-mail and phone, country, default language, time zone, notes; V18) and suspension facts + a token cut-off (V19); names become editable (`PUT /{id}`); `code` stays immutable | `../P2/db-script-tenant.md` 1.3.0 addendum |
| CHANGED | Tenant management | `/api/v1/platform/tenants` gains update, admin-reset (recover a tenant's super administrator) and usage figures; a suspension needs a reason; still `PLATFORM_TENANT_MANAGE` + PLATFORM-tenant caller; no new permission (D5) | `../P1/srs-tenant.md` 1.3.0 B1 |
| CHANGED | DEPENDENCY MAP | + `TENANT ──crossmodule──▶ SEC (SecUserDirectoryApi counts, SecAdminRecoveryApi)`, `──▶ FILE (FileDocumentLookupApi counts)`, `──▶ NOTIF (NotificationLogQueryApi.countDispatchedSince)` — each called inside `TenantContext.callAs(id)` | `../P1/srs-tenant.md` 1.3.0 B7 |
| CHANGED | DEFERRED | "Editing a tenant's name, a usage endpoint" leaves DEFERRED; "tenant profile, suspension facts" leave DEFERRED (the token cut-off is stored, its enforcement is package C.2); lifecycle events, branding, idempotent provisioning and export stay with packages C and E; level 2 stays deferred | plan §0 D2, §4, §5, §7 |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package E — tenant branding (logo, brand colour, `/api/v1/tenant/me`, public branding; plan §0 D5, §7)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

| Kind | Aspect | Delta | Source |
|---|---|---|---|
| NEW — FE | Branding for the UI | a tenant gets an optional logo and brand colour, set by the platform operator on `PLATFORM_TENANTS` (`PUT` / `DELETE /{id}/logo`, `PATCH /{id}/branding`); `GET /api/v1/tenant/me` (authenticated, any realm) and `GET /api/v1/public/tenants/{tenantCode}/branding` (public, path tenant, rate-limited per client address) feed the shell and the login page; the logo document lives in the tenant's own rows as a PUBLIC file | `../P1/srs-tenant.md` 1.3.0 E1 |
| CHANGED | DEPENDENCY MAP | + `TENANT ──crossmodule──▶ FILE (FileImageStoreApi, FileDocumentLookupApi.publicUrl; soft reference CORE_TENANT.LOGO_FILE_ID)` | E6 |
| CHANGED | DEFERRED | "branding" leaves DEFERRED; a tenant self-service branding screen is deferred (ADR-TENANT-005) | plan §0 D5 |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C12 — tenant lifecycle events and the per-tenant token cut-off (plan §5 C.1, C.2)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

| Kind | Aspect | Delta | Source |
|---|---|---|---|
| NEW | Lifecycle events | `TenantSuspendedEvent`, `TenantActivatedEvent` are published after commit on real transitions; SEC ends the suspended tenant's sessions, NOTIF holds its queued notifications until it is active again; core event catalogue 11 → 13 | `../P1/srs-tenant.md` 1.3.0 C6 |
| CHANGED — FE | Tenant of a request | the resolution order is unchanged; a token whose `iat` is before its tenant's `TOKENS_INVALID_BEFORE` is refused 401 `TENANT_TOKEN_REVOKED` (both realms, `/api/v1/tenant/me` included), also when its session was already ended; a re-activation cuts off every earlier token | C3 RULE-TENANT-023 |
| NEW — FE | Tenant management | `POST /api/v1/platform/tenants/{id}/revoke-tokens` signs every user of a tenant out (not PLATFORM) | C1 |
| CHANGED | DEPENDENCY MAP | + `TENANT ──crossmodule──▶ SEC (SecAdminRecoveryApi.terminateAllSessions)`; `SEC ──event──▶ TenantSuspendedEvent`; `NOTIF ──crossmodule──▶ TENANT (TenantLookupApi.isActive)`, `NOTIF ──event──▶ TenantActivatedEvent` | C7 |
| CHANGED | DEFERRED | "lifecycle events" and "token cut-off enforcement" leave DEFERRED; idempotent provisioning (C.4), export (C.5) and the `ScopedValue` spike (C.6) stay with package C | plan §5 |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C6 — `ScopedValue` spike for `TenantContext`, go / no-go (plan §0 D6, §5 C.6)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

| Kind | Aspect | Delta | Source |
|---|---|---|---|
| NEW (spike) | Tenant context | `TenantContext` on `ScopedValue` behind the same public API (`current` / `find` / `require` / `set` / `clear` / `runAs` / `callAs`); go / no-go decided by ADR-TENANT-004 against fixed criteria (touches `events` and `notif`); may slip to 1.4.0 | `../P1/srs-tenant.md` 1.3.0 C6-1; ADR-TENANT-004 |
