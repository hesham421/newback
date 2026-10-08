<!-- SRS — Governed by SRS Governance Engine (Project 1) | PART A + PART B -->

# وثيقة التحليل (SRS)
## المرافق المشتركة | Common Utils (CU)

---

# PART A — MODULE FOUNDATION

## A1 — معلومات الوثيقة (Document Information)

| البند | القيمة |
|---|---|
| **اسم المشروع** | منصة Foundation — أصول برمجية أساس قابلة لإعادة الاستخدام (Domain: ERP) |
| **الموديول** | المرافق المشتركة (Common Utils) |
| **Feature Code** | CU-001 |
| **Feature Type** | Configuration (مكوّن أساس متقاطع — Cross-Cutting Library) |
| **الطبقة / النوع** | L1 · Cross-Cutting Foundation · ROOT |
| **إعداد بواسطة** | SRS Governance Engine (P1) — Architect: Hesham |
| **النسخة** | 1.0 |
| **التاريخ** | 2026-09-02 |
| **الحالة** | Draft |
| **Open Questions** | None — see OQ Log |
| **Governed by** | SRS Governance Engine (Project 1) |
| **Deployment Surface** | Backend-only — لا واجهة أمامية (قرار Architect 2026-09-02) |

## A2 — السياق الوظيفي (Functional Context)

### ما يشمله هذا الموديول
> Common Utils مكوّن أساس متقاطع (Cross-Cutting) — **مكتبة مشتركة** لا موديول أعمال — تعتمد عليه كل الموديولات ولا يعتمد على أيٍّ منها (ROOT). يجمّع خمس قدرات (المصدر: module-registry-CU §RESPONSIBILITIES):
> - **Specification / Filtering** — استعلام وفلترة ديناميكية (predicate builder). كود صرف — بلا كيان.
> - **Global Exceptions** — تسلسل استثناءات + معالج مركزي + شكل خطأ موحّد. كود صرف. (الـ Error Catalog/ERR-IDs ملك P3.)
> - **Bundle (i18n)** — رسائل AR/EN عبر resource bundles ملفّية. بلا كيان.
> - **Configuration** — مخزن إعدادات key/value وقت التشغيل (الكيان الوحيد: `AppConfiguration`).
> - **Events** — أحداث داخلية متزامنة (Spring ApplicationEvent). بلا كيان، بلا وسيط.

### ما لا يشمله هذا الموديول
> لا Workflow Engine (RULE-13=OFF) · لا وسيط رسائل خارجي · لا Error Catalog (ملك P3) · لا واجهة أمامية · لا MD_MASTER_LOOKUP مركزي في نطاق Foundation.

### وظيفة الموديول
> يمنح بقية الموديولات مرافق موحّدة (فلترة، استثناءات، i18n، إعدادات، أحداث) بأقل glue code. الـ Operator يضبط إعدادات المنصة key/value وقت التشغيل دون إعادة نشر.

### الوصف الوظيفي التفصيلي
> القدرات الأربع آليات كود بلا بيانات، تُستهلَك بالحقن. القدرة الوحيدة ذات بيانات مُخزَّنة هي Configuration: مخزن `AppConfiguration` key/value يقرأه أي موديول عبر `ConfigurationService`، ويُدار عبر REST APIs يستهلكها تطبيق إدارة لاحق. مبدأ حاكم: تعقيد متوسط (POLICY-CLI-01).

### ملاحظات عامة
- **قرار P1:** `AppConfiguration` key/value نقي — `configKey` هو الهوية — لذا **بلا `nameAr`/`nameEn`** (لا يناسبان key/value)، ويُكتفى بـ `notes`. المصدر: طبيعة الكيان + POLICY-CLI-01. *(انحراف موثّق — HR-1.)*
- **قرار P1:** لا `valueType` LOV (module-registry يتركها لـ P1؛ التعقيد المتوسط يرجّح تخزين نصي). النتيجة: CU يملك **صفر LOVs**. Override: يُضاف عند حاجة فعلية.
- **قرار P1:** i18n عبر resource bundles ملفّية (لا كيان MessageCatalog). Override: يُضاف عند طلب ترجمات محرّرة وقت التشغيل.

## A3 — الكيانات والحقول (Entities & Fields)

### ENTITY-CU-001 — AppConfiguration (إعدادات المنصة)

| البند | القيمة |
|---|---|
| **النوع** | PRIVATE — Configuration |
| **Business Code** | NO — BC-RULE-0 (كيان إعدادات داخلي) |
| **المصدر** | module-registry-CU §ENTITIES OWNED + §RESPONSIBILITIES; prd-CU US-CU-001 |
| **العمليات** | Create, Read, Update, Deactivate (soft) |
| **Cross-Module** | None (ROOT) |

#### حقول الكيان — (DB_TARGET = POSTGRESQL_16)

| اسم الحقل | نوع البيانات | إلزامي | القيم/المصدر | ملاحظات | Label-AR | Label-EN |
|---|---|---|---|---|---|---|
| appConfigurationPk | BIGINT (PK) | نظام | Sequence | رقم تلقائي | المعرف | ID |
| configKey | VARCHAR(150) | نعم | UNIQUE | Read-Only بعد الإنشاء (RULE-CU-003) | مفتاح الإعداد | Config Key |
| configValue | TEXT | نعم | — | قيمة الإعداد (نص) | قيمة الإعداد | Config Value |
| notes | VARCHAR(2000) | لا | — | وصف اختياري | ملاحظات | Notes |
| isActiveFl | SMALLINT | نعم | 1/0 | ⚠ Fl إلزامي | نشط | Active |
| createdBy | VARCHAR(255) | نظام | — | AuditEntityListener | أنشئ بواسطة | Created By |
| createdAt | TIMESTAMP | نظام | — | AuditEntityListener | تاريخ الإنشاء | Created At |
| updatedBy | VARCHAR(255) | نظام | — | AuditEntityListener | عُدِّل بواسطة | Updated By |
| updatedAt | TIMESTAMP | نظام | — | AuditEntityListener | تاريخ التعديل | Updated At |

