<!-- SRS — Governed by SRS Governance Engine (Project 1) | PART A + PART B -->

# وثيقة التحليل (SRS)
## خدمة الملفات | File Service (FILE)

---

# PART A — MODULE FOUNDATION

## A1 — معلومات الوثيقة (Document Information)

| البند | القيمة |
|---|---|
| **اسم المشروع** | منصة Foundation (Domain: ERP) |
| **الموديول** | خدمة الملفات (File Service) |
| **Feature Code** | FILE-001 |
| **Feature Type** | Transactional (FileDocument) + Reference (FileCategory) |
| **الطبقة / النوع** | L1 · Service (Provider) · dep: CU, SEC |
| **النسخة** | 1.1 (أُضيفت PART B — Frontend) |
| **التاريخ** | 2026-09-02 |
| **الحالة** | Draft |
| **Open Questions** | None — see OQ Log |
| **Governed by** | SRS Governance Engine (Project 1) |
| **Deployment Surface** | **Backend + Frontend** — provider (@Service) + REST APIs + شاشات إدارة |

## A2 — السياق الوظيفي (Functional Context)

### ما يشمله هذا الموديول
> أساس تخزين ملفات قابل لإعادة الاستخدام: بايتات في PostgreSQL BYTEA مع بياناتها الوصفية، ووصول محدود المدة عبر روابط برموز مشفّرة AES/GCM (منفصلة عن JWT). نمط مزوّد (@Service) للموديولات المستهلِكة، **مع شاشات إدارة أمامية** للفئات ومستعرض الملفات. ملكية عامة (owner_id + owner_type + module_code). المصدر: module-registry-FILE §SCOPE NOTE.

### ما لا يشمله هذا الموديول
> معالجة/معاينة PDF (PDFBox) · وسيط رسائل / معالجة غير متزامنة · تخزين على نظام ملفات خارجي · التحقق من JWT ذاتياً (يثق بمرشّح Security) · Workflow Engine (RULE-13=OFF).

### وظيفة الموديول
> رفع/تخزين آمن لأي ملف، واسترجاع عبر رابط آمن محدود المدة، وتصنيف بحدود لكل فئة، وأرشفة/إزالة ضمن دورة حياة واضحة — عبر خدمة تُحقَن وشاشات إدارة.

### الوصف الوظيفي التفصيلي
> FileDocument يخزّن البايتات والبيانات؛ الملكية polymorphic (owner_id/type/module_code) — مرجع تطبيقي لا FK محوكَم. FileCategory يعرّف الأنواع والحدود لكل مستهلك. الوصول عبر رمز AES/GCM (~100د) لكل عملية. يثق FILE بمرشّح Security ولا يتحقق من JWT. مبدأ حاكم: تعقيد متوسط.

### ملاحظات عامة
- **قرار P1 (يحسم بند PRD):** الحذف = **soft-delete** (`fileStatusId=DELETED`؛ البايتات تُبقى ما لم تُعرَّف سياسة تطهير). المصدر: business-policies-FILE §SCOPE EXCEPTIONS + prd-FILE Open Item.
- **قرار P1:** FileDocument حركي **تقني** — لا `fiscal_year_id`/`period_id` (لا سياق محاسبي) — انحراف موثّق عن 5.4.2.
- **قرار P1:** لا Business Code (يُشار للملف بالمعرّف/الرمز الآمن — BC-RULE-0 غير منطبق).

## A3 — الكيانات والحقول (Entities & Fields)
*(POSTGRESQL_16 — audit مطوي: createdBy/At, updatedBy/At)*

### ENTITY-FILE-001 — FileDocument (المستند/الملف)
| PRIVATE · Transactional | BC: NO | module-registry-FILE; prd-FILE US-FILE-001/002/004 | Upload, Read/Download(token), List, Archive, Delete(soft) |

| الحقل | النوع | إلزامي | القيم/المصدر | ملاحظات | Label-AR | Label-EN |
|---|---|---|---|---|---|---|
| fileDocumentPk | BIGINT (PK) | نظام | Sequence | — | المعرف | ID |
| ownerId | BIGINT | نعم | — | polymorphic تطبيقي | معرّف المالك | Owner ID |
| ownerType | VARCHAR(100) | نعم | — | نوع الكيان المالك | نوع المالك | Owner Type |
| moduleCode | VARCHAR(50) | نعم | — | الموديول المالك | رمز الموديول | Module Code |
| fileName | VARCHAR(255) | نعم | — | — | اسم الملف | File Name |
| contentType | VARCHAR(150) | نظام | MIME auto-detect | لا يُوثَق من العميل (RULE-FILE-002) | نوع المحتوى | Content Type |
| fileSize | BIGINT | نظام | — | بايت | الحجم | Size |
| fileContent | BYTEA | نعم | — | بايتات الملف | المحتوى | Content |
| fileTypeId | VARCHAR(50) | نعم | LOV-FILE-001 | code | نوع الملف | File Type |
| fileStatusId | VARCHAR(50) | نعم | LOV-FILE-002 | دورة الحياة (A6) | الحالة | Status |
| fileCategoryFk | BIGINT (FK) | لا | ENTITY-FILE-002 | الفئة والحدود | الفئة | Category |

### ENTITY-FILE-002 — FileCategory (فئة المستند)
| PRIVATE · Reference | BC: NO | module-registry-FILE; biz-pol-FILE; prd-FILE US-FILE-003 | Create, Read, Update, Deactivate |

| الحقل | النوع | إلزامي | القيم | ملاحظات | Label-AR | Label-EN |
|---|---|---|---|---|---|---|
| fileCategoryPk | BIGINT (PK) | نظام | Sequence | — | المعرف | ID |
| categoryCode | VARCHAR(50) | نعم | UNIQUE | — | رمز الفئة | Category Code |
| nameAr / nameEn | VARCHAR | نعم | — | — | الاسم | Name |
| maxSizeBytes | BIGINT | لا | افتراضي RULE-FILE-001 | تجاوز الحد لكل فئة | الحد الأقصى | Max Size |
| allowedContentTypes | TEXT | لا | — | تجاوز الأنواع لكل فئة | الأنواع المسموحة | Allowed Types |
| isActiveFl | SMALLINT | نعم | 1/0 | ⚠ Fl | نشط | Active |

## A4 — قواعد التحقق (Business Rules)
| RULE-ID | Scope | Trigger | Statement | Message-AR | Message-EN | Source |
|---|---|---|---|---|---|---|
| RULE-FILE-001 | FILE-001 | Upload | content ≤ 5MB, request ≤ 10MB (overridable/category). | حجم الملف يتجاوز المسموح. | File exceeds allowed size. | biz-pol POLICY-CLI-01 |
| RULE-FILE-002 | FILE-001 | Upload | auto-detect MIME (not client header); restrict to accepted types. | نوع الملف غير مسموح. | File type not allowed. | biz-pol POLICY-CLI-02 |
| RULE-FILE-003 | FILE-001 | Up/Download | require fresh AES/GCM token; invalid after ~100m; single-use. | رابط الوصول غير صالح/منتهٍ. | Access link invalid/expired. | biz-pol POLICY-CLI-03 |
| RULE-FILE-004 | FILE-001 | Access | delegate auth to Security filter (no self JWT). | — (معماري) | — (architecture) | reg §AUTO |
| RULE-FILE-005 | FILE-001 | Upload | require ownerId, ownerType, moduleCode. | بيانات الملكية إلزامية. | Ownership fields required. | reg §SCOPE |
| RULE-FILE-006 | FILE-001 | Delete | soft delete (`fileStatusId=DELETED`); bytes retained by default. | حذف منطقي دون إزالة فيزيائية. | Soft delete; bytes retained. | P1 — يحسم prd-FILE |
| RULE-FILE-007 | FILE-002 | إنشاء | unique `categoryCode`. | رمز الفئة مستخدَم مسبقاً. | Category code exists. | integrity |

## A5 — قوائم القيم (LOV / Lookup)
> لا MD_MASTER_LOOKUP مركزي — قوائم **محلية** لـ FILE. القيمة = code، runtime-loaded، لا ENUMs.

**LOV-FILE-001 — FileType** (`fileTypeId`, FILE-001, Dropdown, `FILE_FILE_TYPE`): IMAGE=صورة · DOCUMENT=مستند · SPREADSHEET=جدول · ARCHIVE=أرشيف · OTHER=أخرى.

**LOV-FILE-002 — FileStatus** (`fileStatusId`, FILE-001, Dropdown, `FILE_FILE_STATUS`): ACTIVE=نشط · ARCHIVED=مؤرشف · DELETED=محذوف.

## A6 — دورة الحالة (Status Lifecycle) — FileDocument
```
[ACTIVE] ──أرشفة──► [ARCHIVED] ──حذف──► [DELETED] ✗   ([ACTIVE] ──حذف soft──► [DELETED])
```
> RULE-13 — لا Workflow.

## A7 — تبعيات الموديولات (Module Dependencies)
### الكيانات المُستهلَكة
> لا كيان محوكَم: owner_id/type مرجع polymorphic (reg §SHARED CONSUMED = none).
### الخدمات والتكاملات
| الخدمة | الغرض | نوع التكامل |
|---|---|---|
| Security (SEC) | مرشّح المصادقة؛ createdBy | SOFT — XM candidate SOFT-READ (MODE 1.5) |
| Common Utils (CU) | exceptions/config/events/filtering | USES (library) |
> master-registry §8: «FILE → SEC : SOFT».

---

# ══════════════════════════════════════════════════════════
# PART B — SCREEN SPECIFICATIONS (Frontend: React/TS/Vite)
# ══════════════════════════════════════════════════════════
> ملاحظة: رفع الملفات عملياً يُدمَج داخل شاشة الموديول المالك (سياقياً)؛ الشاشتان أدناه إداريتان (فئات + مستعرض/إدارة).

---

## SCR-FILE-001 — إدارة فئات الملفات (File Categories)

