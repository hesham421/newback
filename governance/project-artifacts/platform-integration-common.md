# المرجع: الأساس المشترك `common` (`com.erp.common`)

> **الحالة:** مُنفَّذ ومستخدم فعلياً من كل الموديولات (SEC, MDL, FIN, CU, NOTIF, FILE).
> **المصدر:** مستخرج من الكود مباشرة بتاريخ 2026-10-01 — عند أي تعارض، الكود هو المرجع.
> **الفهرس:** [PLATFORM-MODULES-INTEGRATION-INDEX.md](PLATFORM-MODULES-INTEGRATION-INDEX.md)

`common` ليس موديولاً له جداول أو endpoints؛ هو **العقد** بين البنية التحتية والكود المُولَّد.
أي موديول جديد **يستهلكه ولا يعيد اختراعه**. لا يحتاج أي تسجيل — يكفي الـ import.

---

## 1. خريطة المحتوى

| الحزمة | الكلاس | الاستخدام في موديول جديد |
|---|---|---|
| `common.domain` | `AuditableEntity` | كل Entity ترث منه |
| `common.audit` | `AuditEntityListener` | يعمل تلقائياً — لا تستدعه |
| `common.domain.status` | `Status`, `ServiceResult<T>` | ما يرجعه كل Service |
| `common.exception` | `LocalizedException`, `ErrorDetail`, `CommonErrorCodes` | كل خطأ أعمال |
| `common.web` | `ApiResponse`, `ApiError`, `FieldErrorItem`, `OperationCode`, `GlobalExceptionHandler` | الـ Controller والغلاف الموحّد |
| `common.search` | `SearchRequest`, `SearchFilter`, `SearchOperator`, `SpecBuilder`, `PageableBuilder`, `SetAllowedFields`, `*FieldValueConverter` | البحث والترقيم |
| `common.dto` | `BaseSearchContractRequest` | الأب لكل `XxxSearchRequest` |
| `common.converter` | `BooleanNumberConverter`, `BooleanCharYNConverter` | أعمدة Boolean المخزّنة كرقم/حرف |
| `common.util` | `SecurityContextHelper`, `TokenHasher` | المستخدم الحالي، تجزئة التوكنات |

---

## 2. الـ Entity والتدقيق (Audit)

```java
@Entity @Table(name = "XYZ_ITEM")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class XyzItem extends AuditableEntity { ... }
```

`AuditableEntity` (`@MappedSuperclass` + `@EntityListeners(AuditEntityListener.class)`) يضيف:

| الحقل | العمود | ملاحظات |
|---|---|---|
| `createdBy` | `CREATED_BY` | `updatable=false`, `length=100` |
| `createdAt` | `CREATED_AT` | `Instant`, `updatable=false` |
| `updatedBy` | `UPDATED_BY` | `length=100` |
| `updatedAt` | `UPDATED_AT` | `Instant` |

- `AuditEntityListener` يملأها في `@PrePersist`/`@PreUpdate` من `SecurityContextHelper.getCurrentUsername()`، وإن لم يوجد مستخدم مصادق تكون القيمة `"system"`.
- **لا** يُستخدم Spring Data Auditing (`@EnableJpaAuditing`/`AuditorAware` غير موجودين) — لا تضفهما.
- في الـ migration: اجعل `CREATED_BY`/`UPDATED_BY` من نوع `VARCHAR(100)` (طول اسم المستخدم في SEC).
- يمكن للـ Entity أن يملك `@PrePersist`/`@PreUpdate` خاصاً به للتطبيع (مثل تكبير الأكواد) — انظر `FileCategory`.
- `JpaConfig` (`com.erp.main.config`) يفحص `com.erp` بالكامل، فلا يلزم أي إعداد JPA لموديول جديد.

### أعمدة الـ Boolean — اختر حسب نوع العمود