## A4 — قواعد التحقق (Business Rules)

### RULE-CU-001 — تفرّد مفتاح الإعداد
| Scope | Trigger | Statement | Message-AR | Message-EN | Source |
|---|---|---|---|---|---|
| ENTITY-CU-001 | عند الإنشاء | The system MUST prevent creating an AppConfiguration when `configKey` already exists. | مفتاح الإعداد موجود مسبقاً — اختر مفتاحاً فريداً. | Configuration key already exists — choose a unique key. | module-registry-CU §Configuration |

### RULE-CU-002 — الحقول الإلزامية
| Scope | Trigger | Statement | Message-AR | Message-EN | Source |
|---|---|---|---|---|---|
| ENTITY-CU-001 | حفظ/تعديل | The system MUST require `configKey` and `configValue` before saving. | مفتاح الإعداد وقيمته إلزاميان. | Config key and value are required. | طبيعة الكيان (key/value) |

### RULE-CU-003 — ثبات المفتاح بعد الإنشاء
| Scope | Trigger | Statement | Message-AR | Message-EN | Source |
|---|---|---|---|---|---|
| ENTITY-CU-001 | تعديل | The system MUST prevent modifying `configKey` after creation. | لا يمكن تعديل مفتاح الإعداد بعد إنشائه. | Config key cannot be changed after creation. | قرار تصميم P1 (POLICY-CLI-02) ⚠ lower-certainty |

## A5 — قوائم القيم (LOV / Lookup)
> **None.** CU لا يملك LOVs (module-registry §LOVs OWNED = none). لا MD_MASTER_LOOKUP مركزي في domain الأساس.

## A6 — دورة الحالة (Status Lifecycle)
> **لا ينطبق.** `isActiveFl` فقط (حالتان) — دون حدّ SCR-5. لا statusId، لا Workflow (RULE-13).

## A7 — تبعيات الموديولات (Module Dependencies)
> CU **ROOT** — لا يعتمد ولا يستهلك. **لا XM candidates.**
> مُعتمِدون عليه (معلوماتي): SEC/FILE/NOTIF جميعها USES مكتبة CU — علاقة مكتبة لا XM (master-registry §8).

---

# PART B — SCREEN SPECIFICATIONS
> **لا شاشات — موديول Backend-only.** CU مكتبة تُستهلَك برمجياً؛ لا واجهة خاصة به (قرار Architect 2026-09-02). إدارة `AppConfiguration` عبر REST APIs يستهلكها تطبيق إدارة لاحق خارج نطاق CU. **لا SCR-IDs، لا SEC_PAGES، CORE-9 لا ينطبق.**

---

# MODULE-LEVEL FUNCTIONAL APIs (Backend-only)
> STACK-1: `/api/v1/[module]/[resource]` — Spring Boot/Java. أنواع POSTGRESQL_16.

| API-ID | العملية | HTTP | المسار | المدخلات | المخرجات | RULE-IDs |
|---|---|---|---|---|---|---|
| API-CU-001 | إنشاء | POST | /api/v1/common/configurations | configKey, configValue, notes? | AppConfiguration | RULE-CU-001, 002 |
| API-CU-002 | بحث/قائمة | GET | /api/v1/common/configurations | configKey?, isActiveFl?, page, size | قائمة | — |
| API-CU-003 | تعديل | PUT | /api/v1/common/configurations/{key} | configValue, notes?, isActiveFl? | محدَّث | RULE-CU-002, 003 |
| API-CU-004 | إلغاء (soft) | DELETE | /api/v1/common/configurations/{key} | configKey | تأكيد | — |
| API-CU-005 | جلب بالمفتاح | GET | /api/v1/common/configurations/{key} | configKey | AppConfiguration | — |

> **Internal (in-process):** `ConfigurationService.getValue(configKey)` — قراءة داخلية للموديولات الأخرى. جزء من مكتبة CU.

---

# STANDALONE

## Permissions Summary & Registry Update
> لا صفحات ولا صلاحيات CORE-9 (Backend-only بلا شاشات). التفويض على REST APIs يُطبَّق على مستوى الـ API.

### Registry Update — MODE 1
```
Source Mode  : MODE 1 | Feature Code: CU-001
New Entities : ENTITY-CU-001 (AppConfiguration — PRIVATE, Configuration)
New Lookups  : — | New APIs: API-CU-001→005 (+ internal ConfigurationService.getValue)
XM-IDs Open  : — (ROOT) | OQ-IDs Open: None
Gate Status  : PASSED ✓ | Next: MODE 1.5 (Project 2)
```
> لمشرف السجل: master-registry §10 → CU·P1=✓، وأسند ENTITY-CU-001 في §5.

## OQ Log
```
OPEN QUESTIONS LOG — Common Utils (CU) — 2026-09-02
— None — لا أسئلة مفتوحة
```

