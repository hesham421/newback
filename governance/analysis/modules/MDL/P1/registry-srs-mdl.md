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

Registry deltas only; full text in `srs-mdl.md` → "Implementation Addendum — erp-core 1.2.0". No new ids
are assigned here.

### Entities — delta
| ENT id | Delta |
|---|---|
| ENT-MDL-001 LookupType | tenant-scoped + version; key unique per tenant |
| ENT-MDL-002 LookupValue | tenant-scoped + version; code unique per (tenant, type) |

### Consumed — delta
| Owner | Kind | Consumes |
|---|---|---|
| tenant (`CORE_TENANT`) | HARD-FK + provisioning SPI | owning tenant of every row; catalog copy for new tenants |

### Requirements — delta
| Group | NEW | CHANGED | REMOVED |
|---|---|---|---|
| Endpoints | — | — (tenant scope only) | — |
| Rules | tenant confinement, provisioning copy, optimistic lock, audit, report LOOKUP validation | REQ-MDL-001 key uniqueness, RULE-MDL-002, RULE-MDL-001 (code-defined registry) | — |
| Error codes | — (common `CONCURRENT_MODIFICATION`, `NOT_FOUND`) | — | — |
| Permissions | — | catalog code-defined (`MdlPermissions`) | — |


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