### B1 — تعريف الشاشة
| البند | القيمة |
|---|---|
| **SCR-ID** | SCR-FILE-001 · **UI Pattern** PATTERN-2 (SIDE_DRAWER) |
| **Pattern Reason** | FileCategory كيان مرجعي بسيط — لا سطور متكررة ولا هرمية (5.8.2 → PATTERN-2) |
| **SCR-ID Scope** | ONE SCR-ID — Unified — CORE-9 |
| **ENTITY-ID** | ENTITY-FILE-002 |
| **page_code** | `FILE_CATEGORIES` · parent nav: خدمة الملفات |

### B3 — مواصفة الإدخال
- **قائمة + فلاتر:** categoryCode, nameAr, isActiveFl.
- **تحرير (Drawer):** categoryCode, nameAr, nameEn, maxSizeBytes, allowedContentTypes, isActiveFl → A3.
- **القواعد:** حفظ → RULE-FILE-007 (وحدود الفئة تُغذّي RULE-FILE-001/002).

### B4 — الصلاحيات (CORE-9 / SEC-3)
> `page_code = FILE_CATEGORIES` — Security Engine يولّد الصلاحيات الأربع تلقائياً. لا seed لـ PERM_* (SEC-3).

| الشاشة | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|
| SCR-FILE-001 | FILE_ADMIN | FILE_ADMIN | FILE_ADMIN | FILE_ADMIN |
> FILE_ADMIN = دور إدارة الملفات الأساس؛ المنح بيانات (تُدار عبر SEC / SCR-SEC-002).

### B5 — الواجهات المستخدَمة
API-FILE-007 (CRUD categories), API-FILE-008 (lookups).

---

## SCR-FILE-002 — مستعرض الملفات (File Browser / Management)

### B1 — تعريف الشاشة
| البند | القيمة |
|---|---|
| **SCR-ID** | SCR-FILE-002 · **UI Pattern** PATTERN-2 (SIDE_DRAWER) |
| **Pattern Reason** | قائمة بحث + Drawer تفاصيل (بيانات وصفية/تنزيل/أرشفة) — لا سطور محسوبة ولا هرمية |
| **SCR-ID Scope** | ONE SCR-ID — Unified — CORE-9 |
| **ENTITY-ID** | ENTITY-FILE-001 |
| **page_code** | `FILE_BROWSER` · parent nav: خدمة الملفات |

### B3 — مواصفة الإدخال
- **قائمة + فلاتر:** fileName, moduleCode, ownerType/ownerId, `fileTypeId` (LOV-FILE-001), `fileStatusId` (LOV-FILE-002).
- **Drawer (تفاصيل/إجراءات):** عرض البيانات الوصفية (read-only) + تنزيل عبر رمز آمن + أرشفة/حذف. (المحتوى fileContent لا يُعرَض كحقل — يُنزَّل عبر الرابط الآمن.)
- **القواعد:** تنزيل → RULE-FILE-003, 004 · حذف/أرشفة → RULE-FILE-006 · العرض يخضع لملكية RULE-FILE-005.

### B4 — الصلاحيات (CORE-9 / SEC-3)
> `page_code = FILE_BROWSER` — توليد تلقائي للصلاحيات الأربع. لا seed لـ PERM_*.

| الشاشة | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|
| SCR-FILE-002 | FILE_ADMIN | — (الرفع سياقي في الموديول المالك) | FILE_ADMIN (أرشفة) | FILE_ADMIN (حذف soft) |
> CREATE عبر هذه الشاشة اختياري — الرفع الأساسي يتم في شاشة الموديول المالك عبر خدمة FILE.

### B5 — الواجهات المستخدَمة
API-FILE-002 (access-token), API-FILE-003 (download), API-FILE-004 (metadata), API-FILE-005 (list by owner), API-FILE-006 (archive/delete), API-FILE-008 (lookups). *(الرفع: API-FILE-001.)*

---

# MODULE-LEVEL FUNCTIONAL APIs
> STACK-1: `/api/v1/files/...`. POSTGRESQL_16.

| API-ID | العملية | HTTP | المسار | RULE-IDs |
|---|---|---|---|---|
| API-FILE-001 | رفع ملف | POST | /api/v1/files | RULE-FILE-001, 002, 005 |
| API-FILE-002 | رمز وصول | POST | /api/v1/files/{id}/access-token | RULE-FILE-003 |
| API-FILE-003 | تنزيل | GET | /api/v1/files/download?token= | RULE-FILE-003, 004 |
| API-FILE-004 | بيانات وصفية | GET | /api/v1/files/{id} | RULE-FILE-004 |
| API-FILE-005 | قائمة حسب المالك | GET | /api/v1/files?ownerId=&ownerType=&moduleCode= | RULE-FILE-004 |
| API-FILE-006 | أرشفة/حذف (soft) | DELETE | /api/v1/files/{id} | RULE-FILE-006 |
| API-FILE-007 | CRUD الفئات | POST/GET/PUT/DELETE | /api/v1/files/categories | RULE-FILE-007 |
| API-FILE-008 | قوائم القيم | GET | /api/v1/files/lookups/{lookupKey} | — |

> **Provider (in-process):** `FileService` (store / retrieve / issueAccessToken) — يُحقَن في NOTIF والموديولات المستقبلية.

---

# STANDALONE

## Permissions Summary & Registry Update
> CORE-9: كل شاشة = SCR-ID واحد = صف SEC_PAGES واحد. Security Engine يولّد الصلاحيات الأربع لكل page_code (لا seed أسماء PERM_* — SEC-3).

| الشاشة (page_code) | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|
| SCR-FILE-001 (FILE_CATEGORIES) | FILE_ADMIN | FILE_ADMIN | FILE_ADMIN | FILE_ADMIN |
| SCR-FILE-002 (FILE_BROWSER) | FILE_ADMIN | — | FILE_ADMIN | FILE_ADMIN |

### Registry Update — MODE 1
```
Source Mode  : MODE 1 | Feature Code: FILE-001 | v1.1 (+Frontend)
New Entities : FILE-001 FileDocument (Transactional), FILE-002 FileCategory (Reference) — PRIVATE
New Lookups  : FILE_FILE_TYPE, FILE_FILE_STATUS — local
New Screens  : SCR-FILE-001 (FILE_CATEGORIES), SCR-FILE-002 (FILE_BROWSER)
New APIs     : API-FILE-001 → API-FILE-008 (+ provider FileService)
XM-IDs Open  : FILE → SEC (SOFT-READ candidate) — MODE 1.5
OQ-IDs Open  : None (delete-semantics resolved — RULE-FILE-006)
Gate Status  : PASSED ✓ | Next: MODE 1.5 (Project 2)
```
> لمشرف السجل: master-registry §10 → FILE·P1=✓؛ §5 FILE-001/002؛ سجّل SEC_PAGES: FILE_CATEGORIES/FILE_BROWSER.

## OQ Log
```
OPEN QUESTIONS LOG — File Service (FILE) — 2026-09-02
— None — بند حذف الملفات حُسِم في RULE-FILE-006 (soft-delete).
```

---
*End of srs-FILE.md | FILE-001 | v1.1 | Backend + Frontend | Next: MODE 1.5*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 02, 03, 05, 06, 07, 08, 10, 15
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository: `com/erp/…` stands for
`erp-core/src/main/java/com/erp/…`, `V<N>` for `erp-core/src/main/resources/db/migration/core/V<N>__*.sql`;
line numbers are those of `main` on the revision date (the 1.3.0 refactors below moved a few lines; the
behaviour cited is that of the tag). Endpoint paths and methods are taken from `docs/api-docs/file/`.
No RULE / API / ENTITY ids are minted; every row is labelled NEW / CHANGED / REMOVED / NOT IMPLEMENTED.