---
*End of srs-CU.md | Feature Code: CU-001 | v1.0 | Backend-only | Next: MODE 1.5*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 01, 03, 05, 06, 08, 09, 10, 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository at that tag; `cu/`, `common/`, `tenant/`, `sec/` and
`autoconfigure/` abbreviate `erp-core/src/main/java/com/erp/<package>/`, and `V<N>` a script under
`erp-core/src/main/resources/db/migration/core/`. Endpoint paths are taken from `docs/api-docs/cu/`. Every row
is labelled NEW / CHANGED / REMOVED / NOT IMPLEMENTED. New rule ids continue the A4 sequence (RULE-CU-004
onwards); no API or ENTITY id is minted.

### 1. Endpoints (نقاط النهاية)
| Kind | Method | Path | Access | Notes | Source |
|---|---|---|---|---|---|
| CHANGED | POST | `/api/v1/common/configurations` (API-CU-001) | `CONFIG_CREATE`; with `?scope=PLATFORM`: `PLATFORM_SETTINGS_MANAGE` in the PLATFORM tenant | answers **201 Created** (`Status.CREATED`); new query parameter `scope` = `TENANT` (default) or `PLATFORM`; the stored and returned `configKey` is **upper-cased** (RULE-CU-009); body `configKey` (≤ 150), `configValue`, `notes?` (≤ 2000) — no `isActive`, `tenantId` or `scope` in the body (a row always starts active; an unknown body field is ignored); response carries `scope` | cu/service/ConfigurationService.java:72-96; cu/controller/ConfigurationController.java:53-59; cu/dto/ConfigurationCreateRequest.java:22-33; docs/api-docs/cu/endpoints/configuration-management.md:52-54; docs/test-api/core-test-plan.md:281 |
| CHANGED | POST | `/api/v1/common/configurations/search` (API-CU-002) | `CONFIG_VIEW`; with `?scope=PLATFORM`: `PLATFORM_SETTINGS_MANAGE` | **POST `/search` with the shared filter envelope, not `GET /api/v1/common/configurations` with query parameters** (precedent ADR-SEC-003): body `filters[] {field, operator, value}` (operators EQUALS, NOT_EQUALS, LIKE, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IN), `sortField`, `sortDirection`, `page` (default 0), `size` (default 20, maximum 200, larger values clamped); filter fields `configKey`, `isActive` (not `isActiveFl`), `createdAt`, `updatedAt`; sort fields `configKey`, `createdAt`, `updatedAt`; an unknown filter field → 400 `VALIDATION_ERROR` with a `fieldErrors[]` entry naming the field and the code `UNSUPPORTED_FILTER_FIELD`; an unknown sort field is **silently ignored** (unsorted page); `tenantId` is never a client filter — `scope` selects the owner (own overrides, or the platform defaults); the page holds only rows of that owner | ConfigurationService.java:59-68,103-118; common/search/SpecBuilder.java:54-63; common/search/PageableBuilder.java:29-41; cu/repository/AppConfigurationRepository.java:51-55; docs/api-docs/cu/index.md:60-68; configuration-management.md:129-138 |
| CHANGED | GET | `/api/v1/common/configurations/{key}` (API-CU-005) | `CONFIG_VIEW`; with `?scope=PLATFORM`: `PLATFORM_SETTINGS_MANAGE` | `{key}` is matched **case-insensitively** (trimmed and upper-cased before the lookup); no row of that key for the addressed owner → 404 `APP_CONFIGURATION_NOT_FOUND` — a platform default is not visible through `scope=TENANT` and vice versa | ConfigurationService.java:156-159,218-225,233-235; ConfigurationController.java:78-84; core-test-plan.md:278 |
| CHANGED | PUT | `/api/v1/common/configurations/{key}` (API-CU-003) | `CONFIG_UPDATE`; with `?scope=PLATFORM`: `PLATFORM_SETTINGS_MANAGE` | body `configValue` (required; blank → 400 `VALIDATION_ERROR`), `notes?`, `isActive?`; **partial semantics**: `notes` omitted or null = unchanged (send `""` to clear), `isActive` omitted or null = unchanged, `isActive: true` **reactivates** a deactivated row (the only reactivation path — there is no `/activate` endpoint), `isActive: false` deactivates; a `configKey` in the body is silently ignored (not a DTO field, RULE-CU-003); 200 with the row after `saveAndFlush`, so the response already carries the new `updatedAt` / `updatedBy`; unknown key → 404 `APP_CONFIGURATION_NOT_FOUND` | ConfigurationService.java:126-149; cu/mapper/ConfigurationMapper.java:42-59; cu/dto/ConfigurationUpdateRequest.java:29-38; core-test-plan.md:282,284 |
| CHANGED | DELETE | `/api/v1/common/configurations/{key}` (API-CU-004) | `CONFIG_DEACTIVATE`; with `?scope=PLATFORM`: `PLATFORM_SETTINGS_MANAGE` | soft deactivate; answers **204 No Content with no body** (the analysis said "تأكيد"); **repeatable** — a second DELETE on an already inactive row is 204 again; unknown key → 404 `APP_CONFIGURATION_NOT_FOUND`; no hard delete exists | ConfigurationController.java:91-98; ConfigurationService.java:175-183; core-test-plan.md:284 |
| NEW | (all five) | query parameter `scope` | — | `TENANT` when absent, or `PLATFORM`; any other value → 400 `VALIDATION_ERROR` with `fieldErrors[0].field = scope`; `?scope=PLATFORM` from a caller without `PLATFORM_SETTINGS_MANAGE` → 403 `ACCESS_DENIED` (RULE-CU-005); `scope=PLATFORM` does not additionally need `CONFIG_*` | ConfigurationController.java:56; common/web/GlobalExceptionHandler.java:107; erp-core/src/test/java/com/erp/cu/ConfigurationScopeApiIntegrationTest.java:107-110; core-test-plan.md:283 |
| CHANGED | (all) | response shape `ConfigurationResponse` | — | fields `id` (the analysis named `appConfigurationPk`), `scope` (PLATFORM / TENANT, derived from `TENANT_ID`), `configKey`, `configValue`, `notes`, `isActive` (the analysis named `isActiveFl`), `createdAt`, `createdBy`, `updatedAt`, `updatedBy`; **`version` is not exposed** (RULE-CU-011); timestamps are ISO-8601 UTC strings; the wrapper is the common `ApiResponse` envelope | cu/dto/ConfigurationResponse.java:20-49; ConfigurationMapper.java:61-77 |
| NEW | POST | `/api/v1/common/configurations` — two concurrent creates of one key for one owner | — | the domain pre-check passes for both; the second insert hits the unique index and answers 409 `DATA_INTEGRITY_VIOLATION` (not `APP_CONFIGURATION_KEY_DUPLICATE`) | ConfigurationService.java:83-92; GlobalExceptionHandler.java:142-147; V14:68 |
| REMOVED | (in-process) | `ConfigurationService.getValue(configKey)` | — | no caller and tenant-unaware; replaced by `SettingsApi` | docs/DEVIATIONS.md:147 ([09]) |
| NEW | (in-process) | `com.erp.cu.crossmodule.SettingsApi`: `get(key)`, `get(key, Class)`, `find(key)`, `find(key, Class)`, `getOrDefault(key, default)`, `getOrDefault(key, Class, default)` | no `@PreAuthorize` (library call from already authorised work); needs a bound `TenantContext` (RULE-CU-015) | types String, Integer, Long, Boolean, BigDecimal, Duration; constant `SettingsApi.CACHE_NAME = "erpCoreSettings"`; `get` of an unset key throws `NoSuchSettingException` (a `LocalizedException`, public `getKey()` = the normalised key) | cu/crossmodule/SettingsApi.java:25-43; cu/crossmodule/SettingsApiImpl.java:27-43; cu/crossmodule/NoSuchSettingException.java:16-24 |

