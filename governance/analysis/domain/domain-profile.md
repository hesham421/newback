# DOMAIN PROFILE — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile         : erp (ERP Platform)
Version         : 1
Last Updated    : 2026-09-10
Status          : FRESH
Research        : 3 sources cited (block 9)
══════════════════════════════════════════════════════════════════

## 1. SCOPE

**داخل النطاق (In bounds):** بناء منصة ERP متعددة الوحدات (multi-module) بعمارة قائمة على
البيانات لا الشيفرة ("everything that can change = defined data, not code")، تُبنى بلغة
Spring (خلفية) و React (واجهة) وقاعدة بيانات **PostgreSQL** كهدف بناء جديد. النظام القديم
Oracle/ADF يبقى فقط كمصدر أحداث علوي (upstream event source) ولا يُبنى عليه أي وحدة جديدة.

دفعة العمل الحالية (هذه الجلسة) تُغطّي ثلاث وحدات أساسية مشتركة ومؤسِّسة:
1. **وحدة الأمان (SEC)** — مصادقة + تحكم هرمي بالصلاحيات (Module → Screen → Action).
2. **وحدة البيانات المرجعية (MDL/Lookup)** — مركز موحّد لكل قوائم القيم المُرمَّزة.
3. **وحدة الحسابات العامة (FIN/GL)** — دفتر أستاذ عام قابل للتوصيل (pluggable) بأي نظام مضيف.

باقي وحدات المنصة (ORG, PRC, HR, INV, SLS, CTR) معروفة الرمز والسياق ضمن `profiles/erp.yaml`
لكنها **خارج نطاق هذه الدفعة** — تُفصَّل لاحقًا بنفس الأسلوب.

**خارج النطاق صراحة (Out of bounds, stated by the user):**
- أي منطق عمل خاص بمجال مضيف (host business world) داخل وحدة الحسابات.
- تعدد العملات، تعدد الدفاتر/الكيانات، الحسابات الإحصائية، التقويم متعدد الأنماط،
  والمرفقات (attachments) — مستبعدة صراحة من GL في هذه الدفعة.
- محرك سير العمل (workflow engine) — ممنوع بحكم `profiles/erp.yaml → conventions.workflow_engine: forbidden`.
- طبقة النقل (AQ/RabbitMQ) ومستهلك الأحداث (Event consumer) والوحدة التجارية (Business Module)
  — خارج نطاق وثيقة FIN، توثَّق في مكان آخر.

## 2. PURPOSE

المنصّة تحل مشكلة تكرار وتضارب القدرات المشتركة (الهوية، الصلاحيات، البيانات المرجعية)
عبر كل وحدة أعمال جديدة: بدل أن تبني كل وحدة نظام دخول وصلاحيات وقوائم قيم خاصًا بها
(ما يُنتج ازدواجية وتضاربًا حتميًا)، تُبنى هذه القدرات **مرة واحدة، بشكل عام (generic) بالكامل**،
وتُستهلك بيانيًا من أي وحدة عمل — بدءًا بالحسابات العامة كأول وحدة عمل تستهلكها.
لكل واحدة من الوحدات الثلاث سبب وجود مستقل:
- **SEC** يحل مشكلة تشتّت الهوية والصلاحيات: نظام أمان واحد للمنصة بأكملها.
- **MDL** يحل مشكلة انحراف القيم المرجعية (reference-data drift) بين الوحدات.
- **FIN** يحل مشكلة الاعتماد على أرصدة مخزَّنة وغير موثوقة في الأنظمة القديمة، عبر دفتر
  أستاذ يُشتق كل رصيد فيه من القيود المُرحَّلة فقط، لا من عمود رصيد مُجمَّع يفقد التزامنه.

## 3. RESPONSIBILITIES

| الوحدة | تملك | لا تملك |
|---|---|---|
| SEC | تسجيل الدخول/التسجيل/استرجاع كلمة المرور، المستخدمون، الأدوار، التفويض الهرمي (Module→Screen→Action)، القائمة الديناميكية، لوحة تحكم الأمان، سجل التدقيق | أي منطق عمل خاص بوحدة أخرى |
| MDL | نوع اللوكب (Lookup Type) + قيمه (Lookup Value)، شاشة عامة واحدة Master-Detail لكل القوائم، تسجيل الملكية (namespacing) لكل وحدة | معنى القوائم الخاص بمجال وحدة أخرى — الملكية الدلالية تبقى للوحدة المسجِّلة |
| FIN | شجرة الحسابات والأبعاد، محرك القواعد (event_type → قيد)، دورة حياة القيد، دورة حياة الفترة المحاسبية، التقارير المالية المُشتقة | أي مستخدمين/أدوار/تسجيل دخول خاصة بها، أي جدول lookup خاص بها، أي معرفة بالعالم التجاري المضيف |