### 1. Endpoints — الواجهات
| Kind | API | Method | Path | Access | Notes | Source |
|---|---|---|---|---|---|---|
| CHANGED | API-FILE-007 (list) | POST | `/api/v1/files/categories/search` | `PERM_FILE_CATEGORIES_VIEW` | The analysed GET list is a POST with the shared filter envelope (`CategorySearchRequest` extends `BaseSearchContractRequest`; page ≥ 0, size 1..200, default 20). Filterable / sortable: `categoryCode`, `nameAr`, `nameEn` (LIKE), `isActive` (EXACT), `createdAt`; any other field is dropped silently. Precedent ADR-SEC-003. | com/erp/file/controller/FileCategoryController.java:48-53; com/erp/file/service/FileCategoryService.java:53-55, 84-100; docs/api-docs/file/endpoints/file-categories.md; ADR-FILE-006 |
| CHANGED | API-FILE-007 (create) | POST | `/api/v1/files/categories` | `PERM_FILE_CATEGORIES_CREATE` | Body `categoryCode` (≤ 50), `nameAr` (≤ 200), `nameEn` (≤ 100), `maxSizeBytes`, `allowedContentTypes`, `isActiveFl` (default true), `allowPublic` (default false); 201. 409 `FILE_CATEGORY_CODE_DUPLICATE` from the pre-check against the normalised code and from the unique-constraint race (`DataIntegrityViolationException` caught). | FileCategoryService.java:57-82; com/erp/file/dto/CategoryCreateRequest.java |
| CHANGED | API-FILE-007 (read) | GET | `/api/v1/files/categories/{id}` | `PERM_FILE_CATEGORIES_VIEW` | 404 `FILE_CATEGORY_NOT_FOUND`; the response carries `allowPublic`. | FileCategoryService.java:102-113 |
| CHANGED | API-FILE-007 (update) | PUT | `/api/v1/files/categories/{id}` | `PERM_FILE_CATEGORIES_UPDATE` | Body `nameAr`, `nameEn`, `maxSizeBytes`, `allowedContentTypes`, `allowPublic` (null keeps the current value). `categoryCode` is immutable and `isActiveFl` is not editable through PUT, although B3 lists both as drawer fields; 404. | com/erp/file/dto/CategoryUpdateRequest.java:24-41; FileCategoryService.java:115-132; file-categories.md (PUT) |
| CHANGED | API-FILE-007 (delete) | DELETE | `/api/v1/files/categories/{id}` | `PERM_FILE_CATEGORIES_DELETE` | Soft deactivate (`IS_ACTIVE_FL = 0`), 204, no child guard. No activate endpoint exists (`FileCategory.activate()` is never exposed): a deactivated category cannot be reactivated through the API. | FileCategoryController.java:72-77; FileCategoryService.java:138-151; com/erp/file/entity/FileCategory.java:99-105; ADR-FILE-006 |
| CHANGED | API-FILE-005 | GET | `/api/v1/files?ownerId&ownerType&moduleCode` | `PERM_FILE_BROWSER_VIEW` | + optional `fileTypeId`, `fileStatusId` (EXACT), `page` (default 0), `size` (default 20, clamped 1..200), `sort` ∈ {`fileName`, `createdAt`, `fileSize`} (anything else → `createdAt`), always DESC. DELETED rows are returned unless `fileStatusId` excludes them. Rows gain `storageProvider`, `visibility`, `publicUrl` (set only for a servable public document). | com/erp/file/controller/FileController.java:91-104; com/erp/file/service/FileService.java:88, 248-272; com/erp/file/repository/FileDocumentRepository.java:49-62 |
| CHANGED | API-FILE-001 | POST | `/api/v1/files` — multipart `file` + form `ownerId`, `ownerType` (≤ 100), `moduleCode` (≤ 50), `fileCategoryFk` | `PERM_FILE_BROWSER_CREATE` (B4 and the Permissions Summary said "—": upload contextual) | Stores through the selected provider, records `STORAGE_PROVIDER`, `STORAGE_REF`, `CONTENT_HASH`, publishes `FileDocumentPublishedEvent` (PRIVATE); 201. Errors: 400 `FILE_DOCUMENT_OWNERSHIP_REQUIRED`, 404 `FILE_CATEGORY_NOT_FOUND`, 422 `FILE_CATEGORY_INACTIVE`, 413 `FILE_DOCUMENT_SIZE_EXCEEDED`, 415 `FILE_DOCUMENT_TYPE_NOT_ALLOWED`, 500 `FILE_STORAGE_UNAVAILABLE`, 500 `INTERNAL_ERROR` (multipart part unreadable). | FileService.java:108-163, 401-443; com/erp/file/dto/UploadRequest.java; docs/steps/07-report.md; DEVIATIONS [08] |
| CHANGED | API-FILE-003 | GET | `/api/v1/files/download?token=` | `isAuthenticated()` only — no FILE permission; the token must have been issued to the same username | Token TTL 10 min (analysis: ~100 min). A token of another user → 401 `FILE_ACCESS_TOKEN_INVALID` without consuming it; unknown or DELETED document → 404 `FILE_DOCUMENT_NOT_FOUND`; the content is opened from the provider recorded on the document before the token is consumed (single use); 500 `FILE_STORAGE_UNAVAILABLE`. Raw binary body, `Content-Disposition: attachment` (RFC 6266 filename). | FileService.java:194-230; com/erp/file/domain/FileAccessTokenDomainService.java:28; FileController.java:62-75; DEVIATIONS [07] |
| CHANGED | API-FILE-004 | GET | `/api/v1/files/{id}` | `PERM_FILE_BROWSER_VIEW` | + `storageProvider`, `visibility`, `publicUrl`. A DELETED document still answers 200 (the soft-delete is visible as `fileStatusId`); 404 only for an unknown id or another tenant's id. | FileService.java:233-245 |
| CHANGED | API-FILE-006 | DELETE | `/api/v1/files/{id}?action=ARCHIVE\|DELETE` | `PERM_FILE_BROWSER_UPDATE` for `ARCHIVE`, `PERM_FILE_BROWSER_DELETE` for `DELETE` (the gate is bound to the `action` parameter) | Default `ARCHIVE`; 200 + metadata (not 204). Unknown action → 400 `FILE_DOCUMENT_INVALID_TRANSITION`; illegal A6 transition → 422 same code; 404. Archive / delete leave visibility and slug untouched (§2). | FileController.java:106-112; FileService.java:280-304, 424-434 |
| CHANGED | API-FILE-002 | POST | `/api/v1/files/{id}/access-token` | `PERM_FILE_BROWSER_VIEW` | Response `accessToken`, `expiresAt` (now + 10 min). A DELETED document → 404. The token is bound to the issuing username in the token store for the TTL. | FileService.java:166-191; com/erp/file/dto/AccessTokenResponse.java |
| NEW | — | PATCH | `/api/v1/files/{id}/visibility` | `FILE:DOCUMENT:PUBLISH` (the generated contract prints the constant name `DOCUMENT_PUBLISH`) | Body `visibility` = PUBLIC / PRIVATE; 200 + metadata. 409 `FILE_PUBLIC_NOT_ALLOWED` when the category does not allow public files; DELETED → 404; ARCHIVED may be published (the event fires; the URL answers 404 — §2); PRIVATE → PRIVATE is a no-op 200. The step file named `/api/v1/file/documents/{id}/visibility`; implemented under the existing `/api/v1/files` prefix. | FileService.java:313-346; docs/api-docs/file/endpoints/file-documents.md; DEVIATIONS [07] |
| CHANGED | API-FILE-008 | GET | `/api/v1/files/lookups/{lookupKey}` | `isAuthenticated()` | Key trimmed and upper-cased; only `FILE_FILE_TYPE` / `FILE_FILE_STATUS` are served; values are read live from MDL (`MdlLookupApi.readActiveValuesByKey`), MDL's own not-found is translated; 404 `FILE_LOOKUP_KEY_UNKNOWN`. Response items `code`, `labelAr`, `labelEn`. | com/erp/file/service/FileLookupService.java:46-58; com/erp/common/lookup/OwnedLookups.java:32-46 |
| NEW | — | GET, HEAD | `/api/v1/public/files/{tenantCode}/{publicSlug}` | none (customer / public chain; tenant from the path) | 200 stream or 302 to the provider URL. 404 `FILE_DOCUMENT_NOT_FOUND` for an unknown, PRIVATE or non-ACTIVE slug or a category no longer `ALLOW_PUBLIC`; 404 `TENANT_NOT_FOUND` / 403 `TENANT_SUSPENDED` from the path tenant; other methods 401 anonymous, 405 customer. The generated contract lists only the 200. | com/erp/file/controller/PublicFileController.java:55-84; com/erp/autoconfigure/ErpCoreSecurityAutoConfiguration.java:98, 180-181; docs/api-docs/file/endpoints/public-files.md; DEVIATIONS [07] |
| NEW | — (in-process) | — | `FileDocumentLookupApi.isAvailable(Long)` | none (no `@PreAuthorize`; the consuming service carries its gate) | True when the id exists in the current tenant and is not DELETED (ACTIVE or ARCHIVED). Consumer: NOTIF, template `attachmentFileId` (XM-NOTIF-002). | com/erp/file/crossmodule/FileDocumentLookupApi.java:13-14; FileDocumentLookupApiImpl.java:24-29; com/erp/notif/service/NotificationTemplateService.java:47, 153-157 |
| NEW | — (in-process) | — | `FileDocumentLookupApi.publicUrl(Long)` | none | The public URL of a servable public document of the current tenant, else empty. Its Javadoc says "non-deleted"; the implementation requires ACTIVE and category `ALLOW_PUBLIC` (an ARCHIVED document gets no URL). No consumer in erp-core. | FileDocumentLookupApi.java:16-21; FileDocumentLookupApiImpl.java:31-40; com/erp/file/service/PublicFileUrls.java:50-61 |
| CHANGED | "Provider (in-process): `FileService` (store / retrieve / issueAccessToken) injected into NOTIF" | — | — | — | Not the cross-module surface. `FileService` is module-internal; the only in-process surface is `FileDocumentLookupApi` (two rows above). Another module stores a file over REST (API-FILE-001) and keeps the id as a soft reference. | FileService.java:52-55; FileDocumentLookupApi.java:5-10 |