### 2. Business rules (قواعد العمل)
| Kind | Rule | Statement | Source |
|---|---|---|---|
| CHANGED | RULE-CU-001 | Unique **per owner**: one platform default (`TENANT_ID` NULL) and at most one override per tenant for a key (unique index on `(COALESCE(TENANT_ID, 0), CONFIG_KEY)`); the service pre-checks the normalised key within the owner → 409 `APP_CONFIGURATION_KEY_DUPLICATE`; a race past the pre-check → 409 `DATA_INTEGRITY_VIOLATION` | cu/domain/AppConfigurationDomain.java:38-42; ConfigurationService.java:83-89; V14:68 |
| CHANGED | RULE-CU-002 | Enforced twice: bean validation on the DTOs first (`@NotBlank` on `configKey` / `configValue` of create and `configValue` of update → 400 `VALIDATION_ERROR` with `fieldErrors`), then `DomainRules.assertNotBlank` in the Domain (400 `APP_CONFIGURATION_FIELDS_REQUIRED`); over REST a blank field always answers `VALIDATION_ERROR` — the domain code is reachable only from an in-process caller | ConfigurationCreateRequest.java:22-29; ConfigurationUpdateRequest.java:29-31; AppConfigurationDomain.java:39,51 |
| CHANGED | RULE-CU-003 | Enforced by DTO shape, not by a runtime guard: `configKey` is absent from `ConfigurationUpdateRequest`, so a key in a PUT body is ignored; `APP_CONFIGURATION_KEY_IMMUTABLE` is registered and never raised; the owner (`tenantId`) is immutable the same way (`updatable = false`, no DTO field) | ConfigurationUpdateRequest.java:11-38; ConfigurationService.java:133-135; cu/entity/AppConfiguration.java:50 |
| NEW | RULE-CU-004 — الحلّ (resolution) | For a tenant: its active override → the active platform default → absent (`SETTING_NOT_FOUND` from `get`, empty from `find`, the default from `getOrDefault`). A deactivated row counts as absent, so deactivating an override falls back to the default and deactivating a default hides it from every tenant without an override | AppConfigurationDomain.java:70-83; AppConfigurationRepository.java:41-44; erp-core/src/test/java/com/erp/cu/SettingsIntegrationTest.java:51-78 |
| NEW | RULE-CU-005 — نطاق المنصة (platform scope) | `scope=PLATFORM` (reads included) requires `PLATFORM_SETTINGS_MANAGE`, which only the PLATFORM tenant's super roles hold implicitly; the `@PreAuthorize` runs before the method body, so a caller without it answers **403 `ACCESS_DENIED`** (the previous revision of this addendum said `SETTING_PLATFORM_SCOPE_FORBIDDEN`). `AppConfigurationDomain.assertScopeAllowed` (403 `SETTING_PLATFORM_SCOPE_FORBIDDEN`) is a backup check on the tenant half of the rule, reached only if a role outside the PLATFORM tenant was explicitly granted the authority. `scope` absent = `TENANT` (ADR-CU-002) | ConfigurationService.java:72-75,211-216; AppConfigurationDomain.java:59-63; sec/service/MenuService.java:138-147; ConfigurationScopeApiIntegrationTest.java:82-88; core-test-plan.md:279 (TC-CORE-SETTINGS-004) |
| NEW | RULE-CU-006 — القراءة المُنمَّطة (typed reads) | `SettingValueConverter`: `String` verbatim; `Integer` / `Long` / `BigDecimal` from the trimmed text; `Boolean` only `true` / `false` (case-insensitive); `Duration` ISO-8601 or `<n>` + one of `ns|us|ms|s|m|h|d` (no unit = ms); an unsupported type fails **before** any database read; a malformed value → `SETTING_TYPE_MISMATCH` (422), also from `find` / `getOrDefault` — a default covers a missing setting, never a malformed one | cu/domain/SettingValueConverter.java:31-32,41-45,48-105; SettingsApiImpl.java:38-43 |
| NEW | RULE-CU-007 — عقد الذاكرة المؤقتة (cache contract) | One Spring cache `erpCoreSettings`, key `<tenantId>:<KEY>`, value the raw text (conversion happens after the cache); an absent key is cached as `null`; every CRUD write (create, update, deactivate) evicts **all** entries (`allEntries = true`) — a platform-default change affects every tenant's resolution (ADR-CU-001) | ConfigurationService.java:70,120,169,198-205; SettingsApi.java:25; erp-core/src/test/java/com/erp/cu/SettingsCacheTest.java:58-135 |
| NEW | RULE-CU-008 — تشغيل الذاكرة المؤقتة (cache operation) | The cache manager is the application's (`spring.cache.type` = `simple`, `redis`, …; `none` = uncached reads); **no TTL** — an entry lives until the next CRUD write; a local (`simple`) cache is **not cluster-coherent** — a write on one node does not evict the others; a value changed in the database outside the CRUD API (SQL, another application) stays stale until a CRUD write; eviction happens after commit because `@EnableCaching(order = LOWEST_PRECEDENCE - 1)` places the cache advice outside the transaction advice — this holds only when the CRUD call is not wrapped in an outer transaction (ADR-CU-001) | autoconfigure/ErpCoreCacheAutoConfiguration.java:30-46; SettingsIntegrationTest.java:118-135; docs/steps/09-report.md:320; docs/DEVIATIONS.md:149 ([09]) |
| NEW | RULE-CU-009 — توحيد المفتاح (key normalisation) | `CONFIG_KEY` is stored upper-case (`@PrePersist` / `@PreUpdate`); every lookup (uniqueness pre-check, find-by-key, `SettingsApi`) trims and upper-cases the caller's key, so keys are case-insensitive. **Known defect**: the persist hooks upper-case but do not trim, so a create with a leading or trailing blank (`" key"`) stores `" KEY"`, escapes the uniqueness pre-check (which looked for `"KEY"`) and is unreachable by any lookup (ADR-CU-003) | AppConfiguration.java:71-86; ConfigurationService.java:83,219,233-235; SettingsApiImpl.java:28,39 |
| NEW | RULE-CU-010 — دورة التفعيل (activation lifecycle) | A row starts active; DELETE deactivates (soft, repeatable, 204); the only reactivation is PUT with `isActive: true`; omitted PUT fields stay unchanged (`notes` is cleared with `""`) | ConfigurationMapper.java:31-33,42-59; ConfigurationService.java:175-183 |
| NEW | RULE-CU-011 — القفل التفاؤلي (optimistic lock) | `VERSION` is server-side only: `version` is neither accepted nor returned, so a client cannot detect a lost update; 409 `CONCURRENT_MODIFICATION` is raised only when two server-side writes of one row overlap | common/domain/GlobalAuditableEntity.java:51-53; ConfigurationResponse.java:20-49; docs/DEVIATIONS.md ([05]) |
| NEW | RULE-CU-012 — التدقيق (audit) | `@Audited(entityType = "CU_APP_CONFIGURATION")`: writes are recorded under the acting tenant (a platform-default write lands in PLATFORM); `configValue` is recorded (not on the audit denylist) — secrets must not be stored in configuration rows | AppConfiguration.java:38; docs/DEVIATIONS.md:159 ([10]) |
| NEW | RULE-CU-013 — لا حدث تغيير (no settings-changed event) | CU publishes nothing on `com.erp.events` and listens to nothing; a consumer that must react to a change re-reads through `SettingsApi` | docs/steps/09-report.md:58; no `DomainEvent` under `cu/` |
| NEW | RULE-CU-014 — لا تهيئة للمستأجر الجديد (no provisioning) | No CU `TenantProvisioningContributor`: a newly provisioned tenant gets no configuration rows and inherits the platform defaults through resolution until it writes its own overrides | tenant/TenantProvisioning.java; contributors exist only under `mdl/`, `notif/`, `sec/`, `sequence/` (`*TenantProvisioningContributor.java`) |
| NEW | RULE-CU-015 — سياق المستأجر (tenant context) | `SettingsApi` requires a bound `TenantContext` (`TenantContext.require()`): a call from a thread with no tenant (job, startup, async path without propagation) fails with 500 `TENANT_CONTEXT_MISSING` — a programming error, never a client error | SettingsApiImpl.java:41; tenant/TenantContext.java:44-52 |
| CHANGED | foundation — events | Events are no longer a synchronous CU bus: `com.erp.events` with `@TransactionalEventListener(AFTER_COMMIT)` on a tenant-propagating executor (analysis: "synchronous in-process events (Spring ApplicationEvent)"); implemented asynchronous after-commit because the step-08 plan prescribes async dispatch | docs/steps/08-report.md |
| NEW | foundation — error handling (1.2.0) | Unknown path → 404 `NOT_FOUND`; `page × size + size > Integer.MAX_VALUE` → 400 `VALIDATION_ERROR` with `fieldErrors[0].field = page` (rejected, not clamped); the catch-all handler answers the first `LocalizedException` in the cause chain (depth ≤ 16) with its own code and status | docs/DEVIATIONS.md ([15]); docs/CHANGELOG.md [1.2.0]; PageableBuilder.java:32-35 |
| CHANGED | foundation — i18n | English base bundle `messages.properties`, Arabic `messages_ar.properties` (analysis named `messages_en`); the library's `messageSource` puts application bundles first | docs/DEVIATIONS.md ([01], [03]) |

