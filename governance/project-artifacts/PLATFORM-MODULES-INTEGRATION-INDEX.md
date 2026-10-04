# مرجع موديولات المنصة: common · SEC · FILE · NOTIF

> **الغرض:** مرجع واحد لكل من يضيف موديولاً جديداً لهذا الـ backend، أو يعيد استخدام هذه الأساسات في مشروع آخر.
> **المصدر:** الكود الفعلي في `src/main/java/com/erp/` مع حالة الحوكمة في `governance/shared/` (commit `201491c`)، بتاريخ 2026-10-01.
> إذا تعارض أي شيء هنا مع الكود، فالكود هو المرجع. حدّث هذا الملف عندما يتغير سطح التكامل.

---

## 1. حالة التنفيذ

| الموديول | الحزمة | مراحل الـ backend (CORE → ALIGN-BE) | الاختبارات | وثائق الـ API | المرجع التفصيلي |
|---|---|---|---|---|---|
| **common** | `com.erp.common` | الأساس المشترك (ليس موديول حوكمة) | تغطيه اختبارات كل الموديولات | — | [platform-integration-common.md](platform-integration-common.md) |
| **SEC** | `com.erp.sec` | ✅ 8/8 COMPLETE | `TEST-PLAN-BE` PARTIAL | `governance/shared/backend/modules/SEC/api-docs/` | [platform-integration-sec.md](platform-integration-sec.md) |
| **FILE** | `com.erp.file` | ✅ 8/8 COMPLETE (Legacy Path) | PENDING | `governance/shared/backend/modules/FILE/api-docs/` | [platform-integration-file.md](platform-integration-file.md) |
| **NOTIF** | `com.erp.notif` | ✅ 8/8 COMPLETE (Legacy Path) | PENDING | `governance/shared/backend/modules/NOTIF/api-docs/` | [platform-integration-notif.md](platform-integration-notif.md) |

موديولات منصة أخرى قد يحتاجها موديولك: **MDL** للقوائم (lookups): `com.erp.mdl.crossmodule.MdlLookupApi.readActiveValuesByKey(String typeKey)`. يستخدمه FILE وNOTIF وFIN.

> `governance/shared` هو git submodule. بعد الـ clone شغّل `./scripts/governance pull` (أو `git submodule update --init`) وإلا ظهر المجلد فارغاً.

---

## 2. كيف تتكامل الموديولات (صورة واحدة)

```
                 ┌──────────────────────── com.erp.common ─────────────────────────┐
                 │ AuditableEntity · ServiceResult/Status · LocalizedException      │
                 │ ApiResponse/OperationCode/GlobalExceptionHandler · SpecBuilder   │
                 └──────────────────────────────────────────────────────────────────┘
                        ▲ يرثه/يستورده كل موديول (مسموح دائماً)

  ┌─────────────┐  PermissionConstants (في @PreAuthorize)  ┌──────────────┐
  │ موديول جديد │ ─────────────────────────────────────────▶ │     SEC      │
  │  com.erp.xyz│  sec.crossmodule.SecUserDirectoryApi       │ JWT · RBAC   │
  │             │ ─────────────────────────────────────────▶ │ Registry     │
  │             │  Flyway seed: MODULE/SCREEN/ACTION + grants└──────────────┘
  │             │
  │             │  notif.crossmodule.NotificationDispatchApi ┌──────────────┐
  │             │ ─────────────────────────────────────────▶ │    NOTIF     │
  │             │  Flyway seed: NOTIF_TEMPLATE               └──────────────┘
  │             │
  │             │  REST (الواجهة الأمامية): ownerId/ownerType/moduleCode
  │             │  file.crossmodule.FileDocumentLookupApi     ┌──────────────┐
  │             │ ─────────────────────────────────────────▶ │     FILE     │
  └─────────────┘  (تحقق من fileId مخزّن)                     └──────────────┘
```

**القاعدة الذهبية (ArchUnit):** لا تستورد من موديول آخر إلا من حزمة `crossmodule` الخاصة به، والاستثناء الوحيد هو `com.erp.sec.permission.PermissionConstants`.

