## MODULE REGISTRY — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module Code    : MDL   (profile.vocabulary.module_prefixes)
Bounded context: organization
Layer / Type   : L1 / reference hub     Execution tier : 1.3
Source         : NEW
Knowledge      : new project/lookup-module-plan-en.md; profiles/erp/knowledge/erp-domain-standards.md §2-§3
Readiness      : READY
══════════════════════════════════════════════════════════════════

ENTITIES OWNED   (names only — entity IDs are assigned by P1)
| Entity (ar/en) | Kind (master / transactional / lookup / config / security) | PRIVATE / SHARED | Source |
|---|---|---|---|
| نوع اللوكب / LookupType | master | SHARED (owner) — every consuming module registers and reads its own types here | lookup-module-plan-en.md §2-§3 |
| قيمة اللوكب / LookupValue | lookup | SHARED (owner) — every consuming module reads/manages its own values here | lookup-module-plan-en.md §3 |

LOOKUPS OWNED    (value lists this module masters — MDL owns the MECHANISM only; see rule below)
| Lookup key | Description | Initial values (only those the user named) | Source |
None — MDL itself introduces no domain-specific coded list of its own; it is the generic
mechanism every OTHER module's lookup types run on top of. (MDL does not "consume itself.")
Rule (profile): all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs — this rule is MDL's own reason to exist.

LOOKUPS CONSUMED (from other modules)
| Lookup key | Owner code | READ-ONLY |
None.

SHARED ENTITIES CONSUMED
| Entity | Owner code | HARD-FK / SOFT-READ | Why |
| ModuleRegistry (ENT-SEC-004) | SEC | SOFT-READ | validate that a lookup type's declared owner module code is a real, registered platform module before accepting the registration — mirrors SEC's own RULE-SEC-004 pattern for screens, applied here to lookup types |

DEPENDENCIES
| Module code | HARD / SOFT / LOOKUP | What is consumed |
| SEC | SOFT | ModuleRegistry (owner-code validation only — see SHARED ENTITIES CONSUMED) |
ROOT: NO (has one SOFT dependency on SEC; still Tier 0 Foundation per KB §1 — a SOFT-READ
foundation dependency on another Tier-0 module does not change tiering)

AUTO-DECISIONS
AUTO: classified MDL's SEC dependency as SOFT-READ, not HARD-FK
  FROM: [KB:erp-domain-standards §5] "SOFT-READ: a read-only lookup by code — allowed in any direction"; a HARD-FK would make MDL's own schema depend physically on SEC's PK, which is heavier than the actual need (a one-time/per-write existence check)
  IF WRONG: promote to HARD-FK if a later requirement needs referential-integrity-level guarantees stronger than an application-level check — would need its own ADR.
AUTO: LookupType classified kind=master, LookupValue kind=lookup
  FROM: profiles/erp.yaml conventions.entity_defaults — LookupValue's fields (code, nameAr, nameEn, sortOrder, isActiveFl) match the `lookup` kind's default set exactly; LookupType (code, nameAr, nameEn, ownerModuleCode, isActiveFl) is closer to `master` (bilingual, coded, soft-deletable, not itself a coded VALUE of some other type)
  IF WRONG: none recommended — this is the plan's own vocabulary (§3 "a lookup is a type (master) + its values (detail)").
AUTO: tier/numbering 1.3 (Foundation, Tier 0)
  FROM: [KB:erp-domain-standards §1]
  IF WRONG: renumber if the platform later reprioritizes.

RESOLVED DECISIONS (dialogue, this module)
| # | Point | Recommended | Confirmed by user | Sources |
None — lookup-module-plan-en.md fully settles this module's P0 scope; no point required dialogue.

POLICIES OWNED (full text in business-policies-mdl.md)
POL-MDL-001, POL-MDL-002, POL-MDL-003, POL-MDL-004, POL-MDL-005, POL-MDL-006
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 06, 08, 10, 11
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository at that tag; `mdl/` abbreviates
`erp-core/src/main/java/com/erp/mdl/`. No ENT / XM ids are minted here.

