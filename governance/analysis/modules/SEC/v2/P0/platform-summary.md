# PLATFORM SUMMARY — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile : erp   Domain profile : v1   Registry : v1.3.1
Delta   : v2 على خط الأساس v1 (SEC) — تُعرض هنا الأقسام المتغيرة فقط؛ ما لم يُذكر يبقى كما في v1.
══════════════════════════════════════════════════════════════════

## OVERVIEW
(MODIFIED — فقرة مُضافة؛ فقرة v1 تبقى كما هي)
الإصدار الثاني لوحدة الأمان يسدّ فجوة واحدة: لا تعرف SEC في v1 أي مُستدعٍ غير بشري. يُبنى الآن
برنامج خدمي (daemon) يستهلك أحداث الأعمال من نظام Oracle القديم ويستدعي FIN باستمرار ودون
إشراف بشري. رمز الوصول في v1 ينتهي بعد ساعة، وكل طلب يُتحقق منه مقابل صف جلسة حيّ؛ فلو
شُغِّل البرنامج بهوية مستخدم بشري لأسقط التنظيفُ الدوري للجلسات التكاملَ بصمت. يضيف v2
مفهوم **حساب الخدمة / Service account**: أساس أمني (principal) آلي يُصادَق دون جلسة دخول بشرية،
يحمل الأدوار والمنح كأي مستخدم، ويُدار ويُلغى ويُدقَّق من شاشات إدارة الأمان القائمة. كل ثوابت
SEC في v1 موروثة كما هي ولا يُعاد فتحها. [مانيفست التغيير v2 §1، معايير القبول 1–2]

## MODULES
(MODIFIED — الصفوف المتغيرة فقط؛ الصفوف 1.1، 2.1، 2.3، 3.1، 3.2، 3.3 دون تغيير — NEW، خارج الدفعة)
| #   | Code | Module (ar/en) | Bounded context | Layer | Type | Depends on | Status |
|-----|------|--------|-----------------|-------|------|------------|--------|
| 1.2 | SEC | الأمان / Security | organization | L1 | security engine | ROOT | EXISTING — v2 يوسّعها (حساب الخدمة) |
| 1.3 | MDL | البيانات المرجعية / Master Data Lookup | organization | L1 | reference | ROOT | EXCEPTION — تجاوزت P0 (v1 pass-1 مكتمل)، تُقرأ كما هي |
| 2.2 | FIN | الحسابات العامة / Finance (GL) | finance | L3 | transactional/reporting | SEC (هوية وتفويض عبر المعترِض القياسي، بلا FK مادي)، MDL (SOFT-READ) | EXCEPTION — تجاوزت P0 (v1 pass-1 مكتمل)، تُقرأ كما هي |
Status: NEW (Phase 2 produces) · EXISTING (Phase 2 extends) · EXCEPTION (read as-is)
Numbering: [tier].[sequence within tier] — الأرقام ثابتة منذ v1.
This run's Phase 2 request: **1.2 SEC** (EXISTING — توسيع، لا استبدال).

## DEPENDENCY MAP
Build order: دون تغيير عن v1.
Key dependencies:
  (MODIFIED) FIN → SOFT-READ → MDL : التحقق من رموز قيم اللوكب — مواءمة مع فهرس الاعتماديات في السجل (السجل هو المرجع)
  (MODIFIED) FIN → SEC : الهوية والتفويض وفصل المهام عبر المعترِض القياسي للمنصة، دون FK مادي — مواءمة مع فهرس القرارات في السجل (#9)
  (ADDED) مستهلك أحداث Oracle (برنامج خدمي خارجي) → EXTERNAL PRINCIPAL → SEC : يُصادَق آليًا كحساب خدمة في SEC
  (ADDED) مستهلك أحداث Oracle → FIN : يحمل منح إجراء واحدًا فقط (إنشاء قيد يومية) عبر دور حساب الخدمة

## DEFERRED (not in scope for this version)
(ADDED rows — صفوف v1 باقية)
| Item | Reason / activation trigger |
|---|---|
| طابور قاعدة البيانات، خدمة المستهلك، وكل ما داخل نظام Oracle القديم / In-database queue, consumer service, legacy Oracle internals | خارج نطاق هذا التغيير صراحة [مانيفست v2 — Out of scope] |
| تغييرات FIN (ربط الحسابات، وسوم الأبعاد على السطور المُولَّدة بالقواعد، خطأ الحدث المكرر، رمز الفترة المغلقة) / FIN-side deltas | تُتابَع كمجموعة تغيير مستقلة لوحدة FIN [مانيفست v2 — Out of scope] |
| هوية أحمال العمل الموحَّدة / شهادات mTLS للعملاء / Workload identity federation, mTLS client certificates | لا حاجة لبرنامج خدمي داخلي واحد؛ التفعيل: مُستدعون آليون خارجيون أو مستضافون سحابيًا |
| انتهاء إلزامي لبيانات اعتماد الخدمة / Forced service-credential expiry | التفعيل: طلب صريح من سياسة الأمان (انظر قرارات business-policies-sec.md) |

## RESOLVED DECISIONS (this phase)
(ADDED rows — صفوف v1 رقم 1–2 باقية)
| # | Point | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 3 | طلب Phase 2 لهذا الإصدار | 1.2 SEC بحالة EXISTING — توسيع سجل v1 دون استبداله | نعم — مانيفست التغيير v2 يحدد SEC وحدها | مانيفست v2؛ ENGINE §2.1 STEP 3 |
| 4 | حالة MDL وFIN في v2 | EXCEPTION — كلتاهما تجاوزت P0 وفق حالة خط الإنتاج في السجل | نعم — حقيقة من السجل لا اختيار | project-registry → PIPELINE / PROGRESS STATUS |
| 5 | سطرا اعتماديات FIN في v1 يخالفان السجل (HARD) | يُواءَمان مع السجل (MDL: SOFT-READ؛ SEC: معترِض قياسي بلا FK) — السجل هو المرجع | نعم — قاعدة القراءة: "extend, never contradict" | ENGINE §1 STEP B؛ project-registry → CROSS-MODULE DEPENDENCY INDEX، DECISION INDEX #9 |

## OPEN ITEMS
None — platform scope fully determined; نطاق v2 محصور في حساب الخدمة داخل SEC، ولا تعارض قائم بين السجل والملخص بعد المواءمة أعلاه.

## NEXT STEP
الوحدة 1.2 SEC تتوسع في module-registry-sec.md وbusiness-policies-sec.md لهذا الإصدار.
للتعديل: تعليمات نصية مباشرة؛ وإلا فالمرحلة التالية P0.5 لوحدة SEC v2.