---

## 3. قائمة التحقق لإضافة موديول جديد `XYZ`

### البنية والأساس (common)
- [ ] الحزمة `com.erp.xyz` بالطبقات: `entity, domain, repository, dto, mapper, service, controller, exception` و`crossmodule` (إن كان سيُستهلك من غيره).
- [ ] أضف `new Module("com.erp.xyz", "com.erp.xyz.crossmodule")` إلى `MODULES` في `src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java`.
- [ ] كل Entity يرث `AuditableEntity`. الأعمدة بأحرف كبيرة وبادئة الموديول (`XYZ_*`)، وأعمدة Boolean من نوع `BOOLEAN` أصلي.
- [ ] كل Service يرجع `ServiceResult<T>`، وكل Controller يستخدم `OperationCode.craftResponse`، والبحث `POST /search` مع DTO يرث `BaseSearchContractRequest`.
- [ ] `XyzErrorCodes` بصيغة `XYZ-<http>-<SLUG>`، ومفاتيحه مضافة في **قسم جديد** داخل `i18n/messages.properties` **و**`messages_ar.properties`.
- [ ] لا `@RestControllerAdvice` خاص بالموديول، ولا `RuntimeException` خام.

### الأمان (SEC)
- [ ] ثوابت `PERM_XYZ_<SCREEN>_<ACTION>` في `com.erp.sec.permission.PermissionConstants`.
- [ ] `@PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_...)")` على كل method عامة في الـ Service.
- [ ] migration: `SEC_MODULE_REG` (كود ≤10)، `SEC_SCREEN_REG`، `SEC_ACTION_REG` (VIEW لكل شاشة).
- [ ] **في نفس الإصدار:** منح الطبقات الثلاث لـ `SYS_ADMIN` (وإلا ستحصل على 403 للجميع). ويمكن إضافة دور `XYZ_ADMIN`.

### الإشعارات (NOTIF) — إن احتجتها
- [ ] migration يزرع قوالب `XYZ_*` في `NOTIF_TEMPLATE` (AR وEN، بمتغيرات `{var}`).
- [ ] حقن `NotificationDispatchApi` واستدعاء `dispatchIndependently` داخل `try/catch`.
- [ ] تمرير `variables.email` بعد جلبه عبر `SecUserDirectoryApi.findContact(userPk)`. NOTIF لا يملؤه تلقائياً، ويتخطى المستخدم غير النشط بنفسه.

### الملفات (FILE) — إن احتجتها
- [ ] حدد `moduleCode = "XYZ"` وقيم `ownerType` ثابتة لكل كيان، ووثّقها.
- [ ] امنح أدوار موديولك `PERM_FILE_BROWSER_VIEW/CREATE` (مع Tier-1 على `FILE` وTier-2 على `FILE_BROWSER`).
- [ ] (اختياري) تصنيف `FILE_CATEGORY` خاص بالموديول لحدود الحجم والنوع.
- [ ] إن خزّنت `fileId` في جدولك، تحقق منه بـ `FileDocumentLookupApi.isAvailable`.

### التحقق
- [ ] `mvn test` (يشمل ArchUnit)، ثم المهارة `gov-validate-backend-feature`.
- [ ] `generate-api-docs` بعد تشغيل التطبيق، وأي فجوة تُسجّل في `api_doc_gaps` الخاصة بموديولك.

---

## 4. إعادة الاستخدام في مشروع آخر

هذا الـ backend مصمم كقالب: الاسم `erp` متغير واحد.

1. **الاستنساخ وإعادة التسمية**
   ```bash
   ./scripts/rename-project.sh acme     # com.erp → com.acme، والـ artifact، والـ DB، وDocker، وOpenAPI
   mvn -q -DskipTests compile           # JDK 25 إلزامي
   ```
