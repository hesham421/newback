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

**لا إنشاء لقيمة تحت نوع معطَّل / no value is created under an inactive type** (ADR-MDL-042).
إضافة قيمة معطَّلة عندما يكون النوع المختار غير فعّال، ويذكر السبب بنصّ RULE-MDL-004 نفسه —
ar: «هذا النوع معطّل حاليًا» · en: "This lookup type is currently inactive". القيمة المنشأة تحت
نوع معطَّل لا يمكن الوصول إليها أبدًا — لا إجراء لإعادة التفعيل (ADR-MDL-005)، وRULE-MDL-004 تستبعد
قيم النوع المعطَّل من كل قراءة استهلاكية — وتحجز رمزها تحت ذلك النوع بلا استرجاع. تحرير القيم
القائمة وإعادة ترتيبها يبقيان متاحين، فهما يصونان صفوفًا يضمن AC-MDL-004 بقاءها بعد التعطيل.
الحارس هنا نصف العميل فقط؛ النصف الخادمي مسجَّل في PF-MDL-007.

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
