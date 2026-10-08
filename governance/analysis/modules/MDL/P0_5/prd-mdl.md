# PRD — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module          : MDL     Version : v1
Source artifacts: platform-summary, module-registry, business-policies
Stories         : 5   Policies covered : 6/6   Deferred : 0
Status          : DRAFT — awaiting prd-approval
══════════════════════════════════════════════════════════════════

## USER STORIES

US-MDL-001
  Title          : إدارة أنواع اللوكب / Manage lookup types
  Story          : As a platform administrator, I need to create, edit and deactivate a lookup type naming its owning module, so that every coded list has one authoritative, namespaced home.
  Priority       : HIGH
  Success metric : —
  Traces         : POL-MDL-002, POL-MDL-003
  Source         : lookup-module-plan-en.md §2-§3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-MDL-002
  Title          : إدارة قيم اللوكب عبر الشاشة العامة / Manage lookup values through the generic screen
  Story          : As a user authorized for a lookup type, I need one generic master-detail screen to pick the type and manage its values (code, bilingual labels, sort order, active flag), so that I never need a screen per list.
  Priority       : HIGH — plan names this the module's core mechanism (§3)
  Success metric : —
  Traces         : POL-MDL-004, POL-MDL-005, POL-MDL-006
  Source         : lookup-module-plan-en.md §3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-MDL-003
  Title          : قراءة القيم من أي وحدة مستهلكة / Read values from any consuming module
  Story          : As a consuming module's backend, I need to read a lookup type's active values by key, so that I never hardcode a coded value list of my own.
  Priority       : HIGH — this is the module's entire reason to exist (§2)
  Success metric : —
  Traces         : POL-MDL-001
  Source         : lookup-module-plan-en.md §2, §4
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-MDL-004
  Title          : تسجيل نوع لوكب جديد كبيانات / Register a new lookup type as data
  Story          : As a consuming module's integrator, I need to register my own lookup type here as data (naming my module as owner), so that adding a new list never requires touching MDL's code.
  Priority       : HIGH
  Success metric : —
  Traces         : POL-MDL-002, POL-MDL-003
  Source         : lookup-module-plan-en.md §4
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-MDL-005
  Title          : سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner
  Story          : As a platform administrator, I need to browse lookup types grouped by their owning module, so that I can find and audit a module's reference lists at a glance.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-MDL-002
  Source         : lookup-module-plan-en.md §7 (screen inventory row 2)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-MDL-001 | POL-MDL-002, POL-MDL-003 | lookup-module-plan-en.md §2-§3 |
| US-MDL-002 | POL-MDL-004, POL-MDL-005, POL-MDL-006 | lookup-module-plan-en.md §3 |
| US-MDL-003 | POL-MDL-001 | lookup-module-plan-en.md §2, §4 |
| US-MDL-004 | POL-MDL-002, POL-MDL-003 | lookup-module-plan-en.md §4 |
| US-MDL-005 | POL-MDL-002 | lookup-module-plan-en.md §7 |
Every policy POL-MDL-001 … POL-MDL-006 appears in at least one row above.

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
None — lookup-module-plan-en.md left no story's scope, priority or role genuinely
ambiguous; no dialogue question was required.

## DEFERRED
| US | Reason | Activation trigger |
None — every capability named in lookup-module-plan-en.md is represented by a story in
this v1 PRD.

## APPROVAL
Approved by : PENDING   Date : PENDING
Once approved, no stage may raise a question; P1 onward self-resolve
per the ambiguity rule (shared/GOVERNANCE-CORE.md).
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 06, 08, 10, 11
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository at that tag; `mdl/` abbreviates
`erp-core/src/main/java/com/erp/mdl/`. No US ids are minted here.

