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
Steps          : 02, 03, 05, 06, 07, 08, 10
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. Endpoint paths and methods are
taken from `docs/api-docs/file/`. No RULE / API / ENTITY ids are minted; items are labelled
NEW / CHANGED / REMOVED.

### 1. Endpoints
| Kind | Method | Path | Access | Notes | Source |
|---|---|---|---|---|---|
| NEW | PATCH | `/api/v1/files/{id}/visibility` | `FILE:DOCUMENT:PUBLISH` | body `visibility` = PUBLIC / PRIVATE; 409 `FILE_PUBLIC_NOT_ALLOWED` when the category does not allow public files. The step file named `/api/v1/file/documents/{id}/visibility`; implemented under the existing `/api/v1/files` prefix. | docs/api-docs/file/endpoints/file-documents.md; DEVIATIONS [07] |
| NEW | GET (and HEAD) | `/api/v1/public/files/{tenantCode}/{publicSlug}` | none (customer / public chain; tenant from the path) | 200 stream or 302 to the provider URL; other methods: 401 anonymous, 405 customer | docs/api-docs/file/endpoints/public-files.md; DEVIATIONS [07] |
| CHANGED | POST | `/api/v1/files` (API-FILE-001) | `PERM_FILE_BROWSER_CREATE` | stores through the selected provider; records `STORAGE_PROVIDER`, `STORAGE_REF`, `CONTENT_HASH`; publishes `FileDocumentPublishedEvent` (PRIVATE) | docs/steps/07-report.md; DEVIATIONS [08] |
| CHANGED | GET | `/api/v1/files/download?token=` (API-FILE-003) | token | streams from the provider recorded on the document; the token is still consumed only after the content was opened | DEVIATIONS [07] |
| CHANGED | GET | `/api/v1/files/{id}`, `/api/v1/files?ownerId&ownerType&moduleCode` | as before | metadata gains `storageProvider`, `visibility`, `publicUrl` (only for a servable public document) | DEVIATIONS [07] |
| CHANGED | POST / PUT / GET | `/api/v1/files/categories` and sub-paths (API-FILE-007) | as before | + `allowPublic` (update: null keeps the value) | DEVIATIONS [07] |

