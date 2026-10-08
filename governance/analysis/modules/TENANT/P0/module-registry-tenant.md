## MODULE REGISTRY — المستأجرون / Tenant (TENANT)
══════════════════════════════════════════════════════════════════
Module Code    : TENANT   (package `com.erp.tenant`; permission-registry module `PLATFORM`; api-docs folder `tenant`)
Bounded context: platform
Layer / Type   : L1 / platform infrastructure (multi-tenancy)     Execution tier : foundation (V10, erp-core plan step 05)
Source         : AS-BUILT (erp-core 1.2.0; code at main @ 2274f86)
Knowledge      : erp-core-plan/05-STEP-multi-tenancy.md; docs/steps/05-report.md; docs/DEVIATIONS.md [05], [07], [09], [15]; docs/CONSUMING.md §3
Readiness      : READY (built)
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`, migrations to
`erp-core/src/main/resources/db/migration/core/`.

ENTITIES OWNED   (entity ids are assigned in P1: ENT-TENANT-001)
| Entity (ar/en) | Kind | PRIVATE / SHARED | Source |
|---|---|---|---|
| المستأجر / Tenant (`CORE_TENANT`) | platform registry (global, no `TENANT_ID`) | SHARED (owner) — every tenant-scoped table of every module carries a HARD FK `TENANT_ID` → `CORE_TENANT(ID)` (22 tables, `../P2/db-script-tenant.md`) | tenant/entity/Tenant.java:32-40; V10__tenant_schema.sql:31-51 |

Not entities, but owned runtime surface (no table): `TenantContext` (the request tenant, ThreadLocal),
`TenantConstants`, the provisioning SPI `TenantProvisioningContributor` / `TenantProvisioning`, the
cross-module read `TenantLookupApi`, the Hibernate resolver `TenantIdentifierResolver`, and the request
filter `TenantResolutionFilter` (tenant/TenantContext.java:25; tenant/TenantConstants.java:4;
tenant/TenantProvisioningContributor.java:22; tenant/TenantProvisioning.java:14; tenant/crossmodule/TenantLookupApi.java:10;
tenant/config/TenantIdentifierResolver.java:28; tenant/security/TenantResolutionFilter.java:53).

LOOKUPS OWNED
| Lookup key | Description | Initial values | Source |
|---|---|---|---|
| (value set of `CORE_TENANT.STATUS_CODE`) | حالة المستأجر / tenant status | `ACTIVE`, `SUSPENDED` — a CHECK constraint (`CHK_CORE_TENANT_STATUS`), not an MDL lookup type (same pattern as ADR-SEC-001) | V10__tenant_schema.sql:50; tenant/TenantConstants.java:23, :26 |

LOOKUPS CONSUMED
None.

SHARED ENTITIES CONSUMED
None — the tenant module reads no other module's table. (Its permission rows live in SEC's global
registry, written by SEC's catalog synchronizer from `TenantPermissions`; see DEPENDENCIES.)

DEPENDENCIES
| Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|
| SEC | SPI (implements) | `com.erp.sec.permission.PermissionContributor` — `TenantPermissions` declares module `PLATFORM`, screen `PLATFORM_TENANTS` and its two actions | tenant/permission/TenantPermissions.java:17-46 |
| SEC | wiring (SEC calls tenant) | `JwtAuthenticationFilter` sets `TenantContext` from the token claim `tid` and clears a leaked tenant at request start; `JwtTokenIssuer` writes `tid` | sec/security/JwtAuthenticationFilter.java:85-91, :129-136, :173-176; sec/security/JwtTokenIssuer.java:46 |
| events | publishes | `TenantCreatedEvent` (tenant id = the NEW tenant, actor = the platform operator) | tenant/service/TenantService.java:99-100; events/TenantCreatedEvent.java:11-18 |
| audit | SOFT (entity listener) | `@Audited(entityType = "CORE_TENANT")` — every insert/update of a tenant is recorded field by field in `CORE_AUDIT_EVENT` (under the acting PLATFORM tenant) | tenant/entity/Tenant.java:33; docs/DEVIATIONS.md [10] "Tenant of a row" |
| common | foundation | `GlobalAuditableEntity`, `ServiceResult`/`Status`, `LocalizedException`, `DomainRules`, search builders, `FilterErrorResponseWriter`, `SecurityContextHelper` | tenant/service/TenantService.java:3-11; tenant/security/TenantResolutionFilter.java:3 |
ROOT: YES for data (CORE_TENANT references nothing); the module is wired by `com.erp.autoconfigure`
(`ErpCoreSecurityAutoConfiguration` builds `TenantResolutionFilter` into both security chains).

EXPOSED SURFACE (consumed by other modules)
| Surface | Kind | Consumers | XM id (P1) | Source |
|---|---|---|---|---|
| `com.erp.tenant.crossmodule.TenantLookupApi.codeOf(Long)` | crossmodule read | FILE (`PublicFileUrls`, public file URLs), SEQUENCE (`NumberAllocationService`, `{TENANT}` token) | XM-TENANT-001 | tenant/crossmodule/TenantLookupApi.java:10-14; file/service/PublicFileUrls.java:34; sequence/service/NumberAllocationService.java:52 |
| `com.erp.tenant.TenantProvisioningContributor` (+ `TenantProvisioning`) | SPI | SEC (order 0), MDL (10), NOTIF (20), SEQUENCE (40); applications may add their own | XM-TENANT-002 | tenant/TenantProvisioningContributor.java:22-31; sec/tenant/SecTenantProvisioningContributor.java:62-64; mdl/tenant/MdlTenantProvisioningContributor.java:27-29; notif/tenant/NotifTenantProvisioningContributor.java:27-29; sequence/tenant/SequenceTenantProvisioningContributor.java:28-30 |
| `com.erp.tenant.TenantContext`, `TenantConstants` | root-package API | every module, the event executor, jobs, applications | — (public API, docs/RELEASE.md) | tenant/TenantContext.java:25-93; tenant/TenantConstants.java:4-27 |
| `CORE_TENANT(ID)` | HARD FK target | 22 tenant-scoped tables | DBF-TENANT-011…032 (P2) | V10__tenant_schema.sql:102-119; V11__sec_realms.sql:84; V13__notif_async_inbox.sql:61; V14__sequence_and_settings.sql:58; V15__audit_schema.sql:40 |

PERMISSION MODULE → SCREEN → ACTIONS (registry rows; code-defined since step 06, seeded by V10 §6)
| Registry module | Screen (page code) | Action code | Authority | Meaning | Source |
|---|---|---|---|---|---|
| `PLATFORM` — إدارة المنصة / Platform Administration | `PLATFORM_TENANTS` — المستأجرون / Tenants | `VIEW` | `PERM_PLATFORM_TENANTS_VIEW` | gateway action of the screen (RULE-SEC-007); grants no endpoint by itself | tenant/permission/TenantPermissions.java:20-28, :43; V10__tenant_schema.sql:213-228 |
| `PLATFORM` | `PLATFORM_TENANTS` | `MANAGE` | `PLATFORM_TENANT_MANAGE` | every `/api/v1/platform/tenants` operation, reads included (step-05 literal, not `PERM_<PAGE>_<ACTION>`) | tenant/permission/TenantPermissions.java:26, :44; V10__tenant_schema.sql:225 |
Both are granted only to the PLATFORM tenant's `SYS_ADMIN` (V10__tenant_schema.sql:230-244) and are never
copied to a new tenant (sec/tenant/SecTenantProvisioningContributor.java:84-121). The registry module
`PLATFORM` also carries CU's screen `PLATFORM_SETTINGS` (owned by `CuPermissions`, not by this module).

AUTO-DECISIONS
AUTO: module code TENANT for the analysis folder although the permission-registry module is `PLATFORM`
  FROM: governance/analysis/platform/project-registry.md (row "TENANT — platform tenant provisioning"); docs/api-docs/tenant/
  IF WRONG: rename the folder; no id changes (the ids carry TENANT, the registry rows keep PLATFORM).
AUTO: Tenant classified SHARED
  FROM: the 22 HARD FKs to CORE_TENANT(ID)
  IF WRONG: none — the FKs exist.

RESOLVED DECISIONS
| # | Point | Decision | Sources |
|---|---|---|---|
| 1 | Multi-tenancy model | row-level discriminator in one shared schema | ADR-TENANT-001 |

POLICIES OWNED (full text in business-policies-tenant.md)
POL-TENANT-001, POL-TENANT-002, POL-TENANT-003, POL-TENANT-004, POL-TENANT-005, POL-TENANT-006,
POL-TENANT-007, POL-TENANT-008, POL-TENANT-009, POL-TENANT-010, POL-TENANT-011
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package B — tenant level 1 (edit, suspension facts, admin-reset, usage)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

ENTITIES OWNED — delta
| Kind | Entity | Delta | Source |
|---|---|---|---|
| CHANGED | المستأجر / Tenant (`CORE_TENANT`) | + profile columns (V18), suspension facts + `TOKENS_INVALID_BEFORE` (V19); still global, still SHARED (owner); names editable | `../P2/db-script-tenant.md` 1.3.0 addendum |

LOOKUPS OWNED — delta
| Kind | Lookup key | Values | Source |
|---|---|---|---|
| NEW | (value set of `CORE_TENANT.DEFAULT_LOCALE`) | `ar`, `en` — CHECK `CHK_CORE_TENANT_LOCALE` (NULL allowed), not an MDL lookup | V18__tenant_profile.sql |
| unchanged | `STATUS_CODE` | `ACTIVE`, `SUSPENDED` (no `ARCHIVED`, plan §0 D2) | — |

DEPENDENCIES — delta (TENANT still reads no other module's table)
| Kind | Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|---|
| NEW | SEC | crossmodule (in-core API) | `SecUserDirectoryApi.countStaff()`, `countCustomers()`, `countActiveSessions()`; NEW `SecAdminRecoveryApi.findRecoveryTarget(String)`, `resetSuperUserPassword(String, String, Boolean)` — called inside `TenantContext.callAs(id)` | `../P1/srs-tenant.md` 1.3.0 B7 |
| NEW | FILE | crossmodule (in-core API) | `FileDocumentLookupApi.countDocuments()`, `sumBytes()` | same |
| NEW | NOTIF | crossmodule (in-core API) | `NotificationLogQueryApi.countDispatchedSince(Instant)` | same |
| CHANGED | audit | SOFT | + actions `ADMIN_PASSWORD_RESET` (recorded by SEC's recovery in the target tenant) and `TENANT_ADMIN_RESET` (recorded by TENANT in PLATFORM through `AuditApi`) | same, B8 |

EXPOSED SURFACE, PERMISSION MODULE → SCREEN → ACTIONS: unchanged (plan §0 D5: no module, screen, permission
or grant seed; every new endpoint sits behind `PLATFORM_TENANT_MANAGE` on `PLATFORM_TENANTS`).

POLICIES OWNED — delta: + POL-TENANT-012, POL-TENANT-013 (`business-policies-tenant.md` 1.3.0 addendum).

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package E — tenant branding (logo, brand colour, `/api/v1/tenant/me`, public branding; plan §0 D5, §7)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

ENTITIES OWNED — delta
| Kind | Entity | Delta | Source |
|---|---|---|---|
| CHANGED | المستأجر / Tenant (`CORE_TENANT`) | + `LOGO_FILE_ID` (soft reference to `FILE_DOCUMENT.ID`, no FK) and `BRAND_COLOR` (V20); still global. "ROOT for data" no longer holds strictly: one column points into FILE (XM-TENANT-003) | `../P2/db-script-tenant.md` 1.3.0 addendum |

DEPENDENCIES — delta (TENANT still reads no other module's table)
| Kind | Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|---|
| NEW | FILE | SOFT (crossmodule, XM-TENANT-003) | `FileImageStoreApi.storePublicImage`, `discard`; `FileDocumentLookupApi.publicUrl` — inside `TenantContext.callAs(id)` | `../P1/srs-tenant.md` 1.3.0 E6 |
| CHANGED | audit | SOFT | + action `TENANT_LOGO_CHANGED` (target tenant and PLATFORM) | same, E8 |

EXPOSED SURFACE — delta
| Kind | Surface | Consumers | Through | Source |
|---|---|---|---|---|
| NEW | `GET /api/v1/tenant/me`, `GET /api/v1/public/tenants/{tenantCode}/branding` (`TenantBrandingResponse`) | the frontend shell and login page (plan §8 F2) | HTTP | plan §7 E.2 |

CONFIGURATION — delta
| Kind | Property | Delta | Source |
|---|---|---|---|
| CHANGED | `erp.core.tenant.path-tenant-paths` | default + `/api/v1/public/tenants/{tenantCode}/branding` | plan §7 E.2 |
| NEW | `erp.core.tenant.public-branding-rate-limit.capacity` / `period` | 60 / 1 min per client address (RULE-TENANT-022), like the customer login's bucket4j limiter | plan §7 E.2 |

PERMISSION MODULE → SCREEN → ACTIONS: unchanged (plan §0 D5: no module, screen, permission or grant seed).

RESOLVED DECISIONS — delta: 5 · who sets a tenant's logo → the platform administrator from `PLATFORM_TENANTS`
(decision D5, ADR-TENANT-005). POLICIES OWNED — delta: + POL-TENANT-014.

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C12 — tenant lifecycle events and the per-tenant token cut-off (plan §5 C.1, C.2)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

ENTITIES OWNED — delta: `CORE_TENANT.TOKENS_INVALID_BEFORE` (V19) is now enforced and also written by revoke-tokens; no
new column, no migration.

DEPENDENCIES — delta (TENANT still reads no other module's table)
| Kind | Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|---|
| NEW | SEC | crossmodule (in-core API) | `SecAdminRecoveryApi.terminateAllSessions()` — inside `TenantContext.callAs(id)` | `../P1/srs-tenant.md` 1.3.0 C7 |
| NEW | events | publishes | `TenantSuspendedEvent(tenantId, tenantCode, reason, actor)`, `TenantActivatedEvent(tenantId, tenantCode, actor)` — after commit | C6 |
| NEW (consumer of TENANT) | SEC | listener | `TenantSuspendedEvent` → terminates the tenant's sessions (SEC REQ-SEC-092) | C6 |
| NEW (consumer of TENANT) | NOTIF | crossmodule + listener | `TenantLookupApi.isActive` (claim, requeue) and `TenantActivatedEvent` → re-dispatch (NOTIF RULE-NOTIF-024) | C7 |
| CHANGED | audit | SOFT | + action `TOKENS_REVOKED` (target tenant and PLATFORM) | C8 |

EXPOSED SURFACE — delta
| Kind | Surface | Consumers | Through | Source |
|---|---|---|---|---|
| CHANGED | `TenantLookupApi` + `boolean isActive(Long tenantId)` (XM-TENANT-001) | NOTIF | crossmodule | C7 |
| NEW | `com.erp.tenant.TenantTokenFacts` (root package) | SEC `JwtAuthenticationFilter` | request attribute | C7; ADR-TENANT-002 |
| NEW | `POST /api/v1/platform/tenants/{id}/revoke-tokens` | frontend `PLATFORM_TENANTS` (plan §8 F3) | HTTP | C1 |

PERMISSION MODULE → SCREEN → ACTIONS: unchanged (plan §0 D5).

RESOLVED DECISIONS — delta: 2 · token cut-off vs `jti` denylist → per-tenant cut-off (ADR-TENANT-002). POLICIES
OWNED — delta: + POL-TENANT-015.

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C6 — `ScopedValue` spike for `TenantContext`, go / no-go (plan §0 D6, §5 C.6)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

EXPOSED SURFACE — delta: none; `TenantContext` keeps its public API and behaviour (`current`, `find`, `require`,
`isPlatform`, `set`, `clear`, `runAs`, `callAs`) and its `ThreadLocal` binding.

RESOLVED DECISIONS — delta: 4 · `ScopedValue` for `TenantContext` → no-go after the spike (ADR-TENANT-004 REJECTED;
`../P1/srs-tenant.md` 1.3.0 C6-2).