ENTITIES OWNED — deltas
| Kind | Entity | Delta | Source |
|---|---|---|---|
| CHANGED | LookupType, LookupValue | tenant-scoped (`TENANT_ID`, Hibernate `@TenantId`) + optimistic lock (`VERSION`), neither on the wire; uniqueness per tenant; `@Audited` (deactivation audited as UPDATE — MDL never hard-deletes); `activate()` exists on both entities but no endpoint calls it | V10__tenant_schema.sql; mdl/entity/LookupType.java:34,87-93; docs/steps/05-report.md |
| CHANGED | LookupType | `key VARCHAR(80)`, names `VARCHAR(150)`; owner must exist **and** be active in SEC's registry (`SecModuleRegistryApi.isModuleActive`) | V3__mdl_schema.sql:26-29; mdl/service/LookupTypeService.java:80 |
| CHANGED | LookupValue | `sort_order NUMERIC` mapped to `Integer`, mandatory on create; a value may be created under an inactive type (no guard); no protected / system flag distinguishes seeded rows | V3:46; mdl/dto/LookupValueCreateRequest.java:37-40; mdl/service/LookupValueService.java:66-67 |

LOOKUPS OWNED — still none: MDL owns no key of its own (every key in its tables belongs to the module whose
code is in `OWNER_MODULE_CODE`).

LOOKUPS hosted (rows owned by other modules, seeded by raw `INSERT`, PLATFORM tenant, copied to every new tenant)
| Kind | Lookup key | Owner | Values | Source |
|---|---|---|---|---|
| NEW (seed) | NOTIF_CHANNEL | NOTIF | EMAIL, SMS, WHATSAPP, PUSH, INTERNAL | V8__mdl_seed.sql:28,43-47 |
| NEW (seed) | NOTIF_STATUS | NOTIF | PENDING, SENT, FAILED, CHANNEL_DISABLED | V8:29,49-52 |
| NEW (seed) | FILE_FILE_STATUS | FILE | ACTIVE, ARCHIVED, DELETED | V8:30,54-56 |
| NEW (seed) | FILE_FILE_TYPE | FILE | IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER | V8:31,58-62 |
| NEW (seed) | NOTIF_STATUS | NOTIF | + `QUEUED`, `SKIPPED_NO_PROVIDER` | V13__notif_async_inbox.sql §3a |
| NEW (seed) | NOTIF_CHANNEL | NOTIF | + `IN_APP` | V13 §3a |
No registration SPI exists: a module's types are seeded by its own Flyway `INSERT` (core `V8` / `V13`,
application `V1000+`) or created through `POST /api/v1/mdl/lookup-types`.

DEPENDENCIES — deltas
| Kind | Module | Kind of link | What | Source |
|---|---|---|---|---|
| CHANGED | SEC | SOFT-READ, in-process | `SecModuleRegistryApi.isModuleActive(code)` — `SEC_MODULE_REG.CODE` with `IS_ACTIVE_FL = TRUE` (the ModuleRegistry row must exist **and** be active); the registry is code-defined, synchronized at startup | mdl/service/LookupTypeService.java:68,80 |
| NEW | SEC | SPI | `MdlPermissions implements PermissionContributor` (module MDL, screens MDL_LOOKUPS, MDL_TYPE_REGISTRY, 4 actions) | mdl/permission/MdlPermissions.java; docs/steps/06-report.md |
| NEW | tenant | HARD FK + SPI | `CORE_TENANT` FK on both tables; `MdlTenantProvisioningContributor` (order 10, JDBC) copies all of PLATFORM's types and values — inactive rows included, re-linked by `KEY` — into a new tenant; no `@Audited` event for the copies | mdl/tenant/MdlTenantProvisioningContributor.java:26-55; DEVIATIONS [05] |
| NEW | audit | SOFT | `@Audited(entityType = "MDL_LOOKUP_TYPE" / "MDL_LOOKUP_VALUE")` → `CORE_AUDIT_EVENT` CREATE / UPDATE rows | DEVIATIONS [10] |
| NEW | events | — | MDL publishes and consumes no domain event (`com.erp.events`) | mdl/ (no reference) |
| NEW | caching | — | no `@Cacheable` / `@CacheEvict`; MDL is absent from the cache-eligibility register | mdl/service/LookupTypeService.java:47-51 |

