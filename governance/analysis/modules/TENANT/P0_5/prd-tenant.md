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
