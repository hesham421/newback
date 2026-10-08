# SRS — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp
Inputs : prd, domain-profile, project-registry (PRD approved 2026-09-10 — gate `prd-approval`)
Counts : REQ 13 · AC 13 · ENT 2 · RULE 4 · SCR-REQ 2 · ADR 0
══════════════════════════════════════════════════════════════════

# PART A — MODULE FOUNDATION

## A1 — Document information

| Item | Value |
|---|---|
| الوحدة / Module | البيانات المرجعية / Master Data Lookup |
| رمز الوحدة / Module code | MDL |
| السياق المحدود / Bounded context | organization |
| النسخة / Version | v1 |
| التاريخ / Date | 2026-09-19 |
| الحالة / Status | COMPLETE — pass P1, round 1 |
| أعدّها / Prepared by | P1 (SRS engine), lane `analysis` |
| القرارات المطبَّقة / Decisions applied | 0 ADR + 6 DEFAULT + سابقتان منصّيتان (see STANDALONE) |
| المدخلات / Inputs | prd-mdl.md (APPROVED), domain-profile.md v1, project-registry.md v1.3.0, module-registry-mdl.md, business-policies-mdl.md |

## A2 — Functional context

**داخل النطاق / In scope**
كيانان اثنان (نوع اللوكب وقيمته) · تسجيل نوع لوكب باسم وحدة مالكة · إدارة قيم أي نوع عبر
شاشة عامة واحدة رئيسي-تفصيلي · قراءة القيم الفعّالة بالمفتاح من أي وحدة مستهلكة ·
تصفّح سجل الأنواع مجمّعًا حسب الوحدة المالكة · التعطيل الناعم على المستويين.

**خارج النطاق / Out of scope**
شاشة مخصَّصة لقائمة بعينها (POL-MDL-004) · قيم لوكب هرمية أو شجرية — القائمة مسطّحة
(business-policies SCOPE EXCEPTIONS) · صلاحية دقيقة لكل نوع لوكب على حدة، فالحدّ هو منح
الشاشة (business-policies SCOPE EXCEPTIONS) · قائمة قيم مُرمَّزة تملكها MDL لنفسها —
الوحدة هي الآلية لا صاحبة القوائم (module-registry-mdl.md LOOKUPS OWNED) · إعادة تفعيل
نوع أو قيمة معطَّلة: لا قصة معتمدة تطلبها · محرك سير عمل (domain-profile G12).

**وظيفة الوحدة / Module function**
تُعطي الوحدة المنصّةَ مخزنًا واحدًا لكل قائمة قيم مُرمَّزة: تُسجِّل كل وحدة أنواعها هنا
كبيانات لا كشيفرة، وتبقى الملكية الدلالية للوحدة المسجِّلة عبر رمزها المخزَّن مع النوع،
ويقرأ الجميع القيم الفعّالة بمفتاح النوع. إضافة قائمة جديدة لا تمسّ شيفرة MDL ولا تُنشئ
شاشة جديدة (POL-MDL-001, POL-MDL-002, POL-MDL-004).

**الوصف التفصيلي / Detailed description**
مُكامِل الوحدة يُسجّل نوع لوكب بمفتاح فريد على مستوى المنصة، ورمز وحدته المالكة، واسمين
عربي وإنجليزي؛ يرفض النظام التسجيل إن لم يكن للرمز صف في سجل وحدات الأمان
(POL-MDL-003). منسّق المنصة — أو مَن مُنح الشاشة من مديري قوائم الوحدة المالكة — يفتح
الشاشة العامة، يختار النوع من القائمة الرئيسية، فتُعرض قيمه في الجزء التفصيلي: يضيف قيمة
برمز وتسميتين ورتبة، يُنقّح تسمياتها ورتبتها، يُعيد ترتيب القيم، يُعطّل ما لم يعد مطلوبًا.
الوحدة المستهلكة لا تفتح شاشة إطلاقًا: تطلب قيم النوع بمفتاحه فتصلها القيم الفعّالة وحدها
مرتَّبة، وإن كان النوع نفسه معطَّلًا لم تصلها قيمة. الأدوار ثلاثة: منسّق المنصة، مدير قوائم
الوحدة المالكة، والحساب الخدمي للوحدة المستهلكة الذي لا يملك غير القراءة.

**الوضع الحالي / Current situation**
| الخطوة | الجهة | ملاحظة |
|---|---|---|
| تعريف قائمة قيم مُرمَّزة | كل وحدة على حدة | جدول lookup محلي أو ثابت مكتوب في الشيفرة |
| تعديل قيمة | مطوّر الوحدة | تغيير شيفرة ونشر، لا تغيير بيانات |
| مشاركة القائمة مع وحدة أخرى | نسخ يدوي | نسختان تفترقان مع أول تعديل |

**الصعوبات الحالية / Current difficulties**
انحراف القيم المرجعية بين الوحدات (reference-data drift): القيمة الواحدة لها تمثيلان
متضاربان أو أكثر، وكل قائمة جديدة تكلّف شاشة وشيفرة، ولا موضع واحد يُسأل عن المعنى.

**النظام المقترح ومنافعه / Proposed system and benefits**
مركز واحد على نمط MDM hub (domain-profile §9 R2): تخزين وشاشة مركزيان، وملكية دلالية
موزّعة بالتسمية (namespacing) على الوحدة المسجِّلة (G4, G5). إضافة قائمة صارت تسجيل
بيانات، والقيمة الواحدة صار لها مصدر واحد موثوق، والقراءة موحّدة بمفتاح واحد لكل مستهلك.

**ملاحظات عامة / General notes**
لا ترقيم مستندات: لا كيان من كياني الوحدة مستندٌ مُرقَّم ولا يُستخدم معرّفه خارج النظام
(§3.3 NUMBERING) · التعطيل ناعم بـ`isActiveFl` ولا حذف نهائي [KB:erp-domain-standards §6] ·
`key` في نوع اللوكب يؤدّي دور `code` في حقول النوع `master` القياسية (A3) · الوحدة لا
تستهلك من MDL شيئًا، فهي MDL نفسها.

## A3 — Entities and fields

**الحقول القياسية لكل نوع كيان (من الملف الشخصي) / Standard fields per entity kind**

| Kind | Default fields |
|---|---|
| master | nameAr, nameEn, code, isActiveFl, createdBy, createdAt, updatedBy, updatedAt |
| transactional | docNo, docDate, statusCode, fiscalYearId, periodId, createdBy, createdAt, updatedBy, updatedAt |
| lookup | code, nameAr, nameEn, sortOrder, isActiveFl |
| config | key, valueAr, valueEn, isActiveFl |

تُكتب مرة واحدة هنا ولا تُعاد لكل كيان. المفتاح الأساسي `{entity}Pk`؛ حقول العلم تنتهي بـ`Fl`؛
حقول التدقيق `createdBy, createdAt, updatedBy, updatedAt` يملؤها النظام ولا تُقبل من العميل
أبدًا. الأنواع هنا منطقية فقط — الأنواع الفيزيائية من إنتاج P2.

### ENT-MDL-001 — نوع اللوكب / LookupType

| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| master | SHARED (owner) — كل وحدة تُسجّل أنواعها هنا وتقرأها (project-registry SHARED ENTITY DECLARATIONS) | **لا / no** — المعرّف لا يُستخدم خارج النظام، ولا سياسة ولا قصة تطلب مرجعًا مقروءًا، وليس مستندًا مُرقَّمًا (§3.3 NUMBERING)؛ `key` مفتاح عمل يكتبه المُسجِّل لا رقم يولّده النظام | create · read · search · update · deactivate | `ownerModuleCode` يُتحقَّق من وجوده في سجل وحدات الأمان ENT-SEC-004 (SOFT-READ، بلا مفتاح أجنبي — A8) | module-registry-mdl.md ENTITIES OWNED; POL-MDL-002, POL-MDL-003 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| lookupTypePk | reference | yes | مولَّد من النظام / system-generated | المفتاح الأساسي، لا يُعرض كمرجع عمل | معرّف نوع اللوكب | LookupType id |
| key | text | yes | يكتبه المُسجِّل / supplied by the registrar | فريد على مستوى المنصة، وغير قابل للتعديل بعد الإنشاء — RULE-MDL-003؛ هو عقد القراءة لكل مستهلك (REQ-MDL-011) | المفتاح | Key |
| ownerModuleCode | text | yes | رمز وحدة مسجَّلة في ENT-SEC-004 / a module code registered in SEC | إلزامي عند الإنشاء — RULE-MDL-001؛ لا يُعدَّل بعد الإنشاء | رمز الوحدة المالكة | Owner module code |
| nameAr | text | yes | يكتبه المُسجِّل | الاسم العربي للنوع | الاسم (عربي) | Name (Arabic) |
| nameEn | text | yes | يكتبه المُسجِّل | الاسم الإنجليزي للنوع | الاسم (إنجليزي) | Name (English) |
| isActiveFl | flag | yes | `true` عند الإنشاء / `true` on creation | التعطيل الناعم — RULE-MDL-004 | نشط | Active |
| createdBy | text | yes | نظام / system | معرّف الأصل المُصادَق | أنشأه | Created by |
| createdAt | date-time | yes | نظام / system | UTC مخزَّنة، تُعرض بتوقيت المستأجر [KB:erp-domain-standards §6] | تاريخ الإنشاء | Created at |
| updatedBy | text | no | نظام / system | يُملأ عند أول تعديل | عدّله | Updated by |
| updatedAt | date-time | no | نظام / system | يُملأ عند أول تعديل | تاريخ التعديل | Updated at |

**ملاحظة على حقول `master` القياسية / note on the master default set** — الكيان يحمل
`nameAr`، `nameEn`، `isActiveFl` وحقول التدقيق الأربعة كاملةً؛ ودور `code` يؤدّيه `key`
باسمه الوارد في مفردات الوحدة (domain-profile §7.1 "نوع اللوكب") وفي module-registry-mdl.md،
لا حقلًا ثانيًا باسم آخر. و`ownerModuleCode` إضافة فوق المجموعة القياسية تفرضها قاعدة
التسمية بالمالك (POL-MDL-002).

### ENT-MDL-002 — قيمة اللوكب / LookupValue

| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| lookup | SHARED (owner) — كل وحدة تقرأ قيمها الفعّالة هنا (project-registry SHARED ENTITY DECLARATIONS) | **لا / no** — القيمة تُعرَّف برمزها ضمن نوعها، ولا مرجع لها خارج النظام (§3.3 NUMBERING) | create · read · search · update · deactivate · reorder | لا شيء — الكيان داخلي التبعية، يرتبط بنوعه داخل الوحدة | module-registry-mdl.md ENTITIES OWNED; POL-MDL-005, POL-MDL-006 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| lookupValuePk | reference | yes | مولَّد من النظام / system-generated | المفتاح الأساسي | معرّف القيمة | LookupValue id |
| lookupTypeId | reference | yes | ENT-MDL-001 | النوع الذي تنتمي إليه القيمة — يُحدَّد باختيار النوع لا بكتابته | نوع اللوكب | Lookup type |
| code | text | yes | يكتبه المستخدم / supplied by the user | فريد ضمن النوع الواحد — RULE-MDL-002؛ هو ما تخزّنه الوحدات المستهلكة عندها | الرمز | Code |
| nameAr | text | yes | يكتبه المستخدم | التسمية العربية المعروضة | الاسم (عربي) | Name (Arabic) |
| nameEn | text | yes | يكتبه المستخدم | التسمية الإنجليزية المعروضة | الاسم (إنجليزي) | Name (English) |
| sortOrder | number | yes | `0` افتراضًا / `0` by default | ترتيب العرض تصاعديًا — REQ-MDL-010 | الترتيب | Sort order |
| isActiveFl | flag | yes | `true` عند الإنشاء / `true` on creation | التعطيل الناعم؛ القيمة المعطَّلة تغيب عن قراءة المستهلك (REQ-MDL-011) | نشط | Active |
| createdBy | text | yes | نظام / system | معرّف الأصل المُصادَق | أنشأها | Created by |
| createdAt | date-time | yes | نظام / system | UTC مخزَّنة [KB:erp-domain-standards §6] | تاريخ الإنشاء | Created at |
| updatedBy | text | no | نظام / system | يُملأ عند أول تعديل | عدّلها | Updated by |
| updatedAt | date-time | no | نظام / system | يُملأ عند أول تعديل | تاريخ التعديل | Updated at |

**إضافة فوق حقول `lookup` القياسية / addition beyond the lookup default set** — حقول
التدقيق الأربعة محمولة على هذا الكيان أيضًا رغم غيابها عن مجموعة النوع `lookup`
القياسية: القيمة هنا بيانات يحرّرها مستخدم عبر شاشة، لا صفٌّ ثابت يُزرع مرة واحدة،
فأثر التدقيق واجب عليها [KB:erp-domain-standards §6 audit trail].

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-MDL-001 — تسجيل نوع لوكب / Register a lookup type
  Pattern    : event
  Statement  : When a registrar submits a new lookup type carrying a key, an owner module code and Arabic and English names, the system shall store the lookup type with that key unique across the platform and shall mark it active.
  بالعربية   : عند إرسال المُسجِّل نوع لوكب جديد يحمل مفتاحًا ورمز وحدة مالكة واسمين عربي وإنجليزي، يخزّن النظام النوع بمفتاح فريد على مستوى المنصة ويجعله فعّالًا.
  Traces     : US-MDL-001, US-MDL-004
  Entities   : ENT-MDL-001
  Rationale  : إضافة قائمة جديدة تسجيلُ بيانات لا تعديلُ شيفرة (POL-MDL-001, POL-MDL-002)
  Source     : US-MDL-001; US-MDL-004; POL-MDL-002; lookup-module-plan-en.md §3-§4
  Priority   : HIGH

#### AC-MDL-001 — المسار السعيد لتسجيل النوع / register a type, happy path
  Traces : REQ-MDL-001
  Given  : منسّق ممنوح إجراء CREATE على شاشة اللوكبات العامة، ولا نوع على المنصة بالمفتاح `PAYMENT_METHOD`
  When   : يحفظ نوعًا مفتاحه `PAYMENT_METHOD` ووحدته المالكة `FIN` واسماه «طريقة الدفع» / "Payment method"
  Then   : يُخزَّن النوع بـ`key = PAYMENT_METHOD` و`ownerModuleCode = FIN` و`isActiveFl = true`، ويحمل `createdBy` و`createdAt` من النظام، وتظهر رسالة — ar: «تم حفظ نوع اللوكب.» · en: "The lookup type has been saved."

