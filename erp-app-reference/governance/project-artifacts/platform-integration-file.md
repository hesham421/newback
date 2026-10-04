# المرجع: خدمة الملفات FILE (`com.erp.file`)

> **الحالة:** مُنفَّذ — كل مراحل الـ backend الثماني `COMPLETE` في
> `governance/shared/backend/modules/FILE/execution-state.json`؛ اختبارات الـ backend `PENDING`.
> وثائق الـ API المولّدة: `governance/shared/backend/modules/FILE/api-docs/`.
> موديول **Legacy Path** (انظر CLAUDE.md).
> **المصدر:** مستخرج من الكود بتاريخ 2026-10-01 — عند التعارض، الكود هو المرجع.
> **الفهرس:** [PLATFORM-MODULES-INTEGRATION-INDEX.md](PLATFORM-MODULES-INTEGRATION-INDEX.md)

FILE يخزّن ملفات مرتبطة بأي سجل أعمال في أي موديول (ربط مرن polymorphic)، ويتحقق من الحجم والنوع، ويُنزّلها عبر توكن مؤقت لمرة واحدة.

---

## 1. طريقة التكامل — الخلاصة

| السؤال | الجواب |
|---|---|
| هل توجد واجهة Java عبر الموديولات؟ | نعم، للتحقق فقط: `com.erp.file.crossmodule.FileDocumentLookupApi.isAvailable(Long fileId)`. الرفع والتنزيل عبر REST (من الواجهة الأمامية عادةً) |
| كيف أربط ملفاً بسجلّي؟ | ارفعه بـ `ownerId` = معرّف السجل، `ownerType` = نوع السجل، `moduleCode` = كود موديولك |
| كيف أعرض ملفات سجلّ؟ | `GET /api/v1/files?ownerId=&ownerType=&moduleCode=` |
| هل أخزّن `fileId` في جدولي؟ | اختياري — عمود `Long` بدون FK، يُتحقق منه بـ `FileDocumentLookupApi` (نمط `NOTIF_TEMPLATE.ATTACHMENT_FILE_ID`) |
| أين تُخزَّن البايتات؟ | في قاعدة البيانات: `FILE_DOCUMENT.FILE_CONTENT` (`BYTEA`) — لا نظام ملفات |
| ما الذي يحتاجه المستخدم؟ | صلاحيات `PERM_FILE_BROWSER_*` (انظر §5) |
| تبعيات وقت التشغيل | PostgreSQL + **Redis** (للتنزيل) |

---

## 2. Endpoints

الحماية على الـ Service. الردود بالغلاف `ApiResponse` ما عدا التنزيل.

### 2.1 المستندات — `/api/v1/files`

| الطريقة والمسار | المدخلات | المخرجات | الصلاحية |
|---|---|---|---|
| `POST /api/v1/files` (`multipart/form-data`) | `file` + `ownerId` (Long, إلزامي)، `ownerType` (≤100، إلزامي)، `moduleCode` (≤50، إلزامي)، `fileCategoryFk` (Long، اختياري) | `FileMetadataResponse`، 201 | `PERM_FILE_BROWSER_CREATE` |
| `POST /{id}/access-token` | — | `{accessToken, expiresAt}` | `PERM_FILE_BROWSER_VIEW` |
| `GET /download?token=` | `token` | بايتات خام + `Content-Type` + `Content-Disposition: attachment` | `isAuthenticated()` |
| `GET /{id}` | — | `FileMetadataResponse` | `PERM_FILE_BROWSER_VIEW` |
| `GET /api/v1/files` | `ownerId`, `ownerType`, `moduleCode` (إلزامية)، `fileTypeId`, `fileStatusId`، `page`(0), `size`(20), `sort` | `Page<FileMetadataResponse>` | `PERM_FILE_BROWSER_VIEW` |
| `DELETE /{id}?action=ARCHIVE` (الافتراضي) أو `action=DELETE` | — | `FileMetadataResponse`، 200 | ARCHIVE ⇒ `_UPDATE`، DELETE ⇒ `_DELETE` |

