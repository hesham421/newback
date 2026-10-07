# SRS — الأمان / Security (SEC) — DELTA v2
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v2 (delta on v1 — change set CS-SEC-001, ADDITIVE)   Profile : erp
Inputs : prd, domain-profile, project-registry, change-manifest (PRD v2 approved 2026-09-23 by hesham421)
Counts : REQ 79 · AC 85 · ENT 14 · RULE 13 · SCR-REQ 10 · ADR 6 (this stage)   (current state, v1 + v2)
Delta  : ADDED — ENT 1 · REQ 44 · AC 50 · RULE 6 · SCR-REQ 0 · ADR 6 (G3 fix added REQ-SEC-078/AC-SEC-084/RULE-SEC-013, the service-account credential-count cap (77/83/12 → 78/84/13); G5 added REQ-SEC-072…076/AC-SEC-078…082 retracing API-SEC-032…036; G6 added REQ-SEC-077/AC-SEC-083 for POL-SEC-024's unwanted-integration path; pass-1 REVISE review added REQ-SEC-079/AC-SEC-085 for the reactivation-audit gap, RG6, and raised ADR-SEC-046 (RG4, C5.14 parser) and ADR-SEC-047 (RG6))
         MODIFIED — ENT-SEC-001 (principal type field; meaning of email/passwordHash for a service account) · SCR-REQ-SEC-004 · SCR-REQ-SEC-007
         REMOVED — none
Status : COMPLETE — ADR-SEC-031's breaking ambiguity (the decided consumer role vs the inherited VIEW gateway) was resolved at the human stop by ADR-SEC-034; AC-SEC-077 states the role shape
══════════════════════════════════════════════════════════════════

Delta reading note: هذا الملف يعيد إصدار السجلات المُضافة والمُعدَّلة فقط. كل سجل من v1 لا يُعاد إصداره هنا
يبقى نافذًا دون تغيير، ويحمله `gov.py state` إلى `_state/current-srs.md`. أما الأقسام التي لا تحمل معرّفًا
(A1، A2، تمهيد A3، A6، A7، A8، STANDALONE) فتحلّ محلّ نظيراتها في v1 داخل الحالة المطويّة، لذا تُعاد
كاملةً هنا: محتوى v1 كما هو، ومحتوى v2 معلَّم.

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | SEC — الأمان / Security |
| Feature code | SEC |
| Version | v2 (delta — CS-SEC-001, ADDITIVE) |
| Date | 2026-09-23 |
| Status | COMPLETE (P1) — ADR-SEC-031 SUPERSEDED by ADR-SEC-034 (human decision); nothing in this delta is open |
| Prepared by | governance-factory (analysis lane) |
| Decisions applied count | 6 new ADRs (ADR-SEC-031 SUPERSEDED by ADR-SEC-034, ADR-SEC-032, ADR-SEC-033, ADR-SEC-034, ADR-SEC-046, ADR-SEC-047) + 19 upstream v2 ADRs applied (ADR-SEC-012 … ADR-SEC-030) + 5 DEFAULTs (STANDALONE → Decisions applied) |
| Acceptance criteria | 85 (AC-SEC-001 … AC-SEC-085); AC-SEC-077 states the consumer role's shape per ADR-SEC-034; AC-SEC-078…082 (G5) retrace API-SEC-032…036; AC-SEC-083 (G6) covers POL-SEC-024; AC-SEC-084 (G3) covers RULE-SEC-013's service-account credential-count cap; AC-SEC-085 (pass-1 REVISE, RG6) covers REQ-SEC-079's reactivation audit |

## A2 — Functional context

**In scope (v1, unchanged):** المصادقة (تسجيل الدخول، التسجيل الذاتي، إعادة تعيين كلمة المرور)، RBAC هرمي
بثلاث مستويات (وحدة→شاشة→إجراء)، تسجيل الوحدات/الشاشات/الإجراءات كبيانات لأي وحدة مستهلكة،
فصل المهام (SoD) على مستوى المستخدم، القائمة الديناميكية ثنائية المستوى، لوحة تحكم الأمان،
سجل التدقيق غير القابل للتعديل، إدارة الجلسات النشطة، تكامل اختياري مع خدمة الإشعارات.

**In scope (v2 — CS-SEC-001):** حساب الخدمة للمُستدعي الآلي: مستخدمٌ من نوع الأساس الأمني SERVICE
(ADR-SEC-013) يُنشئه مسؤول الأمان من شاشة المستخدمين القائمة ويراه فيها مميَّزًا عن المستخدم البشري، ويُسنِد
إليه الأدوار والمنح بالإدارة نفسها؛ بيانات اعتماد (سرّ) تُصدَر له وتُكشف مرة واحدة وتُخزَّن بصيغة غير قابلة
للعكس، ويجوز أن يحمل أكثر من واحدة للتدوير دون انقطاع؛ مصادقة آلية بمنحة "بيانات اعتماد العميل"
(OAuth 2.0 client credentials — ADR-SEC-012) تُصدر رمز وصول دون جلسة بشرية ودون رمز تحديث؛ تحقق كل طلب
آلي من الحالة الحيّة لبيانات الاعتماد وللحساب لا من صف جلسة (ADR-SEC-014)؛ إلغاء بيانات الاعتماد وتعطيل
الحساب نافذان من الطلب التالي بما فيه رمز صادر قبلهما؛ منع أي استخدام تفاعلي للحساب؛ تدقيق مصادقاته
ودورة حياة بيانات اعتماده وتغييرات أدواره في سجل التدقيق القائم.

**Out of scope:** المصادقة متعددة العوامل (MFA)، تسجيل الدخول الموحد (SSO)/موفرو هوية خارجيون — غير مذكورين
في `security-module-plan-en.md` [business-policies-sec.md → SCOPE EXCEPTIONS]؛ أي منطق عمل خاص بوحدة
مستهلكة. v2 يضيف: الانتهاء الإلزامي لبيانات اعتماد الخدمة؛ هوية أحمال العمل الموحَّدة وشهادات mTLS؛ تسجيل
كل استدعاء أعمال لحساب الخدمة داخل سجل SEC (الأفعال التجارية تُنسب عبر حقول التدقيق القياسية في الوحدة
المالكة — ADR-SEC-017)؛ طابور قاعدة البيانات وخدمة المستهلك ونظام Oracle؛ تغييرات FIN (مجموعة تغيير مستقلة).

**Module function (one paragraph):** وحدة SEC هي نظام الأمان الوحيد للمنصة بأكملها: تُصدر
الهوية (المصادقة) وتُقرّر الصلاحيات الفعلية (RBAC هرمي)، بحيث لا تملك أي وحدة أخرى مستخدمين
أو أدوارًا أو تسجيل دخول خاصًا بها؛ كل وحدة تستهلك SEC عبر تسجيل نفسها كبيانات ثم فحص
المنح الصادرة عنها. في v2 يصبح للأساس الأمني نوعان: بشري يدخل تفاعليًا بجلسة، وخدمي يُصادَق آليًا ببيانات
اعتماد دون جلسة؛ ويخضع الاثنان لبوابة الوحدة والمنح الهرمية ذاتها دون استثناء (POL-SEC-012).

**Detailed description (workflow narrative, roles):** مستخدم يُسجّل ذاتيًا فيبقى معلّقًا بلا
صلاحيات → يوافق مسؤول أمان عليه فيصبح نشطًا → يُسنَد له دور واحد أو أكثر → عند كل طلب،
يُفحص منح الوحدة أولاً (بوابة)، ثم منح الشاشة، ثم منح الإجراء. مسؤول الأمان يدير الأدوار
والمستخدمين والجلسات النشطة ويراقب سجل التدقيق ولوحة التحكم. أي وحدة مستهلكة (مثل FIN
لاحقًا) تُسجّل نفسها وشاشاتها وإجراءاتها هنا كبيانات فقط، دون أي تعديل على شيفرة SEC.
(v2) مسار حساب الخدمة: مسؤول الأمان يُنشئ حساب خدمة من شاشة المستخدمين فيولد نشطًا بلا أي دور أو منح →
يُسنِد إليه دورًا يحمل ما يحتاجه عمله فقط → يُصدر له بيانات اعتماد ويتسلّم السرّ مرة واحدة ليضعه في
إعدادات البرنامج الخدمي → البرنامج الخدمي يبادل معرّف العميل والسرّ برمز وصول، ويعيد طلب رمز جديد بنفسه
كلما انتهى الرمز، وكل طلب يحمله يُتحقق منه مقابل الحالة الحيّة → للتدوير: يُصدر المسؤول بيانات اعتماد ثانية،
ويبدّل البرنامج إليها، ويتأكد من "آخر استخدام" للأولى ثم يلغيها → عند الاختراق أو الاستغناء: إلغاء بيانات
الاعتماد أو تعطيل الحساب ينفذ من الطلب التالي.

**Current situation:** قبل v2 لا يعرف SEC مُستدعيًا غير بشري: رمز الوصول ينتهي بعد ساعة، وكل طلب يُتحقق منه
مقابل صف جلسة حيّ، فيضطر البرنامج الخدمي إلى العمل ببيانات دخول مستخدم بشري، وأي تنظيف دوري للجلسات
يُسقط التكامل بصمت (مانيفست التغيير v2 — Summary).

**Current difficulties:** (v2) ربط تكامل آلي بجلسة شخص؛ انقطاع صامت عند تنظيف الجلسات؛ غياب أي فصل بين
هوية الشخص وهوية البرنامج في سجل التدقيق.

**Proposed system and benefits:** نظام أمان مركزي واحد يمنع ازدواج/تضارب الصلاحيات بين
الوحدات، يضمن بوابة وحدة صارمة (لا شاشة يتيمة)، ويوفر أثرًا تدقيقيًا كاملاً غير قابل للتعديل.
(v2) هوية آلية مُدارة من الشاشات القائمة، تستمر دون إشراف طوال تشغيل البرنامج الخدمي، وتُقطع فورًا عند الحاجة،
ويظهر نشاطها الأمني في سجل التدقيق كأي أساس أمني.

**General notes (constraints, deferred items):** محرك سير العمل ممنوع منصّيًا
(`profiles/erp.yaml → conventions.workflow_engine: forbidden`)؛ لا آلية قفل تلقائي بعد محاولات
دخول فاشلة متكررة — لم يذكرها `security-module-plan-en.md`، فلم تُخترع (تُعرض فقط أعداد
الدخول الفاشل في لوحة التحكم، REQ-SEC-002/022). (v2) حساب الخدمة لا ينشئ صف جلسة ولا يملك قائمة
تنقّل (module-registry-sec.md → AUTO-DECISIONS)؛ ولا يمرّ بحالة PENDING. **نقطة موقوفة:** شكل دور المستهلك
المقرَّر في P0 (منح إجراء واحد دون VIEW) يتعارض مع بوابة VIEW الموروثة (REQ-SEC-030، RULE-SEC-007) —
حُسم هذا عند التوقّف البشري بـ ADR-SEC-034 (ADR-SEC-031 SUPERSEDED): بوابة VIEW جزء من المسار البنيوي للإجراء،
ودور المستهلك هو وحدة FIN + شاشة القيد + VIEW + CREATE، وهو ما ينصّ عليه AC-SEC-077.

## A3 — Entities and fields

Standard fields per kind (profile.conventions.entity_defaults): the `security` entity
kind carries no fixed default-field set in the profile (only master/transactional/
lookup/config do); every SEC entity below still carries the platform's audit fields
(`createdBy, createdAt, updatedBy, updatedAt`, `profile.stack.db.naming.audit_fields`)
except pure append-only log/session rows where a "who created it" field is redundant
with the row's own actor field (documented per entity). v2 re-emits ENT-SEC-001 (MODIFIED) and
adds ENT-SEC-014; every other v1 entity (ENT-SEC-002 … ENT-SEC-013) is unchanged and carried from v1.

### ENT-SEC-001 — المستخدم / User
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | SHARED (owner) — every module's audit fields (createdBy/updatedBy) reference it | No — login identity (email/username) is the natural key, not a generated number [§3.3 NUMBERING test] | create, read, search, update, activate, deactivate | consumed read-only by every future consumer module for its own audit fields | security-module-plan-en.md §3, §4.4; v2: ADR-SEC-013 (a service account is a User of principal type SERVICE) |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| userPk | number | yes (system) | — | primary key | معرّف المستخدم | User id |
| username | text | yes | unique | login identity; for a service account it is also the client identifier of machine authentication (v2, DEFAULT D2) | اسم المستخدم | Username |
| principalTypeCode | lookup | yes | lookup key `PRINCIPAL_TYPE` (A6) | v2 ADDED — set at creation, read-only afterwards [RULE-SEC-012]; HUMAN for every user created from a sign-up [REQ-SEC-038] | نوع الأساس الأمني | Principal type |
| email | text | yes | unique, valid email | used for password-reset delivery; for a service account: the responsible team's contact address, never a reset target [ADR-SEC-032, REQ-SEC-064] | البريد الإلكتروني (للخدمة: بريد جهة الاتصال) | Email (service: contact email) |
| passwordHash | text | yes (system) | never exposed to any client [POL-SEC-004] | write-only; for a service account an unusable random value, never logged, returned or transmitted [ADR-SEC-032] | تجزئة كلمة المرور | Password hash |
| fullNameAr | text | yes | — | for a service account: the integration's name | الاسم الكامل (عربي) | Full name (Arabic) |
| fullNameEn | text | yes | — | for a service account: the integration's name | الاسم الكامل (إنجليزي) | Full name (English) |
| statusCode | lookup | yes | lookup key `USER_STATUS` (A6) | drives A7 lifecycle; a service account uses ACTIVE / DISABLED only | الحالة | Status |
| lastLoginAt | date-time | no | — | informational; interactive logins only — never set for a service account | آخر دخول | Last login |
| failedLoginCount24h | number | no | derived, not stored per ERP-…(see POL-SEC-010) — displayed from AuditLogEntry, not persisted on User | dashboard-only figure; kept here only as a documentation note, not a real column | عدد محاولات الدخول الفاشلة (٢٤س) | Failed logins (24h) |
| isActiveFl | flag | yes | true/false | mirrors statusCode ≠ DISABLED, kept for the platform's standard flag convention | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | standard audit fields | — | — |

### ENT-SEC-014 — بيانات اعتماد حساب الخدمة / ServiceAccountCredential
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No — a credential is identified by its row and description inside SEC; nothing outside the system refers to it by a number, and the daemon authenticates by client identifier + secret [§3.3 NUMBERING test] | create (issue), read (list), update (record last use), revoke | none | module-registry-sec.md → ENTITIES OWNED (ServiceAccountCredential — ADDED); ADR-SEC-012; ADR-SEC-015; ADR-SEC-016 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| serviceAccountCredentialPk | number | yes (system) | — | primary key | معرّف بيانات الاعتماد | Credential id |
| userId | reference | yes | ENT-SEC-001 — must be of principal type SERVICE and ACTIVE at issuance [RULE-SEC-008] | the service account the credential authenticates | حساب الخدمة | Service account |
| secretHash | text | yes (system) | one-way hash of the generated secret [REQ-SEC-054] | write-once; never returned by any response [REQ-SEC-053] | تجزئة السرّ | Secret hash |
| description | text | no | free text entered at issuance (where the secret is deployed) | helps the administrator tell two live credentials apart during rotation | الوصف | Description |
| lastUsedAt | date-time | no (system) | set on each successful machine authentication with this credential [REQ-SEC-057] | informational; no rule reads it [ADR-SEC-033] | آخر استخدام | Last used |
| revokedAt | date-time | no (system) | set once on revocation [REQ-SEC-058] | empty = active; revoked is terminal | أُلغيت في | Revoked at |
| revokedBy | reference | no (system) | ENT-SEC-001 — the administrator who revoked it | set with revokedAt | أُلغيت بواسطة | Revoked by |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | standard audit fields; createdAt is the issuance time, createdBy the issuing administrator | — | — |

## A4 — Functional requirements (EARS) and acceptance criteria

v2 adds REQ-SEC-036 … REQ-SEC-071 with AC-SEC-036 … AC-SEC-076. Every v1 requirement and criterion
(REQ-SEC-001 … REQ-SEC-035) is unchanged and carried from v1; each applies to a service account as to any
principal unless a v2 requirement below states a narrower behaviour for it (acceptance 2).

### REQ-SEC-036 — إنشاء حساب خدمة / Create a service account
Pattern    : event
Statement  : When an administrator creates a service account, the system shall record it as a user of principal type SERVICE with status ACTIVE.
Traces     : US-SEC-013
Entities   : ENT-SEC-001
Rationale  : a machine caller is created by an administrative decision, in the existing administration
Source     : POL-SEC-018, POL-SEC-022; ADR-SEC-013; ADR-SEC-032
Priority   : HIGH
#### AC-SEC-036 — [REQ-SEC-036]
Given  : an administrator on the Users screen enters a unique username, a unique contact email and both name labels for a new service account
When   : they save it
Then   : the system creates one User with principalTypeCode=SERVICE and statusCode=ACTIVE, and creates 0 ServiceAccountCredential rows

### REQ-SEC-037 — لا صلاحية ضمنية عند الإنشاء / No implicit permission at creation
Pattern    : event
Statement  : When a service account is created, the system shall assign it no role and no grant.
Traces     : US-SEC-013
Entities   : ENT-SEC-001, ENT-SEC-003
Rationale  : the account holds only what an administrator later assigns explicitly
Source     : POL-SEC-013; ADR-SEC-018
Priority   : HIGH
#### AC-SEC-037 — [REQ-SEC-037]
Given  : a service account has just been created
When   : its role assignments are read
Then   : the system returns 0 UserRoleAssignment rows for it, and an authorization check for any registered module denies it

### REQ-SEC-038 — التسجيل الذاتي يُنشئ مستخدمًا بشريًا فقط / Sign-up creates human users only
Pattern    : ubiquitous
Statement  : The system shall assign principal type HUMAN to every user it creates from an approved sign-up request.
Traces     : US-SEC-013
Entities   : ENT-SEC-001, ENT-SEC-013
Rationale  : self sign-up never becomes a path to a machine principal
Source     : POL-SEC-022; ADR-SEC-020
Priority   : HIGH
#### AC-SEC-038 — [REQ-SEC-038]
Given  : a SignupRequest with status PENDING
When   : an administrator approves it
Then   : the system creates the User with principalTypeCode=HUMAN

### REQ-SEC-039 — نوع الأساس الأمني ثابت / Principal type is fixed after creation
Pattern    : unwanted
Statement  : If an update to an existing user changes its principal type, then the system shall reject the update.
Traces     : US-SEC-013
Entities   : ENT-SEC-001
Rationale  : converting a human user into a service account would bypass administrative creation and carry the human's roles and grants across (RULE-SEC-012)
Source     : POL-SEC-022, POL-SEC-013
Priority   : HIGH
#### AC-SEC-039 — [REQ-SEC-039]
Given  : an existing user with principalTypeCode=HUMAN
When   : an administrator submits an update setting principalTypeCode=SERVICE
Then   : the system rejects it with message ar: "لا يمكن تغيير نوع الأساس الأمني بعد إنشائه" · en: "The principal type cannot be changed after creation" and the user is unchanged

### REQ-SEC-040 — عرض نوع الأساس الأمني في قائمة المستخدمين / Show the principal type in the users list
Pattern    : event
Statement  : When an administrator searches the users list, the system shall display each result's principal type.
Traces     : US-SEC-013
Entities   : ENT-SEC-001
Rationale  : service accounts are distinguishable from human users in the existing screen
Source     : POL-SEC-018
Priority   : HIGH
#### AC-SEC-040 — [REQ-SEC-040]
Given  : 2 human users and 1 service account exist
When   : an administrator searches the users list with no filter
Then   : the system lists 3 rows, each showing its principal-type label from `PRINCIPAL_TYPE`

### REQ-SEC-041 — تصفية المستخدمين بنوع الأساس الأمني / Filter users by principal type
Pattern    : event
Statement  : When an administrator filters the users list by principal type, the system shall return only users of that principal type.
Traces     : US-SEC-013
Entities   : ENT-SEC-001
Rationale  : an administrator can see every service account at once
Source     : POL-SEC-018
Priority   : HIGH
#### AC-SEC-041 — [REQ-SEC-041]
Given  : 3 human users and 1 service account exist
When   : an administrator filters the users list by principal type SERVICE
Then   : the system returns exactly 1 row, the service account

### REQ-SEC-042 — لوحة التحكم تفصل حسابات الخدمة / Dashboard separates service accounts
Pattern    : event
Statement  : When an authorized administrator opens the admin dashboard, the system shall show the users-overview figures separately for each principal type.
Traces     : US-SEC-013
Entities   : ENT-SEC-001
Rationale  : the dashboard is one of the existing administration screens where service accounts are distinguishable
Source     : POL-SEC-018, POL-SEC-010; ADR-SEC-026 (breakdown left to P1)
Priority   : HIGH
#### AC-SEC-042 — [REQ-SEC-042]
Given  : 5 active human users and 1 active service account exist
When   : an administrator holding SEC_DASHBOARD VIEW and SEC_USERS VIEW opens the dashboard
Then   : the users-overview widget shows Human=5 and Service=1, computed from current data

### REQ-SEC-043 — إسناد أدوار لحساب خدمة / Assign roles to a service account
Pattern    : event
Statement  : When an administrator assigns one or more roles to a service account, the system shall record each assignment through the same role assignment it uses for a human user.
Traces     : US-SEC-014
Entities   : ENT-SEC-001, ENT-SEC-002, ENT-SEC-003
Rationale  : no parallel administration for service accounts
Source     : POL-SEC-012, POL-SEC-018
Priority   : HIGH
#### AC-SEC-043 — [REQ-SEC-043]
Given  : a service account and 2 active roles
When   : an administrator assigns both roles to it on the Users screen
Then   : the system creates 2 UserRoleAssignment rows for the service account, each with assignedBy and assignedAt set as for a human user

### REQ-SEC-044 — الصلاحية الفعلية لحساب الخدمة / A service account's effective permissions
Pattern    : ubiquitous
Statement  : The system shall compute a service account's effective permissions as the union of the grants of every role assigned to it, exactly as for a human user.
Traces     : US-SEC-014
Entities   : ENT-SEC-001, ENT-SEC-003, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Rationale  : v1 invariants are inherited, not reopened (acceptance 2)
Source     : POL-SEC-012, POL-SEC-008
Priority   : HIGH
Note       : the Oracle consumer's grant set is settled by ADR-SEC-034 (ADR-SEC-031 SUPERSEDED): the VIEW gateway is part of the structural path to an action, exactly as the module gate is
#### AC-SEC-044 — [REQ-SEC-044]
Given  : a service account holding role A, which grants action X, and role B, which grants action Y, each role also holding every prerequisite grant the v1 hierarchy requires for its action
When   : the service account calls action X and then action Y with a valid access token
Then   : the system allows both calls
#### AC-SEC-077 — [REQ-SEC-044]
Given  : the Oracle event consumer's role, holding the FIN module grant, the journal-entry screen grant, that screen's VIEW action grant and its create-journal-entry action grant, and no other grant of any kind
When   : an administrator saves that role and the consumer then calls create journal entry with a valid access token
Then   : the system accepts the role without SEC-409-NO-VIEW-GRANT and allows the call, and it denies every action on every other FIN screen and every other module

### REQ-SEC-045 — رفض طلب غير ممنوح لحساب خدمة / Deny an ungranted service-account request
Pattern    : unwanted
Statement  : If a service account's effective grants do not authorize a requested module, screen or action, then the system shall deny the request with the authorization error a human user receives.
Traces     : US-SEC-014
Entities   : ENT-SEC-001, ENT-SEC-004, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Rationale  : the module gate and the grant hierarchy apply to every principal (POL-SEC-001, POL-SEC-002)
Source     : POL-SEC-012, POL-SEC-001
Priority   : HIGH
#### AC-SEC-045 — [REQ-SEC-045]
Given  : a service account whose roles hold no grant for module FIN
When   : it calls a FIN endpoint with a valid, unexpired access token
Then   : the system denies the request with message ar: "غير مصرح بهذا الإجراء" · en: "You are not authorized to perform this action"

### REQ-SEC-046 — المصادقة الآلية ببيانات اعتماد العميل / Machine authentication by client credentials
Pattern    : event
Statement  : When a service account presents its client identifier with the secret of one of its active credentials, the system shall issue it an access token without creating an active session.
Traces     : US-SEC-015
Entities   : ENT-SEC-001, ENT-SEC-014, ENT-SEC-010
Rationale  : unattended authentication that no human login session backs
Source     : POL-SEC-014; ADR-SEC-012 (OAuth 2.0 client credentials, RFC 6749 §4.4)
Priority   : HIGH
#### AC-SEC-046 — [REQ-SEC-046]
Given  : an ACTIVE service account with one active credential
When   : the machine caller submits grant type `client_credentials`, the account's username as client identifier and that credential's secret
Then   : the system returns an access token with an expiry of 3600 seconds and creates 0 ActiveSession rows

### REQ-SEC-047 — رفض بيانات اعتماد عميل غير صحيحة / Reject invalid client credentials
Pattern    : unwanted
Statement  : If a machine authentication request presents an unknown client identifier or a secret that matches none of the named service account's active credentials, then the system shall reject it without issuing an access token.
Traces     : US-SEC-015
Entities   : ENT-SEC-001, ENT-SEC-014
Rationale  : RULE-SEC-009; the answer never reveals which part was wrong
Source     : POL-SEC-014; RFC 6749 §5.2 (`invalid_client`)
Priority   : HIGH
#### AC-SEC-047 — [REQ-SEC-047]
Given  : the client identifier of an ACTIVE service account
When   : a machine authentication request presents it with a wrong secret
Then   : the system rejects the request with message ar: "بيانات اعتماد العميل غير صالحة" · en: "Invalid client credentials" and issues no access token
#### AC-SEC-048 — [REQ-SEC-047]
Given  : a client identifier that names no user
When   : a machine authentication request presents it with any secret
Then   : the system rejects the request with the same message ar: "بيانات اعتماد العميل غير صالحة" · en: "Invalid client credentials" and issues no access token

### REQ-SEC-048 — الاستمرار دون تدخّل بشري / Survive session expiry and cleanup
Pattern    : state
Statement  : While a service account and at least one of its credentials remain active, the system shall accept that account's machine authentication requests regardless of the expiry, termination or cleanup of any login session.
Traces     : US-SEC-015
Entities   : ENT-SEC-001, ENT-SEC-014, ENT-SEC-010
Rationale  : routine session cleanup no longer takes the integration down
Source     : POL-SEC-015; ADR-SEC-014
Priority   : HIGH
#### AC-SEC-049 — [REQ-SEC-048]
Given  : an ACTIVE service account with one active credential, whose last access token has expired, and every ActiveSession row in the platform terminated by cleanup
When   : the machine caller requests a new access token with the same credential
Then   : the system returns 1 new access token with an expiry of 3600 seconds, with no person re-authenticating the account

### REQ-SEC-049 — لا رمز تحديث / No refresh token
Pattern    : ubiquitous
Statement  : The system shall issue no refresh token in response to a machine authentication request.
Traces     : US-SEC-015
Entities   : ENT-SEC-014
Rationale  : the caller re-authenticates with its credential; no second long-lived secret is put in play
Source     : ADR-SEC-012 (RFC 6749 §4.4.3)
Priority   : HIGH
#### AC-SEC-050 — [REQ-SEC-049]
Given  : an ACTIVE service account with one active credential
When   : it authenticates successfully
Then   : the response contains an access token and its expiry, and contains no refresh token

### REQ-SEC-050 — التحقق من رمز حساب الخدمة مقابل الحالة الحيّة / Validate a service-account token against live state
Pattern    : event
Statement  : When a request bears an access token issued to a service account, the system shall validate it against the current state of the issuing credential and of the service account instead of an active session.
Traces     : US-SEC-015, US-SEC-017
Entities   : ENT-SEC-001, ENT-SEC-014, ENT-SEC-010
Rationale  : a machine caller has no session row, and revocation must take effect on the next request
Source     : POL-SEC-015, POL-SEC-017; ADR-SEC-014 (the mechanism is chosen by P3.1)
Priority   : HIGH
#### AC-SEC-051 — [REQ-SEC-050]
Given  : an unexpired access token issued from an active credential of an ACTIVE service account, and no ActiveSession row for that account
When   : the caller sends a request its grants authorize
Then   : the system allows the request and returns its result

### REQ-SEC-051 — إصدار بيانات اعتماد / Issue a credential
Pattern    : event
Statement  : When an administrator issues a credential for a service account, the system shall generate a new secret and return it in the issuance response only.
Traces     : US-SEC-016
Entities   : ENT-SEC-014, ENT-SEC-001
Rationale  : the administrator receives the secret once, to hand it to the daemon
Source     : POL-SEC-020; ADR-SEC-016
Priority   : —
#### AC-SEC-052 — [REQ-SEC-051]
Given  : an ACTIVE service account
When   : an administrator issues a credential for it with description "Oracle consumer — primary"
Then   : the system creates one active ServiceAccountCredential row and returns its secret once in that response, with message ar: "انسخ السرّ الآن؛ لن يُعرض مرة أخرى" · en: "Copy the secret now; it will not be shown again"

### REQ-SEC-052 — رفض الإصدار لغير حساب خدمة نشط / Reject issuance for anything but an active service account
Pattern    : unwanted
Statement  : If an administrator attempts to issue a credential for a user that is not an active service account, then the system shall reject the issuance.
Traces     : US-SEC-016
Entities   : ENT-SEC-014, ENT-SEC-001
Rationale  : RULE-SEC-008; a human user never receives a machine credential
Source     : POL-SEC-016, POL-SEC-022
Priority   : —
#### AC-SEC-053 — [REQ-SEC-052]
Given  : a user with principalTypeCode=HUMAN
When   : an administrator attempts to issue a credential for it
Then   : the system rejects it with message ar: "لا تُصدَر بيانات الاعتماد إلا لحساب خدمة نشط" · en: "Credentials can be issued only for an active service account" and creates no ServiceAccountCredential
#### AC-SEC-054 — [REQ-SEC-052]
Given  : a service account with statusCode=DISABLED
When   : an administrator attempts to issue a credential for it
Then   : the system rejects it with message ar: "لا تُصدَر بيانات الاعتماد إلا لحساب خدمة نشط" · en: "Credentials can be issued only for an active service account" and creates no ServiceAccountCredential

### REQ-SEC-053 — لا كشف للسرّ بعد الإصدار / The secret is never revealed after issuance
Pattern    : ubiquitous
Statement  : The system shall exclude a credential's secret and its stored hash from every response other than that credential's issuance response.
Traces     : US-SEC-016
Entities   : ENT-SEC-014
Rationale  : extends v1's password-hash discipline (POL-SEC-004) to service secrets
Source     : POL-SEC-020
Priority   : —
#### AC-SEC-055 — [REQ-SEC-053]
Given  : a credential issued earlier
When   : an administrator lists the service account's credentials or reads the service account
Then   : no response field contains the secret or the secretHash value

### REQ-SEC-054 — تخزين السرّ بصيغة غير قابلة للعكس / Store the secret in non-reversible form
Pattern    : ubiquitous
Statement  : The system shall store a credential's secret only as a one-way hash.
Traces     : US-SEC-016
Entities   : ENT-SEC-014
Rationale  : a leaked data store must not disclose a secret that can post to FIN unattended
Source     : POL-SEC-023; ADR-SEC-021; OWASP Secrets Management Cheat Sheet
Priority   : —
#### AC-SEC-056 — [REQ-SEC-054]
Given  : a credential that has just been issued
When   : its stored record is read directly from the data store
Then   : secretHash differs from the secret, no stored field or log line contains the secret, and verifying the secret against secretHash succeeds

### REQ-SEC-055 — قبول كل بيانات اعتماد نشطة أثناء التدوير / Accept every active credential during rotation
Pattern    : state
Statement  : While a service account holds more than one active credential, the system shall accept each of them for machine authentication until that credential is individually revoked.
Traces     : US-SEC-016
Entities   : ENT-SEC-014, ENT-SEC-001
Rationale  : the secret is rotated without stopping the daemon
Source     : POL-SEC-021; ADR-SEC-015; ADR-SEC-023
Priority   : —
#### AC-SEC-057 — [REQ-SEC-055]
Given  : an ACTIVE service account with two active credentials C1 and C2
When   : the machine caller authenticates with C1 and then with C2
Then   : the system returns 1 access token for each of the 2 credentials
#### AC-SEC-058 — [REQ-SEC-055]
Given  : an ACTIVE service account whose credential C1 is revoked and whose credential C2 is active
When   : the machine caller authenticates with C2
Then   : the system returns 1 access token

### REQ-SEC-056 — عرض بيانات اعتماد حساب الخدمة / List a service account's credentials
Pattern    : event
Statement  : When an administrator opens a service account's credentials, the system shall list each credential's description, issue time, last-use time and revocation time.
Traces     : US-SEC-016
Entities   : ENT-SEC-014
Rationale  : the administrator sees which credentials are live before revoking one
Source     : POL-SEC-018, POL-SEC-021; ADR-SEC-033
Priority   : —
#### AC-SEC-059 — [REQ-SEC-056]
Given  : a service account with one active and one revoked credential
When   : an administrator opens its Credentials tab
Then   : the system lists 2 rows, each with description, createdAt, lastUsedAt and revokedAt, and no secret

### REQ-SEC-057 — تسجيل آخر استخدام / Record a credential's last use
Pattern    : event
Statement  : When a credential authenticates a machine authentication request successfully, the system shall record that time as the credential's last-use time.
Traces     : US-SEC-016
Entities   : ENT-SEC-014
Rationale  : shows when the daemon has switched to the new credential, so revoking the old one is safe
Source     : ADR-SEC-033
Priority   : —
#### AC-SEC-060 — [REQ-SEC-057]
Given  : an active credential with an empty lastUsedAt
When   : the machine caller authenticates with it successfully at time T
Then   : the system sets that credential's lastUsedAt=T and leaves every other credential's lastUsedAt unchanged

### REQ-SEC-058 — إلغاء بيانات اعتماد / Revoke a credential
Pattern    : event
Statement  : When an administrator revokes a service-account credential, the system shall mark it revoked and reject its secret in every later machine authentication request.
Traces     : US-SEC-017
Entities   : ENT-SEC-014
Rationale  : a compromised or retired secret is cut off at once
Source     : POL-SEC-017; ADR-SEC-014
Priority   : HIGH
#### AC-SEC-061 — [REQ-SEC-058]
Given  : an active credential
When   : an administrator revokes it
Then   : the system sets its revokedAt and revokedBy, and the next machine authentication request with its secret is rejected with message ar: "بيانات اعتماد العميل غير صالحة" · en: "Invalid client credentials"

### REQ-SEC-059 — رفض رمز صادر عن بيانات اعتماد ملغاة / Reject a token issued from a revoked credential
Pattern    : unwanted
Statement  : If a request bears an access token issued from a credential that has since been revoked, then the system shall reject the request.
Traces     : US-SEC-017
Entities   : ENT-SEC-014
Rationale  : no trust window between revocation and the token's own expiry
Source     : POL-SEC-017; ADR-SEC-014
Priority   : HIGH
#### AC-SEC-062 — [REQ-SEC-059]
Given  : an access token issued from credential C at 10:00 with a 3600-second expiry, and C revoked at 10:05
When   : the caller sends a request bearing that token at 10:06
Then   : the system rejects the request with message ar: "بيانات اعتماد العميل غير صالحة" · en: "Invalid client credentials"

### REQ-SEC-060 — تعطيل حساب الخدمة يُسقط كل بيانات اعتماده ورموزها / Deactivation rejects every credential and token of the account
Pattern    : event
Statement  : When an administrator deactivates a service account, the system shall reject every later request authenticated by any of its credentials or by any access token issued from them.
Traces     : US-SEC-017
Entities   : ENT-SEC-001, ENT-SEC-014
Rationale  : deactivation reaches every live credential, including the second one held during rotation
Source     : POL-SEC-017; ADR-SEC-022; ADR-SEC-014
Priority   : HIGH
#### AC-SEC-063 — [REQ-SEC-060]
Given  : an ACTIVE service account with two active credentials and one unexpired access token issued from each
When   : an administrator deactivates the account
Then   : the system sets statusCode=DISABLED, rejects the next machine authentication request with either secret, and rejects the next request bearing either token

### REQ-SEC-061 — رفض إلغاء بيانات اعتماد ملغاة / Reject revoking an already revoked credential
Pattern    : unwanted
Statement  : If an administrator attempts to revoke a credential that is already revoked, then the system shall reject the revocation.
Traces     : US-SEC-017
Entities   : ENT-SEC-014
Rationale  : RULE-SEC-011; the first revocation's time and actor stay the record
Source     : POL-SEC-017
Priority   : HIGH
#### AC-SEC-064 — [REQ-SEC-061]
Given  : a credential whose revokedAt is set
When   : an administrator attempts to revoke it again
Then   : the system rejects the request with message ar: "بيانات الاعتماد هذه ملغاة بالفعل" · en: "This credential is already revoked" and revokedAt and revokedBy are unchanged

### REQ-SEC-062 — إعادة تفعيل حساب الخدمة / Reactivate a service account
Pattern    : event
Statement  : When an administrator reactivates a disabled service account, the system shall again accept those of its credentials that were never individually revoked.
Traces     : US-SEC-017
Entities   : ENT-SEC-001, ENT-SEC-014
Rationale  : deactivation is a live-state check, not a revocation of each credential; REQ-SEC-031 applies to every principal
Source     : ADR-SEC-014; POL-SEC-017
Priority   : HIGH
#### AC-SEC-065 — [REQ-SEC-062]
Given  : a DISABLED service account with credential C1 never revoked and credential C2 revoked
When   : an administrator reactivates the account
Then   : the system sets statusCode=ACTIVE, a machine authentication with C1 succeeds, and one with C2 is rejected

### REQ-SEC-063 — رفض الدخول التفاعلي لحساب خدمة / Reject interactive login for a service account
Pattern    : unwanted
Statement  : If an interactive login names a service account, then the system shall reject the login attempt.
Traces     : US-SEC-018
Entities   : ENT-SEC-001, ENT-SEC-010, ENT-SEC-011
Rationale  : RULE-SEC-010; a machine identity opens no interactive attack surface
Source     : POL-SEC-016; ADR-SEC-019
Priority   : —
#### AC-SEC-066 — [REQ-SEC-063]
Given  : a service account's username
When   : it is submitted on the Login screen with any password
Then   : the system rejects the attempt with message ar: "بيانات الدخول غير صحيحة" · en: "Invalid credentials", creates 0 ActiveSession rows and appends one `LOGIN_FAILED` audit entry

### REQ-SEC-064 — لا رمز إعادة تعيين لحساب خدمة / No password-reset token for a service account
Pattern    : unwanted
Statement  : If a password reset is requested for the email of a service account, then the system shall issue no reset token.
Traces     : US-SEC-018
Entities   : ENT-SEC-001, ENT-SEC-012
Rationale  : RULE-SEC-010; the contact email of a service account is never a reset target (ADR-SEC-032)
Source     : POL-SEC-016; ADR-SEC-019
Priority   : —
#### AC-SEC-067 — [REQ-SEC-064]
Given  : the contact email of a service account
When   : a password reset is requested for it
Then   : the system creates 0 PasswordResetToken rows, dispatches no reset notification, and returns the same generic confirmation it returns for an unregistered email

### REQ-SEC-065 — تدقيق المصادقة الآلية الناجحة / Audit a successful machine authentication
Pattern    : event
Statement  : When a service account authenticates successfully, the system shall append one SERVICE_AUTH_SUCCESS audit entry whose actor is that service account.
Traces     : US-SEC-019
Entities   : ENT-SEC-011, ENT-SEC-001
Rationale  : machine authentications are visible like any principal's logins
Source     : POL-SEC-019, POL-SEC-009; ADR-SEC-017
Priority   : HIGH
#### AC-SEC-068 — [REQ-SEC-065]
Given  : an ACTIVE service account with an active credential
When   : it authenticates successfully
Then   : the system appends one AuditLogEntry with eventTypeCode=SERVICE_AUTH_SUCCESS and actorUserId equal to the service account's userPk, and alters no existing entry

### REQ-SEC-066 — تدقيق المصادقة الآلية الفاشلة / Audit a failed machine authentication
Pattern    : unwanted
Statement  : If the system rejects a machine authentication request or a request bearing a service-account access token, then the system shall append one SERVICE_AUTH_FAILED audit entry attributed to the service account it names.
Traces     : US-SEC-019
Entities   : ENT-SEC-011, ENT-SEC-001, ENT-SEC-014
Rationale  : failed machine authentications are visible like failed logins
Source     : POL-SEC-019, POL-SEC-009; ADR-SEC-017
Priority   : HIGH
#### AC-SEC-069 — [REQ-SEC-066]
Given  : the client identifier of an ACTIVE service account
When   : a machine authentication request presents it with a wrong secret
Then   : the system appends one AuditLogEntry with eventTypeCode=SERVICE_AUTH_FAILED and actorUserId equal to that service account's userPk
#### AC-SEC-070 — [REQ-SEC-066]
Given  : an unexpired access token issued from a credential that has since been revoked
When   : a request bearing that token is rejected
Then   : the system appends one AuditLogEntry with eventTypeCode=SERVICE_AUTH_FAILED and actorUserId equal to the token's service account
#### AC-SEC-071 — [REQ-SEC-066]
Given  : a client identifier that names no user
When   : a machine authentication request presents it
Then   : the system appends one AuditLogEntry with eventTypeCode=SERVICE_AUTH_FAILED, an empty actorUserId and targetRef equal to the submitted client identifier

### REQ-SEC-067 — تدقيق إصدار بيانات الاعتماد / Audit credential issuance
Pattern    : event
Statement  : When an administrator issues a service-account credential, the system shall append one SERVICE_CREDENTIAL_ISSUED audit entry whose target is that service account.
Traces     : US-SEC-019
Entities   : ENT-SEC-011, ENT-SEC-014
Rationale  : the credential lifecycle is audited
Source     : POL-SEC-019
Priority   : HIGH
#### AC-SEC-072 — [REQ-SEC-067]
Given  : an ACTIVE service account
When   : an administrator issues a credential for it
Then   : the system appends one AuditLogEntry with eventTypeCode=SERVICE_CREDENTIAL_ISSUED, actorUserId equal to the administrator and targetRef naming the service account, with no secret in any of its fields

### REQ-SEC-068 — تدقيق إلغاء بيانات الاعتماد / Audit credential revocation
Pattern    : event
Statement  : When an administrator revokes a service-account credential, the system shall append one SERVICE_CREDENTIAL_REVOKED audit entry whose target is that service account.
Traces     : US-SEC-019
Entities   : ENT-SEC-011, ENT-SEC-014
Rationale  : the credential lifecycle is audited
Source     : POL-SEC-019
Priority   : HIGH
#### AC-SEC-073 — [REQ-SEC-068]
Given  : an active credential
When   : an administrator revokes it
Then   : the system appends one AuditLogEntry with eventTypeCode=SERVICE_CREDENTIAL_REVOKED, actorUserId equal to the administrator and targetRef naming the service account

### REQ-SEC-069 — تدقيق تغيير أدوار حساب الخدمة / Audit a service account's role changes
Pattern    : event
Statement  : When an administrator assigns a role to or revokes a role from a service account, the system shall append one audit entry whose target is that service account.
Traces     : US-SEC-019
Entities   : ENT-SEC-011, ENT-SEC-003
Rationale  : a service account's role changes are audited as a human's; a change to a grant of one of its roles is already audited against that role (REQ-SEC-024)
Source     : POL-SEC-019, POL-SEC-009
Priority   : HIGH
#### AC-SEC-074 — [REQ-SEC-069]
Given  : a service account and an active role
When   : an administrator assigns the role to it
Then   : the system appends one AuditLogEntry with eventTypeCode=ROLE_ASSIGNED and targetRef naming the service account

### REQ-SEC-070 — تدقيق إنشاء حساب الخدمة / Audit service-account creation
Pattern    : event
Statement  : When an administrator creates a service account, the system shall append one SERVICE_ACCOUNT_CREATED audit entry whose target is that service account.
Traces     : US-SEC-019, US-SEC-013
Entities   : ENT-SEC-011, ENT-SEC-001
Rationale  : every machine principal's existence starts with an audited administrative act
Source     : POL-SEC-019; module-registry-sec.md → AUTO-DECISIONS (AUDIT_EVENT_TYPE)
Priority   : HIGH
#### AC-SEC-075 — [REQ-SEC-070]
Given  : an administrator on the Users screen
When   : they create a service account
Then   : the system appends one AuditLogEntry with eventTypeCode=SERVICE_ACCOUNT_CREATED, actorUserId equal to the administrator and targetRef naming the new service account

### REQ-SEC-071 — تدقيق تعطيل حساب الخدمة / Audit service-account deactivation
Pattern    : event
Statement  : When an administrator deactivates a service account, the system shall append one SERVICE_ACCOUNT_DEACTIVATED audit entry whose target is that service account.
Traces     : US-SEC-019, US-SEC-017
Entities   : ENT-SEC-011, ENT-SEC-001
Rationale  : cutting a machine caller off is an audited act
Source     : POL-SEC-019; module-registry-sec.md → AUTO-DECISIONS (AUDIT_EVENT_TYPE)
Priority   : HIGH
#### AC-SEC-076 — [REQ-SEC-071]
Given  : an ACTIVE service account
When   : an administrator deactivates it
Then   : the system appends one AuditLogEntry with eventTypeCode=SERVICE_ACCOUNT_DEACTIVATED, actorUserId equal to the administrator and targetRef naming the service account

### REQ-SEC-072 — قراءة مستخدم مع أدواره / Read a user with its roles (G5, ADDED)
Pattern    : event
Statement  : When an administrator opens a user's detail, the system shall return that user together with its assigned roles.
Traces     : US-SEC-004
Entities   : ENT-SEC-001, ENT-SEC-003
Rationale  : API-SEC-032 was built and declared (ADR-SEC-038) with no SRS event pattern behind it — C5.15 found the gap; this closes it. ADDITIVE, no new behaviour (the endpoint already exists)
Source     : G5 (pass-1 review); ADR-SEC-038; C5.15
Priority   : —
#### AC-SEC-078 — [REQ-SEC-072]
Given  : a user with 2 assigned roles
When   : an administrator opens that user's detail
Then   : the system returns the user's fields and exactly its 2 roles, each with its code and both names

### REQ-SEC-073 — قراءة دور / Read a role (G5, ADDED)
Pattern    : event
Statement  : When an administrator opens a role's detail, the system shall return that role's fields.
Traces     : US-SEC-005
Entities   : ENT-SEC-002
Rationale  : API-SEC-033 was built and declared (ADR-SEC-038) with no SRS event pattern behind it — C5.15
Source     : G5 (pass-1 review); ADR-SEC-038; C5.15
Priority   : —
#### AC-SEC-079 — [REQ-SEC-073]
Given  : an existing role
When   : an administrator opens its detail
Then   : the system returns every field of that role

### REQ-SEC-074 — تحديث اسم الدور ووصفه / Update a role's names and descriptions (G5, ADDED)
Pattern    : event
Statement  : When an administrator updates a role's bilingual names or descriptions, the system shall record the change.
Traces     : US-SEC-005
Entities   : ENT-SEC-002
Rationale  : API-SEC-034 was built and declared (ADR-SEC-038) with no SRS event pattern behind it — C5.15
Source     : G5 (pass-1 review); ADR-SEC-038; C5.15
Priority   : —
#### AC-SEC-080 — [REQ-SEC-074]
Given  : an existing role
When   : an administrator submits new bilingual names and descriptions for it
Then   : the system updates those four fields and leaves the role's code and active flag unchanged

### REQ-SEC-075 — قراءة شجرة منح الدور / Read a role's grant tree (G5, ADDED)
Pattern    : event
Statement  : When an administrator opens a role's grant editor, the system shall return that role's full module/screen/action grant tree.
Traces     : US-SEC-005
Entities   : ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Rationale  : API-SEC-035 was built and declared (ADR-SEC-038) with no SRS event pattern behind it — C5.15
Source     : G5 (pass-1 review); ADR-SEC-038; C5.15
Priority   : —
#### AC-SEC-081 — [REQ-SEC-075]
Given  : a role holding 1 module grant, 2 screen grants and 3 action grants
When   : an administrator opens its grant editor
Then   : the system returns the module, its 2 screens and their 3 actions, assembled as a tree

### REQ-SEC-076 — بحث طلبات التسجيل المعلّقة / Search sign-up requests (G5, ADDED)
Pattern    : event
Statement  : When an administrator searches sign-up requests, the system shall return the requests matching the search filters.
Traces     : US-SEC-002
Entities   : ENT-SEC-013
Rationale  : API-SEC-036 was built and declared (ADR-SEC-038), traced to REQ-SEC-004/005 (approve/reject, a different operation) with no SRS event pattern for the search itself — C5.15
Source     : G5 (pass-1 review); ADR-SEC-038; C5.15
Priority   : —
#### AC-SEC-082 — [REQ-SEC-076]
Given  : sign-up requests in PENDING, APPROVED and REJECTED status
When   : an administrator searches filtered by statusCode=PENDING
Then   : the system returns exactly the PENDING requests

### REQ-SEC-077 — تسجيل تعذّر تكامل الإشعارات / Record an unreachable Notifications integration (G6, ADDED)
Pattern    : unwanted
Statement  : If the Notifications integration is unavailable when a reset token is issued, then the system shall complete the reset request and record the failed dispatch.
Traces     : US-SEC-012
Entities   : ENT-SEC-012, ENT-SEC-011
Rationale  : POL-SEC-024 requires recording that an optional integration could not be reached; no v1 REQ carried it, and API-SEC-003 only said a dispatch failure does not roll back — the record itself was missing
Source     : G6 (pass-1 review); POL-SEC-024
Priority   : —
#### AC-SEC-083 — [REQ-SEC-077]
Given  : the Notifications integration is unavailable and a reset token is issued
When   : the dispatch is attempted
Then   : the system completes the reset request unchanged (REQ-SEC-006) and appends one WARN application-log entry naming the user id, never the email — no new AUDIT_EVENT_TYPE code and no CHECK re-creation

### REQ-SEC-078 — حدّ أعلى لبيانات اعتماد حساب الخدمة / An upper limit on a service account's active credentials (G3, ADDED)
Pattern    : unwanted
Statement  : If a service account already holds ten unrevoked credentials, then the system shall refuse to issue another and create no credential.
Traces     : US-SEC-016
Entities   : ENT-SEC-014, ENT-SEC-001
Rationale  : every token call verifies the presented secret against each unrevoked hash, so an unbounded credential set makes the cost of one authentication grow without bound, and a compromised account harder to clean up. Rotation needs two at a time, well under the cap
Source     : G3 and G7 (pass-1 review); ADR-SEC-042; Google Cloud's per-account service-account-key limit
Priority   : —
#### AC-SEC-084 — [REQ-SEC-078]
Given  : a service account holding ten unrevoked credentials
When   : an administrator issues another credential for it
Then   : the system refuses with ar: "بلغ عدد بيانات الاعتماد النشطة الحد الأقصى (10)" · en: "Active credential limit (10) reached", no credential row is created, and revoking one then lets the next issuance succeed

### REQ-SEC-079 — تدقيق إعادة تفعيل حساب الخدمة / Audit service-account reactivation (RG6, ADDED)
Pattern    : event
Statement  : When an administrator reactivates a service account, the system shall append one SERVICE_ACCOUNT_REACTIVATED audit entry whose target is that service account.
Traces     : US-SEC-017
Entities   : ENT-SEC-011, ENT-SEC-001
Rationale  : deactivating a service account is audited (REQ-SEC-071, SERVICE_ACCOUNT_DEACTIVATED), but reactivating it — which restores every never-revoked secret to live use (REQ-SEC-062) — appended no audit entry; the act that restores machine access was the one unaudited step of the account lifecycle, against POL-SEC-019/POL-SEC-009's intent
Source     : RG6 (pass-1 REVISE review); ADR-SEC-047; module-registry-sec.md → AUTO-DECISIONS (AUDIT_EVENT_TYPE)
Priority   : HIGH
#### AC-SEC-085 — [REQ-SEC-079]
Given  : a DISABLED service account
When   : an administrator reactivates it
Then   : the system appends one AuditLogEntry with eventTypeCode=SERVICE_ACCOUNT_REACTIVATED, actorUserId equal to the administrator and targetRef naming the service account

## A5 — Business rules

v2 adds RULE-SEC-008 … RULE-SEC-013. RULE-SEC-001 … RULE-SEC-007 are unchanged and carried from v1; each
applies to a service account's roles and grants as to a human's (POL-SEC-012).

### RULE-SEC-008 — بيانات الاعتماد لحساب خدمة نشط فقط / Credentials only for an active service account
Scope      : ENT-SEC-014
Trigger    : on create (credential issuance)
Statement  : The system shall prevent issuing a credential when the target user is not of principal type SERVICE or is not ACTIVE.
Data source: ENT-SEC-001.principalTypeCode · ENT-SEC-001.statusCode
Message    : ar: "لا تُصدَر بيانات الاعتماد إلا لحساب خدمة نشط" · en: "Credentials can be issued only for an active service account"
Traces     : REQ-SEC-052
Source     : POL-SEC-016, POL-SEC-022; ADR-SEC-013

### RULE-SEC-009 — مصادقة آلية ببيانات اعتماد نشطة لحساب خدمة نشط فقط / Machine authentication only by an active credential of an active service account
Scope      : ENT-SEC-014, ENT-SEC-001
Trigger    : on machine authentication request, and on every request bearing a service-account access token
Statement  : The system shall reject a machine authentication request or a service-account access token when the named user is not an ACTIVE user of principal type SERVICE, when the presented secret matches none of that user's unrevoked credentials, or when the credential that issued the token is revoked.
Data source: ENT-SEC-001.username · ENT-SEC-001.principalTypeCode · ENT-SEC-001.statusCode · ENT-SEC-014.userId · ENT-SEC-014.secretHash · ENT-SEC-014.revokedAt
Message    : ar: "بيانات اعتماد العميل غير صالحة" · en: "Invalid client credentials"
Traces     : REQ-SEC-047, REQ-SEC-058, REQ-SEC-059, REQ-SEC-060
Source     : POL-SEC-014, POL-SEC-017; ADR-SEC-014; RFC 6749 §5.2
Test-Hint  : one message on every failure path — the answer never tells which condition failed

### RULE-SEC-010 — لا استخدام تفاعلي لحساب خدمة / No interactive use of a service account
Scope      : ENT-SEC-001, ENT-SEC-010, ENT-SEC-012
Trigger    : on login; on password-reset request
Statement  : The system shall reject an interactive login, and shall issue no password-reset token, when the named user is of principal type SERVICE.
Data source: ENT-SEC-001.principalTypeCode · ENT-SEC-001.username · ENT-SEC-001.email
Message    : ar: "بيانات الدخول غير صحيحة" · en: "Invalid credentials" (login — the v1 message, so the answer does not disclose the account type); the reset request answers with the generic confirmation REQ-SEC-006 gives for any email
Traces     : REQ-SEC-063, REQ-SEC-064
Source     : POL-SEC-016; ADR-SEC-019; POL-SEC-004 (non-disclosure discipline)

### RULE-SEC-011 — لا إلغاء لبيانات اعتماد ملغاة / No revoking an already revoked credential
Scope      : ENT-SEC-014
Trigger    : on update (credential revocation)
Statement  : The system shall prevent revoking a credential whose revocation time is already set.
Data source: ENT-SEC-014.revokedAt
Message    : ar: "بيانات الاعتماد هذه ملغاة بالفعل" · en: "This credential is already revoked"
Traces     : REQ-SEC-061
Source     : POL-SEC-017

### RULE-SEC-012 — نوع الأساس الأمني ثابت بعد الإنشاء / Principal type fixed after creation
Scope      : ENT-SEC-001
Trigger    : on update (user)
Statement  : The system shall prevent any update that changes an existing user's principal type.
Data source: ENT-SEC-001.principalTypeCode
Message    : ar: "لا يمكن تغيير نوع الأساس الأمني بعد إنشائه" · en: "The principal type cannot be changed after creation"
Traces     : REQ-SEC-039
Source     : POL-SEC-022, POL-SEC-013

### RULE-SEC-013 — حدّ بيانات الاعتماد النشطة / Active credential limit
Scope      : ENT-SEC-014
Trigger    : on create (credential issuance)
Statement  : The system shall prevent issuing a credential when the target account already holds ten unrevoked credentials.
Data source: ENT-SEC-014.revokedAt over ENT-SEC-014.userId
Message    : ar: "بلغ عدد بيانات الاعتماد النشطة الحد الأقصى (10)" · en: "Active credential limit (10) reached"
Traces     : REQ-SEC-078
Source     : ADR-SEC-042; G3 (pass-1 review)

## A6 — Lookups

**USER_STATUS** — owned by SEC — used by ENT-SEC-001.statusCode — control type: lookup
| Code | Label (ar) | Label (en) |
|---|---|---|
| PENDING | معلّق | Pending |
| ACTIVE | نشط | Active |
| DISABLED | معطّل | Disabled |
Source: AUTO (module-registry-sec.md → AUTO-DECISIONS), values fixed by the A7 lifecycle below.
(v2) unchanged — a service account uses ACTIVE and DISABLED only.

**SIGNUP_STATUS** — owned by SEC — used by ENT-SEC-013.statusCode — control type: lookup
| Code | Label (ar) | Label (en) |
|---|---|---|
| PENDING | معلّق | Pending |
| APPROVED | مقبول | Approved |
| REJECTED | مرفوض | Rejected |
DEFAULT: a distinct lookup from USER_STATUS because a rejected sign-up never becomes a
User row (no DISABLED-equivalent needed) — Source: this stage, applying profile.conventions.lookups
("no hardcoded enums") to the SignupRequest lifecycle (REQ-SEC-003/004/005) — Override: merge
into USER_STATUS if the client prefers one shared status set.

**PRINCIPAL_TYPE** — owned by SEC — used by ENT-SEC-001.principalTypeCode — control type: lookup — (v2 ADDED)
| Code | Label (ar) | Label (en) |
|---|---|---|
| HUMAN | مستخدم بشري | Human user |
| SERVICE | حساب خدمة | Service account |
Source: module-registry-sec.md → AUTO-DECISIONS (PRINCIPAL_TYPE, HUMAN / SERVICE); storage follows the v1
decision for SEC-owned lookups (project-registry DECISION INDEX #7, ADR-SEC-001) — not reopened here.

**AUDIT_EVENT_TYPE** — owned by SEC — used by ENT-SEC-011.eventTypeCode — control type: lookup — (v2 MODIFIED: 6 values added)
| Code | Label (ar) | Label (en) |
|---|---|---|
| LOGIN_SUCCESS | دخول ناجح | Login success |
| LOGIN_FAILED | دخول فاشل | Login failed |
| LOGOUT | خروج | Logout |
| PASSWORD_RESET_REQUESTED | طلب إعادة تعيين | Password reset requested |
| PASSWORD_RESET_COMPLETED | إتمام إعادة التعيين | Password reset completed |
| ROLE_ASSIGNED | إسناد دور | Role assigned |
| ROLE_REVOKED | سحب دور | Role revoked |
| MODULE_GRANTED | منح وحدة | Module granted |
| MODULE_REVOKED | سحب منح وحدة | Module revoked |
| SCREEN_GRANTED | منح شاشة | Screen granted |
| SCREEN_REVOKED | سحب منح شاشة | Screen revoked |
| ACTION_GRANTED | منح إجراء | Action granted |
| ACTION_REVOKED | سحب منح إجراء | Action revoked |
| SESSION_TERMINATED | إنهاء جلسة | Session terminated |
| SERVICE_ACCOUNT_CREATED | إنشاء حساب خدمة | Service account created |
| SERVICE_ACCOUNT_DEACTIVATED | تعطيل حساب خدمة | Service account deactivated |
| SERVICE_ACCOUNT_REACTIVATED | إعادة تفعيل حساب خدمة | Service account reactivated |
| SERVICE_CREDENTIAL_ISSUED | إصدار بيانات اعتماد خدمة | Service credential issued |
| SERVICE_CREDENTIAL_REVOKED | إلغاء بيانات اعتماد خدمة | Service credential revoked |
| SERVICE_AUTH_SUCCESS | مصادقة خدمة ناجحة | Service authentication success |
| SERVICE_AUTH_FAILED | مصادقة خدمة فاشلة | Service authentication failed |
Source: module-registry-sec.md → AUTO-DECISIONS; profiles/erp.yaml conventions.lookups. The seven SERVICE_*
values are the v2 AUTO extension (SERVICE_ACCOUNT_REACTIVATED added by the pass-1 REVISE review, RG6, ADR-SEC-047);
ROLE_ASSIGNED / ROLE_REVOKED and the grant values apply to service accounts unchanged.

Consumed lookups: none — SEC is a Tier-0 foundation module.

## A7 — Status lifecycle

**ENT-SEC-001 User.statusCode (USER_STATUS, 3 states, >2 transitions — diagram required)**
```
PENDING --(REQ-SEC-004, admin approves sign-up)--> ACTIVE
ACTIVE  --(REQ-SEC-011, admin deactivates)--------> DISABLED
DISABLED--(REQ-SEC-031, admin reactivates)--------> ACTIVE
```
(A User created directly by an administrator — REQ-SEC-009 — starts at ACTIVE, bypassing PENDING.)
(v2) A service account starts at ACTIVE (REQ-SEC-036) and never enters PENDING. Its ACTIVE → DISABLED
transition also rejects every credential and token of the account (REQ-SEC-060, RULE-SEC-009), and its
DISABLED → ACTIVE transition restores the credentials never individually revoked (REQ-SEC-062).

**ENT-SEC-013 SignupRequest.statusCode (SIGNUP_STATUS, 3 states)**
```
PENDING --(REQ-SEC-004, approve)--> APPROVED
PENDING --(REQ-SEC-005, reject)---> REJECTED
```
APPROVED and REJECTED are terminal — no further transition.

**ENT-SEC-014 ServiceAccountCredential (v2)** — two states only, active (revokedAt empty) and revoked
(REQ-SEC-058); revoked is terminal (RULE-SEC-011) — not applicable for a diagram.

All other statuses in this module (grant rows, sessions) are a binary
active/terminated flag, not a multi-state lifecycle — not applicable for a diagram.

**DEFAULT — reset-token expiry window**: not stated by the plan; this stage applies a
DEFAULT of 30 minutes from `requestedAt` to `expiresAt` (industry-standard short-lived
reset window) — Source: domain best practice (no conflicting statement in
security-module-plan-en.md or the knowledge file) — Override: configurable value if the
client states a different window; non-breaking, no ADR required (a pure numeric default
with no story/policy it could contradict).

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM candidate (assigned by P2) |
|---|---|---|---|---|
None — SEC is ROOT; it consumes no entity owned by another in-scope module. (v2: unchanged.)

| External service | Purpose | Integration kind |
|---|---|---|
| Notifications (ready, external) | password-reset message (REQ-SEC-029) | SOFT / optional, per new project/integration-notifications-fileservice.md §1 |

**(v2) External machine callers.** The legacy Oracle event consumer (a daemon, outside the platform) is a
*caller* of SEC, not a dependency: it authenticates as a SEC service account and then calls FIN. SEC does
not depend on it. Workload identity federation and mTLS stay out of scope.

| External caller | Authenticates as | Integration kind |
|---|---|---|
| Oracle event consumer (daemon, out of scope) | a service account (ENT-SEC-001, principal type SERVICE) holding one role an administrator assigns | inbound, machine authentication (REQ-SEC-046) |

**(v2) Machine-facing operation (no screen).** Machine authentication has no screen; its API expectation
is stated here, so no SCR-REQ carries it (DEFAULT D3).

| Operation | Verb | Path (per base path) | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| issue access token (client credentials) | POST | /api/v1/sec/auth/token | grant type `client_credentials`, client identifier, client secret | access token, token type, expiry — no refresh token | RULE-SEC-009 | REQ-SEC-046, REQ-SEC-047, REQ-SEC-048, REQ-SEC-049, REQ-SEC-055, REQ-SEC-057 |
Every other secured endpoint also accepts a service-account access token, validated per request against live
state (REQ-SEC-050, REQ-SEC-059, REQ-SEC-060; RULE-SEC-009). That validation is not an operation of its own.

**AMENDMENT 2026-09-11 — the EXPOSED direction.** The two tables above describe only what SEC
*consumes*; both remain true. SEC additionally exposes one read-only inbound surface. It registers
no entity, table or column, so it carries no XM row here — formal `XM-*` ids for this direction are
assigned by the *consuming* module's own P2, not by SEC.

| Exposed entity | Owner ENT id | Consumer | Read-model | Surface |
|---|---|---|---|---|
| User (contact details only: email + both display names + active) | ENT-SEC-001 | NOTIF — XM-NOTIF-001, and REQ-SEC-029's delivery half | `UserContact` | `com.erp.sec.crossmodule.SecUserDirectoryApi` (REQ-SEC-034) |
| Holders of a permission code (spans User → UserRoleAssignment → RoleActionGrant → ActionRegistry) | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | FIN — RULE-FIN-015 separation of duties | `List<Long>` (user ids only) | `com.erp.sec.crossmodule.SecUserDirectoryApi` (REQ-SEC-035, QR-SEC-039) |
(v2) Both surfaces cover service accounts unchanged. The contact read returns a service account's contact
email (ADR-SEC-032). The holder set includes a service account that holds the code through its roles.

# PART B — SCREEN REQUIREMENTS

v2 re-emits SCR-REQ-SEC-004 and SCR-REQ-SEC-007 (MODIFIED). SCR-REQ-SEC-001 … 003, 005, 006 and 008 … 010
are unchanged and carried from v1. No screen is added: service accounts are administered in the existing
screens (POL-SEC-018).

## SCR-REQ-SEC-004 — المستخدمون / Users
### B1 — Definition
Purpose      : إدارة المستخدمين وإسناد الأدوار لهم؛ (v2) وإنشاء حسابات الخدمة وتمييزها عن المستخدمين البشريين، وإصدار بيانات اعتمادها وإلغائها.
Entities     : ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-013, ENT-SEC-014
Operations   : search, create, read, update, activate, deactivate; approve/reject a SignupRequest; (v2) create a service account, filter by principal type, list / issue / revoke a service account's credentials
Users        : مسؤول الأمان
Navigation   : SEC → Authorization → Users; to: user detail (roles tab; v2: credentials tab, for a service account only)
Content shape: flat record (search list + entry form; roles and — for a service account — credentials shown as repeating sub-lists on the detail)
Traces       : REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-031, REQ-SEC-004, REQ-SEC-005, REQ-SEC-036, REQ-SEC-037, REQ-SEC-039, REQ-SEC-040, REQ-SEC-041, REQ-SEC-043, REQ-SEC-051, REQ-SEC-052, REQ-SEC-053, REQ-SEC-056, REQ-SEC-058, REQ-SEC-060, REQ-SEC-061, REQ-SEC-062
Composite    : Search + Entry = ONE screen requirement
### B2 — Search / list
Filters: username/email, fullName, statusCode, (v2) principalTypeCode (lookup key `PRINCIPAL_TYPE`) — each corresponds to a result column (REQ-SEC-040, REQ-SEC-041).
### B3 — Input
Fields: username, email, fullNameAr, fullNameEn, statusCode (ENT-SEC-001); roles multi-select (ENT-SEC-003). Buttons: Activate/Deactivate → REQ-SEC-031/REQ-SEC-011; a separate "Pending sign-ups" tab lists SignupRequest rows with Approve/Reject → REQ-SEC-004/REQ-SEC-005.
(v2) principalTypeCode (ENT-SEC-001, lookup `PRINCIPAL_TYPE`) is entered on create only and is read-only afterwards (RULE-SEC-012). For a service account, `email` is labelled as the contact email (ADR-SEC-032), and no password is entered. The account is created with no role (REQ-SEC-036, REQ-SEC-037), and roles are assigned with the same multi-select (REQ-SEC-043). Deactivate / Activate act as for any user, with the effects in REQ-SEC-060 and REQ-SEC-062.
(v2) Credentials tab (service account only; ENT-SEC-014): list → REQ-SEC-056 (never the secret, REQ-SEC-053); "Issue credential" with an optional description → REQ-SEC-051, applies RULE-SEC-008, and shows the secret once with its copy-now message; "Revoke" per row → REQ-SEC-058, applies RULE-SEC-011.
### B4 — Access
Page code: SEC_USERS. Actions: VIEW (list/search, incl. v2 credentials list), CREATE (v2: incl. creating a service account), UPDATE (incl. activate/deactivate/approve/reject; v2: issue and revoke a credential), per §7.1 (RULE-SEC-007 gateway). Roles: مسؤول الأمان / security administrator holds all three.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| search users | POST | /api/v1/sec/users/search | filters (v2: + principalTypeCode), paging | Page\<User\> (v2: with principalTypeCode) | — | REQ-SEC-009, REQ-SEC-040, REQ-SEC-041 |
| create user | POST | /api/v1/sec/users | user fields (v2: + principalTypeCode) | User | — | REQ-SEC-009, REQ-SEC-036, REQ-SEC-037 |
| update user | PUT | /api/v1/sec/users/{id} | user fields | User | RULE-SEC-012 | REQ-SEC-009, REQ-SEC-039 |
| assign roles | PUT | /api/v1/sec/users/{id}/roles | role ids | User with roles | — | REQ-SEC-010, REQ-SEC-043 |
| deactivate user | DELETE | /api/v1/sec/users/{id} | id | confirmation | RULE (session termination, REQ-SEC-011) | REQ-SEC-011, REQ-SEC-060 |
| reactivate user | PATCH | /api/v1/sec/users/{id} | status=ACTIVE | User | — | REQ-SEC-031, REQ-SEC-062 |
| approve/reject signup | PATCH | /api/v1/sec/signup-requests/{id} | decision | User (on approve) / SignupRequest (on reject) | — | REQ-SEC-004, REQ-SEC-005 |
| list credentials (v2) | GET | /api/v1/sec/users/{id}/credentials | id | list of ServiceAccountCredential (no secret, no hash) | — | REQ-SEC-056, REQ-SEC-053 |
| issue credential (v2) | POST | /api/v1/sec/users/{id}/credentials | description | ServiceAccountCredential + secret (this response only) | RULE-SEC-008 | REQ-SEC-051, REQ-SEC-052 |
| revoke credential (v2) | DELETE | /api/v1/sec/users/{id}/credentials/{credentialId} | ids | confirmation | RULE-SEC-011 | REQ-SEC-058, REQ-SEC-061 |

## SCR-REQ-SEC-007 — لوحة تحكم الأمان / Admin dashboard
### B1 — Definition
Purpose      : عرض حالة الأمان العامة للمنصة بشكل حي؛ (v2) مع فصل أرقام نظرة المستخدمين العامة حسب نوع الأساس الأمني.
Entities     : ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011
Operations   : read (aggregate figures)
Users        : مسؤول الأمان (لكل عنصر لوحة تحكم بمقتضى صلاحيته الخاصة)
Navigation   : SEC → Monitoring → Dashboard; to: SCR-REQ-SEC-008 (audit), SCR-REQ-SEC-009 (sessions)
Content shape: other (dashboard — a grid of independent widgets, not a record/list)
Traces       : REQ-SEC-022, REQ-SEC-023, REQ-SEC-042
Composite    : single screen (widgets are not separate screen requirements)
### B2 — Search / list
Not applicable — aggregate widgets, not a browsable list.
### B3 — Input
Read-only; no data entry.
### B4 — Access
Page code: SEC_DASHBOARD. Action: VIEW; each widget additionally requires the VIEW permission of the screen it summarizes (REQ-SEC-023) — e.g. the active-sessions widget requires SEC_SESSIONS VIEW, and the users overview (v2: per principal type) requires SEC_USERS VIEW.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| dashboard summary | GET | /api/v1/sec/dashboard | — | live aggregate figures per widget the caller may see (v2: users overview per principal type) | — | REQ-SEC-022, REQ-SEC-023, REQ-SEC-042 |

# STANDALONE

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-SEC-001 | REQ-SEC-001, REQ-SEC-002 | AC-SEC-001, AC-SEC-002 | — | ENT-SEC-001, ENT-SEC-010, ENT-SEC-011 | SCR-REQ-SEC-001 |
| US-SEC-002 | REQ-SEC-003, REQ-SEC-004, REQ-SEC-005, REQ-SEC-076 (G5) | AC-SEC-003…005, AC-SEC-082 | — | ENT-SEC-013, ENT-SEC-001 | SCR-REQ-SEC-002, SCR-REQ-SEC-004 |
| US-SEC-003 | REQ-SEC-006, REQ-SEC-007, REQ-SEC-008 | AC-SEC-006…008 | RULE-SEC-006 | ENT-SEC-012, ENT-SEC-001 | SCR-REQ-SEC-003 |
| US-SEC-004 | REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-031, REQ-SEC-072 (G5) | AC-SEC-009…011, AC-SEC-031, AC-SEC-078 | — | ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-010 | SCR-REQ-SEC-004 |
| US-SEC-005 | REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-030, REQ-SEC-073, REQ-SEC-074, REQ-SEC-075 (G5) | AC-SEC-012…015, AC-SEC-030, AC-SEC-079…081 | RULE-SEC-001, RULE-SEC-002, RULE-SEC-003, RULE-SEC-007 | ENT-SEC-002, ENT-SEC-004…009 | SCR-REQ-SEC-005 |
| US-SEC-006 | REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019 | AC-SEC-016…019 | RULE-SEC-004 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 | SCR-REQ-SEC-006 |
| US-SEC-007 | REQ-SEC-020, REQ-SEC-035 | AC-SEC-020, AC-SEC-035 | RULE-SEC-005 | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | SCR-REQ-SEC-005 |
| US-SEC-008 | REQ-SEC-021, REQ-SEC-032, REQ-SEC-033 | AC-SEC-021, AC-SEC-032, AC-SEC-033 | — | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | SCR-REQ-SEC-010 |
| US-SEC-009 | REQ-SEC-022, REQ-SEC-023 | AC-SEC-022, AC-SEC-023 | — | ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 | SCR-REQ-SEC-007 |
| US-SEC-010 | REQ-SEC-024, REQ-SEC-025, REQ-SEC-026 | AC-SEC-024…026 | — | ENT-SEC-011 | SCR-REQ-SEC-008 |
| US-SEC-011 | REQ-SEC-027, REQ-SEC-028 | AC-SEC-027, AC-SEC-028 | — | ENT-SEC-010 | SCR-REQ-SEC-009 |
| US-SEC-012 | REQ-SEC-029, REQ-SEC-034, REQ-SEC-077 (G6) | AC-SEC-029, AC-SEC-034, AC-SEC-083 | — | ENT-SEC-012, ENT-SEC-001 | SCR-REQ-SEC-003 |
| US-SEC-013 (v2) | REQ-SEC-036, REQ-SEC-037, REQ-SEC-038, REQ-SEC-039, REQ-SEC-040, REQ-SEC-041, REQ-SEC-042, REQ-SEC-070 | AC-SEC-036…042, AC-SEC-075 | RULE-SEC-012 | ENT-SEC-001, ENT-SEC-003, ENT-SEC-011, ENT-SEC-013 | SCR-REQ-SEC-004, SCR-REQ-SEC-007 |
| US-SEC-014 (v2) | REQ-SEC-043, REQ-SEC-044, REQ-SEC-045 | AC-SEC-043…045, AC-SEC-077 | — (v1 RULE-SEC-001…003, 005, 007 apply; the role shape is settled by ADR-SEC-034) | ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-004, ENT-SEC-007…009 | SCR-REQ-SEC-004 |
| US-SEC-015 (v2) | REQ-SEC-046, REQ-SEC-047, REQ-SEC-048, REQ-SEC-049, REQ-SEC-050 | AC-SEC-046…051 | RULE-SEC-009 | ENT-SEC-001, ENT-SEC-010, ENT-SEC-014 | — (machine-facing, A8) |
| US-SEC-016 (v2) | REQ-SEC-051, REQ-SEC-052, REQ-SEC-053, REQ-SEC-054, REQ-SEC-055, REQ-SEC-056, REQ-SEC-057 | AC-SEC-052…060 | RULE-SEC-008 | ENT-SEC-001, ENT-SEC-014 | SCR-REQ-SEC-004 |
| US-SEC-017 (v2) | REQ-SEC-050, REQ-SEC-058, REQ-SEC-059, REQ-SEC-060, REQ-SEC-061, REQ-SEC-062, REQ-SEC-071, REQ-SEC-079 (RG6) | AC-SEC-051, AC-SEC-061…065, AC-SEC-076, AC-SEC-085 | RULE-SEC-009, RULE-SEC-011 | ENT-SEC-001, ENT-SEC-011, ENT-SEC-014 | SCR-REQ-SEC-004 |
| US-SEC-018 (v2) | REQ-SEC-063, REQ-SEC-064 | AC-SEC-066, AC-SEC-067 | RULE-SEC-010 | ENT-SEC-001, ENT-SEC-010, ENT-SEC-011, ENT-SEC-012 | SCR-REQ-SEC-001, SCR-REQ-SEC-003 (v1 screens, unchanged) |
| US-SEC-019 (v2) | REQ-SEC-065, REQ-SEC-066, REQ-SEC-067, REQ-SEC-068, REQ-SEC-069, REQ-SEC-070, REQ-SEC-071 | AC-SEC-068…076 | — | ENT-SEC-001, ENT-SEC-003, ENT-SEC-011, ENT-SEC-014 | SCR-REQ-SEC-008 (v1 screen, unchanged — new event types load from the lookup) |

Every story traces to ≥1 REQ; every REQ traces to ≥1 AC; every RULE traces to a REQ;
every SCR-REQ traces to ≥1 REQ (rows above). No orphan, no dangling id. US-SEC-015's
requirements have no screen: machine authentication is an API expectation only (A8).

## Decisions applied
| DEFAULT / ADR | What | Source | Override / status |
|---|---|---|---|
| DEFAULT | Password-reset token expiry = 30 minutes | domain best practice (A7 note) | configurable; non-breaking |
| DEFAULT | USER_STATUS/AUDIT_EVENT_TYPE/SIGNUP_STATUS initial lookup values | profiles/erp.yaml conventions.lookups + lookup-module-plan-en.md §3 | extend the value set as new events/states are needed; non-breaking |
| DEFAULT D1 (v2) | Service-account access token lifetime = 3600 seconds (AC-SEC-046, AC-SEC-062) | the v1 platform access-token lifetime (change manifest — Summary); revocation does not depend on it, because every request is checked against live state (ADR-SEC-014) | configurable; non-breaking |
| DEFAULT D2 (v2) | The client identifier of machine authentication is the service account's username | RFC 6749 §2.2 (client identifier); ADR-SEC-013 (a service account is a User) | a separate generated client id per account; non-breaking |
| DEFAULT D3 (v2) | Machine authentication's API expectation sits in A8, not in a screen requirement | engine §7 (SCR-REQ lists screens; this operation has none) | — |
| ADR-SEC-031 | The decided consumer role (one action grant, no VIEW) conflicts with the inherited VIEW gateway | business-policies decision #4; ADR-SEC-018; ADR-SEC-030; REQ-SEC-030; RULE-SEC-007; profile `gateway_action` | **SUPERSEDED** by ADR-SEC-034 — the human chose option A |
| ADR-SEC-032 | A service account's email is the responsible contact; its password hash is an unusable random value | NIST SP 800-53 Rev.5 AC-2; REQ-SEC-004 Note precedent | ACCEPTED (non-breaking) |
| ADR-SEC-034 | The VIEW gateway is part of the structural path to an action, so the consumer's role is FIN module + journal-entry screen + VIEW + CREATE; supersedes ADR-SEC-031 and overrides the "no VIEW grant" clause of ADR-SEC-018 and decision #4 | human decision at the P1 breaking-ambiguity stop; ADR-SEC-031 options table | ACCEPTED |
| ADR-SEC-033 | Each credential records its last successful use | POL-SEC-021 rotation; Google Cloud / GitHub key practice | ACCEPTED (non-breaking) |
| ADR-SEC-012 … ADR-SEC-030 | Upstream v2 decisions (mechanism, principal model, per-request live check, no forced expiry, no interactive use, audit scope, "exactly one permission", story split) | P0 / P0.5 dialogue | applied as-is (RESOLVED-IN-DIALOGUE) |
| ADR-SEC-046 (pass-1 REVISE, RG4) | C5.14's AC-block checker accepts both the labelled `Given :/When :/Then :` form every v2 AC uses and the unlabelled prose form every v1-carried AC block uses, instead of rewriting the 35 v1 AC blocks to match the checker | C5.14 finding, 35 rows, one systemic cause; `gov.py state` folds v1 AC text unchanged | ACCEPTED (non-breaking) — the fold-time normalization alternative is rejected because it would silently rewrite carried v1 text |
| ADR-SEC-047 (pass-1 REVISE, RG6) | Reactivating a service account gets its own audit code, SERVICE_ACCOUNT_REACTIVATED, symmetric to SERVICE_ACCOUNT_DEACTIVATED (REQ-SEC-071), rather than reusing an existing code or leaving the act unaudited | POL-SEC-019, POL-SEC-009; REQ-SEC-079 | ACCEPTED (non-breaking) |

## Access summary
| Page code | Screen | VIEW | CREATE | UPDATE | DELETE | Custom |
|---|---|---|---|---|---|---|
| SEC_LOGIN | Login | public | — | — | — | — |
| SEC_SIGNUP | Sign-up | public | — | — | — | — |
| SEC_PWD_RESET | Forgot/reset password | public | — | — | — | — |
| SEC_USERS | Users | role-granted (v2: incl. principal-type filter and credentials list) | role-granted (v2: incl. service account) | role-granted (incl. activate/deactivate, approve/reject signup; v2: issue/revoke credential) | — | — |
| SEC_ROLES | Roles & permissions | role-granted | role-granted | role-granted (grant tree edits) | role-granted (deactivate role) | — |
| SEC_MODULE_REGISTRY | Module/screen/action registry | role-granted | (via registering module's own call) | role-granted (deactivate row) | — | — |
| SEC_DASHBOARD | Admin dashboard | role-granted (+ per-widget VIEW of its source screen) | — | — | — | — |
| SEC_AUDIT_LOG | Audit log | role-granted | — | — | — | export (shares VIEW) |
| SEC_SESSIONS | Active sessions | role-granted | — | — | role-granted (terminate) | — |
| (menu) | Dynamic menu | derived from the above — no page code of its own | — | — | — | — |
| (machine token, v2) | no screen — A8 | authenticated by client credentials (RULE-SEC-009), no page code | — | — | — | — |
Every action beyond VIEW additionally requires VIEW on the same screen (RULE-SEC-007). This holds for a
service account's roles as for a human's; the consumer role's shape is settled by ADR-SEC-034 and stated by AC-SEC-077.
══════════════════════════════════════════════════════════════════
