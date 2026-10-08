## REGISTRY — P1 — MDL v1
══════════════════════════════════════════════════════════════════
Module : MDL (البيانات المرجعية / Master Data Lookup)   Version : v1   Profile : erp
Source : analysis/modules/MDL/P1/srs-mdl.md
══════════════════════════════════════════════════════════════════

### Entities
| ENT id | Name (ar / en) | Kind | PRIVATE / SHARED | Status |
|---|---|---|---|---|
| ENT-MDL-001 | نوع اللوكب / LookupType | master | SHARED (owner) | REGISTERED |
| ENT-MDL-002 | قيمة اللوكب / LookupValue | lookup | SHARED (owner) | REGISTERED |

المعرّفان مُعاد استعمالهما كما هما من project-registry ENTITY OWNERSHIP — لا كيان جديد
ولا إعادة ترقيم (engine §1.1).

### Consumed (shared entities of other modules)
| Owner ENT id | Owner module | HARD-FK / SOFT-READ | Consumes |
|---|---|---|---|
| ENT-SEC-004 | SEC | SOFT-READ (no FK) | وجود رمز الوحدة المالكة عند إنشاء نوع لوكب — RULE-MDL-001 / سجل الوحدات / ModuleRegistry existence check |
مرشَّح XM واحد إلى SEC؛ المعرّف من إسناد P2. لا مفتاح أجنبي عابرًا للوحدات
(سابقة ADR-FIN-001 — project-registry DECISION INDEX #9).

### Lookups owned
| Key | ENT | Values count |
|---|---|---|
| — | — | 0 — الوحدة هي آلية القوائم ولا تملك قائمة قيم خاصة بها / the module owns the mechanism, not a list |

### Lookups consumed
| Key | Owner |
|---|---|
| — | — لا قيمة مُرمَّزة تُقرأ من أي وحدة / none consumed |

### Screens
| SCR-REQ id | Name (ar / en) | Page code |
|---|---|---|
| SCR-REQ-MDL-001 | اللوكبات العامة / Generic Lookups | MDL_LOOKUPS |
| SCR-REQ-MDL-002 | سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner | MDL_TYPE_REGISTRY |

### Requirements
| Atom | Count | Last sequence |
|---|---|---|
| REQ | 13 | REQ: 13 |
| AC | 13 | AC: 13 |
| ENT | 2 | ENT: 2 |
| RULE | 4 | RULE: 4 |
| SCR-REQ | 2 | SCR-REQ: 2 |

Requirements registered (REQ-MDL-001 … REQ-MDL-013):
REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-005, REQ-MDL-006,
REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010, REQ-MDL-011, REQ-MDL-012,
REQ-MDL-013

Acceptance criteria registered (AC-MDL-001 … AC-MDL-013):
AC-MDL-001, AC-MDL-002, AC-MDL-003, AC-MDL-004, AC-MDL-005, AC-MDL-006,
AC-MDL-007, AC-MDL-008, AC-MDL-009, AC-MDL-010, AC-MDL-011, AC-MDL-012,
AC-MDL-013

Business rules registered (RULE-MDL-001 … RULE-MDL-004):
RULE-MDL-001, RULE-MDL-002, RULE-MDL-003, RULE-MDL-004

### Decisions
| ADR id | Subject | Status |
|---|---|---|
| — | لا قرار جديد في P1 / no ADR raised at P1 | — |
سابقتان مطبَّقتان لا مُنشأتان: ADR-SEC-002 (كتالوج الأخطاء البنيوية تحت مظلة PLATFORM-STD)
وADR-FIN-001 (تبعية الأمان قراءة تطبيقية لا مفتاحًا أجنبيًا) — project-registry
DECISION INDEX #8, #9. لا قرار بحالة BLOCKED / no BLOCKED ADR — the pass completed.

### Event
"P1 completed: MDL v1 — 2 entities, 13 requirements, 13 AC, 4 rules, 2 screen requirements, 0 ADR"
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 05, 06, 08, 10, 11
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Registry deltas only; full text in `srs-mdl.md` → "Implementation Addendum — erp-core 1.2.0". The RULE ids
RULE-MDL-005 … RULE-MDL-013 are minted there (continuing from RULE-MDL-004); no REQ / AC / ENT / SCR-REQ id is.

### Entities — delta
| Kind | ENT id | Delta |
|---|---|---|
| CHANGED | ENT-MDL-001 LookupType | tenant-scoped + version (neither on the wire); key unique per tenant; `key` ≤ 80, names ≤ 150 |
| CHANGED | ENT-MDL-002 LookupValue | tenant-scoped + version; code unique per (tenant, type); `sortOrder` mandatory on create, `NUMERIC` column mapped to `Integer`; names ≤ 150 |

### Consumed — delta
| Kind | Owner | Kind of link | Consumes |
|---|---|---|---|
| CHANGED | SEC (ENT-SEC-004) | SOFT-READ, in-process `SecModuleRegistryApi.isModuleActive` | existence **and** active flag of the owner module code (RULE-MDL-001) |
| NEW | tenant (`CORE_TENANT`) | HARD-FK + provisioning SPI (`MdlTenantProvisioningContributor`, order 10) | owning tenant of every row; full catalog copy (inactive rows included) for new tenants |
| NEW | SEC permission SPI | `MdlPermissions implements PermissionContributor` | module, 2 screens, 4 actions |
| NEW | audit | `@Audited` on both entities | CREATE / UPDATE events (deactivation = UPDATE) |

### Exposed — delta
| Kind | Surface | Consumers |
|---|---|---|
| NEW | `com.erp.mdl.crossmodule.MdlLookupApi.readActiveValuesByKey(String)` → `List<LookupOptionView(code, labelAr, labelEn, sortOrder)>`; no `@PreAuthorize`; 404 `MDL-404-TYPE-KEY` for an unknown or inactive type | FILE (`FileLookupService`), NOTIF (`NotificationLookupService`), REPORT (`ReportService`) |

### Lookups hosted — delta
| Kind | Key | Owner | Values |
|---|---|---|---|
| NEW (seed) | NOTIF_CHANNEL | NOTIF | 5 (V8) + IN_APP (V13) |
| NEW (seed) | NOTIF_STATUS | NOTIF | 4 (V8) + QUEUED, SKIPPED_NO_PROVIDER (V13) |
| NEW (seed) | FILE_FILE_STATUS | FILE | 3 (V8) |
| NEW (seed) | FILE_FILE_TYPE | FILE | 5 (V8) |
"Lookups owned — 0" still holds; these rows are hosted for their owners and seeded by raw `INSERT`.

### Screens — delta
| Kind | SCR-REQ id | Page code | Delta |
|---|---|---|---|
| CHANGED | SCR-REQ-MDL-001 | MDL_LOOKUPS | label «قوائم البيانات المرجعية» / "Master data lookups"; actions VIEW, CREATE, UPDATE only (no DELETE — deactivation is UPDATE) |
| CHANGED | SCR-REQ-MDL-002 | MDL_TYPE_REGISTRY | label «سجل أنواع البيانات المرجعية» / "Master data type registry"; VIEW only, as analysed |

### Requirements — delta
| Group | NEW | CHANGED | REMOVED |
|---|---|---|---|
| Endpoints | — | all 11: `POST /lookup-types/search` (generic `filters[]`, no combined name filter) · `POST /lookup-types` (codes) · `PUT /lookup-types/{id}` (code) · `DELETE /lookup-types/{id}` (UPDATE authority, 200 + body) · `POST /lookup-types/values/search` (paged, `lookupTypeId` in `filters[]`) · `POST /lookup-types/{id}/values` (`sortOrder` mandatory) · `PUT /lookup-values/{id}` (RULE-MDL-002 cannot fire) · `DELETE /lookup-values/{id}` (UPDATE authority, 200 + body) · `PATCH …/reorder` (0-based, partial list, 400) · `POST /lookup-types/by-owner/search` (active only, paging ignored) · `GET /lookups?type=` (404 for an inactive type) | — |
| Rules | RULE-MDL-005 tenant confinement · RULE-MDL-006 provisioning copy · RULE-MDL-007 optimistic lock (VERSION not on the wire) · RULE-MDL-008 audit (deactivation = UPDATE, no hard delete) · RULE-MDL-009 report LOOKUP validation · RULE-MDL-010 value under an inactive type allowed · RULE-MDL-011 no protected flag on seeded types · RULE-MDL-012 no caching · RULE-MDL-013 no registration SPI | RULE-MDL-001 (owner exists **and** active, in-process) · REQ-MDL-001 (per tenant) · RULE-MDL-002 (per tenant, create only) · RULE-MDL-004 (404 instead of exclusion) · REQ-MDL-005 (paged, inactive included) · REQ-MDL-006 (`sortOrder` mandatory) · REQ-MDL-010 (0-based, partial) | — |
| Error codes | `MDL-409-MODULE-NOT-REGISTERED`, `MDL-409-TYPE-DUP`, `MDL-409-VALUE-DUP`, `MDL-404-TYPE`, `MDL-404-VALUE`, `MDL-400-REORDER-MISMATCH`; common `CONCURRENT_MODIFICATION`, `DATA_INTEGRITY_VIOLATION`, `NOT_FOUND`, `VALIDATION_ERROR`, `METHOD_NOT_ALLOWED` | `MDL-404-TYPE-KEY` (unknown **or inactive** type; text differs from AC-MDL-012) | RULE-MDL-004 message "currently inactive"; AC-MDL-001 / AC-MDL-006 success messages |
| Permissions | — | `PERM_MDL_LOOKUPS_VIEW`, `_CREATE`, `_UPDATE` (also deactivation + reorder), `PERM_MDL_TYPE_REGISTRY_VIEW` code-defined (`MdlPermissions`); screen labels; only `SYS_ADMIN` seeded; in-process `MdlLookupApi` ungated | `PERM_MDL_LOOKUPS_DELETE` (never existed) |

| Kind | Atom | Count | Last sequence |
|---|---|---|---|
| CHANGED | RULE | 13 | RULE: 13 (RULE-MDL-005 … 013 minted in the 1.2.0 addendum of srs-mdl.md) |

### Decisions — delta
| Kind | ADR id | Subject | Status |
|---|---|---|---|
| NEW | ADR-MDL-045 | index strategy as built (V3): active-flag and FK indexes, no name indexes — replaces the dropped ADR-MDL-009 / 010 | ACCEPTED |
| NEW | ADR-MDL-046 | in-process `MdlLookupApi` is ungated; VIEW gates the HTTP read only — supersedes ADR-MDL-007 in part | ACCEPTED |


## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C5 — tenant data export contributor
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-mdl.md` → "Implementation Addendum — erp-core 1.3.0".

### Consumed — delta
| Owner | Kind | Consumes |
|---|---|---|
| tenant | SPI (implemented) | `TenantExportContributor` (XM-TENANT-004) — `MdlTenantExportContributor`: `MDL_LOOKUP_TYPE`, `MDL_LOOKUP_VALUE` of the exported tenant |
No id minted; endpoints, rules, error codes, permissions, schema unchanged.

### Refactors without behaviour change — shared helpers moved to `com.erp.common`
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no MDL behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

| Kind | Item | Delta |
|---|---|---|
| CHANGED | Exposed — consumers' read model | FILE / NOTIF answer `com.erp.common.lookup.LookupOptionResponse` through `OwnedLookups.read`; `MdlLookupApi` / `LookupOptionView` unchanged |
| CHANGED | Rules — RULE-MDL-002, REQ-MDL-001 | `LookupTypeDomain` / `LookupValueDomain` raise the duplicate refusals through `DomainRules.assertUnique`; same codes, no behaviour change |
