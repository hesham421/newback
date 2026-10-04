# المرجع: الإشعارات NOTIF (`com.erp.notif`)

> **الحالة:** مُنفَّذ — كل مراحل الـ backend الثماني `COMPLETE` في
> `governance/shared/backend/modules/NOTIF/execution-state.json`؛ اختبارات الـ backend `PENDING`.
> وثائق الـ API المولّدة: `governance/shared/backend/modules/NOTIF/api-docs/`.
> موديول **Legacy Path** (انظر CLAUDE.md).
> **المصدر:** مستخرج من الكود بتاريخ 2026-10-01 — عند التعارض، الكود هو المرجع.
> **الفهرس:** [PLATFORM-MODULES-INTEGRATION-INDEX.md](PLATFORM-MODULES-INTEGRATION-INDEX.md)

NOTIF يرسل إشعارات من قوالب ثنائية اللغة عبر قنوات (EMAIL فعلياً)، ويسجّل كل محاولة في `NOTIF_LOG`.
الإرسال **متزامن** (لا queue)، وفشل الإرسال **لا يرمي خطأ**: يُسجَّل في السجل فقط.

---

## 1. طريقة التكامل — الخلاصة

| السيناريو | الطريقة |
|---|---|
| موديول داخل نفس التطبيق (الموصى به) | حقن `com.erp.notif.crossmodule.NotificationDispatchApi` |
| خدمة منفصلة / الواجهة الأمامية | `POST /api/v1/notifications/dispatch` |
| قراءة نتيجة الإرسال من Java | `NotificationLogQueryApi` |
| تفعيل/تعطيل قناة من Java | `NotificationChannelAdminApi` |

> **NOTIF لا يستمع لأحداث الموديولات الأخرى** (لا يوجد أي `@EventListener` في المشروع). الموديول المُرسِل هو من يستدعي `NotificationDispatchApi` صراحةً.
> `SecurityAuthEventListener` المذكور في migrations قديمة **لم يُكتب أبداً**.

---

## 2. واجهات Java (`com.erp.notif.crossmodule`)

### 2.1 `NotificationDispatchApi`

```java
List<Long> dispatch(DispatchCommand command);               // REQUIRED — ينضم لمعاملة المستدعي
List<Long> dispatchIndependently(DispatchCommand command);  // REQUIRES_NEW — معاملة مستقلة
```

- كلاهما محمي بـ `@PreAuthorize("isAuthenticated()")` ويرجع معرّفات `NOTIF_LOG` (واحد لكل قناة).
- **استخدم `dispatchIndependently` افتراضياً** لسببين موثّقين في `DispatchService`:
  1. فشل داخل `dispatch` (مثل قالب غير موجود) يجعل معاملة المستدعي rollback-only حتى لو التقطت الاستثناء.
  2. داخل `@TransactionalEventListener(AFTER_COMMIT)`، الـ REQUIRED ينضم لمعاملة منتهية فيضيع صف السجل بصمت.
- `dispatchIndependently` يفحص المستلم **داخل معاملة المستدعي** ثم يرسل في معاملة `REQUIRES_NEW`، لذا يصح إرسال إشعار لمستخدم أنشأته معاملتك ولم تُثبَّت (commit) بعد.
- استخدم `dispatch` فقط عندما تريد أن يفشل عملك إذا فشل تجهيز الإشعار.

```java
public record DispatchCommand(
    Long recipientId,              // userPk في SEC — إلزامي
    String templateCode,           // كود القالب — إلزامي
    List<String> channelHint,      // مثل List.of("EMAIL") — إلزامي وغير null
    String moduleCode,             // كود موديولك ≤50 — إلزامي
    Long referenceId,              // اختياري: معرّف السجل المصدر
    String referenceType,          // اختياري ≤100
    Map<String, String> variables  // قيم المتغيرات
) {}
```

> لا يوجد Bean Validation على `DispatchCommand`، لذا تحقق من القيم بنفسك.

### 2.2 `NotificationLogQueryApi`

`List<DispatchLogRecord> findByRecipientModuleAndReference(Long recipientId, String moduleCode, String referenceType)`
← `DispatchLogRecord(Long id, String templateCode, String notificationStatusId)`، الأحدث أولاً، بدون `@PreAuthorize`.

### 2.3 `NotificationChannelAdminApi`