### 3. Error codes (أكواد الأخطاء)
All seven CU codes are registered in `cu/exception/CuErrorCodes.java` and in both bundles (`erp-core/src/main/resources/i18n/messages.properties:25-28,170-172`; `messages_ar.properties:22-25,167-169`).

| Kind | Code | Owner | HTTP (Status) | Raised when | Source |
|---|---|---|---|---|---|
| NEW | `APP_CONFIGURATION_KEY_DUPLICATE` | CU | 409 (`ALREADY_EXISTS`) | POST: the normalised key already exists for the same owner (RULE-CU-001); message = RULE-CU-001's | CuErrorCodes.java:16; AppConfigurationDomain.java:40; messages.properties:25 |
| NEW | `APP_CONFIGURATION_FIELDS_REQUIRED` | CU | 400 (`VALIDATION_ERROR`) | Domain fallback of RULE-CU-002; over REST bean validation answers `VALIDATION_ERROR` first, so this code reaches a client only from an in-process caller | CuErrorCodes.java:19; AppConfigurationDomain.java:39,51 |
| NOT IMPLEMENTED | `APP_CONFIGURATION_KEY_IMMUTABLE` | CU | — | registered in `CuErrorCodes` and both bundles, never raised (RULE-CU-003 is structural) | CuErrorCodes.java:27; docs/api-docs/cu/index.md:83 |
| NEW | `APP_CONFIGURATION_NOT_FOUND` | CU | 404 (`NOT_FOUND`) | GET / PUT / DELETE `/{key}`: no row of that key for the addressed owner; the key is passed as argument but the message has no `{0}` placeholder | CuErrorCodes.java:33; ConfigurationService.java:223-224; messages.properties:28 |
| NEW | `SETTING_NOT_FOUND` | CU | 404 (`NOT_FOUND`) | `SettingsApi.get` for a key with neither an active override nor an active default (`NoSuchSettingException`; `{0}` = the normalised key); never raised by an endpoint | CuErrorCodes.java:38; NoSuchSettingException.java:17 |
| NEW | `SETTING_TYPE_MISMATCH` | CU | 422 (`BUSINESS_RULE_VIOLATION`) | `SettingsApi` typed read: unsupported type, or value not readable as the type (`{0}` key, `{1}` type); never raised by an endpoint | CuErrorCodes.java:41; SettingValueConverter.java:102-105 |
| NEW | `SETTING_PLATFORM_SCOPE_FORBIDDEN` | CU | 403 (`FORBIDDEN`) | backup check only (RULE-CU-005): `scope=PLATFORM` from a non-PLATFORM tenant whose caller nevertheless holds `PLATFORM_SETTINGS_MANAGE`; otherwise pre-empted by `ACCESS_DENIED` | CuErrorCodes.java:44; AppConfigurationDomain.java:59-63; erp-core/src/test/java/com/erp/cu/domain/AppConfigurationDomainScopeTest.java:17-24 |
| NEW | `ACCESS_DENIED` | common | 403 | the actual answer to `scope=PLATFORM` without `PLATFORM_SETTINGS_MANAGE`, and to any CRUD call without the matching `CONFIG_*` | GlobalExceptionHandler.java:163-167; core-test-plan.md:279 |
| NEW | `VALIDATION_ERROR` | common | 400 | blank `configKey` / `configValue`, `configKey` > 150 or `notes` > 2000 characters (`fieldErrors[].field` = the field); malformed JSON; unknown `scope` (`field = scope`); unknown filter field (`UNSUPPORTED_FILTER_FIELD`); overflowing `page` | GlobalExceptionHandler.java:107; SpecBuilder.java:54-63; PageableBuilder.java:32-35 |
| NEW | `DATA_INTEGRITY_VIOLATION` | common | 409 | concurrent create of one key past the domain pre-check (unique index) | GlobalExceptionHandler.java:142-147 |
| NEW | `CONCURRENT_MODIFICATION` | common | 409 | optimistic-lock conflict between two server-side writes (RULE-CU-011) | GlobalExceptionHandler.java:157; docs/DEVIATIONS.md ([05]) |
| NEW | `NOT_FOUND` | common (1.2.0) | 404 | unknown path | docs/CHANGELOG.md [1.2.0] |
| NEW | `TENANT_CONTEXT_MISSING` | tenant (surfaced through `SettingsApi`) | 500 (`INTERNAL_ERROR`) | `SettingsApi` called with no bound tenant (RULE-CU-015) | TenantContext.java:47-52 |

