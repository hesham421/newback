# المرجع: موديول الأمان SEC (`com.erp.sec`)

> **الحالة:** مُنفَّذ — كل مراحل الـ backend الثماني `COMPLETE` (CORE → ALIGN-BE) في
> `governance/shared/backend/modules/SEC/execution-state.json`؛ مرحلة الاختبار `TEST-PLAN-BE` = `PARTIAL`.
> وثائق الـ API المولّدة: `governance/shared/backend/modules/SEC/api-docs/` (11 ملف endpoints).
> **المصدر:** مستخرج من الكود بتاريخ 2026-10-01 — عند التعارض، الكود هو المرجع.
> **الفهرس:** [PLATFORM-MODULES-INTEGRATION-INDEX.md](PLATFORM-MODULES-INTEGRATION-INDEX.md)

SEC يوفّر: المصادقة بـ JWT، الجلسات، المستخدمين والأدوار، **سجلّ الموديولات/الشاشات/الإجراءات** (مصدر الصلاحيات والقائمة)، وسجلّ التدقيق الأمني.
كل موديول جديد **يتكامل معه إلزامياً** عبر ثلاث نقاط: الصلاحيات (`PermissionConstants`)، التسجيل (Flyway seed)، وبوابات `@PreAuthorize`.

---

## 1. المصادقة (JWT)

### 1.1 Endpoints (`/api/v1/sec/auth`, كلها POST)

| المسار | الطلب | `data` في الرد | الوصول |
|---|---|---|---|
| `/login` | `{username, password}` | `{accessToken, tokenType:"Bearer", expiresIn}` (ثوانٍ) | عام |
| `/logout` | بدون جسم (من ترويسة Authorization) | `{activeSessionPk, terminatedAt}` | مصادَق |
| `/signup` | `{email, fullNameAr, fullNameEn}` | `SignupRequestResponse` | عام |
| `/password-reset/request` | `{email}` | `{messageAr, messageEn}` (لا يكشف وجود البريد) | عام |
| `/password-reset/complete` | `{token, newPassword}` | `{messageAr, messageEn}` | عام |

**غير موجود:** refresh token، endpoint `/me`، CORS (لا يوجد أي إعداد CORS في Java).
القائمة الفعّالة للمستخدم: `GET /api/v1/sec/menu`.

### 1.2 التوكن والفلتر

- التوقيع HMAC بـ `app.jwt.secret`؛ الصلاحية `app.jwt.expiration-ms` (افتراضياً ساعة). لا يوجد refresh token: عند الانتهاء يسجّل العميل الدخول من جديد.
- الـ claims: `sub`=username، `jti`=`SEC_ACTIVE_SESSION.TOKEN_REF`، `uid`=userPk، `iat`، `exp`. **لا توجد صلاحيات داخل التوكن.**
- `JwtAuthenticationFilter` في كل طلب:
  1. يقرأ `Authorization: Bearer <token>` ويتحقق من التوقيع والانتهاء.
  2. المستخدم يجب أن يكون `statusCode=ACTIVE` و`isActiveFl=true`.
  3. الجلسة (`jti`) يجب أن تكون موجودة وغير منتهية ⇒ الـ logout أو إنهاء الجلسة من المدير يُبطل التوكن فوراً.
  4. يحسب الصلاحيات الفعّالة من قاعدة البيانات (بدون cache) ويضعها في الـ SecurityContext.
- **الـ principal = اسم المستخدم (String)** وليس `UserDetails` ولا يحمل userPk.
  استخدم `SecurityContextHelper.getCurrentUsername()`.
- **الصلاحيات (authorities) = أكواد `PERM_*` خام** (بدون بادئة `ROLE_`، ولا أسماء أدوار).

### 1.3 حساب الصلاحيات الفعّالة

المستخدم ← أدواره ← `SEC_ROLE_ACTION_GRANT` ← الإجراء ← الشاشة ← الموديول، بشرط أن تكون كلها نشطة.
**قاعدة البوابة (RULE-SEC-007):** أي إجراء غير `VIEW` يُحتسب فقط إذا كان المستخدم يملك `VIEW` على نفس الشاشة.
منح الموديول (Tier-1) والشاشة (Tier-2) يبنيان القائمة فقط؛ **منح الإجراء (Tier-3) وحده يولّد الصلاحيات.**

---

## 2. نموذج الصلاحيات

