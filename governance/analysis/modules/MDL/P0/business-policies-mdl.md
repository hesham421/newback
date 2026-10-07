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

Paths cited below are relative to the erp-core repository at that tag. Policy ids are not minted here.

| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 1 | Lookup types and values belong to a tenant. POL-MDL-001 "sole platform-wide store" now reads: MDL is the single store, and each tenant holds its own copy of the catalog. A new tenant receives a copy of PLATFORM's lookup types and values at provisioning. | CHANGED | 05-STEP; docs/steps/05-report.md; DEVIATIONS [05] (provisioning entry) |
| 2 | POL-MDL-006 (no duplicate code within a type) is enforced per tenant; a type key is unique per tenant, not platform-wide. | CHANGED | V10__tenant_schema.sql |
| 3 | POL-MDL-003 (owner module must be registered in SEC): the module registry is now the code-defined, global catalog synchronized at startup, so owner codes such as `PLATFORM`, `SEQUENCE`, `AUDIT` or an application's own module are valid once their contributor declares them. | CHANGED (context) | docs/steps/06-report.md |
| 4 | Changes to lookup types and values are recorded in the platform audit log. | NEW | DEVIATIONS [10] |
| 5 | MDL values feed report parameters of type LOOKUP (only active values are accepted). | NEW | DEVIATIONS [11] (run request entry) |

Scope exceptions: unchanged (no hierarchy, no per-type permission).