## 4. MAIN COMPONENTS

| # | Component | Module code | Bounded context | Category (user-defined) | Core / extension | Notes |
|---|-----------|-------------|-----------------|--------------------------|------------------|-------|
| 1 | الأمان / Security | SEC | organization | Foundation | Core | KB §1 Tier 0 — يجب أن يوجد قبل أي وحدة أعمال؛ لا وحدة تملك أمانها الخاص |
| 2 | البيانات المرجعية / Master Data Lookup | MDL | organization | Foundation | Core | KB §1 Tier 0 — مركز واحد لكل القوائم المُرمَّزة عبر المنصة |
| 3 | الحسابات العامة / Finance (General Ledger) | FIN | finance | Business — Tier 1 | Core | KB §1 Tier 1 — أول وحدة عمل تستهلك SEC وMDL؛ قابلة للتوصيل بأي مضيف |
| 4 | الهيكل التنظيمي / Organization | ORG | organization | Foundation | (غير مفصّلة هذه الدفعة) | من `profiles/erp.yaml → vocabulary.module_prefixes` — PROPOSED سابقًا في الملف الشخصي، خارج نطاق هذه الجلسة |
| 5 | المشتريات / Procurement | PRC | supply | Business — Tier 1 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 6 | الموارد البشرية / Human Resources | HR | people | Business — Tier 2 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 7 | المخزون / Inventory | INV | supply | Business — Tier 1 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 8 | المبيعات / Sales | SLS | commercial | Business — Tier 2 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 9 | العقود / Contracts | CTR | commercial | Business — Tier 2 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |

الصفوف 1–3 مصدرها خطط الوحدات الثلاث المرفقة لهذه الجلسة (security-module-plan-en.md،
lookup-module-plan-en.md، general-accounting-system-plan-en.md)؛ الصفوف 4–9 مصدرها
`profiles/erp.yaml → vocabulary.module_prefixes` المُثبَّت مسبقًا من قِبل المستخدم — تُدرَج هنا
للاكتمال المرجعي فقط ولا تُفصَّل في هذه الدفعة.

## 5. GOVERNING RULES

| # | القاعدة | المصدر |
|---|---|---|
| G1 | كل ما يمكن أن يتغير = بيانات مُعرَّفة، لا شيفرة (no hardcode / no duplication / no contradiction) | الخطط الثلاث §Governing rule؛ [KB:erp-domain-standards §2-3] |
| G2 | نظام أمان واحد للمنصة بأكملها؛ لا وحدة تملك مستخدمين/أدوار/دخولاً خاصًا بها | security-module-plan-en.md §2 |
| G3 | بوابة الوحدة (module gate) تُفحص أولاً، قبل أي فحص شاشة/إجراء؛ عدم امتلاك الوحدة = غياب تام من القائمة ومن الوصول المباشر بالرابط | security-module-plan-en.md §4.2 |
| G4 | مركز واحد وموثوق للبيانات المرجعية؛ لا وحدة تحتفظ بجداول lookup خاصة بها | lookup-module-plan-en.md §2؛ بحث: نمط "MDM hub" — راجع قسم 9 |
| G5 | التخزين والشاشة مركزيان، لكن المعنى الدلالي للقائمة يبقى ملكًا للوحدة المسجِّلة (namespacing بالمالك) | lookup-module-plan-en.md §2, §3 |
| G6 | الحسابات لا تعرف شيئًا عن العالم التجاري المضيف؛ لا اسم حساب داخل حمولة الحدث (event payload) | general-accounting-system-plan-en.md §3, §15 |
| G7 | تساوي المدين والدائن ثابت مطلق (debit = credit invariant) لكل قيد يصل POSTED | general-accounting-system-plan-en.md §12.1 |
| G8 | لا موافقة على مستوى القيد الفردي؛ نقطة التحكم البشري الوحيدة هي إغلاق الفترة، وفصل المهام بين منشئ القيد ومعتمد الإغلاق يُنفَّذ عبر SEC | general-accounting-system-plan-en.md §8.2, §10.3 |
| G9 | الأرصدة تُشتق دائمًا من القيود المُرحَّلة (POSTED) فقط، ولا يوجد عمود رصيد مُخزَّن يُعتمَد عليه | general-accounting-system-plan-en.md §11, §12.9 |
| G10 | الهدف الأول لقاعدة البيانات هو PostgreSQL؛ Oracle/ADF القديم مصدر أحداث فقط | الخطط الثلاث §1؛ توجيه GENERATION-INSTRUCTIONS.md |
| G11 | اللغتان العربية والإنجليزية إلزاميتان في كل قطعة مُسمّاة (وحدة/كيان/حقل/شاشة) | `profiles/erp.yaml → languages` |
| G12 | لا محرك سير عمل (workflow engine) في هذه المنصة | `profiles/erp.yaml → conventions.workflow_engine: forbidden` |