| نوع العمود في DB | التحويل | أمثلة |
|---|---|---|
| `BOOLEAN` (PostgreSQL أصلي) — **الموصى به للجديد** | بدون converter | SEC, MDL, FIN (`isActiveFl`) |
| `SMALLINT` 0/1 | `@Convert(converter = BooleanNumberConverter.class)` | FILE, NOTIF, CU (`isActive`) |
| `CHAR(1)` Y/N | `@Convert(converter = BooleanCharYNConverter.class)` | غير مستخدم حالياً |

> تطبيق converter على عمود `BOOLEAN` أصلي يكسره (موثّق في `sec/entity/User.java`).

---

## 3. نتيجة الخدمة و`Status`

كل method في الـ Service ترجع `ServiceResult<T>`:

```java
return ServiceResult.success(mapper.toResponse(saved), Status.CREATED); // 201
return ServiceResult.success(page.map(mapper::toResponse));             // 200 (SUCCESS)
```

| `Status` | HTTP | | `Status` | HTTP |
|---|---|---|---|---|
| `SUCCESS` | 200 | | `BUSINESS_RULE_VIOLATION` | 422 |
| `CREATED` | 201 | | `VALIDATION_ERROR` | 400 |
| `UPDATED` | 200 | | `PAYLOAD_TOO_LARGE` | 413 |
| `NOT_FOUND` | 404 | | `UNSUPPORTED_MEDIA_TYPE` | 415 |
| `ALREADY_EXISTS` | 409 | | `UNAUTHORIZED` | 401 |
| `CONFLICT` | 409 | | `FORBIDDEN` | 403 |
| | | | `INTERNAL_ERROR` | 500 |

> لا يوجد `ACCEPTED (202)` — مسجّل كفجوة في NOTIF.

---

## 4. الأخطاء والتعريب (i18n)

### 4.1 رمي الخطأ

```java
// كود واحد مع وسائط {0},{1}
throw new LocalizedException(Status.NOT_FOUND, XyzErrorCodes.XYZ_ITEM_NOT_FOUND, id);

// عدة أخطاء حقول (تظهر في error.fieldErrors)
throw LocalizedException.withDetails(Status.ALREADY_EXISTS, XyzErrorCodes.XYZ_409_DUP,
        List.of(ErrorDetail.ofField("code", XyzErrorCodes.XYZ_409_DUP, code)));
```

- لا ترمِ `RuntimeException` خاماً ولا نصوصاً ثابتة — يرفضها `gov-enforce-error-handling`.
- لحالات السباق (TOCTOU) على القيود الفريدة: التقط `DataIntegrityViolationException` وحوّله إلى `ALREADY_EXISTS` (مثال: `FileCategoryService.create`).
- `ErrorDetail.of(code, args)` يُستخدم أيضاً كحارس غير رامٍ يرجع `Optional<ErrorDetail>` في الـ Domain (مثال: `fin/domain/AccountDomain.checkPostable()`).

### 4.2 تعريف أكواد الموديول

ملف واحد لكل موديول: `com.erp.<mod>.exception.<Mod>ErrorCodes`:

```java
public final class XyzErrorCodes {
    private XyzErrorCodes() { throw new UnsupportedOperationException(); }
    /** RULE-XYZ-001 */
    public static final String XYZ_404_ITEM = "XYZ-404-ITEM";
}
```

يوجد اصطلاحان في الكود؛ **الموصى به للجديد** هو `MOD-<http>-<SLUG>` (SEC, MDL, FIN). الاصطلاح القديم `ENTITY_SCENARIO` (FILE, NOTIF, CU).
في الحالتين: **قيمة الثابت = الـ `code` في الرد = مفتاح رسالة الترجمة.**

### 4.3 تسجيل الرسائل

- `spring.messages.basename=i18n/messages` → الملفان `src/main/resources/i18n/messages.properties` (EN، الافتراضي) و`messages_ar.properties` (AR).
- **يُضاف قسم جديد داخل نفس الملفين** (لا تنشئ bundle منفصلاً)، بنفس المفاتيح في الاثنين:
  ```properties
  # --- Xyz (XYZ) — domain-layer error codes ---
  XYZ-404-ITEM=Item with ID {0} was not found.
  ```
