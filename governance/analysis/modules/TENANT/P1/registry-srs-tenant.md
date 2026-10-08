## REGISTRY — P1 — TENANT v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Every id below is traced to the code it was read from (main @ 2274f86). Java paths are relative to
`erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`.

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status | Code location |
|---|---|---|---|---|---|
| ENT-TENANT-001 | المستأجر / Tenant | platform registry (global) | SHARED (owner) | REGISTERED (built) | tenant/entity/Tenant.java:32-95; V10__tenant_schema.sql:31-51 |

Consumed
none — TENANT reads no other module's table (SPI / event / listener use only: srs-tenant.md A8).

Cross-module surfaces (exposed direction)
| XM id | Surface | Kind | Consumers / implementers | Status | Code location |
|---|---|---|---|---|---|
| XM-TENANT-001 | `com.erp.tenant.crossmodule.TenantLookupApi.codeOf(Long)` | crossmodule read | FILE (`PublicFileUrls`), SEQUENCE (`NumberAllocationService`) | ACTIVE (built) | tenant/crossmodule/TenantLookupApi.java:10-14; tenant/crossmodule/TenantLookupApiImpl.java:15-26 |
| XM-TENANT-002 | `com.erp.tenant.TenantProvisioningContributor` + `TenantProvisioning` | SPI | SEC (0), MDL (10), NOTIF (20), SEQUENCE (40) | ACTIVE (built) | tenant/TenantProvisioningContributor.java:22-31; tenant/TenantProvisioning.java:14-28 |
Note: in SEC's analysis XM ids are minted by the consuming module (SEC `P2/db-script-sec.md` §2 AMENDMENT).
The tenant module has no consumer analysis that would mint them, so the tenant-maturity plan (§3) assigns
these two exposed surfaces TENANT ids here.

Lookups owned
| Key | ENT | Values count | Code location |
|---|---|---|---|
| STATUS_CODE value set (`CHK_CORE_TENANT_STATUS`) | ENT-TENANT-001 | 2 (ACTIVE, SUSPENDED) | V10__tenant_schema.sql:50; tenant/TenantConstants.java:23, :26 |

Lookups consumed
none.

Screens
| SCR-REQ id | Name (ar/en) | Page code | Code location |
|---|---|---|---|
| SCR-REQ-TENANT-001 | المستأجرون / Tenants | PLATFORM_TENANTS | tenant/permission/TenantPermissions.java:28; V10__tenant_schema.sql:216-218 |

Requirements
REQ count: 23 · AC count: 23 · RULE count: 9 · ENT count: 1 · SCR-REQ count: 1 · XM count: 2
Last sequence per atom: REQ: 023 · AC: 023 · ENT: 001 · RULE: 009 · SCR-REQ: 001 · XM: 002 · US: 008 · POL: 011

