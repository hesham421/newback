# FLOW DIAGRAM — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp   Stage : P3.2 (Part A — UX design)
Sources: srs-mdl.md (functional ceiling — REQ/AC/SCR-REQ) · prd-mdl.md (priority and intent)
Screens: SCR-MDL-001, SCR-MDL-002 — two screens, four navigation paths
══════════════════════════════════════════════════════════════════

## The screens these flows move between

| SCR | الاسم / Name | Page code | SRS screen requirement |
|---|---|---|---|
| SCR-MDL-001 | اللوكبات العامة / Generic Lookups | MDL_LOOKUPS | SCR-REQ-MDL-001 |
| SCR-MDL-002 | سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner | MDL_TYPE_REGISTRY | SCR-REQ-MDL-002 |

Each is one screen with one page code, its sub-views included — the search and the entry of both
levels of the management screen are sub-views under `SCR-MDL-001`, not screens of their own
(`profile.conventions.composite_screen`; SRS Part B states the same in each `Composite` line).

Navigation into both starts at the platform menu entry البيانات المرجعية / Master Data Lookup →
اللوكبات / Lookups, which the SRS `Navigation` lines of both screen requirements name.

---

## FLOW — تسجيل نوع لوكب / Register a lookup type          traces=US-MDL-001,US-MDL-004,REQ-MDL-001,REQ-MDL-002,SCR-MDL-001
```
Screens   : SCR-MDL-001
Sequence  : القائمة الرئيسية / main menu → SCR-MDL-001 بحث الأنواع / type search
            → SCR-MDL-001 نموذج نوع جديد / new-type entry → حفظ / save
            → SCR-MDL-001 بحث الأنواع، والنوع الجديد ظاهر فيه / back to the type search, the new type in it
Trigger   : مُكامِل وحدة يسجّل قائمة قيم جديدة بيانات لا شيفرة /
            an integrator registers a new coded list as data, not as code
Priority  : HIGH (US-MDL-001, US-MDL-004)
```
الحفظ يرفض رمز وحدة مالكة لا صفّ لها في سجل وحدات الأمان (REQ-MDL-002)، فالمسار يعود إلى النموذج
نفسه برسالة الحقل لا إلى شاشة أخرى / a rejected owner module code (REQ-MDL-002) returns to the same
entry sub-view with the field message — it is not a navigation step.

## FLOW — تنقيح نوع قائم / Maintain an existing type       traces=US-MDL-001,REQ-MDL-003,REQ-MDL-004,SCR-MDL-001
```
Screens   : SCR-MDL-001
Sequence  : SCR-MDL-001 بحث الأنواع / type search → اختيار نوع / select a type
            → SCR-MDL-001 نموذج تعديل النوع / type edit sub-view → حفظ أو تعطيل / save or deactivate
            → SCR-MDL-001 بحث الأنواع / back to the type search
Trigger   : تغيّر تسمية قائمة، أو إخراجها من الخدمة /
            a list is renamed, or taken out of service
Priority  : HIGH (US-MDL-001)
```
المفتاح غير قابل للتعديل (RULE-MDL-003) والتعطيل لا رجعة له من هذه الشاشة (REQ-MDL-004) — كلاهما
يُقال في الشاشة لا في مسار تنقّل / the key is immutable and deactivation is one-way from this
screen; both are said on the screen, neither is a separate path.

## FLOW — إدارة قيم النوع / Manage a type's values         traces=US-MDL-002,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,SCR-MDL-001
```
Screens   : SCR-MDL-001
Sequence  : SCR-MDL-001 بحث الأنواع / type search → اختيار النوع / select the type
            → SCR-MDL-001 جزء القيم التفصيلي / the values pane of the selected type
            → SCR-MDL-001 نموذج قيمة (إضافة أو تعديل) / value entry sub-view (add or edit)
            → حفظ / save → جزء القيم / back to the values pane
Trigger   : صاحب قائمة يضيف قيمة، أو يعدّل تسمية، أو يعطّل قيمة، أو يعيد الترتيب /
            a list's owner adds a value, revises a label, deactivates a value, or reorders
Priority  : HIGH — الخطة تسمّي هذه الشاشة آلية الوحدة الجوهرية / the plan names this the module's
            core mechanism (US-MDL-002)
```
التعطيل وإعادة الترتيب إجراءان على الصفّ داخل جزء القيم ولا يفتحان سطحًا ثانيًا (REQ-MDL-009,
REQ-MDL-010) / deactivating a value and reordering are row actions inside the values pane; neither
opens a second surface.

## FLOW — تدقيق السجل حسب المالك / Audit the registry by owner   traces=US-MDL-005,REQ-MDL-013,SCR-MDL-002,SCR-MDL-001
```
Screens   : SCR-MDL-002, SCR-MDL-001
Sequence  : القائمة الرئيسية / main menu → SCR-MDL-002 السجل مجمّعًا حسب الوحدة المالكة /
            the registry grouped by owner module → اختيار نوع للفتح / pick a type to open
            → SCR-MDL-001 مع النوع مختارًا / SCR-MDL-001 with that type selected
Trigger   : منسّق المنصة يراجع قوائم وحدة بعينها في نظرة واحدة /
            a platform administrator reviews one module's lists at a glance
Priority  : MEDIUM (US-MDL-005)
```
المسار في اتجاه واحد: السجل يقرأ ولا يكتب (SRS §B3)، والانتقال منه إلى الشاشة العامة هو المكان
الوحيد الذي يُكتب فيه / the path is one-way: the registry reads and never writes, and the step into
the management screen is the only place anything is written. The target route carries its own
guard, so a reviewer who may browse but not manage is refused at the door rather than shown a
dead link.

---

## قصة بلا مسار تنقّل / A story with no navigation path

`US-MDL-003` — قراءة القيم الفعّالة بالمفتاح من أي وحدة مستهلكة / read active values by key from a
consuming module — carries **no flow**. Its caller is a consuming module's backend over the
platform's in-process interface, not a user at a screen: `REQ-MDL-011` and `REQ-MDL-012` are
written from the caller's side, and the SRS lists the operation under `SCR-REQ-MDL-002` §B5
because that is where the consumer contract is recorded, not because the registry screen calls
it (ADR-MDL-007). Inventing a "preview this type's values" screen for it would be inventing
navigation the SRS does not have — §A.2 forbids exactly that.

What a user can observe of that read is indirect and already on `SCR-MDL-001`: the values a
consumer receives, and the active flags that decide which of them are returned (RULE-MDL-004).

## تغطية القصص / Story coverage

| US | Flow | SCR |
|---|---|---|
| US-MDL-001 | تسجيل نوع لوكب · تنقيح نوع قائم / register, maintain | SCR-MDL-001 |
| US-MDL-002 | إدارة قيم النوع / manage a type's values | SCR-MDL-001 |
| US-MDL-003 | — بلا مسار: قراءة خادم لخادم / none: a server-to-server read (ADR-MDL-007) | — |
| US-MDL-004 | تسجيل نوع لوكب / register a lookup type | SCR-MDL-001 |
| US-MDL-005 | تدقيق السجل حسب المالك / audit the registry by owner | SCR-MDL-002 → SCR-MDL-001 |

أربعة مسارات، كلٌّ منها يبدأ بشاشة تقرّها الوحدةُ في SRS ويستشهد بقصة وبمتطلَّب / four flows, each
starting at a screen the SRS declares and each citing a story and a requirement. لا مسار بلا شاشة
مسنودة بالمواصفة / no flow rests on a screen the SRS does not have.
══════════════════════════════════════════════════════════════════
