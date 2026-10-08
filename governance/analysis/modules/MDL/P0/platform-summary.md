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
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

The full description of the implemented platform (erp-core library + consuming apps, the new core
modules tenant / audit / events / sequence / report, conventions, release policy) is recorded once in
[`../../SEC/P0/platform-summary.md`](../../SEC/P0/platform-summary.md) → "Implementation Addendum —
erp-core 1.2.0" (decision recorded there).

MDL's own platform-relevant changes:
| Kind | Change | Source |
|---|---|---|
| REMOVED | The "SEC → MDL → FIN" batch and the `FIN → HARD → MDL` dependency: `fin` was removed from erp-core in step 01; no FIN-owned lookup type and no XM-FIN-001 exist. | docs/steps/01-report.md |
| CHANGED | MODULES row 1.3 MDL: status "built, shipped in erp-core 1.1.0 / 1.2.0"; depends on SEC (SOFT-READ through the in-process `SecModuleRegistryApi.isModuleActive` — the owner module must exist **and** be active), tenant (`CORE_TENANT` FK + provisioning SPI) and audit (`@Audited`). | erp-core/src/main/java/com/erp/mdl/service/LookupTypeService.java:80; V10__tenant_schema.sql |
| CHANGED | DEPENDENCY MAP: the consumers of MDL are FILE and NOTIF (their lookup lists moved into MDL — `V8__mdl_seed.sql` — and are read through `com.erp.mdl.crossmodule.MdlLookupApi`) and REPORT (LOOKUP report parameters); the in-process API carries no permission gate (ADR-MDL-046). | erp-core/src/main/java/com/erp/file/service/FileLookupService.java; erp-core/src/main/java/com/erp/notif/service/NotificationLookupService.java; DEVIATIONS [11] |
| NEW | Tenant-scoped lookup catalog; PLATFORM's catalog (types and values, inactive rows included) is copied to every new tenant by `MdlTenantProvisioningContributor` (order 10). | docs/steps/05-report.md |
| NEW | Permissions declared in code (`MdlPermissions`): `PERM_MDL_LOOKUPS_VIEW` / `_CREATE` / `_UPDATE` (UPDATE also gates deactivation — no DELETE authority), `PERM_MDL_TYPE_REGISTRY_VIEW`; screens «قوائم البيانات المرجعية» / "Master data lookups" and «سجل أنواع البيانات المرجعية» / "Master data type registry"; only `SYS_ADMIN` seeded. | docs/steps/06-report.md; V7__sec_seed.sql:64-65,108-112,171 |
| CHANGED | DEFERRED row "Migrating SEC's USER_STATUS / SIGNUP_STATUS / AUDIT_EVENT_TYPE into MDL": still deferred (SEC keeps its CHECK-constrained columns); what did move into MDL are FILE's and NOTIF's lists (`V8`). | V8__mdl_seed.sql; governance/analysis/decisions/SEC/ADR-SEC-001.md |
| NEW | Seven MDL error codes exist (`MDL-409-MODULE-NOT-REGISTERED`, `MDL-409-TYPE-DUP`, `MDL-409-VALUE-DUP`, `MDL-404-TYPE`, `MDL-404-VALUE`, `MDL-400-REORDER-MISMATCH`, `MDL-404-TYPE-KEY`); an unknown or inactive type is refused with the last one on every consumer read. | erp-core/src/main/java/com/erp/mdl/exception/MdlErrorCodes.java |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (CHANGELOG [Unreleased]); no MDL behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| Kind | Change | Source |
|---|---|---|
| CHANGED | The MDL-backed lookup endpoints of FILE and NOTIF share one read model, `com.erp.common.lookup.LookupOptionResponse`, served through `com.erp.common.lookup.OwnedLookups` (same JSON shape); `MdlLookupApi` is unchanged. | erp-core/src/main/java/com/erp/common/lookup/; CHANGELOG [Unreleased] |
| CHANGED | MDL's Domain objects raise their uniqueness refusals through `com.erp.common.domain.DomainRules.assertUnique`; same codes, no behaviour change. | erp-core/src/main/java/com/erp/mdl/domain/ |
