I'll apply G1 (with a new ADR, since the finding requires a choice) and G2 across the writable files. `flow-diagram-mdl.md` is unaffected by either finding, so I'm not rewriting it.

<<<FILE: governance-shared/analysis/decisions/MDL/ADR-MDL-016.md>>>
# ADR-MDL-016 — SCR-MDL-001's master-list owner-module filter degrades like a filter, not like the create-form select
Status: ACCEPTED (non-breaking)
Module: MDL   Version: v1   Raised at: P3.2, gate `pass-2` revision (round 3, G1)

## Context
`UXD-MDL-001` (the owner-module data read from SEC's `ModuleRegistry`) backs two controls on
SCR-MDL-001: the create-form's owner-module select, and the master list's read-only
`ownerModuleCode` search filter. The frontend-execution-plan and ui-ux-spec previously stated one
degraded behaviour for both — "a refused read leaves the select empty and disabled, and the
create action disabled behind it" (ADR-MDL-013) — leaving the filter's own behaviour on a refused
read unspecified, with the nearest stated text implying the same empty-and-disabled treatment.

`PERM_SEC_MODULE_REGISTRY_VIEW`, the grant behind a refused read, is tied by ADR-MDL-013 and
SEC-FE to `PERM_MDL_LOOKUPS_CREATE` alone. A caller who holds `PERM_MDL_LOOKUPS_VIEW` without
`PERM_MDL_LOOKUPS_CREATE` — SRS §B4's consuming-module service account, or a reviewer holding the
screen without CREATE — is expected not to hold the SEC grant either: the degraded path is that
caller's *normal* path, not an edge case. As written, it cost that caller a filter their own
permissions never gated in the first place.

The sibling screen, SCR-MDL-002, already answers the identical failure differently: ADR-MDL-015
falls back its owner-module filter to the distinct `ownerModuleCode` values present in the
current `API-MDL-010` response, specifically so that "browsing is never blocked by the degraded
source." `API-MDL-001` (the master-list search) returns `ownerModuleCode` on every row already
(F1-MODEL, `LookupTypeResponse`), so the same fallback data SCR-MDL-002 uses is already in hand
for SCR-MDL-001's own filter. ADR-MDL-013's own reasoning — "a code typed blind is a code the
server is certain to refuse" (RULE-MDL-001) — is an argument about the *write* path (the create
form must not offer a value the server cannot accept) and does not reach a *read* filter over
rows the server has already returned.

## Decision
`UXD-MDL-001` now has two degraded behaviours on SCR-MDL-001, one per control, not one shared
between them:
- **create-form select** — unchanged (ADR-MDL-013): a refused or failed read leaves the select
  empty and disabled, and the create action disabled behind it. This is the only place the field
  is ever a value the server could refuse.
- **master-list search filter** — a refused or failed read leaves the filter falling back to the
  distinct `ownerModuleCode` values already present in the current `API-MDL-001` response,
  exactly as ADR-MDL-015 already does for SCR-MDL-002's filter. Never free text, never disabled.
  Browsing the type list is never blocked by the degraded source.

## Rationale
The failure mode a caller meets on the filter is not the failure mode ADR-MDL-013 was written
for: nothing is being submitted, nothing can be refused, and the data the fallback needs is
already sitting in the very rows the filter is scoped over. Disabling a read-only filter because
a foreign write-time safeguard fired denies a capability to exactly the callers — VIEW-only,
no CREATE — who were never going to hit that safeguard. Matching SCR-MDL-002's already-accepted
pattern is the non-arbitrary choice: the same data source, the same shape of control (a filter
over rows already on screen), the same failure, the same fallback.

## Consequences
- `frontend-execution-plan-mdl.md` F2-SCREEN-INIT (SCR-MDL-001) and F4-SCREEN Cold-load section
  are unaffected by this ADR directly, but the "Foreign data" line is split into the two
  controls' separate behaviours.
- `ui-ux-spec-mdl.md` UXD-MDL-001's Degraded (SCR-MDL-001) line is split the same way.
- No backend change, no new endpoint, no new permission: the fallback uses data `API-MDL-001`
  already returns on every row.
<<<END FILE>>>

<<<FILE: governance-shared/analysis/modules/MDL/P3_2/ui-ux-spec-mdl.md>>>
# UI/UX SPEC — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp   Stage : P3.2 (Part A — UX design)
Sources: srs-mdl.md (fields, rules, permissions — the functional ceiling) · prd-mdl.md (intent)
Mints  : SCR-MDL-001, SCR-MDL-002 · UXD-MDL-001
Scope  : design intent — final component names, routes and code are Part B's
══════════════════════════════════════════════════════════════════

هذه المواصفة لا تضيف حقلًا ولا قاعدة ولا صلاحية ليست في SRS، ولا تُسقط شيئًا منها؛ كل سطر أدناه
منقول عن مصدره ومنسوب إليه / This spec adds no field, rule or permission the SRS does not have and
omits none of them: every line below is copied from its source and attributed to it.