### 4. Permissions (الصلاحيات — exact authority strings)
| Kind | Authority / item | Screen / meaning | Source |
|---|---|---|---|
| CHANGED | registry module `CU` | names **`الأدوات المشتركة` / `Common Utilities`** (A1 and the module registry say `المرافق المشتركة` / `Common Utils`); seeded by V7 and re-asserted on every start by `PermissionCatalogSynchronizer` | cu/permission/CuPermissions.java:52; V7:40 |
| CHANGED | `CONFIG_VIEW`, `CONFIG_CREATE`, `CONFIG_UPDATE`, `CONFIG_DEACTIVATE` | screen `CU_CONFIGURATIONS` (`إدارة إعدادات المنصة` / `Platform Configuration`, a backend-only holder screen), action codes **`VIEW` / `CREATE` / `UPDATE` / `DEACTIVATE`**; literal authorities (not `PERM_<SCREEN>_<ACTION>`, a recorded decision); the analysis said "no CORE-9 permissions" — the API is gated by these: VIEW → search + GET, CREATE → POST, UPDATE → PUT, DEACTIVATE → DELETE; seeded by V7, code-defined in `CuPermissions` | CuPermissions.java:22-28,47-48,62-66; V7:66,114-117 |
| NEW | `PLATFORM_SETTINGS_MANAGE` | registry module `PLATFORM` (`إدارة المنصة` / `Platform Administration`, V10), screen **`PLATFORM_SETTINGS`** (`إعدادات المنصة الافتراضية` / `Platform Default Settings`), action `MANAGE`; gates every `?scope=PLATFORM` call, reads included; held **implicitly** by the PLATFORM tenant's super roles (`IS_SUPER`: PLATFORM-module authorities are added only inside the PLATFORM tenant) and never by another tenant's super role; no seeded grant | CuPermissions.java:36-45,68; MenuService.java:138-147; V10:214; docs/DEVIATIONS.md:139 ([09]) |
| NEW | `PERM_PLATFORM_SETTINGS_VIEW` | gateway (VIEW) action of `PLATFORM_SETTINGS` (RULE-SEC-007: `MANAGE` is effective only together with it); **gates no API** — it only lets the menu show the screen | CuPermissions.java:39,67 |
| NEW | `PLATFORM_SETTINGS` screen and its two actions — **not seeded** | inserted into `SEC_SCREEN_REG` / `SEC_ACTION_REG` by the startup catalog synchroniser from `CuPermissions`; no migration seeds them | sec/service/PermissionCatalogSynchronizer.java:33-40; erp-core/src/test/java/com/erp/sec/PermissionCatalogIntegrationTest.java:43-46; docs/DEVIATIONS.md:151 ([09]) |
| NEW | role `CU_ADMIN` (`مدير الإعدادات` / `Configuration Administrator`) | seeded by V7 with the `CU` module grant and, derived from it, the screen `CU_CONFIGURATIONS` and its four actions; `SYS_ADMIN` holds the same CU grants; no role holds a `PLATFORM_SETTINGS_*` grant | V7:151,172,175,187-200 |
| CHANGED | Part B "no screens" | the consuming frontend renders `/settings/configurations` (`CU_CONFIGURATIONS`) and `/platform/settings` (`PLATFORM_SETTINGS`) from its own analysis; nothing in erp-core changed; its E2E archive is `governance/frontend/modules/CU/tests/` | governance/frontend/modules/CU/tests/REPORT-2026-10-05.md:32,54-56 |