EXPOSED SURFACE — deltas
| Kind | Surface | Delta | Source |
|---|---|---|---|
| NEW | `com.erp.mdl.crossmodule.MdlLookupApi.readActiveValuesByKey(String typeKey)` | → `List<LookupOptionView(code, labelAr, labelEn, sortOrder)>` ordered by `sortOrder`; `@Transactional(readOnly = true)`; **no `@PreAuthorize`** (ADR-MDL-046); throws `LocalizedException(NOT_FOUND, MDL-404-TYPE-KEY)` for an unknown or inactive type; empty list for an active type with no active value. The only MDL surface outside the module. | mdl/crossmodule/MdlLookupApi.java; mdl/crossmodule/MdlLookupApiImpl.java:51-66 |
| NEW | consumer FILE | `FileLookupService` fronts `FILE_FILE_TYPE`, `FILE_FILE_STATUS` on `GET /api/v1/files/lookups/{lookupKey}` (`isAuthenticated()`), translating the 404 into `FILE_LOOKUP_KEY_UNKNOWN` | erp-core/src/main/java/com/erp/file/service/FileLookupService.java:48-58 |
| NEW | consumer NOTIF | `NotificationLookupService` fronts `NOTIF_CHANNEL`, `NOTIF_STATUS` on `GET /api/v1/notifications/lookups/{lookupKey}` (`isAuthenticated()`), translating the 404 into `NOTIF_LOOKUP_KEY_UNKNOWN` | erp-core/src/main/java/com/erp/notif/service/NotificationLookupService.java:39-49 |
| NEW | consumer REPORT | `ReportService` validates `LOOKUP` report parameters against the active codes (an unknown or inactive type → `REPORT_PARAM_INVALID`) | erp-core/src/main/java/com/erp/report/service/ReportService.java:64,143-150; DEVIATIONS [11] |
| CHANGED | HTTP consumer read `GET /api/v1/mdl/lookups?type=` | gated by `PERM_MDL_LOOKUPS_VIEW`; answers 404 `MDL-404-TYPE-KEY` for an unknown **or inactive** type | mdl/service/LookupConsumerService.java:47-62 |

PERMISSIONS (exact authority strings — code-defined by `MdlPermissions`, seeded with the same names by `V7__sec_seed.sql:108-112`)
| Kind | Authority | Screen (as built) | Gates | Source |
|---|---|---|---|---|
| CHANGED | `PERM_MDL_LOOKUPS_VIEW` | MDL_LOOKUPS «قوائم البيانات المرجعية» / "Master data lookups" | `POST /lookup-types/search`, `POST /lookup-types/values/search`, `GET /lookups` (HTTP only — not the in-process API) | mdl/permission/MdlPermissions.java:19-20,31-32 |
| CHANGED | `PERM_MDL_LOOKUPS_CREATE` | MDL_LOOKUPS | `POST /lookup-types`, `POST /lookup-types/{id}/values` | mdl/permission/MdlPermissions.java:21-22 |
| CHANGED | `PERM_MDL_LOOKUPS_UPDATE` | MDL_LOOKUPS | `PUT /lookup-types/{id}`, `PUT /lookup-values/{id}`, `PATCH …/reorder`, **and** `DELETE /lookup-types/{id}`, `DELETE /lookup-values/{id}` (deactivation is UPDATE) | mdl/permission/MdlPermissions.java:23-27; V7:79-80 |
| REMOVED | `PERM_MDL_LOOKUPS_DELETE` | — | assumed by the SRS access summary; never created | V7:79-80,108-112 |
| CHANGED | `PERM_MDL_TYPE_REGISTRY_VIEW` | MDL_TYPE_REGISTRY «سجل أنواع البيانات المرجعية» / "Master data type registry" | `POST /lookup-types/by-owner/search` | mdl/permission/MdlPermissions.java:28-29,33-34 |
| CHANGED | role seeds | — | only `SYS_ADMIN` holds the MDL module grant and, derived, both screens and all four actions; no lookup-manager or service-account role | V7:171 and tiers 2–3 |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (CHANGELOG [Unreleased]); no MDL behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| Kind | Surface | Delta | Source |
|---|---|---|---|
| CHANGED | consumers FILE / NOTIF | answer `com.erp.common.lookup.LookupOptionResponse{code, labelAr, labelEn}` through `com.erp.common.lookup.OwnedLookups.read` (key trimmed / upper-cased; a non-owned key or MDL's 404 → the consumer's own 404); same JSON shape; `MdlLookupApi` and `LookupOptionView` unchanged | erp-core/src/main/java/com/erp/common/lookup/; CHANGELOG [Unreleased] |
| CHANGED | `LookupTypeDomain`, `LookupValueDomain` | duplicate refusals through `com.erp.common.domain.DomainRules.assertUnique`; same codes, no behaviour change | mdl/domain/LookupTypeDomain.java:48; mdl/domain/LookupValueDomain.java:34 |