## SCR-MDL-001 — اللوكبات العامة / Generic Lookups
Traces            : REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010, AC-MDL-001, AC-MDL-002, AC-MDL-003, AC-MDL-004, AC-MDL-005, AC-MDL-006, AC-MDL-007, AC-MDL-008, AC-MDL-009, AC-MDL-010, UXD-MDL-001
Screen requirement: SCR-REQ-MDL-001 · page code MDL_LOOKUPS
UI pattern        : header + repeating lines — رئيسي (أنواع) + تفصيلي (قيم) قابل لإعادة الترتيب / master (types) + detail (values), reorderable — من SRS §B1 حرفيًا، غير مُبدَّل / verbatim from the SRS screen entry, not changed here
Sub-views         : Search (الأنواع / the types, مُرقَّم الصفحات على الخادم / server-paged) · Entry (نوع / a type) · Detail (قيم النوع المختار / the selected type's values, غير مُرقَّمة الصفحات — محصورة بنوع واحد / not paged — bounded by one type) · Entry (قيمة / a value) — أربعة سطوح تحت SCR واحد / four surfaces under ONE SCR
Fields shown      : كل حقول ENT-MDL-001 وENT-MDL-002 كما في SRS §A3 — الجدولان أدناه، بتسمية لكل لغة وبعلم القراءة فقط / every field of ENT-MDL-001 and ENT-MDL-002 as SRS §A3 states them — the two tables below, label per language, read-only flags marked
Composition       : جزء القيم ليس تفصيلًا ثانويًا داخل نموذج النوع / the values are not secondary detail inside the type's form — inline: none · summary row + second level: صفّ لكل قيمة في جزء القيم، ومحرّرها يُفتح من ذلك الصفّ / a row per value in the values pane, its editor opened from that row · submits: one per open form, never two open at once
Permissions       : SRS §B4 — MDL_LOOKUPS: VIEW (بوابة / gateway) · CREATE · UPDATE · DELETE (تعطيل ناعم / soft deactivate) — reference only; names follow `PERM_<PAGE_CODE>_<ACTION>`. The api-docs' own endpoint requirements versus this matrix are filed as PF-MDL-001 in frontend-execution-plan-mdl.md, not resolved here.
Cross-module data : `ownerModuleCode` → UXD-MDL-001 (owner module SEC)
States            : empty — لا نوع يطابق المرشِّحات / no type matches the filters · النوع المختار بلا قيم / the selected type has no values (رسالتان مختلفتان / two distinct messages) · loading — هيكل عظمي للقائمة، ومؤشّر موضعي لجزء القيم / a list skeleton and a local indicator on the values pane · error — لافتة برسالة الكتالوج المحلية / a banner carrying the localized catalog message (الرموز نفسها من نصيب الجزء ب / the codes themselves are Part B's) · offline — لا شيء: SRS لا تنصّ على عمل بلا اتصال / nothing: the SRS states no offline behaviour

**حقول ENT-MDL-001 — نوع اللوكب / LookupType**

| Field | التسمية (ar) | Label (en) | Read-only | من / from |
|---|---|---|---|---|
| lookupTypePk | معرّف نوع اللوكب | LookupType id | نعم / yes — لا يُعرض كمرجع عمل / never shown as a business reference | SRS §A3 |
| key | المفتاح | Key | عند التعديل / on edit — RULE-MDL-003 | SRS §A3, §B3 |
| ownerModuleCode | رمز الوحدة المالكة | Owner module code | عند التعديل / on edit | SRS §A3, §B3 |
| nameAr | الاسم (عربي) | Name (Arabic) | لا / no | SRS §A3 |
| nameEn | الاسم (إنجليزي) | Name (English) | لا / no | SRS §A3 |
| isActiveFl | نشط | Active | نعم / yes — يتغيّر بإجراء التعطيل وحده / changed by the deactivate action alone | SRS §B3 |
| createdBy · createdAt · updatedBy · updatedAt | أنشأه · تاريخ الإنشاء · عدّله · تاريخ التعديل | Created by · Created at · Updated by · Updated at | نعم / yes — يملؤها النظام / system-filled | SRS §A3 |

**حقول ENT-MDL-002 — قيمة اللوكب / LookupValue**

| Field | التسمية (ar) | Label (en) | Read-only | من / from |
|---|---|---|---|---|
| lookupValuePk | معرّف القيمة | LookupValue id | نعم / yes | SRS §A3 |
| lookupTypeId | نوع اللوكب | Lookup type | نعم / yes — يُحدَّد باختيار النوع لا بكتابته / set by selecting the type, never typed | SRS §A3 |
| code | الرمز | Code | عند التعديل / on edit — RULE-MDL-002 يحرس تفرّده ضمن النوع / its uniqueness within the type | SRS §A3, §B3 |
| nameAr | الاسم (عربي) | Name (Arabic) | لا / no | SRS §A3 |
| nameEn | الاسم (إنجليزي) | Name (English) | لا / no | SRS §A3 |
| sortOrder | الترتيب | Sort order | لا / no — يُضبط بالتحرير أو بإعادة الترتيب / set by editing or by reordering | SRS §A3, §B3 |
| isActiveFl | نشط | Active | نعم / yes — يتغيّر بإجراء التعطيل وحده / changed by the deactivate action alone | SRS §B3 |
| createdBy · createdAt · updatedBy · updatedAt | أنشأها · تاريخ الإنشاء · عدّلها · تاريخ التعديل | Created by · Created at · Updated by · Updated at | نعم / yes — يملؤها النظام / system-filled | SRS §A3 |

**المرشِّحات وأعمدة النتيجة / filters and result columns** — SRS §B2, one column per filter:
المفتاح (LIKE) · الوحدة المالكة (EXACT) · الاسم في اللغتين معًا (LIKE) · الحالة (EXACT) على القائمة
الرئيسية؛ ورمز القيمة (LIKE) على الجزء التفصيلي، محصورًا بالنوع المختار (REQ-MDL-005). القائمة
الرئيسية مُرقَّمة الصفحات على الخادم، والجزء التفصيلي يُعرض كاملًا مرتَّبًا بـ`sortOrder` لأنه محصور
بنوع واحد ولا ترقيم صفحات له أصلًا (backend-execution-plan API-MDL-005 · QR-MDL-005 · SRS §B2) /
the master list is server-paged; the detail is shown whole, unpaged, ordered by `sortOrder`,
because it is confined to one type.

**الإجراءات / actions** — SRS §B3: حفظ نوع · حفظ تعديل النوع · تعطيل النوع · حفظ قيمة · حفظ تعديل
القيمة · تعطيل القيمة · إعادة الترتيب / save type · save type changes · deactivate type · save
value · save value changes · deactivate value · reorder. لا إجراء تفعيل ولا محو نهائي على أي من
المستويين / no activate and no hard delete at either level — SRS §B3 and §A2 (re-activation is out
of scope), which is why deactivation is stated as one-way where it is offered.

**أثر التعطيل على الرمز/المفتاح — لا استرجاع له / the code/key consequence of deactivation, and
that it cannot be undone through any surface here.** `UQ_MDL_LOOKUP_TYPE_KEY` and
`UQ_MDL_LOOKUP_VALUE_TYPE_CODE` are unique over every row, active or not (db-script BLOCK 5b),
and no activate endpoint exists at either level (ADR-MDL-005). Deactivating a value or a type
therefore does not free its `code` or `key` for reuse — it reserves it, permanently, under that
type or under the platform. Both deactivate confirmations state this explicitly, in the wording
the act itself deserves, not deferred to a later error message:

- **Type deactivate confirmation** — ar: «سيصبح النوع غير فعّال، ولن يُعاد إليه المفتاح
  `{key}` بعد ذلك — لا يوجد إجراء لإعادة التفعيل من هذه الشاشة.» ·
  en: "The type will become inactive, and its key `{key}` cannot be reused afterwards — there is
  no activate action on this screen."
- **Value deactivate confirmation** — ar: «ستصبح القيمة غير فعّالة، ولن يُعاد إليها الرمز
  `{code}` ضمن هذا النوع بعد ذلك — لا يوجد إجراء لإعادة التفعيل من هذه الشاشة.» ·
  en: "The value will become inactive, and its code `{code}` cannot be reused under this type
  afterwards — there is no activate action on this screen."

A user who then tries to register the same key or code again meets `MDL-409-TYPE-DUP` or
`MDL-409-VALUE-DUP`, which by default reads as a collision with a live record. The value create
form's inline routing of `MDL-409-VALUE-DUP` (frontend-execution-plan-mdl.md, F2-QUERY VALUE
CREATE) therefore adds that a deactivated value of the same code under this type may be the one
holding it, and points at the list's own inactive row — still shown in this manager's pane
(AC-MDL-005, AC-MDL-009) — as the evidence a user can check without leaving the screen. The same
applies at the type level: a rejected registration whose key is provably a prior, now-inactive
type is not a code the platform will ever hand back.

**لماذا هذا التوزيع / why this composition.** الشاشة تحرّر سجلّين من كيانين مختلفين، لا سجلًّا واحدًا
ذا مجموعة أبناء / this screen edits records of two different entities, not one record with a child
set: `LookupTypeCreateRequest` لا يحمل قيمًا أصلًا، وكل قيمة سجلّ قائم بذاته له عملياته / a type's
write carries no values at all, and each value is a record of its own. فالنوع لا "يحفظ قيمه" ولا
يمكن أن يحفظها / a type therefore does not — and cannot — save its values. جزء القيم صفّ ملخَّص لكل
قيمة (الرمز، التسميتان، الترتيب، الحالة) ومحرّر يُفتح منه كمستوى ثانٍ فوقه، وهو:
1. **يحفظ سجلّه هو بحفظ واحد** — القيمة كيان مستقل، لا حقل في نموذج النوع؛ ولا يُفتح محرّر قيمة ونموذج
   نوع معًا أبدًا، فلا يرى المستخدم زرَّي حفظ في وقت واحد / it saves its OWN record with ONE submit,
   and a value editor and a type form are never open at once, so two Save buttons never face the
   user together;
2. **حالة فتحه في حالة التنقّل** حيث تعيش بقية حالة التنقّل، فالرجوع يغلقه، والصرف يغلقه وحده،
   والرابط العميق يفتحه إن وُجد سجلّه في الذاكرة المؤقتة، وإلا يُعاد التوجيه إلى مستوى النوع
   (frontend-execution-plan-mdl.md ADR-MDL-014) / it lives in navigation state: back closes it,
   dismissing closes only it, and a deep link opens it when its record is in cache, else redirects
   to the type level (ADR-MDL-014);
3. **لا منطقة تمرير خاصة به** — القائمة تتمرّر مع الجسم الذي تسكنه / it carries no scroll region of
   its own; the list scrolls with the body it sits in;
4. **يُرسم شقيقًا للمستوى الأول** لا داخل عنصره / it renders as a SIBLING of the first level, never
   inside its element.
إعادة الترتيب إجراء واحد على القائمة كلها، وعلى المجموعة **الكاملة غير المُرشَّحة** من قيم النوع —
لا مجموعة جزئية: مسح مُرشِّح الرمز شرط لإتاحة السحب، لأن الخادم لا يميّز إرسال جزء من القيم عن
إرسال المجموعة كاملة عمدًا (frontend-execution-plan-mdl.md, G3) / reordering is one action over
the whole, unfiltered value set — never a partial one: clearing the code filter is a precondition
for the drag affordance, because the server cannot distinguish a partial submission from a
deliberate whole-set one. ولا إجراء على هذه الشاشة يملك نداءين / no action of this screen owns two
calls, so nothing here needs an ordered pair.

## SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner
Traces            : REQ-MDL-013, AC-MDL-013, UXD-MDL-001
Screen requirement: SCR-REQ-MDL-002 · page code MDL_TYPE_REGISTRY
UI pattern        : true hierarchy (parent/child) — وحدة مالكة → أنواعها / owner module → its types — من SRS §B1 حرفيًا / verbatim from the SRS screen entry
Sub-views         : Search (مرشِّحان / two filters) · Browse (المجموعات / the groups) — لا سطح إدخال البتة / no entry surface at all (SRS §B3: read-only browse)
Fields shown      : حقول ENT-MDL-001 المعروضة بالإشارة، كلها للقراءة / the displayed fields of ENT-MDL-001, every one read-only — الجدول أدناه / the table below
Composition       : none — سجل للقراءة فقط بلا تفصيل ثانوي ولا مُنتقٍ ولا مجموعة أبناء تُحرَّر / a read-only browse with no secondary detail, no picker and no editable child set · submits: none — الشاشة لا تكتب شيئًا / the screen writes nothing
Permissions       : SRS §B4 — MDL_TYPE_REGISTRY: VIEW (بوابة / gateway) وحدها؛ لا CREATE ولا UPDATE ولا DELETE / VIEW alone — reference only; names follow `PERM_<PAGE_CODE>_<ACTION>`
Cross-module data : `ownerModuleCode` → UXD-MDL-001 (owner module SEC) — هنا عنوان المجموعة نفسه، مع مصدر بديل عند تعذّر القراءة (ADR-MDL-015) / here it is the group heading itself, with a degraded-source fallback (ADR-MDL-015)
States            : empty — لا وحدة مالكة تطابق المرشِّحات / no owner module matches the filters (مجموعة بلا أنواع لا تُعاد أصلًا / a group with no types is not returned at all) · loading — هيكل عظمي للمجموعات / a skeleton over the groups · error — لافتة برسالة الكتالوج المحلية / a banner carrying the localized catalog message · offline — لا شيء: SRS لا تنصّ على عمل بلا اتصال / nothing: the SRS states no offline behaviour

| Field | التسمية (ar) | Label (en) | Read-only | من / from |
|---|---|---|---|---|
| ownerModuleCode | الوحدة المالكة | Owner module | نعم / yes — عنوان المجموعة / the group heading | SRS §B2, AC-MDL-013 |
| key | المفتاح | Key | نعم / yes | SRS §B2, AC-MDL-013 |
| nameAr | الاسم (عربي) | Name (Arabic) | نعم / yes | AC-MDL-013 |
| nameEn | الاسم (إنجليزي) | Name (English) | نعم / yes | AC-MDL-013 |

**المرشِّحان / the two filters** — SRS §B2: الوحدة المالكة (EXACT، وهو عنوان المجموعة نفسه) والمفتاح
(LIKE)، ولكلٍّ عمود نتيجة. المجموعة تُعاد كاملة بلا ترقيم صفحات، والأنواع الفعّالة وحدها تدخل السجل
(REQ-MDL-013)، فلا مرشِّح حالة هنا / the groups come back whole with no paging, and only active
types enter the registry, so there is no active-state filter on this screen.

**الإجراء الوحيد / the only action** — فتح النوع في الشاشة العامة / open the type in Generic
Lookups (SRS §B3): انتقال إلى SCR-MDL-001 بالنوع مختارًا، بحارس تلك الشاشة نفسه / a navigation to
SCR-MDL-001 with that type selected, under that screen's own guard.

---

## التبعيات العابرة للوحدات في طبقة العرض / Cross-module display dependencies

### UXD-MDL-001 — الوحدة المالكة تُقرأ من سجل وحدات الأمان / The owner module is read from the security module's registry
Traces      : REQ-MDL-001, REQ-MDL-002, REQ-MDL-013, AC-MDL-001, AC-MDL-002, AC-MDL-013
Screens     : SCR-MDL-001 (حقل إدخال عند الإنشاء، وعمود، ومرشِّح / an input on create, a column and a filter) · SCR-MDL-002 (عنوان المجموعة / the group heading)
Field       : `ENT-MDL-001.ownerModuleCode`
Owner       : SEC — `ModuleRegistry` (ENT-SEC-004)، وهو الكيان الوحيد الذي تستهلكه MDL (SRS §A8) / the one entity MDL consumes
Real API    : `POST /api/v1/sec/registry/search` (API-SEC-021) — سجل وحدات الأمان، شكله ومعرّفه من مواصفة تلك الوحدة لا من هنا / the security module's registry search; its shape and its id belong to that module's own artifacts
Grant       : كل دور يُمنح `PERM_MDL_LOOKUPS_CREATE` يلزمه `PERM_SEC_MODULE_REGISTRY_VIEW` أيضًا / every role granted `PERM_MDL_LOOKUPS_CREATE` must also hold `PERM_SEC_MODULE_REGISTRY_VIEW` — منحٌ تملكه وحدة الأمان، يُسمّى هنا ولا يُنشأ (ADR-MDL-013) / SEC's grant to make; named here, minted nowhere
Control     : قائمة اختيار على رموز الوحدات المسجَّلة، لا حقل نصّ حرّ (ADR-MDL-004) / a select over the registered module codes, never a free-text field

`UXD-MDL-001` backs **three** controls, and each has its own degraded behaviour — the control's
own shape decides the fallback, not which screen it sits on:

Degraded (SCR-MDL-001 · create-form select) : القراءة مرفوضة أو متعذّرة → القائمة فارغة ومعطَّلة
                  برسالة تسمّي القراءة الناقصة، وإجراء الإنشاء معطَّل خلفها؛ ولا رجوع إلى النصّ الحرّ
                  (ADR-MDL-013) / a refused or failed read leaves the create-form select empty and
                  disabled with a message naming the missing read, and the create action disabled
                  behind it — never a fall back to free text. This is the only one of the three
                  controls where a submitted value could reach the server unvalidated, which is
                  why it alone degrades to empty-and-disabled (ADR-MDL-013).
Degraded (SCR-MDL-001 · master-list search filter) : القراءة مرفوضة أو متعذّرة → المرشِّح يسقط إلى
                  القيم المميَّزة لـ`ownerModuleCode` الحاضرة فعلًا في ردّ API-MDL-001 نفسه، لا إلى
                  نصّ حرّ ولا إلى تعطيل الحقل؛ تصفّح قائمة الأنواع لا يُحجب بهذا العطل بحال — البيانات
                  البديلة موجودة أصلًا في كل صفّ يعيده البحث (ADR-MDL-016، G1) / a refused or failed
                  read leaves this read-only filter falling back to the distinct `ownerModuleCode`
                  values already present in the current API-MDL-001 response — never free text and
                  never disabled; browsing the type list is never blocked by the degraded source,
                  because the fallback data is already in every row the search returns
                  (ADR-MDL-016, G1). Nothing is submitted through a filter, so ADR-MDL-013's
                  write-path reasoning ("a code typed blind is a code the server is certain to
                  refuse") has no purchase here.
Degraded (SCR-MDL-002 · registry filter) : القراءة مرفوضة أو متعذّرة → المرشِّح يسقط إلى القيم
                  المميَّزة لـ`ownerModuleCode` الحاضرة فعلًا في ردّ API-MDL-010 نفسه، لا إلى نصّ حرّ
                  ولا إلى رمز لا تحمله أي مجموعة معروضة؛ عناوين المجموعات تبقى على الرمز الخام الذي
                  يعيده التصفّح أصلًا، والتصفّح لا يُحجب بهذا العطل بحال (ADR-MDL-015، G6) / a
                  refused or failed read leaves the SCR-MDL-002 filter falling back to the distinct
                  `ownerModuleCode` values already present in the current API-MDL-010 response —
                  never free text, never a code no displayed group carries; group headings stay on
                  the raw code the browse already returns, and browsing is never blocked by it
                  (ADR-MDL-015, G6).

`UXD-MDL-001` هو حاجة في طبقة التطبيق لا قيد قاعدة بيانات: شاشة تملكها هذه الوحدة تعرض بيانات
مرجعها الموثوق وحدةٌ أخرى / an application-layer need, not a DB constraint: a screen this module
owns displays data whose authoritative source is another module's real API. لا يشترك في شيء مع
`XM-MDL-001` (وهو فحص السلامة المرجعية في الخادم عند الإنشاء — RULE-MDL-001) ولا يظهر في أي أثر
خلفي / it shares nothing with the backend's cross-module record, which is the server-side
existence check behind RULE-MDL-001, and it appears in no backend artifact.

واحد لا ثلاثة: التبعية نفسها والخطّاف نفسه يخدمان الشاشتين وكل تحكّم فيهما، فلا يُمنت `UXD-*` لكل
شاشة ولا لكل حقل / ONE, not three: the same dependency and the same shared hook serve both screens
and every control on them; only ONE `UXD-*` is minted. The three degraded paths differ because
their **controls** differ, not because their screens differ: a select that must accept an
unpicked value (SCR-MDL-001's create form) cannot degrade the way a read-only filter over an
already-known result set can — and SCR-MDL-001's own master-list filter is exactly that second
kind, so it shares its fallback pattern with SCR-MDL-002's filter rather than with its own
screen's create select. Each control-kind's degraded behaviour is now its own ADR: ADR-MDL-013
for the select, ADR-MDL-016 for the master-list filter, ADR-MDL-015 for the registry filter.

**لا تبعية عرض ثانية** / no second display dependency: MDL لا تملك مفتاح قائمة قيم ولا تستهلك أيًّا
منه (SRS §A6)، فلا حقل على أي من الشاشتين مسنود بقائمة قيم، ولا قيمة مُرمَّزة تُقرأ من وحدة أخرى /
MDL owns no lookup key and consumes none, so no field on either screen is backed by a list of
values and no coded value is read from another module.

---

## RECONCILIATION — MDL v1

```
RECONCILIATION — MDL v1
B1 every US-* used in a flow has an SRS counterpart (REQ/AC/screen)
   ✓ US-MDL-001, US-MDL-002, US-MDL-004, US-MDL-005 — each named by the flow that uses it and
     each carried by a REQ and a screen requirement of the SRS. US-MDL-003 is used by no flow:
     its caller has no screen (ADR-MDL-007), so no screen was invented for it.
B2 no RULE-* contradicts a flow or a spec outcome
   ✓ RULE-MDL-001 (owner module registered) → the create form's owner-module field and its
     server message · RULE-MDL-002 (no duplicate code within a type) → the value form's code
     field, now naming the reserved-code case (G7) · RULE-MDL-003 (the key is immutable) →
     `key` read-only on edit, stated beside it · RULE-MDL-004 (an inactive type hides its
     values from consumers) → said in the deactivate confirmation, alongside the reserved-key
     consequence (G7), and NOT applied to this screen's own value list, which is the manager's
     view and shows inactive rows (AC-MDL-005, AC-MDL-009). No contradiction; no ADR needed for
     the rules themselves.
B3 every field and permission on a screen exists in the SRS
   ✓ extra: none — every field above is an SRS §A3 field of the owning entity and every
     permission is an SRS §B4 row. missing: none — no field of either entity is dropped, the
     audit four included, and they are marked system-filled rather than hidden.
B4 every screen entry of the SRS has exactly one SCR-* block
   ✓ SCR-REQ-MDL-001 → SCR-MDL-001 · SCR-REQ-MDL-002 → SCR-MDL-002. Two screen requirements,
     two SCR blocks, and the sub-views of each stay under their one SCR.
RESULT  reconciled 2 · reworked 0 · ADRs applied this pass: ADR-MDL-016 (new — SCR-MDL-001's
        master-list owner-module filter now falls back to the current search response's own
        distinct values instead of sharing the create-form select's empty-and-disabled
        behaviour, G1); applied unchanged: ADR-MDL-003, ADR-MDL-004, ADR-MDL-005, ADR-MDL-006,
        ADR-MDL-007, ADR-MDL-013, ADR-MDL-014, ADR-MDL-015
```

لا سؤال في هذه المرحلة ولا قرار بحالة BLOCKED / no question is raised at this stage and no ADR is
BLOCKED: كل نقطة احتملت وجهين حسمها مدخلٌ قائم أو قرار مُسجَّل / every two-sided point was settled
by an existing input or a recorded decision.
══════════════════════════════════════════════════════════════════
<<<END FILE>>>

<<<FILE: governance-shared/analysis/modules/MDL/P3_2/frontend-execution-plan-mdl.md>>>
# FRONTEND EXECUTION PLAN — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module    : MDL   Version : v1   Profile : erp   Track : frontend   Plan : exec
Framework : react-ts-vite · routing react-router · server-state tanstack-query ·
            forms react-hook-form · validation zod · state zustand ·
            one lazily-loaded chunk per composite screen (profile.stack.frontend)
Inputs    : srs (v1) · prd (v1) · api-docs (fetched from the backend repo, digest `bac95437…`,
            4 files) · registry-srs (v1) · registry-exec-be (v1)
Screens   : 2 — SCR-MDL-001, SCR-MDL-002 · UXD : 1 — UXD-MDL-001 · API published : 11, bound 11
Decisions : ADR-MDL-002 · ADR-MDL-003 · ADR-MDL-004 · ADR-MDL-005 · ADR-MDL-006 · ADR-MDL-007 ·
            ADR-MDL-011 · ADR-MDL-012 · ADR-MDL-013 · ADR-MDL-014 · ADR-MDL-015 · ADR-MDL-016 —
            all ACCEPTED, all non-breaking
            (superseded, cited by them and not applied here: ADR-MDL-001, ADR-MDL-008)
══════════════════════════════════════════════════════════════════

## API SURFACE — MDL v1   (shapes: `_inputs/api-docs-mdl.md` — cited by API id, never restated)

The published document now carries a `Contract ID` line per endpoint and an API column in its
catalog — 11 of 11 served endpoints — so every block below cites its endpoint by the id the
api-docs themselves publish (ADR-MDL-011). Verb, path, request and response DTOs, the paging
envelope and the error catalog are read there and are not copied here.

```
BINDING   REQ → API — the binding only
  REQ-MDL-001, REQ-MDL-002  → API-MDL-002   create a lookup type
  REQ-MDL-001, REQ-MDL-003  → API-MDL-001   search lookup types (the list the registrar returns to)
  REQ-MDL-003               → API-MDL-003   update a lookup type
  REQ-MDL-004               → API-MDL-004   deactivate a lookup type
  REQ-MDL-005               → API-MDL-005   search the selected type's values
  REQ-MDL-006, REQ-MDL-007  → API-MDL-006   create a lookup value
  REQ-MDL-008               → API-MDL-007   update a lookup value
  REQ-MDL-009               → API-MDL-008   deactivate a lookup value
  REQ-MDL-010               → API-MDL-009   reorder a type's values
  REQ-MDL-013               → API-MDL-010   browse the type registry by owner
  REQ-MDL-011, REQ-MDL-012  → API-MDL-011   read active values by key — a consuming module's
                                            backend call, bound here and called by no screen
                                            (ADR-MDL-007)
UNMAPPED  REQ needing an endpoint that has none : none — all 13 requirements bind.
          Documented endpoint mapping to no REQ : none — all 11 endpoints bind.
          Operations the SRS names for which nothing is published: read one type by id · read
          one value by id. Neither is required by a REQ; both are omitted rather than faked,
          and the entry sub-views hydrate from the row the search query already holds where
          that row is in cache, with a stated cold-load fallback where it is not
          (ADR-MDL-005, ADR-MDL-014). This SRS version names no `activate` action at either
          level (§B3: "لا إجراء تفعيل ولا محو نهائي"), so the half-toggle ADR-MDL-005 also
          covers is not a gap in this version — there is nothing to omit.
CODES     runtime error code → the RULE it carries, the link neither source states:
            MDL-409-MODULE-NOT-REGISTERED → RULE-MDL-001   (the owner module is registered in SEC)
            MDL-409-VALUE-DUP             → RULE-MDL-002   (no duplicate code within one type)
            MDL-404-TYPE-KEY              → RULE-MDL-004   (an inactive type hides its values)
            MDL-409-TYPE-DUP · MDL-404-TYPE · MDL-404-VALUE · MDL-400-REORDER-MISMATCH → no
            RULE of their own: platform-standard duplicate / not-found / validation rows under
            the ADR-SEC-002 umbrella the SRS §A5 names.
            RULE-MDL-003 carries no code at all: `key` is absent from the update request, so no
            code path can raise one. The form states the rule instead of waiting for a message.
          Platform rows the shared handler owns and this module does not mint:
            VALIDATION_ERROR (400) · ACCESS_DENIED (403) · METHOD_NOT_ALLOWED (405) ·
            DATA_INTEGRITY_VIOLATION (409) · INTERNAL_ERROR (500)
ENVELOPE  every response is wrapped in the published `ApiResponse<T>` — `success`, `data`,
          `error { code, message, fieldErrors[] { field, message } }`, `timestamp`. The
          published error object carries ONE message, not a bilingual pair: the client keys the
          text it displays on `error.code` against the module's catalog, which carries the ar
          and the en wording, so the language follows the user's locale; `error.message` is the
          fallback when a code is not in the catalog. `error.fieldErrors[].field` is what routes
          a validation message to a control.
PAGING    the published pagination envelope is `PageLookup<T>` (index.md), constraints from
          `PageableBuilder`: default page 0 · default size 20 · maximum size 200 — the same two
          numbers the SRS §B2 states. THREE response shapes travel in this module and the
          difference is load-bearing in every F2 block:
            · paginated  — API-MDL-001 only
            · bare array — API-MDL-005, API-MDL-009, API-MDL-010, API-MDL-011
            · one object — API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-006, API-MDL-007,
                           API-MDL-008
          **Correction applied a prior pass (G2):** an earlier revision modelled API-MDL-005 as
          paginated, on no published source. `_inputs/api-docs-mdl.md`, backend-execution-plan
          API-MDL-005 ("No paging: the result is bounded by one type and returned whole,
          ordered") and QR-MDL-005 (`Pagination: NO`) all agree the detail read is unpaged; SRS
          §B2 states the same. There is no divergence to record as an ADR here — every source
          agrees, and the frontend plan was the one artifact that had assumed otherwise. F1, F2
          and this table are corrected together.
SEARCH    the three reads of a collection are `POST …/search` carrying a
          `filters[] {field, operator, value}` envelope (operators EQUALS, NOT_EQUALS, LIKE,
          GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IN) — which is
          what the SRS §B5 tables state as well. Only the backend plan's contract summary still
          predicts GET for them, and that summary is never read as an API source (ADR-MDL-002).
LOOKUPS   none. MDL owns no lookup key and consumes none (SRS §A6): no field on either screen
          is backed by a list of values, no lookup hook exists anywhere in this plan, and no
          enum is modelled. The one constrained field, `ownerModuleCode`, binds to
          UXD-MDL-001's runtime-loaded list, which is another module's registry — not lookup
          data, and not read through API-MDL-011.
PERMS     declared by the backend and cited, never redeclared: PERM_MDL_LOOKUPS_VIEW (gateway) ·
          PERM_MDL_LOOKUPS_CREATE · PERM_MDL_LOOKUPS_UPDATE · PERM_MDL_LOOKUPS_DELETE ·
          PERM_MDL_TYPE_REGISTRY_VIEW. The api-docs state each endpoint's requirement, and the
          two deactivate endpoints require UPDATE, not DELETE — read there, not assumed here,
          and filed as PF-MDL-001 below (G5) rather than settled in prose.
```

**Where the business codes are stated, and where they are not.** The api-docs publish this
module's business codes once, in the index's Known Error Codes table, and state per endpoint
only the structurally guaranteed answers (`ACCESS_DENIED`, and `VALIDATION_ERROR` where a body
is accepted). The per-block routing below therefore maps each code to the endpoint whose rule
scope can raise it — the mapping is this plan's reading, not a published per-endpoint list, and
no code appears below that the catalog does not publish.

**Field precisions (G1, a prior pass).** Every `maxLength` in this plan is sourced from the
deployed column widths — db-script §1 and ADR-MDL-010 — and not restated from
`_inputs/api-docs-mdl.md` where the two disagree: `key` and `code` are `VARCHAR(50)`, every
`nameAr`/`nameEn` is `VARCHAR(200)`. If the api-docs genuinely publish 80/150, that is a defect
of the MDL api-doc generator on the backend repo, not a frontend choice: it is filed as
PF-MDL-002 below, and this plan binds to the db-script meanwhile so a value the database accepts
is never refused by the client and vice versa.

### Platform findings — MDL v1 frontend track

| id | status | owner | evidence | impact |
|---|---|---|---|---|
| PF-MDL-001 | OPEN | MDL backend track (P3.1) + backend repo method-level security annotations | backend-execution-plan-mdl.md PHASE 7 permission matrix and BOOTSTRAP DATA mark the DELETE column for API-MDL-004 and API-MDL-008 and seed `PERM_MDL_LOOKUPS_DELETE` with grant targets; `_inputs/api-docs-mdl.md` puts `PERM_MDL_LOOKUPS_UPDATE` on both deactivate endpoints | as built, a caller holding UPDATE alone can deactivate a type or a value, and a caller granted DELETE alone can deactivate neither; SRS §B4's DELETE row is unenforceable as built |
| PF-MDL-002 | OPEN (conditional) | MDL backend track — the api-docs generator | if `_inputs/api-docs-mdl.md` publishes `key`/`code` maxLength 80 and `nameAr`/`nameEn` maxLength 150, that contradicts db-script §1 (`VARCHAR(50)`, `VARCHAR(200)`) and ADR-MDL-010 | a 51–80 character key passes client validation and is refused by the database; a 151–200 character label the platform accepts is refused by the client |
| PF-MDL-003 | OPEN | platform — the search-filter contract for every module that omits a by-id read | no published filter set for API-MDL-001 or API-MDL-005 carries the record's own id (key, ownerModuleCode, name, isActiveFl on the first; lookupTypeId, code on the second) | a cold-load deep link into a type or value editor cannot resolve its target through any search, one-row or otherwise; see ADR-MDL-014 |
| PF-MDL-004 | OPEN | MDL backend track (P3.1) | QR-MDL-009's statement and Result-shape lines (backend-execution-plan-mdl.md) enforce only `lookup_type_id = :typeId` per submitted id, against this plan's F2-QUERY VALUE REORDER submission rule, which states the submitted set must always be the type's complete, unfiltered value set | a submitted set that is incomplete or carries a duplicate re-ranks 1..n and collides with the ranks of every value left out — nothing server-side rejects it; required backend change: reject in the API-MDL-009 orchestration, before the UPDATE loop and inside the same transaction, when the DISTINCT submitted id count does not equal the count of rows under `lookup_type_id = :typeId` (active and inactive both, matching QR-MDL-005's scope), and broaden MDL-400-REORDER-MISMATCH's catalog trigger — today it reads only "a submitted id does not belong to the type in the path" — to cover an incomplete or duplicated set |
| PF-MDL-005 | OPEN | MDL backend track (P3.1) | backend-test-plan-mdl.md TC-MDL-014's Preconditions line ("SEC's registry-search endpoint (the published HTTP read named in `ui-ux-spec-mdl.md`, ADR-MDL-013) is made unreachable/times out") against backend-execution-plan-mdl.md PHASE 6 (INT-R), which states there is no HTTP-level way to simulate "SEC unreachable" and no test should try to, and against the ALIGN-BE strike note for the same impossibility. that endpoint is this plan's own UXD-MDL-001 HTTP read, not the backend's in-process XM-MDL-001 call | TC-MDL-014 names the wrong surface and an unconstructible failure mode, and duplicates TC-MDL-002's coverage; required change: retarget the test to the SOFT-READ's real untested edge — a module deregistered from SEC after its types were accepted must not invalidate them (RULE-MDL-001's own Test-Hint) — and drop the foreign-endpoint citation |
| PF-MDL-006 | OPEN | SEC / P3.1 track | registry-exec-be-mdl.md XM STATUS and backend-execution-plan-mdl.md INT-C XM-MDL-001's Interface paragraph both state that SEC's own P3.1 artifacts register only `SecUserDirectoryApi`, so the module-registry read XM-MDL-001 consumes is written down nowhere on SEC's side, disclosed only as prose and deferred to "SEC's own re-run" while the XM is carried ACTIVE with "Unblock condition: none outstanding" | SEC v1's P3.1 artifacts must register `SecModuleRegistryApi` as an exposed cross-module surface alongside `SecUserDirectoryApi` — distinct from the published HTTP read UXD-MDL-001 uses (named in `ui-ux-spec-mdl.md`); the gap is specifically the in-process backend contract |

### Reconciliation against the SRS — run once, before any F-content

- **Every REQ that needs an endpoint has one**, and every documented endpoint maps to a REQ —
  the BINDING block above is the whole mapping, with no gap in either direction.
- **Three reads are `POST …/search`** where the backend plan's contract summary predicts `GET`.
  The api-docs and the SRS §B5 tables agree with each other, so the backend plan is the one
  artifact that lags; `gov.py analyze` reports it there (C8.4). Recorded, not silently corrected
  (ADR-MDL-002).
- **The published ids are now the api-docs' own** — the generator emits the `Contract ID` line
  it emits for the other modules, so this plan cites `API-MDL-*` rather than the plan-local
  labels its previous revision used (ADR-MDL-011, superseding ADR-MDL-008).
- **Two by-id reads the SRS names are published nowhere**; both entry sub-views hydrate from the
  search cache where the row is present, and redirect on a cold load where it is not
  (ADR-MDL-005, ADR-MDL-014).
- **`isActiveFl` is read-only at both levels**, which the SRS §B3 Editable column states as well;
  no published write DTO carries it, and the deactivate endpoints are what change it
  (ADR-MDL-006).
- **`ownerModuleCode` is a select over another module's registry**, cited as UXD-MDL-001 and
  never as a foreign path or a foreign API id (ADR-MDL-004); the foreign endpoint is named in
  `ui-ux-spec-mdl.md`, and the grant that must travel with the screen is ADR-MDL-013. Its
  degraded-source fallback on SCR-MDL-002 is ADR-MDL-015, and on SCR-MDL-001's own master-list
  search filter is ADR-MDL-016 (G1) — distinct from the create-form select's fallback,
  ADR-MDL-013, because the filter is a read over rows already on screen and the select is the
  one place a value could reach the server unvalidated.
- **Action-level grants are readable from no published surface**, so no affordance is hidden on
  a guess; the page gate is the readable half and the server's 403 is the other (ADR-MDL-012).
  The DELETE/UPDATE divergence itself is filed as PF-MDL-001, not resolved by this plan.
- **The detail read is unpaged**, agreeing with every backend and SRS source; a prior revision's
  paginated model was corrected (G2, that pass).
- **The whole value set is what a reorder submits**, and the screen refuses a reorder drag
  from a filtered or paged detail pane, because a partial submission collides ranks rather than
  being refused server-side (G3, a prior pass).
- **Nothing is invented.** No value absent from the api-docs appears in this plan, and no
  permission name, route, component or field is derived from anything but the SRS, the api-docs
  and the profile's own stack.

## EXECUTION PLAN INDEX — MDL v1

| # | Phase | Split | Blocks |
|---|---|---|---|
| 1 | F1 — Models & Types | per screen — always | `SUB:F1-SCR-MDL-001`, `SUB:F1-SCR-MDL-002` |
| 2 | F2 — Data Hooks | per screen — always | `SUB:F2-SCR-MDL-001`, `SUB:F2-SCR-MDL-002` |
| 3 | F3 — Forms & Validators | per screen — always | `SUB:F3-SCR-MDL-001`, `SUB:F3-SCR-MDL-002` |
| 4 | F4 — Screens & Routes | per screen — always | `SUB:F4-SCR-MDL-001`, `SUB:F4-SCR-MDL-002` |
| 5 | SEC-FE | never split | level-1 only |
| 6 | ALIGN-FE | never split | level-1 only |

One `SUB` per screen in each of the four sub-bearing phases, at any screen count: what the split
exists to give is a per-screen address — an implementer takes one screen's F2 block, not "the F2
phase of a small module" — and that is worth the same at two screens as at twelve
(`profile.tracks.frontend.plans.exec`).

**SCREEN REGISTRY**

| SCR | الاسم / Name | Page code | Container pattern | Owning ENT |
|---|---|---|---|---|
| SCR-MDL-001 | اللوكبات العامة / Generic Lookups | MDL_LOOKUPS | TREE_MASTER_DETAIL | ENT-MDL-001 (+ ENT-MDL-002) |
| SCR-MDL-002 | سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner | MDL_TYPE_REGISTRY | FULL_PAGE — no entry sub-view (ADR-MDL-003) | ENT-MDL-001 |

<!-- PHASE:F1:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-001,AC-MDL-003,AC-MDL-005,AC-MDL-006,AC-MDL-008,AC-MDL-010,AC-MDL-011,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009,API-MDL-010,API-MDL-011 -->
## PHASE 1 — F1 — Models & Types

Per entity and per screen: the source DTO with each property's type and its read-only /
system-only status, then the screen's search model, form model and container. Names are carried
per language (ar, en) wherever a label is modelled. Nothing is modelled that the api-docs do not
return, no internal identifier is invented, and **no enum and no lookup-backed field appears
anywhere in this phase** — MDL owns no coded list of its own (SRS §A6).

Field and DTO binding: see `_inputs/api-docs-mdl.md` — the published request and response shapes
for this module are the source and are not restated here. What is stated here is the shape's
consequence for the client: which properties a form may write, and which it may only display.
Every `maxLength` below is the deployed db-script column width (db-script §1, ADR-MDL-010), per
the correction in API SURFACE above (G1, a prior pass); a divergence from the api-docs is
PF-MDL-002, not a number restated from them.

<!-- SUB:F1-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,AC-MDL-001,AC-MDL-003,AC-MDL-005,AC-MDL-006,AC-MDL-008,AC-MDL-010,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009 -->
### F1 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

#### F1-MODEL — ENT-MDL-001 — نوع اللوكب / LookupType
Source DTOs  : `LookupTypeResponse` (read) · `LookupTypeCreateRequest` · `LookupTypeUpdateRequest`
  lookupTypePk    : number · read-only (PK) · system-only · never shown as a business reference
  key             : string · maxLength 50 (db-script §1, ADR-MDL-010) · required on create,
                    **read-only on edit** — RULE-MDL-003, and the update request does not carry it
  ownerModuleCode : string · maxLength 10 · required on create, **read-only on edit** — a plain
                    string holding the code, never an enum and never a union of literals; its
                    valid set is the security module's registry (UXD-MDL-001)
  nameAr          : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  nameEn          : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  isActiveFl      : boolean · read-only — flipped by API-MDL-004 alone (ADR-MDL-006)
  createdBy, createdAt, updatedBy, updatedAt : read-only · system-only (audit)

#### F1-MODEL — ENT-MDL-002 — قيمة اللوكب / LookupValue
Source DTOs  : `LookupValueResponse` (read) · `LookupValueCreateRequest` ·
               `LookupValueUpdateRequest` · `LookupValueReorderRequest`
  lookupValuePk : number · read-only (PK) · system-only
  lookupTypeId  : number · read-only — the path id of API-MDL-006, taken from the selected
                  parent, never typed
  code          : string · maxLength 50 · required on create, **read-only on edit** — the update
                  request does not carry it; unique within its type (RULE-MDL-002)
  nameAr        : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  nameEn        : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  sortOrder     : number · required on create **and** on update — and the same field the reorder
                  writes through `{ orderedValueIds[] }`; two paths, one field
  isActiveFl    : boolean · read-only — flipped by API-MDL-008 alone (ADR-MDL-006)
  createdBy, createdAt, updatedBy, updatedAt : read-only · system-only

#### F1-SCREEN — SCR-MDL-001
Search model : master — key : string · LIKE · ownerModuleCode : string · EXACT (options from the
               UXD-MDL-001 hook) · isActiveFl : boolean · EXACT · plus page, size, sortDirection,
               all inside the one request object of API-MDL-001 (the only paged read on this
               screen). No `sortField` is modelled: QR-MDL-001 declares a single ordering
               (`ORDER BY key`) and PHASE 1 states the module offers no free-form sort parameter,
               so `sortDirection` is available on `key` alone and keying a variation the server
               cannot produce would fragment the cache for nothing (G7).
               detail — code : string · LIKE · lookupTypeId : number · EXACT (from the selected
               parent, not typed) — **no page, no size, no sort**: API-MDL-005 is unpaged
               (backend-execution-plan API-MDL-005, QR-MDL-005 `Pagination: NO`, SRS §B2), so
               none of the three is modelled and none belongs in this screen's detail cache key
               (a prior pass's correction — G2)
Form model   : type · create — key, ownerModuleCode, nameAr, nameEn (all required)
               type · edit   — nameAr, nameEn (required); key and ownerModuleCode read-only
               value · create — code, nameAr, nameEn, sortOrder (all required)
               value · edit   — nameAr, nameEn, sortOrder (required); code read-only
               reorder        — the ordered list of value ids, and only ever the type's complete
               value set (G3, see F2-QUERY VALUE REORDER); not a per-row edit and not a form
               excluded system fields : both PKs · lookupTypeId · both isActiveFl · the audit four
Container    : TREE_MASTER_DETAIL — a master list of types and, beside it, the selected type's
               values; each of the two entry surfaces models its own record and nothing else
The master read returns the published pagination envelope; the detail read returns a bare array,
returned whole because it is confined to one type. Both write models drop every property no
published write DTO carries, so no form offers a field the server would ignore (ADR-MDL-006).
<!-- SUB:F1-SCR-MDL-001:END -->

<!-- SUB:F1-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-011,AC-MDL-013,API-MDL-010,API-MDL-011 -->
### F1 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

#### F1-MODEL — OwnerGroupResponse — المجموعة حسب المالك / Owner group
Source DTO   : `OwnerGroupResponse[]` — a bare array (API-MDL-010); this screen writes nothing
  ownerModuleCode : string · read-only — the group key, labelled through UXD-MDL-001
  types[]         : the same type projection as above, **every property read-only here** —
                    lookupTypePk, key, ownerModuleCode, nameAr, nameEn, isActiveFl and the audit
                    four. The type model of `F1-SCR-MDL-001` is reused, not re-declared; only its
                    writability differs, and on this screen there is none.

**No F1-MODEL for the consumer read (G11, a prior pass).** The bare array
`LookupValueResponse[]` API-MDL-011 returns is accounted for once, in the F2 block that binds it
(`F2 · SCR-MDL-002` below, per ADR-MDL-007): that block already carries its own caller, its own
error answers and `Cache key : n/a`. Modelling it a second time here, in a screen this frontend
renders, instructed an implementer to write a client type nothing in the delivered frontend
consumes; removed rather than kept as documentation, since the F2 block is the complete and
correct place for a foreign contract this plan binds but never emits.

#### F1-SCREEN — SCR-MDL-002
Search model : ownerModuleCode : string · EXACT · key : string · LIKE. **No page, no size, no
               sort** — the request object of API-MDL-010 carries `filters` alone, so none of the
               three is modelled and none belongs in this screen's cache key
Form model   : none — a read-only browse (SRS §B3); no property of the response is writable
Container    : FULL_PAGE, no entry sub-view (ADR-MDL-003) — the owner → types hierarchy is the
               grouped list the endpoint returns, not a second pane with a form in it
The response is a bare array of groups and is modelled as one: reading it through a pagination
envelope would invent fields the endpoint does not send.
<!-- SUB:F1-SCR-MDL-002:END -->
<!-- PHASE:F1:END -->

<!-- PHASE:F2:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-004,AC-MDL-005,AC-MDL-007,AC-MDL-009,AC-MDL-010,AC-MDL-011,AC-MDL-012,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009,API-MDL-010,API-MDL-011 -->
## PHASE 2 — F2 — Data Hooks

What each screen needs from the API — not hook code. Verb, path and request/response shape are
cited by the API id at each block's head and are read in the api-docs, never restated. Every read
query's cache key carries every filter that changes the response, page and size included **where
the endpoint is paged**; page and page size live inside the filter object and are never
independent state, and are absent from the key of an unpaged read entirely. Every mutation
declares its invalidation. Components use the facade only; the facade uses the declared queries
only (server-state library: `tanstack-query`).

Error routing is uniform and is stated per block only where a business code makes it specific:
field validation → inline on the field `error.fieldErrors[].field` names · business rule → the
user message for that rule · unauthenticated → the platform's sign-in destination, discarding the
server-state cache · forbidden → the localized catalog message on the surface that attempted the
call · server → the generic message.

<!-- SUB:F2-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,AC-MDL-001,AC-MDL-002,AC-MDL-004,AC-MDL-005,AC-MDL-007,AC-MDL-009,AC-MDL-010,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009 -->
### F2 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

#### F2-QUERY — TYPE SEARCH — API-MDL-001      traces=API-MDL-001,REQ-MDL-001,REQ-MDL-003
Kind         : read query — a POST that mutates nothing (ADR-MDL-002); paginated response
Cache key    : `[lookup-types, filters]`, `filters` being the whole request object — key,
               ownerModuleCode, isActiveFl, sortDirection **and page, size**. Every one of them
               changes the response, so every one is in the key. No `sortField` is in the key or
               the request: the backend offers no free-form sort parameter (G7, F1-SCREEN above).
Errors       : `VALIDATION_ERROR` → inline on the offending filter · `ACCESS_DENIED` → the
               localized forbidden message · `INTERNAL_ERROR` → generic
Loading      : LOCAL — the SRS states nothing about this call being slow, so no global indicator
Cache policy : defaults
Invalidation : n/a (a read); refreshed by API-MDL-002, API-MDL-003 and API-MDL-004
Note (G1)    : this query's response is also the fallback source for the master-list
               `ownerModuleCode` filter's own degraded state (see F2-SCREEN-INIT, ADR-MDL-016) —
               its rows are read for their `ownerModuleCode` values, not through a second call.

#### F2-QUERY — TYPE CREATE — API-MDL-002      traces=API-MDL-002,REQ-MDL-001,REQ-MDL-002,AC-MDL-001,AC-MDL-002
Kind         : mutation
Errors       : `MDL-409-MODULE-NOT-REGISTERED` → the RULE-MDL-001 message, routed to the
               owner-module field — ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» ·
               en: "The owning module is not registered in the Security module" ·
               `MDL-409-TYPE-DUP` → inline on `key` (the platform duplicate row) ·
               `VALIDATION_ERROR` → inline per `error.fieldErrors[].field` ·
               `ACCESS_DENIED` → the localized forbidden message (ADR-MDL-012)
Loading      : LOCAL — on the submitting form
Invalidation : `[lookup-types, *]`, and `[lookup-types-by-owner, *]` — a new type changes what
               SCR-MDL-002's registry shows
Success      : ar: «تم حفظ نوع اللوكب.» · en: "The lookup type has been saved." (AC-MDL-001)

#### F2-QUERY — TYPE UPDATE — API-MDL-003      traces=API-MDL-003,REQ-MDL-003,AC-MDL-003
Kind         : mutation
Errors       : `MDL-404-TYPE` → user message · `VALIDATION_ERROR` → inline ·
               `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL
Invalidation : `[lookup-types, *]`, `[lookup-types-by-owner, *]`
The request carries the two names and nothing else — RULE-MDL-003 expressed in the surface
itself: there is no key field to send, and none is sent.

#### F2-QUERY — TYPE DEACTIVATE — API-MDL-004  traces=API-MDL-004,REQ-MDL-004,AC-MDL-004
Kind         : mutation · no request body
Errors       : `MDL-404-TYPE` → user message · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL — on the confirmation
Invalidation : `[lookup-types, *]`, `[lookup-types-by-owner, *]` **and** `[lookup-values, *]` —
               RULE-MDL-004 makes an inactive type's values invisible to consumers, so a cached
               value list of that type is stale in meaning even though its rows did not change
Requires `PERM_MDL_LOOKUPS_UPDATE` per the api-docs, not DELETE — read there, not assumed, and
filed as PF-MDL-001 rather than settled here (G5).

#### F2-QUERY — VALUE SEARCH — API-MDL-005     traces=API-MDL-005,REQ-MDL-005,AC-MDL-005
Kind         : read query (ADR-MDL-002); **not paginated** — a bare array, ordered by
               `sortOrder` then `code`, returned whole because it is bounded by one type
               (backend-execution-plan API-MDL-005, QR-MDL-005, SRS §B2). A prior revision of
               this block modelled this call as paginated; corrected then (G2).
Cache key    : `[lookup-values, filters]` — lookupTypeId, code. No page and no size: the endpoint
               accepts neither, and keying a variation the server cannot produce would fragment
               the cache for nothing. The parent id is part of the key, so selecting another type
               is a different cache entry, not a refetch of the same one.
Errors       : `VALIDATION_ERROR` → inline on the offending filter · `ACCESS_DENIED` → the
               localized forbidden message · `INTERNAL_ERROR` → generic
Loading      : LOCAL — on the values pane alone; the master list does not blank while it loads
Cache policy : defaults
Invalidation : n/a (a read); refreshed by API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009
This read returns inactive values beside active ones — it is the manager's view, not the
consumer's, and the screen shows what API-MDL-011 hides (AC-MDL-005, AC-MDL-009).

#### F2-QUERY — VALUE CREATE — API-MDL-006     traces=API-MDL-006,REQ-MDL-006,REQ-MDL-007,AC-MDL-006,AC-MDL-007
Kind         : mutation · the parent type is the path id, taken from the selection
Errors       : `MDL-409-VALUE-DUP` → the RULE-MDL-002 message, routed inline to `code` —
               ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» ·
               en: "This code is already used within this type" — a deactivated value of the
               same code under this type may be the one holding it (RULE-MDL-002 permits no
               reactivation, G7); the list's own inactive row, still shown in this pane
               (AC-MDL-005), is the evidence the user can check ·
               `MDL-404-TYPE` → user message (the parent type is gone) ·
               `VALIDATION_ERROR` → inline · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL
Invalidation : `[lookup-values, *]`
Success      : ar: «تم حفظ القيمة.» · en: "The value has been saved." (AC-MDL-006)

#### F2-QUERY — VALUE UPDATE — API-MDL-007     traces=API-MDL-007,REQ-MDL-008,AC-MDL-008
Kind         : mutation
Errors       : `MDL-404-VALUE` → user message · `MDL-409-VALUE-DUP` → inline on `code` if the
               server ever raises it here (RULE-MDL-002 is scoped to create and update alike) ·
               `VALIDATION_ERROR` → inline · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL
Invalidation : `[lookup-values, *]` — the submitted `sortOrder` may have moved the row

#### F2-QUERY — VALUE DEACTIVATE — API-MDL-008 traces=API-MDL-008,REQ-MDL-009,AC-MDL-009
Kind         : mutation · no request body
Errors       : `MDL-404-VALUE` → user message · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL — on the confirmation
Invalidation : `[lookup-values, *]`
The row stays in this screen's list afterwards, marked inactive: it leaves the consumer's read,
not the manager's (AC-MDL-009). No endpoint exists to reverse it (ADR-MDL-005), so its `code`
stays reserved under this type for the life of the type — the confirmation states this (G7,
ui-ux-spec-mdl.md).

#### F2-QUERY — VALUE REORDER — API-MDL-009    traces=API-MDL-009,REQ-MDL-010,AC-MDL-010
Kind         : mutation · request `{ orderedValueIds[] }` · response a bare array in the
               persisted order — not a paginated envelope
Errors       : `MDL-400-REORDER-MISMATCH` → user message on the value list (the submitted set is
               not exactly that type's values) · `MDL-404-TYPE` → user message ·
               `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL — on the list, with the dragged order held until the call settles
Invalidation : on success only — `[lookup-values, *]`, since every row's `sortOrder` may have
               changed and the response carries the persisted order the list then renders from.
               On any non-2xx answer the pending order is discarded and the list re-renders from
               the last persisted response; no invalidation runs (G10). MDL-400-REORDER-MISMATCH
               therefore always leaves the list showing the order the server actually holds,
               never the rejected drag.
Submission rule (G3) : **the submitted list is always the type's complete, unfiltered value set.**
               `lookup_type_id = :typeId` is the only predicate QR-MDL-009 checks — it does not
               (and cannot) verify that every value of the type was submitted — so a subset
               re-ranks 1..n and collides with the ranks of every value left out. The values
               pane can produce exactly that subset (a non-empty `code` filter, F1/F3), so the
               drag affordance is **disabled** whenever the pane shows less than the whole type:
               a non-empty `code` filter is active. A shown reason accompanies the disabled
               state — ar: «امسح مرشّح الرمز لإعادة الترتيب» · en: "Clear the code filter to
               reorder" — because the API-MDL-005 read this pane is built on carries no paging to
               also guard against (G2). Reordering from a filtered pane is refused by the client
               before the call is made, not answered by the server, because the server cannot
               distinguish a partial submission from a deliberate whole-set one.
<!-- moved from a bare inline note: the endpoint takes `orderedValueIds[]`; the mismatch code
     exists for an id that does not belong to the type, never for a merely incomplete list. -->

#### F2-LOOKUP — none
MDL owns no lookup key and consumes none (SRS §A6). The one option list on this screen is the
owner-module select, which is not lookup data: it is UXD-MDL-001, declared below.

#### F2-SCREEN-INIT — SCR-MDL-001
Permission read : `MDL_LOOKUPS` present in the caller's effective menu → VIEW, the gateway.
                  CREATE, UPDATE and DELETE are readable from no published surface; the server
                  answers them per call (ADR-MDL-012, PF-MDL-001).
Lookups used    : none
Foreign data    : the owner-module select (create form) and the owner-module filter (master
                  list) both resolve through UXD-MDL-001 — ONE shared hook, long-lived cache,
                  shared with SCR-MDL-002 — but the two controls degrade differently, because a
                  select that must accept an unpicked value cannot degrade the way a read-only
                  filter over an already-known result set can:
                    · create-form select — a refused read leaves the select empty and disabled,
                      and the create action disabled behind it (ADR-MDL-013)
                    · master-list filter — a refused read leaves the filter falling back to the
                      distinct `ownerModuleCode` values already present in the current
                      API-MDL-001 response (see F2-QUERY TYPE SEARCH above) — never empty, never
                      disabled, and browsing the type list is never blocked by the degraded
                      source (ADR-MDL-016, G1)
Entity by id    : none published at either level; both entry sub-views hydrate from the row their
                  own search query already holds when that row is in cache; on a cold load the
                  hydrating row is absent and the route redirects to `/reference-data/lookups`
                  (type editor) or `/reference-data/lookups/:typeId` (value editor) rather than
                  render empty (ADR-MDL-014)

#### F2-FACADE — SCR-MDL-001
Composes     : API-MDL-001, API-MDL-005 (the two lists) · API-MDL-002, API-MDL-003, API-MDL-004,
               API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009 (the mutations) · the
               UXD-MDL-001 hook
State it owns: the type list and the selected type's value list, both derived from their queries'
               data and never copied into state; the selected type id, read from the route param;
               two filter objects (the master's carrying page and size, the detail's carrying
               `code` alone); the pending drag order while a reorder is in flight, and whether the
               detail's `code` filter is empty (the reorder-eligibility flag of G3); the open
               editor's identity, held in navigation state rather than in a boolean; and a
               derived loading flag over the calls in flight
Operations   : createType · updateType · deactivateType (confirmed, and the confirmation says
               consumer reads will stop returning this type's values, and that `key` stays
               reserved and unreusable afterwards — RULE-MDL-004, G7) · createValue · updateValue
               · deactivateValue (confirmed, same reserved-code statement, G7) · reorderValues
               (the whole ordered list, submitted once, disabled while the detail pane is
               filtered — G3)
Ordered pairs: none. **No operation of this screen owns two calls** — a type's write carries no
               values, so nothing here updates a record and then replaces its child set, and no
               ordering or skip-if-unchanged rule is needed. Each operation is exactly one call.
There is no activateType and no activateValue: the SRS names no such action and no endpoint
exists for one (ADR-MDL-005). Components use the facade only; the facade uses the declared
queries only.
<!-- SUB:F2-SCR-MDL-001:END -->

<!-- SUB:F2-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-011,AC-MDL-012,AC-MDL-013,API-MDL-010,API-MDL-011 -->
### F2 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

#### F2-QUERY — REGISTRY BROWSE — API-MDL-010  traces=API-MDL-010,REQ-MDL-013,AC-MDL-013
Kind         : read query (ADR-MDL-002) · request carries `filters` alone — no sort, no paging ·
               response a bare array of owner groups
Cache key    : `[lookup-types-by-owner, filters]` — ownerModuleCode and key, and nothing more.
               No page or size belongs in this key because the endpoint accepts neither; adding
               them would key a variation the server cannot produce.
Errors       : `VALIDATION_ERROR` → inline on the offending filter · `ACCESS_DENIED` → the
               localized forbidden message · `INTERNAL_ERROR` → generic
Loading      : LOCAL
Cache policy : defaults
Invalidation : n/a (a read). It is refreshed by SCR-MDL-001's type mutations, which name this
               key family in their own invalidation lines — a type created, renamed or
               deactivated there changes what this registry shows.

#### F2-QUERY — CONSUMER READ BY KEY — API-MDL-011  traces=API-MDL-011,REQ-MDL-011,REQ-MDL-012,AC-MDL-011,AC-MDL-012
Kind         : read query · **bound, and called by no screen of this module** (ADR-MDL-007).
               This block is the complete accounting for API-MDL-011's response shape — no
               second model of it exists in F1 (G11).
Caller       : a consuming module's backend, over the platform's in-process module interface —
               not a user at a screen. Stated here so the published surface is completely
               accounted for.
Errors       : `MDL-404-TYPE-KEY` [REQ-MDL-012, RULE-MDL-004] → answered to the calling module —
               ar: «لا يوجد نوع لوكب بهذا المفتاح» · en: "No lookup type exists with this key" —
               an unknown key is a not-found, never an empty success ·
               `ACCESS_DENIED` → answered to the calling module
Cache key    : n/a — no client of this plan calls it
Invalidation : n/a. What a user can observe of this endpoint is indirect: deactivating a value,
               or its type, on SCR-MDL-001 is what makes it stop being returned (RULE-MDL-004).

#### F2-LOOKUP — none
Same as the other screen, and for the same reason: MDL owns no lookup key and consumes none.

#### F2-SCREEN-INIT — SCR-MDL-002
Permission read : `MDL_TYPE_REGISTRY` present in the caller's effective menu → VIEW. This screen
                  has no other action to gate (SRS §B4).
Lookups used    : none
Foreign data    : the owner-module filter and the group headings resolve through UXD-MDL-001 —
                  the same shared hook SCR-MDL-001 uses, not a second call. When that read is
                  refused or fails, the owner select falls back to the **distinct
                  `ownerModuleCode` values present in the current API-MDL-010 response**, which
                  the browse already returns on every group — never to free text, and never to a
                  code the registry does not currently hold. Group headings themselves fall back
                  to the bare code the browse returns, unchanged from before; browsing is never
                  blocked by the degraded source (ADR-MDL-013, ADR-MDL-015, G6)
Entity by id    : none — each group carries its full type rows

#### F2-FACADE — SCR-MDL-002
Composes     : API-MDL-010 · the UXD-MDL-001 hook
State it owns: the groups derived from the query's data, and the filter object (owner module,
               key) mirrored from the route's search params, so a filtered registry view is
               shareable by address (ADR-MDL-002). No grouping and no count is composed here —
               both arrive in the response.
Operations   : none — this screen writes nothing. Opening a type in SCR-MDL-001 is a route
               change, not an operation.
Ordered pairs: none — there is no call to order.
<!-- SUB:F2-SCR-MDL-002:END -->
<!-- PHASE:F2:END -->

<!-- PHASE:F3:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-010,REQ-MDL-011,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-006,AC-MDL-007,AC-MDL-008,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-010 -->
## PHASE 3 — F3 — Forms & Validators

One block per `RULE-*` a form enforces, plus the field constraints the published DTOs state. No
frontend-only validation the SRS does not state; every message is read from its catalog code
rather than hard-coded; the locale resolves session → browser → `ar` (`profile.languages.primary`);
and a caller without the write permission is answered by the server, not by a pre-emptively
disabled field (ADR-MDL-012). Schemas are written with `zod` and bound with `react-hook-form`.
Every `maxLength` below is the deployed column width (db-script §1, ADR-MDL-010), per the
correction in API SURFACE (G1, a prior pass).

**No option-set validator exists anywhere in this module.** MDL owns no coded list (SRS §A6), so
no field binds to a set of lookup values. The one field with a constrained set is
`ownerModuleCode`, whose set is another module's registry: its validator binds to the
runtime-loaded list of UXD-MDL-001, never to a static list of module codes.

<!-- SUB:F3-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-010,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-006,AC-MDL-007,AC-MDL-008,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-005,API-MDL-006,API-MDL-007 -->
### F3 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

Validation timing for this screen, declared once and holding for both entry surfaces: **on blur
for the unique key and the unique code, on submit for everything else.**

#### F3-FIELD — SCR-MDL-001 (type · create)
key             · REQUIRED · LENGTH (maxLength 50) · UNIQUE_CHECK · on blur
ownerModuleCode · REQUIRED · LENGTH (maxLength 10) · MEMBER_OF the UXD-MDL-001 list ·
                  BUSINESS_RULE (RULE-MDL-001) · on submit
nameAr, nameEn  · REQUIRED · LENGTH (maxLength 200) · on submit

#### F3-FIELD — SCR-MDL-001 (type · edit)
key, ownerModuleCode · read-only — not inputs at all; the update request carries neither
nameAr, nameEn       · REQUIRED · LENGTH (maxLength 200) · on submit

#### F3-FIELD — SCR-MDL-001 (value · create)
code            · REQUIRED · LENGTH (maxLength 50) · UNIQUE_CHECK within the selected type
                  (RULE-MDL-002) · on blur
nameAr, nameEn  · REQUIRED · LENGTH (maxLength 200) · on submit
sortOrder       · REQUIRED · integer · on submit

#### F3-FIELD — SCR-MDL-001 (value · edit)
code            · read-only — the update request does not carry it
nameAr, nameEn  · REQUIRED · LENGTH (maxLength 200) · on submit
sortOrder       · REQUIRED · integer · on submit

UNIQUE_CHECK    : async, on blur — both backend filters are LIKE, never EQUALS (QR-MDL-013's
                  `key LIKE :key`, QR-MDL-005's `code LIKE :code`; PHASE 1's Search contract and
                  SRS §B2 agree), so the check requests the LIKE filter the backend actually
                  declares and then asserts exact string equality client-side over the returned
                  rows before showing the inline message: the type's `key` through API-MDL-001
                  (LIKE `key`, then filter the response for an exact match) — the value's `code`
                  through API-MDL-005 scoped to `lookupTypeId` (LIKE `code`, then filter the
                  response for an exact match), so the check's scope is the rule's scope. This
                  uses only the published surface and is correct whether or not the service ever
                  honours EQUALS (G6). Neither blocks submit on its own: `MDL-409-TYPE-DUP` and
                  `MDL-409-VALUE-DUP` from the server are the authority, routed inline to the
                  same field. On edit neither field is an input, so neither check runs.

#### F3-VALIDATION — RULE-MDL-001   traces=REQ-MDL-002,AC-MDL-002
Statement : The system shall reject a lookup type registration whose owner module code has no
            ModuleRegistry row in the security module.
Message   : catalog code `MDL-409-MODULE-NOT-REGISTERED` —
            ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» ·
            en: "The owning module is not registered in the Security module"
Scope     : CREATE · Field : ownerModuleCode · kind BUSINESS_RULE · when submit
Shape     : the control is a select over the registered module codes loaded through UXD-MDL-001,
            so the common case cannot be typed wrong at all; the validator asserts that the
            submitted value is one the runtime-loaded list contains, never that it is one of a
            static set. The server stays the authority — a module deregistered between load and
            submit is caught there — and the catalog message routes to this field. When the
            foreign read is refused the select is empty and disabled and create is disabled
            behind it, rather than falling back to free text (ADR-MDL-013). The read-only
            master-list filter beside this control degrades differently — to the current search
            response's own values, never disabled — because nothing is submitted through a
            filter (ADR-MDL-016, G1).

#### F3-VALIDATION — RULE-MDL-002   traces=REQ-MDL-007,AC-MDL-007
Statement : The system shall reject a lookup value whose code already exists under the same
            lookup type.
Message   : catalog code `MDL-409-VALUE-DUP` — ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» ·
            en: "This code is already used within this type"
Scope     : CREATE (and UPDATE, per the rule's trigger) · Field : the value's code ·
            kind UNIQUE_CHECK · when blur, and again on submit by the server
Shape     : uniqueness is scoped to the parent type, never globally — the same code under
            another type is legitimate, and a global check would reject a value the server
            accepts. The async check is bound to the selected parent id. On edit the field is
            read-only, so the client check cannot fire and the rule is the server's alone. A
            deactivated value under the same type can be the one holding a submitted code — no
            activate endpoint exists (ADR-MDL-005), so the code stays reserved — and the inline
            message names that possibility (G7, F2-QUERY VALUE CREATE).

#### F3-VALIDATION — RULE-MDL-003   traces=REQ-MDL-003,AC-MDL-003
Statement : The system shall prevent editing a lookup type's key after creation.
Message   : the rule's own text — ar: «لا يمكن تعديل مفتاح نوع اللوكب بعد إنشائه» ·
            en: "A lookup type's key cannot be changed after creation"
Scope     : UPDATE · Field : key · kind BUSINESS_RULE · not a form check at all
Shape     : there is **nothing to validate** — `key` is not an input on edit, because the update
            request does not carry it. The rule is expressed by the absence of the field rather
            than by a message on a control that would refuse. The form still states the rule
            beside the read-only key, so an editor learns why it cannot be changed instead of
            meeting a disabled control with no explanation.

#### F3-VALIDATION — RULE-MDL-004   traces=REQ-MDL-004,AC-MDL-004
Statement : While a lookup type is inactive, the system shall exclude its values from consumer
            reads.
Message   : the rule's own text — ar: «هذا النوع معطّل حاليًا» ·
            en: "This lookup type is currently inactive"
Scope     : the deactivate action (API-MDL-004) · Field : none — a row action ·
            kind BUSINESS_RULE · when submit
Shape     : **not a validation this form performs** — it is a consequence the deactivate
            confirmation names before the act: every consuming module stops receiving this
            type's values, and — because no activate endpoint exists — the type's `key` stays
            reserved under the platform and cannot be reused by a later registration (G7). The
            rule's text is also the state label on an inactive type row, so the same words
            explain the row and the warning. Nothing on this screen is hidden by it: the
            manager's value list still shows the values, which is the difference between this
            screen and a consumer.

Business-code fields: `key` and `code` are client-chosen strings, not platform-numbered, and both
are read-only after create per the two update DTOs. Neither is generated or predicted on the
client (SRS §3.3 numbering).
Locale : session → browser → `ar`.
Permission-driven behaviour: a caller without CREATE, UPDATE or DELETE receives `ACCESS_DENIED`
on submit and the form shows the localized message; fields are not pre-emptively disabled,
because no published surface tells this screen which actions its caller holds (ADR-MDL-012,
PF-MDL-001).
<!-- SUB:F3-SCR-MDL-001:END -->

<!-- SUB:F3-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-013,AC-MDL-013,API-MDL-010 -->
### F3 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

This screen has **no form**: SRS §B3 reads "read-only browse; no create/update here", and every
property of the response is read-only.

#### F3-FIELD — SCR-MDL-002 (filters — not a form)
ownerModuleCode · optional · the select offers the UXD-MDL-001 list; when that read is refused
                  or fails the select falls back to the distinct `ownerModuleCode` values in the
                  current API-MDL-010 response, never to free text (ADR-MDL-013, ADR-MDL-015, G6)
                  — a code that is no longer registered but still owns types is still shown in
                  the results either way, because the grouping is the server's
key             · optional · LENGTH (maxLength 50) · a LIKE filter

#### F3-VALIDATION — none on this screen
No `RULE-*` is enforced here, because nothing is written. RULE-MDL-004's effect is visible — a
deactivated type leaves this registry's active set — but the rule fires on the consumer read
(API-MDL-011), not on this screen.
Shape  : filter validation only, written with `zod` over the route's search params, so an address
         someone shared is validated exactly as a typed filter is.
Locale : session → browser → `ar`.
Permission-driven behaviour: a caller without VIEW never reaches this screen — the navigation
guard of SEC-FE stops the route before any of this runs.
<!-- SUB:F3-SCR-MDL-002:END -->
<!-- PHASE:F3:END -->

<!-- PHASE:F4:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-005,AC-MDL-009,AC-MDL-010,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009,API-MDL-010,API-MDL-011 -->
## PHASE 4 — F4 — Screens & Routes

One block per `SCR-*`: routes, chunk, guard, facade, components, composition and cross-module
citations. Routes are named by the container pattern — `TREE_MASTER_DETAIL` → one page hosting
the master list and the detail beside it, with the list route registered **before** any `:id`
route and every static segment registered before the parameterised ones; `FULL_PAGE` with no
entry sub-view → a single page and no entry route. One lazily-loaded chunk per composite screen.
Every `PERM_*` name below is the backend registry's, cited and never invented, and every route
sits under the module segment `/reference-data`.

<!-- SUB:F4-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,AC-MDL-005,AC-MDL-009,AC-MDL-010,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009 -->
### F4 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

#### F4-SCREEN — SCR-MDL-001
Routes       : base slug `lookups`, under `/reference-data` —
               `/reference-data/lookups` — the type list, registered **before** any `:id` route ·
               `/reference-data/lookups/new` — the type entry, a **static** segment registered
               before the parameterised ones ·
               `/reference-data/lookups/:typeId` — that type's values beside the list ·
               `/reference-data/lookups/:typeId/edit` — the type entry, edit ·
               `/reference-data/lookups/:typeId/values/new` — the value entry, create ·
               `/reference-data/lookups/:typeId/values/:valueId/edit` — the value entry, edit
Chunk        : one lazy chunk for this composite screen — both panes and both entry surfaces
               share it; neither entry is a chunk of its own
Guard        : every route element guarded by `PERM_MDL_LOOKUPS_VIEW`, evaluated as "`MDL_LOOKUPS`
               is in the caller's effective menu". The write routes carry the same guard, because
               CREATE, UPDATE and DELETE are readable from no published surface and the server's
               403 is the authority on the write itself (ADR-MDL-012, PF-MDL-001)
Facade       : the SCR-MDL-001 facade of F2 — the page never calls a query directly
Components   : `LookupsPage` (route-level, TREE_MASTER_DETAIL) · `LookupTypeList`,
               `LookupTypeFilters`, `LookupTypeForm`, `LookupValuePane`, `LookupValueRow`,
               `LookupValueFilters`, `LookupValueForm`, `ValueReorderHandle`, `DeactivateConfirm`
               (presentational)
Mode         : CREATE | EDIT | VIEW resolved from the route match — `/new` and `/values/new` →
               CREATE, `/edit` → EDIT, `/:typeId` → VIEW — never from a parent's prop
Cold-load hydration (ADR-MDL-014) : the two **true edit routes** —
               `/reference-data/lookups/:typeId/edit` and
               `/reference-data/lookups/:typeId/values/:valueId/edit` — hydrate their form from
               the row their own search query already holds (ADR-MDL-005). No by-id read is
               published for either entity (PF-MDL-003: the search filter sets of API-MDL-001 and
               API-MDL-005 carry no id operator), so on a cold load — the hydrating row absent
               from cache, as when a link is opened directly rather than navigated to — each of
               these two routes redirects instead of rendering an empty form.
               `/reference-data/lookups/:typeId/values/new` is different in kind, not degree: it
               **creates** a record, so it has no row of its own to hydrate at all — nothing
               about it can be "empty" the way an edit form can. Its redirect on a cold load
               protects the **parent type's display context**, not a value record: the values
               pane it opens over renders from API-MDL-005, whose cache key is
               `[lookup-values, {lookupTypeId, code}]` and needs no cached type row to answer —
               so what a missing type in cache actually costs this route is the surrounding page
               (the type's own name and state around the pane), not the create form's own fields,
               which start blank regardless (G2).
               All three routes redirect to `/reference-data/lookups/:typeId`; if the type itself
               is not in cache either, to `/reference-data/lookups`. The destination shows the
               localized message — ar: «افتح السجل من القائمة» · en: "Open the record from the
               list" — rather than a blank editor. This needs no unpublished endpoint and no
               invented filter, and it keeps every route addressable for the in-session case it
               was written for.
               **Note (G2):** the corresponding line of `ADR-MDL-014.md` still groups all three
               routes under one "hydrate a row from cache or redirect" rule, which overstates
               `/values/new`'s own constraint. That file is outside this stage's writable set;
               the correction above is carried forward against it, in the same posture G8/G9
               already record (registry-exec-fe-mdl.md).
Composition  : the spec's line resolved to components. Nothing is inline: `LookupValuePane` is not
               a control inside `LookupTypeForm`, and the type's write carries no values, so the
               type form has nothing of the values in it. Each value is a **summary row**
               (`LookupValueRow` — code, both names, order, state) and its editor is the **second
               level**: `LookupValueForm` is a SIBLING of `LookupValuePane`, never rendered inside
               its element; it is opened from the route (`/values/new`, `/values/:valueId/edit`),
               so back closes it, dismissing closes only it and a deep link opens it, subject to
               the cold-load redirect above; it carries no scroll region of its own — the pane
               scrolls with the body it sits in
Saves        : ONE per open surface, and one surface at a time. `LookupTypeForm` submits once
               (API-MDL-002 or API-MDL-003); `LookupValueForm` submits once (API-MDL-006 or
               API-MDL-007) for its own record. The two are never open together: a value route is
               a state of the detail level, not a panel beside the open type form. **No action of
               this screen owns two calls**, so no ordered pair and no skip-if-unchanged rule
               arises. Deactivate and reorder are direct actions over a row and over the list;
               neither is a form and neither carries a second submit. The reorder handle is
               disabled whenever the detail pane's `code` filter is non-empty (G3, F2-QUERY VALUE
               REORDER)
Cross-module : UXD-MDL-001 — the owner-module select on the type entry (fallback: empty and
               disabled, ADR-MDL-013), the owner-module filter on the master list (fallback: the
               distinct values already in the current search response, ADR-MDL-016, G1), and the
               owner column of the list. The one field on this screen whose authoritative source
               is another module
The selected type is a route param, so a type's values are a linkable address and the browser's
back gesture returns to the list. Both levels offer Deactivate and neither offers an Activate: no
endpoint exists for the second half, and the confirmation says the act is not reversible from
this screen and that the code/key stays reserved (ADR-MDL-005, G7). The drag handle submits the
whole ordered set through API-MDL-009 rather than writing one row's `sortOrder`.
<!-- SUB:F4-SCR-MDL-001:END -->

<!-- SUB:F4-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-013,API-MDL-010,API-MDL-011 -->
### F4 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

#### F4-SCREEN — SCR-MDL-002
Routes       : base slug `type-registry`, under `/reference-data` —
               `/reference-data/type-registry` — the only route. No `new`, no `:id`, no
               `:id/edit`: this screen addresses no record it could edit. The owner module and
               the key filter live in the route's search params, so the browse IS its address
Chunk        : one lazy chunk for this screen
Guard        : the route element guarded by `PERM_MDL_TYPE_REGISTRY_VIEW`, evaluated as
               "`MDL_TYPE_REGISTRY` is in the caller's effective menu"
Facade       : the SCR-MDL-002 facade of F2
Components   : `TypeRegistryPage` (route-level, FULL_PAGE) · `RegistryFilters`,
               `OwnerGroupSection`, `RegistryTypeTable` (presentational)
Mode         : not applicable — no CREATE, EDIT or VIEW mode to resolve; this screen writes
               nothing
Composition  : `none`, resolved: no picker, no child-row editor, no second level and no component
               opened over this page. `OwnerGroupSection` renders the groups the endpoint
               returns, in the endpoint's own grouping
Saves        : none — the screen submits nothing at all. Its one affordance is a link into
               SCR-MDL-001, which is a navigation, not a save
Cross-module : UXD-MDL-001 — here it is the grouping itself and the label on each group heading,
               not a field of a form; its degraded-source fallback is ADR-MDL-015 (G6)
Each type row links to `/reference-data/lookups/:typeId`, which is SCR-MDL-001's own route and
carries its own guard: reviewing and managing are two steps of one task, and this screen does
neither half of the second. API-MDL-011 has no component and no route here (ADR-MDL-007).
<!-- SUB:F4-SCR-MDL-002:END -->
<!-- PHASE:F4:END -->

<!-- PHASE:SEC-FE:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-004,REQ-MDL-005,REQ-MDL-009,REQ-MDL-013 -->
## PHASE 5 — SEC-FE

The frontend half of the security model, per `SCR-*`: the navigation guard and the UI behaviour
per action. Permission names are the backend registry's and the SRS Access summary's, cited and
never redeclared. One mechanism gates both screens — the **menu gate**: the screen's page code is
present in the effective menu the security module serves this caller. Action-level grants are
published nowhere, so an action's affordance renders for a caller who holds the screen and the
server's `ACCESS_DENIED` is the authority, shown as its localized message (ADR-MDL-012). Never
split — level-1 only.

### SEC-FE · SCR-MDL-001 — اللوكبات العامة / Generic Lookups
Permissions      : `PERM_MDL_LOOKUPS_VIEW` (gateway) · `PERM_MDL_LOOKUPS_CREATE` ·
                   `PERM_MDL_LOOKUPS_UPDATE` · `PERM_MDL_LOOKUPS_DELETE`
Navigation guard : `MDL_LOOKUPS` must be in the caller's effective menu. A caller without it is
                   sent to the unauthorized destination, and every route of this screen — the
                   list, `new`, `:typeId`, `:typeId/edit` and both value routes — carries the
                   same guard.
Per action       : **VIEW** → the gate above, exact, and it covers both panes: a caller who holds
                   the screen sees types and values alike; without it no route of this screen
                   renders at all. **CREATE** (a type, and a value under it) → not readable
                   before the call; the affordance renders and a 403 is shown as the localized
                   message on the form that attempted it. **UPDATE** (edit at either level, and
                   the reorder) → the same. **DELETE** (which here means deactivate and nothing
                   else — no hard delete exists at either level) → the same.

**Filed as a platform finding, not settled here (G5).** The api-docs put `PERM_MDL_LOOKUPS_UPDATE`
on **both** deactivate endpoints, while backend-execution-plan-mdl.md PHASE 7's permission matrix
and BOOTSTRAP DATA mark the DELETE column for API-MDL-004 and API-MDL-008 and seed
`PERM_MDL_LOOKUPS_DELETE` with grant targets — a real divergence, not a naming detail: as built,
UPDATE alone lets a caller deactivate, and DELETE alone lets a caller deactivate nothing, so SRS
§B4's DELETE row is unenforceable as built. This is filed as **PF-MDL-001** (API SURFACE, above),
owner the MDL backend track, evidence the two artifacts just named. The frontend neither checks
`PERM_MDL_LOOKUPS_DELETE` nor depends on it: the guard it can evaluate is the page gate, and the
authority on a write is the server's answer to the call actually made.

The screen-level grant is the whole granularity available. Per-lookup-type permissions — letting
a role manage one type but not another — are an explicit SRS scope exception (§A2), so no
per-type gate is drawn, attempted or hinted at in the UI.
Foreign grant    : a role granted `PERM_MDL_LOOKUPS_CREATE` needs `PERM_SEC_MODULE_REGISTRY_VIEW`
                   as well, or the owner-module select it must fill stays empty and disabled. The
                   grant is the security module's to make; this plan names it and mints nothing
                   (UXD-MDL-001, ADR-MDL-013). The master-list owner-module filter needs no such
                   grant of its own: it degrades to data the search response already carries
                   (ADR-MDL-016, G1), never to a blocked control.

### SEC-FE · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner
Permissions      : `PERM_MDL_TYPE_REGISTRY_VIEW`
Navigation guard : `MDL_TYPE_REGISTRY` must be in the caller's effective menu.
Per action       : **VIEW** → the gate above. There is no CREATE, UPDATE or DELETE on this
                   screen: the SRS Access summary gives it VIEW alone and the screen writes
                   nothing, so there is no affordance to hide and no submit to refuse. Its links
                   into SCR-MDL-001 render unconditionally; the target route's own guard stops a
                   caller who does not hold that screen, so a reviewer who may browse but not
                   manage sees the registry and is refused at the door of the editor rather than
                   shown a dead link.

**Across both screens.** A forbidden response is shown as its localized catalog message, never as
a silent no-op and never as a generic failure. An unauthenticated response returns the caller to
the platform's sign-in destination and discards the server-state cache, so no data of the
previous identity survives into the next. No screen composes a permission name and no screen
keeps a local copy of the caller's grants: the menu response is the single source, and a failure
to load it renders no entry of this module and grants no route of it — access narrows, never
widens.
<!-- PHASE:SEC-FE:END -->

<!-- PHASE:ALIGN-FE:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-005,AC-MDL-006,AC-MDL-007,AC-MDL-008,AC-MDL-009,AC-MDL-010,AC-MDL-011,AC-MDL-012,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009,API-MDL-010,API-MDL-011 -->
## PHASE 6 — ALIGN-FE

The alignment self-check is this phase's content. Every row names the check that backs it, and
there are no other rows: a row nothing can falsify manufactures confidence, so a dimension with
no check behind it is not asserted here at all. The `RESULT` row is written by the orchestrator
from the analyze report. Never split — level-1 only.

```
ALIGN-FE — MDL v1
row           backing check   assertion
SCREENS       orphans         every SCR is referenced by a plan block — SCR-MDL-001 and
                              SCR-MDL-002 each carry a SUB in F1, F2, F3 and F4 and a block in
                              SEC-FE
COMPOSITION   screen-composition  every SCR names where its secondary detail sits and that it
                              saves once — SCR-MDL-001: none inline, a summary row per value and
                              its editor as a second level, one submit per open surface;
                              SCR-MDL-002: none, and it submits nothing
UXD           orphans         every UXD is cited by a plan block — UXD-MDL-001 is cited by both
                              F2 SCREEN-INIT blocks, by both facades, by the F3 validator for
                              RULE-MDL-001, by both F4 Cross-module lines and by SEC-FE
TRACES        traces          every PHASE and SUB carries traces=; UXD-MDL-001 traces to its REQ
                              and its AC; every SCR traces to its REQ and its UXD
API           traces          every API this plan cites is defined in the fetched api-docs —
                              API-MDL-001..011, each carrying its own Contract ID line there
                              (ADR-MDL-011) — and never in the backend plan's contract summary.
                              No foreign module's API id is cited here at all
FOREIGN       xref-surface    every reference to another module's surface resolves in that
                              module's own artifacts. This plan writes no foreign path and no
                              foreign id: the one cross-module read is cited as UXD-MDL-001 and
                              named in ui-ux-spec-mdl.md, so this clause has no subject here
REGISTRY      registry-agree  the UXD and both SCR defined here are in registry-exec-fe-mdl.md,
                              and nothing else is
LANGUAGES     languages       labels and messages in ar + en
MARKERS       markers         the parser reports no structural or semantic error for this track
                              and plan
DECISIONS     refs-exist      every ADR this plan cites exists on disk in
                              analysis/decisions/MDL/ — ADR-MDL-002, ADR-MDL-003, ADR-MDL-004,
                              ADR-MDL-005, ADR-MDL-006, ADR-MDL-007, ADR-MDL-011, ADR-MDL-012,
                              ADR-MDL-013, ADR-MDL-014, ADR-MDL-015, ADR-MDL-016, and the two
                              superseded ones they cite
COVERAGE      (the report)    none — the analyze report lists no clause as having examined
                              nothing. Two facts sit behind that word: every clause that counts
                              its subjects counted at least one here, and `xref-surface` reports
                              no count at all, so it appears in neither the coverage map nor that
                              list. It has no subject in this plan, by the decision the API row
                              above records.
RESULT        PASSED ✓ — 0 findings
```

### Operations coverage

| Operation | API | SCR action | Route | Status |
|---|---|---|---|---|
| search lookup types | API-MDL-001 | SCR-MDL-001 · type search | `/reference-data/lookups` | ✓ |
| create lookup type | API-MDL-002 | SCR-MDL-001 · type entry, create | `/reference-data/lookups/new` | ✓ |
| update lookup type | API-MDL-003 | SCR-MDL-001 · type entry, edit | `/reference-data/lookups/:typeId/edit` | ✓ |
| deactivate lookup type | API-MDL-004 | SCR-MDL-001 · deactivate a type | `/reference-data/lookups/:typeId` | ✓ |
| search a type's values | API-MDL-005 | SCR-MDL-001 · the values pane | `/reference-data/lookups/:typeId` | ✓ |
| create lookup value | API-MDL-006 | SCR-MDL-001 · value entry, create | `/reference-data/lookups/:typeId/values/new` | ✓ |
| update lookup value | API-MDL-007 | SCR-MDL-001 · value entry, edit | `/reference-data/lookups/:typeId/values/:valueId/edit` | ✓ |
| deactivate lookup value | API-MDL-008 | SCR-MDL-001 · deactivate a value | `/reference-data/lookups/:typeId` | ✓ |
| reorder a type's values | API-MDL-009 | SCR-MDL-001 · drag to reorder (disabled while filtered, G3) | `/reference-data/lookups/:typeId` | ✓ |
| browse the registry by owner | API-MDL-010 | SCR-MDL-002 · the browse itself | `/reference-data/type-registry` | ✓ |
| read active values by key | API-MDL-011 | — a consuming module's backend call | — (ADR-MDL-007) | ✗ |
| read one type by id | — none published | hydrated from the search cache, redirected on cold load | — (ADR-MDL-005, ADR-MDL-014) | ✗ |
| read one value by id | — none published | hydrated from the search cache, redirected on cold load | — (ADR-MDL-005, ADR-MDL-014) | ✗ |

Ten of the eleven published endpoints carry a route and a ✓. Three rows carry a ✗ with the ADR
that explains it: one endpoint published for a caller that is not this frontend, and two
operations the SRS names for which nothing is published. No row is a ✗ for want of a decision,
and no row carries an empty route without one.
<!-- PHASE:ALIGN-FE:END -->

---

## Hand-off

The implementer reads the phases in profile order — F1 models, F2 hooks, F3 forms, F4 screens and
routes, SEC-FE guards — takes design intent from `ui-ux-spec-mdl.md`, and takes every request and
response shape from `_inputs/api-docs-mdl.md` at the commit this module version pins, **except**
the two published numbers PF-MDL-002 disputes (`key`/`code` maxLength, `nameAr`/`nameEn`
maxLength), which this plan binds to the db-script instead until that finding is resolved. No
route, component, permission or field that is not traceable to an F-block above is invented: a
gap is an ADR in `analysis/decisions/MDL/`, never an invention.

Two response shapes travel in this module and the difference is load-bearing: one paginated read,
four bare arrays and six single objects. No block may be read through an envelope another block
declares.

The plan and its registry are split by the toolkit into the frontend execution package inside the
shared repo after the `gate:pass-2` verdict, and tagged `mdl-v1`. Nothing is copied anywhere: the
implementer reads it where it was written.
══════════════════════════════════════════════════════════════════
<<<END FILE>>>

<<<FILE: governance-shared/analysis/modules/MDL/P3_2/registry-exec-fe-mdl.md>>>
## REGISTRY — P3.2 — MDL v1
══════════════════════════════════════════════════════════════════
Module : MDL (البيانات المرجعية / Master Data Lookup)   Version : v1   Profile : erp
Source : analysis/modules/MDL/P3_2/ui-ux-spec-mdl.md · frontend-execution-plan-mdl.md
══════════════════════════════════════════════════════════════════

ID RANGES
UXD-MDL-001 .. UXD-MDL-001 · SCR-MDL-001 .. SCR-MDL-002

SCR ids : SCR-MDL-001, SCR-MDL-002
UXD ids : UXD-MDL-001
Last sequence per atom : SCR 002 · UXD 001

Neither sequence restarts and nothing is renumbered: both are the assignment this module version
already carries, cited unchanged by the decisions on disk (ADR-MDL-003, ADR-MDL-004, ADR-MDL-007)
and by every downstream artifact that names them. This run mints no new `SCR-*` or `UXD-*` id —
the two screens the SRS declares and the one cross-module display dependency are the same two and
the same one; it mints one new ADR id (ADR-MDL-016) against this same unchanged set.

SCREENS
| SCR | الاسم / Name | Owning ENT | Container pattern | Permissions |
|---|---|---|---|---|
| SCR-MDL-001 | اللوكبات العامة / Generic Lookups | ENT-MDL-001 (+ ENT-MDL-002) | TREE_MASTER_DETAIL | PERM_MDL_LOOKUPS_VIEW (gateway), PERM_MDL_LOOKUPS_CREATE, PERM_MDL_LOOKUPS_UPDATE, PERM_MDL_LOOKUPS_DELETE |
| SCR-MDL-002 | سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner | ENT-MDL-001 | FULL_PAGE — no entry sub-view (ADR-MDL-003) | PERM_MDL_TYPE_REGISTRY_VIEW |

Two composite screens, two page codes (MDL_LOOKUPS, MDL_TYPE_REGISTRY), and one `SUB` per screen
in each of the four sub-bearing phases — eight per-screen blocks, plus a level-1 block per screen
in SEC-FE. The permission names are the backend registry's; this stage cites them and declares
none. **PF-MDL-001** (frontend-execution-plan-mdl.md, API SURFACE) records that the api-docs
require `PERM_MDL_LOOKUPS_UPDATE`, not `PERM_MDL_LOOKUPS_DELETE`, on both deactivate endpoints —
filed against the backend track, not corrected in this table, which states the SRS §B4 grant as
written.

COMPOSITION
| SCR | Secondary detail | Placement | Submits |
|---|---|---|---|
| SCR-MDL-001 | the selected type's values — records of a second entity, absent from every type write DTO | none inline · a summary row per value, its editor a second level opened from the route (with a cold-load redirect when the hydrating row is not in cache, ADR-MDL-014), rendered as a sibling and carrying no scroll region of its own | one per open surface, one surface at a time; no action of this screen owns two calls; the reorder submits only the type's complete, unfiltered value set (G3) |
| SCR-MDL-002 | none — a read-only browse | none | none — the screen writes nothing |

UXD INDEX
| UXD | Screen(s) | Field | Owner module · API used |
|---|---|---|---|
| UXD-MDL-001 | SCR-MDL-001, SCR-MDL-002 | `ownerModuleCode` — the type's owning module | SEC · `ModuleRegistry` (ENT-SEC-004), read through the security module's registry search, named in ui-ux-spec-mdl.md and cited by no id inside the frontend plan (ADR-MDL-004, ADR-MDL-011) |

One `UXD-*` for the whole module, and the same shared, long-lived hook serves both screens and
every control on them. It is **not** a lookup dependency: SRS §A6 records that MDL introduces no
coded list of its own, and the owner-module field's valid set is another module's registry data.
It is not the backend's cross-module record either: that one is the server-side existence check
behind RULE-MDL-001 and appears in no frontend artifact.
Grant that travels with it: every role granted `PERM_MDL_LOOKUPS_CREATE` must also hold
`PERM_SEC_MODULE_REGISTRY_VIEW`, or the owner-module select stays empty and disabled and creation
is blocked behind it. The grant is the security module's to make (ADR-MDL-013).
Degraded source, one per control, not one per screen: the create-form select on SCR-MDL-001
degrades to empty-and-disabled (ADR-MDL-013) — the only one of the three controls where a
submitted value could reach the server unvalidated. Both read-only filters degrade instead to the
distinct `ownerModuleCode` values already present in their own screen's current search response —
SCR-MDL-001's master-list filter falls back to the current API-MDL-001 response (ADR-MDL-016, new
this run, G1), and SCR-MDL-002's registry filter falls back to the current API-MDL-010 response
(ADR-MDL-015, G6) — never to free text, and never blocking browsing.

API COVERAGE
| Status | Count | API ids |
|---|---|---|
| used by this frontend | 10 | API-MDL-001 … API-MDL-010 |
| documented, deliberately uncalled | 1 | API-MDL-011 — the consumer read a consuming module's backend performs over the platform's in-process interface; bound in F2 and drawn on no screen (ADR-MDL-007) |
| used but undocumented | 0 | no endpoint is called that the api-docs do not publish |
| documented but unbound | 0 | all 11 published endpoints are bound, each cited by the `Contract ID` the api-docs now publish for it (ADR-MDL-011, superseding ADR-MDL-008) |

RESPONSE SHAPES — the difference is load-bearing and is stated per block in F2 — corrected in a
prior run (G2): API-MDL-005 moved from paginated to bare array. An earlier revision modelled it as
paginated on no published source; `_inputs/api-docs-mdl.md`, backend-execution-plan API-MDL-005
and QR-MDL-005 (`Pagination: NO`) and SRS §B2 all agree it is unpaged.

| Shape | API ids |
|---|---|
| paginated (`PageLookup<T>`) | API-MDL-001 |
| a bare array | API-MDL-005, API-MDL-009, API-MDL-010, API-MDL-011 |
| a single object | API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-006, API-MDL-007, API-MDL-008 |
Only API-MDL-001 carries page and size in its cache key. API-MDL-005 and API-MDL-010 accept
`filters` alone — no sort and no paging — so neither belongs in either one's key.

SHAPE DIFFS AGAINST THE BACKEND PLAN
Three reads are `POST …/search` where `backend-execution-plan-mdl.md`'s contract summary still
predicts `GET`: API-MDL-001, API-MDL-005, API-MDL-010. The api-docs and the SRS §B5 tables agree
with each other, so the backend plan is the one artifact that lags; `gov.py analyze` reports it
there (C8.4). Correcting it is three rows in a P3.1 artifact, outside this stage's boundary —
recorded here and in ADR-MDL-002, not silently corrected.

FIELD PRECISION DIFF (G1, a prior run) — `key`/`code` and `nameAr`/`nameEn` maxLength is bound to
db-script §1 and ADR-MDL-010 (50 / 200) rather than to any conflicting number
`_inputs/api-docs-mdl.md` may publish (80 / 150); filed as PF-MDL-002 in
frontend-execution-plan-mdl.md against the MDL api-doc generator, with the db-script column
widths as evidence.

OPERATIONS WITHOUT AN ENDPOINT
read one lookup type by id · read one lookup value by id — named by SRS Part B, required by no
`REQ-*`, and omitted from the frontend rather than faked; both entry sub-views hydrate from the
row the search query already holds when it is present, and redirect to the parent surface on a
cold load when it is not (ADR-MDL-005, ADR-MDL-014). The value-create route (`/values/new`) is a
special case of that redirect, not a third kind of hydration: it creates a record and has no row
to hydrate at all, so its cold-load redirect protects the parent type's display context, not a
value record (ADR-MDL-014, corrected this run — G2). This SRS version names no `activate` action
at either level, so deactivation being one-way is the SRS's own statement, not an omission.

LOOKUPS
None. MDL introduces no domain-specific coded list of its own — it is the generic mechanism every
other module's lookup types run on. No lookup hook exists anywhere in the plan, no validator binds
an option set, and no enum is modelled. The one constrained field, `ownerModuleCode`, binds to the
runtime-loaded list of UXD-MDL-001.

ALIGN
Verdict as stamped in frontend-execution-plan-mdl.md → ALIGN-FE → the `RESULT` line (written by
the orchestrator from the analyze report).

Findings fixed in this revision (gate `pass-2`, third round): a shared degraded-source fallback
that left SCR-MDL-001's own master-list `ownerModuleCode` filter undocumented and defaulting, by
the nearest stated text, to the create-form select's empty-and-disabled treatment — even though
that select's failure grant (`PERM_SEC_MODULE_REGISTRY_VIEW`) is tied to CREATE alone, so a
VIEW-only caller was denied a read-only filter their own permissions never gated (this round's
G1, ADR-MDL-016, new); and an overstated cold-load hydration rule that grouped the value-create
route's redirect with the two true edit routes' record-hydration rule, when the create route has
no record to hydrate at all and its redirect protects the parent type's display context instead
(this round's G2, documentation-only, no ADR). The corresponding line in `ADR-MDL-014.md` needs
the same G2 correction and remains open against that file, which this stage's writable set does
not include — carried forward in the same posture G8/G9 already record below.

Findings fixed in earlier revisions of this same gate, kept for the record: the six-way
field-precision divergence from the deployed schema (G1, round 1), the detail read modelled as
paginated on no published source (G2, round 1), a reorder submittable from a partial pane (G3,
round 1), an unbuildable cold-load deep link (G4, round 1, ADR-MDL-014), a cross-artifact
permission divergence settled in prose instead of filed (G5, round 1, PF-MDL-001), a silent
degraded-source gap on the SCR-MDL-002 owner filter (G6, round 1, ADR-MDL-015), the unstated
permanence of a deactivated code/key (G7, round 1), a stale ADR-MDL-008 status header and an
ADR-MDL-005 Context-table citation of a superseded SRS line (G8, G9, round 1 — filed against the
ADR files themselves, outside that pass's writable set), an unstated optimistic-reorder failure
path (G10, round 1), an unconsumed F1 model for the consumer read (G11, round 1), a server-side
reorder invariant gap settled client-side instead of filed (G1, round 2, PF-MDL-004), a backend
test naming the wrong surface and an unconstructible failure mode (G2, round 2, PF-MDL-005), an
undisclosed cross-module registration gap on SEC's side carried as ACTIVE with no outstanding
condition (G3, round 2, PF-MDL-006), the UNIQUE_CHECK line requesting an EQUALS filter the
backend never honours (G6, round 2), and a `sortField` modelled for an ordering the backend
cannot produce (G7, round 2). G4 and G5 of round 2 land outside this stage's writable files
(srs-mdl.md and ADR-MDL-008.md respectively) and remain carried, not applied — the same posture
G8/G9 and this round's ADR-MDL-014 correction already state. No new ADR was raised in round 2;
this round raises exactly one (ADR-MDL-016), because G1 of this round needed a choice among two
live, already-accepted patterns (empty-and-disabled vs. current-response fallback) rather than a
restatement of an existing decision.

ADRs
analysis/decisions/MDL/ADR-MDL-002.md (ACCEPTED — three reads are POST `…/search`; the backend
contract summary is what lags) ·
analysis/decisions/MDL/ADR-MDL-003.md (ACCEPTED — container pattern for the read-only browse) ·
analysis/decisions/MDL/ADR-MDL-004.md (ACCEPTED — the owner-module field reads the security
module's registry; UXD-MDL-001) ·
analysis/decisions/MDL/ADR-MDL-005.md (ACCEPTED — operations with no published endpoint are
omitted, not faked) ·
analysis/decisions/MDL/ADR-MDL-006.md (ACCEPTED — `isActiveFl` read-only at both levels) ·
analysis/decisions/MDL/ADR-MDL-007.md (ACCEPTED — API-MDL-011 bound and called by no screen) ·
analysis/decisions/MDL/ADR-MDL-011.md (ACCEPTED — the re-fetched api-docs publishes a Contract ID
per endpoint, so the plan cites API ids; supersedes ADR-MDL-008) ·
analysis/decisions/MDL/ADR-MDL-012.md (ACCEPTED — no published surface tells a screen which
actions its caller holds) ·
analysis/decisions/MDL/ADR-MDL-013.md (ACCEPTED — the grant that travels with UXD-MDL-001's
create-form select, and that select's own degraded-source behaviour) ·
analysis/decisions/MDL/ADR-MDL-014.md (ACCEPTED — cold-load redirect for the deep-linked entry
routes; its own text still owes the G2 correction this round applies in
frontend-execution-plan-mdl.md, carried forward against the file itself) ·
analysis/decisions/MDL/ADR-MDL-015.md (ACCEPTED — the SCR-MDL-002 degraded-source fallback for
UXD-MDL-001's registry filter, G6) ·
analysis/decisions/MDL/ADR-MDL-016.md (ACCEPTED, raised this run — SCR-MDL-001's own master-list
owner-module filter falls back to the current search response's distinct values rather than
sharing the create-form select's empty-and-disabled behaviour, G1)
Superseded, kept on disk and cited only by the decisions that replaced them: ADR-MDL-001,
ADR-MDL-008 — the latter's own status header remains to be corrected on disk (G8, outside this
pass's writable files). Carried from earlier stages: ADR-MDL-009 and ADR-MDL-010 (P2) touch no
frontend artifact and are not applied here. No BLOCKED ADR — the pass was not stopped, and no
question was raised.

TRACEABILITY
REQ reached by ≥1 F-block: 13/13 — REQ-MDL-001 … REQ-MDL-010 appear in the `traces=` of
SCR-MDL-001's blocks, and REQ-MDL-011, REQ-MDL-012, REQ-MDL-013 in SCR-MDL-002's. Orphan REQ:
none.
REQ traced by an `SCR-*` or `UXD-*` record: **11/13**, and the two numbers are not in conflict.
REQ-MDL-011 and REQ-MDL-012 are the consumer's read by key — a server-to-server call with no
screen (ADR-MDL-007) — so they are reached by the F2 block that binds API-MDL-011 (and by that
block alone — no F1 model duplicates it, G11) and by the values and active flags managed on
SCR-MDL-001 that decide what that read returns, and they are traced by no screen, because no
screen implements them. The analyze report's `req-ux` ratio says 84.6% for exactly this reason;
inflating it would mean asserting a screen that does not exist.
AC covered: 13/13 — each AC accompanies its REQ in the same traces.
SCR covered: 2/2 — each carries a SUB in F1, F2, F3 and F4 and a block in SEC-FE.
UXD cited by an F-block: 1 of 1 — none unreferenced, none dangling.
Screen operations the SRS names, resolved to a route: 8/8 on SCR-MDL-001 (search, create, read,
update, deactivate, reorder across both levels) and 2/2 on SCR-MDL-002 (search, browse).

PLATFORM FINDINGS (frontend track, this run)
| id | status | subject |
|---|---|---|
| PF-MDL-001 | OPEN | deactivate-endpoint permission (UPDATE vs DELETE) divergence — G5, round 1 |
| PF-MDL-002 | OPEN (conditional) | api-docs field-precision divergence from the db-script — G1, round 1 |
| PF-MDL-003 | OPEN | no by-id read published for either entity, blocking cold-load hydration — G4, round 1 |
| PF-MDL-004 | OPEN | QR-MDL-009 checks only type membership per id, never that the submitted set is the type's complete, non-duplicated value set — G1, round 2, owner MDL backend track (P3.1) |
| PF-MDL-005 | OPEN | TC-MDL-014 names the wrong surface (API-SEC-021, a frontend HTTP read) for an unconstructible in-process failure mode — G2, round 2, owner MDL backend track (P3.1) |
| PF-MDL-006 | OPEN | SEC's own P3.1 artifacts register only `SecUserDirectoryApi`, leaving XM-MDL-001's module-registry read unregistered on SEC's side while the XM is carried ACTIVE — G3, round 2, owner SEC / P3.1 track |
No new platform-finding row is filed this round: G1 and G2 of this round are both corrected
within this stage's own writable files (frontend-execution-plan-mdl.md, ui-ux-spec-mdl.md), with
G2's mirror correction in `ADR-MDL-014.md` carried forward rather than filed as a platform
finding, since it names no other track as owner — it is this same track's own file, just outside
this pass's writable set.
Full text of each in frontend-execution-plan-mdl.md, API SURFACE.

Event
"P3.2 revised (gate pass-2, third round): MDL v1 — 2 screens, 1 UXD, 11/11 API bound (10
called), 1 new ADR (ADR-MDL-016), 0 new platform-findings rows filed, 2 findings (G1, G2 of this
round) applied — G2's ADR-MDL-014 mirror correction lands outside this stage's writable files and
is carried forward, alongside G4/G5 of round 2 and G8/G9 of round 1"
══════════════════════════════════════════════════════════════════
<<<END FILE>>>