## 6. RELATIONSHIPS WITH OTHER DOMAINS

| This component | Depends on | Kind | Direction | Stated by |
|---|---|---|---|---|
| كل وحدة عمل (بما فيها FIN لاحقًا كل وحدة أخرى) | SEC | HARD (تسجيل الوحدة/الشاشات/الإجراءات كبيانات + بوابة الوحدة) | consumer → SEC | security-module-plan-en.md §7 |
| كل وحدة عمل (بما فيها FIN) | MDL | HARD (تسجيل أنواع lookup كبيانات + قراءة القيم) | consumer → MDL | lookup-module-plan-en.md §4 |
| FIN | SEC | HARD-FK (هوية + صلاحيات الشاشات/الإجراءات + SoD) | FIN → SEC | general-accounting-system-plan-en.md §2 |
| FIN | MDL | HARD-FK (طرق الدفع، أنواع أحداث المحاسبة، أنواع الحسابات، حالات الفترة، أنواع اليومية) | FIN → MDL | general-accounting-system-plan-en.md §5.2 |
| FIN | Notifications (جاهزة، خارج هذه الجلسة) | SOFT/EVENT — اختياري فقط | FIN → NOTIF | general-accounting-system-plan-en.md §2.3؛ `new project/integration-notifications-fileservice.md` |
| FIN | File Service (جاهزة، خارج هذه الجلسة) | SOFT/EVENT — اختياري فقط | FIN → FILESVC | general-accounting-system-plan-en.md §2.3؛ `new project/integration-notifications-fileservice.md` |
| SEC | Notifications (جاهزة) | SOFT — اختياري (مثال: بريد إعادة تعيين كلمة المرور) | SEC → NOTIF | security-module-plan-en.md §8 |
| FIN | نظام مضيف (host business system) | EVENT فقط، عبر حدث محاسبي قياسي (canonical event)؛ لا قراءة ولا كتابة مباشرة لجداول المضيف | host → FIN (event only) | general-accounting-system-plan-en.md §3 |

## 7. STEERING  (read verbatim by every later stage)

### 7.1 Ubiquitous language