`FileMetadataResponse`: `id, ownerId, ownerType, moduleCode, fileName, contentType, fileSize, fileTypeId, fileStatusId, fileCategoryId, createdAt, createdBy, updatedAt, updatedBy` — **لا يحتوي البايتات أبداً**.
الترتيب المسموح في القائمة: `fileName, createdAt, fileSize` (غيرها ⇒ `createdAt`)، والاتجاه دائماً DESC.

### 2.2 التصنيفات — `/api/v1/files/categories`

| الطريقة والمسار | الصلاحية |
|---|---|
| `POST /` — `{categoryCode≤50, nameAr, nameEn, maxSizeBytes?, allowedContentTypes?, isActiveFl?}` → 201 | `PERM_FILE_CATEGORIES_CREATE` |
| `POST /search` (`BaseSearchContractRequest`؛ حقول: `categoryCode, nameAr, nameEn, isActive, createdAt`) | `PERM_FILE_CATEGORIES_VIEW` |
| `GET /{id}` | `PERM_FILE_CATEGORIES_VIEW` |
| `PUT /{id}` — `{nameAr, nameEn, maxSizeBytes, allowedContentTypes}` (الكود غير قابل للتعديل) | `PERM_FILE_CATEGORIES_UPDATE` |
| `DELETE /{id}` → 204 (تعطيل ناعم) | `PERM_FILE_CATEGORIES_DELETE` |

لا يوجد endpoint لإعادة التفعيل.

### 2.3 القوائم — `GET /api/v1/files/lookups/{key}` (`isAuthenticated()`)

`FILE_FILE_TYPE` (IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER) و`FILE_FILE_STATUS` (ACTIVE, ARCHIVED, DELETED) — تُقرأ من MDL. أي مفتاح آخر ⇒ 404 `FILE_LOOKUP_KEY_UNKNOWN`.

---

## 3. مسار الرفع والتحقق

1. حقول الملكية الثلاثة إلزامية ⇒ وإلا 400.
2. التصنيف (اختياري): غير موجود ⇒ 404 `FILE_CATEGORY_NOT_FOUND`؛ معطّل ⇒ 422 `FILE_CATEGORY_INACTIVE`.
3. **النوع:** يُكتشف بـ magic bytes (`URLConnection.guessContentTypeFromStream`) — لا يوجد تحقق بالامتداد.
   إذا كان للتصنيف `allowedContentTypes` (CSV من MIME types) فيجب أن يطابق النوعُ المكتشفُ أحدَها، ونوع غير قابل للاكتشاف يُرفض ⇒ 415 `FILE_DOCUMENT_TYPE_NOT_ALLOWED`.
   بدون allow-list ⇒ لا قيود على النوع.
4. **الحجم:** حدّ التصنيف `maxSizeBytes` إن كان > 0 وإلا **5MB**، بالإضافة إلى سقف ثابت **10MB** ⇒ 413 `FILE_DOCUMENT_SIZE_EXCEEDED`.
   (حدود Spring multipart: 15MB/25MB؛ الحدود الفعلية ثوابت Java وليست إعدادات.)
5. `fileTypeId` يُشتق من النوع، و`fileStatusId = ACTIVE`، ثم الحفظ ⇒ 201.

> ⚠️ `guessContentTypeFromStream` يتعرف على مجموعة محدودة من الأنواع (الصور أساساً). ملفات Office وربما PDF قد تُرفض إن وُضعت في allow-list لتصنيف. **اختبر قبل الاعتماد على allow-list.**

**دورة الحياة:** `ACTIVE → ARCHIVED → DELETED` أو `ACTIVE → DELETED`؛ `DELETED` نهائية. الحذف ناعم (البايتات تبقى، لا يوجد purge).
انتقال غير مسموح ⇒ 422؛ قيمة `action` غير معروفة ⇒ 400 (كلاهما `FILE_DOCUMENT_INVALID_TRANSITION`).