```
SEC_MODULE_REG (CODE ≤ 10)            ← Tier-1 grant: SEC_ROLE_MODULE_GRANT
  └─ SEC_SCREEN_REG (PAGE_CODE ≤ 50)  ← Tier-2 grant: SEC_ROLE_SCREEN_GRANT
       └─ SEC_ACTION_REG (ACTION_CODE ≤ 40, PERMISSION_CODE unique)
                                      ← Tier-3 grant: SEC_ROLE_ACTION_GRANT  → authority
SEC_USER ── SEC_USER_ROLE ── SEC_ROLE
```

- **صيغة كود الصلاحية:** `PERM_<PAGE_CODE>_<ACTION_CODE>` — مثال `PERM_FIN_ACCOUNTS_CREATE`, `PERM_FIN_PERIODS_CLOSE_APPROVE`.
- الإجراءات: `VIEW, CREATE, UPDATE, DELETE` + إجراءات مخصّصة (`REVERSE`, `CLOSE_APPROVE`...). `VIEW` هو بوابة الشاشة.
  FIN وMDL يمثّلان التعطيل (deactivate) كـ `UPDATE`.
- ترتيب المنح مفروض: موديول ← شاشة ← إجراء، والإجراء غير VIEW يحتاج VIEW (`SEC-409-NO-VIEW-GRANT`).
- لا يوجد جدول قائمة؛ القائمة تُبنى من المنح.

---

## 3. خطوات دمج موديول جديد مع SEC (إلزامية)

### 3.1 الثوابت في `PermissionConstants`

**كل** ثوابت الصلاحيات لكل الموديولات تعيش في ملف SEC واحد: `src/main/java/com/erp/sec/permission/PermissionConstants.java`.
أضف قسماً لموديولك (اسم الثابت = قيمته):

```java
// ---- XYZ — Items screen ----
public static final String PERM_XYZ_ITEMS_VIEW   = "PERM_XYZ_ITEMS_VIEW";
public static final String PERM_XYZ_ITEMS_CREATE = "PERM_XYZ_ITEMS_CREATE";
public static final String PERM_XYZ_ITEMS_UPDATE = "PERM_XYZ_ITEMS_UPDATE";
```

هذا الكلاس هو **الاستثناء الوحيد** المسموح عبر حدود الموديولات في SpEL (ArchUnit).

### 3.2 بوابات `@PreAuthorize` — على الـ Service لا الـ Controller

```java
@PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_XYZ_ITEMS_CREATE)")
public ServiceResult<XyzItemResponse> create(XyzItemCreateRequest r) { ... }

@PreAuthorize("isAuthenticated()")          // لخدمة غير مرتبطة بشاشة (مثل dispatch في NOTIF)
```

- صلاحية تعتمد على وسيط: SpEL بـ `#param` (مثال `FileService.softDelete`).
- صلاحية تعتمد على محتوى الجسم: `SecurityContextHelper.hasAuthority(...)` داخل الـ method.
- `SecurityConfig` **لا يحتاج تعديلاً**: كل مسار غير عام هو `authenticated()` تلقائياً.

### 3.3 التسجيل والمنح عبر Flyway (وليس عبر الـ API)

migration جديد بالرقم التالي (`ls src/main/resources/db/migration/ | ...` — انظر CLAUDE.md). الأمثلة المرجعية:
`V24__fin_security_seed.sql` (تسجيل) + `V25__fin_role_grants.sql` (منح)، أو `V31__cu_notif_file_security_seed.sql` (الاثنان معاً + دور مدير خاص بالموديول)،
و`V37__fin_v2_account_mappings_security_seed.sql` (إضافة شاشة لموديول قائم).