`void setChannelEnabled(String channelTypeId, boolean enabled)` — مرّر الكود بأحرف كبيرة؛ يتطلب أن يملك المستدعي `PERM_NOTIF_CHANNELS_UPDATE`.

### 2.4 النمط المرجعي (من `sec/service/PasswordResetService`)

```java
@Service @RequiredArgsConstructor @Slf4j
public class XyzOrderService {
    private final NotificationDispatchApi notificationDispatchApi;

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_XYZ_ORDERS_UPDATE)")
    public ServiceResult<XyzOrderResponse> approve(Long id) {
        XyzOrder order = ...; // منطق الأعمال
        try {
            notificationDispatchApi.dispatchIndependently(new DispatchCommand(
                order.getRequesterUserId(), "XYZ_ORDER_APPROVED", List.of("EMAIL"), "XYZ",
                order.getId(), "XYZ_ORDER",
                Map.of("email", requesterEmail,           // إلزامي لقناة EMAIL
                       "lang", "AR",                      // AR أو أي قيمة أخرى ⇒ EN
                       "orderNo", order.getOrderNo(),
                       "actionLink", frontendUrl + "/xyz/orders/" + order.getId(),
                       "ctaLabelAr", "عرض الطلب", "ctaLabelEn", "View order")));
        } catch (RuntimeException e) {
            log.warn("Notification dispatch failed for order {}", order.getId(), e); // لا تُفشل العملية
        }
        return ServiceResult.success(mapper.toResponse(order), Status.UPDATED);
    }
}
```

**عنوان البريد:** NOTIF لا يملأ البريد تلقائياً، بل يجب تمريره في `variables.email`، وهذا مقصود: لا يُرسل بريد إلى عنوان لم يختره المستدعي.
للحصول عليه من معرّف المستخدم استخدم `SecUserDirectoryApi.findContact(userPk)` (انظر [sec](platform-integration-sec.md)):

```java
String email = secUserDirectoryApi.findContact(userPk).map(UserContact::email).orElse(null);
```

**مستدعٍ بلا مستخدم مصادَق** (job أو scheduler): البوابة `isAuthenticated()` تتطلب سياقاً. SEC يستخدم `InternalCallerContext.call(...)`، لكنه في `sec.security`، واستيراده من موديول آخر يكسر ArchUnit. تحتاج قرار حوكمة أولاً (انظر §5 في مرجع SEC).

---

## 3. REST

| الطريقة والمسار | الصلاحية | الجسم / الرد |
|---|---|---|
| `POST /api/v1/notifications/dispatch` | `isAuthenticated()` | `DispatchRequest` (نفس حقول `DispatchCommand` مع Validation) ⇒ **200** `{ "logIds": [..] }` |
| `POST /api/v1/notifications/templates` | `PERM_NOTIF_TEMPLATES_CREATE` | `TemplateCreateRequest` ⇒ 201 |
| `POST /api/v1/notifications/templates/search` · `GET /{id}` | `PERM_NOTIF_TEMPLATES_VIEW` | |
| `PUT /api/v1/notifications/templates/{id}` | `PERM_NOTIF_TEMPLATES_UPDATE` | الكود غير قابل للتعديل |
| `DELETE /api/v1/notifications/templates/{id}` | `PERM_NOTIF_TEMPLATES_DELETE` | تعطيل ناعم، 204 |
| `POST /api/v1/notifications/channels` · `/search` · `GET/PUT/DELETE /{id}` | `PERM_NOTIF_CHANNELS_*` | `{channelTypeId, isEnabledFl, configJson}` |
| `POST /api/v1/notifications/logs/search` · `GET /{id}` | `PERM_NOTIF_LOG_VIEW` | فلاتر: `recipientId, moduleCode, channelTypeId, notificationStatusId, referenceType, sentAt` |
| `GET /api/v1/notifications/lookups/{key}` | `isAuthenticated()` | `NOTIF_CHANNEL` أو `NOTIF_STATUS` فقط (من MDL) |

```bash
curl -X POST http://localhost:7272/api/v1/notifications/dispatch \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"recipientId":42,"templateCode":"XYZ_ORDER_APPROVED","channelHint":["EMAIL"],
       "moduleCode":"XYZ","referenceId":1001,"referenceType":"XYZ_ORDER",
       "variables":{"email":"user@example.com","lang":"AR","orderNo":"SO-1001"}}'
```

---

## 4. ماذا يحدث داخل الإرسال