| Kind | Story | Delta | Source |
|---|---|---|---|
| CHANGED | US-MDL-001 Manage lookup types | Types are managed per tenant; a key is unique within the tenant. The owning module must exist **and** be active in SEC's code-defined registry. "Deactivate" is gated by the UPDATE action (no DELETE action exists) and answers 200 with the type; there is no reactivation. Every change is recorded in the platform audit log (deactivation as an update). The list is `POST /lookup-types/search` with generic filters (`key`, `ownerModuleCode`, `nameAr`, `nameEn`, `isActiveFl`, `createdAt`), paged 20 / max 200. | V10__tenant_schema.sql; mdl/service/LookupTypeService.java:80,120-133,144-159; DEVIATIONS [10] |
| CHANGED | US-MDL-002 Manage lookup values | Values are managed per tenant; a code is unique within its type in the tenant. The detail list is **paged** (default sort `sortOrder`), active and inactive alike; `sortOrder` is mandatory on create; reorder assigns 0-based positions to the submitted ids (a partial list is allowed); a value may be added under an inactive type; deactivation is gated by UPDATE and answers 200 with the value. Every change is audited. | mdl/service/LookupValueService.java:61-78,121-152,171-203; V10 |
| CHANGED | US-MDL-003 Read values from any consuming module | Two reads exist: the HTTP `GET /api/v1/mdl/lookups?type=` (gated by `PERM_MDL_LOOKUPS_VIEW`, returns full value rows) and the in-process `MdlLookupApi.readActiveValuesByKey` (`code`, `labelAr`, `labelEn`, `sortOrder`; no permission gate — ADR-MDL-046). Both answer the request tenant's values and refuse an unknown **or inactive** type with 404 `MDL-404-TYPE-KEY` instead of an empty list. Consumers today: FILE and NOTIF (their own lookup endpoints) and REPORT (LOOKUP parameters). No caching. | mdl/service/LookupConsumerService.java:47-62; mdl/crossmodule/MdlLookupApiImpl.java:51-66; docs/steps/05-report.md |
| CHANGED | US-MDL-004 Register a new lookup type as data | Registration is `POST /api/v1/mdl/lookup-types` at runtime or a Flyway `INSERT` (core `V8` seeds NOTIF's and FILE's four types for PLATFORM; an application's `V1000+` does the same); no registration SPI exists. A new tenant starts with a copy of PLATFORM's whole catalog (inactive rows included); types seeded for PLATFORM afterwards reach only tenants created later. The seeded types carry no protected flag and are editable like any other. | V8__mdl_seed.sql; mdl/tenant/MdlTenantProvisioningContributor.java; DEVIATIONS [05] |
| CHANGED | US-MDL-005 Lookup-type registry by owner | `POST /lookup-types/by-owner/search`, gated by `PERM_MDL_TYPE_REGISTRY_VIEW` (screen «سجل أنواع البيانات المرجعية» / "Master data type registry"); active types only, grouped by owner, not paged (paging fields ignored); optional `ownerModuleCode` / `key` filters. | mdl/service/LookupTypeService.java:173-200 |
| CHANGED | Roles (all stories) | Only `SYS_ADMIN` is seeded with MDL grants; the "owning-module lookup manager" and "consuming-module service account" roles the stories assume are not seeded (a tenant administrator may create them). | V7__sec_seed.sql:171 |

No story was added or removed; no user-facing capability beyond the five stories exists in MDL.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (CHANGELOG [Unreleased]); no MDL behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| Kind | Story | Delta | Source |
|---|---|---|---|
| CHANGED | US-MDL-003 Read values from any consuming module | FILE and NOTIF serve their MDL-stored lists through the shared `com.erp.common.lookup.LookupOptionResponse` / `OwnedLookups`; same JSON shape, same behaviour. `MdlLookupApi` unchanged. | erp-core/src/main/java/com/erp/common/lookup/; CHANGELOG [Unreleased] |
| CHANGED | US-MDL-001, US-MDL-002 | the duplicate-key / duplicate-code refusals are raised through `DomainRules.assertUnique`; same codes, no behaviour change. | mdl/domain/ |