---

## 4. التنزيل عبر التوكن

```
1) POST /api/v1/files/{id}/access-token          (Authorization: Bearer <jwt>)
   → { "accessToken": "…", "expiresAt": "…" }
2) GET  /api/v1/files/download?token=<accessToken> (نفس المستخدم ونفس JWT)
   → بايتات الملف
```

- التوكن مشفّر AES-GCM بمفتاح مشتق من `file.access-token.secret`، صالح **10 دقائق** (ثابت)، **لمرة واحدة**، ومربوط **باسم المستخدم** عبر Redis (`file:dl-token:<sha256>`).
- تلاعب/انتهاء/مستخدم مختلف/استُخدم سابقاً ⇒ 401 `FILE_ACCESS_TOKEN_INVALID`.
- الملف `DELETED` ⇒ 404؛ الملف `ARCHIVED` **ما زال قابلاً للتنزيل**.
- لا يوجد مسار تنزيل عام في `SecurityConfig`، لذا `<a href>` أو `<img src>` المباشران لا يعملان. على الواجهة الأمامية الجلب مع ترويسة Authorization ثم الحفظ كـ blob.

```bash
curl -X POST http://localhost:7272/api/v1/files \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@invoice.png" -F "ownerId=1001" -F "ownerType=XYZ_ORDER" -F "moduleCode=XYZ"
```

---

## 5. خطوات دمج موديول جديد مع FILE

1. **اختر الاصطلاح:** `moduleCode` = كود موديولك في `SEC_MODULE_REG`، و`ownerType` = اسم ثابت للكيان (مثل `XYZ_ORDER`). وثّقهما في خطة موديولك.
   FILE **لا يتحقق** من وجود المالك أو صحة `moduleCode`؛ المسؤولية على موديولك.
2. **الصلاحيات:** أدوار موديولك تحتاج `PERM_FILE_BROWSER_VIEW` و`PERM_FILE_BROWSER_CREATE` (و`_UPDATE`/`_DELETE` للأرشفة والحذف) مع منح Tier-1 على موديول `FILE` وTier-2 على الشاشة `FILE_BROWSER`. انسخ النمط من `V31__cu_notif_file_security_seed.sql`.
3. **التصنيف (اختياري)** لفرض حدود خاصة. لا يوجد seed تصنيفات حالياً، فإما:
   - عبر الـ API `POST /api/v1/files/categories`، أو
   - migration جديد:
     ```sql
     INSERT INTO FILE_CATEGORY (ID, CATEGORY_CODE, NAME_AR, NAME_EN, MAX_SIZE_BYTES, ALLOWED_CONTENT_TYPES,
                                IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
     SELECT nextval('SEQ_FILE_CATEGORY'), 'XYZ_ATTACHMENT', 'مرفقات', 'Xyz attachments',
            5242880, 'image/png,image/jpeg', 1, 'SYSTEM', CURRENT_TIMESTAMP
     WHERE NOT EXISTS (SELECT 1 FROM FILE_CATEGORY WHERE CATEGORY_CODE = 'XYZ_ATTACHMENT');
     ```
     اكتب الكود بأحرف كبيرة (SQL يتجاوز `@PrePersist`). عند الرفع يُمرَّر **الـ id الرقمي** للتصنيف في `fileCategoryFk` (لا يوجد بحث بالكود إلا عبر `/search`).
