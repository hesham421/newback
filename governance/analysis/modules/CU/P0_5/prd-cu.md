# PRD — Common Utils (CU)
══════════════════════════════════════════════════════════════════
Module          : Common Utils (CU prefix)
Source artifacts: platform-summary.md, module-registry-CU.md,
                  business-policies-CU.md
Status          : DRAFT — awaiting Reconciliation Gate (Project 2.5)
Open Questions  : None — see OQ Log
══════════════════════════════════════════════════════════════════

## USER STORIES

US-CU-001
  Story    : كمشغّل للمنصة، أحتاج ضبط إعدادات المنصة وقت التشغيل
             كمدخلات key/value دون إعادة نشر، ليبقى النظام قابلاً للتهيئة.
  Priority : —
  Success metric : —
  Source   : module-registry-CU.md §ENTITIES OWNED (AppConfiguration)
             + §RESPONSIBILITIES (Configuration);
             business-policies-CU.md §POLICY-CLI-02 (Configurable)
  Status   : DRAFT

## STORIES EXCLUDED (لا NEED نهائي متتبَّع — بنية تحتية code)

  — Specification / Filtering — آلية استعلام/predicate مشتركة؛ pure
    code، لا كيان، لا حاجة مستخدم مستقلة.
  — Global Exceptions — بنية معالجة أخطاء؛ الـ Error Catalog / ERR-IDs
    مُلك P3، ليست حاجة مستخدم في CU.
  — Events (in-process bus) — آلية نشر/استماع داخلية؛ لا سطح مستخدم.
  — Bundle (i18n AR/EN) — آلية حلّ رسائل (resource bundles ملفّية)؛
    بنية تحتية، متّسقة مع الثلاثة أعلاه. الرسائل المترجمة تظهر عبر
    الموديول المستهلِك، لا كميزة منتج في CU.

## DESIGN CONSTRAINTS (سياق فقط — ليست قصصاً)

  سياسات تصميم شاملة من P0، تُطبَّق على كل موديولات Foundation؛
  P1 يحوّلها لـ RULE-IDs حيث تصبح قابلة للإنفاذ.
  — POLICY-CLI-01 : تعقيد متوسط — أبسط حل يفي بالمتطلب؛ لا إفراط هيكلة.
  — POLICY-CLI-02 : Reusable + Configurable + Integrable + Composable.
  — POLICY-CLI-03 : بناء كامل مستقل — لا أجزاء مؤجّلة داخل Foundation.
  Source: business-policies-CU.md §CLIENT-SPECIFIC POLICIES.

## OPEN ITEMS (ambiguous, not yet a story)

  None — الغموضان السابقان حُسِما (2026-09-01): i18n صُنّف بنية تحتية
  (مستبعَد)، وسياسات التصميم حُمِلت كقيود لا كقصص.

══════════════════════════════════════════════════════════════════
*End of prd-CU.md*
*Next stage: Project 2.5 (UI/UX Design Engine) — requires this file
 AND srs.md together (CONTRACT-11). Does not gate Project 1.*
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 05, 06, 09, 10 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository at that tag. No US ids are minted here; rule ids are
the ones minted in `../P1/srs-cu.md`.

Product capabilities
| Kind | Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|---|
| NEW | Platform default settings | platform operator (PLATFORM tenant, `PLATFORM_SETTINGS_MANAGE` — held implicitly by its super roles) | Manages defaults that apply to every tenant through the same configuration API with `?scope=PLATFORM`; reads included (defaults may hold values tenants must not see). An administrator of any other tenant is refused with 403 `ACCESS_DENIED` | docs/steps/09-report.md; docs/DEVIATIONS.md [09]; srs-cu.md RULE-CU-005 |
| NEW | Tenant overrides | tenant administrator (`CONFIG_*`, e.g. role `CU_ADMIN`) | Overrides a default for the own tenant (`scope=TENANT`, the default); deactivating the override falls back to the default; a new tenant starts with no rows and inherits every default | docs/DEVIATIONS.md [09]; srs-cu.md RULE-CU-004, 014 |
| NEW | Typed settings for modules | developer of a module or application | Reads a key as String / Integer / Long / Boolean / BigDecimal / Duration with tenant → platform fallback, cached (no TTL; evicted by every write through the API; a change made directly in the database is not seen until the next API write); keys are case-insensitive | docs/steps/09-report.md; srs-cu.md RULE-CU-006 … 009 |

Existing story
| Kind | Story | Delta | Source |
|---|---|---|---|
| CHANGED | US-CU-001 configure platform settings at runtime | Every row is either a platform default or a tenant override; responses carry `scope`; existing rows remained overrides of PLATFORM; each change is recorded in the platform audit log (values included — secrets must not be stored as settings) | docs/DEVIATIONS.md [09], [10] |
| CHANGED | US-CU-001 — operations | the list is a `POST …/search` with filters on key / active flag / dates; a deactivated setting is reactivated by updating it with `isActive: true` (no separate activate action); deactivate answers no content and may be repeated | srs-cu.md §1; RULE-CU-010 |
| CHANGED | STORIES EXCLUDED — Events | the in-process bus is now the `com.erp.events` core module (asynchronous, after commit); CU raises no event when a setting changes | docs/steps/08-report.md; RULE-CU-013 |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (commit 6b01816; CHANGELOG [Unreleased]); no CU behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | `AppConfigurationDomain` | uses the shared `com.erp.common.domain.DomainRules`; no product-visible change | erp-core/src/main/java/com/erp/cu/domain/AppConfigurationDomain.java:39-40,51 |
| CHANGED | `ConfigurationService.owner` | uses `TenantContext.isPlatform()`; no product-visible change | erp-core/src/main/java/com/erp/cu/service/ConfigurationService.java:214 |

No story or capability delta.