- الصيغة `MessageFormat`: الوسائط `{0}`، والفاصلة العليا تُكتب `''`.
- اختيار اللغة من ترويسة `Accept-Language` (لا يوجد `LocaleResolver` مخصص).
- لرسائل الـ Bean Validation استخدم المفاتيح العامة: `{validation.required}`, `{validation.size}`, `{validation.invalid}`, `{validation.min}`, `{validation.max}`, `{validation.pattern}`.
- مفتاح غير مسجّل ⇒ يُرجَع الكود نفسه كرسالة مع تحذير في الـ log.

### 4.4 أكواد عامة (`CommonErrorCodes`)

`VALIDATION_ERROR`, `INTERNAL_ERROR`, `ACCESS_DENIED`, `DATA_INTEGRITY_VIOLATION`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_FILTER_FIELD`, `UNSUPPORTED_FILTER_OPERATOR`.

---

## 5. الغلاف الموحّد للرد و`GlobalExceptionHandler`

```json
// نجاح
{ "success": true, "data": { ... }, "timestamp": "2026-10-01T10:00:00Z" }
// فشل
{ "success": false,
  "error": { "code": "XYZ-404-ITEM", "message": "...", "fieldErrors": [ { "field": "code", "message": "..." } ] },
  "timestamp": "..." }
```

### Controller رفيع

```java
@RestController @RequestMapping("/api/v1/xyz/items") @RequiredArgsConstructor
public class XyzItemController {
    private final XyzItemService service;
    private final OperationCode operationCode;

    @PostMapping
    public ResponseEntity<ApiResponse<XyzItemResponse>> create(@Valid @RequestBody XyzItemCreateRequest r) {
        return operationCode.craftResponse(service.create(r));   // HTTP من ServiceResult.status
    }
    @PostMapping("/search")   // البحث POST دائماً
    public ResponseEntity<ApiResponse<Page<XyzItemResponse>>> search(@Valid @RequestBody XyzItemSearchRequest r) {
        return operationCode.craftResponse(service.search(r));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.deactivate(id); }
}
```

الاستثناء الوحيد من `craftResponse`: تنزيل ملف ثنائي (`FileController.download` يرجع `ResponseEntity<byte[]>`).

### ما يعالجه `GlobalExceptionHandler` (لا تكتب `@RestControllerAdvice` خاصاً بموديولك)

| الاستثناء | HTTP | `code` |
|---|---|---|
| `LocalizedException` | من `Status` | الكود المرمي (+ `fieldErrors` من `ErrorDetail`) |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` (حقل لكل خطأ) |
| `HttpMessageNotReadableException` | 400 | `VALIDATION_ERROR` |
| `MissingServletRequestParameter` / `MethodArgumentTypeMismatch` | 400 | `VALIDATION_ERROR` |
| `HttpRequestMethodNotSupportedException` | 405 | `METHOD_NOT_ALLOWED` |
| `DataIntegrityViolationException` | 409 | `DATA_INTEGRITY_VIOLATION` |
| `AccessDeniedException` (من `@PreAuthorize`) | 403 | `ACCESS_DENIED` |
| أي `Exception` آخر | 500 | `INTERNAL_ERROR` |

> أخطاء سلسلة الفلاتر (توكن مفقود/منتهي) لا تصل هنا — يكتبها SEC بنفس الشكل. انظر [platform-integration-sec.md](platform-integration-sec.md).

---

## 6. البحث والترقيم

### 6.1 DTO البحث

```java
@Data @EqualsAndHashCode(callSuper = true) @NoArgsConstructor @SuperBuilder
public class XyzItemSearchRequest extends BaseSearchContractRequest { }
```

جسم الطلب:

```json
{ "filters": [ { "field": "nameEn", "operator": "LIKE", "value": "cash" },
               { "field": "isActiveFl", "operator": "EQUALS", "value": true } ],
  "sortField": "createdAt", "sortDirection": "DESC", "page": 0, "size": 20 }
```