2. **ما تحتفظ به كحد أدنى للمنصة:**
   - الحزم: `common`، `main` (`ErpMainApplication`، `JpaConfig`، `SecurityConfig`، `OpenApiConfig`)، `sec`، و`mdl` (يعتمد عليه FILE وNOTIF).
   - اختياري: `file` و`notif`. لا تعتمد عليهما بقية المنصة، ما عدا أن SEC يرسل بريد إعادة كلمة المرور عبر NOTIF.
   - `src/main/resources/i18n/` و`application*.properties`.
3. **الـ migrations:** سجل Flyway الحالي يحتوي أزواج إنشاء ثم حذف لموديولات قديمة (مثل `V14__drop_legacy_security_schema.sql`). لمشروع جديد كلياً يمكن البدء بسجل نظيف مبني على المخطط الحالي (`V16__sec_schema.sql` وما بعده، وmigrations الـ FILE/NOTIF/MDL)، بشرط ألا يكون أي منها قد طُبّق على بيئة قائمة. قرر ذلك صراحةً ولا تعدّل ملفات مطبّقة.
4. **متغيرات البيئة** (الموجود في `.env.example`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `FRONTEND_URL`, `PASSWORD_RESET_PATH`؛ والباقي يُقرأ من `application*.properties` بقيم افتراضية):

   | المتغير | الموديول |
   |---|---|
   | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | الكل |
   | `JWT_SECRET` | SEC |
   | `FRONTEND_URL`, `PASSWORD_RESET_PATH` | SEC (روابط البريد) |
   | `FILE_ACCESS_TOKEN_SECRET` (إلزامي في prod، غير موجود في `.env.example`) | FILE |
   | Redis host/port | FILE (توكنات التنزيل) + الـ cache |
   | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | NOTIF |

5. **البنية التحتية:** PostgreSQL وRedis عبر `docker compose --env-file .env -f docker/docker-compose.yml up -d`، والمنفذ `7272`. حساب التطوير `admin` / `admin`.
6. **لا يوجد CORS** مضبوط في `SecurityConfig`. أضفه إذا كانت الواجهة الأمامية على أصل (origin) مختلف دون proxy.

---

## 5. قيود عابرة للموديولات تحتاج قراراً قبل الاعتماد عليها

| القيد | الأثر على موديول جديد |
|---|---|
| `InternalCallerContext` في `sec.security` وليس `crossmodule` | لا يمكن لموديول آخر تنفيذ استدعاء بلا مستخدم (job أو scheduler) إلى NOTIF/SEC دون كسر ArchUnit. **نقله إلى `sec.crossmodule` بانتظار موافقة.** |
| اكتشاف نوع الملف بـ `URLConnection.guessContentTypeFromStream` | ملفات PDF/Office قد تُرفض إن وُضعت في allow-list لتصنيف — اختبرها قبل الاعتماد |
| رفض `@PreAuthorize` خارج SEC يرجع `ACCESS_DENIED` بينما داخل SEC يرجع `SEC-403-FORBIDDEN` | على الواجهة الأمامية التعامل مع الكودين |
| لا تدقيق أمني (`SEC_AUDIT_LOG`) متاح للموديولات الأخرى | تدقيق موديولك يقتصر على أعمدة `AuditableEntity` |

وفق CLAUDE.md: عند اكتشاف فجوة أثناء التنفيذ، سجّلها في `api_doc_gaps` ضمن `execution-state.json` الخاص بموديولك، ولا تلتفّ عليها.

---

## 6. ملفات ذات صلة

- `governance/shared/platform/rules/GOVERNANCE-RULES.md`: جدول توجيه المهارات.
- `.claude/skills/build-*` و`gov-*`: مهارات توليد الكود والتحقق منه، وتفترض أساس `common` كما هو.
- `governance/project-artifacts/sec-implementation-notes.md`: خلفية تصميم SEC وقراراته.
- [integration-notifications-fileservice.md](integration-notifications-fileservice.md): ملخص بالإنجليزية لعقد NOTIF وFILE، بأرقام أقسام ثابتة تستشهد بها ملفات الـ factory.