4. **إن خزّنت `fileId` في جدولك:** عمود `BIGINT` بدون FK، وتحقق منه عند الحفظ:
   ```java
   private final FileDocumentLookupApi fileDocumentLookupApi;   // com.erp.file.crossmodule
   ...
   if (attachmentFileId != null && !fileDocumentLookupApi.isAvailable(attachmentFileId)) {
       throw new LocalizedException(Status.NOT_FOUND, XyzErrorCodes.XYZ_404_ATTACHMENT, attachmentFileId);
   }
   ```
   `isAvailable` يرجع `true` للملف `ACTIVE` أو `ARCHIVED`، و`false` للملف غير الموجود أو `DELETED`. لا يحمل `@PreAuthorize` لأنه لا يكشف أي محتوى.
5. **لا تحقن `FileService` من موديولك:** هو داخلي (ArchUnit)، و`@PreAuthorize` عليه يُطبَّق على المستدعي. استخدم REST أو `FileDocumentLookupApi`.

---

## 6. أكواد الأخطاء

| الكود | HTTP | المعنى |
|---|---|---|
| `FILE_DOCUMENT_SIZE_EXCEEDED` | 413 | تجاوز الحد |
| `FILE_DOCUMENT_TYPE_NOT_ALLOWED` | 415 | النوع غير مسموح للتصنيف |
| `FILE_ACCESS_TOKEN_INVALID` | 401 | توكن تنزيل غير صالح |
| `FILE_DOCUMENT_OWNERSHIP_REQUIRED` | 400 | حقول الملكية ناقصة (عملياً يسبقها `VALIDATION_ERROR`) |
| `FILE_CATEGORY_CODE_DUPLICATE` | 409 | كود تصنيف مكرر |
| `FILE_CATEGORY_INACTIVE` | 422 | تصنيف معطّل |
| `FILE_LOOKUP_KEY_UNKNOWN` | 404 | مفتاح قائمة غير معروف |
| `FILE_DOCUMENT_INVALID_TRANSITION` | 422 / 400 | انتقال حالة غير مسموح / action غير معروف |
| `FILE_DOCUMENT_NOT_FOUND` | 404 | غير موجود (أو `DELETED`) |
| `FILE_CATEGORY_NOT_FOUND` | 404 | تصنيف غير موجود |

---

## 7. الجداول والإعدادات

- `FILE_CATEGORY`: `ID, CATEGORY_CODE (unique), NAME_AR, NAME_EN, MAX_SIZE_BYTES, ALLOWED_CONTENT_TYPES, IS_ACTIVE_FL (SMALLINT)` + أعمدة التدقيق — `SEQ_FILE_CATEGORY`.
- `FILE_DOCUMENT`: `ID, OWNER_ID, OWNER_TYPE, MODULE_CODE, FILE_NAME, CONTENT_TYPE, FILE_SIZE, FILE_CONTENT (BYTEA), FILE_TYPE_ID, FILE_STATUS_ID, FILE_CATEGORY_FK` + التدقيق — `SEQ_FILE_DOCUMENT`؛ فهرس `(OWNER_ID, OWNER_TYPE, MODULE_CODE)`.
- Migrations: `V8__file_schema.sql`، `V9`/`V31` (الأمان)، `V20` (قوائم MDL).

| المفتاح | البيئة |
|---|---|
| `file.access-token.secret` | `FILE_ACCESS_TOKEN_SECRET` (إلزامي في prod) |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | 15MB / 25MB |
| `spring.data.redis.host` / `.port` | Redis |

---

## 8. فجوات وقيود معروفة

- لا checksum، ولا تخزين على نظام الملفات، ولا تحقق بالامتداد، ولا purge.
- أي مستخدم يملك `PERM_FILE_BROWSER_VIEW` يرى ملفات أي مالك (لا تقييد حسب السجل/الموديول).
- `FILE_DOCUMENT_OWNERSHIP_REQUIRED` غير قابل للوصول عبر REST (الـ Bean Validation يسبقه) — فجوة `OPEN` بانتظار قرار.
- ملخص بالإنجليزية بنفس أرقام الأقسام التي تستشهد بها ملفات الحوكمة: [integration-notifications-fileservice.md](integration-notifications-fileservice.md) §2.