## Id → code location
| Id | Title | Code location (primary) | Verified by |
|---|---|---|---|
| REQ-TENANT-001 / AC-TENANT-001 | Provision a tenant | tenant/controller/PlatformTenantController.java:41-46; tenant/service/TenantService.java:68-103 | TC-CORE-TENANT-005, -006, -012 |
| REQ-TENANT-002 / AC-TENANT-002 | Reject an invalid code | tenant/domain/TenantDomain.java:34-36 | TC-CORE-TENANT-009 |
| REQ-TENANT-003 / AC-TENANT-003 | Reject a duplicate code | tenant/service/TenantService.java:73-83; tenant/domain/TenantDomain.java:38 | TC-CORE-TENANT-008 |
| REQ-TENANT-004 / AC-TENANT-004 | Reject an incomplete create request | tenant/dto/TenantCreateRequest.java:25-63 | TC-CORE-TENANT-010 |
| REQ-TENANT-005 / AC-TENANT-005 | List tenants | tenant/service/TenantService.java:133-139 | TC-CORE-TENANT-011 |
| REQ-TENANT-006 / AC-TENANT-006 | Read a tenant | tenant/service/TenantService.java:105-114 | TC-CORE-TENANT-011 |
| REQ-TENANT-007 / AC-TENANT-007 | Search tenants | tenant/service/TenantService.java:59-61, :116-131 | TC-CORE-TENANT-011 |
| REQ-TENANT-008 / AC-TENANT-008 | Change a tenant's status | tenant/service/TenantService.java:141-160; tenant/dto/TenantStatusUpdateRequest.java:19-22 | TC-CORE-TENANT-019, -020, -024 |
| REQ-TENANT-009 / AC-TENANT-009 | Protect the PLATFORM tenant | tenant/domain/TenantDomain.java:53-59 | TC-CORE-TENANT-019 |
| REQ-TENANT-010 / AC-TENANT-010 | Refuse a suspended tenant's requests | tenant/security/TenantResolutionFilter.java:89-99, :109-112, :139-142 | TC-CORE-TENANT-021, -022, -023, -026 |
| REQ-TENANT-011 / AC-TENANT-011 | Tenant from the path | tenant/security/TenantResolutionFilter.java:83-87, :131-171; autoconfigure/ErpCoreProperties.java:252-261 | TC-CORE-TENANT-023 (suspended on the path) |
| REQ-TENANT-012 / AC-TENANT-012 | Tenant from the token | sec/security/JwtAuthenticationFilter.java:129-136; tenant/security/TenantResolutionFilter.java:89-99 | TC-CORE-TENANT-016 |
| REQ-TENANT-013 / AC-TENANT-013 | Tenant from the header | tenant/security/TenantResolutionFilter.java:101-120 | TC-CORE-TENANT-002, -003, -018 |
| REQ-TENANT-014 / AC-TENANT-014 | Request without a tenant | tenant/security/TenantResolutionFilter.java:122-127; autoconfigure/ErpCoreProperties.java:233-246 | TC-CORE-TENANT-001, -004 |
| REQ-TENANT-015 / AC-TENANT-015 | Platform API for operators only | autoconfigure/ErpCoreSecurityAutoConfiguration.java:85-88, :130-131, :199-205 | TC-CORE-PLATFORM-001…004 |
| REQ-TENANT-016 / AC-TENANT-016 | Row-level isolation | common/domain/AuditableEntity.java:35-37; tenant/config/TenantIdentifierResolver.java:36-47 | TC-CORE-TENANT-014, -015, -025 |
| REQ-TENANT-017 / AC-TENANT-017 | Fail fast without a tenant | tenant/TenantContext.java:47-53; tenant/config/TenantHibernateConfiguration.java:34, :50-52 | `TenantContextIntegrationTest`, `TenantBootstrapWindowIntegrationTest` |
| REQ-TENANT-018 / AC-TENANT-018 | Run as a tenant outside a request | tenant/TenantContext.java:71-92 | `TenantContextTest` |
| REQ-TENANT-019 / AC-TENANT-019 | Cross-module read of a tenant code | tenant/crossmodule/TenantLookupApiImpl.java:21-25 | FILE public-URL tests |
| REQ-TENANT-020 / AC-TENANT-020 | Provisioning SPI contract | tenant/service/TenantService.java:85-95; tenant/TenantProvisioningContributor.java:22-31 | TC-CORE-TENANT-012, -025 |
| REQ-TENANT-021 / AC-TENANT-021 | Tenant-created event | tenant/service/TenantService.java:98-100; events/TenantCreatedEvent.java:11-18 | — |
| REQ-TENANT-022 / AC-TENANT-022 | Audit tenant changes | tenant/entity/Tenant.java:33 | — |
| REQ-TENANT-023 / AC-TENANT-023 | Clear a leaked tenant | sec/security/JwtAuthenticationFilter.java:85-91 | DEVIATIONS [15] tests |
| RULE-TENANT-001 | Code format | tenant/domain/TenantDomain.java:18, :34-36; V10__tenant_schema.sql:51 | `TenantDomainTest` |
| RULE-TENANT-002 | Code uniqueness | tenant/domain/TenantDomain.java:38; V10__tenant_schema.sql:49 | `TenantDomainTest` |
| RULE-TENANT-003 | Code immutability | tenant/entity/Tenant.java:50 | — |
| RULE-TENANT-004 | Status values | tenant/dto/TenantStatusUpdateRequest.java:20; V10__tenant_schema.sql:50 | TC-CORE-TENANT-019 |
| RULE-TENANT-005 | PLATFORM not suspendable | tenant/domain/TenantDomain.java:53-59 | `TenantDomainTest` |
| RULE-TENANT-006 | Suspended tenant not served | tenant/security/TenantResolutionFilter.java:89-99, :109-112, :139-142 | TC-CORE-TENANT-021…023 |
| RULE-TENANT-007 | PLATFORM-module permissions never leave PLATFORM | sec/tenant/SecTenantProvisioningContributor.java:84-121; sec/service/MenuService.java:142-143 | `TenantSchemaIntegrationTest.thePlatformTenantIsSeeded_andOnlyItsSysAdminHoldsPlatformTenantManage` |
| RULE-TENANT-008 | Provisioning writes name TENANT_ID | tenant/TenantProvisioningContributor.java:14-17 | ArchUnit `CoreLibraryRulesArchTest` (raw JDBC allow-list) |
| RULE-TENANT-009 | Header / path never switch a token's tenant | tenant/security/TenantResolutionFilter.java:89-99, :143-147 | TC-CORE-TENANT-016 |
| ENT-TENANT-001 | Tenant | tenant/entity/Tenant.java:32-95 | `TenantSchemaIntegrationTest` |
| XM-TENANT-001 | TenantLookupApi | tenant/crossmodule/TenantLookupApi.java:10-14 | — |
| XM-TENANT-002 | TenantProvisioningContributor | tenant/TenantProvisioningContributor.java:22-31 | TC-CORE-TENANT-012 |
| SCR-REQ-TENANT-001 | PLATFORM_TENANTS | tenant/permission/TenantPermissions.java:28, :43-44 | `governance/frontend/modules/PLATFORM/tests/` |
| US-TENANT-001…008 | stories | `../P0_5/prd-tenant.md` | — |
| POL-TENANT-001…011 | policies | `../P0/business-policies-tenant.md` | — |