Notes: the generated "Required permission(s)" line of `docs/api-docs/cu/endpoints/configuration-management.md` prints
the `@PreAuthorize` type references literally (`PLATFORM, PLATFORM_SETTINGS_MANAGE, PLATFORM, CONFIG_CREATE`); the
effective rule is this table. `CuPermissions.java:21-27` Javadoc maps the API ids differently from this SRS
(VIEW "API-CU-003", UPDATE "004", DEACTIVATE "005"); the SRS numbering (003 = PUT, 004 = DELETE, 005 = GET) stands.

### 5. Entity (الكيان)
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | ENTITY-CU-001 AppConfiguration — base | global entity: extends `GlobalAuditableEntity` (audit columns + `@Version`), no Hibernate `@TenantId`, so no query is tenant-filtered automatically — every repository method names its owner | AppConfiguration.java:41; AppConfigurationRepository.java:20-24 |
| NEW | `tenantId` (`TENANT_ID BIGINT` nullable, FK `CORE_TENANT`) | NULL = platform default, a tenant id = that tenant's override; `updatable = false` — the owner of a row can never change; derived `scope` (PLATFORM / TENANT) in responses | AppConfiguration.java:49-51; ConfigurationMapper.java:67; V10:63,102; V14:66 |
| NEW | `version` (`VERSION BIGINT NOT NULL DEFAULT 0`) | optimistic lock, not on the wire (RULE-CU-011) | GlobalAuditableEntity.java:51-53; V10:63 |
| CHANGED | `isActiveFl` → `isActive` | Java field `Boolean isActive` ↔ column `IS_ACTIVE_FL SMALLINT` through `BooleanNumberConverter`; `isActive` is also the DTO and filter field name | AppConfiguration.java:66-69 |
| CHANGED | `configKey` | stored upper-case by the `@PrePersist` / `@PreUpdate` hooks (RULE-CU-009); `@NotBlank`, `@Size(max = 150)` | AppConfiguration.java:53-56,71-86 |
| REMOVED | JPA `@UniqueConstraint` on `@Table` | the V14 uniqueness is an expression index `(COALESCE(TENANT_ID, 0), CONFIG_KEY)`, which `@UniqueConstraint` cannot express; uniqueness is enforced by the database and the service pre-check only | AppConfiguration.java:33-35,39; docs/steps/09-report.md:104 |
| CHANGED | audit columns | A3 and V2 declare `CREATED_BY` / `UPDATED_BY VARCHAR(255)` and `CREATED_AT` / `UPDATED_AT TIMESTAMP` (no time zone); the entity maps `length = 100` and `java.time.Instant`; no migration changes the column types — the two declarations disagree | GlobalAuditableEntity.java:34-44; V2:26-29 |
| CHANGED | A6 lifecycle | still two states; the operations gain "Reactivate (through PUT `isActive: true`)" (RULE-CU-010) | ConfigurationMapper.java:52-58 |

