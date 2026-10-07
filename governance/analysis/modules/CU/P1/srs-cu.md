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

Paths cited below are relative to the erp-core repository at that tag. Endpoint paths are taken from
`docs/api-docs/cu/`. No RULE / API / ENTITY ids are minted; items are labelled NEW / CHANGED / REMOVED.

### 1. Endpoints
| Kind | Method | Path | Access | Notes | Source |
|---|---|---|---|---|---|
| CHANGED | POST | `/api/v1/common/configurations` (API-CU-001) | `CONFIG_CREATE`; with `scope=PLATFORM`: `PLATFORM_SETTINGS_MANAGE` in the PLATFORM tenant | new query parameter `scope` = `TENANT` (default) or `PLATFORM`; response carries `scope` | docs/api-docs/cu/endpoints/configuration-management.md; DEVIATIONS [09] |
| CHANGED | POST | `/api/v1/common/configurations/search` (API-CU-002) | `CONFIG_VIEW` / `PLATFORM_SETTINGS_MANAGE` | `scope` selects tenant overrides or platform defaults | same |
| CHANGED | GET / PUT / DELETE | `/api/v1/common/configurations/{key}` (API-CU-005 / 003 / 004) | `CONFIG_VIEW` / `CONFIG_UPDATE` / `CONFIG_DEACTIVATE`; `PLATFORM_SETTINGS_MANAGE` with `scope=PLATFORM` | `scope` addresses the override or the default of that key; every write evicts the settings cache | same |
| REMOVED | (in-process) | `ConfigurationService.getValue(configKey)` | — | no caller and tenant-unaware; replaced by `SettingsApi` | DEVIATIONS [09] |
| NEW | (in-process) | `com.erp.cu.crossmodule.SettingsApi`: `get`, `get(key, Class)`, `find`, `find(key, Class)`, `getOrDefault`, `getOrDefault(key, Class, default)` | no `@PreAuthorize` (library call from already authorised work) | types String, Integer, Long, Boolean, BigDecimal, Duration | docs/steps/09-report.md; DEVIATIONS [09] |

Note (informational, predates erp-core): API-CU-002 is implemented as `POST /api/v1/common/configurations/search`; the analysis above names `GET /api/v1/common/configurations`. — docs/api-docs/cu/index.md

### 2. Business rules
| Kind | Rule | Source |
|---|---|---|
| NEW | Resolution: active tenant override → active platform default (`TENANT_ID` NULL) → `SETTING_NOT_FOUND`. A deactivated row counts as absent. | DEVIATIONS [09] |
| NEW | `scope=PLATFORM` (reads included) requires `PLATFORM_SETTINGS_MANAGE` and the PLATFORM tenant; outside it → 403 `SETTING_PLATFORM_SCOPE_FORBIDDEN`. `scope=PLATFORM` does not additionally need `CONFIG_*`. | DEVIATIONS [09] |
| NEW | Typed reads: Boolean accepts only `true` / `false` (case-insensitive); Duration accepts ISO-8601 or `<n>` followed by one of ns, us, ms, s, m, h, d (no unit = ms); a malformed value or unsupported type → `SETTING_TYPE_MISMATCH` (also from `find` / `getOrDefault`: a default covers a missing setting, never a malformed one). | DEVIATIONS [09] |
| NEW | Cache `erpCoreSettings`, key `<tenantId>:<KEY>`, absent keys cached as null; every CRUD write evicts all entries after commit. | DEVIATIONS [09] (caching entry) |
| CHANGED | RULE-CU-001 (unique `configKey`): unique per owner — one platform default and at most one override per tenant for a key (unique index on `(COALESCE(TENANT_ID, 0), CONFIG_KEY)`). | V14__sequence_and_settings.sql |
| NEW | Optimistic locking (`VERSION`); a concurrent update → 409 `CONCURRENT_MODIFICATION`. | DEVIATIONS [05] |
| NEW | `@Audited` on `AppConfiguration`: writes are recorded under the acting tenant (a platform-default write lands in PLATFORM); `configValue` is recorded. | DEVIATIONS [10] |
| CHANGED (foundation) | Events are no longer a synchronous CU bus: `com.erp.events` with `@TransactionalEventListener(AFTER_COMMIT)` on a tenant-propagating executor. Analysis said "synchronous in-process events (Spring ApplicationEvent)"; implemented asynchronous after-commit because the step-08 plan prescribes async dispatch. | docs/steps/08-report.md |
| NEW (foundation, 1.2.0) | Unknown path → 404 `NOT_FOUND`; `page × size + size > Integer.MAX_VALUE` → 400 `VALIDATION_ERROR` with `fieldErrors[0].field = page` (rejected, not clamped); the catch-all handler answers the first `LocalizedException` in the cause chain (depth ≤ 16) with its own code and status. | DEVIATIONS [15]; CHANGELOG [1.2.0] |
| CHANGED (foundation) | i18n: English base bundle is `messages.properties` and Arabic `messages_ar.properties` (analysis named `messages_en`); the library's `messageSource` puts application bundles first. | DEVIATIONS [01], [03] |

### 3. Error codes
| Kind | Code | HTTP | Raised when | Source |
|---|---|---|---|---|
| NEW | `SETTING_NOT_FOUND` | 404 | `SettingsApi.get` for a key with neither an active override nor an active default (`NoSuchSettingException`) | docs/api-docs/cu/index.md; DEVIATIONS [09] |
| NEW | `SETTING_TYPE_MISMATCH` | 422 | stored value not readable as the requested type | same |
| NEW | `SETTING_PLATFORM_SCOPE_FORBIDDEN` | 403 | `scope=PLATFORM` outside the PLATFORM tenant | same |
| NEW (common) | `CONCURRENT_MODIFICATION` | 409 | optimistic-lock conflict | DEVIATIONS [05] |
| NEW (common, 1.2.0) | `NOT_FOUND` | 404 | unknown path | CHANGELOG [1.2.0] |
| NEW (common) | `Status.TOO_MANY_REQUESTS` → 429 | 429 | used by SEC's customer login limiter | DEVIATIONS [06] |
`APP_CONFIGURATION_*` codes are unchanged.

### 4. Permissions (exact authority strings)
| Kind | Authority | Source |
|---|---|---|
| CHANGED (now code-defined in `CuPermissions`; registry rows pre-date erp-core) | `CONFIG_VIEW`, `CONFIG_CREATE`, `CONFIG_UPDATE`, `CONFIG_DEACTIVATE` (screen `CU_CONFIGURATIONS`). Analysis said "no CORE-9 permissions"; the API is gated by these literal authorities. | erp-core/src/main/java/com/erp/cu/permission/CuPermissions.java |
| NEW | `PERM_PLATFORM_SETTINGS_VIEW` (gateway), `PLATFORM_SETTINGS_MANAGE` — registry module `PLATFORM`, screen `PLATFORM_SETTINGS`; effective only inside the PLATFORM tenant | DEVIATIONS [09] |

### 5. Entity
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | ENTITY-CU-001 AppConfiguration | + `tenantId` (nullable: NULL = platform default), + `version`; derived `scope` (PLATFORM / TENANT) in responses; global entity (`GlobalAuditableEntity`, filtered explicitly, not by `@TenantId`) | V10, V14; DEVIATIONS [09] |

### 6. Dependencies (A7) — deltas
"CU ROOT — no dependency" no longer holds strictly: `CORE_TENANT` FK and `TenantContext` (tenant),
`CuPermissions` (SEC permission SPI), `@Audited` (audit). — V10; DEVIATIONS [06], [09], [10]