### 2. Business rules
| Kind | Rule | Source |
|---|---|---|
| NEW | Storage provider selection: `erp.core.files.storage` (DB default, LOCAL, S3) decides new uploads; reads use the document's `STORAGE_PROVIDER`; a provider not configured → 500 `FILE_STORAGE_UNAVAILABLE`. Startup fails for an unknown value, LOCAL without an existing writable root, S3 without bucket or SDK. | DEVIATIONS [07] |
| NEW | LOCAL / S3 key layout `tenantId/category/yyyy/MM/documentId_filename`; segments sanitised; references must resolve inside the root (no path traversal); the object of a rolled-back upload is deleted. | docs/steps/07-report.md; DEVIATIONS [07] |
| NEW | Publish rule: PUBLIC requires a category with `ALLOW_PUBLIC = TRUE` (`FileDocumentDomain.assertCanBePublic`); PUBLIC if and only if a slug is present (DB CHECK); slug = 24 random bytes, base64url, unique per tenant. | DEVIATIONS [07] |
| NEW | Public serving: only PUBLIC + ACTIVE + category still `ALLOW_PUBLIC`, otherwise 404 `FILE_DOCUMENT_NOT_FOUND`. Headers: `Cache-Control: max-age=86400, public`, `ETag: "<sha256>"`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy: sandbox; default-src 'none'`. `inline` only for `image/png`, `image/jpeg`, `image/gif`, `image/webp`, `image/avif`, `image/bmp`, `application/pdf` (SVG excluded); everything else `attachment`. No `If-None-Match` / 304 handling. | DEVIATIONS [07] (public GET, review round 1) |
| NEW | Public URL: relative `/api/v1/public/files/{tenantCode}/{slug}`, absolute when `erp.core.files.public-base-url` is set, or the provider's direct URL (S3 with `s3.public-base-url`). On that path the path tenant always wins over token and header; a token of another tenant is ignored. | DEVIATIONS [07] |
| NEW (operational constraint) | With S3 `public-base-url`, private and public objects share one bucket and carry no ACL: expose only a public prefix on the CDN; withdrawing a file does not revoke a direct S3 / CDN URL already handed out. | DEVIATIONS [07] (review round 1, S3 risk) |
| CHANGED | RULE-FILE-001 limits configurable via `erp.core.files.max-content-bytes` / `max-request-bytes` (same defaults). | DEVIATIONS [03] |
| CHANGED | RULE-FILE-003: single-use token store `DownloadTokenStore` — in-memory by default, Redis when present. | DEVIATIONS [02], [03] |
| NEW | Tenant confinement of all FILE rows; RULE-FILE-007 category code unique per tenant. | docs/steps/05-report.md |
| NEW | `@Audited` on documents and categories (`contentHash` never recorded). | DEVIATIONS [10] |

### 3. Error codes
| Kind | Code | HTTP | Raised when | Source |
|---|---|---|---|---|
| NEW | `FILE_PUBLIC_NOT_ALLOWED` | 409 | publish in a category without `ALLOW_PUBLIC` | docs/api-docs/file/index.md |
| NEW | `FILE_STORAGE_UNAVAILABLE` | 500 | the provider holding the content is not configured or failed (`Status` has no 503) | same; DEVIATIONS [07] |
| NEW (common) | `CONCURRENT_MODIFICATION` | 409 | optimistic-lock conflict | DEVIATIONS [05] |
| NEW (common, 1.2.0) | `NOT_FOUND` | 404 | unknown path (was 500) | CHANGELOG [1.2.0]; DEVIATIONS [15] |
| NEW (common, 1.2.0) | `VALIDATION_ERROR` | 400 | search `page` whose offset overflows `int` (`fieldErrors[0].field = page`; was 500) | CHANGELOG [1.2.0]; DEVIATIONS [15] |
All analysed `FILE_*` codes are unchanged.

### 4. Permissions (exact authority strings)
| Kind | Authority | Source |
|---|---|---|
| unchanged (code-defined in `FilePermissions`) | `PERM_FILE_CATEGORIES_VIEW/CREATE/UPDATE/DELETE`, `PERM_FILE_BROWSER_VIEW/CREATE/UPDATE/DELETE` | erp-core/src/main/java/com/erp/file/permission/FilePermissions.java |
| NEW | `FILE:DOCUMENT:PUBLISH` (screen `FILE_BROWSER`, action `PUBLISH`; effective with the `PERM_FILE_BROWSER_VIEW` gateway; seeded for PLATFORM's `SYS_ADMIN` and `FILE_ADMIN`) | V12 §3; DEVIATIONS [07] |
| NEW (no catalog entry) | public file GET: unauthenticated | DEVIATIONS [07] |

### 5. Entities, fields, LOVs
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | ENTITY-FILE-001 FileDocument | + `storageProvider` (DB / LOCAL / S3), `storageRef`, `visibility` (PRIVATE / PUBLIC, default PRIVATE), `publicSlug`, `contentHash`, `tenantId`, `version`; `fileContent` no longer required (DB provider only). Analysis said `fileContent` BYTEA required; implemented nullable because LOCAL / S3 keep the bytes outside the row (step 07). | V12; V10 |
| CHANGED | ENTITY-FILE-002 FileCategory | + `allowPublic` (default false), `tenantId`, `version` | V12; V10 |
| unchanged | LOV-FILE-001 / 002 | — | — |

### 6. Dependencies — deltas
tenant (`CORE_TENANT` FK, `TenantLookupApi`, path tenant resolution); events (`FileDocumentPublishedEvent`);
audit (`@Audited`). — DEVIATIONS [07], [08], [10]

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