1. القالب بـ `templateCode` (يُطبَّع trim + أحرف كبيرة). غير موجود ⇒ 404 `NOTIF_TEMPLATE_NOT_FOUND`؛ معطّل ⇒ 422 `NOTIF_TEMPLATE_INACTIVE`.
2. فحص المستلم (RULE-NOTIF-007) عبر المنفذ `RecipientDirectory`، الذي ينفّذه `SecRecipientDirectory` فوق `SecUserDirectoryApi.findContact` في SEC.
   مستخدم غير موجود أو غير نشط ⇒ رد 200 مع `logIds: []`، ولا يُكتب أي سجل.
3. لكل قناة في `channelHint`: لا يوجد إعداد لها أو هي معطّلة ⇒ صف `CHANNEL_DISABLED` بدون إرسال. وإلا تُرسل مع إعادة محاولة حتى **5 مرات فورية** (الـ backoff محسوب لكنه لا ينتظر).
4. الحالة النهائية لكل صف: `SENT` أو `FAILED` (مع `errorMessage`) أو `CHANNEL_DISABLED`. **الرد 200 في كل الحالات.** لمعرفة النجاح افحص السجل.

### القنوات

| القناة | الحالة |
|---|---|
| `EMAIL` | فعلية عبر `JavaMailSender` (SMTP)، HTML + نص بديل، UTF-8، و RTL للعربية |
| `SMS, WHATSAPP, PUSH, INTERNAL` | معرّفة في قائمة MDL `NOTIF_CHANNEL` لكن **بلا إعداد** ⇒ `CHANNEL_DISABLED`. وإن أُضيف لها إعداد، فالمزوّد stub **يسجّلها `SENT` دون إرسال فعلي** |

### المتغيرات

- الصيغة **قوس واحد `{var}`** (ليست `{{var}}` ولا `${var}`). متغير غير معرّف يبقى كما هو في النص.
- متغيرات خاصة:

| المتغير | الأثر |
|---|---|
| `email` | عنوان المستلم — **بدونه تفشل كل رسائل EMAIL** |
| `lang` | `AR` ⇒ `subjectAr/bodyAr`، وأي قيمة أخرى ⇒ EN |
| `actionLink` | زر CTA في HTML (يبقى الرابط نصاً في النسخة النصية) |
| `ctaLabelAr` / `ctaLabelEn` | نص الزر |

نص القالب وكل قيم المتغيرات تُهرَّب (HTML-escaped) في نسخة الـ HTML، والنسخة النصية تحمل القيم الخام. لذلك يمكن تمرير نص أدخله المستخدم بأمان.

---

## 5. خطوات دمج موديول جديد مع NOTIF

1. **القوالب عبر migration جديد** (بالرقم التالي، نمط `V11__notif_email_channel_seed.sql`):
   ```sql
   INSERT INTO NOTIF_TEMPLATE (ID, TEMPLATE_CODE, NAME_AR, NAME_EN, SUBJECT_AR, SUBJECT_EN,
                               BODY_AR, BODY_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
   VALUES (nextval('SEQ_NOTIF_TEMPLATE'), 'XYZ_ORDER_APPROVED',
           'اعتماد طلب', 'Order approved',
           'تم اعتماد الطلب {orderNo}', 'Order {orderNo} approved',
           'تم اعتماد طلبك رقم {orderNo}.' || chr(10) || 'شكراً لك.',
           'Your order {orderNo} has been approved.' || chr(10) || 'Thank you.',
           1, 'SYSTEM', CURRENT_TIMESTAMP);
   ```
   - الكود بأحرف كبيرة، ويُفضّل أن يبدأ بكود موديولك. `chr(10)` يتحول إلى `<br>`.
   - القالب الواحد ثنائي اللغة ويخدم كل القنوات (لا عمود لغة ولا عمود قناة).
   - الجسمان AR وEN إلزاميان؛ العنوان اختياري (يُستخدم الاسم بدلاً منه).
   - تعديل نص قالب قائم يكون بـ migration جديد فيه `UPDATE` (كما فعل V12 وV32)، وليس بتعديل migration قديم.