REQ ids (full text in srs-tenant.md → A4): REQ-TENANT-001, REQ-TENANT-002, REQ-TENANT-003,
REQ-TENANT-004, REQ-TENANT-005, REQ-TENANT-006, REQ-TENANT-007, REQ-TENANT-008, REQ-TENANT-009,
REQ-TENANT-010, REQ-TENANT-011, REQ-TENANT-012, REQ-TENANT-013, REQ-TENANT-014, REQ-TENANT-015,
REQ-TENANT-016, REQ-TENANT-017, REQ-TENANT-018, REQ-TENANT-019, REQ-TENANT-020, REQ-TENANT-021,
REQ-TENANT-022, REQ-TENANT-023

AC ids (full text in srs-tenant.md → A4, one per REQ above): AC-TENANT-001 … AC-TENANT-023

RULE ids (full text in srs-tenant.md → A5): RULE-TENANT-001, RULE-TENANT-002, RULE-TENANT-003,
RULE-TENANT-004, RULE-TENANT-005, RULE-TENANT-006, RULE-TENANT-007, RULE-TENANT-008, RULE-TENANT-009

Error codes (full table in srs-tenant.md → STANDALONE)
| Code | HTTP | Code location |
|---|---|---|
| `TENANT_REQUIRED` | 400 | tenant/security/TenantResolutionFilter.java:124 |
| `TENANT_NOT_FOUND` | 404 | tenant/security/TenantResolutionFilter.java:106, :136; tenant/service/TenantService.java:111, :147 |
| `TENANT_SUSPENDED` | 403 | tenant/security/TenantResolutionFilter.java:94, :110, :140 |
| `TENANT_CONTEXT_MISSING` | 500 | tenant/TenantContext.java:50 |
| `TENANT_CODE_INVALID` | 400 | tenant/domain/TenantDomain.java:36 |
| `TENANT_CODE_DUPLICATE` | 409 | tenant/domain/TenantDomain.java:38; tenant/service/TenantService.java:81-82 |
| `TENANT_PLATFORM_PROTECTED` | 422 | tenant/domain/TenantDomain.java:56-57 |
All seven: tenant/exception/TenantErrorCodes.java:14-32; i18n `messages.properties` 143–149, `messages_ar.properties` 140–146.

Decisions
ADR ids: ADR-TENANT-001 (ACCEPTED, as built) — `governance/analysis/decisions/TENANT/ADR-TENANT-001.md`.

Event
"P1 completed: TENANT v1 (as built) — 1 entity, 23 requirements, 23 acceptance criteria, 9 rules, 1 screen requirement, 2 XM, 1 ADR"
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C3 — automated tenant-isolation tests
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-tenant.md` → "Implementation Addendum — erp-core 1.3.0". Ids
continue from the highest number ever issued for TENANT (REQ / AC 023, RULE 009).

Entities, lookups, screens, cross-module surfaces, error codes: unchanged (no ENT, DBF, XM, page code,
permission or error code added; no migration).

Requirements — new items
| Kind | Id | Title | Traces | Code location (primary) | Verified by |
|---|---|---|---|---|---|
| NEW | REQ-TENANT-024 / AC-TENANT-024 | Tenant isolation guarantee (entity set, HTTP behaviour of every core module, raw SQL) | US-TENANT-004; POL-TENANT-007; RULE-TENANT-010, -011 | common/domain/AuditableEntity.java:35-37; tenant/config/TenantIdentifierResolver.java:36-47 | `TenantIsolationIntegrationTest` (SEC, MDL, FILE, NOTIF, CU, SEQUENCE, AUDIT); `TenantScopedEntityTest` |
| NEW | RULE-TENANT-010 | The global entity set: `Tenant`, `ModuleRegistry`, `ScreenRegistry`, `ActionRegistry`, `AppConfiguration` | REQ-TENANT-024 | tenant/entity/Tenant.java:40; sec/entity/ModuleRegistry.java:36; sec/entity/ScreenRegistry.java:45; sec/entity/ActionRegistry.java:45; cu/entity/AppConfiguration.java:41 | `TenantScopedEntityTest` |
| NEW | RULE-TENANT-011 | Every raw SQL statement on a tenant-scoped table names `TENANT_ID` | REQ-TENANT-024 | `governance/rules/GOVERNANCE-RULES.md` (Governance Rules); tenant/TenantProvisioningContributor.java:14-17 | review checklist of `gov-validate-backend-feature`; `CoreLibraryRulesArchTest.rule7_*` (where raw SQL may live) |

Counts after this addendum: REQ 24 · AC 24 · RULE 11 · ENT 1 · SCR-REQ 1 · XM 2.
Last sequence per atom: REQ: 024 · AC: 024 · ENT: 001 · RULE: 011 · SCR-REQ: 001 · XM: 002 · US: 008 · POL: 011

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D — what SEC's user profile and password policy change on the tenant side
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-tenant.md` → "Implementation Addendum — erp-core 1.3.0" (package D rows).

