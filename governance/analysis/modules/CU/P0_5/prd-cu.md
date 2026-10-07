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
Steps          : 05, 09, 10
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No US ids are minted here.

NEW product capabilities
| Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|
| Platform default settings | platform operator (PLATFORM tenant, `PLATFORM_SETTINGS_MANAGE`) | Manages defaults that apply to every tenant, through the same configuration API with `?scope=PLATFORM`; reads included (defaults may hold values tenants must not see). | docs/steps/09-report.md; DEVIATIONS [09] |
| Tenant overrides | tenant administrator (`CONFIG_*`) | Overrides a default for the own tenant (`scope=TENANT`, the default); deactivating the override falls back to the default. | DEVIATIONS [09] |
| Typed settings for modules | developer of a module or application | Reads a key as String / Integer / Long / Boolean / BigDecimal / Duration with tenant → platform fallback, cached. | docs/steps/09-report.md |

CHANGED behaviour of existing stories
| Story | Delta | Source |
|---|---|---|
| US-CU-001 configure platform settings at runtime | Every row is now either a platform default or a tenant override; responses carry `scope`; existing rows remained overrides of PLATFORM; each change is recorded in the platform audit log. | DEVIATIONS [09], [10] |