```sql
-- 1) الموديول
INSERT INTO SEC_MODULE_REG (MODULE_REG_PK, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_MODULE_REG'), 'XYZ', 'الموديول', 'Xyz Module', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- 2) الشاشات
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_SCREEN_REG'), v.page_code,
       (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'XYZ'),
       v.name_ar, v.name_en, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES ('XYZ_ITEMS', 'الأصناف', 'Items')) AS v(page_code, name_ar, name_en);

-- 3) الإجراءات (PERMISSION_CODE يُشتق بنفس صيغة PermissionConstants)
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_ACTION_REG'), 'PERM_' || v.page_code || '_' || v.action_code, s.SCREEN_REG_PK, v.action_code,
       s.NAME_AR || ' - ' || v.action_ar, s.NAME_EN || ' - ' || v.action_code, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES ('XYZ_ITEMS','VIEW','عرض'), ('XYZ_ITEMS','CREATE','إنشاء'), ('XYZ_ITEMS','UPDATE','تعديل'))
     AS v(page_code, action_code, action_ar)
JOIN SEC_SCREEN_REG s ON s.PAGE_CODE = v.page_code;

-- 4) المنح لـ SYS_ADMIN — الطبقات الثلاث بالترتيب (نمط V25)
INSERT INTO SEC_ROLE_MODULE_GRANT (ROLE_MODULE_GRANT_PK, ROLE_ID, MODULE_ID, GRANTED_BY, GRANTED_AT)
VALUES (nextval('SEQ_SEC_ROLE_MODULE_GRANT'),
        (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'XYZ'), 'SYSTEM', CURRENT_TIMESTAMP);
-- ثم SEC_ROLE_SCREEN_GRANT و SEC_ROLE_ACTION_GRANT بـ SELECT ... JOIN ... WHERE m.CODE = 'XYZ' (انسخ من V25)
```

قواعد مستخلصة من تاريخ المشروع:
- المفاتيح من `nextval('SEQ_SEC_*')`، والمراجع بالكود الطبيعي (`CODE`/`PAGE_CODE`) — **لا IDs ثابتة**.
- **التسجيل ليس منحاً:** V19 سجّل MDL بدون منح، فأرجعت كل endpoints الـ MDL حالة 403 حتى لـ SYS_ADMIN إلى أن أُضيف V21. اشحن المنح في نفس الإصدار.
- المنح بـ `SELECT ... WHERE m.CODE='X'` **لا يغطي** إجراءات تُضاف لاحقاً؛ كل إجراء جديد يحتاج منحاً صريحاً (V28, V30, V37).
- السكربتات غير idempotent؛ لا تعدّل migration مطبَّقاً أبداً.
- يمكن إنشاء دور مدير خاص (`XYZ_ADMIN`) كما في V31.

---

## 4. واجهات SEC للموديولات الأخرى (`com.erp.sec.crossmodule`)

حقن Spring مباشر (لا HTTP loopback)، ترجع records/primitives لا entities.

| الواجهة | التوقيع | الحماية | المستهلكون |
|---|---|---|---|
| `SecModuleRegistryApi` | `boolean isModuleActive(String moduleCode)` | بدون `@PreAuthorize` (يعتمد على بوابة المستدعي) | MDL |
| `SecUserDirectoryApi` | `Optional<UserContact> findContact(Long userPk)` | `isAuthenticated()` | NOTIF (`SecRecipientDirectory`: فحص نشاط المستلم) |
| `SecUserDirectoryApi` | `List<Long> findUserIdsHoldingPermission(String permissionCode)` | `isAuthenticated()` | لا أحد بعد |

`UserContact(Long userPk, String email, String fullNameAr, String fullNameEn, boolean active)` — لا يحمل كلمة المرور ولا الأدوار.
`findContact` يرجع `Optional.empty()` لمعرّف غير موجود (لا 404).

```java
@Service @RequiredArgsConstructor
public class XyzApprovalService {
    private final SecUserDirectoryApi userDirectory;   // من com.erp.sec.crossmodule فقط
    ...
    String email = userDirectory.findContact(approverId).map(UserContact::email).orElse(null);
}
```

> لا تستورد أي شيء من `sec.service` / `sec.entity` / `sec.repository` — يكسر ArchUnit.

---

## 5. المستدعي الداخلي بلا مستخدم (`InternalCallerContext`)

`com.erp.sec.security.InternalCallerContext.call(Supplier<T>)` يركّب مصادقة اصطناعية (`"internal"` + `INTERNAL_TRUSTED_CALLER`) طوال الاستدعاء ثم يستعيد السياق.
- يمرّر بوابات `isAuthenticated()` فقط — **لا** يمرّر `hasAuthority(PERM_...)`.
- الفلتر يحذف هذه الصلاحية من أي طلب خارجي.
- `createdBy` في الصفوف المكتوبة داخله = `internal`.
- الاستخدام الوحيد حالياً: `PasswordResetService` (مسار غير مصادَق يرسل إشعاراً).