| Kind | Id | Delta |
|---|---|---|
| CHANGED | XM-TENANT-001 | + `TenantLookupApi.summaryOf(Long)` → `Optional<TenantSummary>`; consumer SEC (`/me`) |
| CHANGED | REQ-TENANT-001 | + 400 `SEC-400-PASSWORD-POLICY` (field `adminPassword`) from the SEC provisioning contributor |

No new TENANT id, endpoint, entity field or error code from package D.

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package B — tenant level 1 (edit, suspension facts, admin-reset, usage)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-tenant.md` → "Implementation Addendum — erp-core 1.3.0" (package B
block, B1–B11). Ids continue from the highest ever issued (REQ / AC 024, RULE 011; RULE-TENANT-012 … 015
reserved for the analysis-coverage work's as-built rules).

Entities — delta
| Kind | ENT id | Delta | Code location |
|---|---|---|---|
| CHANGED | ENT-TENANT-001 Tenant | names editable; + `contactEmail`, `contactPhone`, `countryCode`, `defaultLocale`, `timezone`, `notes` (V18); + `suspendedAt`, `suspendedBy`, `suspensionReason`, `tokensInvalidBefore` (V19) — DBF-TENANT-033 … 042 | tenant/entity/Tenant.java; V18__tenant_profile.sql; V19__tenant_lifecycle.sql |

Consumed — delta (TENANT reads no other module's table; these are in-core APIs)
| Kind | Owner | Surface | Used by |
|---|---|---|---|
| NEW | SEC | `SecUserDirectoryApi.countStaff / countCustomers / countActiveSessions` (REQ-SEC-090) | usage |
| NEW | SEC | `SecAdminRecoveryApi.findRecoveryTarget / resetSuperUserPassword` (REQ-SEC-091) | admin-reset |
| NEW | FILE | `FileDocumentLookupApi.countDocuments / sumBytes` (XM-FILE-001 CHANGED) | usage |
| NEW | NOTIF | `NotificationLogQueryApi.countDispatchedSince(Instant)` | usage |

Lookups owned — delta
| Kind | Key | Values | Code location |
|---|---|---|---|
| NEW | `CORE_TENANT.DEFAULT_LOCALE` value set (`CHK_CORE_TENANT_LOCALE`, NULL allowed) | 2 (`ar`, `en`) | V18__tenant_profile.sql |

Screens — delta
| Kind | SCR-REQ id | Delta |
|---|---|---|
| CHANGED | SCR-REQ-TENANT-001 PLATFORM_TENANTS | B2 + 3 filter / sort fields; B3 edit form, suspend reason, admin-reset form, usage panel; B5 + `PUT /{id}`, `POST /{id}/admin-reset`, `GET /{id}/usage`; B4 unchanged (D5) |

Requirements — new / changed items
| Kind | Id | Title | Traces | Code location (primary) | Verified by |
|---|---|---|---|---|---|
| NEW | REQ-TENANT-025 / AC-TENANT-025 | Update a tenant's names and profile (`PUT /{id}`) | US-TENANT-009; POL-TENANT-001, -006; RULE-TENANT-003 | tenant/service/TenantService.java (`update`); tenant/mapper/TenantMapper.java (`updateEntityFromRequest`) | `TenantProfileIntegrationTest`; TC-CORE-TENANT-028, -029 |
| NEW | REQ-TENANT-026 / AC-TENANT-026 | Suspend with a reason; suspension facts; activation clears them and sets the cut-off | US-TENANT-003; POL-TENANT-012; RULE-TENANT-016 | tenant/domain/TenantDomain.java; tenant/service/TenantService.java (`updateStatus`) | `TenantProfileIntegrationTest`, `TenantDomainTest`; TC-CORE-TENANT-030 … 032 |
| NEW | REQ-TENANT-027 / AC-TENANT-027 | Reset a tenant administrator's password (`/{id}/admin-reset`) | US-TENANT-010; POL-TENANT-013; RULE-TENANT-017 | tenant/service/TenantService.java (`resetAdministratorPassword`); sec/crossmodule/SecAdminRecoveryApi.java | `TenantAdminResetIntegrationTest`; TC-CORE-TENANT-033 … 035 |
| NEW | REQ-TENANT-028 / AC-TENANT-028 | Tenant usage figures (`/{id}/usage`) | US-TENANT-011; POL-TENANT-006, -007 | tenant/service/TenantService.java (`getUsage`) | `TenantUsageIntegrationTest`; TC-CORE-TENANT-027, -036 |
| NEW | RULE-TENANT-016 | Suspension requires a reason (3..500); activation clears the facts and sets `TOKENS_INVALID_BEFORE` | REQ-TENANT-026 | tenant/domain/TenantDomain.java | `TenantDomainTest` |
| NEW | RULE-TENANT-017 | Admin-reset target: a STAFF user of that tenant holding an active super role; never the PLATFORM tenant (review round 1) | REQ-TENANT-027 | tenant/domain/TenantDomain.java | `TenantDomainTest`, `TenantAdminResetIntegrationTest` |
| CHANGED | RULE-TENANT-003, RULE-TENANT-004 | names editable (code still immutable); re-applying a status leaves the facts and the cut-off | REQ-TENANT-025, -026 | — | — |
| CHANGED | REQ-TENANT-007 | search / sort allow-list + `contactEmail`, `countryCode`, `suspendedAt` | — | tenant/service/TenantService.java (`ALLOWED_SORT_FIELDS`) | `TenantProfileIntegrationTest` |

Error codes — delta
| Code | HTTP | Code location |
|---|---|---|
| `TENANT_SUSPENSION_REASON_REQUIRED` | 400 | tenant/domain/TenantDomain.java; tenant/exception/TenantErrorCodes.java |
| `TENANT_ADMIN_RESET_PLATFORM` | 422 | tenant/domain/TenantDomain.java; tenant/exception/TenantErrorCodes.java (review round 1) |
| `TENANT_ADMIN_NOT_FOUND` | 404 | tenant/domain/TenantDomain.java; tenant/exception/TenantErrorCodes.java |
| `TENANT_ADMIN_NOT_SUPER` | 422 | tenant/domain/TenantDomain.java; tenant/exception/TenantErrorCodes.java |
Referenced: SEC `SEC-400-PASSWORD-POLICY` (400, admin-reset). i18n: one `tenant-maturity B` block in both bundles.

Permissions — delta: none (plan §0 D5). Audit actions — delta: `ADMIN_PASSWORD_RESET` (written by SEC inside
the target tenant) and `TENANT_ADMIN_RESET` (written by TENANT in PLATFORM, review round 1). Decisions — delta: none (no alternative was weighed that needs an ADR; the choices are in
`srs-tenant.md` B10).

Counts after this addendum: REQ 28 · AC 28 · RULE 13 · ENT 1 · SCR-REQ 1 · XM 2.
Last sequence per atom: REQ: 028 · AC: 028 · ENT: 001 · RULE: 017 (012 … 015 reserved) · SCR-REQ: 001 · XM: 002 · US: 011 · POL: 013 · DBF: 042

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package E — tenant branding (logo, brand colour, `/api/v1/tenant/me`, public branding)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-tenant.md` → "Implementation Addendum — erp-core 1.3.0" (package E
block, E1–E11). Ids continue from the highest ever issued (REQ / AC 028, RULE 017, XM 002; RULE-TENANT-012 … 015
reserved for the analysis-coverage work's as-built rules).

Entities — delta
| Kind | ENT id | Delta | Code location |
|---|---|---|---|
| CHANGED | ENT-TENANT-001 Tenant | + `logoFileId` (soft reference, XM-TENANT-003), `brandColor` (V20) — DBF-TENANT-043, -044 | tenant/entity/Tenant.java; V20__tenant_branding.sql |

Consumed — delta (TENANT still reads no other module's table)
| Kind | Id | Owner | Surface | Used by |
|---|---|---|---|---|
| NEW | XM-TENANT-003 | FILE | SOFT-REF `CORE_TENANT.LOGO_FILE_ID` → `FILE_DOCUMENT.ID` (no FK); `FileImageStoreApi.storePublicImage` / `discard` (XM-FILE-002), `FileDocumentLookupApi.publicUrl` (XM-FILE-001), inside `TenantContext.callAs(id)` | logo endpoints, `logoUrl` of every tenant response |

Exposed — delta
| Kind | Surface | Consumers |
|---|---|---|
| NEW | HTTP `GET /api/v1/tenant/me`, `GET /api/v1/public/tenants/{tenantCode}/branding` (`TenantBrandingResponse`) | frontend shell and login page (plan §8 F2) |

Screens — delta
| Kind | SCR-REQ id | Delta |
|---|---|---|
| CHANGED | SCR-REQ-TENANT-001 PLATFORM_TENANTS | B1 + logo and brand colour; B3 branding row (file input, preview, remove, colour); B5 + `PUT` / `DELETE /{id}/logo`, `PATCH /{id}/branding`; B4 unchanged (D5) |

Requirements — new / changed items
| Kind | Id | Title | Traces | Code location (primary) | Verified by |
|---|---|---|---|---|---|
| NEW | REQ-TENANT-029 / AC-TENANT-029 | Tenant logo set by the platform administrator (`PUT` / `DELETE /{id}/logo`) | US-TENANT-012; POL-TENANT-014; RULE-TENANT-018, -019, -020; ADR-TENANT-005 | tenant/service/TenantService.java (`setLogo`, `removeLogo`); tenant/service/TenantLogoUrls.java | `TenantBrandingIntegrationTest`; TC-CORE-TENANT-038, -040, -041, -044, -045, TC-CORE-PLATFORM-005 |
| NEW | REQ-TENANT-030 / AC-TENANT-030 | Brand colour (`PATCH /{id}/branding`) | US-TENANT-012; POL-TENANT-014; RULE-TENANT-020, -021 | tenant/service/TenantService.java (`updateBranding`) | `TenantBrandingIntegrationTest`, `TenantDomainTest`; TC-CORE-TENANT-042 |
| NEW | REQ-TENANT-031 / AC-TENANT-031 | Branding of the token's tenant (`GET /api/v1/tenant/me`, any realm) | US-TENANT-013; POL-TENANT-007, -014 | tenant/service/TenantBrandingService.java; autoconfigure/ErpCoreSecurityAutoConfiguration.java (`TENANT_ME_PATH`) | `TenantBrandingIntegrationTest`; TC-CORE-TENANT-039 |
| NEW | REQ-TENANT-032 / AC-TENANT-032 | Public branding by tenant code (`GET /api/v1/public/tenants/{tenantCode}/branding`) | US-TENANT-014; POL-TENANT-008; RULE-TENANT-006, -012, -022 | tenant/service/TenantBrandingService.java; autoconfigure/ErpCoreProperties.java (`DEFAULT_PATH_TENANT_PATHS`) | `TenantBrandingIntegrationTest`; TC-CORE-TENANT-043, -046 |
| NEW | RULE-TENANT-018 | Tenant logo: PUBLIC, ≤ 1 MB, PNG / JPEG / WebP / plain SVG, in the tenant's own rows, one per tenant, discard on replace / remove | REQ-TENANT-029 | tenant/domain/TenantDomain.java (`assertLogoAccepted`, `LOGO_*`) | `TenantDomainTest`, `TenantBrandingIntegrationTest` |
| NEW | RULE-TENANT-019 | PLATFORM may carry a logo; the platform mark is a static frontend asset | REQ-TENANT-029 | — (no refusal) | `TenantBrandingIntegrationTest` |
| NEW | RULE-TENANT-020 | Branding is written by the platform only (D5) | REQ-TENANT-029, -030 | autoconfigure/ErpCoreSecurityAutoConfiguration.java (`PLATFORM_PATHS`); tenant/service/TenantService.java (`@PreAuthorize`) | `TenantBrandingIntegrationTest`; TC-CORE-PLATFORM-005 |
| NEW | RULE-TENANT-021 | Brand colour `^#[0-9A-Fa-f]{6}$` or null, stored upper-case; `CHK_CORE_TENANT_BRAND_COLOR` | REQ-TENANT-030 | tenant/domain/TenantDomain.java (`assertBrandColorValid`); tenant/entity/Tenant.java (normalisation) | `TenantDomainTest`; TC-CORE-TENANT-042 |
| NEW | RULE-TENANT-022 | Public branding rate limit per client address (IPv6 by /64), counted before the tenant lookup (`erp.core.tenant.public-branding-rate-limit.*`, 60 / 1 min), `Retry-After` on 429, buckets expire after `period` unused, ≤ 10 000 keys (review round 1) | REQ-TENANT-032 | tenant/security/PublicBrandingRateLimitFilter.java | `PublicBrandingRateLimitFilterTest`, `TenantBrandingIntegrationTest.publicBranding_isRateLimitedPerClientAddress_unknownCodesIncluded`; TC-CORE-TENANT-046 |
| CHANGED | RULE-TENANT-012, RULE-TENANT-006 | + the public branding path (path tenant; suspended → 403) | REQ-TENANT-032 | autoconfigure/ErpCoreProperties.java; tenant/domain/TenantDomain.java (`assertServed`) | `TenantBrandingIntegrationTest` |
| CHANGED | US-TENANT-005 | a second path-tenant path (public branding) | — | — | — |

Error codes — delta
| Code | HTTP | Code location |
|---|---|---|
| `TENANT_LOGO_INVALID` | 400 | tenant/domain/TenantDomain.java; tenant/exception/TenantErrorCodes.java |
| `TENANT_BRAND_COLOR_INVALID` | 400 | tenant/domain/TenantDomain.java; tenant/exception/TenantErrorCodes.java |
| `TENANT_BRANDING_RATE_LIMITED` | 429 | tenant/security/PublicBrandingRateLimitFilter.java; tenant/exception/TenantErrorCodes.java |
i18n: one `tenant-maturity E` block in both bundles.

Permissions — delta: none (plan §0 D5). Audit actions — delta: `TENANT_LOGO_CHANGED` (written by TENANT in the
target tenant and in PLATFORM). Configuration — delta: `erp.core.tenant.public-branding-rate-limit.capacity` /
`period` (NEW); `erp.core.tenant.path-tenant-paths` default (CHANGED).

Decisions — delta
| Kind | ADR | Subject | Status |
|---|---|---|---|
| NEW | ADR-TENANT-005 | The tenant logo is set by the platform administrator from `PLATFORM_TENANTS`; no tenant self-service screen in 1.3.0 | ACCEPTED (decision D5) |

Counts after this addendum: REQ 32 · AC 32 · RULE 18 · ENT 1 · SCR-REQ 1 · XM 3.
Last sequence per atom: REQ: 032 · AC: 032 · ENT: 001 · RULE: 022 (012 … 015 reserved) · SCR-REQ: 001 · XM: 003 · US: 014 · POL: 014 · DBF: 044 · ADR: 005

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C12 — tenant lifecycle events (plan §5 C.1) and the per-tenant token cut-off with `POST /{id}/revoke-tokens` (plan §5 C.2)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-tenant.md` → "Implementation Addendum — erp-core 1.3.0" (package C12
block, C1–C11). Ids continue from the highest ever issued (REQ / AC 032, RULE 022, POL 014, US 014; RULE-TENANT-012
… 015 reserved for the analysis-coverage work's as-built rules).

Entities — delta
| Kind | ENT id | Delta | Code location |
|---|---|---|---|
| CHANGED | ENT-TENANT-001 Tenant | `tokensInvalidBefore` (DBF-TENANT-042) enforced and also written by revoke-tokens (`revokeTokens(Instant)`); no new column | tenant/entity/Tenant.java |

Exposed — delta
| Kind | Id | Surface | Consumers |
|---|---|---|---|
| CHANGED | XM-TENANT-001 | `TenantLookupApi` + `boolean isActive(Long tenantId)` (uncached; reads as PLATFORM without a current tenant) | NOTIF (claim, requeue) |
| NEW | — | root-package `TenantTokenFacts(tenantId, issuedAt)` + `REQUEST_ATTRIBUTE` | SEC `JwtAuthenticationFilter` (writes) |
| NEW | — | events `TenantSuspendedEvent`, `TenantActivatedEvent` (published after commit by `TenantService.updateStatus`) | SEC, NOTIF, applications |
| NEW | — | HTTP `POST /api/v1/platform/tenants/{id}/revoke-tokens` (`TenantTokenRevocationResponse`) | frontend `PLATFORM_TENANTS` |

Consumed — delta
| Kind | Owner | Surface | Used by |
|---|---|---|---|
| NEW | SEC | `SecAdminRecoveryApi.terminateAllSessions()` (REQ-SEC-093), inside `TenantContext.callAs(id)` | revoke-tokens |

Screens — delta
| Kind | SCR-REQ id | Delta |
|---|---|---|
| CHANGED | SCR-REQ-TENANT-001 PLATFORM_TENANTS | B1 + revoke a tenant's tokens; B3 confirmation dialog; B5 + `POST /{id}/revoke-tokens`; B4 unchanged (D5) |

Requirements — new / changed items
| Kind | Id | Title | Traces | Code location (primary) | Verified by |
|---|---|---|---|---|---|
| NEW | REQ-TENANT-033 / AC-TENANT-033 | Tenant lifecycle events (`TenantSuspendedEvent`, `TenantActivatedEvent` after commit; SEC ends sessions; NOTIF holds queued rows) | US-TENANT-003; POL-TENANT-002; RULE-TENANT-006 | tenant/service/TenantService.java (`updateStatus`); events/TenantSuspendedEvent.java, events/TenantActivatedEvent.java | `TenantLifecycleEventsIntegrationTest`, `NotificationSuspendedTenantIntegrationTest` (NOTIF hold); TC-CORE-TENANT-047 |
| NEW | REQ-TENANT-034 / AC-TENANT-034 | Per-tenant token cut-off (401 `TENANT_TOKEN_REVOKED`, both realms, `/tenant/me`) | US-TENANT-003, -004, -015; POL-TENANT-015; RULE-TENANT-023; ADR-TENANT-002 | tenant/security/TenantResolutionFilter.java; tenant/domain/TenantDomain.java (`isTokenRevoked`); sec/security/JwtAuthenticationFilter.java | `TenantTokenCutOffIntegrationTest`, `PlatformTenantApiIntegrationTest`; TC-CORE-TENANT-047, -048 |
| NEW | REQ-TENANT-035 / AC-TENANT-035 | Revoke a tenant's tokens (`POST /{id}/revoke-tokens`) | US-TENANT-015; POL-TENANT-015, -006; RULE-TENANT-023, -024 | tenant/service/TenantService.java (`revokeTokens`) | `TenantTokenCutOffIntegrationTest`; TC-CORE-TENANT-048 … 050 |
| NEW | RULE-TENANT-023 | Per-tenant token cut-off: `iat` (s) < cut-off truncated to the second → 401 `TENANT_TOKEN_REVOKED`; also for a token SEC dropped, on non-public paths; status first; revoke-tokens' cut-off = the next whole second (review round 1) | REQ-TENANT-034, -035 | tenant/domain/TenantDomain.java; tenant/security/TenantResolutionFilter.java | `TenantDomainTest`, `TenantTokenCutOffIntegrationTest` |
| NEW | RULE-TENANT-024 | PLATFORM's tokens are not revoked (422 `TENANT_REVOKE_TOKENS_PLATFORM`) | REQ-TENANT-035 | tenant/domain/TenantDomain.java (`assertTokenRevocationAllowed`) | `TenantDomainTest`, `TenantTokenCutOffIntegrationTest`; TC-CORE-TENANT-050 |
| CHANGED | RULE-TENANT-006 | + SEC ends the suspended tenant's sessions; NOTIF holds its queued rows; a dropped token of a suspended tenant still answers 403 `TENANT_SUSPENDED` | REQ-TENANT-033 | sec/service/TenantSuspendedSessionListener.java; notif/service/NotificationDeliveryProcessor.java; tenant/security/TenantResolutionFilter.java | `TenantLifecycleEventsIntegrationTest`; TC-CORE-TENANT-022, -047 |
| CHANGED | RULE-TENANT-016 | the activation's cut-off is enforced; transitions publish the lifecycle events | REQ-TENANT-033, -034 | tenant/service/TenantService.java | `PlatformTenantApiIntegrationTest`, `TenantLifecycleEventsIntegrationTest` |
| CHANGED | REQ-TENANT-012 | the token tenant is also checked against the cut-off; a dropped token is checked for its tenant before the header source (review round 1: replaces two citations of rule ids not defined on main) | REQ-TENANT-034 | tenant/security/TenantResolutionFilter.java | `TenantTokenCutOffIntegrationTest` |
| CHANGED | US-TENANT-003, US-TENANT-004 | re-activation cuts off earlier tokens; suspension ends sessions | — | — | — |

Error codes — delta
| Code | HTTP | Code location |
|---|---|---|
| `TENANT_TOKEN_REVOKED` | 401 | tenant/security/TenantResolutionFilter.java; tenant/exception/TenantErrorCodes.java |
| `TENANT_REVOKE_TOKENS_PLATFORM` | 422 | tenant/domain/TenantDomain.java; tenant/exception/TenantErrorCodes.java |
| `TENANT_REVOKE_SESSIONS_FAILED` | 500 | tenant/service/TenantService.java; tenant/exception/TenantErrorCodes.java (review round 1) |
i18n: one `tenant-maturity C12` block in both bundles.

Permissions — delta: none (plan §0 D5). Audit actions — delta: `TOKENS_REVOKED` (written by TENANT in the target
tenant and in PLATFORM). Configuration — delta: none.

Decisions — delta
| Kind | ADR | Subject | Status |
|---|---|---|---|
| NEW | ADR-TENANT-002 | Per-tenant token cut-off (`TOKENS_INVALID_BEFORE`) instead of a `jti` denylist | ACCEPTED (erp-core 1.3.0, package C12) |

Counts after this addendum: REQ 35 · AC 35 · RULE 20 · ENT 1 · SCR-REQ 1 · XM 3.
Last sequence per atom: REQ: 035 · AC: 035 · ENT: 001 · RULE: 024 (012 … 015 reserved) · SCR-REQ: 001 · XM: 003 · US: 015 · POL: 015 · DBF: 044 · ADR: 005 (002 used by this block; 003, 004 reserved for C.4, C.6)

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C6 — `ScopedValue` spike for `TenantContext`, go / no-go (plan §0 D6, §5 C.6)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-tenant.md` → "Implementation Addendum — erp-core 1.3.0" (package C6 block,
C6-1). No requirement, rule, entity, endpoint, error code, permission, property or migration.

Decisions — delta
| Kind | ADR | Subject | Status |
|---|---|---|---|
| NEW | ADR-TENANT-004 | `ScopedValue` for `TenantContext` — spike with go / no-go criteria (H1–H4, B1/B2) | PROPOSED (pending the spike, may slip to 1.4.0) |

Last sequence per atom: unchanged (REQ 035 · AC 035 · RULE 024 · POL 015 · US 015 · XM 003 · DBF 044) · ADR: 005 (004 used by this block; 003 reserved for C.4)
