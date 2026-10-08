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
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository at that tag; `mdl/` abbreviates
`erp-core/src/main/java/com/erp/mdl/` and `V<N>__…` the scripts under
`erp-core/src/main/resources/db/migration/core/`. Endpoint paths, authorities and codes are taken from
`docs/api-docs/mdl/`. No REQ / AC / ENT ids are minted; the NEW rules of §2 continue the RULE sequence from
RULE-MDL-004 (the last id the body uses). Every row is labelled NEW / CHANGED / REMOVED / NOT IMPLEMENTED.

### 1. Endpoints — نقاط النهاية
All 11 operations of B5 exist with the analysed verb and path, confined to the request tenant (step 05).
Every one of them differs from B5 in authority, input shape, output or error behaviour as the rows state.

| Kind | Method | Path | Authority | Delta against B5 | Error codes | Source |
|---|---|---|---|---|---|---|
| CHANGED | POST | `/api/v1/mdl/lookup-types/search` | `PERM_MDL_LOOKUPS_VIEW` | Input is the generic `filters[]` envelope (`field`, `operator`, `value`) over `key`, `ownerModuleCode`, `nameAr`, `nameEn`, `isActiveFl`, `createdAt` (also the sortable fields); no combined "name" filter over both languages — the client filters `nameAr` or `nameEn` by name. Paged (`page` 0-based, `size` default 20, max 200). | — (common only) | mdl/service/LookupTypeService.java:58-59,144-159; docs/api-docs/mdl/endpoints/lookup-type-management.md |
| CHANGED | POST | `/api/v1/mdl/lookup-types` | `PERM_MDL_LOOKUPS_CREATE` | 201 with the created `LookupTypeResponse`; `key` ≤ 80, `ownerModuleCode` ≤ 10, names ≤ 150 characters. The two business refusals are catalogued codes (B5 named rules only). | 409 `MDL-409-MODULE-NOT-REGISTERED`, 409 `MDL-409-TYPE-DUP` | mdl/service/LookupTypeService.java:74-94; mdl/domain/LookupTypeDomain.java:40-50 |
| CHANGED | PUT | `/api/v1/mdl/lookup-types/{id}` | `PERM_MDL_LOOKUPS_UPDATE` | Names only, as analysed; an unknown id is a catalogued code. | 404 `MDL-404-TYPE` | mdl/service/LookupTypeService.java:100-113 |
| CHANGED | DELETE | `/api/v1/mdl/lookup-types/{id}` | `PERM_MDL_LOOKUPS_UPDATE` (B4 said DELETE — no DELETE authority exists on MDL_LOOKUPS) | Returns 200 with the `LookupTypeResponse` (`isActiveFl = false`), not a bare confirmation. | 404 `MDL-404-TYPE` | mdl/permission/MdlPermissions.java:23-27; V7__sec_seed.sql:79-80,109-112; mdl/controller/LookupTypeController.java:68-72 |
| CHANGED | POST | `/api/v1/mdl/lookup-types/values/search` | `PERM_MDL_LOOKUPS_VIEW` | B2 said the detail is "shown in full"; implemented as a page (default 20, max 200), default sort `sortOrder` ascending, active and inactive values alike. `lookupTypeId` travels inside `filters[]` (field `lookupTypeId`, `PARENT_ID_FILTER`), never as a path variable; `code` is a generic filter with a client-chosen operator. A missing or unknown `lookupTypeId` → 404. | 404 `MDL-404-TYPE` | mdl/service/LookupValueService.java:171-203; mdl/dto/LookupValueSearchRequest.java:40-50 |
| CHANGED | POST | `/api/v1/mdl/lookup-types/{id}/values` | `PERM_MDL_LOOKUPS_CREATE` | `sortOrder` is mandatory (`@NotNull`) — A3's "0 by default" survives only as the column default, never as a request default. 201 with the created `LookupValueResponse`. The parent type may be inactive (§2 RULE-MDL-010). | 404 `MDL-404-TYPE`, 409 `MDL-409-VALUE-DUP` | mdl/dto/LookupValueCreateRequest.java:37-40; mdl/service/LookupValueService.java:61-78 |
| CHANGED | PUT | `/api/v1/mdl/lookup-values/{id}` | `PERM_MDL_LOOKUPS_UPDATE` | `code` is absent from the body, so RULE-MDL-002 cannot fire on update (B5 listed it); names and `sortOrder` only. | 404 `MDL-404-VALUE` | mdl/dto/LookupValueUpdateRequest.java:12-15; mdl/service/LookupValueService.java:81-94 |
| CHANGED | DELETE | `/api/v1/mdl/lookup-values/{id}` | `PERM_MDL_LOOKUPS_UPDATE` (B4 said DELETE) | Returns 200 with the `LookupValueResponse` (`isActiveFl = false`). | 404 `MDL-404-VALUE` | mdl/service/LookupValueService.java:97-110; mdl/controller/LookupValueController.java:69-73 |
| CHANGED | PATCH | `/api/v1/mdl/lookup-types/{id}/values/reorder` | `PERM_MDL_LOOKUPS_UPDATE` | Body `orderedValueIds` (non-empty list of value ids). Positions are assigned 0-based (`sortOrder` = 0, 1, 2 …), not 1, 2, 3 as AC-MDL-010 states; a partial list is accepted (only the listed values are renumbered, the others keep their `sortOrder`); an id that is unknown or belongs to another type — including an unknown `{id}` — → 400. Returns the renumbered values in the submitted order. | 400 `MDL-400-REORDER-MISMATCH` | mdl/service/LookupValueService.java:121-152; mdl/dto/LookupValueReorderRequest.java:22-24 |
| CHANGED | POST | `/api/v1/mdl/lookup-types/by-owner/search` | `PERM_MDL_TYPE_REGISTRY_VIEW` | Active types only (predicate ANDed unconditionally, no `isActiveFl` filter accepted); optional generic filters `ownerModuleCode`, `key`; grouped by `ownerModuleCode` (sorted) into `List<OwnerGroupResponse{ownerModuleCode, types[]}>`; `page` / `size` / `sortField` are accepted and ignored (not paged). | — (common only) | mdl/service/LookupTypeService.java:173-200; mdl/dto/LookupTypeByOwnerSearchRequest.java:11-20 |
| CHANGED | GET | `/api/v1/mdl/lookups?type=` | `PERM_MDL_LOOKUPS_VIEW` | An unknown key **or an inactive type** both answer 404 `MDL-404-TYPE-KEY` (RULE-MDL-004 / AC-MDL-004 said an inactive type's values are excluded; REQ-MDL-012 limited the 404 to an unknown key). An active type with no active value answers 200 with an empty list. Returns full `LookupValueResponse` rows (id, `lookupTypeId`, audit fields included), not only `code`, `nameAr`, `nameEn`. | 404 `MDL-404-TYPE-KEY` | mdl/service/LookupConsumerService.java:47-62; mdl/controller/LookupConsumerController.java:30-35 |

As-built contract, one line per endpoint (authority → business codes):
`POST /lookup-types/search` VIEW → none · `POST /lookup-types` CREATE → MDL-409-MODULE-NOT-REGISTERED, MDL-409-TYPE-DUP ·
`PUT /lookup-types/{id}` UPDATE → MDL-404-TYPE · `DELETE /lookup-types/{id}` UPDATE → MDL-404-TYPE ·
`POST /lookup-types/values/search` VIEW → MDL-404-TYPE · `POST /lookup-types/{id}/values` CREATE → MDL-404-TYPE, MDL-409-VALUE-DUP ·
`PUT /lookup-values/{id}` UPDATE → MDL-404-VALUE · `DELETE /lookup-values/{id}` UPDATE → MDL-404-VALUE ·
`PATCH /lookup-types/{id}/values/reorder` UPDATE → MDL-400-REORDER-MISMATCH · `POST /lookup-types/by-owner/search` TYPE_REGISTRY_VIEW → none ·
`GET /lookups?type=` VIEW → MDL-404-TYPE-KEY (VIEW = `PERM_MDL_LOOKUPS_VIEW`, CREATE = `PERM_MDL_LOOKUPS_CREATE`,
UPDATE = `PERM_MDL_LOOKUPS_UPDATE`, TYPE_REGISTRY_VIEW = `PERM_MDL_TYPE_REGISTRY_VIEW`; all paths under `/api/v1/mdl`).
Common codes reachable on every row: `VALIDATION_ERROR` 400, `ACCESS_DENIED` 403, `METHOD_NOT_ALLOWED` 405,
`DATA_INTEGRITY_VIOLATION` 409, `CONCURRENT_MODIFICATION` 409, `INTERNAL_ERROR` 500, and `NOT_FOUND` 404 for an
unknown path (1.2.0). — docs/api-docs/mdl/index.md "Known Error Codes"; CHANGELOG [1.2.0]

### 2. Business rules — قواعد العمل
| Kind | Rule | Source |
|---|---|---|
| CHANGED | RULE-MDL-001: the owner module must exist **and be active** (`SEC_MODULE_REG.CODE` with `IS_ACTIVE_FL = TRUE`), read in-process through `SecModuleRegistryApi.isModuleActive(code)` — an interface call, no HTTP and no FK. The registry is the code-defined global catalog synchronized at startup by `PermissionCatalogSynchronizer`, so `PLATFORM`, `SEQUENCE`, `AUDIT`, `REPORT` and an application's own modules are valid owners once declared. Message and create-only trigger as analysed; a module deactivated later does not invalidate its types (the Test-Hint holds). | mdl/service/LookupTypeService.java:80; erp-core/src/main/java/com/erp/sec/crossmodule/SecModuleRegistryApiImpl.java:36-39; docs/steps/06-report.md |
| CHANGED | REQ-MDL-001 "key unique across the platform": unique per tenant (`UQ_MDL_LOOKUP_TYPE_KEY (TENANT_ID, key)`), because step 05 scopes every core table by tenant; the duplicate answers 409 `MDL-409-TYPE-DUP`. | V10__tenant_schema.sql:177-178; mdl/domain/LookupTypeDomain.java:48 |
| CHANGED | RULE-MDL-002: per tenant (`UQ_MDL_LOOKUP_VALUE_TYPE_CODE (TENANT_ID, lookup_type_id, code)`); fires on create only — `code` is absent from the update body, so the "on update" trigger of the rule never occurs. | V10__tenant_schema.sql:179-180; mdl/dto/LookupValueUpdateRequest.java:12-15 |
| CHANGED | RULE-MDL-004: an inactive type does not "hide its values" — the consumer read is **refused** with 404 `MDL-404-TYPE-KEY`, on the HTTP read and on `MdlLookupApi` alike. AC-MDL-004's "none of its values is returned" is therefore satisfied by an error, not by an empty success. The values' own flags are untouched by a type deactivation (Test-Hint holds); no cascade. | mdl/service/LookupConsumerService.java:53-55; mdl/crossmodule/MdlLookupApiImpl.java:54-57 |
| CHANGED | REQ-MDL-005: the selected type's values are read as a **page** (default 20, max 200), default sort `sortOrder` ascending, active and inactive alike (AC-MDL-005's "all four, active and inactive" holds within the page). | mdl/service/LookupValueService.java:171-203 |
| CHANGED | REQ-MDL-006: `sortOrder` is a mandatory input of a new value; the "0 by default" of A3 is only the column default (`DEFAULT 0`) and the entity's `@PrePersist` fallback. | mdl/dto/LookupValueCreateRequest.java:37-40; mdl/entity/LookupValue.java:84-87,99-101 |
| CHANGED | REQ-MDL-010: positions are 0-based (`sortOrder` = list index); a partial list is accepted and renumbers only the listed values; an unknown id, an id of another type or an unknown type id → 400 `MDL-400-REORDER-MISMATCH`, nothing saved. AC-MDL-010's "1 / 2 / 3" reads as "0 / 1 / 2". | mdl/service/LookupValueService.java:126-147 |
| NEW | RULE-MDL-005 — Tenant confinement: every MDL read and write is filtered by the request tenant (Hibernate `@TenantId` on both entities); another tenant's id answers as not found. | docs/steps/05-report.md; V10__tenant_schema.sql:64-65,103-104 |
| NEW | RULE-MDL-006 — Provisioning: `MdlTenantProvisioningContributor` (SPI `com.erp.tenant.TenantProvisioningContributor`, `order() = 10`) copies **all** of the source (PLATFORM) tenant's lookup types and values into the new tenant — inactive rows included, `IS_ACTIVE_FL` copied as is, `SORT_ORDER` copied, values re-linked to the copied types by `KEY`, `CREATED_BY` = the provisioning actor, `CREATED_AT = now()`, `UPDATED_*` left null. Plain JDBC in the tenant-insert transaction, so no `@Audited` event is written for the copied rows. | mdl/tenant/MdlTenantProvisioningContributor.java:26-55; DEVIATIONS [05] |
| NEW | RULE-MDL-007 — Optimistic locking: both tables carry `VERSION` (`@Version`), but it is not on the wire — neither response nor request DTO exposes it — so 409 `CONCURRENT_MODIFICATION` arises only between two concurrent transactions, never from a stale client copy. | V10__tenant_schema.sql:64-65; mdl/dto/LookupTypeResponse.java; mdl/dto/LookupValueResponse.java; DEVIATIONS [05] |
| NEW | RULE-MDL-008 — Audit: `@Audited(entityType = "MDL_LOOKUP_TYPE" / "MDL_LOOKUP_VALUE")` writes a `CORE_AUDIT_EVENT` row with field-level changes on CREATE and UPDATE. MDL never hard-deletes, so a deactivation is audited as an **UPDATE** of `isActiveFl` (true → false); no DELETE event is ever produced by MDL. | mdl/entity/LookupType.java:34; mdl/entity/LookupValue.java:38; erp-core/src/main/java/com/erp/audit/listener/AuditedEntityListener.java:96,115; DEVIATIONS [10] |
| NEW | RULE-MDL-009 — Report LOOKUP parameters: the report module validates a `LOOKUP` parameter against `MdlLookupApi.readActiveValuesByKey(typeKey)` (active codes only); an unknown or inactive type makes the parameter invalid (`REPORT_PARAM_INVALID` 400 on the report side, MDL's 404 never leaks). | erp-core/src/main/java/com/erp/report/service/ReportService.java:143-150; DEVIATIONS [11] |
| NEW | RULE-MDL-010 — A value may be created under an **inactive** type: `POST /lookup-types/{id}/values` checks the parent's existence only, not `isActiveFl`; no domain guard exists (the former ADR-MDL-042 was dropped as a known gap). | mdl/service/LookupValueService.java:66-67; mdl/domain/LookupValueDomain.java:31-36; docs/governance-vendoring-report.md §6 |
| NEW | RULE-MDL-011 — No protected / system flag: the seeded types (`NOTIF_CHANNEL`, `NOTIF_STATUS`, `FILE_FILE_STATUS`, `FILE_FILE_TYPE`) and their values are ordinary rows — renamable, deactivatable and extendable through the API by any holder of `PERM_MDL_LOOKUPS_UPDATE`. Deactivating one breaks the NOTIF / FILE lookup endpoints and the report LOOKUP parameters that read it (a recorded, accepted drift risk). | V8__mdl_seed.sql:16-18; mdl/entity/LookupType.java (no such column) |
| NEW | RULE-MDL-012 — No caching: no `@Cacheable` / `@CacheEvict` on any MDL service; MDL entities are absent from the cache-eligibility register, and every read hits the database. | mdl/service/LookupTypeService.java:47-51; mdl/service/LookupConsumerService.java:28-29 |
| NEW | RULE-MDL-013 — No registration SPI: a module registers a lookup type either through `POST /api/v1/mdl/lookup-types` at runtime or through a Flyway `INSERT` (core `V8`, `V13`; an application's `V1000+`); MDL exposes no Java contributor for types or values. A raw `INSERT` must itself satisfy RULE-MDL-001 (the owner row in `SEC_MODULE_REG` seeded first, `V7` before `V8`). | V8__mdl_seed.sql:5-9; mdl/crossmodule/ (only `MdlLookupApi`) |

### 3. Error codes — رموز الأخطاء
The analysis catalogued no code (it names messages in AC-MDL-001/002/007/012 and RULE-MDL-001..004); the seven MDL
codes below are the complete as-built catalogue. A code's value is also its i18n key in
`erp-core/src/main/resources/i18n/messages.properties:126-137` / `messages_ar.properties:123-134`.

| Kind | Code | HTTP | Raised at | Rule / anchor | Message (en / ar) | Source |
|---|---|---|---|---|---|---|
| NEW | `MDL-409-MODULE-NOT-REGISTERED` | 409 | `LookupTypeDomain.create` (from `LookupTypeService.create`) | RULE-MDL-001 — message identical to the rule's | "The owning module is not registered in the Security module" / «الوحدة المالكة غير مسجّلة في وحدة الأمان» | mdl/exception/MdlErrorCodes.java:27; mdl/domain/LookupTypeDomain.java:44-47 |
| NEW | `MDL-409-TYPE-DUP` | 409 | `LookupTypeDomain.create` | REQ-MDL-001 (PLATFORM-STD uniqueness; AC-MDL-001 named no message) | "This key is already in use — choose a unique key." / «هذا المفتاح مستخدم بالفعل — اختر مفتاحاً فريداً» | mdl/exception/MdlErrorCodes.java:33; mdl/domain/LookupTypeDomain.java:48 |
| NEW | `MDL-409-VALUE-DUP` | 409 | `LookupValueDomain.create` (from `LookupValueService.create`) | RULE-MDL-002 — message identical to the rule's | "This code is already used within this type" / «هذا الرمز مستخدم بالفعل ضمن هذا النوع» | mdl/exception/MdlErrorCodes.java:39; mdl/domain/LookupValueDomain.java:34 |
| NEW | `MDL-404-TYPE` | 404 | `LookupTypeService.update` / `deactivate`; `LookupValueService.create` / `search` | PLATFORM-STD not-found (unknown or other-tenant type id); no rule | "Lookup type not found" / «نوع اللوكب غير موجود» | mdl/exception/MdlErrorCodes.java:45; mdl/service/LookupTypeService.java:106,126; mdl/service/LookupValueService.java:67,178 |
| NEW | `MDL-404-VALUE` | 404 | `LookupValueService.update` / `deactivate` | PLATFORM-STD not-found (unknown or other-tenant value id); no rule | "Lookup value not found" / «قيمة اللوكب غير موجودة» | mdl/exception/MdlErrorCodes.java:51; mdl/service/LookupValueService.java:87,103 |
| NEW | `MDL-400-REORDER-MISMATCH` | 400 | `LookupValueService.reorder` | REQ-MDL-010 (an id outside the type, or an unknown id / type) | "One of the values does not belong to this type" / «إحدى القيم لا تنتمي لهذا النوع» | mdl/exception/MdlErrorCodes.java:57; mdl/service/LookupValueService.java:135-138 |
| CHANGED | `MDL-404-TYPE-KEY` | 404 | `LookupConsumerService.readByKey`; `MdlLookupApiImpl.readActiveValuesByKey` | REQ-MDL-012 **and** RULE-MDL-004: scope widened from "unknown key" to "unknown key or inactive type"; text differs from AC-MDL-012 ("No lookup type exists with this key" / «لا يوجد نوع لوكب بهذا المفتاح») | "This lookup type key does not exist or is inactive" / «مفتاح النوع غير موجود أو غير نشط» | mdl/exception/MdlErrorCodes.java:65; mdl/service/LookupConsumerService.java:54; mdl/crossmodule/MdlLookupApiImpl.java:56 |
| REMOVED | RULE-MDL-004 message "This lookup type is currently inactive" / «هذا النوع معطّل حاليًا» | — | — | folded into `MDL-404-TYPE-KEY`; no code or message key carries this text | — | i18n bundles (absent) |
| REMOVED | AC success messages «تم حفظ نوع اللوكب.» / "The lookup type has been saved." (AC-MDL-001) and «تم حفظ القيمة.» / "The value has been saved." (AC-MDL-006) | — | — | the `ApiResponse` envelope carries `success` + `data` and no per-operation message; the client renders its own | — | erp-core/src/main/java/com/erp/common/web/ApiResponse.java |
| NOT IMPLEMENTED | RULE-MDL-003 message "A lookup type's key cannot be changed after creation" / «لا يمكن تعديل مفتاح نوع اللوكب بعد إنشائه» | — | — | enforced by DTO omission (`key` absent from `LookupTypeUpdateRequest`); an unknown JSON property is ignored, so the message is never raised — exactly the rule's Test-Hint | — | mdl/dto/LookupTypeUpdateRequest.java:12-15 |
| NEW (common) | `CONCURRENT_MODIFICATION` 409 · `DATA_INTEGRITY_VIOLATION` 409 (a unique-constraint race the pre-check missed) · `VALIDATION_ERROR` 400 (bean validation, bad `page`/`size`, unknown filter field) · `ACCESS_DENIED` 403 · `METHOD_NOT_ALLOWED` 405 · `INTERNAL_ERROR` 500 · `NOT_FOUND` 404 (unknown path, 1.2.0) | — | `GlobalExceptionHandler` | shared handler, every MDL endpoint | see docs/api-docs/mdl/index.md | erp-core/src/main/java/com/erp/common/web/GlobalExceptionHandler.java; CHANGELOG [1.2.0] |

The message key `LOOKUP_KEY_NOT_FOUND` (`messages.properties:49`) is an orphan of another module's history and is
raised by no MDL code; `NOTIF_LOOKUP_KEY_UNKNOWN` / `FILE_LOOKUP_KEY_UNKNOWN` are the consumers' own translations of
`MDL-404-TYPE-KEY` (§6).

### 4. Permissions (exact authority strings) — الصلاحيات
Code-defined by `MdlPermissions implements PermissionContributor` and seeded with the same names by `V7`; the
analysis left the names to SEC ("derived from the page code").

| Kind | Authority / item | Screen / meaning | Source |
|---|---|---|---|
| CHANGED | `PERM_MDL_LOOKUPS_VIEW` | MDL_LOOKUPS VIEW — gates `POST /lookup-types/search`, `POST /lookup-types/values/search` and the HTTP consumer read `GET /lookups`. It does **not** gate the in-process `MdlLookupApi` (next rows), so B5's "granted VIEW to the consuming module's service account" is not required for in-core consumers. | mdl/permission/MdlPermissions.java:19-20; mdl/service/LookupConsumerService.java:48 |
| CHANGED | `PERM_MDL_LOOKUPS_CREATE` | MDL_LOOKUPS CREATE — gates both `POST /lookup-types` and `POST /lookup-types/{id}/values` (one screen-level grant, as B4). | mdl/permission/MdlPermissions.java:21-22 |
| CHANGED | `PERM_MDL_LOOKUPS_UPDATE` | MDL_LOOKUPS UPDATE — gates `PUT /lookup-types/{id}`, `PUT /lookup-values/{id}`, `PATCH …/reorder` **and the two deactivations** `DELETE /lookup-types/{id}`, `DELETE /lookup-values/{id}` ("deactivate is UPDATE"). | mdl/permission/MdlPermissions.java:23-27; V7__sec_seed.sql:79-80 |
| REMOVED | `PERM_MDL_LOOKUPS_DELETE` | B4 / Access summary assumed a DELETE (soft deactivate) action on MDL_LOOKUPS; it was never created — no such row in `V7` and no constant in `MdlPermissions`. | V7__sec_seed.sql:79-80,108-112; mdl/permission/MdlPermissions.java:23-27 |
| CHANGED | `PERM_MDL_TYPE_REGISTRY_VIEW` | MDL_TYPE_REGISTRY VIEW — gates `POST /lookup-types/by-owner/search`; code-defined, a separate screen gate as B4. | mdl/permission/MdlPermissions.java:28-29 |
| CHANGED | Screen labels | MDL_LOOKUPS = «قوائم البيانات المرجعية» / "Master data lookups" (analysis: «اللوكبات العامة» / "Generic Lookups"); MDL_TYPE_REGISTRY = «سجل أنواع البيانات المرجعية» / "Master data type registry" (analysis: «سجل أنواع اللوكب حسب المالك» / "Lookup-type registry by owner"). Module MDL = «البيانات المرجعية» / "Master Data Lookup" as analysed. | mdl/permission/MdlPermissions.java:31-38; V7__sec_seed.sql:37,64-65 |
| CHANGED | Role seeds | Only `SYS_ADMIN` holds the MDL module grant (tier 1), hence both screens and all four actions (tiers 2–3 derived). B4's three role kinds — platform administrator, owning-module lookup manager, consuming-module service account — collapse to `SYS_ADMIN`; no lookup-manager or service-account role is seeded, and a tenant's `SYS_ADMIN` is the super role (every catalog authority). | V7__sec_seed.sql:158-200 (rows 171, tiers 2–3); docs/steps/06-report.md |
| CHANGED | Consumer authorization | `MdlLookupApiImpl` carries **no** `@PreAuthorize`: an in-process consumer (FILE, NOTIF, REPORT) needs no MDL authority — only the caller's own gate (`isAuthenticated()` on the FILE / NOTIF lookup endpoints). Only the HTTP read `GET /lookups` is gated by VIEW. This contradicts ADR-MDL-007 ("gated by PERM_MDL_LOOKUPS_VIEW granted to the calling module's service principal") and B5's last paragraph — decision recorded in ADR-MDL-046, which supersedes ADR-MDL-007 in that part. | mdl/crossmodule/MdlLookupApiImpl.java:32-46; governance/analysis/decisions/MDL/ADR-MDL-046.md |

### 5. Entities, fields, seeded values — الكيانات والحقول والبذور
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | ENT-MDL-001 / ENT-MDL-002 | + `tenantId` (`TENANT_ID BIGINT NOT NULL`, FK `CORE_TENANT`, Hibernate `@TenantId`), + `version` (`VERSION BIGINT NOT NULL DEFAULT 0`, `@Version`); neither is on the wire. | V10__tenant_schema.sql:64-65,103-104,122-123 |
| CHANGED | Physical widths | `key VARCHAR(80)` (A3 / db-script: 50), `name_ar` / `name_en VARCHAR(150)` on both tables (200), `sort_order NUMERIC` (INTEGER) — the entity maps `NUMERIC` to `Integer`; the request DTOs validate the same limits (`@Size(max = 80 / 10 / 150)`, `@NotNull sortOrder`). | V3__mdl_schema.sql:26-29,43-46; mdl/entity/LookupValue.java:80-87; mdl/dto/LookupTypeCreateRequest.java:21-39 |
| CHANGED | `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` on both tables (the analysis stated no database default on the audit columns; the application still fills it, the default serves the raw `INSERT`s of `V8` / `V13` / provisioning). | V3__mdl_schema.sql:32,49 |
| NEW | `IDX_MDL_LOOKUP_TYPE_ACTIVE` | index on `MDL_LOOKUP_TYPE (is_active_fl)` — the analysis indexed neither flag (former ADR-MDL-009). | V3__mdl_schema.sql:113; mdl/entity/LookupType.java:41 |
| NEW | `IDX_MDL_LOOKUP_VALUE_TYPE` | index on `MDL_LOOKUP_VALUE (lookup_type_id)` next to the composite one. | V3__mdl_schema.sql:114; mdl/entity/LookupValue.java:44 |
| CHANGED | `IDX_MDL_LOOKUP_VALUE_SORT` | the composite `(lookup_type_id, sort_order)` index exists under this name (analysis: `IDX_MDL_LOOKUP_VALUE_TYPE_SORT`). | V3__mdl_schema.sql:115; mdl/entity/LookupValue.java:45 |
| REMOVED | `IDX_MDL_LOOKUP_TYPE_NAME_AR`, `IDX_MDL_LOOKUP_TYPE_NAME_EN` | never created; the name filters run unindexed. Index strategy as built: ADR-MDL-045. | V3__mdl_schema.sql:112-115 |
| CHANGED | Column comments | abbreviated to the DBF id (plus a rule pointer on `key`, `owner_module_code`, `code`) instead of the bilingual text of the db-script BLOCK 4. | V3__mdl_schema.sql:57-80 |
| NEW (seed) | `V8__mdl_seed.sql` | 4 types / 17 values inserted raw (`INSERT … nextval(…)`, `CREATED_BY = 'SYSTEM'`, PLATFORM tenant — `TENANT_ID` backfilled to 1 by `V10`): `NOTIF_CHANNEL` (owner NOTIF: EMAIL, SMS, WHATSAPP, PUSH, INTERNAL), `NOTIF_STATUS` (owner NOTIF: PENDING, SENT, FAILED, CHANNEL_DISABLED), `FILE_FILE_STATUS` (owner FILE: ACTIVE, ARCHIVED, DELETED), `FILE_FILE_TYPE` (owner FILE: IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER); the analysis (A6, db-script BLOCK 8) said no row is ever seeded by `INSERT`. | V8__mdl_seed.sql:25-65 |
| NEW (seed) | values of NOTIF-owned types | `NOTIF_STATUS`: `QUEUED` (5), `SKIPPED_NO_PROVIDER` (6); `NOTIF_CHANNEL`: `IN_APP` (6) — for every existing tenant that holds the type. | V13__notif_async_inbox.sql:66-80 |
| NOT IMPLEMENTED | `activate()` on both entities | the method exists on `LookupType` and `LookupValue` but no endpoint calls it; A7's single transition (active → inactive, no reverse) holds — ADR-MDL-005. | mdl/entity/LookupType.java:87-89; mdl/entity/LookupValue.java:104-106 |

### 6. Dependencies (A8) — التبعيات
| Kind | Direction | Item | Source |
|---|---|---|---|
| CHANGED | consumed | XM-MDL-001 (SEC `ModuleRegistry`): read as `SEC_MODULE_REG.CODE` **with** `IS_ACTIVE_FL = TRUE` through the in-process `SecModuleRegistryApi.isModuleActive(String)` (`com.erp.sec.crossmodule`), the only cross-module reference MDL holds; still a SOFT-READ with no FK. | mdl/service/LookupTypeService.java:68,80; erp-core/src/main/java/com/erp/sec/crossmodule/SecModuleRegistryApiImpl.java:36-39 |
| NEW | consumed | tenant: `CORE_TENANT` (FK of every `TENANT_ID`), `TenantContext`, provisioning SPI (`MdlTenantProvisioningContributor`, order 10 — RULE-MDL-006). | docs/steps/05-report.md; mdl/tenant/MdlTenantProvisioningContributor.java |
| NEW | consumed | SEC permission SPI: `MdlPermissions implements PermissionContributor` (module, 2 screens, 4 actions). | mdl/permission/MdlPermissions.java |
| NEW | consumed | audit: `@Audited` on both entities (RULE-MDL-008). | mdl/entity/LookupType.java:34; mdl/entity/LookupValue.java:38 |
| NEW | exposed | `com.erp.mdl.crossmodule.MdlLookupApi.readActiveValuesByKey(String typeKey)` → `List<LookupOptionView(code, labelAr, labelEn, sortOrder)>`, ordered by `sortOrder`; `@Transactional(readOnly = true)`, **no `@PreAuthorize`**; throws `LocalizedException(NOT_FOUND, MDL-404-TYPE-KEY)` for an unknown or inactive type; an active type with no active value returns an empty list. Reached by direct repository access, never through `LookupConsumerService`. The only MDL surface outside the module (ArchUnit `crossmodule` boundary). | mdl/crossmodule/MdlLookupApi.java:16-34; mdl/crossmodule/MdlLookupApiImpl.java:51-66; mdl/crossmodule/LookupOptionView.java |
| NEW | consumer | FILE — `FileLookupService` fronts `FILE_FILE_TYPE`, `FILE_FILE_STATUS` on `GET /api/v1/files/lookups/{lookupKey}` (`isAuthenticated()`), translating MDL's 404 into `FILE_LOOKUP_KEY_UNKNOWN`. | erp-core/src/main/java/com/erp/file/service/FileLookupService.java:48-58 |
| NEW | consumer | NOTIF — `NotificationLookupService` fronts `NOTIF_CHANNEL`, `NOTIF_STATUS` on `GET /api/v1/notifications/lookups/{lookupKey}` (`isAuthenticated()`), translating MDL's 404 into `NOTIF_LOOKUP_KEY_UNKNOWN`. | erp-core/src/main/java/com/erp/notif/service/NotificationLookupService.java:39-49 |
| NEW | consumer | REPORT — `ReportService` resolves the active codes of a `LOOKUP` parameter's type (RULE-MDL-009). | erp-core/src/main/java/com/erp/report/service/ReportService.java:64,143-150 |
| REMOVED | consumer | FIN — the consumer the body's examples assume (AC-MDL-001 `FIN`, AC-MDL-013, platform-summary "FIN → HARD → MDL"): `fin` was removed from erp-core in step 01; no XM-FIN-001 and no FIN-owned type exists. | docs/steps/01-report.md |
| NEW | — | No registration SPI: types and values are registered by `POST` or Flyway `INSERT` only (RULE-MDL-013). | mdl/crossmodule/ (only `MdlLookupApi`) |
| NEW | — | MDL publishes and consumes **no** domain event (`com.erp.events`); its only outbound records are the `@Audited` rows. | mdl/ (no `events` reference) |
Deviation records: `docs/DEVIATIONS.md` [05], [06], [10], [11].


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

### Refactors without behaviour change — shared helpers moved to `com.erp.common`
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no MDL behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | `LookupOptionResponse` | the FILE / NOTIF lookup endpoints that front MDL-stored keys now answer `com.erp.common.lookup.LookupOptionResponse{code, labelAr, labelEn}` (one shared class instead of a copy per module), read through `com.erp.common.lookup.OwnedLookups.read(key, ownedKeys, unknownKeyErrorCode, reader)`: the key is trimmed and upper-cased, a key outside the module's owned set and MDL's `MDL-404-TYPE-KEY` are both answered with the consumer's own 404 code. Same JSON shape; `sortOrder` is still dropped by the consumers. MDL's own `LookupOptionView` and `MdlLookupApi` are unchanged. | erp-core/src/main/java/com/erp/common/lookup/LookupOptionResponse.java; erp-core/src/main/java/com/erp/common/lookup/OwnedLookups.java:32-46; erp-core/src/main/java/com/erp/file/service/FileLookupService.java:52-58; erp-core/src/main/java/com/erp/notif/service/NotificationLookupService.java:43-49; CHANGELOG [Unreleased] |
| CHANGED | `LookupTypeDomain.create`, `LookupValueDomain.create` | the uniqueness refusals (`MDL-409-TYPE-DUP`, `MDL-409-VALUE-DUP`) are raised through `com.erp.common.domain.DomainRules.assertUnique(taken, code, arg)` instead of an inline `throw`; same status (`ALREADY_EXISTS` → 409), same code and argument — no behaviour change. | mdl/domain/LookupTypeDomain.java:48; mdl/domain/LookupValueDomain.java:34; `git diff v1.2.0 -- erp-core/src/main/java/com/erp/mdl` |
