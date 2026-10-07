# PLATFORM SUMMARY — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile : erp   Domain profile : v1   Registry : v1.1.0
══════════════════════════════════════════════════════════════════

## OVERVIEW
منصة ERP متعددة الوحدات، قائمة على مبدأ "كل ما يمكن أن يتغير = بيانات، لا شيفرة"،
تُبنى بـ Spring (خلفية) وReact (واجهة) على PostgreSQL كهدف بناء وحيد. الوحدة الأولى (SEC)
اكتملت (pass-1 APPROVE)؛ هذه الجلسة تُنشئ **الوحدة الثانية MDL** ثم تتبعها FIN.
[domain-profile §1-§2; project-registry PIPELINE/PROGRESS STATUS]

## MODULES
| #   | Code | Module (ar/en) | Bounded context | Layer | Type | Depends on | Status |
|-----|------|--------|-----------------|-------|------|------------|--------|
| 1.1 | ORG | الهيكل التنظيمي / Organization | organization | L1 | master data | ROOT | NEW (not this batch) |
| 1.2 | SEC | الأمان / Security | organization | L1 | security engine | ROOT | **EXCEPTION — pass-1 COMPLETE, read as-is** |
| 1.3 | MDL | البيانات المرجعية / Master Data Lookup | organization | L1 | reference | SEC (SOFT-READ, module validation) | NEW — this batch, second |
| 2.1 | PRC | المشتريات / Procurement | supply | L3 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 2.2 | FIN | الحسابات العامة / Finance (GL) | finance | L3 | transactional/reporting | SEC (HARD), MDL (HARD) | NEW (next, not this run) |
| 2.3 | INV | المخزون / Inventory | supply | L3 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 3.1 | SLS | المبيعات / Sales | commercial | L4 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 3.2 | CTR | العقود / Contracts | commercial | L4 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 3.3 | HR | الموارد البشرية / Human Resources | people | L4 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
Status: NEW (Phase 2 produces) · EXISTING (Phase 2 extends) · EXCEPTION (read as-is)
Numbering: [tier].[sequence within tier] — the user requests Phase 2 by this number.
This run's Phase 2 request: **1.3 MDL** (module convergence below).

## DEPENDENCY MAP
Build order: Tier 1 [ORG, SEC, MDL] → Tier 2 [PRC, FIN, INV] → Tier 3 [SLS, CTR, HR] → Tier 4 (reporting, none yet)
Key dependencies (one line each):
  MDL → SOFT-READ → SEC : validates a lookup type's owner module code against SEC's ModuleRegistry (new this run — identified during MDL's own convergence, not pre-listed in domain-profile §6, since it only becomes concrete once MDL's integration contract §4 is read against SEC's already-gated ModuleRegistry entity)
  FIN → HARD → SEC : identity + module/screen/action grants + SoD (unchanged, from SEC's own pass)
  FIN → HARD → MDL : payment methods, accounting event types, account types, period states, journal types (unchanged; MDL not yet built when this was first recorded — now concrete)

## DEFERRED (not in scope for this version)
| Item | Reason / activation trigger |
| ORG, PRC, HR, INV, SLS, CTR detailed analysis | user-scoped this batch to SEC → MDL → FIN only (GENERATION-INSTRUCTIONS.md §3) |
| Migrating SEC's USER_STATUS/SIGNUP_STATUS/AUDIT_EVENT_TYPE into MDL's shared lookup table | ADR-SEC-001 defers this to a SEC v2 delta version; out of scope for this batch's v1 passes |
| Workflow engine | profile: `forbidden` |
| Notifications / File Service redesign | ready external modules; consumed only on real need [lookup-module-plan-en.md §5] |

## RESOLVED DECISIONS (this phase)
| # | Point | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | MDL → SEC dependency kind | SOFT-READ (validate module code only; no FK, no shared data ownership) | Yes — consistent with security-module-plan-en.md §7's "registers itself as data" pattern applied in reverse (MDL reads, doesn't own, SEC's registry) | [KB:erp-domain-standards §5 HARD-FK vs SOFT-READ] |
| 2 | Phase 2 request for this P0 run | Module 1.3 MDL (second of the batch order SEC→MDL→FIN) | Yes — GENERATION-INSTRUCTIONS.md §3 | GENERATION-INSTRUCTIONS.md §3 |

## OPEN ITEMS
None.

## NEXT STEP
Module 1.3 MDL converges below (module-registry-mdl.md, business-policies-mdl.md).
Reply with a plain instruction to adjust, or request module 2.2 (FIN) next per the
mandated order (FIN must wait until MDL's pass-1 gate as well, per GENERATION-
INSTRUCTIONS.md's dependency rule).

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 01–12 (plan), 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

The full description of the implemented platform (erp-core library + consuming apps, the new core
modules tenant / audit / events / sequence / report, conventions, release policy) is recorded once in
[`../../SEC/P0/platform-summary.md`](../../SEC/P0/platform-summary.md) → "Implementation Addendum —
erp-core 1.2.0" (decision recorded there). The "SEC → MDL → FIN" batch above no longer applies: `fin`
was removed from erp-core (step 01).

MDL's own platform-relevant changes:
| Change | Source |
|---|---|
| Tenant-scoped lookup catalog; PLATFORM's catalog copied to every new tenant | docs/steps/05-report.md |
| Permissions declared in code (`MdlPermissions`) | docs/steps/06-report.md |
| Lookup values used as report LOOKUP parameters | DEVIATIONS [11] |
