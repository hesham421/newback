# PRD — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module          : SEC     Version : v2   (delta on baseline v1)
Source artifacts: platform-summary, module-registry, business-policies, change-manifest CS-SEC-001
Stories         : 7 added in v2 (19 in total with v1)   Policies covered : 13/13 added in v2 (24/24 overall)   Deferred : 0
Status          : APPROVED — approved 2026-09-23 by hesham421 (recorded in srs-sec.md A1; G4 fix — this header previously read DRAFT/PENDING after approval had already been recorded)
══════════════════════════════════════════════════════════════════
تُعرض هنا القصص المُضافة في v2 فقط. قصص v1 من الأولى إلى الثانية عشرة باقية كما هي دون تعديل،
وتسري على حساب الخدمة كما تسري على أي أساس أمني (معيار القبول 2 في مانيفست التغيير v2).

## USER STORIES

US-SEC-013
  Title          : إدارة حسابات الخدمة / Manage service accounts
  Story          : بصفتي مسؤول الأمان، أحتاج إلى إنشاء حسابات الخدمة بنفسي من شاشات إدارة الأمان القائمة، وأن أراها فيها مميَّزةً عن المستخدمين البشريين، كي يكون كل مُستدعٍ آلي في المنصة مُنشأً بقرار إداري وظاهرًا حيث أدير غيره، ولا يحمل عند إنشائه صلاحية لم أمنحها بعد. (تعطيل الحساب وأثره تحمله US-SEC-017.)
  Priority       : HIGH — implied (acceptance 1: "administered … like any other principal"; manifest §1: "visible … from SEC's existing administration screens")
  Success metric : —
  Traces         : POL-SEC-018, POL-SEC-022, POL-SEC-013
  Source         : مانيفست التغيير v2 §1، معيار القبول 1؛ module-registry-sec.md → ENTITIES OWNED (User — MODIFIED)، قرار الحوار #2
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-014
  Title          : إسناد الأدوار والمنح لحساب الخدمة / Assign a service account its roles and grants
  Story          : بصفتي مسؤول الأمان، أحتاج إلى إسناد الأدوار والمنح لحساب الخدمة بإدارة الأدوار والمنح نفسها التي أستعملها للمستخدمين، كي يحمل المُستدعي الآلي الصلاحية التي يحتاجها عمله فقط (لمستهلك أحداث Oracle: إنشاء قيد يومية في FIN ولا شيء غيره)، وتسري عليه كل سياسات التفويض القائمة كما تسري على أي أساس أمني.
  Priority       : HIGH — stated (manifest §1: "exactly one … and nothing else"; acceptance 1)
  Success metric : يُصادَق المُستدعي الآلي حاملًا صلاحية واحدة بالضبط، هي إنشاء قيد يومية في FIN (معيار القبول 1) — تُعدّ منح إجراء واحدًا؛ منح وحدة FIN وشاشة القيد هو المسار البنيوي إلى ذلك الإجراء لا صلاحية إضافية (business-policies-sec.md قرار الحوار #4). **تصحيح لاحق (ADR-SEC-034)**: بوابة VIEW جزء من المسار
                   البنيوي نفسه، فدور المستهلك هو وحدة FIN + شاشة القيد + VIEW + CREATE؛ "صلاحية واحدة" تعدّ القدرة
                   التجارية الواحدة لا عدد المنح. يقرأ ذلك أن المُستدعي يستطيع أيضًا قراءة قيود FIN والبحث فيها.
  Traces         : POL-SEC-012, POL-SEC-013
  Source         : مانيفست التغيير v2 §1 ("hold permissions like any other principal … exactly one")، معيار القبول 1–2؛ business-policies-sec.md قرار الحوار #4؛ platform-summary → DEPENDENCY MAP (ADDED)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-015
  Title          : مصادقة آلية مستمرة دون إشراف / Unattended, continuous machine authentication
  Story          : بصفتي مُستدعيًا آليًا (مثل مستهلك أحداث Oracle)، أحتاج إلى المصادقة على المنصة ببيانات اعتماد حساب الخدمة دون دخول تفاعلي ودون الاستناد إلى جلسة أي مستخدم بشري، وأن تبقى قدرتي على المصادقة قائمة طوال تشغيلي، كي لا يسقط التكامل بصمت حين تنتهي الجلسات أو تُنظَّف دوريًا.
  Priority       : HIGH — stated (manifest: "it will break the integration if it is skipped")
  Success metric : يبقى المُستدعي الآلي قادرًا على المصادقة طوال تشغيل البرنامج الخدمي دون أن يعيد أي شخص مصادقته (مانيفست v2 §1: "survives unattended for as long as the service runs")
  Traces         : POL-SEC-014, POL-SEC-015
  Source         : مانيفست التغيير v2 — Summary و§1، معيار القبول 1؛ module-registry-sec.md قرارا الحوار #1 و#3؛ platform-summary → OVERVIEW
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-016
  Title          : إصدار بيانات اعتماد حساب الخدمة وتدويرها / Issue and rotate service-account credentials
  Story          : بصفتي مسؤول الأمان، أحتاج إلى إصدار بيانات اعتماد لحساب الخدمة أتسلّم سرّها مرة واحدة عند الإصدار لأسلّمه للبرنامج الخدمي، مطمئنًا إلى أن المنصة لا تحتفظ به بصيغة يُستعاد منها، وإلى إصدار بيانات اعتماد ثانية قبل إلغاء الأولى، كي أبدّل السرّ دون إيقاف التكامل ودون أن يُكشف السرّ لاحقًا من شاشة أو من مخزن البيانات.
  Priority       : —
  Success metric : —
  Traces         : POL-SEC-020, POL-SEC-021, POL-SEC-023
  Source         : business-policies-sec.md قرارا الحوار #1 و#2 (والتنقيح #5-ب)؛ module-registry-sec.md → ENTITIES OWNED (ServiceAccountCredential — ADDED)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-017
  Title          : إلغاء بيانات الاعتماد وتعطيل حساب الخدمة / Revoke a credential or deactivate a service account
  Story          : بصفتي مسؤول الأمان، أحتاج إلى إلغاء بيانات اعتماد بعينها أو تعطيل حساب الخدمة كله، وأن يسري ذلك من الطلب التالي على كل ما صدر عنها، بما في ذلك رمز وصول صدر قبل الإلغاء، كي أقطع فورًا مُستدعيًا آليًا مخترقًا أو مُستغنى عنه دون نافذة ثقة متبقية.
  Priority       : HIGH — stated (manifest §1: "visible and revocable")
  Success metric : —
  Traces         : POL-SEC-017
  Source         : مانيفست التغيير v2 §1؛ module-registry-sec.md قرار الحوار #3 وAUTO-DECISIONS (USER_STATUS: ACTIVE / DISABLED)؛ business-policies-sec.md قرار الحوار #5 (ج)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-018
  Title          : لا استخدام تفاعلي لحساب الخدمة / No interactive use of a service account
  Story          : بصفتي مسؤول الأمان، أحتاج إلى أن يبقى حساب الخدمة هوية آلية فقط، فلا يُدخَل بها تفاعليًا ولا تُعاد تعيين كلمة مرورها، كي لا يفتح المُستدعي الآلي سطح هجوم تفاعليًا لا حاجة له.
  Priority       : —
  Success metric : —
  Traces         : POL-SEC-016
  Source         : business-policies-sec.md قرار الحوار #2 والتنقيح #5 (أ)؛ security-module-plan-en.md §3 (مستشهَد به في القرار #2)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-019
  Title          : تدقيق نشاط حساب الخدمة / Audit service-account activity
  Story          : بصفتي مسؤول الأمان، أحتاج إلى أن أرى في سجل التدقيق القائم كل مصادقة ناجحة أو فاشلة لحساب الخدمة، وكل إصدار أو إلغاء لبيانات اعتماده، وكل تغيير في أدواره أو منحه، منسوبًا إلى حساب الخدمة ذاته ومحفوظًا كغيره دون تعديل، كي أتتبّع نشاط المُستدعي الآلي كما أتتبّع نشاط أي أساس أمني.
  Priority       : HIGH — stated (acceptance 1: "administered and audited like any other principal")
  Success metric : —
  Traces         : POL-SEC-019, POL-SEC-009
  Source         : مانيفست التغيير v2 §1 ("its activity shows up in the audit log like any other principal's")؛ business-policies-sec.md قرار الحوار #3؛ module-registry-sec.md → AUTO-DECISIONS (AUDIT_EVENT_TYPE)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-006
  Title          : سجل الوحدة/الشاشة/الإجراء / Module / screen / action registry
  Story          : As a consuming module's integrator, I need to register my module, its screens and its actions as data, so that my module can be granted to roles without any change to security code.
  Priority       : HIGH — foundational to every other module's onboarding
  Success metric : —
  Traces         : POL-SEC-001, POL-SEC-002
  Source         : security-module-plan-en.md §4.3, §4.5, §7; module-registry-sec.md → ENTITIES OWNED (ModuleRegistry, ScreenRegistry, ActionRegistry)؛ MODIFIED v2 — أثر فقط، بلا تغيير سلوك (C4.4)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-011
  Title          : إدارة الجلسات النشطة / Active sessions management
  Story          : As a security administrator, I need to see currently signed-in users and force-terminate a session when authorized, so that I can respond to a compromised or abandoned session.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-SEC-011, POL-SEC-009
  Source         : security-module-plan-en.md §5.1, §5.3; module-registry-sec.md → ENTITIES OWNED (ActiveSession)؛ MODIFIED v2 — أثر فقط، بلا تغيير سلوك (C4.4)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-012
  Title          : إشعار اختياري عبر خدمة الإشعارات / Optional notification on password reset
  Story          : As a user requesting a password reset, I need to optionally receive that reset through the platform's ready Notifications service, so that I am not blocked if this integration is skipped.
  Priority       : LOW — explicitly "only on real need", never a hard dependency
  Success metric : —
  Traces         : POL-SEC-024
  Source         : security-module-plan-en.md §8; new project/integration-notifications-fileservice.md §1؛ MODIFIED v2 — أثر فقط، بلا تغيير سلوك؛ POL-SEC-024 يحمل الشرط الذي كانت القصة تنصّ عليه نصًّا (C4.4)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-SEC-013 | POL-SEC-018, POL-SEC-022, POL-SEC-013 | مانيفست v2 §1، معيار القبول 1 |
| US-SEC-014 | POL-SEC-012, POL-SEC-013 | مانيفست v2 §1، معيار القبول 1–2 |
| US-SEC-015 | POL-SEC-014, POL-SEC-015 | مانيفست v2 Summary، §1 |
| US-SEC-016 | POL-SEC-020, POL-SEC-021, POL-SEC-023 | business-policies-sec.md قرارا الحوار #1، #2 |
| US-SEC-017 | POL-SEC-017 | مانيفست v2 §1؛ module-registry-sec.md قرار الحوار #3 |
| US-SEC-018 | POL-SEC-016 | business-policies-sec.md قرار الحوار #2 |
| US-SEC-019 | POL-SEC-019, POL-SEC-009 | مانيفست v2 §1؛ business-policies-sec.md قرار الحوار #3 |
| US-SEC-006 | POL-SEC-001, POL-SEC-002 | قصة v1 بلا أثر؛ السجل هو ما تعمل عليه بوابة الوحدة ومنع المنح اليتيم (C4.4) |
| US-SEC-011 | POL-SEC-011, POL-SEC-009 | قصة v1 بلا أثر؛ "when authorized" هي بوابة الصلاحية، والإنهاء القسري يُدقَّق (C4.4) |
| US-SEC-012 | POL-SEC-024 | قصة v1 بلا أثر؛ POL-SEC-024 تنصّ على القاعدة التي حملتها القصة نصًّا (C4.4) |
كل سياسة من سياسات v2 تظهر في صف واحد على الأقل: 012 ← 014؛ 013 ← 013، 014؛ 014 و015 ← 015؛
016 ← 018؛ 017 ← 017؛ 018 ← 013؛ 019 ← 019؛ 020 و021 و023 ← 016؛ 022 ← 013.
سياسات v1 (من 001 إلى 011) تبقى مغطاة بقصص v1 كما في جدول تتبع v1. POL-SEC-024 سياسة v1 مفقودة
كشفها C4.4 عند هذه الدلتا، وتغطيها US-SEC-012.

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | توسيع قصص v1 أم إضافة قصص جديدة / Extend v1 stories or add new ones | قصص جديدة من US-SEC-013 إلى US-SEC-019؛ قصص v1 تبقى UNCHANGED | Recommended by dialogue round 1; accepted unchanged in rounds 2–3 — converged | ENGINE §2 SEQUENCE RULE؛ مانيفست v2 (Change type: ADDITIVE؛ معيار القبول 2) |
| 2 | دور قصة المصادقة الآلية / Role of the unattended-authentication story | "المُستدعي الآلي / machine caller" بنص المدخلات، على سابقة الدور غير البشري في US-SEC-007 | Recommended by dialogue round 1; accepted unchanged in rounds 2–3 — converged | platform-summary → OVERVIEW؛ module-registry-sec.md → DEPENDENCIES؛ PRD v1 US-SEC-007 |
| 3 | لوحة التحكم وشاشة الجلسات لحسابات الخدمة / Dashboard and sessions views | لا قصة مستقلة: اللوحة من شاشات الإدارة القائمة التي تغطيها US-SEC-013 (POL-SEC-018)؛ حساب الخدمة لا ينشئ جلسة فلا يمسّ US-SEC-011؛ تفصيل الأرقام حسب نوع الأساس الأمني من شأن P1 | Recommended by dialogue round 1; accepted unchanged in rounds 2–3 — converged | POL-SEC-018؛ module-registry-sec.md قرار الحوار #3؛ ENGINE §3 DO NOT EXTRACT |
| 4 | قصص مؤجلة لاستثناءات النطاق / Deferred stories for P0 scope exceptions | لا قصص مؤجلة؛ الاستثناءات قرارات حُسمت في P0 لا أسئلة معلّقة | Recommended by dialogue round 1; accepted unchanged in rounds 2–3 — converged | ENGINE §4 ("an item the user will not decide")؛ business-policies-sec.md → SCOPE EXCEPTIONS |
| 5 | أولويات قصص v2 / Priorities of the v2 stories | HIGH حيث ينصّ عليها معيار القبول 1 أو مانيفست §1 (013، 014، 015، 017، 019)؛ "—" لما اشتُق من قرارات الحوار وحدها (016، 018) | Recommended by dialogue round 1; accepted unchanged in rounds 2–3 — converged | ENGINE §2 (Priority "only if stated or clearly implied")؛ مانيفست v2 §1، معيار القبول 1 |
| 6 | موضع تعطيل حساب الخدمة / Where deactivation lives | في US-SEC-017 وحدها (الإلغاء والتعطيل وأثرهما)؛ US-SEC-013 للإنشاء والظهور المميَّز فقط — لئلا يولّد P1 متطلبين للحاجة نفسها | Recommended by dialogue round 3 — converged | POL-SEC-017؛ POL-SEC-018؛ مانيفست v2 §1 ("visible and revocable") |
| 7 | "صلاحية واحدة بالضبط" في مقياس نجاح US-SEC-014 / "Exactly one permission" in US-SEC-014's metric | يُعدّ منح إجراء واحدًا؛ منح الوحدة والشاشة مسار بنيوي لا صلاحية إضافية — وإلا تعارض المقياس مع POL-SEC-001 وPOL-SEC-002 | Recommended by dialogue round 3 — converged | business-policies-sec.md قرار الحوار #4؛ POL-SEC-001؛ POL-SEC-002؛ security-module-plan-en.md §4.2 |

## DEFERRED
| US | Reason | Activation trigger |
|---|---|---|
None — لم تؤجَّل أي قصة. استثناءات نطاق v2 (الانتهاء الإلزامي لبيانات الاعتماد؛ هوية أحمال العمل وmTLS؛
تسجيل كل استدعاء أعمال داخل سجل SEC؛ الطابور والمستهلك وOracle؛ تغييرات FIN) قرارات إقصاء محسومة في
business-policies-sec.md → SCOPE EXCEPTIONS وplatform-summary → DEFERRED، لا بنود معلّقة (القرار #4 أعلاه).

## APPROVAL
Approved by : hesham421   Date : 2026-09-23
Once approved, no stage may raise a question; P1 onward self-resolve
per the ambiguity rule (shared/GOVERNANCE-CORE.md).
══════════════════════════════════════════════════════════════════