> ⚠️ **قيد معماري:** الكلاس في `sec.security` وليس `sec.crossmodule`، لذا **استيراده من موديول آخر يكسر `CrossModuleBoundaryArchTest`**.
> موديول جديد يحتاج استدعاءً بلا مستخدم (scheduler/job) يحتاج قراراً: نقله/تعريضه عبر `sec.crossmodule`، أو إضافة استثناء صريح. سجّلها كـ `api_doc_gap` ولا تلتفّ عليها.

---

## 6. أشكال أخطاء 401/403

| الحالة | HTTP | `error.code` |
|---|---|---|
| توكن مفقود/غير صالح/منتهٍ، جلسة منتهية، مستخدم غير نشط (داخل الفلتر) | 401 | `SEC-401-INVALID-CREDENTIALS` |
| رفض داخل سلسلة الفلاتر | 403 | `SEC-403-FORBIDDEN` |
| رفض `@PreAuthorize` في خدمات SEC | 403 | `SEC-403-FORBIDDEN` (عبر `SecForbiddenAdvisor`) |
| رفض `@PreAuthorize` في **أي موديول آخر** | 403 | `ACCESS_DENIED` (من `GlobalExceptionHandler`) |

كلها بالغلاف الموحّد `ApiResponse` والرسالة معرّبة حسب لغة الطلب.

---

## 7. باقي واجهات SEC (للإدارة والواجهة الأمامية)

| المسار | الغرض |
|---|---|
| `/api/v1/sec/users` | CRUD المستخدمين، `PUT /{id}/roles`، تعطيل `DELETE`، إعادة تفعيل `PATCH` |
| `/api/v1/sec/roles` | CRUD الأدوار + `GET /{id}/grants`، `POST /{id}/modules|screens|actions`، `DELETE /{id}/modules/{moduleId}` |
| `/api/v1/sec/registry` | `POST /search`، `POST /modules|screens|actions` — تسجيل وقت التشغيل (الموديولات لا تستخدمه عند النشر) |
| `/api/v1/sec/sessions` | بحث وإنهاء الجلسات |
| `/api/v1/sec/signup-requests` | مراجعة طلبات التسجيل (`PATCH` قبول/رفض) |
| `/api/v1/sec/audit-log` | بحث + `GET /export` (CSV) |
| `/api/v1/sec/menu` | قائمة المستخدم الفعّالة: موديولات ← شاشات |
| `/api/v1/sec/dashboard` | لوحة الأمان |
| `/api/v1/sec/dev/password-reset-token` | ملف `dev` فقط — أداة اختبار |

---

## 8. سجلّ التدقيق الأمني

`SEC_AUDIT_LOG` إلحاقي فقط، وأنواع الأحداث مقيّدة بـ CHECK في DB:
`LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, PASSWORD_RESET_REQUESTED, PASSWORD_RESET_COMPLETED, ROLE_ASSIGNED, ROLE_REVOKED, MODULE_/SCREEN_/ACTION_GRANTED|REVOKED, SESSION_TERMINATED`.

**موديول آخر لا يستطيع الكتابة فيه** (لا API، والـ CHECK يرفض أنواعاً جديدة). تدقيق صفوف موديولك يأتي من `AuditableEntity` (انظر [common](platform-integration-common.md)).

---

## 9. الإعدادات ومتغيرات البيئة

| المفتاح | البيئة | ملاحظة |
|---|---|---|
| `app.jwt.secret` | `JWT_SECRET` | إلزامي في prod |
| `app.jwt.expiration-ms` | — | 3600000 |
| `app.frontend-url` | `FRONTEND_URL` | لروابط البريد (reset) |
| `app.password-reset-path` | `PASSWORD_RESET_PATH` | `/password-reset/complete` |

حساب التطوير: `admin` / `admin` (seed V17). لا أحداث Spring (`ApplicationEvent`) تُنشر من SEC.

---

## 10. فجوات مفتوحة (من `execution-state.json` وقيود معروفة)

- RULE-SEC-005 (تعارض الفصل بين المهام SoD) خامل بالتصميم — بانتظار قرار الـ factory (`HUMAN`).
- لغة بريد إعادة التعيين (`lang`) غير محددة — `PENDING`.
- endpoint التطوير `/dev/password-reset-token` بانتظار قرار (`HUMAN`).
- رفض `@PreAuthorize` يرجع `SEC-403-FORBIDDEN` داخل SEC و`ACCESS_DENIED` خارجه. على الواجهة الأمامية معاملة الكودين كـ 403.
