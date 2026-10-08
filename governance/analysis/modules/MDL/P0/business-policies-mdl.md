## BUSINESS POLICIES — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module   : MDL     Source of truth : new project/lookup-module-plan-en.md
Read by  : P0.5 (every user story cites the policies it serves)
══════════════════════════════════════════════════════════════════

CLIENT-SPECIFIC POLICIES   (only from user text or confirmed dialogue answers)

POL-MDL-001 — مركز واحد للقيم المرجعية / One central hub for reference data
  Statement (ar) : يجب على النظام أن يكون المخزن الوحيد على مستوى المنصة لقوائم القيم المُرمَّزة التي قد تحتاجها أكثر من وحدة.
  Statement (en) : The system shall be the sole platform-wide store of coded reference-value lists that more than one module may need.
  Pattern   : ubiquitous
  Trigger   : Any module needing a coded value list
  Rationale : منع انحراف القيم بين الوحدات (value drift)
  Source    : lookup-module-plan-en.md §2
  Status    : CONFIRMED

POL-MDL-002 — تسمية المالك لكل نوع لوكب / Owner namespacing per lookup type
  Statement (ar) : يجب على النظام تسجيل الوحدة المالكة لمعنى كل نوع لوكب.
  Statement (en) : The system shall record, for every lookup type, the module that owns its meaning.
  Pattern   : ubiquitous
  Trigger   : Lookup type registration
  Rationale : تخزين مركزي، لكن الملكية الدلالية تبقى للوحدة المُسجِّلة
  Source    : lookup-module-plan-en.md §2-§3
  Status    : CONFIRMED

POL-MDL-003 — رفض تسجيل نوع لوحدة غير مسجَّلة / Reject a type registered under an unregistered module
  Statement (ar) : إذا كان تسجيل نوع اللوكب يُسمّي وحدة غير مسجَّلة في وحدة الأمان، فيجب على النظام رفض التسجيل.
  Statement (en) : If a lookup type registration names a module that is not registered in the Security module, then the system shall reject the registration.
  Pattern   : unwanted
  Trigger   : Lookup type registration
  Rationale : سلامة مرجعية — لا نوع بلا مالك حقيقي
  Source    : lookup-module-plan-en.md §3; module-registry-mdl.md → SHARED ENTITIES CONSUMED
  Status    : CONFIRMED

POL-MDL-004 — شاشة عامة واحدة، لا شاشة لكل قائمة / One generic screen, never one per list
  Statement (ar) : يجب على النظام إدارة قيم كل أنواع اللوكب عبر شاشة عامة واحدة من نوع رئيسي-تفصيلي، دون أي شاشة مخصصة لقائمة بعينها.
  Statement (en) : The system shall manage the values of every lookup type through one generic master-detail screen, never a screen dedicated to one specific list.
  Pattern   : ubiquitous
  Trigger   : Any lookup-value management need, for any module
  Rationale : آلية واحدة تخدم كل الوحدات دون تكرار شاشات
  Source    : lookup-module-plan-en.md §3
  Status    : CONFIRMED

POL-MDL-005 — الحقول القياسية لقيمة اللوكب / Standard fields for a lookup value
  Statement (ar) : يجب على النظام تخزين رمز وتسميتين ثنائيتي اللغة وترتيب وحالة نشاط لكل قيمة لوكب.
  Statement (en) : The system shall store a code, bilingual labels, a sort order and an active flag for every lookup value.
  Pattern   : ubiquitous
  Trigger   : Lookup value creation
  Rationale : اتساق البنية عبر كل القوائم
  Source    : lookup-module-plan-en.md §3
  Status    : CONFIRMED

POL-MDL-006 — رفض تكرار الرمز ضمن النوع نفسه / Reject a duplicate code within the same type
  Statement (ar) : إذا شارك رمزا قيمتين نفس نوع اللوكب، فيجب على النظام رفض تسجيل الثانية.
  Statement (en) : If two lookup values under the same lookup type share the same code, then the system shall reject the second.
  Pattern   : unwanted
  Trigger   : Lookup value creation
  Rationale : قيمة موثوقة واحدة لكل مفهوم مُرمَّز — لا تضارب
  Source    : lookup-module-plan-en.md §6 "No contradiction: one authoritative value per coded concept"
  Status    : CONFIRMED

CUSTOM LOOKUP VALUES   (values the user named that the standard lists lack)
| Lookup key | Added values | Source |
None — standard values apply (MDL introduces no domain-specific list of its own; see
module-registry-mdl.md → LOOKUPS OWNED).