### 6. Dependencies (A7) — deltas (التبعيات)
"CU ROOT — no dependency" no longer holds strictly.

| Kind | Direction | Item | Source |
|---|---|---|---|
| NEW | consumed | tenant: `CORE_TENANT` FK on `TENANT_ID` (`FK_CU_APP_CONFIGURATION_TENANT`); `TenantContext.require()` and the PLATFORM-tenant check decide the owner and the scope of every call; no provisioning contributor (RULE-CU-014) | V10:102; ConfigurationService.java:211-216 |
| NEW | consumed | SEC permission SPI: `CuPermissions implements PermissionContributor` (one module, two screens, six permissions) | CuPermissions.java:17 |
| NEW | consumed | audit: `@Audited` on `AppConfiguration` | AppConfiguration.java:38 |
| NEW | exposed | `com.erp.cu.crossmodule.SettingsApi` (+ `NoSuchSettingException`, `CACHE_NAME`) — CU's only cross-module surface; **no core module consumes it** (the only reference outside `cu/` is `ErpCoreCacheAutoConfiguration`, for the cache name); applications are the consumers | SettingsApi.java; ErpCoreCacheAutoConfiguration.java:3 |
| NEW | exposed (internal) | `ConfigurationService.resolve(tenantId, key)`: public, `@Cacheable`, no `@PreAuthorize`, returns the raw `String` or `null`; backs `SettingsApi` and is the one method on the caching register; not a cross-module contract (`com.erp.cu.service`, not `crossmodule`) | ConfigurationService.java:198-205; docs/DEVIATIONS.md:146 ([09]) |
| NEW | infrastructure | `com.erp.autoconfigure.ErpCoreCacheAutoConfiguration` (`AutoConfiguration.imports`, before Boot's `CacheAutoConfiguration`): `@EnableCaching(order = LOWEST_PRECEDENCE - 1)` and a `CacheManagerCustomizer<ConcurrentMapCacheManager>` that registers `erpCoreSettings` on the simple cache manager; the application chooses the provider | ErpCoreCacheAutoConfiguration.java:30-46; erp-core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports |
| NEW | infrastructure | OpenAPI group `cu` (`CU — Common Utils`, package `com.erp.cu.controller`) — the source of `docs/api-docs/cu/` | autoconfigure/ErpCoreOpenApiAutoConfiguration.java:74 |
| NEW | events | none published, none consumed (RULE-CU-013) | docs/steps/09-report.md:58 |


## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C5 — `CuTenantExportContributor` implements TENANT's export SPI (XM-TENANT-004) for `POST /api/v1/platform/tenants/{id}/export` (plan §5 C.5)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

No CU id is minted (the export rules are TENANT's RULE-TENANT-027 / -028; the SPI is TENANT's XM-TENANT-004). No
endpoint, entity field, rule, error code, permission or migration of CU changes.

| Kind | Item | Delta | Source |
|---|---|---|---|
| NEW | `com.erp.cu.tenant.CuTenantExportContributor` (`moduleCode` `CU`) | writes `CU/CU_APP_CONFIGURATION`: the exported tenant's **overrides only** (`TENANT_ID = {id}`; the platform defaults with a NULL tenant are not the tenant's data), every column but `TENANT_ID` / `VERSION`, ordered by `ID`; plain SQL naming `TENANT_ID` (RULE-TENANT-011 — `AppConfiguration` is a global entity, so the predicate is the only scope) | `../../TENANT/P1/srs-tenant.md` 1.3.0 X5, X7 |
| NEW (dependency) | tenant | + SPI implemented: `com.erp.tenant.TenantExportContributor` (root package) | TENANT XM-TENANT-004 |
| NOTE | values | `CONFIG_VALUE` is exported as stored: CU has no secret marking (a tenant override is the tenant's own setting, readable by its administrators through `/api/v1/common/configurations`). An application that stores a secret as a CU setting should keep it out of CU (environment / vault) | TENANT RULE-TENANT-027 |

### Refactors without behaviour change — shared helpers moved to `com.erp.common`
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no CU behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | `AppConfigurationDomain` | the RULE-CU-002 / RULE-CU-001 checks call the shared `com.erp.common.domain.DomainRules.assertNotBlank` / `assertUnique` instead of inline `if` + `throw`; same codes and statuses (`APP_CONFIGURATION_FIELDS_REQUIRED` 400 `VALIDATION_ERROR`, `APP_CONFIGURATION_KEY_DUPLICATE` 409 `ALREADY_EXISTS`), same argument (the key), same order (required fields before uniqueness) | cu/domain/AppConfigurationDomain.java:39-40,51; common/domain/DomainRules.java:17-27 |
| CHANGED | `ConfigurationService.owner` | the tenant half of RULE-CU-005 reads the new `TenantContext.isPlatform()` instead of comparing `TenantConstants.PLATFORM_TENANT_ID` itself (import removed); same outcome | cu/service/ConfigurationService.java:214; tenant/TenantContext.java:56-58 |