| Term (ar/en) | Definition | Do not say | Module code |
|---|---|---|---|
| بوابة الوحدة / Module gate | الفحص الأول والحاسم لامتلاك الدور للوحدة قبل أي فحص شاشة/إجراء؛ غيابها = غياب تام | "صلاحية الوحدة" بمعنى فضفاض | SEC |
| منح الشاشة / Screen grant | صلاحية وصول لشاشة محددة داخل وحدة ممنوحة فعلاً | "صلاحية القائمة" | SEC |
| منح الإجراء / Action grant | صلاحية VIEW/CREATE/UPDATE/DELETE أو إجراء مخصّص على شاشة ممنوحة | "دور" (Role يبقى مصطلحًا مستقلاً) | SEC |
| الشاشة المركّبة / Composite Screen | بحث + إدخال (أو رئيسي + تفصيلي، أو معالج) تُعامَل كشاشة واحدة بمعرّف SCR واحد | "صفحة" منفردة لكل جزء | (عام) |
| نوع اللوكب / Lookup Type | الـ"master" لقائمة قيم مُرمَّزة، يملكه اسميًا موديول مُسجِّل | "جدول Enum" | MDL |
| قيمة اللوكب / Lookup Value | الـ"detail" — قيمة مُرمَّزة ضمن نوع لوكب: كود، تسميتان، ترتيب، حالة نشاط | "قيمة ثابتة" في الشيفرة | MDL |
| الحدث المحاسبي القياسي / Canonical accounting event | مدخل الحسابات الوحيد؛ حدث جاهز الشكل قادم من مستهلك الأحداث خارج النطاق | "معاملة تجارية" (يحمل دلالة عالم المضيف) | FIN |
| شجرة الحسابات / Chart of Accounts | بنية هرمية للحسابات؛ الأوراق فقط تقبل ترحيلاً مباشرًا | "دليل حسابات" بلا بنية هرمية | FIN |
| البُعد / Dimension | مقطع بيانات (segment) يُعرَّف كبيانات ليُشكّل مع الحساب الأساسي تركيبة الترحيل | "تصنيف تحليلي" غامض | FIN |
| الترحيل / Posting | إدخال قيد بحالة POSTED مؤثرًا في الأرصدة وغير قابل للتعديل | "اعتماد" (الاعتماد مصطلح مختلف يخص إغلاق الفترة) | FIN |
| قيد اليومية / Journal Entry | وحدة الإدخال المحاسبي الأساسية (رأس + سطور متوازنة مدين/دائن) | "سند" فقط دون تحديد | FIN |
| الفترة المحاسبية / Accounting Period | نافذة زمنية بحالة (Open / Soft Close / Hard Close / Year-End Close) تتحكم بقبول الترحيل | "شهر مالي" | FIN |
| قيد العكس / Reversing Entry | تصحيح عبر قيد جديد مرتبط ثنائي الاتجاه بالأصل، مطابق سطرًا بسطر بعكس الاتجاه | "حذف القيد" (ممنوع) | FIN |
| ميزان المراجعة / Trial Balance | تقرير متوازن دائمًا، مُشتق من القيود المُرحَّلة فقط | "كشف حساب" | FIN |

### 7.2 Bounded contexts

| Context | Owns module codes | Boundary statement |
|---|---|---|
| organization | ORG, SEC, MDL | القدرات المؤسِّسة (Tier 0) التي يعتمد عليها أي سياق آخر: الهيكل التنظيمي، الأمان، البيانات المرجعية |
| supply | PRC, INV | تدفقات التوريد والمخزون (خارج نطاق هذه الدفعة) |
| finance | FIN | الحسابات العامة ودفتر الأستاذ؛ تستهلك organization ولا تُنتج له شيئًا |
| people | HR | الموارد البشرية (خارج نطاق هذه الدفعة) |
| commercial | SLS, CTR | المبيعات والعقود (خارج نطاق هذه الدفعة) |

(منقولة حرفيًا من `profile.vocabulary.bounded_contexts`)

### 7.3 Module prefixes proposal

| Code | Display | Status |
|---|---|---|
| ORG | Organization | IN PROFILE |
| SEC | Security | IN PROFILE — هذه الدفعة |
| MDL | Master Data Lookup | IN PROFILE — هذه الدفعة |
| PRC | Procurement | IN PROFILE |
| FIN | Finance | IN PROFILE — هذه الدفعة |
| HR | Human Resources | IN PROFILE |
| INV | Inventory | IN PROFILE |
| SLS | Sales | IN PROFILE |
| CTR | Contracts | IN PROFILE |

كل الرموز موجودة مسبقًا في `profiles/erp.yaml → vocabulary.module_prefixes`؛ لا رمز جديد اقترحته
هذه الجلسة.

### 7.4 Identifier rules

المعرّفات اللاحقة تُبنى بالصيغة `{prefix}-{MOD}-{seq}` بعرض تسلسل 3 خانات
(`factory.ids.pattern`, `factory.ids.seq_width`). أنواع الكيانات: master, transactional,
lookup, config, security (`profile.vocabulary.entity_kinds`).

### 7.5 Knowledge sources to cite

- `profiles/erp/knowledge/erp-domain-standards.md`
- `new project/security-module-plan-en.md` — المصدر التأسيسي لوحدة SEC
- `new project/lookup-module-plan-en.md` — المصدر التأسيسي لوحدة MDL
- `new project/general-accounting-system-plan-en.md` — المصدر التأسيسي لوحدة FIN، بما فيه القسم
  12 "details the analysis agent MUST honor" المُلزِم لكل تحليل لاحق
- `new project/integration-notifications-fileservice.md` — يُستخدم فقط عند حاجة فعلية مذكورة
  صراحة في إحدى الخطط الثلاث (لا يُعاد تصميمه)
- plus the research sources in block 9

## 8. RESOLVED DECISIONS