SCOPE EXCEPTIONS   (explicit exclusions or non-standard scope)
| Excluded / Deferred | Statement | Activation trigger | Source |
|---|---|---|---|
| Hierarchical / tree-shaped lookup values | Not mentioned by the plan; flat code/label list only | explicit future request | lookup-module-plan-en.md §3 (no hierarchy mentioned) |
| Per-lookup-type fine-grained permission (beyond the shared screen's normal SEC gate) | Not mentioned by the plan — "subject to Security grants" means the normal screen-level gate, not per-type sub-permissions | explicit future request | lookup-module-plan-en.md §4 |

RESOLVED DECISIONS (dialogue, this module)
| # | Question | Recommended answer | Confirmed by user | Sources |
None — no open question was raised for MDL; the plan is fully prescriptive for this stage's scope.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 06, 08, 10, 11
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository at that tag; `mdl/` abbreviates
`erp-core/src/main/java/com/erp/mdl/`. Policy ids are not minted here.

| # | Kind | Policy-level delta | Source |
|---|---|---|---|
| 1 | CHANGED | POL-MDL-001 "sole platform-wide store": MDL is the single store, and each tenant holds its own copy of the catalog. A new tenant receives a copy of PLATFORM's lookup types and values at provisioning (inactive rows included). Consumers read it through the HTTP read or the in-process `MdlLookupApi`; no module keeps a lookup table of its own — FILE's and NOTIF's lists moved into MDL (`V8`). | docs/steps/05-report.md; mdl/tenant/MdlTenantProvisioningContributor.java; V8__mdl_seed.sql |
| 2 | CHANGED | POL-MDL-006 (no duplicate code within a type) is enforced per tenant (`UQ_MDL_LOOKUP_VALUE_TYPE_CODE`, 409 `MDL-409-VALUE-DUP`), on create only; a type key is unique per tenant, not platform-wide (`UQ_MDL_LOOKUP_TYPE_KEY`, 409 `MDL-409-TYPE-DUP`). | V10__tenant_schema.sql:177-180 |
| 3 | CHANGED | POL-MDL-003 (owner module registered in SEC): the module must exist **and be active** in the code-defined, global registry synchronized at startup (`SecModuleRegistryApi.isModuleActive`), so owner codes such as `PLATFORM`, `SEQUENCE`, `AUDIT`, `REPORT` or an application's own module are valid once their contributor declares them; the refusal is 409 `MDL-409-MODULE-NOT-REGISTERED`. A raw seed `INSERT` must satisfy the same policy by ordering (`V7` before `V8`). | mdl/service/LookupTypeService.java:80; docs/steps/06-report.md |
| 4 | CHANGED | POL-MDL-002 (owner namespacing): types are registered by `POST /api/v1/mdl/lookup-types` or by a Flyway `INSERT` — there is no registration SPI; the owner code is stored as data and never re-checked after creation. | V8__mdl_seed.sql:5-9 |
| 5 | CHANGED | POL-MDL-004 (one generic screen): the screen is `MDL_LOOKUPS` «قوائم البيانات المرجعية» / "Master data lookups" with actions VIEW / CREATE / UPDATE — deactivation of a type or a value is an UPDATE action, no DELETE action exists, and no reactivation is offered. | mdl/permission/MdlPermissions.java:19-34 |
| 6 | CHANGED | POL-MDL-005 (standard fields of a value): `sortOrder` is mandatory on create (no request default); reorder assigns 0-based positions. | mdl/dto/LookupValueCreateRequest.java:37-40; mdl/service/LookupValueService.java:140-144 |
| 7 | NEW | An inactive lookup type is **refused** to consumers (404 `MDL-404-TYPE-KEY` on both reads), not silently emptied. | mdl/service/LookupConsumerService.java:53-55 |
| 8 | NEW | Changes to lookup types and values are recorded in the platform audit log (`@Audited`; deactivation as an update — MDL never hard-deletes). | DEVIATIONS [10] |
| 9 | NEW | MDL values feed report parameters of type LOOKUP (only active values of an active type are accepted). | DEVIATIONS [11] |
| 10 | NEW | The seeded NOTIF / FILE types carry no protected or system flag; an administrator can rename, extend or deactivate them through the API (accepted drift risk against the Java code that still switches on their codes). | V8__mdl_seed.sql:16-18 |
| 11 | NEW | A value may be created under an inactive type; the type's state is not checked on value creation. | mdl/service/LookupValueService.java:66-67 |
| 12 | NEW | Access: only `SYS_ADMIN` is seeded with MDL grants; in-process consumers (FILE, NOTIF, REPORT) need no MDL permission — `PERM_MDL_LOOKUPS_VIEW` gates the HTTP read only (ADR-MDL-046). | V7__sec_seed.sql:171; mdl/crossmodule/MdlLookupApiImpl.java:32-46 |

| Kind | Scope exception | Delta | Source |
|---|---|---|---|
| CHANGED | Hierarchical / tree-shaped values | still excluded; the schema is flat (`FK_LOOKUP_VALUE_TYPE` only). | V3__mdl_schema.sql:97 |
| CHANGED | Per-lookup-type fine-grained permission | still excluded; the gate is the screen-level authority, and the in-process API carries none at all. | mdl/permission/MdlPermissions.java |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (CHANGELOG [Unreleased]); no MDL behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| # | Kind | Policy-level delta | Source |
|---|---|---|---|
| 1 | CHANGED | POL-MDL-001: FILE and NOTIF front their MDL-stored lists through the shared `com.erp.common.lookup.LookupOptionResponse` / `OwnedLookups` (one read model for every consumer); no policy effect. | erp-core/src/main/java/com/erp/common/lookup/; CHANGELOG [Unreleased] |
| 2 | CHANGED | POL-MDL-006 and the key uniqueness: refusals raised through `DomainRules.assertUnique`; same codes, no behaviour change. | mdl/domain/ |