### REQ-MDL-002 — رفض وحدة مالكة غير مسجَّلة / Reject an unregistered owner module
  Pattern    : unwanted
  Statement  : If a lookup type registration names an owner module code that has no module registry row in the security module, then the system shall reject the registration and shall store no lookup type.
  بالعربية   : إذا سمّى تسجيلُ نوع لوكب رمزَ وحدة مالكة لا صفّ لها في سجل وحدات الأمان، فيرفض النظام التسجيل ولا يخزّن أي نوع.
  Traces     : US-MDL-001, US-MDL-004
  Entities   : ENT-MDL-001
  Rationale  : سلامة مرجعية — لا نوع بلا مالك حقيقي (POL-MDL-003)، وبغير الفحص تصير التسمية بالمالك حبرًا
  Source     : US-MDL-001; US-MDL-004; POL-MDL-003; module-registry-mdl.md SHARED ENTITIES CONSUMED
  Priority   : HIGH

#### AC-MDL-002 — رمز وحدة غير مسجَّل / an unregistered module code
  Traces : REQ-MDL-002
  Given  : منسّق في نموذج نوع جديد، والرمز `XYZ` بلا صفّ في سجل وحدات الأمان (ENT-SEC-004)
  When   : يحفظ نوعًا مفتاحه `SHIPPING_MODE` ووحدته المالكة `XYZ`
  Then   : يُرفض الحفظ برسالة RULE-MDL-001 — ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» · en: "The owning module is not registered in the Security module" — ولا يُخزَّن صفّ نوع البتة

### REQ-MDL-003 — إعادة تسمية نوع لوكب / Rename a lookup type
  Pattern    : event
  Statement  : When a registrar submits revised Arabic and English names for an existing lookup type, the system shall update that type's stored names with the submitted ones.
  بالعربية   : عند إرسال المُسجِّل اسمين عربي وإنجليزي مُنقّحين لنوع لوكب قائم، يحدّث النظام اسمَي النوع المخزَّنين بالمُرسَلين.
  Traces     : US-MDL-001
  Entities   : ENT-MDL-001
  Rationale  : التسمية تتغيّر، والمفتاح عقدٌ لا يتغيّر (RULE-MDL-003)
  Source     : US-MDL-001; lookup-module-plan-en.md §3
  Priority   : HIGH

#### AC-MDL-003 — تعديل الاسمين دون المفتاح / names change, key does not
  Traces : REQ-MDL-003
  Given  : نوع لوكب قائم مفتاحه `PAYMENT_METHOD` واسماه «طريقة الدفع» / "Payment method"
  When   : يُرسل المنسّق الاسمين «وسيلة الدفع» / "Payment means"
  Then   : يُحدَّث `nameAr` و`nameEn` بالقيمتين المُرسلتين، ويبقى `key = PAYMENT_METHOD` كما هو (RULE-MDL-003)، ويُملأ `updatedBy` و`updatedAt`

### REQ-MDL-004 — تعطيل نوع لوكب / Deactivate a lookup type
  Pattern    : event
  Statement  : When a registrar deactivates a lookup type, the system shall mark that lookup type inactive.
  بالعربية   : عند تعطيل المُسجِّل نوعَ لوكب، يجعل النظام النوع غير فعّال.
  Traces     : US-MDL-001
  Entities   : ENT-MDL-001
  Rationale  : إخراج قائمة من الخدمة تعطيلٌ ناعم لا محو — الصفوف تبقى وأثر الاستهلاك محفوظ
  Source     : US-MDL-001; DEFAULT [KB:erp-domain-standards §6 soft delete]
  Priority   : HIGH

#### AC-MDL-004 — أثر تعطيل النوع / the effect of deactivating a type
  Traces : REQ-MDL-004
  Given  : نوع فعّال مفتاحه `PAYMENT_METHOD` له ثلاث قيم فعّالة
  When   : يُعطّله المنسّق
  Then   : يصير `isActiveFl = false`، وتبقى قيمه الثلاث مخزَّنة كما هي، ولا تُعاد أيٌّ منها في قراءة المستهلك (RULE-MDL-004)

### REQ-MDL-005 — عرض قيم النوع المختار / List the selected type's values
  Pattern    : event
  Statement  : When a user selects a lookup type on the generic lookup screen, the system shall list that type's values ordered ascending by sort order.
  بالعربية   : عند اختيار المستخدم نوعَ لوكب على الشاشة العامة، يعرض النظام قيم ذلك النوع مرتَّبةً تصاعديًا بالترتيب.
  Traces     : US-MDL-002
  Entities   : ENT-MDL-001, ENT-MDL-002
  Rationale  : الشاشة العامة رئيسي-تفصيلي: اختيار النوع هو ما يحصر التفصيل (POL-MDL-004)
  Source     : US-MDL-002; POL-MDL-004; lookup-module-plan-en.md §3
  Priority   : HIGH

#### AC-MDL-005 — التفصيل محصور بالنوع المختار / the detail is confined to the selected type
  Traces : REQ-MDL-005
  Given  : النوع `PAYMENT_METHOD` له أربع قيم برُتب 1 و2 و3 و4، إحداها معطَّلة، ونوع آخر له قيمتان
  When   : يختار المستخدم `PAYMENT_METHOD` في القائمة الرئيسية للشاشة
  Then   : تُعرض قيم `PAYMENT_METHOD` الأربع كلها — الفعّالة والمعطَّلة — مرتَّبة تصاعديًا بـ`sortOrder`، ولا تظهر قيمة من النوع الآخر

### REQ-MDL-006 — إضافة قيمة لوكب / Add a lookup value
  Pattern    : event
  Statement  : When a user submits a new value under a selected lookup type carrying a code, Arabic and English labels and a sort order, the system shall store the value under that type and shall mark it active.
  بالعربية   : عند إرسال المستخدم قيمةً جديدة تحت نوع مختار تحمل رمزًا وتسميتين عربية وإنجليزية ورتبةً، يخزّن النظام القيمة تحت ذلك النوع ويجعلها فعّالة.
  Traces     : US-MDL-002
  Entities   : ENT-MDL-002
  Rationale  : الحقول القياسية لقيمة اللوكب منصوص عليها (POL-MDL-005)
  Source     : US-MDL-002; POL-MDL-005; lookup-module-plan-en.md §3
  Priority   : HIGH

#### AC-MDL-006 — المسار السعيد لإضافة قيمة / add a value, happy path
  Traces : REQ-MDL-006
  Given  : النوع المختار `PAYMENT_METHOD` لا يحمل قيمة رمزها `CASH`
  When   : يحفظ المستخدم قيمة رمزها `CASH` وتسميتاها «نقدًا» / "Cash" ورتبتها 1
  Then   : تُخزَّن القيمة تحت `PAYMENT_METHOD` بـ`code = CASH` و`sortOrder = 1` و`isActiveFl = true`، وتظهر رسالة — ar: «تم حفظ القيمة.» · en: "The value has been saved."

### REQ-MDL-007 — رفض رمز مكرَّر ضمن النوع / Reject a duplicate code within a type
  Pattern    : unwanted
  Statement  : If a submitted lookup value carries a code that another value already carries under the same lookup type, then the system shall reject the value and shall store nothing.
  بالعربية   : إذا حملت قيمةٌ مُرسَلة رمزًا تحمله قيمة أخرى تحت نوع اللوكب نفسه، فيرفض النظام القيمة ولا يخزّن شيئًا.
  Traces     : US-MDL-002
  Entities   : ENT-MDL-002
  Rationale  : قيمة موثوقة واحدة لكل مفهوم مُرمَّز — لا تضارب (POL-MDL-006)
  Source     : US-MDL-002; POL-MDL-006; lookup-module-plan-en.md §6
  Priority   : HIGH