| # | Point | Decision | Recommended by dialogue? | Confirmed by user | Sources |
|---|---|---|---|---|---|
| 1 | هدف قاعدة البيانات | PostgreSQL هو الهدف الوحيد لهذا البناء؛ Oracle/ADF يبقى مصدر أحداث فقط | لا — منصوص صراحة | نعم — منصوص في الخطط الثلاث وفي أمر التنفيذ | الخطط الثلاث §1؛ GENERATION-INSTRUCTIONS.md |
| 2 | نموذج الأمان | RBAC هرمي بثلاث مستويات (Module→Screen→Action) بديلاً عن أي أمان محلي بالوحدات | لا — منصوص صراحة | نعم | security-module-plan-en.md §4 |
| 3 | نموذج البيانات المرجعية | مركز lookup عام Master-Detail واحد لكل المنصة، بدل جداول lookup محلية | لا — منصوص صراحة | نعم | lookup-module-plan-en.md §2-3 |
| 4 | نموذج اعتماد قيود المحاسبة | لا اعتماد على مستوى القيد الفردي؛ الاعتماد الوحيد عند إغلاق الفترة فقط | لا — منصوص صراحة، ويتوافق مع ممارسات GL الحديثة القائمة على الأحداث (انظر قسم 9، R1) | نعم | general-accounting-system-plan-en.md §8 |
| 5 | نطاق هذه الدفعة | SEC ثم MDL ثم FIN فقط، بهذا الترتيب الصارم؛ باقي الوحدات خارج النطاق الآن | لا — منصوص صراحة | نعم | GENERATION-INSTRUCTIONS.md §3 |
| 6 | استخدام Notifications/File Service | تكامل اختياري بحت، فقط عند حاجة صريحة يذكرها أحد الخطط الثلاث؛ لا إعادة تصميم لهما | لا — منصوص صراحة | نعم | الخطط الثلاث §8/§5/§2.3؛ GENERATION-INSTRUCTIONS.md §4.6 |

## 9. RESEARCH LOG

| # | Point | What established systems do | Source(s) (title, URL/path, date) | Used in |
|---|---|---|---|---|
| R1 | RBAC هرمي بمستوى module→screen→action | الأنظمة الناضجة تفصل "ما الذي يمكن فعله" (action) عن "أين" (scope/module)، وتستخدم الهرمية لتقليل تكرار الأدوار؛ التفويض الهرمي يُستخدم بحيث تتحكم صلاحية بوحدة كاملة وأخرى بإجراء داخلها | [How to Design an RBAC System — NocoBase](https://www.nocobase.com/en/blog/how-to-design-rbac-role-based-access-control-system), accessed 2026-09-10; [Access Control Design for Scalable RBAC Systems](https://www.loginradius.com/blog/identity/design-effective-rbac-system), accessed 2026-09-10 | G3، §7.1 "بوابة الوحدة" |
| R2 | مركز بيانات مرجعية موحّد (MDM hub) | النمط الشائع هو مركز واحد (hub) يخزن البيانات المرجعية/الأساسية ويُنشرها للأنظمة الأخرى؛ النطاقات (domains) تتوافق مع بيانات مرجعية مُدارة مركزيًا بدل نسخ محلية متضاربة | [Why the Data Hub is the Future of Data Management — Semarchy](https://www.semarchy.com/blog/backtobasics-mdm-hub-patterns/), accessed 2026-09-10 | G4/G5، وحدة MDL بأكملها |
| R3 | محاسبة قائمة على الأحداث (event-driven GL) | الأنظمة الحديثة تدمج دفتر الأستاذ مع معمارية قائمة على الأحداث: مصدر يُصدر معاملات، خدمة تحقق/تطبيع، ثم خدمة ترحيل تطبّق قاعدة القيد المزدوج وتُلحق القيود في مخزن إلحاقي فقط (append-only)؛ من التحديات الشائعة الأحداث المكرَّرة التي تُسبب ترحيلاً مزدوجًا | [General Ledger Postings: A Comprehensive Guide — Dualentry](https://www.dualentry.com/blog/general-ledger-postings), accessed 2026-09-10 | G7/G9، §12.12 "idempotency at the boundary" في خطة FIN |

## 10. OPEN ITEMS

لا يوجد — النطاق محدَّد بالكامل من الخطط الثلاث المرفقة وتوجيهات GENERATION-INSTRUCTIONS.md؛
لا نقطة غموض تتطلب حوارًا إضافيًا مع المستخدم في هذه المرحلة.
══════════════════════════════════════════════════════════════════
