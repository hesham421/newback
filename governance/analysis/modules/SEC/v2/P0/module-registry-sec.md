## MODULE REGISTRY — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module Code    : SEC   (profile.vocabulary.module_prefixes)
Bounded context: organization
Layer / Type   : L1 / security engine     Execution tier : 1.2
Source         : EXTENDED from prior registry (v1) — v2 delta: حساب الخدمة / service account
Knowledge      : new project/security-module-plan-en.md; profiles/erp/knowledge/erp-domain-standards.md §4, §6; مانيفست التغيير v2 §1
Readiness      : READY
══════════════════════════════════════════════════════════════════
تُعرض هنا العناصر المُضافة أو المُعدَّلة فقط؛ كل ما عداها يبقى كما في v1.

ENTITIES OWNED   (names only — entity IDs are assigned by P1)
| Entity (ar/en) | Kind | PRIVATE / SHARED | Source |
|---|---|---|---|
| المستخدم / User — MODIFIED | security | SHARED — أصبح يغطي نوعين من الأسس الأمنية: بشري (HUMAN) وخدمي (SERVICE)؛ حساب الخدمة مستخدمٌ من نوع SERVICE يعيد استخدام إسناد الأدوار والمنح والتدقيق القائمة | مانيفست v2 §1؛ قرار الحوار #2 أدناه |
| بيانات اعتماد حساب الخدمة / ServiceAccountCredential — ADDED | security | PRIVATE | مانيفست v2 §1؛ قرار الحوار #1 أدناه |
الجلسة النشطة / ActiveSession — UNCHANGED: تبقى لجلسات الدخول البشري فقط؛ حساب الخدمة لا ينشئ صف جلسة (قرار الحوار #3).

LOOKUPS OWNED    (value lists this module masters)
| Lookup key | Description | Initial values (only those the user named) | Source |
|---|---|---|---|
| PRINCIPAL_TYPE — ADDED | نوع الأساس الأمني / principal type | None — no specific values named by the user | AUTO (see AUTO-DECISIONS) |
| AUDIT_EVENT_TYPE — MODIFIED (قيم مُضافة) | نوع حدث التدقيق / audit-log event type | None — no specific values named by the user | AUTO (see AUTO-DECISIONS) |
Rule (profile): all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs

LOOKUPS CONSUMED (from other modules)
UNCHANGED — None.

SHARED ENTITIES CONSUMED
UNCHANGED — None; SEC remains ROOT.

DEPENDENCIES
UNCHANGED — None within the platform's module registry. ROOT: YES
External (ADDED, not a registry module): مستهلك أحداث Oracle القديم (برنامج خدمي) — مُستدعٍ آلي يُصادَق كحساب خدمة لدى SEC؛ SEC لا تعتمد عليه، هو من يعتمد عليها.

AUTO-DECISIONS
(ADDED — قرارات v1 باقية)
AUTO: registered PRINCIPAL_TYPE as a SEC-owned lookup type with initial values HUMAN / SERVICE (AUTO, not user-named)
  FROM: profiles/erp.yaml conventions.lookups ("no hardcoded enums in APIs or field specs"); قرار الحوار #2
  IF WRONG: replace with a boolean service-account marker on User — revise this registry and business-policies-sec.md accordingly.
AUTO: extended AUDIT_EVENT_TYPE with SERVICE_ACCOUNT_CREATED / SERVICE_ACCOUNT_DEACTIVATED / SERVICE_CREDENTIAL_ISSUED / SERVICE_CREDENTIAL_REVOKED / SERVICE_AUTH_SUCCESS / SERVICE_AUTH_FAILED (AUTO, not user-named); existing ROLE_ASSIGNED / ROLE_REVOKED and grant values apply unchanged to service accounts
  FROM: مانيفست v2 §1 ("its activity shows up in the audit log like any other principal's"); security-module-plan-en.md §5.2
  IF WRONG: reuse LOGIN_SUCCESS / LOGIN_FAILED for machine authentication and distinguish by principal type instead of new values.
AUTO: SEC lookup storage for the new/extended value sets follows the v1 storage decision for SEC-owned lookups (registry decision index #7) — not reopened here
  FROM: project-registry → DECISION INDEX #7; معيار القبول 2 (الثوابت موروثة)
  IF WRONG: migrate SEC lookups to MDL as a separate change set covering all SEC lookup types together.
AUTO: a service account uses only ACTIVE / DISABLED of USER_STATUS; PENDING does not apply (no self sign-up path) and LOCKED is not reached through failed interactive logins
  FROM: قرار الحوار #1؛ business-policies-sec.md (منع الاستخدام التفاعلي؛ الإنشاء عبر الإدارة فقط)
  IF WRONG: allow LOCKED for a service account after repeated failed machine authentications — would need a threshold policy.
AUTO: a service account holds no menu — the dynamic menu serves interactive users only
  FROM: security-module-plan-en.md §6؛ حساب الخدمة لا يدخل تفاعليًا
  IF WRONG: none recommended.

RESOLVED DECISIONS (dialogue, this module)
| # | Point | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | آلية مصادقة المُستدعي الآلي / Machine-caller authentication mechanism | حساب خدمة يُصادَق بمنحة "بيانات اعتماد العميل" (OAuth 2.0 client credentials): سرّ يُبدَّل برمز وصول قصير العمر يُعاد طلبه آليًا، دون رمز تحديث ودون جلسة بشرية | Recommended by dialogue round 1; accepted unchanged in round 2 — converged | RFC 6749 §4.4, §4.4.3؛ ممارسة Google Cloud IAM service accounts وMicrosoft Entra service principals؛ NIST SP 800-53 Rev.5 AC-2, IA-5 |
| 2 | نموذج الأساس الأمني / Principal model | حساب الخدمة مستخدمٌ من نوع SERVICE يعيد استخدام الأدوار والمنح وشاشات الإدارة والتدقيق؛ الكيان الجديد الوحيد هو بيانات الاعتماد | Recommended by dialogue round 1; accepted unchanged in round 2 — converged | مانيفست v2 §1 ("like any other principal", "existing administration screens")؛ domain-profile §5 G1 |
| 3 | التحقق لكل طلب مقابل الجلسات / Per-request validation vs sessions | كل طلب آلي يُتحقق منه مقابل الحالة الحيّة لبيانات الاعتماد والحساب، لا مقابل صف جلسة؛ **يشمل ذلك كل طلب يحمل رمز وصول صادرًا عن بيانات الاعتماد، لا فقط طلبًا يقدّم السرّ نفسه**؛ وتعطيل الحساب يُسقط كل بيانات اعتماده ورموزها معًا — تنظيف الجلسات لا يمسّه والإلغاء فوري ولا نافذة ثقة بعد الإلغاء | Recommended by dialogue round 1; amended in round 2; deactivation wording made explicit in round 3 — converged (see POL-SEC-017) | security-module-plan-en.md §5.1؛ مانيفست v2 §1؛ round-2 gap analysis (token vs. secret distinction under RFC 6749 §4.4) |

POLICIES OWNED (full text in business-policies-sec.md) — ADDED in v2
POL-SEC-012, POL-SEC-013, POL-SEC-014, POL-SEC-015, POL-SEC-016,
POL-SEC-017, POL-SEC-018, POL-SEC-019, POL-SEC-020, POL-SEC-021,
POL-SEC-022, POL-SEC-023, POL-SEC-024
POL-SEC-024 is not a service-account policy: it is the v1 policy the module always relied on and never
wrote down — an optional platform integration is never a hard dependency. C4.4 surfaced it at this delta,
because US-SEC-012 stated the rule in its own prose and no policy carried it.
══════════════════════════════════════════════════════════════════