#### AC-MDL-007 — رمز مكرَّر تحت النوع نفسه / a duplicate code under the same type
  Traces : REQ-MDL-007
  Given  : النوع `PAYMENT_METHOD` يحمل قيمة رمزها `CASH`
  When   : يحفظ المستخدم قيمة ثانية رمزها `CASH` تحت `PAYMENT_METHOD`
  Then   : يُرفض الحفظ برسالة RULE-MDL-002 — ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» · en: "This code is already used within this type" — ويبقى عدد قيم النوع كما كان قبل الطلب

### REQ-MDL-008 — تعديل قيمة لوكب / Update a lookup value
  Pattern    : event
  Statement  : When a user submits revised Arabic and English labels and a revised sort order for an existing lookup value, the system shall update that value's stored labels and sort order with the submitted ones.
  بالعربية   : عند إرسال المستخدم تسميتين مُنقّحتين ورتبةً مُنقّحة لقيمة لوكب قائمة، يحدّث النظام تسميتَي القيمة ورتبتها المخزَّنة بالمُرسَلة.
  Traces     : US-MDL-002
  Entities   : ENT-MDL-002
  Rationale  : التسمية والرتبة تتغيّران، والرمز عقدُ القيمة عند مستهلكيها فلا يتغيّر
  Source     : US-MDL-002; POL-MDL-005
  Priority   : HIGH

#### AC-MDL-008 — تعديل التسميات دون الرمز / labels change, code does not
  Traces : REQ-MDL-008
  Given  : قيمة قائمة تحت `PAYMENT_METHOD` رمزها `CASH` ورتبتها 1
  When   : يُرسل المستخدم التسميتين «نقد» / "Cash payment" والرتبة 2
  Then   : تُحدَّث `nameAr` و`nameEn` و`sortOrder = 2` بالقيم المُرسَلة، ويبقى `code = CASH` كما هو، ويُملأ `updatedBy` و`updatedAt`

### REQ-MDL-009 — تعطيل قيمة لوكب / Deactivate a lookup value
  Pattern    : event
  Statement  : When a user deactivates a lookup value, the system shall mark that value inactive.
  بالعربية   : عند تعطيل المستخدم قيمةَ لوكب، يجعل النظام القيمة غير فعّالة.
  Traces     : US-MDL-002
  Entities   : ENT-MDL-002
  Rationale  : سحب قيمة من التداول دون فقد الصفوف التي تشير إليها عند المستهلكين
  Source     : US-MDL-002; DEFAULT [KB:erp-domain-standards §6 soft delete]
  Priority   : HIGH

#### AC-MDL-009 — القيمة المعطَّلة تغيب عن المستهلك وتبقى في الإدارة / inactive for consumers, visible in management
  Traces : REQ-MDL-009
  Given  : قيمة فعّالة رمزها `CHEQUE` تحت نوع فعّال مفتاحه `PAYMENT_METHOD`
  When   : يُعطّلها المستخدم
  Then   : تصير `isActiveFl = false`، ولا تُعاد في قراءة المستهلك للمفتاح `PAYMENT_METHOD`، وتبقى معروضة في الجزء التفصيلي للشاشة العامة

### REQ-MDL-010 — إعادة ترتيب قيم النوع / Reorder a type's values
  Pattern    : event
  Statement  : When a user submits a new display order for a lookup type's values, the system shall update each value's sort order to its position in the submitted order.
  بالعربية   : عند إرسال المستخدم ترتيب عرض جديدًا لقيم نوع لوكب، يحدّث النظام رتبة كل قيمة إلى موضعها في الترتيب المُرسَل.
  Traces     : US-MDL-002
  Entities   : ENT-MDL-002
  Rationale  : الترتيب حقل من الحقول القياسية (POL-MDL-005)، ومعناه أن يكون قابلًا للضبط دفعة واحدة
  Source     : US-MDL-002; POL-MDL-005
  Priority   : MEDIUM

#### AC-MDL-010 — الرتب تتبع الترتيب المُرسَل / sort orders follow the submitted order
  Traces : REQ-MDL-010
  Given  : نوع له ثلاث قيم `CASH` و`CHEQUE` و`TRANSFER` برُتب 1 و2 و3
  When   : يُرسل المستخدم الترتيب `TRANSFER`، `CASH`، `CHEQUE`
  Then   : تصير `sortOrder = 1` لـ`TRANSFER` و`2` لـ`CASH` و`3` لـ`CHEQUE`، وتُعاد القيم بهذا الترتيب في قراءة المستهلك

### REQ-MDL-011 — قراءة القيم الفعّالة بالمفتاح / Read active values by key
  Pattern    : event
  Statement  : When a consuming module requests a lookup type's values by that type's key, the system shall return the active values of the type ordered ascending by sort order.
  بالعربية   : عند طلب وحدة مستهلكة قيمَ نوع لوكب بمفتاحه، يعيد النظام القيم الفعّالة لذلك النوع مرتَّبةً تصاعديًا بالترتيب.
  Traces     : US-MDL-003
  Entities   : ENT-MDL-001, ENT-MDL-002
  Rationale  : سبب وجود الوحدة: لا وحدة تحتفظ بقائمة قيم مُرمَّزة خاصة بها (POL-MDL-001, G4)
  Source     : US-MDL-003; POL-MDL-001; lookup-module-plan-en.md §2, §4
  Priority   : HIGH

#### AC-MDL-011 — الفعّالة وحدها ومرتَّبة / active values only, in order
  Traces : REQ-MDL-011
  Given  : نوع فعّال مفتاحه `PAYMENT_METHOD` له قيمتان فعّالتان برتبتَي 1 و2 وقيمة ثالثة معطَّلة
  When   : تطلب وحدة مستهلكة قيم المفتاح `PAYMENT_METHOD`
  Then   : تُعاد القيمتان الفعّالتان وحدهما مرتَّبتين بـ`sortOrder`، وتحمل كل قيمة `code` و`nameAr` و`nameEn`، والقيمة المعطَّلة غائبة عن الرد