2. **حقن `NotificationDispatchApi`** في الـ Service واستدعاء `dispatchIndependently` داخل `try/catch` (النمط في §2.4).
3. **`moduleCode` = كود موديولك**، و`referenceType`/`referenceId` لربط السجل بكيانك. هذا يتيح الاستعلام لاحقاً بـ `NotificationLogQueryApi`.
4. **لا صلاحية خاصة للإرسال:** يكفي أن يكون المستخدم مصادَقاً. صلاحيات `PERM_NOTIF_*` لإدارة القوالب والقنوات والسجل فقط (مُعطاة لـ `NOTIF_ADMIN` و`SYS_ADMIN` في V31).
5. قناة غير EMAIL تحتاج صف `NOTIF_CHANNEL_CONFIG` وتنفيذ مزوّد فعلي في `DefaultChannelProvider`، وهذا خارج نطاق الموديول المستهلك.

---

## 6. أكواد الأخطاء

| الكود | HTTP |
|---|---|
| `NOTIF_TEMPLATE_NOT_FOUND` / `NOTIF_CHANNEL_CONFIG_NOT_FOUND` / `NOTIF_LOG_NOT_FOUND` / `NOTIF_LOOKUP_KEY_UNKNOWN` | 404 |
| `NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND` — `attachmentFileId` لا يشير إلى ملف متاح في FILE (عند إنشاء القالب أو تعديله) | 404 |
| `NOTIF_TEMPLATE_INACTIVE` / `NOTIF_LOG_INVALID_TRANSITION` | 422 |
| `NOTIF_TEMPLATE_CODE_DUPLICATE` / `NOTIF_CHANNEL_CONFIG_DUPLICATE` | 409 |
| `NOTIF_TEMPLATE_BILINGUAL_REQUIRED` / `NOTIF_CHANNEL_TYPE_REQUIRED` | 400 |

---

## 7. الجداول والإعدادات

- `NOTIF_TEMPLATE`: `TEMPLATE_CODE (unique ≤80), NAME_AR/EN, SUBJECT_AR/EN, BODY_AR/EN (TEXT), ATTACHMENT_FILE_ID, IS_ACTIVE_FL (SMALLINT)`.
  `ATTACHMENT_FILE_ID` مرجع مرن (بدون FK) يُتحقق منه عبر `FileDocumentLookupApi.isAvailable` من FILE.
- `NOTIF_CHANNEL_CONFIG`: `CHANNEL_TYPE_ID (unique), IS_ENABLED_FL, CONFIG_JSON` (الـ `CONFIG_JSON` يُمرَّر للمزوّد لكنه غير مستخدم).
- `NOTIF_LOG`: `RECIPIENT_ID` (بدون FK)، `CHANNEL_TYPE_ID, NOTIFICATION_STATUS_ID, MODULE_CODE, REFERENCE_ID, REFERENCE_TYPE, RETRY_COUNT, ERROR_MESSAGE, SENT_AT, TEMPLATE_FK`.
- القوالب المزروعة: `PASSWORD_RESET` (مستخدم من SEC)، و`ACCOUNT_ACTIVATION` (مزروع لكن لا يوجد كود يرسله).
- Migrations: `V6` (المخطط)، `V7`/`V31` (الأمان)، `V11`/`V12`/`V32` (القناة والقوالب)، `V20` (قوائم MDL).

| المفتاح | البيئة |
|---|---|
| `spring.mail.host` / `port` | `MAIL_HOST` (افتراضي smtp.gmail.com) / `MAIL_PORT` (587) |
| `spring.mail.username` / `password` | `MAIL_USERNAME` / `MAIL_PASSWORD`، والمرسِل = `username` |

لا توجد خصائص `notif.*`.

---

## 8. قيود معروفة

- المرفقات: `attachmentFileId` يُتحقق منه، لكن الملف **لا يُرفق** بالبريد فعلياً.
- لا يوجد 202 Accepted، ولا queue. إعادة المحاولات الخمس فورية: الـ backoff محسوب في `RetryPolicy` لكنه لا ينتظر داخل الطلب.
- القنوات غير EMAIL ليس لها مزوّد حقيقي.
- `NOTIF_TEMPLATE_BILINGUAL_REQUIRED` غير قابل للوصول عبر REST (الـ Bean Validation يسبقه)، وهي فجوة `OPEN` بانتظار قرار.
- القالب `ACCOUNT_ACTIVATION` مزروع لكن لا يوجد كود يرسله.
- ملخص بالإنجليزية بنفس أرقام الأقسام التي تستشهد بها ملفات الحوكمة: [integration-notifications-fileservice.md](integration-notifications-fileservice.md) §1.