### 2. Business rules — قواعد العمل
| Kind | Rule | Statement | Source |
|---|---|---|---|
| CHANGED | RULE-FILE-001 | Defaults 5 MB content / 10 MB request, configurable as `erp.core.files.max-content-bytes` / `max-request-bytes`. A category `maxSizeBytes > 0` REPLACES the default (it may be larger, not only tighter; ≤ 0 is ignored). The request ceiling is applied to the content length and no category overrides it. Both answer 413 `FILE_DOCUMENT_SIZE_EXCEEDED`. The servlet multipart limits (`spring.servlet.multipart.max-file-size` / `max-request-size`) must be ≥ the FILE limits, otherwise Spring's own `MaxUploadSizeExceededException` answers first (the reference app sets 15 MB / 25 MB). | com/erp/file/domain/FileValidationDomainService.java:39-61; FileService.java:137-141; com/erp/autoconfigure/ErpCoreProperties.java:156-160; erp-app-reference/src/main/resources/application.yml:29-33; DEVIATIONS [03] |
| CHANGED | RULE-FILE-002 | No platform-wide accepted-type list exists: POLICY-CLI-02's JPG…7Z list is not enforced, and a category without `allowedContentTypes` accepts every type. Only the category allow-list (comma-separated, case-insensitive) is enforced, and only against the content-sniffed MIME (`URLConnection.guessContentTypeFromStream`, magic bytes); an upload whose type cannot be sniffed is rejected 415 whenever an allow-list applies. The stored `contentType` is the sniffed type, else the filename-derived type, else `application/octet-stream`. `fileTypeId` is derived from the stored MIME: `image/*` → IMAGE; spreadsheet / excel / csv → SPREADSHEET; pdf / msword / wordprocessing / `text/*` → DOCUMENT; zip / x-tar / x-7z / x-rar → ARCHIVE; else OTHER. Known limitation, to be covered by a test case: the JDK sniffer recognises PNG, JPEG, GIF, HTML/XML and a few more, but answers null for PDF, ZIP/OOXML (docx, xlsx), OLE2 (doc, xls) and plain text, so a category allow-list that names `application/pdf` or the Office types rejects every such upload with 415. | FileService.java:130-135, 445-476; FileValidationDomainService.java:72-86 |
| CHANGED | RULE-FILE-003 | Token = AES/GCM (12-byte IV, 128-bit tag) over `fileId + expiry + 16-byte nonce`, base64url; TTL 10 minutes (analysis: ~100 min); key = SHA-256 of `erp.core.files.access-token-secret` (required; startup fails without it). Issue requires `PERM_FILE_BROWSER_VIEW`; the token is bound to the issuing username in the `DownloadTokenStore` (key = SHA-256 of the token) and is single-use: it is consumed only after the content was opened, so a failed load does not burn it, and another user's token answers 401 without consuming it. Store: in-memory by default (per JVM, not shared), Redis when a Redis template bean exists (required for a multi-instance deployment). | FileAccessTokenDomainService.java:28-57, 60-81, 87-112; FileService.java:182-187, 199-227, 486-490; com/erp/file/config/FileTokenConfig.java:16-19; com/erp/autoconfigure/DownloadTokenStoreAutoConfiguration.java:21-23; com/erp/file/service/InMemoryDownloadTokenStore.java:10-14; DEVIATIONS [02], [03]; ADR-FILE-003 |
| REMOVED | POLICY-CLI-03 upload token | No token gates an upload: upload = staff JWT + `PERM_FILE_BROWSER_CREATE`. The time-limited token exists for download only. | FileService.java:108-111 |
| CHANGED | RULE-FILE-005 | Ownership is a mandatory structural triple on upload (400 `FILE_DOCUMENT_OWNERSHIP_REQUIRED`) and a FILTER of the owner list — not an authorisation: any holder of `PERM_FILE_BROWSER_VIEW` reads any owner's metadata, list and download token inside the tenant. B3's "العرض يخضع لملكية RULE-FILE-005" is not an access rule. | FileService.java:117-119, 416-422, 233-272 |
| CHANGED | RULE-FILE-006 (A6) | Transitions ACTIVE → ARCHIVED, ACTIVE → DELETED, ARCHIVED → DELETED; no un-archive; DELETED is terminal (422 `FILE_DOCUMENT_INVALID_TRANSITION`). Soft-delete keeps the bytes (unchanged). | com/erp/file/domain/FileDocumentDomain.java:27-31, 48-50 |
| CHANGED | RULE-FILE-007 | Unique per (tenant, code). The code is trimmed and upper-cased on persist and on update, and the pre-check probes the normalised form. | FileCategory.java:79-97; FileCategoryService.java:63-68, 157-159; V10 L197-198 |
| NEW | Soft-delete visibility | A DELETED document answers 404 `FILE_DOCUMENT_NOT_FOUND` on access-token issue, download and visibility change, but is still returned (200) by metadata and by the owner list; `FileDocumentLookupApi.isAvailable` answers false. | FileService.java:177-180, 216-219, 322-323; FileDocumentDomain.java:67-71; FileDocumentLookupApiImpl.java:26-29 |
| NEW | Archive / delete do not unpublish | ARCHIVE and DELETE leave `VISIBILITY` and `PUBLIC_SLUG` as they are: the public URL stops serving (the public lookup requires ACTIVE) and `publicUrl` is no longer handed out, but the slug stays reserved. | FileService.java:286-304; FileDocumentRepository.java:74-82; PublicFileUrls.java:50-53 |
| NEW | ARCHIVED may be published | `assertNotDeleted` refuses only DELETED, so an ARCHIVED document can be set PUBLIC: it receives a slug and `FileDocumentPublishedEvent(PUBLIC)` fires, yet its URL answers 404 and `publicUrl` is null, and A6 offers no way back to ACTIVE. Recorded as implemented. | FileService.java:322-342; FileDocumentDomain.java:67-71, 98-102 |
| NEW | Storage provider selection | `erp.core.files.storage` (DB default, LOCAL, S3) decides where NEW uploads go; reads use the document's `STORAGE_PROVIDER`; a provider not configured → 500 `FILE_STORAGE_UNAVAILABLE`. Startup fails for an unknown value, LOCAL without an existing writable root, S3 without bucket or SDK. Registry = application `StorageProvider` beans first (`putIfAbsent`: a bean with the key LOCAL or S3 replaces the built-in one; the DB bean is replaced by defining `erpDbStorageProvider`), then LOCAL when `local.root` is set, then S3 when the SDK is present and `s3.bucket` is set (an application `S3Client` bean is honoured). The keys are closed to DB / LOCAL / S3 by `CHK_FILE_DOCUMENT_STORAGE_PROVIDER`: a provider under a new key cannot be stored. | com/erp/autoconfigure/FileStorageAutoConfiguration.java:52-87, 119-146; com/erp/file/storage/StorageProviderRegistry.java:23-46; V12 L34-35; DEVIATIONS [07]; ADR-FILE-001 |
| NEW | LOCAL / S3 key layout | `<tenantId>/<category code lower-cased, or uncategorized>/<yyyy>/<MM>/<documentId>_<filename>`; every segment sanitised to `[A-Za-z0-9._-]` (other characters → `_`, leading dots removed, empty → `file`), the filename segment keeps its last 150 characters. LOCAL resolves every reference inside the root (an escaping reference → `FILE_STORAGE_UNAVAILABLE`) and writes through a temporary file moved atomically into place; the object of a rolled-back upload is deleted again (`TransactionSynchronization`). | com/erp/file/storage/StoragePaths.java:15, 21-36; LocalFsStorageProvider.java:47-65, 100-111; FileService.java:379-391; docs/steps/07-report.md |
| NEW | Publish rule | PUBLIC requires a category with `ALLOW_PUBLIC = TRUE` (`FileDocumentDomain.assertCanBePublic`, 409 `FILE_PUBLIC_NOT_ALLOWED`; a document without a category never may); PUBLIC if and only if a slug is present (DB CHECK); slug = 24 `SecureRandom` bytes, base64url without padding (32 characters), unique per tenant. Re-publishing keeps the slug (stable URL); PRIVATE clears it. | FileDocumentDomain.java:57-61; FileService.java:325-333; PublicFileUrls.java:38-43; V12 L39-44; DEVIATIONS [07] |
| NEW | Public serving | Served only when PUBLIC + ACTIVE + category `ALLOW_PUBLIC = TRUE`; the category's `IS_ACTIVE_FL` is NOT checked (a deactivated category still serves its public files). Headers: `Cache-Control: max-age=86400, public`, `ETag: "<sha256>"`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy: sandbox; default-src 'none'`. `inline` only for `image/png`, `image/jpeg`, `image/gif`, `image/webp`, `image/avif`, `image/bmp`, `application/pdf` (SVG excluded); everything else `attachment`. No `If-None-Match` / 304 handling. | FileDocumentRepository.java:74-82; PublicFileController.java:48-51, 65-84; FileDocumentDomain.java:79-91; DEVIATIONS [07] (public GET, review round 1); ADR-FILE-002 |
| NEW | Public URL | Relative `/api/v1/public/files/{tenantCode}/{slug}`, absolute when `erp.core.files.public-base-url` is set, or the provider's direct URL (S3 with `s3.public-base-url`, `<base>/<key>`). On that path the path tenant always wins over token and header; a token of another tenant is ignored (treated as anonymous). | PublicFileUrls.java:50-70; com/erp/file/storage/S3StorageProvider.java:17-28; ErpCoreProperties.java:252-261; DEVIATIONS [07] |
| NEW | S3 operational constraint | With `s3.public-base-url`, private and public objects share one bucket, keys are predictable and objects carry no ACL: expose only a public prefix on the CDN; withdrawing a file does not revoke a direct S3 / CDN URL already handed out (only the platform URL stops). The redirect target's headers are the operator's (serve it on a separate origin). | DEVIATIONS [07] (review round 1, S3 risk); ADR-FILE-002 |
| NEW | Tenant confinement | Every FILE row is tenant-scoped (`TENANT_ID`, Hibernate tenant filter); an id of another tenant answers as not found; RULE-FILE-007 is per tenant. | docs/steps/05-report.md; V10 |
| NEW | Audit | `@Audited` on documents (`FILE_DOCUMENT`) and categories (`FILE_CATEGORY`); `contentHash` is never recorded. | com/erp/file/entity/FileDocument.java:45; FileCategory.java:33; DEVIATIONS [10] |
| NEW | Content hash | SHA-256 hex of the upload bytes, stored as `CONTENT_HASH` and used only as the public ETag; no de-duplication (two identical uploads are two documents). | FileService.java:145; V12 L20, L27 |
| REMOVED | A2 exclusion "تخزين على نظام ملفات خارجي" | LOCAL and S3 providers exist; DB remains the default. PDF processing, a message broker and self JWT validation stay excluded. | docs/steps/07-report.md |

### 3. Error codes — رموز الأخطاء
The analysis above names no `FILE_*` code; this is the catalogue as implemented (all bilingual in `i18n/messages*.properties`).