### REQ-MDL-012 — مفتاح غير معروف في قراءة المستهلك / An unknown key on a consumer read
  Pattern    : unwanted
  Statement  : If a consumer read names a key that no lookup type carries, then the system shall reject the read with a not-found error instead of an empty successful result.
  بالعربية   : إذا سمّت قراءةُ مستهلك مفتاحًا لا يحمله أي نوع لوكب، فيرفض النظام القراءة بخطأ «غير موجود» بدل نتيجة ناجحة فارغة.
  Traces     : US-MDL-003
  Entities   : ENT-MDL-001
  Rationale  : القائمة الفارغة تُخفي الخطأ البرمجي في المفتاح؛ التمييز بينهما شرط أن تُكتشف الأخطاء عند أول تكامل
  Source     : US-MDL-003; POL-MDL-001; PLATFORM-STD error catalog (project-registry DECISION INDEX #8 / ADR-SEC-002)
  Priority   : HIGH

#### AC-MDL-012 — مفتاح لا وجود له / a key that does not exist
  Traces : REQ-MDL-012
  Given  : لا نوع لوكب على المنصة بالمفتاح `NO_SUCH_KEY`
  When   : تطلب وحدة مستهلكة قيم المفتاح `NO_SUCH_KEY`
  Then   : يُرفض الطلب بخطأ «غير موجود» — ar: «لا يوجد نوع لوكب بهذا المفتاح» · en: "No lookup type exists with this key" — ولا يُعاد ردّ ناجح بقائمة فارغة

### REQ-MDL-013 — تصفّح سجل الأنواع حسب المالك / Browse the type registry by owner
  Pattern    : event
  Statement  : When a platform administrator browses the lookup type registry, the system shall return the active lookup types grouped by their owner module code.
  بالعربية   : عند تصفّح منسّق المنصة سجلَّ أنواع اللوكب، يعيد النظام الأنواع الفعّالة مجمّعةً حسب رمز وحدتها المالكة.
  Traces     : US-MDL-005
  Entities   : ENT-MDL-001
  Rationale  : التسمية بالمالك بلا عرضٍ حسب المالك لا تُراجَع ولا تُدقَّق (POL-MDL-002)
  Source     : US-MDL-005; POL-MDL-002; lookup-module-plan-en.md §7
  Priority   : MEDIUM

#### AC-MDL-013 — التجميع حسب الوحدة المالكة / grouping by owner module
  Traces : REQ-MDL-013
  Given  : ثلاثة أنواع فعّالة: اثنان مالكهما `FIN` وواحد مالكه `SEC`
  When   : يفتح منسّق المنصة سجلَّ الأنواع دون أي مُرشِّح
  Then   : تُعاد مجموعتان — `FIN` بنوعَيها و`SEC` بنوعه — ويحمل كل صف `key` و`nameAr` و`nameEn` و`ownerModuleCode`

## A5 — Business rules

### RULE-MDL-001 — الوحدة المالكة مسجَّلة في الأمان / The owner module is registered in SEC
  Scope      : ENT-MDL-001
  Trigger    : on create
  Statement  : The system shall reject a lookup type registration whose owner module code has no ModuleRegistry row in SEC.
  Message    : ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» · en: "The owning module is not registered in the Security module"
  Traces     : REQ-MDL-001, REQ-MDL-002
  Data source: ENT-MDL-001.ownerModuleCode
  Cross-module: وجود الرمز يُقرأ من ModuleRegistry (ENT-SEC-004، SOFT-READ — A8)، بلا مفتاح أجنبي
  Source     : POL-MDL-003; module-registry-mdl.md SHARED ENTITIES CONSUMED
  Test-Hint  : الفحص عند الإنشاء وحده؛ إلغاء تسجيل وحدة في الأمان لاحقًا لا يُبطل أنواعها القائمة

### RULE-MDL-002 — لا رمز مكرَّر ضمن النوع الواحد / No duplicate code within one type
  Scope      : ENT-MDL-002
  Trigger    : on create / on update
  Statement  : The system shall reject a lookup value whose code already exists under the same lookup type.
  Message    : ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» · en: "This code is already used within this type"
  Traces     : REQ-MDL-006, REQ-MDL-007, REQ-MDL-008
  Data source: ENT-MDL-002.lookupTypeId, ENT-MDL-002.code
  Source     : POL-MDL-006; lookup-module-plan-en.md §6
  Test-Hint  : الرمز نفسه تحت نوع آخر مقبول — التفرّد ضمن النوع لا على مستوى الوحدة

### RULE-MDL-003 — مفتاح النوع لا يتغيّر بعد الإنشاء / A type's key is immutable after creation
  Scope      : ENT-MDL-001
  Trigger    : on update
  Statement  : The system shall prevent editing a lookup type's key after creation.
  Message    : ar: «لا يمكن تعديل مفتاح نوع اللوكب بعد إنشائه» · en: "A lookup type's key cannot be changed after creation"
  Traces     : REQ-MDL-003
  Data source: ENT-MDL-001.key
  Source     : DEFAULT — المفتاح عقد القراءة لكل مستهلك (POL-MDL-001, G4)؛ [KB:erp-domain-standards §2 rule 3]
  Test-Hint  : التعديل لا يُرفض برسالة فحسب — المفتاح غائب أصلًا عن حمولة التعديل

### RULE-MDL-004 — النوع المعطَّل يحجب قيمه عن المستهلك / An inactive type hides its values from consumers
  Scope      : ENT-MDL-001, ENT-MDL-002
  Trigger    : on evaluate — عند كل قراءة مستهلك
  Statement  : While a lookup type is inactive, the system shall exclude its values from consumer reads.
  Message    : ar: «هذا النوع معطّل حاليًا» · en: "This lookup type is currently inactive"
  Traces     : REQ-MDL-004, REQ-MDL-011
  Data source: ENT-MDL-001.isActiveFl, ENT-MDL-002.isActiveFl
  Source     : POL-MDL-001; engine §3.3 ARCH-5
  Test-Hint  : الحجب أثر قراءة لا كتابة — لا يُغيَّر علم النشاط على أي قيمة عند تعطيل نوعها

**ARCH-5** — الكيانان كلاهما SHARED (owner) ويقبلان التعطيل: أثر التعطيل على المستهلك
SOFT-READ منصوصٌ عليه بالمنع في RULE-MDL-004 للنوع، وفي REQ-MDL-011 للقيمة (الفعّالة
وحدها تُعاد). لا انتشار (cascade) ولا إشعار: الأثر أثر قراءة، والصفوف تبقى كما هي.

**قيود بلا قاعدة خاصة / constraints carried by the platform error catalog** — تفرّد
المفتاح على مستوى المنصة (REQ-MDL-001) و«غير موجود» لمفتاح مجهول (REQ-MDL-012) صفّان من
صفوف كتالوج الأخطاء البنيوية (duplicate، not-found) تُذكر تحت مظلة PLATFORM-STD الواحدة،
لا كقاعدة عمل لكل منها — سابقة معتمدة في project-registry DECISION INDEX #8 (ADR-SEC-002).
رسالتاهما ثنائيتا اللغة في AC-MDL-001 وAC-MDL-012.

## A6 — Lookups

لا تملك الوحدة أي مفتاح قائمة قيم ولا تستهلك أيًّا منه في هذه النسخة: MDL هي الآلية التي
تعمل عليها قوائم الوحدات الأخرى، ولا "تستهلك نفسها" (module-registry-mdl.md LOOKUPS OWNED).
علم النشاط `isActiveFl` قيمة منطقية باصطلاح المنصة لا نوع lookup، و`ownerModuleCode` رمز
وحدة يُقرأ من سجل وحدات الأمان لا مفتاح قائمة قيم (A8).
MDL owns no lookup key and consumes none; قاعدة الملف الشخصي "كل قيم LOV تُحمَّل زمن
التشغيل من وحدة اللوكب" هي وصف هذه الوحدة نفسها لا قيدًا تستهلكه.

## A7 — Status lifecycle

لا ينطبق / not applicable — لكل من الكيانين حالتان اثنتان لا أكثر (فعّال / معطَّل) وانتقال
واحد في هذه النسخة، وهو دون عتبة الثلاث حالات التي تستوجب مخطّطًا (§6):

```
فعّال (isActiveFl = true)  ──(تعطيل / deactivate)──▶  معطَّل (isActiveFl = false)
```
لا انتقال عكسي: إعادة التفعيل خارج النطاق (A2) — لا قصة معتمدة تطلبها.

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM candidate (assigned by P2) |
|---|---|---|---|---|
| ModuleRegistry | ENT-SEC-004 | SEC | SOFT-READ — فحص وجود رمز الوحدة المالكة عند الإنشاء (RULE-MDL-001) | نعم — مرشَّح XM، المعرّف من إسناد P2 |

الكيان أعلاه هو الكيان الوحيد الذي تستهلكه MDL. لا مفتاح أجنبي عابرًا للوحدات: الفحص
قراءة تطبيقية بالرمز [KB:erp-domain-standards §5]، وهو مسموح في أي اتجاه، ولا يغيّر كون
MDL وحدة تأسيسية Tier 0 [KB:erp-domain-standards §1].

| External service | Purpose | Integration kind |
|---|---|---|
| — | لا خدمة خارجية: لا إشعارات ولا خدمة ملفات ولا أي قائمة قيم مستهلكة | — |

# PART B — SCREEN REQUIREMENTS

## SCR-REQ-MDL-001 — اللوكبات العامة / Generic Lookups

### B1 — Definition
  Purpose      : أن يدير المستخدم أنواع اللوكب وقيمها كلها من شاشة واحدة، دون شاشة لكل قائمة
  Entities     : ENT-MDL-001, ENT-MDL-002
  Operations   : search · create · read · update · deactivate · reorder
  Users        : منسّق المنصة / Platform administrator · مدير قوائم الوحدة المالكة / Owning-module lookup manager
  Navigation   : البيانات المرجعية / Master Data Lookup → اللوكبات / Lookups → اللوكبات العامة / Generic Lookups ; from: القائمة الرئيسية / main menu ; to: سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner (SCR-REQ-MDL-002)
  Content shape: header + repeating lines — رئيسي (أنواع) + تفصيلي (قيم) قابل لإعادة الترتيب — تلميح لا تصميم
  Traces     : REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010
  Composite    : بحث + إدخال على المستويين (رئيسي + تفصيلي) = متطلَّب شاشة واحد (profile.conventions.composite_screen)

### B2 — Search / list
| Filter | Field (ENT) | Result column | Notes |
|---|---|---|---|
| المفتاح / Key | ENT-MDL-001.key | المفتاح / Key | تطابق جزئي (LIKE) |
| الوحدة المالكة / Owner module | ENT-MDL-001.ownerModuleCode | الوحدة المالكة / Owner module | تطابق تام (EXACT) |
| الاسم / Name | ENT-MDL-001.nameAr, ENT-MDL-001.nameEn | الاسم (عربي) · الاسم (إنجليزي) / Name (ar · en) | تطابق جزئي في اللغتين معًا [KB:erp-domain-standards §6 search] |
| الحالة / Active | ENT-MDL-001.isActiveFl | نشط / Active | تطابق تام |
| رمز القيمة / Value code | ENT-MDL-002.code | الرمز / Code | مُرشِّح الجزء التفصيلي، محصور بالنوع المختار (REQ-MDL-005) |

كل مُرشِّح يقابله عمود نتيجة. القائمة الرئيسية مُرقَّمة الصفحات على الخادم (20 افتراضًا،
200 حدًّا أقصى — [KB:erp-domain-standards §6 paging])، والجزء التفصيلي يُعرض كاملًا مرتَّبًا
بـ`sortOrder` لأنه محصور بنوع واحد. لا مفتاح قائمة قيم في أي مُرشِّح (A6).

### B3 — Input
| Field (ENT) | Editable | Notes |
|---|---|---|
| ENT-MDL-001.key | عند الإنشاء وحده / create-only | applies RULE-MDL-003 |
| ENT-MDL-001.ownerModuleCode | عند الإنشاء وحده / create-only | applies RULE-MDL-001 |
| ENT-MDL-001.nameAr, ENT-MDL-001.nameEn | yes | REQ-MDL-001, REQ-MDL-003 |
| ENT-MDL-001.isActiveFl | no | يتغيّر بإجراء التعطيل وحده (REQ-MDL-004) |
| ENT-MDL-002.code | عند الإنشاء وحده / create-only | applies RULE-MDL-002 |
| ENT-MDL-002.nameAr, ENT-MDL-002.nameEn | yes | REQ-MDL-006, REQ-MDL-008 |
| ENT-MDL-002.sortOrder | yes | يُضبط بالتحرير أو بإعادة الترتيب (REQ-MDL-010) |
| ENT-MDL-002.isActiveFl | no | يتغيّر بإجراء التعطيل وحده (REQ-MDL-009) |
| حقول التدقيق على الكيانين / audit fields | no | يملؤها النظام |

| Action (ar / en) | Operation | REQ | RULEs |
|---|---|---|---|
| حفظ نوع / Save type | create | REQ-MDL-001, REQ-MDL-002 | RULE-MDL-001 |
| حفظ تعديل النوع / Save type changes | update | REQ-MDL-003 | RULE-MDL-003 |
| تعطيل النوع / Deactivate type | deactivate | REQ-MDL-004 | RULE-MDL-004 |
| حفظ قيمة / Save value | create | REQ-MDL-006, REQ-MDL-007 | RULE-MDL-002 |
| حفظ تعديل القيمة / Save value changes | update | REQ-MDL-008 | RULE-MDL-002 |
| تعطيل القيمة / Deactivate value | deactivate | REQ-MDL-009 | — |
| إعادة الترتيب / Reorder | reorder | REQ-MDL-010 | — |

لا إجراء تفعيل ولا محو نهائي على أي من المستويين: إعادة التفعيل خارج النطاق (A2)،
والتعطيل ناعم [KB:erp-domain-standards §6].

### B4 — Access
| Page code | Action | Roles |
|---|---|---|
| MDL_LOOKUPS | VIEW (بوابة / gateway) | منسّق المنصة · مدير قوائم الوحدة المالكة · الحساب الخدمي للوحدة المستهلكة (قراءة B5 وحدها، بلا واجهة) |
| MDL_LOOKUPS | CREATE | منسّق المنصة · مدير قوائم الوحدة المالكة |
| MDL_LOOKUPS | UPDATE | منسّق المنصة · مدير قوائم الوحدة المالكة |
| MDL_LOOKUPS | DELETE (تعطيل ناعم / soft deactivate) | منسّق المنصة · مدير قوائم الوحدة المالكة |

صفٌّ واحد في SEC_PAGES لهذه الشاشة المركّبة. أسماء الصلاحيات تشتقّها وحدة الأمان من رمز
الصفحة ولا تُعدَّد هنا. منح الشاشة هو كل الدقّة المتاحة: الصلاحية لكل نوع لوكب على حدة
مستثناة صراحةً (business-policies SCOPE EXCEPTIONS)، ولا منطق تفويض مكتوب داخل الوحدة
(domain-profile G2, G3).

### B5 — API expectations
Base path : `/api/v1/mdl/lookup-types` و`/api/v1/mdl/lookup-values` — Response `ApiResponse<T>` ·
Paging `Page<T>` · Errors `LocalizedException → {code, messageAr, messageEn}`

| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| search types | POST | `/api/v1/mdl/lookup-types/search` | key, ownerModuleCode, name, isActiveFl, paging | page of lookup types | — | REQ-MDL-001, REQ-MDL-003 |
| create type | POST | `/api/v1/mdl/lookup-types` | key, ownerModuleCode, nameAr, nameEn | the created type | RULE-MDL-001 | REQ-MDL-001, REQ-MDL-002 |
| update type | PUT | `/api/v1/mdl/lookup-types/{id}` | id, nameAr, nameEn | the updated type | RULE-MDL-003 | REQ-MDL-003 |
| deactivate type | DELETE | `/api/v1/mdl/lookup-types/{id}` | id | confirmation | RULE-MDL-004 | REQ-MDL-004 |
| search values of a type | POST | `/api/v1/mdl/lookup-types/values/search` | lookupTypeId, code | the type's values, ordered | — | REQ-MDL-005 |
| create value | POST | `/api/v1/mdl/lookup-types/{id}/values` | id, code, nameAr, nameEn, sortOrder | the created value | RULE-MDL-002 | REQ-MDL-006, REQ-MDL-007 |
| update value | PUT | `/api/v1/mdl/lookup-values/{id}` | id, nameAr, nameEn, sortOrder | the updated value | RULE-MDL-002 | REQ-MDL-008 |
| deactivate value | DELETE | `/api/v1/mdl/lookup-values/{id}` | id | confirmation | — | REQ-MDL-009 |
| reorder values | PATCH | `/api/v1/mdl/lookup-types/{id}/values/reorder` | id, القيم بترتيبها الجديد | confirmation | — | REQ-MDL-010 |

القراءات الثلاث تُطلب بـ`POST …/search` لأن مُرشِّحاتها مركَّبة، وهو اصطلاح المنصة للبحث؛
وباقي الأفعال على اصطلاح `profile.stack.backend.api`. لا قيمة مُرمَّزة مُعدَّدة في أي عملية
(A6). المفاتيح والرموز تُرسل كما كتبها المستخدم، ولا يولّد النظام أيًّا منها (§3.3 NUMBERING).

## SCR-REQ-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

### B1 — Definition
  Purpose      : أن يجد منسّق المنصة قوائم أي وحدة ويُدقّقها في نظرة واحدة، مجمّعةً حسب مالكها
  Entities     : ENT-MDL-001
  Operations   : search · browse (قراءة فقط / read-only)
  Users        : منسّق المنصة / Platform administrator
  Navigation   : البيانات المرجعية / Master Data Lookup → اللوكبات / Lookups → سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner ; from: القائمة الرئيسية / main menu, اللوكبات العامة / Generic Lookups ; to: اللوكبات العامة / Generic Lookups (SCR-REQ-MDL-001)
  Content shape: true hierarchy (parent/child) — وحدة مالكة → أنواعها — تلميح لا تصميم
  Traces     : REQ-MDL-011, REQ-MDL-012, REQ-MDL-013
  Composite    : تصفّح مجمَّع بلا إدخال = متطلَّب شاشة واحد (profile.conventions.composite_screen)

### B2 — Search / list
| Filter | Field (ENT) | Result column | Notes |
|---|---|---|---|
| الوحدة المالكة / Owner module | ENT-MDL-001.ownerModuleCode | الوحدة المالكة / Owner module | تطابق تام (EXACT) — وهو عنوان المجموعة نفسه |
| المفتاح / Key | ENT-MDL-001.key | المفتاح / Key | تطابق جزئي (LIKE) |

كل مُرشِّح يقابله عمود نتيجة. المجموعة تُعاد كاملة بلا ترقيم صفحات — عدد الأنواع محكوم
بعدد الوحدات — والأنواع الفعّالة وحدها تدخل السجل (REQ-MDL-013)، فلا مُرشِّح حالة هنا.

### B3 — Input
تصفّح للقراءة فقط؛ لا إنشاء ولا تعديل هنا (الإدارة على SCR-REQ-MDL-001) /
Read-only browse; no create/update here (management happens on SCR-REQ-MDL-001).
الحقول المعروضة كلها من ENT-MDL-001 بالإشارة، وأيٌّ منها غير قابل للتحرير.

| Action (ar / en) | Operation | REQ | RULEs |
|---|---|---|---|
| فتح النوع في الشاشة العامة / Open in Generic Lookups | read | REQ-MDL-013 | — |

### B4 — Access
| Page code | Action | Roles |
|---|---|---|
| MDL_TYPE_REGISTRY | VIEW (بوابة / gateway) | منسّق المنصة / Platform administrator |

لا CREATE ولا UPDATE ولا DELETE على هذه الشاشة — وهو نصّ B3 نفسه مقروءًا من جهة التفويض.
صفٌّ واحد في SEC_PAGES، وأسماء الصلاحيات تشتقّها وحدة الأمان من رمز الصفحة.

### B5 — API expectations
Base path : `/api/v1/mdl/lookup-types` و`/api/v1/mdl/lookups` — Response `ApiResponse<T>` ·
Errors `LocalizedException → {code, messageAr, messageEn}`

| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| browse registry by owner | POST | `/api/v1/mdl/lookup-types/by-owner/search` | ownerModuleCode, key | الأنواع الفعّالة مجمّعة حسب المالك | — | REQ-MDL-013 |
| read values by key (consumer) | GET | `/api/v1/mdl/lookups` | مفتاح النوع / the type key | القيم الفعّالة مرتَّبة بـ`sortOrder` | RULE-MDL-004 | REQ-MDL-011, REQ-MDL-012 |

الصفّ الثاني عمليةٌ بلا سطح شاشة: تستدعيها خلفيّاتُ الوحدات المستهلكة لا مستخدمٌ
(US-MDL-003)، وتُدرج في هذا المتطلَّب لأن متطلَّبَيها (REQ-MDL-011, REQ-MDL-012) يخصّان
السجلَّ نفسه قراءةً بالمفتاح. تفويضها هو منح VIEW على MDL_LOOKUPS للحساب الخدمي للوحدة
المستهلكة (B4 في SCR-REQ-MDL-001)، لا منحٌ خاص بها.

# STANDALONE

## Traceability matrix

| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-MDL-001 | REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004 | AC-MDL-001, AC-MDL-002, AC-MDL-003, AC-MDL-004 | RULE-MDL-001, RULE-MDL-003, RULE-MDL-004 | ENT-MDL-001 | SCR-REQ-MDL-001 |
| US-MDL-002 | REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010 | AC-MDL-005, AC-MDL-006, AC-MDL-007, AC-MDL-008, AC-MDL-009, AC-MDL-010 | RULE-MDL-002 | ENT-MDL-001, ENT-MDL-002 | SCR-REQ-MDL-001 |
| US-MDL-003 | REQ-MDL-011, REQ-MDL-012 | AC-MDL-011, AC-MDL-012 | RULE-MDL-004 | ENT-MDL-001, ENT-MDL-002 | SCR-REQ-MDL-002 |
| US-MDL-004 | REQ-MDL-001, REQ-MDL-002 | AC-MDL-001, AC-MDL-002 | RULE-MDL-001 | ENT-MDL-001 | SCR-REQ-MDL-001 |
| US-MDL-005 | REQ-MDL-013 | AC-MDL-013 | — | ENT-MDL-001 | SCR-REQ-MDL-002 |

كل قصة من القصص الخمس مغطّاة بمتطلَّب واحد على الأقل، وكل متطلَّب من الثلاثة عشر يتتبّع
قصةً واحدة على الأقل وله معيار قبول واحد، وكل قاعدة من الأربع تتتبّع متطلَّبًا، وكل
متطلَّب شاشة يتتبّع متطلَّبات مسمّاة. لا يتيم ولا مرجع معلّق. (US-MDL-005 قصة تصفّح
للقراءة فقط: لا قاعدة عمل تخصّها، والقيد الوحيد عليها — الأنواع الفعّالة وحدها — منصوصٌ
في نصّ REQ-MDL-013 نفسه.)

## Decisions applied

| DEFAULT / ADR | What | Source | Override / status |
|---|---|---|---|
| DEFAULT | التعطيل ناعم بـ`isActiveFl` على الكيانين، ولا حذف نهائي | [KB:erp-domain-standards §6 soft delete] | Override: لا يُغيَّر إلا بقرار منصّي يشمل الوحدات كلها |
| DEFAULT | حقول التدقيق الأربعة على الكيانين معًا، ومنها `lookup` رغم غيابها عن مجموعته القياسية | [KB:erp-domain-standards §6 audit trail] | Override: إسقاطها عن قيمة اللوكب يُفقد أثر مَن غيّر قائمةً يقرأها الجميع |
| DEFAULT | ترقيم صفحات قائمة الأنواع على الخادم: 20 افتراضًا، 200 حدًّا أقصى | [KB:erp-domain-standards §6 paging] | Override: رقمان قابلان للضبط دون أثر على أي متطلَّب |
| DEFAULT | البحث على الخادم بالمفتاح والاسم في اللغتين | [KB:erp-domain-standards §6 search] | Override: حصره بالمفتاح وحده إن طلب العميل |
| DEFAULT | التواريخ تُخزَّن UTC وتُعرض بتوقيت المستأجر | [KB:erp-domain-standards §6 dates] | Override: قرار منصّي لا قرار وحدة |
| DEFAULT | مفتاح النوع غير قابل للتعديل بعد الإنشاء (RULE-MDL-003) | POL-MDL-001; G4; [KB:erp-domain-standards §2 rule 3] | Override: السماح بالتعديل يوجب مسار ترحيل لكل مستهلك يقرأ بالمفتاح — قرار منصّي |
| سابقة / precedent — ADR-SEC-002 | تفرّد المفتاح و«غير موجود» صفّا كتالوج أخطاء بنيوية تحت مظلة PLATFORM-STD، لا قاعدة عمل لكل منهما | project-registry DECISION INDEX #8 | ACCEPTED — مطبَّقة كما هي، بلا قرار جديد |
| سابقة / precedent — ADR-FIN-001 | التبعية على الأمان قراءة تطبيقية لا مفتاحًا أجنبيًا عابرًا للوحدات | project-registry DECISION INDEX #9; [KB:erp-domain-standards §5] | ACCEPTED — مطبَّقة على RULE-MDL-001 وA8 |

لا قرار ADR جديد في هذه المرحلة: كل نقطة كانت تحتمل وجهين حسمها مدخلٌ قائم — سياسة، أو
سجلّ، أو مصدر معرفة، أو سابقة معتمدة في سجلّ القرارات. ولا قرار بحالة BLOCKED.

## Access summary

| Page code | Screen (ar / en) | Actions | Roles |
|---|---|---|---|
| MDL_LOOKUPS | اللوكبات العامة / Generic Lookups | VIEW (بوابة) · CREATE · UPDATE · DELETE (تعطيل) | منسّق المنصة · مدير قوائم الوحدة المالكة · الحساب الخدمي للوحدة المستهلكة (VIEW وحدها) |
| MDL_TYPE_REGISTRY | سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner | VIEW (بوابة) | منسّق المنصة |

صفّان في SEC_PAGES لا أكثر — شاشتان مركّبتان، لكل واحدة رمز صفحة واحد. المصدر هو B4 في
كلا متطلَّبَي الشاشة، وأسماء الصلاحيات تشتقّها وحدة الأمان من رمز الصفحة ولا تُعدَّد هنا.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 06, 08, 10, 11
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. Endpoint paths are taken from
`docs/api-docs/mdl/`. No REQ / AC / RULE / ENT ids are minted; items are labelled NEW / CHANGED / REMOVED.

### 1. Endpoints
No endpoint added, changed or removed by erp-core. All 11 MDL operations
(`/api/v1/mdl/lookups`, `/api/v1/mdl/lookup-types…`, `/api/v1/mdl/lookup-values/{id}`) keep their
analysed behaviour, now confined to the request tenant. — docs/api-docs/mdl/index.md

### 2. Business rules
| Kind | Rule | Source |
|---|---|---|
| CHANGED | REQ-MDL-001 "key unique across the platform": implemented unique per tenant (`UQ_MDL_LOOKUP_TYPE_KEY (TENANT_ID, key)`), because step 05 scopes every core table by tenant. | V10__tenant_schema.sql |
| CHANGED | RULE-MDL-002 (no duplicate code within a type): per tenant (`UQ_MDL_LOOKUP_VALUE_TYPE_CODE (TENANT_ID, lookup_type_id, code)`). | V10 |
| NEW | Tenant confinement: every MDL read and write is filtered by the request tenant (Hibernate `@TenantId`); another tenant's id answers as not found. | docs/steps/05-report.md |
| NEW | Provisioning: a new tenant receives a copy of all of PLATFORM's lookup types and values (`MdlTenantProvisioningContributor`, same transaction as the tenant insert). | DEVIATIONS [05] |
| CHANGED | RULE-MDL-001 owner-module check reads the global, code-defined module registry (synchronized at startup by `PermissionCatalogSynchronizer`). | docs/steps/06-report.md |
| NEW | Optimistic locking (`VERSION`); a concurrent update → 409 `CONCURRENT_MODIFICATION`. | DEVIATIONS [05] |
| NEW | `@Audited` on `LookupType` and `LookupValue` → `CORE_AUDIT_EVENT` (CREATE / UPDATE / DELETE with field changes). | DEVIATIONS [10] |
| NEW | `MdlLookupApi` validates report LOOKUP parameters (active values; an unknown or inactive type makes the parameter invalid). | DEVIATIONS [11] |

### 3. Error codes
No new MDL code. Common additions visible on MDL endpoints: `CONCURRENT_MODIFICATION` 409 (step 05),
`NOT_FOUND` 404 for an unknown path and `VALIDATION_ERROR` 400 for an overflowing `page` (1.2.0).
— DEVIATIONS [05]; CHANGELOG [1.2.0]

### 4. Permissions (exact authority strings)
| Kind | Authority | Source |
|---|---|---|
| unchanged (code-defined in `MdlPermissions`) | `PERM_MDL_LOOKUPS_VIEW`, `PERM_MDL_LOOKUPS_CREATE`, `PERM_MDL_LOOKUPS_UPDATE`, `PERM_MDL_TYPE_REGISTRY_VIEW` | erp-core/src/main/java/com/erp/mdl/permission/MdlPermissions.java |

### 5. Entities and seeded values
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | ENT-MDL-001 / 002 | + `tenantId`, + `version` | V10 |
| NEW (seed) | values of NOTIF-owned types | `NOTIF_STATUS`: `QUEUED`, `SKIPPED_NO_PROVIDER`; `NOTIF_CHANNEL`: `IN_APP` (every existing tenant) | V13__notif_async_inbox.sql §3a |

### 6. Dependencies (A8) — deltas
tenant (`CORE_TENANT` FK, provisioning SPI); SEC permission SPI (`MdlPermissions`); audit (`@Audited`);
consumer: report module (LOOKUP parameters). — DEVIATIONS [05], [06], [10], [11]


## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C5 — `MdlTenantExportContributor` implements TENANT's export SPI (XM-TENANT-004) for `POST /api/v1/platform/tenants/{id}/export` (plan §5 C.5)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

No MDL id is minted (the export rules are TENANT's RULE-TENANT-027 / -028; the SPI is TENANT's XM-TENANT-004). No
endpoint, entity field, rule, error code, permission or migration of MDL changes.

| Kind | Item | Delta | Source |
|---|---|---|---|
| NEW | `com.erp.mdl.tenant.MdlTenantExportContributor` (`moduleCode` `MDL`) | writes `MDL/MDL_LOOKUP_TYPE` and `MDL/MDL_LOOKUP_VALUE` of the exported tenant (every column but `TENANT_ID` / `VERSION`; the value file adds `LOOKUP_TYPE_KEY`, the type's `KEY` joined within the same tenant) with plain SQL naming `TENANT_ID` on both tables (RULE-TENANT-011), ordered by the primary key | `../../TENANT/P1/srs-tenant.md` 1.3.0 X5, X7 |
| NEW (dependency) | tenant | + SPI implemented: `com.erp.tenant.TenantExportContributor` (root package) | TENANT XM-TENANT-004 |
