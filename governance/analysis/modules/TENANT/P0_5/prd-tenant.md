# PRD — المستأجرون / Tenant (TENANT)
══════════════════════════════════════════════════════════════════
Module          : TENANT     Version : v1 (as-built baseline, erp-core 1.2.0)
Source artifacts: platform-summary, module-registry-tenant, business-policies-tenant
Stories         : 8   Policies covered : 11/11   Deferred : 0
Status          : AS-BUILT — every story is implemented; the code location is in `Source`
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`. HTTP verification: the `TC-CORE-TENANT-*`
cases of `docs/test-api/core-test-plan.md`.

## USER STORIES

US-TENANT-001
  Title          : تجهيز مستأجر مع مديره الأول / Provision a tenant with its first administrator
  Story          : As a platform operator, I need to create a tenant together with its first administrator in one step, so that the new organisation can sign in and manage itself immediately.
  Priority       : HIGH — the module's entry point (05-STEP task 1)
  Success metric : the new administrator signs in with `X-Tenant-Code` (TC-CORE-TENANT-005, -006)
  Traces         : POL-TENANT-001, POL-TENANT-004, POL-TENANT-006, POL-TENANT-010, POL-TENANT-011
  Source         : tenant/controller/PlatformTenantController.java:41-46; tenant/service/TenantService.java:68-103
  Status         : AS-BUILT

US-TENANT-002
  Title          : عرض المستأجرين والبحث فيهم / List, view and search tenants
  Story          : As a platform operator, I need to list, open and search tenants, so that I can see which organisations exist and in which state.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-TENANT-006
  Source         : tenant/controller/PlatformTenantController.java:48-67; tenant/service/TenantService.java:105-139
  Status         : AS-BUILT

US-TENANT-003
  Title          : تعليق مستأجر وإعادة تفعيله / Suspend and re-activate a tenant
  Story          : As a platform operator, I need to suspend a tenant and later re-activate it, so that I can stop an organisation's access at once without losing its data.
  Priority       : HIGH
  Success metric : a suspended tenant's logins and already issued tokens are refused (TC-CORE-TENANT-021, -022, -026)
  Traces         : POL-TENANT-002, POL-TENANT-003, POL-TENANT-005, POL-TENANT-006
  Source         : tenant/controller/PlatformTenantController.java:69-75; tenant/service/TenantService.java:141-160
  Status         : AS-BUILT

US-TENANT-004
  Title          : الدخول إلى مستأجر / Work inside one tenant
  Story          : As any user (staff or customer), I need every request to run inside my tenant only, so that I see and change only my organisation's data.
  Priority       : HIGH
  Success metric : cross-tenant reads answer "not found" (TC-CORE-TENANT-014, -015, -016, -025)
  Traces         : POL-TENANT-007, POL-TENANT-008, POL-TENANT-009
  Source         : tenant/security/TenantResolutionFilter.java:81-128; common/domain/AuditableEntity.java:35-37
  Status         : AS-BUILT

US-TENANT-005
  Title          : رابط عام يحمل المستأجر / Public URL carrying its tenant
  Story          : As an anonymous visitor (e.g. of a storefront), I need a public file URL that names its tenant in the path, so that it works in a plain browser without a header or token.
  Priority       : MEDIUM
  Success metric : TC-CORE-TENANT-023 (suspended tenant refused on the path too)
  Traces         : POL-TENANT-008, POL-TENANT-009
  Source         : tenant/security/TenantResolutionFilter.java:131-158; autoconfigure/ErpCoreProperties.java:252-261
  Status         : AS-BUILT

US-TENANT-006
  Title          : تجهيز بيانات الوحدة للمستأجر الجديد / Set up a module's data for a new tenant
  Story          : As a module (core or application) that owns reference data every tenant needs, I need a hook that runs when a tenant is created, so that the new tenant starts with my data without the tenant module touching my tables.
  Priority       : HIGH — without it a new tenant has no roles, lookups, templates or series
  Success metric : provisioning copies the PLATFORM catalog (TC-CORE-TENANT-012, -025)
  Traces         : POL-TENANT-004, POL-TENANT-010
  Source         : tenant/TenantProvisioningContributor.java:22-31; docs/CONSUMING.md §3 "Reference data for new tenants"
  Status         : AS-BUILT

US-TENANT-007
  Title          : قراءة رمز المستأجر من معرّفه / Read a tenant's code from its id
  Story          : As another module, I need the code of the current tenant, so that I can build tenant-carrying URLs and document numbers.
  Priority       : LOW
  Success metric : —
  Traces         : —  (scope only; supports POL-TENANT-008 for path-tenant URLs)
  Source         : tenant/crossmodule/TenantLookupApi.java:10-14
  Status         : AS-BUILT

US-TENANT-008
  Title          : تشغيل عمل النظام داخل مستأجر / Run system work as a tenant
  Story          : As system code running outside a request (start-up runner, scheduler, async listener), I need to run as a given tenant and fail fast when I forget to, so that no work ever runs against the wrong tenant or none.
  Priority       : HIGH
  Success metric : without `runAs` the work fails with `TENANT_CONTEXT_MISSING` (TenantContextIntegrationTest)
  Traces         : POL-TENANT-007
  Source         : tenant/TenantContext.java:47-53, :71-92; tenant/config/TenantIdentifierResolver.java:36-42
  Status         : AS-BUILT

## THE PROVISIONING STORY (US-TENANT-001 + US-TENANT-006, as built)
1. The platform operator (PLATFORM tenant, `PLATFORM_TENANT_MANAGE`) posts the tenant (`code`, `nameAr`,
   `nameEn`) and its first administrator (`adminUsername`, `adminEmail`, `adminPassword`,
   `adminFullNameAr`, `adminFullNameEn`) — tenant/dto/TenantCreateRequest.java:25-63.
2. `TenantDomain.create` checks the code format and that it is free — tenant/domain/TenantDomain.java:34-40.
3. The `CORE_TENANT` row is inserted (`ACTIVE`); a concurrent duplicate is answered 409 —
   tenant/service/TenantService.java:76-83.
4. In the same transaction every `TenantProvisioningContributor` runs, lowest `order()` first —
   tenant/service/TenantService.java:85-95:
   - SEC (0): copies PLATFORM's catalog roles `SYS_ADMIN`, `CU_ADMIN`, `NOTIF_ADMIN`, `FILE_ADMIN` (with
     `IS_SUPER`) and their module/screen/action grants minus the `PLATFORM` module and
     `PLATFORM_TENANT_MANAGE`; creates the administrator `ACTIVE`, realm `STAFF`, BCrypt-hashed, holding
     `SYS_ADMIN`; aborts the whole provisioning when PLATFORM has no `SYS_ADMIN` —
     sec/tenant/SecTenantProvisioningContributor.java:40, :66-148.
   - MDL (10): copies every lookup type and value — mdl/tenant/MdlTenantProvisioningContributor.java:31-55.
   - NOTIF (20): copies channel configurations without `CONFIG_JSON` and templates without
     `ATTACHMENT_FILE_ID` — notif/tenant/NotifTenantProvisioningContributor.java:32-53.
   - SEQUENCE (40): copies each series' anchor row with `NEXT_VALUE = 1` —
     sequence/tenant/SequenceTenantProvisioningContributor.java:32-44.
5. `TenantCreatedEvent` is published (tenant = the new tenant) and the tenant is returned 201 —
   tenant/service/TenantService.java:99-102.
6. The administrator signs in with `X-Tenant-Code: <code>`.

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-TENANT-001 | POL-TENANT-001, POL-TENANT-004, POL-TENANT-006, POL-TENANT-010, POL-TENANT-011 | tenant/service/TenantService.java:68-103 |
| US-TENANT-002 | POL-TENANT-006 | tenant/service/TenantService.java:105-139 |
| US-TENANT-003 | POL-TENANT-002, POL-TENANT-003, POL-TENANT-005, POL-TENANT-006 | tenant/service/TenantService.java:141-160 |
| US-TENANT-004 | POL-TENANT-007, POL-TENANT-008, POL-TENANT-009 | tenant/security/TenantResolutionFilter.java:81-128 |
| US-TENANT-005 | POL-TENANT-008, POL-TENANT-009 | tenant/security/TenantResolutionFilter.java:131-158 |
| US-TENANT-006 | POL-TENANT-004, POL-TENANT-010 | tenant/TenantProvisioningContributor.java:22-31 |
| US-TENANT-007 | — (scope only) | tenant/crossmodule/TenantLookupApi.java:10-14 |
| US-TENANT-008 | POL-TENANT-007 | tenant/TenantContext.java:47-92 |
Every policy POL-TENANT-001 … POL-TENANT-011 appears in at least one row above (001, 011 → US-001;
002, 003, 005 → US-003; 004, 010 → US-001/US-006; 006 → US-001/002/003; 007 → US-004/US-008;
008, 009 → US-004/US-005).

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | Row-level (discriminator) or schema-per-tenant? | row-level, shared schema | fixed decision of erp-core plan step 05, as built | ADR-TENANT-001 |
No other question: the stories describe built behaviour, read from the code.

## DEFERRED
| US | Reason | Activation trigger |
|---|---|---|
| (rename, delete or measure a tenant) | not built (module-registry-tenant.md; business-policies-tenant.md SCOPE EXCEPTIONS) | tenant-maturity plan package B (1.3.0 addenda; edit and usage only — no delete) |
| (schema-per-tenant, RLS, billing, per-tenant feature flags) | out of scope (ADR-TENANT-001; 05-STEP "Out of scope") | explicit future request |

## APPROVAL
Approved by : n/a — as-built baseline (the stories describe implemented behaviour, erp-core 1.2.0)   Date : 2026-10-07
Later changes are appended as "Implementation Addendum — erp-core 1.3.0" sections, never by rewriting
the stories above.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package B — tenant level 1 (edit, suspension facts, admin-reset, usage)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

NEW stories (ids continue from US-TENANT-008)

US-TENANT-009
  Title          : تعديل أسماء المستأجر وملفه / Edit a tenant's names and profile
  Story          : As a platform operator, I need to correct a tenant's names and keep its contact e-mail and phone, country, default language, time zone and notes, so that the record describes the organisation — without ever changing its code.
  Priority       : MEDIUM
  Success metric : `PUT /{id}` echoes the values and the code is unchanged (TC-CORE-TENANT-028)
  Traces         : POL-TENANT-001, POL-TENANT-006
  Source         : `../P1/srs-tenant.md` REQ-TENANT-025
  Status         : IMPLEMENTED (erp-core 1.3.0)

US-TENANT-010
  Title          : استعادة مدير مستأجر / Recover a tenant's administrator
  Story          : As a platform operator, I need to set a new password for a tenant's super administrator when that tenant is locked out, so that the organisation regains access without a database intervention.
  Priority       : HIGH
  Success metric : the administrator signs in with the new password and must change it (TC-CORE-TENANT-034)
  Traces         : POL-TENANT-006, POL-TENANT-011, POL-TENANT-013
  Source         : `../P1/srs-tenant.md` REQ-TENANT-027
  Status         : IMPLEMENTED (erp-core 1.3.0)

US-TENANT-011
  Title          : أرقام استخدام المستأجر / Tenant usage figures
  Story          : As a platform operator, I need a tenant's counts (staff, customers, open sessions, documents and their bytes, notifications of the last 30 days), so that I can see how large and how active an organisation is.
  Priority       : MEDIUM
  Success metric : a fresh tenant answers one staff user and zeros elsewhere (TC-CORE-TENANT-027)
  Traces         : POL-TENANT-006, POL-TENANT-007
  Source         : `../P1/srs-tenant.md` REQ-TENANT-028
  Status         : IMPLEMENTED (erp-core 1.3.0)

CHANGED behaviour of existing stories
| Story | Delta | Source |
|---|---|---|
| US-TENANT-002 list, view, search | responses carry the profile and the suspension facts; search and sort gain `contactEmail`, `countryCode`, `suspendedAt` | REQ-TENANT-007 (CHANGED) |
| US-TENANT-003 suspend and re-activate | a suspension needs a reason (3..500) and records `suspendedAt` / `suspendedBy` / `suspensionReason`; re-activation clears them and stores a token cut-off (`TOKENS_INVALID_BEFORE`, enforced by package C.2) | REQ-TENANT-026; POL-TENANT-012 |

TRACEABILITY — delta
| US | Traces (POL) | Source |
|---|---|---|
| US-TENANT-009 | POL-TENANT-001, POL-TENANT-006 | REQ-TENANT-025 |
| US-TENANT-010 | POL-TENANT-006, POL-TENANT-011, POL-TENANT-013 | REQ-TENANT-027 |
| US-TENANT-011 | POL-TENANT-006, POL-TENANT-007 | REQ-TENANT-028 |
| US-TENANT-003 (CHANGED) | + POL-TENANT-012 | REQ-TENANT-026 |
Every policy POL-TENANT-001 … 013 appears in at least one row (012 → US-003, 013 → US-010).

DEFERRED — delta
| Kind | US | Reason | Activation trigger |
|---|---|---|---|
| CHANGED | (rename, measure a tenant) | leaves DEFERRED: US-TENANT-009, US-TENANT-011 | this addendum |
| unchanged | (delete a tenant) | POL-TENANT-005 | — |
| NEW | (quotas, `ARCHIVED`, self-signup switch, per-tenant rate limits, platform-set tenant settings) | level 2 | plan §0 D2, §9 |

APPROVAL — delta: scope APPROVED by the platform owner on 2026-10-07 (plan header); the stories are
IMPLEMENTED with package B and checked against the code in its check commit.

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package E — tenant branding (logo, brand colour, `/api/v1/tenant/me`, public branding; plan §0 D5, §7)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

NEW stories (ids continue from US-TENANT-011)

US-TENANT-012
  Title          : شعار المستأجر ولون علامته / A tenant's logo and brand colour
  Story          : As a platform operator, I need to upload, replace or remove a tenant's logo and set an optional brand colour from the tenants screen, so that the organisation's users see their own mark beside the platform's.
  Priority       : MEDIUM
  Success metric : the uploaded logo's URL is served under the tenant's code and shown by `/tenant/me` (TC-CORE-TENANT-038, -039)
  Traces         : POL-TENANT-014, POL-TENANT-006
  Source         : `../P1/srs-tenant.md` REQ-TENANT-029, REQ-TENANT-030
  Status         : IMPLEMENTED (erp-core 1.3.0)

US-TENANT-013
  Title          : قراءة علامة مستأجري / Read my tenant's branding
  Story          : As any signed-in user (staff or customer), I need my tenant's code, names, logo URL, brand colour and default language, so that the application shell can show them after login.
  Priority       : MEDIUM
  Success metric : `/tenant/me` answers exactly the six branding fields (TC-CORE-TENANT-039)
  Traces         : POL-TENANT-007, POL-TENANT-014
  Source         : `../P1/srs-tenant.md` REQ-TENANT-031
  Status         : IMPLEMENTED (erp-core 1.3.0)

US-TENANT-014
  Title          : علامة المستأجر قبل الدخول / Tenant branding before login
  Story          : As an anonymous visitor on the login page, I need the branding of the tenant whose code I typed, so that I see the right logo before I sign in.
  Priority       : MEDIUM
  Success metric : the public branding answers 200 / 404 / 403 / 429 as specified (TC-CORE-TENANT-043, -046)
  Traces         : POL-TENANT-008, POL-TENANT-014
  Source         : `../P1/srs-tenant.md` REQ-TENANT-032
  Status         : IMPLEMENTED (erp-core 1.3.0)

CHANGED behaviour of existing stories
| Story | Delta | Source |
|---|---|---|
| US-TENANT-002 list, view, search | responses carry `logoUrl` and `brandColor` | REQ-TENANT-029, -030 |
| US-TENANT-005 public URL carrying its tenant | a second path-tenant path: the public branding endpoint | REQ-TENANT-032 |

TRACEABILITY — delta
| US | Traces (POL) | Source |
|---|---|---|
| US-TENANT-012 | POL-TENANT-006, POL-TENANT-014 | REQ-TENANT-029, -030 |
| US-TENANT-013 | POL-TENANT-007, POL-TENANT-014 | REQ-TENANT-031 |
| US-TENANT-014 | POL-TENANT-008, POL-TENANT-014 | REQ-TENANT-032 |
Every policy POL-TENANT-001 … 014 appears in at least one row (014 → US-012 … 014).

DEFERRED — delta
| Kind | US | Reason | Activation trigger |
|---|---|---|---|
| NEW | (tenant self-service branding screen) | decision D5 | a later version (ADR-TENANT-005 alternative) |

APPROVAL — delta: scope APPROVED by the platform owner on 2026-10-07 (plan header, decision D5); the stories are
IMPLEMENTED with package E and checked against the code in its check commit.

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C12 — tenant lifecycle events and the per-tenant token cut-off (plan §5 C.1, C.2)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

NEW stories (ids continue from US-TENANT-014)

US-TENANT-015
  Title          : إبطال رموز مستأجر / Revoke a tenant's tokens
  Story          : As a platform operator, I need to invalidate every token a tenant's users hold without suspending the tenant, so that a compromised tenant starts again from fresh logins.
  Priority       : HIGH
  Success metric : after `revoke-tokens` every earlier token answers 401 `TENANT_TOKEN_REVOKED` and a fresh login works (TC-CORE-TENANT-048)
  Traces         : POL-TENANT-015, POL-TENANT-006
  Source         : `../P1/srs-tenant.md` REQ-TENANT-035
  Status         : IMPLEMENTED (erp-core 1.3.0)

CHANGED behaviour of existing stories
| Story | Delta | Source |
|---|---|---|
| US-TENANT-003 suspend and re-activate | a suspension ends every open session of the tenant and holds its queued notifications; a re-activation cuts off every token issued before it (users sign in again); both publish a lifecycle event | REQ-TENANT-033, -034; POL-TENANT-015 |
| US-TENANT-004 work inside one tenant | a token issued before the tenant's cut-off is refused 401 `TENANT_TOKEN_REVOKED`, whichever realm | REQ-TENANT-034 |

TRACEABILITY — delta
| US | Traces (POL) | Source |
|---|---|---|
| US-TENANT-015 | POL-TENANT-006, POL-TENANT-015 | REQ-TENANT-035 |
| US-TENANT-003 (CHANGED) | + POL-TENANT-015 | REQ-TENANT-034 |
Every policy POL-TENANT-001 … 015 appears in at least one row (015 → US-003, US-015).

APPROVAL — delta: scope APPROVED by the platform owner on 2026-10-07 (plan header); the story is IMPLEMENTED with
package C12 and checked against the code in its check commit.

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C4 — idempotent provisioning (plan §5 C.4)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

No new story. CHANGED behaviour of an existing story
| Story | Delta | Source |
|---|---|---|
| US-TENANT-001 provision a tenant | the create request may carry `Idempotency-Key`: a retry with the same key and body replays the first answer (`Idempotent-Replayed: true`) and creates nothing; another body under the same key → 409 `IDEMPOTENCY_KEY_CONFLICT`; keys expire after 24 h | REQ-TENANT-036; POL-TENANT-016 |

TRACEABILITY — delta
| US | Traces (POL) | Source |
|---|---|---|
| US-TENANT-001 (CHANGED) | + POL-TENANT-016 | REQ-TENANT-036 |
Every policy POL-TENANT-001 … 016 appears in at least one row (016 → US-001).

THE PROVISIONING STORY — delta: step 1 (the operator posts the tenant) may carry `Idempotency-Key`; when it does, the
stored answer commits with the tenant in step 4's transaction; nothing else changes.

APPROVAL — delta: scope APPROVED by the platform owner on 2026-10-07 (plan header); the story change is IMPLEMENTED
with package C4 and checked against the code in its check commit.

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C5 — tenant data export (plan §5 C.5)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

NEW story
| Kind | US | Title | Story | Actor | Traces (POL) | Source |
|---|---|---|---|---|---|---|
| NEW | US-TENANT-016 | تصدير بيانات مستأجر / Export a tenant's data | As a platform operator, I need one downloadable archive of a tenant's data written by every module, so that the organisation can take its data with it — without passwords, tokens, credentials or file contents. | platform operator | POL-TENANT-017 | plan §5 C.5 (`/{id}/export`, `TenantExportContributor`); REQ-TENANT-037 |

TRACEABILITY — delta
| US | Traces (POL) | Source |
|---|---|---|
| US-TENANT-016 (NEW) | POL-TENANT-017 | REQ-TENANT-037 |
Every policy POL-TENANT-001 … 017 appears in at least one row (017 → US-016).

APPROVAL — delta: scope APPROVED by the platform owner on 2026-10-07 (plan header); the story is IMPLEMENTED with
package C5 and checked against the code in its check commit.