| Kind | Code | HTTP | Raised when | Rule | Source |
|---|---|---|---|---|---|
| NEW | `FILE_DOCUMENT_SIZE_EXCEEDED` | 413 | content larger than the category limit or the configured default; content larger than the request ceiling | RULE-FILE-001 | FileValidationDomainService.java:44-47, 57-60 |
| NEW | `FILE_DOCUMENT_TYPE_NOT_ALLOWED` | 415 | sniffed type absent from the category allow-list, or unsniffable while an allow-list applies | RULE-FILE-002 | FileValidationDomainService.java:82-85 |
| NEW | `FILE_ACCESS_TOKEN_INVALID` | 401 | tampered, malformed or expired token; token of another user; token already consumed or unknown to the store; token issue failure | RULE-FILE-003 | FileAccessTokenDomainService.java:79, 103-110; FileService.java:204-207, 224-227 |
| NEW | `FILE_DOCUMENT_OWNERSHIP_REQUIRED` | 400 | `ownerId`, `ownerType` or `moduleCode` missing / blank on upload | RULE-FILE-005 | FileService.java:416-422 |
| NEW | `FILE_CATEGORY_CODE_DUPLICATE` | 409 | create with a code already present in the tenant (pre-check or constraint race) | RULE-FILE-007 | FileCategoryService.java:65-78; com/erp/file/domain/FileCategoryDomain.java:22-25 |
| NEW | `FILE_CATEGORY_INACTIVE` | 422 | upload referencing a deactivated category | — (no analysed rule) | FileService.java:408-412 |
| NEW | `FILE_LOOKUP_KEY_UNKNOWN` | 404 | lookup key other than `FILE_FILE_TYPE` / `FILE_FILE_STATUS`, or MDL answers not-found for it | API-FILE-008 | FileLookupService.java:56-57; OwnedLookups.java:35-43 |
| NEW | `FILE_DOCUMENT_INVALID_TRANSITION` | 400 / 422 | 400: `action` other than ARCHIVE / DELETE; 422: transition not allowed by A6 | RULE-FILE-006 | FileService.java:424-434; FileDocumentDomain.java:48-50; com/erp/common/domain/StatusTransitions.java:31 |
| NEW | `FILE_DOCUMENT_NOT_FOUND` | 404 | unknown id (or another tenant's); DELETED document on token issue, download or visibility change; public slug not servable | — | FileService.java:172-180, 211-219, 318-323, 359-363 |
| NEW | `FILE_CATEGORY_NOT_FOUND` | 404 | unknown category id on read, update, deactivate or upload | — | FileCategoryService.java:108-110; FileService.java:405-407 |
| NEW | `FILE_PUBLIC_NOT_ALLOWED` | 409 | publish in a category without `ALLOW_PUBLIC`, or without a category | publish rule (§2) | FileDocumentDomain.java:57-61 |
| NEW | `FILE_STORAGE_UNAVAILABLE` | 500 | provider recorded on the document not configured; provider I/O failure; malformed or escaping storage reference; DB content missing. Also reachable from metadata and the owner list through `PublicFileUrls.of` for a servable public document whose provider is gone. `Status` has no 503. | storage SPI contract | StorageProviderRegistry.java:40-46; LocalFsStorageProvider.java:61-64, 100-111; DbStorageProvider.java:48-50, 65-71; PublicFileUrls.java:55; DEVIATIONS [07]; ADR-FILE-007 |
| NEW (tenant module, surfaced on the public path) | `TENANT_NOT_FOUND` / `TENANT_SUSPENDED` | 404 / 403 | unknown / suspended `{tenantCode}` in the public file path | path tenant resolution | ErpCoreProperties.java:255-261; DEVIATIONS [07] |
| NEW (common) | `INTERNAL_ERROR` | 500 | the multipart part cannot be read | — | FileService.java:436-443 |
| NEW (common) | `CONCURRENT_MODIFICATION` | 409 | optimistic-lock conflict (`VERSION`) | — | DEVIATIONS [05] |
| NEW (common, 1.2.0) | `NOT_FOUND` | 404 | unknown path (was 500) | — | CHANGELOG [1.2.0]; DEVIATIONS [15] |
| NEW (common, 1.2.0) | `VALIDATION_ERROR` | 400 | bean validation of the DTOs and query parameters; search `page` whose offset overflows `int` (`fieldErrors[0].field = page`; was 500) | — | CHANGELOG [1.2.0]; DEVIATIONS [15] |

### 4. Permissions (exact authority strings) — الصلاحيات
| Kind | Authority | Screen / meaning | Source |
|---|---|---|---|
| NEW | `PERM_FILE_BROWSER_CREATE` | `FILE_BROWSER` CREATE — gates the upload (API-FILE-001). B4 and the Permissions Summary said "—" (no CREATE, upload contextual); implemented, code-defined and seeded. | com/erp/file/permission/FilePermissions.java:28-29; V7 L134 |
| CHANGED | `PERM_FILE_BROWSER_UPDATE`, `PERM_FILE_BROWSER_DELETE` | Both gate the single endpoint API-FILE-006, selected by its `action` parameter (ARCHIVE → UPDATE, DELETE → DELETE); any other action passes the gate and answers 400. | FileService.java:280-285; FilePermissions.java:30-33 |
| CHANGED | `PERM_FILE_CATEGORIES_VIEW/CREATE/UPDATE/DELETE`, `PERM_FILE_BROWSER_VIEW` | Same names as analysed, but code-defined (`FilePermissions implements PermissionContributor`) and seeded: B4's "Security Engine generates the four permissions, no seed for PERM_* (SEC-3)" does not hold. V7 seeds the module `FILE`, the screens `FILE_CATEGORIES` / `FILE_BROWSER`, the 8 `PERM_FILE_*` actions and the role `FILE_ADMIN`, and grants every FILE module / screen / action to `SYS_ADMIN` and `FILE_ADMIN` (three tiers). `PERM_FILE_BROWSER_VIEW` is the gateway of `FILE_BROWSER`. | FilePermissions.java:17-68; V7 L39, L70-71, L129-136, L153, L166-199 (FILE_ADMIN L174, SYS_ADMIN L177); docs/steps/06-report.md |
| NEW | `FILE:DOCUMENT:PUBLISH` | `FILE_BROWSER` action `PUBLISH` (PATCH visibility); literal legacy-style code, not `PERM_FILE_BROWSER_PUBLISH`, because V12 seeded and granted that exact code (PLATFORM's `SYS_ADMIN` and `FILE_ADMIN`; provisioning copies it); effective with the `PERM_FILE_BROWSER_VIEW` gateway. | FilePermissions.java:34-40, 67; V12 L57-69; DEVIATIONS [07]; ADR-FILE-004 |
| CHANGED | download (API-FILE-003) | Authentication only (`isAuthenticated()`) plus the user binding of the token; no FILE permission. B4 mapped download to FILE_ADMIN VIEW — the VIEW gate sits on token issue (API-FILE-002), not on the download. | FileService.java:194-207 |
| CHANGED | lookups (API-FILE-008) | `isAuthenticated()` — any authenticated principal, no FILE permission. | FileLookupService.java:50-51 |
| NEW | `FileDocumentLookupApi` | No `@PreAuthorize` on the in-process surface; the consumer's service carries the gate. | FileDocumentLookupApiImpl.java:13-15 |
| NEW (no catalog entry) | public file GET / HEAD | Unauthenticated; tenant from the path; other methods are not permitted. | ErpCoreSecurityAutoConfiguration.java:180-181; DEVIATIONS [07] |

### 5. Entities, fields, LOVs — الكيانات والحقول وقوائم القيم
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | ENTITY-FILE-001 FileDocument | + `storageProvider` VARCHAR(8) (DB / LOCAL / S3, CHECK-closed), `storageRef` VARCHAR(512), `visibility` VARCHAR(8) (PRIVATE / PUBLIC, default PRIVATE), `publicSlug` VARCHAR(64) (unique per tenant where set), `contentHash` VARCHAR(64), `tenantId`, `version`; `fileContent` no longer required (DB provider only). Analysis said `fileContent` BYTEA required; implemented nullable because LOCAL / S3 keep the bytes outside the row (step 07). `contentType` is server-sniffed with a filename fallback (§2). | FileDocument.java:89-138; V12; V10 |
| CHANGED | ENTITY-FILE-002 FileCategory | + `allowPublic` (default false) as a native `BOOLEAN` column with no converter — unlike `isActiveFl`, which keeps the SMALLINT 1/0 convention through `BooleanNumberConverter`; + `tenantId`, `version`. `categoryCode` is normalised (trim + upper-case) on persist and update and is immutable through the API; `isActiveFl` is set on create only and cleared by DELETE; no reactivation. | FileCategory.java:69-77, 79-105; CategoryUpdateRequest.java:24-41; V12 L54; V10 |
| CHANGED | LOV-FILE-001 FileType, LOV-FILE-002 FileStatus | A5 said FILE-local lists, "no central MD_MASTER_LOOKUP". Implemented as MDL lookup types `FILE_FILE_TYPE` / `FILE_FILE_STATUS` (owner module FILE) with the same codes and bilingual labels, seeded by V8 into `MDL_LOOKUP_TYPE` / `MDL_LOOKUP_VALUE`; API-FILE-008 reads them live through `MdlLookupApi`. The codes themselves stay constants in FILE (`FileLookupService.TYPE_*`, `FileDocumentDomain.STATUS_*`) because the classifier and the state machine need them without a database read. | V8 L25-31, L38-62; FileLookupService.java:33-46; FileDocumentDomain.java:19-21; ADR-FILE-005 |
| NEW | `CHK_FILE_DOCUMENT_STORAGE_PROVIDER` | Closes the provider keys to DB / LOCAL / S3 (see §2 storage provider selection). | V12 L34-35 |
| NEW | Content hash, no de-duplication | `CONTENT_HASH` serves the ETag only; identical content uploaded twice yields two rows and two objects. | FileService.java:145; V12 L20 |

### 6. Dependencies (A7) — التبعيات
| Kind | Direction | Item | Source |
|---|---|---|---|
| NEW | exposed | `FileDocumentLookupApi.isAvailable(Long)` → NOTIF (template `attachmentFileId`, XM-NOTIF-002); `publicUrl(Long)` (no consumer in erp-core). | FileDocumentLookupApi.java; NotificationTemplateService.java:153-157 |
| NEW | exposed (SPI) | `com.erp.file.storage.StorageProvider`: `key()`, `put(StorageTarget, InputStream, size, contentType) → StoredObject`, `get(storageRef)`, `delete(storageRef)`, `publicUrl(storageRef)`; records `StorageTarget(tenantId, category, documentId, filename)` (`NO_CATEGORY = "uncategorized"`) and `StoredObject(storageRef, size)`. Contract: failures are signalled only as `LocalizedException(FILE_STORAGE_UNAVAILABLE)`, never a raw I/O exception; the DB provider's `put` must run inside the transaction that persisted the document (it looks the row up in the same persistence context and the bytes are written at flush); keys closed by the DB CHECK. | com/erp/file/storage/StorageProvider.java:14-33; StorageTarget.java:8-11; StoredObject.java:4; DbStorageProvider.java:14-21, 32-40; docs/steps/07-report.md |
| NEW | publishes | `FileDocumentPublishedEvent(documentId, moduleCode, ownerType, ownerId, fileName, contentType, visibility)` plus the `DomainEvent` tenant / actor / realm. Fired on upload (`PRIVATE`) and whenever a document is set PUBLIC (including a re-publish and an ARCHIVED document); not on withdraw, archive or delete. Published inside the transaction (an `AFTER_COMMIT` listener sees it after commit, never on rollback). No consumer in erp-core. | com/erp/events/FileDocumentPublishedEvent.java:11-36; FileService.java:158-160, 337-342; com/erp/events/DomainEventPublisher.java:11-13; DEVIATIONS [08] |
| NEW | consumed | MDL: `MdlLookupApi.readActiveValuesByKey`, `LookupOptionView` through `FileLookupService` — contradicts A5 ("no central lookup") and the module registry's "File Service calls NO other module". | FileLookupService.java:7-8, 48-57; ADR-FILE-005 |
| NEW | consumed | tenant: `CORE_TENANT` FK on every `TENANT_ID`, `TenantContext.require()` (storage target), `TenantLookupApi.codeOf(tenantId)` (public URL), path tenant resolution `erp.core.tenant.path-tenant-paths`. | FileService.java:152; PublicFileUrls.java:34, 59; ErpCoreProperties.java:252-261; DEVIATIONS [07] |
| NEW | consumed | SEC: permission SPI `PermissionContributor` (`FilePermissions`); `SecurityContextHelper.getCurrentUsername()` for the token binding — XM-FILE-001 (SOFT-READ of the identity) unchanged. | FilePermissions.java:3-6, 17; FileService.java:187, 205 |
| NEW | consumed | audit (`@Audited`), events (`DomainEventPublisher`), common (`PageableBuilder`, `SpecBuilder`, `TokenHasher`, `OwnedLookups`, `StatusTransitions`, `DomainRules`). | FileService.java:3-13; DEVIATIONS [10] |
| NEW | configuration | `erp.core.files.access-token-secret` (required, no default); `max-content-bytes` (5242880); `max-request-bytes` (10485760); `storage` (DB); `public-base-url`; `local.root`; `s3.bucket`; `s3.region` (us-east-1); `s3.endpoint` (path-style when set); `s3.access-key` / `s3.secret-key` (static credentials, else the AWS default chain); `s3.public-base-url`. `erp.core.tenant.path-tenant-paths` (default `/api/v1/public/files/{tenantCode}/**`). Host application: `spring.servlet.multipart.max-file-size` / `max-request-size` ≥ the FILE limits; `software.amazon.awssdk:s3` on the classpath for S3; a Redis template for a shared token store. | ErpCoreProperties.java:149-221, 252-261; FileStorageAutoConfiguration.java:58-87, 135-145; DEVIATIONS [07] |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.4 — shared image store (profile photos now, tenant logos in package E)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Ids continue from the highest number ever issued for FILE (tree, this repository's history and
`governance-shared`): RULE-FILE-007, XM-FILE-001, API-FILE-008; FILE ADRs 001..007 are the as-built decisions of the analysis-coverage work (plan ADR-FILE-001 therefore becomes ADR-FILE-008). This addendum adds
XM-FILE-002, RULE-FILE-008..010 and ADR-FILE-008. **No endpoint, no schema change, no migration.**

### 1. Cross-module surface (A7) — NEW / CHANGED
| Kind | Id | Interface | Method | Contract |
|---|---|---|---|---|
| NEW | XM-FILE-002 | `com.erp.file.crossmodule.FileImageStoreApi` | `ImageStoreResult storePublicImage(ImageStoreRequest request)` | `ImageStoreRequest { ownerType, ownerId, moduleCode, content (byte[]), baseName, maxBytes, allowedTypes (Set<String>) }` (`baseName`, e.g. `photo` / `logo`; the stored file name is `<baseName>.<ext of the detected type>` — the client's file name is never kept, review round 1). Validates (RULE-FILE-008/009) and answers a **result, never a validation exception**: `ImageStoreResult.stored(StoredImage { documentId, publicUrl, contentType, size })` or `ImageStoreResult.rejected(ImageRejection)` with `ImageRejection` = `EMPTY`, `TOO_LARGE`, `TYPE_NOT_ALLOWED`, `UNSAFE_SVG`. The caller turns a rejection into its own code (SEC `SEC-400-PHOTO-INVALID`; package E `TENANT_LOGO_INVALID`). Stored in the **current tenant** (E wraps the call in `TenantContext.callAs`). Gate: `isAuthenticated()`; the consuming service carries its own permission. |
| NEW | XM-FILE-002 | same | `void discard(Long documentId)` | Withdraws an image: the document becomes `DELETED` and `PRIVATE` (slug dropped, so its public URL answers 404 at once); bytes retained (RULE-FILE-006). Idempotent: an unknown or already deleted id is a no-op. |
| NEW | XM-FILE-002 | same | constants `TYPE_PNG`, `TYPE_JPEG`, `TYPE_WEBP`, `TYPE_SVG` | the four content types an image request may allow (`image/png`, `image/jpeg`, `image/webp`, `image/svg+xml`) |
| CHANGED | XM-FILE-001 | `FileDocumentLookupApi` | + `Map<Long, String> publicUrls(Collection<Long> documentIds)` | the public URLs of several documents of the current tenant in one query (ids without a servable public document are absent from the map); `publicUrl(Long)` unchanged. Used for user lists (one query per page, not per row). |

Plan deltas (plan §6 D.4 names are proposals): the plan's `StoredImage storePublicImage(...)` answers
`ImageStoreResult` wrapping `StoredImage`, because a rejection must reach the caller as a value (plan:
"from a FILE validation result, never a raw exception"); the request carries no `contentType` — the
type is detected from the bytes and a declared type is never trusted (RULE-FILE-002's principle), so a
field the store would ignore is left out.

### 2. Business rules (A4) — NEW
| RULE-ID | Scope | Trigger | Statement | Message-AR | Message-EN | Source |
|---|---|---|---|---|---|---|
| RULE-FILE-008 | FILE-001 (image store) | `storePublicImage` | The type is detected from the content only: PNG (`89 50 4E 47 0D 0A 1A 0A`), JPEG (`FF D8 FF`), WebP (`RIFF` … `WEBP` at bytes 8–11), SVG (UTF-8 text whose first element, after an optional BOM, XML declaration, comments and whitespace, is `<svg`). The detected type must be in the request's `allowedTypes` (else `TYPE_NOT_ALLOWED` — anything undetected included); the size must be 1..`maxBytes` (`EMPTY` / `TOO_LARGE`). The client file name and declared type are never used for the decision; the stored `CONTENT_TYPE` is the detected one. | — (the caller's code carries the message) | — | plan §6 D.4 |
| RULE-FILE-009 | FILE-001 (image store) | `storePublicImage` of an SVG | SVG is accepted only when the request allows `image/svg+xml` (logos; photos never do) **and** every item below holds (allow-list since review round 1, completed in round 2; anything else → `UNSAFE_SVG`; rejected, never rewritten): (1) **encoding** — strict UTF-8, no other declared encoding; (2) **parse** — hardened namespace-aware DOM parse: DOCTYPE refused, no external/parameter entities, no external DTD, no XInclude, no entity expansion, secure processing; (3) **document level** — comments and exactly one root element `<svg>` in the SVG namespace; no processing instruction anywhere (prolog, inside, after the root); (4) **elements** — SVG namespace only, from a fixed static-drawing set (`svg g defs symbol use title desc style path rect circle ellipse line polyline polygon text tspan textPath linearGradient radialGradient stop clipPath mask pattern marker filter` and the `fe*` primitives `feBlend feColorMatrix feComponentTransfer feFuncR/G/B/A feComposite feDropShadow feFlood feGaussianBlur feMerge feMergeNode feMorphology feOffset feTile`); never `script`, `foreignObject`, `a`, `image`, `feImage`, animation (`animate`, `set`, …), `switch`, `metadata` or any element of another namespace (editor metadata such as `sodipodi:*` / `inkscape:*` is refused); nesting depth ≤ 64; (5) **attributes** — a fixed presentation/geometry list; inert `data-*` and `title` (free text: no `<`, `javascript:`, `//`); `xml:space`, `xml:lang` and namespace declarations; never `on…`, `attributeName`, `src`, `xml:base` or any other foreign-namespace attribute; (6) **references** — `href` / `xlink:href` only a local `#name` (letters, digits, `_ - . :`); (7) **CSS** — every other attribute value, the `style` attribute and the `<style>` sheet (text and CDATA only, concatenated; no comment, element or PI inside `<style>`), checked as written and with `/*…*/` removed: none of `\`, `<`, `//` (every absolute or protocol-relative URL), `javascript:`, `vbscript:`, `data:`, `expression(`, `behavior`, `-moz-binding`; no at-rule except `@media`; no function outside a fixed non-fetching list (colours, `calc`/`min`/`max`/`clamp`/`var`, gradients, transforms, filter functions, `cubic-bezier`/`steps`, `url`) — so `image-set`, `-webkit-image-set`, `image`, `src`, `cross-fade`, `element`, `paint`, `local`, `@font-face`, `@import`, `@namespace` are refused; every `url(` is `url(#…)`; (8) **renderer limits** — at most 100 `<use>` elements, and no `<use>` may reference a `<use>` or a subtree containing one (no nested references: a 10-deep chain would render 10^12 instances). Logos must therefore be plain / optimised SVG (SVGO, Inkscape "Optimized SVG" or "Plain SVG" without metadata, Figma or Illustrator export). | — | — | plan §6 D.4; `ImageValidationDomainService`, `SvgAllowList` |
| RULE-FILE-010 | FILE-001 (image store) | `storePublicImage` / `discard`; public GET | An image-store document has **no category**, is named `<baseName>.<png|jpg|webp|svg>` from the detected type (base reduced to `[a-z0-9_-]`, default `image`), is stored `ACTIVE`, file type `IMAGE`, through the active storage provider, and is `PUBLIC` at once with a fresh random slug (`FileDocumentPublishedEvent` with `PUBLIC`). The public path serves a PUBLIC, ACTIVE document whose category allows public files **or that has no category**; only the image store creates PUBLIC documents without a category (`PATCH /visibility` keeps refusing them, `FILE_PUBLIC_NOT_ALLOWED`). `discard` = `DELETED` + `PRIVATE`. | — | — | ADR-FILE-008 |

### 3. Public serving of images — facts and open points
| Kind | Item |
|---|---|
| fact | `image/png`, `image/jpeg`, `image/webp` are in step 07's inline allow-list (`FileDocumentDomain.INLINE_SAFE_CONTENT_TYPES`): a photo URL renders inline in a browser tab and in `<img>`. Headers as for every public file: `Cache-Control: max-age=86400, public`, `ETag`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy: sandbox; default-src 'none'`. |
| fact | A replacement gets a new slug (new URL), so the day-long cache never shows a stale photo; a discarded URL answers 404 `FILE_DOCUMENT_NOT_FOUND` from the origin at once. |
| OPEN (for package E) | `image/svg+xml` is **not** inline-safe (deliberately, step 07): an SVG logo is served `attachment` (with `nosniff` and the sandbox CSP). `<img src="…">` renders it (the disposition only affects navigation, and an SVG in `<img>` runs no script), but opening the URL downloads it. E keeps the allow-list unchanged and relies on `<img>`; adding SVG to the inline list would be a FILE decision of its own (new ADR, 009+). |
| NOTE (for package E) | Logos must be plain / optimised SVG (SVGO, Inkscape "Optimized SVG" or "Plain SVG" without metadata, Figma or Illustrator export): RULE-FILE-009 refuses editor metadata (`<metadata>`, `sodipodi:*`, `inkscape:*`), DOCTYPE, processing instructions and nested `<use>`; the logo endpoint should say so in its error text. |
| RESOLVED | No `ALLOW_PUBLIC` category is needed or used: image-store documents are uncategorised (RULE-FILE-010, ADR-FILE-008). |

### 4. Endpoints, error codes, permissions, entities
No new FILE endpoint, error code, permission, entity field or LOV. The public GET
(`/api/v1/public/files/{tenantCode}/{publicSlug}`) now also serves uncategorised PUBLIC documents
(RULE-FILE-010). `GET /api/v1/files/{id}` and the owner list show their `publicUrl` like any servable
public document.

### 5. Consumers
| Consumer | Uses | Owner type / module | Limits |
|---|---|---|---|
| SEC (package D) | `storePublicImage`, `discard`, `publicUrl`, `publicUrls` | `SEC_USER` / user id / `SEC` | PNG, JPEG, WebP; 1 MB (SEC RULE-SEC-061) |
| TENANT (package E, planned) | `storePublicImage` inside `TenantContext.callAs(tenantId)`, `discard` | `CORE_TENANT` / tenant id / `TENANT` | PNG, JPEG, WebP, SVG; 1 MB (plan §7 E.1/E.3) |

### 6. Decisions
| Kind | ADR | Decision |
|---|---|---|
| NEW | ADR-FILE-008 | Profile photos and logos are PUBLIC documents with non-guessable slugs; image-store documents carry no category — `governance/analysis/decisions/FILE/ADR-FILE-008.md` |

### 7. Package B — usage counts for the platform (tenant-maturity plan §4 B.4)
Change         : tenant-maturity plan package B — `FileDocumentLookupApi` counts, consumed by TENANT's `GET /api/v1/platform/tenants/{id}/usage`
Statement      : Sections 1–6 above (package D.4) are unchanged; §7 records package B's implemented delta.

No FILE id is minted (no rule: the counts decide nothing; XM-FILE-001 is the existing surface). No endpoint,
entity field, error code, permission or migration changes.

| Kind | Id | Interface | Method | Contract |
|---|---|---|---|---|
| CHANGED | XM-FILE-001 | `com.erp.file.crossmodule.FileDocumentLookupApi` | + `long countDocuments()` | the current tenant's `FILE_DOCUMENT` rows whose status is not `DELETED` (ACTIVE, ARCHIVED), any visibility, with or without a category (image-store documents included) |
| CHANGED | XM-FILE-001 | same | + `long sumBytes()` | the sum of `FILE_SIZE` over the same rows (0 when none; a NULL size counts 0) — bytes the tenant's live documents hold, whatever the storage provider |
Read-only, tenant-filtered by the `@TenantId` discriminator (JPQL), no `@PreAuthorize` like the other lookup
methods (they reveal no content; the consuming service carries its own gate — TENANT: `PLATFORM_TENANT_MANAGE`).
Consumer: TENANT (`TenantService.getUsage`), inside `TenantContext.callAs(tenantId)`.

### 8. Package E — tenant logos; SVG duplicate ids (tenant-maturity plan §7)
Change         : tenant-maturity plan package E — TENANT consumes the image store for logos; RULE-FILE-009 refuses duplicate `id` values (carry-over of package D's review round 3)
Statement      : Sections 1–7 above are unchanged; §8 records package E's implemented delta.

No FILE id is minted. No endpoint, entity field, error code, permission or migration changes.

| Kind | Id | Delta | Source |
|---|---|---|---|
| CHANGED | RULE-FILE-009 (item 8, renderer limits) | + **no two elements may carry the same `id`**: an SVG with a duplicate `id` value is refused (`UNSAFE_SVG`). The nested-`<use>` guard resolves a `<use>` reference by `id`; browsers resolve a duplicated `id` to the **first** element in tree order, while a map filled during the walk kept the **last** one, so a flat decoy placed after the real target hid a nested `<use>` chain (91 `<use>` elements rendering 10^9 instances). Refusing duplicates makes the guard's resolution unambiguous; optimisers (SVGO) and the design tools' exports emit unique ids. Item (8) now reads: at most 100 `<use>` elements; no `<use>` may reference a `<use>` or a subtree containing one; every `id` unique. | package D review round 3 (LOW, carried over to E); `SvgAllowList.Walk` (`putIfAbsent`); `ImageValidationDomainServiceTest.duplicateIds_areRefused_soNoDecoyHidesANestedUseChain` |
| CHANGED | §5 Consumers, TENANT row | implemented: `TenantService.setLogo` / `removeLogo` call `storePublicImage` (`CORE_TENANT` / tenant id / `TENANT`, base name `logo`, 1 048 576 bytes, PNG / JPEG / WebP / SVG) and `discard` inside `TenantContext.callAs(tenantId)` in one transaction of that tenant; `TenantLogoUrls` calls `FileDocumentLookupApi.publicUrl` inside the same tenant (TENANT XM-TENANT-003, REQ-TENANT-029) | TENANT srs-tenant.md 1.3.0 E6 |
| RESOLVED | §3 "OPEN (for package E)" (SVG not inline-safe) | kept as is, no FILE ADR: an SVG logo stays `attachment` + `nosniff` + sandbox CSP and is shown only through `<img>` (TENANT srs-tenant.md 1.3.0 E7); the inline list is unchanged | TENANT E7 |
| RESOLVED | §3 "NOTE (for package E)" (plain / optimised SVG) | the logo error `TENANT_LOGO_INVALID` says so in both languages (TENANT RULE-TENANT-018) | TENANT E4 |

### 9. Package C5 — the private store of a tenant export and FILE's export contributor (tenant-maturity plan §5 C.5)
Change         : tenant-maturity plan package C5 — NEW `FilePrivateStoreApi` (store a server-generated file as a PRIVATE document of the current tenant; issue FILE's single-use download token for it), consumed by TENANT's `POST /api/v1/platform/tenants/{id}/export`; FILE's `TenantExportContributor` writes the tenant's categories and document metadata
Statement      : Sections 1–8 above are unchanged; §9 records package C5's implemented deltas.

Ids continue from the highest ever issued for FILE (tree, this repository's history and `governance-shared`):
RULE-FILE-010, XM-FILE-002, API-FILE-008, ADR-FILE-008. This section adds **XM-FILE-003 and RULE-FILE-011**. No
endpoint, entity field, error code, permission or migration changes; no FILE ADR (the choices are TENANT's
ADR-TENANT-006).

#### 9.1 Cross-module surface (A7) — NEW
| Kind | Id | Interface | Method | Contract |
|---|---|---|---|---|
| NEW | XM-FILE-003 | `com.erp.file.crossmodule.FilePrivateStoreApi` | `StoredPrivateFile storePrivateFile(PrivateFileStoreRequest request)` | `PrivateFileStoreRequest { ownerType, ownerId, moduleCode, fileName, contentType, content (java.nio.file.Path of a readable local file) }`. Stores the file's bytes through the active storage provider as a `FILE_DOCUMENT` of the **current tenant** (RULE-FILE-011) and answers `StoredPrivateFile { documentId, fileName, size, contentHash }`. Joins the caller's transaction (the row and the `DB` provider's bytes commit with it; a `LOCAL` / `S3` object of a rolled-back transaction is removed, the step 07 rule). Gate `isAuthenticated()`; the consuming service carries its own permission (TENANT: `PLATFORM_TENANT_MANAGE`). A missing owner → 400 `FILE_DOCUMENT_OWNERSHIP_REQUIRED` (RULE-FILE-005, a caller bug); an unreadable file → 500 `FILE_STORAGE_UNAVAILABLE`. |
| NEW | XM-FILE-003 | same | `DownloadGrant issueDownloadToken(Long documentId)` | FILE's existing single-use download token (API-FILE-002's mechanism, RULE-FILE-003) for a document of the current tenant, bound to the calling username: `DownloadGrant { token, expiresAt }` (10 minutes). Unknown, foreign-tenant or DELETED → 404 `FILE_DOCUMENT_NOT_FOUND`. Gate `isAuthenticated()` (no `PERM_FILE_BROWSER_VIEW`: the consumer's permission covers it). Downloaded with the unchanged `GET /api/v1/files/download?token=` (API-FILE-003). |

#### 9.2 Business rules (A4) — NEW
| RULE-ID | Scope | Trigger | Statement | Source |
|---|---|---|---|---|
| RULE-FILE-011 | FILE-001 (private store) | `storePrivateFile` / `issueDownloadToken` | A privately stored file is a `FILE_DOCUMENT` of the current tenant with the request's owner (`OWNER_TYPE`, `OWNER_ID`, `MODULE_CODE`), **no category**, `VISIBILITY = PRIVATE` (never published: no slug, not reachable on the public path, `PATCH /visibility` keeps refusing an uncategorised document), `FILE_STATUS_ID = ACTIVE`, `FILE_TYPE_ID` derived from the declared content type like an upload (`application/zip` → `ARCHIVE`), `FILE_NAME` = the request's name (default `file`), `FILE_SIZE` = the file's length, `CONTENT_HASH` = its SHA-256 (hex), `STORAGE_PROVIDER` = the active provider; `FileDocumentPublishedEvent` with `PRIVATE` like an upload. The upload limits (`erp.core.files.max-content-bytes`, `max-request-bytes`, category limits) do **not** apply — the file is generated by the server and bounded by its producer (TENANT: `erp.core.tenant.export.max-rows`); nor does the content sniffing (the producer declares the type). Its download token is the upload's: AES-GCM, 10 minutes, single-use, bound to the issuing **username** (the download refuses another username with 401 `FILE_ACCESS_TOKEN_INVALID` without consuming it) and resolved in the **caller's tenant** (a same-named user of another tenant gets 404 `FILE_DOCUMENT_NOT_FOUND`). The document is otherwise an ordinary private document: listed, re-tokened (`POST /{id}/access-token`), archived or deleted through the FILE API by users of its tenant holding the FILE permissions. | TENANT REQ-TENANT-037, RULE-TENANT-027; ADR-TENANT-006 |

#### 9.3 FILE's export contributor (TENANT XM-TENANT-004) — NEW
`com.erp.file.tenant.FileTenantExportContributor` (`moduleCode` `FILE`) writes `FILE/FILE_CATEGORY.csv` and
`FILE/FILE_DOCUMENT.csv` of the exported tenant with plain SQL naming `TENANT_ID` (RULE-TENANT-011), ordered by `ID`:
**metadata only** — never `FILE_CONTENT` (the bytes), `STORAGE_REF` (the provider-internal location) or `PUBLIC_SLUG`
(the capability of a public URL). Exact column lists: TENANT `../../TENANT/P1/srs-tenant.md` 1.3.0 X7.

#### 9.4 Consumers
| Consumer | Uses | Owner type / module | Limits |
|---|---|---|---|
| TENANT (package C5) | `storePrivateFile`, `issueDownloadToken` inside the PLATFORM request (current tenant PLATFORM) | `CORE_TENANT` / exported tenant id / `TENANT`, `application/zip`, `tenant-export-{CODE}-{instant}.zip` | `erp.core.tenant.export.max-rows` |

#### 9.5 Review round 1 (package C5) — restricted documents; their deletion removes the bytes
Ids continue from RULE-FILE-011. This adds **RULE-FILE-012**, the nullable column `FILE_DOCUMENT.REQUIRED_AUTHORITY`
(`V22__file_document_required_authority.sql`, `../P2/db-script.md` 1.3.0 package C5) and changes XM-FILE-001,
XM-FILE-003, RULE-FILE-006 (for restricted documents only) and the `@Audited` configuration of ENTITY-FILE-001. No
endpoint, error code or permission is added.

| RULE-ID | Scope | Trigger | Statement | Source |
|---|---|---|---|---|
| RULE-FILE-012 | FILE-001 (restricted documents) | owner list, metadata, access token, download, visibility, archive / delete | A document whose `REQUIRED_AUTHORITY` is set (only the private store sets it, from `PrivateFileStoreRequest.requiredAuthority`; immutable) is served only to a caller holding that authority, **in addition** to the endpoint's own FILE permission: the owner list (`GET /api/v1/files`) leaves it out (the query keeps `REQUIRED_AUTHORITY IS NULL OR REQUIRED_AUTHORITY IN (the caller's authorities)`, so paging and totals stay exact), and `GET /{id}`, `POST /{id}/access-token`, `GET /download?token=`, `PATCH /{id}/visibility` and `DELETE /{id}` answer **404** `FILE_DOCUMENT_NOT_FOUND` to anyone else (never 403: the document's existence is not revealed). `FilePrivateStoreApi.issueDownloadToken` applies the same check. `FileDocumentLookupApi.isAvailable` answers `false` for it (no module may reference it). **Deletion** (`DELETE /{id}?action=DELETE`) of a restricted document removes its content through its storage provider — `DB`: `FILE_CONTENT` set to NULL in the deleting transaction; `LOCAL` / `S3`: the object is deleted after the commit — and keeps the row as a `DELETED` tombstone (owner, name, type, size, hash); `ARCHIVE` keeps the content. A document without `REQUIRED_AUTHORITY` behaves exactly as before (soft delete keeps the bytes, RULE-FILE-006). | TENANT RULE-TENANT-027 (review round 1); ADR-TENANT-006 |

| Kind | Id / item | Delta |
|---|---|---|
| CHANGED | XM-FILE-003 `PrivateFileStoreRequest` | + `requiredAuthority` (nullable; TENANT passes `PLATFORM_TENANT_MANAGE`), stored in `REQUIRED_AUTHORITY` (RULE-FILE-012). `issueDownloadToken` answers 404 to a caller without it. |
| CHANGED | XM-FILE-001 `FileDocumentLookupApi.isAvailable` | `false` for a restricted document. `countDocuments` / `sumBytes` unchanged (tenant usage still counts every live document). |
| CHANGED | RULE-FILE-006 | soft delete keeps the bytes **except** for a restricted document (RULE-FILE-012). |
| CHANGED | ENTITY-FILE-001 `@Audited` | `@Audited(entityType = "FILE_DOCUMENT", ignore = {"storageRef", "publicSlug"})`: a storage reference (an object key on `LOCAL` / `S3`) and a public slug (the capability of a public URL) are no longer written to `CORE_AUDIT_EVENT.CHANGES`; rows written before keep them (TENANT's AUDIT contributor removes them from an export, srs-tenant.md X14). `requiredAuthority` is recorded (not sensitive). |
| CHANGED | `FileMetadataView` / `FileDocumentRepository.METADATA_SELECT` | + `requiredAuthority`; the owner list takes the caller's authorities. |

### 10. Closure (tenant-maturity Z) — known limitation of RULE-FILE-012
Change         : tenant-maturity plan closure step Z — a limitation of the implemented RULE-FILE-012 recorded when the merged packages were checked together; no code change
Statement      : Sections 1–9 above are unchanged; §10 records a known limitation, no new id.

| Kind | Id | Delta | Source |
|---|---|---|---|
| NOTE (known limitation) | RULE-FILE-012 — deleting a restricted document stored on `LOCAL` / `S3` | The object is deleted by an after-commit callback (`FileService.purgeContent`). When the provider's delete fails (an `IOException` on `LOCAL`, an `SdkException` on `S3`), the provider **only logs a WARN**: the row is already the committed `DELETED` tombstone, the caller gets its normal answer, and the **object stays in storage** — nothing retries it. `DB` is not affected (the column is cleared inside the deleting transaction). Follow-up, together with the export-archive retention job (TENANT srs-tenant.md X10): a sweeper that deletes again the content of `DELETED` restricted documents. | `FileService.java:406-424`; `LocalFsStorageProvider.java:82-88`; `S3StorageProvider.java:78-84` |

### Refactors without behaviour change — إعادة هيكلة دون تغيير سلوك
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no FILE behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | `FileCategoryDomain` | RULE-FILE-007 decisions go through `com.erp.common.domain.DomainRules.assertUnique` (same code, same 409). | git diff v1.2.0..main — com/erp/file/domain/FileCategoryDomain.java |
| CHANGED | `FileDocumentDomain` | The A6 state machine is a `com.erp.common.domain.StatusTransitions` (same transitions, same 422 `FILE_DOCUMENT_INVALID_TRANSITION`). | git diff v1.2.0..main — com/erp/file/domain/FileDocumentDomain.java |
| CHANGED | `FileLookupService`, API-FILE-008 | Key normalisation and the MDL not-found translation go through `com.erp.common.lookup.OwnedLookups`; the response type is `com.erp.common.lookup.LookupOptionResponse` (same JSON shape `code`, `labelAr`, `labelEn`); `com.erp.file.dto.LookupOptionResponse` deleted. | git diff v1.2.0..main — FileLookupService.java, FileLookupController.java; CHANGELOG [Unreleased] |
| CHANGED | `FileService` | The content hash and the token-store key use `com.erp.common.util.TokenHasher.sha256Hex` (`FileService.sha256Hex` removed; the generated contract's call-walk list still names it until `docs/api-docs/file/` is regenerated). | git diff v1.2.0..main — FileService.java |