المعاملات (`SearchOperator`): `EQUALS, NOT_EQUALS, LIKE` (غير حساس لحالة الأحرف، `%v%`), `GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IN` (القيمة List).

### 6.2 Service البحث (النمط المرجعي — من `FileCategoryService.search`)

```java
private static final Set<String> ALLOWED_FIELDS = Set.of("code", "nameAr", "nameEn", "isActiveFl", "createdAt");
private static final FieldValueConverter CONVERTER = new BooleanFieldValueConverter(Set.of("isActiveFl"));

@Transactional(readOnly = true)
@PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_XYZ_ITEMS_VIEW)")
public ServiceResult<Page<XyzItemResponse>> search(XyzItemSearchRequest request) {
    SearchRequest common = request.toCommonSearchRequest();
    Specification<XyzItem> spec = SpecBuilder.build(common, new SetAllowedFields(ALLOWED_FIELDS), CONVERTER);
    Pageable pageable = PageableBuilder.from(common, ALLOWED_FIELDS);
    return ServiceResult.success(repository.findAll(spec, pageable).map(mapper::toResponse));
}
```

- الـ Repository يجب أن يرث `JpaRepository<T, Long>` و`JpaSpecificationExecutor<T>`.
- أسماء الحقول = أسماء خصائص الـ Entity (لا أسماء الأعمدة).
- حقل فلترة غير مسموح ⇒ 400 `VALIDATION_ERROR` مع `fieldErrors[].message` = `UNSUPPORTED_FILTER_FIELD`.
- حقل ترتيب غير مسموح ⇒ يُتجاهل بصمت.
- `PageableBuilder`: الصفحة الافتراضية 0، الحجم الافتراضي 20، **الحد الأقصى 200**.
- فلتر Boolean يحتاج `BooleanFieldValueConverter`، وإلا وصلت القيمة كنص.
- بحث مقيّد بأب (parent): انظر `mdl/dto/LookupValueSearchRequest` (`extractLongFilter` + `toCommonSearchRequest(excludeFields)`) و`mdl/service/LookupValueService.search` (`parentSpec.and(SpecBuilder.build(...))`).

---

## 7. الأدوات

| الأداة | الاستخدام |
|---|---|
| `SecurityContextHelper.getCurrentUsername()` | اسم المستخدم الحالي (أو `"system"`). لتسجيل "من قام بـ" في الـ Domain |
| `SecurityContextHelper.hasAuthority(String)` | تحقق صلاحية يعتمد على محتوى الطلب ولا يعبّر عنه `@PreAuthorize` |
| `TokenHasher.sha256Hex(raw)` | تخزين أي توكن مُجزّأً فقط (لا تخزّن التوكن الخام) |

---

## 8. حدود الموديولات (ArchUnit)

`src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java` هو **الضامن الوحيد** لحدود الموديولات (المشروع artifact واحد):

1. **لا يعتمد أي كلاس خارج `com.erp.X` على كلاس داخل `com.erp.X` إلا إن كان في `com.erp.X.crossmodule`.**
   `common` و`main` مستثنيان. موديول بلا `crossmodule` لا يمكن الاعتماد عليه إطلاقاً.
2. لا يُسمح بـ `T(fqcn)` عابر للموديولات في `@PreAuthorize`، **باستثناء** `com.erp.sec.permission.PermissionConstants`.

> **إلزامي:** أضف موديولك الجديد إلى قائمة `MODULES` في هذا الاختبار فور إنشاء أول كلاس فيه:
> `new Module("com.erp.xyz", "com.erp.xyz.crossmodule")` — وإلا عومل كـ "مشترك" ولن تُطبَّق حدوده.

---

## 9. ملاحظات

- `ActiveFlagQueryHelper` و`BooleanCharYNConverter` موجودان لكن غير مستخدمين حالياً.
- `GlobalExceptionHandler` يرجع رسالة إنجليزية ثابتة لجسم JSON غير صالح (مقصود).
- المهارات `build-*` و`gov-*` في `.claude/skills/` تفترض هذه الكلاسات بالضبط — لا تغيّر أسماءها.
