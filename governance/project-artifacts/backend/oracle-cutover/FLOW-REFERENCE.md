# المرجع السريع: مسار القيود من النظام القديم (Oracle) إلى FIN (PostgreSQL)

> **آخر تحديث:** 2026-09-25، وكل ما فيه مُتحقَّق منه على النظام الشغّال.
> **الغرض:** مرجع واحد يكفي لفهم المسار كاملاً، ولتنفيذ أي تعديل فني مستقبلاً: إضافة، أو تتبّع، أو حل مشكلة.
> **يُرفع** كملف معرفة في مشروع Claude.ai.
> **المصادر التفصيلية:**
> - `HANDOVER.md`، ويحوي تاريخ التنفيذ والأعطال.
> - `seed-and-cutover-findings.md`، ويحوي القرارات D-01 … D-23.
> - في مستودع `fin-consumer`: `fin-consumer-README.md` و`docs/test-report.md` و`docs/findings.md`.

---

## 0. تعليمات للمساعد (اقرأها قبل أي إجابة)

**الثوابت التي لا تُكسر أبداً:**
1. **لا رقم حساب ولا مفهوم محاسبي في الـ payload.** Oracle يرسل حقائق تجارية فقط، وFIN وحده يقرر الحسابات والاتجاه. الخدمة ترفض أي مفتاح يطابق `account|acct|ledger|gl_code|debit|credit|restriction`.
2. **الحدث موجود إذا وفقط إذا نجح حفظ العملية التجارية.** الإرسال يتم داخل نفس المعاملة: ROLLBACK يُلغي الحدث، وCOMMIT يُظهره.
3. **لا يُفقد حدث، ولا يُرحَّل مرتين، ولا يتعطّل المستخدم.** الـ commit في الخدمة يأتي **بعد** تأكيد FIN، والرد «مكرر» يُعامَل كنجاح، والتريجر لا يرفع خطأً أبداً.
4. **لا معاملات موزّعة (2PC).** الصحة تأتي من ترتيب الـ commit ومن تفرّد `eventReference`.
5. **عطّل، لا تحذف.** القواعد والـ mappings والحسابات تُعطَّل ولا تُحذف، والصفوف المحتجزة تُعلَّم DISCARDED ولا تُمسح.
6. **لا تخترع.** إن لم يوجد حساب أو mapping أو معنى مؤكد لحقل، فقل «غير موجود» واطلب قرار المالك.
7. **لا تُصلح عيباً قديماً بصمت، ولا تنسخه بصمت.** كل انحراف عن النظام القديم يُسجَّل كقرار D-xx.
8. **ملفات Flyway المطبّقة لا تُعدَّل.** أي تغيير في الإعدادات الأساسية يكون migration جديدة.

**كيف تجيب على طلب تعديل:**
1. حدِّد الطبقة المتأثرة (§1).
2. اذكر الملفات والكيانات بالاسم (§2 إلى §6).
3. اتبع دليل الإجراء المناسب (§8).
4. اذكر ترتيب النشر: FIN أولاً ثم Oracle (§8.0).
5. اذكر كيف يُختبر: معاملة ثم ROLLBACK (§8.9).
6. اذكر الأثر على الأحداث القديمة وعلى القيود العكسية.

---

## 1. الصورة الكاملة

```
 مستخدم النظام القديم ──حفظ──► أحد الجداول السبعة (LOAN_SYS)
                                 │  نفس المعاملة
                                 ▼
 [Oracle] TRG_AE_<TABLE>  ──►  PKG_ACCOUNTING_EVENT.emit(...)
          يحدد نوع الحدث          يبني JSON ويكتبه في:
          ويمرر حقائق تجارية      ├─ ACCOUNTING_EVENT_LOG  (سجل دائم: تدقيق + replay)
                                  └─ ACCOUNTING_EVENT_Q    (طابور AQ)
                                     └─ بعد 5 محاولات فاشلة ← ACCOUNTING_EVENT_Q_EXCPT
                    ════ COMMIT ⇒ الحدث مرئي │ ROLLBACK ⇒ لا حدث ════
                                 │
 [fin-consumer] خيط واحد، رسالة واحدة في كل مرة:
     DEQUEUE (معاملة Oracle مفتوحة) ← ترجمة وفحص ← POST إلى FIN ← قرار:
        نجح / مكرر          ⇒ COMMIT (تُحذف الرسالة)
        FIN متوقف / 5xx     ⇒ ROLLBACK + قاطع دائرة (circuit)
        فترة مقفلة          ⇒ FINC_HELD_EVENT (PARKED)، تُعاد تلقائياً
        خطأ إعدادات/بيانات  ⇒ FINC_HELD_EVENT (FAILED)، تُصلَح ثم replay
                                 │ HTTP + JWT
                                 ▼
 [FIN] POST /api/v1/fin/journal-entries/from-event
       EventEntryService.build(): القاعدة ← الفترة ← لكل سطر (الحساب، المبلغ، الاتجاه، الأبعاد) ← توازن ← ترحيل
       ⇒ fin_journal_entry + fin_journal_line + fin_journal_line_dim
```

| الطبقة | تعرف | لا تعرف |
|---|---|---|
| Oracle (التريجرات والحزمة) | ماذا حدث تجارياً | الحسابات |
| fin-consumer | كيف يُسلَّم الحدث مرة واحدة بأمان | المحاسبة |
| FIN | القواعد والحسابات والتوازن والفترات | وجود Oracle أصلاً |

---

## 2. أين يوجد كل شيء

| المكوّن | المكان |
|---|---|
| ملفات Oracle 1→3 | `backend/governance/project-artifacts/backend/oracle-cutover/`: `oracle-aq-setup.sql` (1)، `oracle-event-emit-package.sql` (2)، `oracle-event-emit-triggers.sql` (3) |
| إيقاف المحاسبة القديمة | `legacy-accounting-stop.sql`، **ويُشغَّل آخراً فقط**، بعد نجاح وضع الظل |
| فحص جاهزية قاعدة حية (قراءة فقط) | `oracle-19c-readiness-check.sql` |
| الخدمة | مستودع مستقل `fin-consumer/`، بجوار `backend/` |
| تهيئة Oracle للخدمة | `fin-consumer/oracle/01` (كـ SYS)، و`02` (كـ LOAN_SYS)، و`03` (كـ FIN_CONSUMER) |
| مستخدم FIN للخدمة | `fin-consumer/scripts/provision-fin-principal.sh`، ويعمل عبر API الـ SEC |
| إعدادات FIN الأساسية | Flyway: `V38__fin_legacy_production_config_seed.sql` و`V39__fin_v2_configuration_delta_seed.sql`. أصولها في `backend/governance/project-artifacts/backend/seed-scripts/` |
| منطق FIN | `backend/src/main/java/com/erp/fin/`، وأهم ملفين `service/EventEntryService.java` و`domain/EventTypeRuleDomain.java` |

---

## 3. طبقة Oracle (schema `LOAN_SYS`)

### 3.1 الكيانات

| الكائن | النوع | الوظيفة |
|---|---|---|
| `ACCOUNTING_EVENT_SEQ` | sequence | لاحقة المرجع الفريد |
| `ACCOUNTING_EVENT_LOG` | جدول | كل حدث صدر. الأعمدة: `EVENT_LOG_PK`، `EVENT_REFERENCE` (فريد)، `EVENT_TYPE_CODE`، `SOURCE_TABLE`، `SOURCE_PK`، `SOURCE_ACTION` (INSERT/UPDATE/DELETE/REPLAY)، `DOC_DATE`، `BASE_AMOUNT`، `PAYLOAD`، `ENQUEUED_FL` (1 = أُرسل، 0 = فشل ويُعاد تشغيله)، `ENQUEUE_ERROR`، `UNDO_FL`، `REVERSES_EVENT_REFERENCE`، `CREATED_DATE` |
| `ACCOUNTING_EVENT_Q` | طابور | حاليّاً TxEventQ بـ payload من نوع JSON، shard واحد (أي FIFO)، و`max_retries=5` |
| `ACCOUNTING_EVENT_Q_EXCPT` | exception queue | تستقبل الرسائل التي فشلت 5 مرات. **يجب** أن تُسمّى في كل رسالة عند الإرسال (D-20) |
| `PKG_ACCOUNTING_EVENT` | package | `emit(...)` هي نقطة الدخول الوحيدة، و`set_enabled(FALSE)` تُسكت الإرسال للجلسة الحالية |
| `TRG_AE_*` (7) | triggers | التقاط الأحداث من الجداول السبعة |
| `FINC_QUEUE_DEPTH` | function | عدد الرسائل المنتظرة (READY، ROLBACKREADY، WAITING) للمراقبة |
| `FIN_CONSUMER` | مستخدم | حساب الخدمة: CREATE SESSION/TABLE/SEQUENCE، وEXECUTE DBMS_AQ، وDEQUEUE على الطابورين، وEXECUTE على `FINC_QUEUE_DEPTH`. **لا صلاحية** على أي جدول قديم ولا ENQUEUE |
| `FIN_CONSUMER.FINC_HELD_EVENT` | جدول | مخزن الأحداث المحتجزة (§5.3) |

### 3.2 الجداول السبعة وأنواع الأحداث (21 نوعاً، ولكل نوع توأم `_REVERSED`)

| الجدول | الشرط | نوع الحدث | المبلغ | التاريخ | المؤسسة / الفرع |
|---|---|---|---|---|---|
| `LOAN_PAYMENT` | FL=7 | `INSTALLMENT_PAYMENT_RECEIVED` | `LOAN_PAYMENT_VALUE` | `LOAN_PAYMENT_DATE` | `ORGANIZATION_FK` / `ORGANIZATION_SUB_FK` |
| | FL=32 | `INVESTOR_SUPPORT_OR_DRAWING` ⁻ | | | |
| | FL=66 | `INVESTOR_SALES_INVOICE` ⁻ | | | |
| | FL=58 | `LAWYER_ADVANCE` ⁻ | | | |
| | FL=59 | `INVESTOR_FEE_2_5` ⁻ | | | |
| | FL=49 | `INVESTOR_FEE` | | | |
| | FL=55 | `EXECUTION_ACTION_FEE` ⁻ | | | |
| | FL=56 | `BAEETHA_FEE` ⁻ | | | |
| | FL=57 | `CIVIL_FEE` ⁻ | | | |
| | FL=8، FL=130 | `SUPPLIER_PAYMENT_MADE`، `OWNER_DRAWING`: **بلا قاعدة** (D-07: لم يُستخدما قط)، ويُرفضان FAILED | | | |
| `CONTRACT` | FL=12 | `CONTRACT_CREATED` | `CASH_PRICE`، ويُرسَل معه `cashPrice` و`totalContract` (D-23) | `CONTRACT_DATE` | `ORGANIZATION_FK` / `ORGANIZATION_SUB_FK` |
| | الدخول إلى FL=13 مع `DISCOUNT≠0` | `CONTRACT_SETTLEMENT` | `DISCOUNT` | `QUITTANCE_DATE` | |
| `COMPLAINTS` | `COMPLAINT_FL=1` مع `DISCOUNT≠0` (تعديل وحذف فقط) | `COMPLAINT_SETTLEMENT` | `DISCOUNT` | `QUITTANCE_DATE` | `ORGANIZATION_FK` / `SUB_ORGANIZATION_FK` |
| `COMPLAINT_DT` | FL=43 / FL=44 | `COURT_INSTALLMENT_PAYMENT` / `LAWYER_FEE_PAYMENT` | `VALUE` | `COMPLAINT_DT_DATE` | `ORGANIZATION_FK` / `ORGANIZATION_SUB_FK` |
| `INVOICE_IMPORT` | كل صف | `PURCHASE_INVOICE` | `grossAmount=CASH_PRICE`، `paidAmount=PAYMENT`، `remainingAmount=REMAINING` | `INV_IMPORT_DATE` | `ORGANIZATION_FK` / `ORGANIZATION_SUB_FK` |
| `EXPENSE_TYPE_DT` | الفئة `EXPENSE_TYPE_DT_FK` = 64 / 65 / 131 / 132 | `REVENUE_RECEIVED` / `EXPENSE_PAID` / `CUSTODY_RECEIVED` / `CUSTODY_PAID_OUT` | `EXPENSE_AMOUNT` | `EXPENSE_DATE` | `COST_CENTER_FK` / `SUB_COST_CENTER_FK` |
| `TRANSFER_SAFE` | النوع 25 / 24 | `SAFE_TRANSFER_TO_SAFE` / `SAFE_TRANSFER_TO_BANK` | `AMOUNT` | `TRANSFER_SAFE_DATE` | `TO_ORGANIZATION_FK` / `TO_ORGANIZATION_SUB_FK` |

⁻ تعني أن الإشارة الطبيعية سالبة: القيمة السالبة هي الحالة العادية (D-14). بقية الأنواع إشارتها الطبيعية موجبة.

**سلوك التريجر حسب العملية:**
- **INSERT:** حدث واحد بالقيم الجديدة.
- **UPDATE:** يُرسل فقط إذا تغيّر أحد الأعمدة المراقبة: المبلغ، أو النوع، أو طريقة الدفع، أو التاريخ، أو المؤسسة، أو الفرع (D-17). عندها يُرسل حدثين: **إلغاء القيم القديمة** ثم **إثبات القيم الجديدة**.
- **DELETE:** حدث إلغاء واحد.
- **في العقود والشكاوى:** يمرّر التريجر رقم العقد أو الشكوى بنفسه، لأن الاستعلام عن الجدول نفسه داخل تريجره يسبب ORA-04091 (D-21).

### 3.3 عقد الـ payload (ما تبنيه `emit`)

```json
{
  "eventReference": "LEGACY:LOAN_PAYMENT:39766:19",
  "eventTypeCode":  "INSTALLMENT_PAYMENT_RECEIVED",
  "occurredAt":     "2026-09-24T16:55:44Z",
  "docDate":        "2026-09-22",
  "baseAmount":     96,
  "amounts":        {"amount": 96},
  "fields": {
    "PAYMENT_METHOD": "CHEQUE", "ORGANISATION_CODE": "1", "BRANCH_CODE": "1",
    "EXPENSE_TYPE_CODE": "…",
    "sourceTable": "LOAN_PAYMENT", "sourcePk": "39766", "sourceAction": "INSERT",
    "undo": "N", "reversesEventReference": "…",
    "partyType": "CUSTOMER", "partyCode": "1599", "contractNo": "…", "investorCode": "…", "complaintNo": "…"
  },
  "descriptionAr": "قيد الى لسداد عميل", "descriptionEn": "INSTALLMENT_PAYMENT_RECEIVED"
}
```
- `EXPENSE_TYPE_CODE` يظهر في أحداث `EXPENSE_TYPE_DT` فقط.
- `reversesEventReference` يظهر في أحداث الإلغاء فقط.
- مفاتيح `partyType` وما بعده تظهر حسب الحالة.

**القواعد التي يتبعها بناء الـ payload:**

| العنصر | القاعدة |
|---|---|
| `eventReference` | `LEGACY:<الجدول>:<PK>:<تسلسل>`، فريد لكل حدث. وهو **مفتاح منع التكرار** في FIN |
| النوع `_REVERSED` | `flip = undo XOR (إشارة المبلغ ≠ الإشارة الطبيعية)`. الـ XOR ضروري (D-19): إلغاء قيد كان معكوساً أصلاً يجب أن يعود للنوع العادي |
| المبالغ | دائماً `ABS`، أي موجبة، والاتجاه يحمله النوع. أي قيمة في `amounts` تُحوَّل لقيمتها المطلقة كذلك |
| المبلغ صفر | **لا يُرسل**. يُسجَّل في اللوج بـ `ENQUEUED_FL=0` و`ENQUEUE_ERROR='ZERO_AMOUNT_SKIPPED'` |
| `PAYMENT_METHOD` | إذا كان `PAYMENT_TYPE_FK=1` فهو `CHEQUE`، وأي قيمة أخرى تعني `CASH` |
| `ORGANISATION_CODE` / `BRANCH_CODE` | المفتاح الأساسي في Oracle كنص، ويساوي `FIN_DIMENSION_VALUE.code`. المؤسسة الفارغة تصبح `"1"`، أما **الفرع الفارغ فيبقى فارغاً** فيُرفض الحدث في FIN |
| `docDate` | عمود التاريخ التجاري للصف، لا `SYSDATE` (D-15) |
| مفاتيح `fields` التي يقرأها FIN | فقط الحقول المسجّلة في MDL `FIN_EVENT_BUSINESS_FIELD`: `PAYMENT_METHOD`، `EXPENSE_TYPE_CODE`، `ORGANISATION_CODE`، `BRANCH_CODE`. الباقي للتتبّع فقط |

### 3.4 ما يحدث عند الفشل داخل Oracle
- `emit` **لا ترفع خطأً أبداً**، فحفظ المستخدم لا يتوقف. أي خطأ يُكتب في `ACCOUNTING_EVENT_LOG` بـ `ENQUEUED_FL=0` مع رسالة الخطأ، بمعاملة مستقلة لا تتأثر بالتراجع.
- **الخطر الوحيد الذي يوقف المستخدمين:** أن تصبح الحزمة **INVALID**، لأن التريجرات تستدعيها. ملف 2 يتوقف تلقائياً إذا حدث ذلك. **الإجراء العاجل:** تعطيل تريجرات `TRG_AE_*` (§8.10).

---

## 4. عقد الـ API بين الخدمة وFIN

- **الإرسال:** `POST /api/v1/fin/journal-entries/from-event`.
- **جسم الطلب** (`EventEntryBuildRequest`):
  - إلزامي: `eventReference` (100 حرف كحد أقصى)، `eventTypeCode` (50)، `docDate`، `baseAmount ≥ 0`.
  - اختياري: `amounts{}`، `fields{}`، `descriptionAr/En`.
- **النجاح:** `201` مع `data.journalEntryPk` و`data.docNo`.
- **الدخول:** `POST /api/v1/sec/auth/login` يعيد `accessToken`، وصلاحيته `expiresIn=3600` ثانية. التوكن المنتهي يعيد **401 `SEC-401-INVALID-CREDENTIALS`**.
- **مستخدم الخدمة:** `fin-consumer`، ودوره `FIN_EVENT_CONSUMER`، وصلاحياته `PERM_FIN_JOURNAL_ENTRIES_VIEW` و`_CREATE` فقط. الـ SEC يرفض صلاحية CREATE بدون VIEW (`SEC-409-NO-VIEW-GRANT`).
  - **تنبيه عند التحقق:** جسم فارغ يعيد 400 **قبل** فحص الصلاحيات، فلا يثبت أن الصلاحية تعمل. تحقّق بحدث سليم الشكل من نوع لا قاعدة له، والمتوقع `FIN-404-NO-ACTIVE-RULE`.

**أكواد الرد ومعناها:**

| الكود | المعنى |
|---|---|
| `FIN-409-DUPLICATE-EVENT` | رُحِّل من قبل، وتعامله الخدمة **كنجاح** |
| `FIN-409-PERIOD-NOT-OPEN` | الفترة المالية مقفلة |
| `FIN-404-NO-ACTIVE-RULE` | لا قاعدة فعّالة لهذا النوع |
| `FIN-422-UNMAPPED-VALUE` | لا mapping لهذه القيمة |
| `FIN-422-MISSING-BUSINESS-FIELD` | حقل تحتاجه القاعدة (للحساب أو للبُعد) غير موجود في الحدث |
| `FIN-422-UNRESOLVED-DIMENSION-VALUE` | قيمة البُعد غير معرّفة، مثل فرع جديد |
| `FIN-422-NO-DEFINED-PERIOD` | لا توجد فترة مالية تغطي التاريخ |
| `FIN-422-MISSING-AMOUNT-FIELD` | حقل مبلغ تحتاجه القاعدة غير موجود (D-22) |
| `FIN-422-NEGATIVE-AMOUNT-FIELD` | مبلغ سالب (D-22) |
| `FIN-422-REMAINDER-NOT-POSITIVE` | سطر الباقي ناتجه صفر أو أقل |
| `FIN-409-UNBALANCED` | القيد غير متوازن |

---

## 5. الخدمة `fin-consumer`

### 5.1 الملفات

| الملف | الوظيفة |
|---|---|
| `queue/OracleQueue.java` | `DBMS_AQ.DEQUEUE` عبر JDBC (الـ payload من نوع JSON، لذلك لا JMS). `visibility=ON_COMMIT` |
| `fin/EventTranslator.java` | تنقل الحقول المسموح بها فقط، وتحافظ على دقة المبالغ (BigDecimal)، وترفض مفاتيح المحاسبة وأي عدم تطابق بين `correlation` والمرجع |
| `fin/FinClient.java` | الدخول، والإرسال، وتصنيف الرد في مكان واحد (`classify`) |
| `loop/EventProcessor.java` | جدول القرار (§5.2) |
| `loop/ConsumerLoop.java` | خيط واحد، وقاطع دائرة، وإيقاف آمن (`SmartLifecycle`) |
| `loop/HeldEventService.java` | إعادة المحتجز تلقائياً، والـ replay اليدوي، وتفريغ الـ exception queue |
| `store/HeldEventStore.java` | جدول `FINC_HELD_EVENT` |
| `web/HeldEventController.java` | نقاط تشغيل للمشغّل، على `127.0.0.1:7373` فقط |
| `health/ConsumerHealthIndicator.java` | `/actuator/health` |

### 5.2 جدول القرار

| الرد | الإجراء | أين ينتهي الحدث |
|---|---|---|
| 201 | COMMIT | القيد في FIN |
| `FIN-409-DUPLICATE-EVENT` | COMMIT | نفس القيد السابق (يُعتبر نجاحاً) |
| `FIN-409-PERIOD-NOT-OPEN` | COMMIT مع حفظه **PARKED** في نفس المعاملة | يُعاد تلقائياً كل `FINC_PARKED_RETRY`، ويُرحَّل وحده عند فتح الفترة |
| 401 | دخول جديد وإعادة نفس الطلب مرة واحدة | — |
| 403، أو 401 بعد دخول جديد | ROLLBACK مع فتح الدائرة | خطأ في إعداد الخدمة، يُصلَّح الدور أو كلمة المرور |
| 5xx، 408، 429، انقطاع الاتصال، انتهاء المهلة | ROLLBACK مع فتح الدائرة: يتوقف السحب وتُجرَّب FIN بالدخول مع انتظار متزايد | تُقرأ الرسالة من جديد عند عودة FIN |
| نفس الفشل المؤقت للمرة الرابعة | COMMIT مع حفظه **PARKED / TRANSIENT_EXHAUSTED** | يُعاد تلقائياً |
| 400 / 404 / 422 / 409 أخرى | COMMIT مع حفظه **FAILED / REJECTED** ومعه رد FIN كاملاً | يُصلَح السبب ثم replay |
| payload معيب أو فيه مفتاح محاسبي | COMMIT مع حفظه **FAILED / TRANSLATION_DEFECT** | يُصلَح مصدر البيانات (Oracle) |
| رسالة في الـ exception queue | تُنقل وتُحفظ **FAILED / EXCEPTION_QUEUE** | replay |

### 5.3 `FIN_CONSUMER.FINC_HELD_EVENT`

| المجموعة | الأعمدة |
|---|---|
| التعريف | `HELD_EVENT_PK`، `EVENT_REFERENCE` (فريد)، `EVENT_TYPE_CODE`، `MSG_ID`، `PAYLOAD` (IS JSON) |
| الحالة | `HOLD_KIND` = PARKED أو FAILED. `REASON_CATEGORY` = PERIOD_NOT_OPEN أو TRANSIENT_EXHAUSTED أو REJECTED أو TRANSLATION_DEFECT أو EXCEPTION_QUEUE. `STATUS` = PENDING أو POSTED أو DISCARDED |
| رد FIN | `FIN_HTTP_STATUS`، `FIN_ERROR_CODE`، `FIN_ERROR_MESSAGE`، `FIN_RESPONSE` |
| الزمن والمحاولات | `ATTEMPTS`، `HELD_AT`، `LAST_ATTEMPT_AT` |
| الحل | `RESOLVED_AT`، `RESOLVED_ENTRY_ID`، `RESOLVED_BY` |

**لماذا في Oracle:** لأن الكتابة فيه تتم على نفس اتصال وعملية السحب من الطابور، فإما أن تحدث الاثنتان معاً أو لا تحدث أيهما. الصفوف لا تُحذف أبداً.

### 5.4 التشغيل والمراقبة
- **نقاط المشغّل:**
  - `GET /held-events?kind=FAILED|PARKED&status=PENDING|ALL`
  - `POST /held-events/{pk}/replay?by=<الاسم>`
  - `POST /held-events/replay?kind=FAILED&eventTypeCode=…`
  - `POST /held-events/{pk}/discard?by=…`
- **الصحة:** `/actuator/health`، وحالتها DOWN **فقط** إذا تعطّلت الخدمة نفسها. توقف FIN لا يجعلها DOWN، بل يظهر كـ `finReachable=false`. التفاصيل تشمل `queueDepth` و`parkedPending` و`failedPending` و`lastError`.
- **العدّادات:** `fin.consumer.events{outcome=posted|duplicate|retried|parked|failed|replayed}`، و`fin.consumer.queue.depth`، و`fin.consumer.held.pending{kind}`.
- **التتبّع:** كل سطر لوج يحمل `[eventReference]`.
- **الإعدادات:** كلها متغيرات بيئة `FINC_*`، وجدولها الكامل في `fin-consumer-README.md` §3. قيد مهم: `FINC_MAX_DELIVERY_ATTEMPTS` يجب أن يبقى **أقل من 5** (قيمة `max_retries` للطابور).

---

## 6. طبقة FIN: من يقرأ القاعدة ويطبّقها

### 6.1 خطوات `EventEntryService.build()`، كلها في معاملة واحدة
1. `existsByEventReference`، فإن وُجد المرجع: `FIN-409-DUPLICATE-EVENT`. وخلفه أيضاً فهرس فريد `UQ_FIN_JOURNAL_ENTRY_EVENT_REF`، فحتى السباق بين طلبين يعطي «مكرر».
2. القاعدة الفعّالة للنوع من `fin_event_type_rule`، وإلا `FIN-404-NO-ACTIVE-RULE`.
3. الفترة التي تغطي `docDate` من `fin_fiscal_period`، ويجب أن تكون مفتوحة.
4. لكل سطر في `fin_rule_line`:
   - **الحساب:** نوعه `CONSTANT`، فيُقرأ رمز الحساب من `account_derivation_value`. أو نوعه `MAPPING`، فيُبحث في `fin_account_mapping` بالمفتاح (`event_type_code`، `business_field_code`، `business_value`)، مثل (…, `PAYMENT_METHOD`, `CHEQUE`) الذي يعطي البنك.
   - **المبلغ:** `FIELD` يعني قيمة من `amounts` بالاسم. `PERCENTAGE` نسبة من `baseAmount`. `REMAINDER` هو الفرق، ويُحسب آخراً لكل جانب.
   - **سطر المبلغ صفر (غير الباقي):** يُحذف من القيد (D-22).
   - **الاتجاه:** `direction_code` كما هو مكتوب في القاعدة.
   - **الأبعاد:** من `fin_rule_line_dim`. إما قيمة ثابتة، أو من حقل في الحدث، فيُبحث عنها في `fin_dimension_value` بالرمز.
5. `JournalPostingService`: يتحقق من التوازن، ثم يعطي رقم مستند، ثم يرحّل.

### 6.2 الجداول

| الجدول | المفتاح والملاحظات |
|---|---|
| `fin_event_type_rule` | `event_type_code` **فريد على كل الصفوف**، حتى المعطّلة (انظر F-8) |
| `fin_rule_line` | `event_type_rule_id`، `line_no`، `account_derivation_type_code` (CONSTANT أو MAPPING)، `account_derivation_value`، `account_business_field_code`، `amount_source_type_code` (FIELD أو PERCENTAGE أو REMAINDER)، `amount_source_value`، `direction_code`، `distribution_type_code`، `is_remainder_fl` |
| `fin_rule_line_dim` | `rule_line_id`، `dimension_id`، `value_source_code` (CONSTANT أو BUSINESS_FIELD)، `business_field_code`، `dimension_value_id` |
| `fin_account_mapping` | (`event_type_code`، `business_field_code`، `business_value`) → `account_id`. مسموح بـ mapping **فعّال واحد** لكل مفتاح، والحساب يجب أن يكون ورقة (leaf) فعّالة |
| `fin_account` | `code`، `name_ar`، `account_type_code`، `nature_code`، `parent_account_id`، `is_active_fl` |
| `fin_dimension` / `fin_dimension_value` | الأبعاد ORG (id=1) وBRANCH (id=2)، والقيم برموز Oracle |
| `fin_fiscal_year` / `fin_fiscal_period` | الحالات OPEN، SOFT_CLOSE، HARD_CLOSE، YEAR_END_CLOSE. **فقط SOFT_CLOSE يمكن إعادة فتحها** |
| `fin_journal_entry` / `fin_journal_line` / `fin_journal_line_dim` | الناتج. `event_reference` فريد، و`amount > 0` بقيد CHECK |
| قوائم MDL | `ACCOUNTING_EVENT_TYPE` (كل نوع حدث يجب أن يكون مسجّلاً فيها)، `FIN_EVENT_BUSINESS_FIELD`، `PAYMENT_METHOD` |

### 6.3 القواعد الحالية (أنواع SYSTEM، والتوأم `_REVERSED` مرآة معكوسة الاتجاه لكل منها)
الأبعاد في كل سطر: ORG من `ORGANISATION_CODE`، وBRANCH من `BRANCH_CODE`. والمبلغ في كل الأسطر `FIELD:amount`، إلا في `PURCHASE_INVOICE`.

| النوع | مدين | دائن |
|---|---|---|
> **الأكواد أدناه بعد إعادة هيكلة الشجرة (Flyway V40، 2026-09-26)**: فئة (1) / مجموعة (2) / حساب (4) / فرعي (6). المرجع: `governance/project-artifacts/fin-chart-of-accounts-review.md`.

| النوع | مدين | دائن |
|---|---|---|
| `INSTALLMENT_PAYMENT_RECEIVED` | MAPPING(PAYMENT_METHOD) | ذمم عملاء التقسيط - منتظمة `110201` |
| `COURT_INSTALLMENT_PAYMENT` | MAPPING(PAYMENT_METHOD) | `110201` |
| `LAWYER_FEE_PAYMENT` | MAPPING(PAYMENT_METHOD) | إيرادات استرداد أتعاب قضائية `4403` |
| `INVESTOR_FEE` | MAPPING(PAYMENT_METHOD) | جاري المستثمرين `210201` |
| `INVESTOR_SUPPORT_OR_DRAWING`، `LAWYER_ADVANCE`، `INVESTOR_FEE_2_5`، `EXECUTION_ACTION_FEE`، `BAEETHA_FEE`، `CIVIL_FEE` | جاري المستثمرين `210201` | MAPPING(PAYMENT_METHOD) |
| `INVESTOR_SALES_INVOICE` | `110201` | `210201` |
| `CONTRACT_CREATED` | `210201` | مبيعات التقسيط `4101` |
| `CONTRACT_SETTLEMENT` | خصومات مخالصات العقود `4901` (مقابل إيراد) | `110201` |
| `COMPLAINT_SETTLEMENT` | ديون معدومة ومخالصات عملاء متعثرين `6602` | `110201` |
| `PURCHASE_INVOICE` | مشتريات بضاعة `5102` ← `grossAmount` | MAPPING(PAYMENT_METHOD) ← `paidAmount`، وموردون محليون `210101` ← `remainingAmount` (يُحذف إن كان صفراً) |
| `EXPENSE_PAID` | MAPPING(EXPENSE_TYPE_CODE) | MAPPING(PAYMENT_METHOD) |
| `REVENUE_RECEIVED` | MAPPING(PAYMENT_METHOD) | MAPPING(EXPENSE_TYPE_CODE) |
| `CUSTODY_RECEIVED` | MAPPING(PAYMENT_METHOD) | MAPPING(EXPENSE_TYPE_CODE) |
| `CUSTODY_PAID_OUT` | MAPPING(EXPENSE_TYPE_CODE) | MAPPING(PAYMENT_METHOD) |
| `SAFE_TRANSFER_TO_SAFE` | الصندوق الرئيسي `110101` | البنك التجاري `110111` |
| `SAFE_TRANSFER_TO_BANK` | `110111` | `110101` |

**الـ mappings** (تشير إلى الحساب بالـ id، فانتقلت مع إعادة الترميز تلقائيًا):

| الحقل | القيمة ← الحساب |
|---|---|
| `PAYMENT_METHOD` (لكل الأنواع التي تستخدمه) | `CASH` ← الصندوق الرئيسي `110101`، `CHEQUE` ← البنك التجاري `110111` |
| `EXPENSE_TYPE_CODE` في `EXPENSE_PAID` | 61 مصروفات أخرى ← `6899`، 81 مرتبات ← رواتب أساسية `6101`، 101 مصروفات عمومية ← `6801`، 161 إيجارات ← `6201` |
| `EXPENSE_TYPE_CODE` في `REVENUE_RECEIVED` | 1 إيرادات عقود ← `4401`، 181 إيرادات تسديد عملاء ← `4402`، 561 إيرادات أخرى ← `4501` |
| `EXPENSE_TYPE_CODE` في `CUSTODY_RECEIVED` | 324 سداد قرضات ← قروض ممنوحة للمستثمرين `110307`، 381 عهدة المحاماة ← `110303`، 401 سداد مسحوبات علوش ← جاري الشريك - علوش `340101`، 422 تحويلات ← `110103` |
| `EXPENSE_TYPE_CODE` في `CUSTODY_PAID_OUT` | 281 سحوبات علوش ← `340101`، 282 قرضات للمستثمرين ← `110307`، 382 عهدة المحاماة ← `110303`، 424 تحويلات ← `110103` |

الأنواع 361 و362 و363 **بلا mapping**، والقرار فيها للمالك (D-11).

---

## 7. رحلة حدث واحد: مثال للتتبّع

عميل سدّد 96 بشيك. الصف 39766، ونوعه FL=7، والفرع 1.

1. **التريجر:** `TRG_AE_LOAN_PAYMENT` عند الـ INSERT يستدعي `emit` بنوع `INSTALLMENT_PAYMENT_RECEIVED`.
2. **اللوج:** يُكتب صف في `ACCOUNTING_EVENT_LOG` بالمرجع `LEGACY:LOAN_PAYMENT:39766:19` و`ENQUEUED_FL=1`، وتدخل رسالة الطابور بنفس الـ correlation.
3. **COMMIT المستخدم:** تصبح الرسالة READY.
4. **الخدمة:** تسحب الرسالة، وتترجمها، وترسلها بـ POST، فيأتي الرد 201 (`JV-2026-000001`)، فتعمل COMMIT.
5. **FIN:** السطر 1 مدين، وحسابه عبر MAPPING(PAYMENT_METHOD=CHEQUE) البنك التجارى، بمبلغ 96. السطر 2 دائن على عملاء محليين بمبلغ 96. الأبعاد ORG=1 وBRANCH=1، والفترة سبتمبر.

**تعديل القيمة لاحقاً من 96 إلى 97:**
- يصدر حدث `…_REVERSED` بقيمة 96، ويحمل `reversesEventReference=…:19`، فتعكس القاعدة التوأم القيد السابق.
- ثم يصدر حدث جديد بقيمة 97.
- **الصافي = +1 فقط.**

**استعلامات التتبّع بالمرجع:**
```sql
-- Oracle
SELECT * FROM LOAN_SYS.ACCOUNTING_EVENT_LOG WHERE event_reference = :ref;
SELECT msg_state, retry_count FROM LOAN_SYS.AQ$ACCOUNTING_EVENT_Q WHERE corr_id = :ref;
SELECT * FROM FIN_CONSUMER.FINC_HELD_EVENT WHERE event_reference = :ref;
-- PostgreSQL (FIN)
SELECT je.doc_no, jl.direction_code, a.code, a.name_ar, jl.amount
  FROM fin_journal_entry je JOIN fin_journal_line jl ON jl.journal_entry_id = je.journal_entry_pk
  JOIN fin_account a ON a.account_pk = jl.account_id WHERE je.event_reference = :ref;
```
```bash
grep '<ref>' fin-consumer/logs/fin-consumer.log   # سطور الخدمة للحدث نفسه
```

---

## 8. أدلة الإجراءات

### 8.0 القاعدة العامة لأي تعديل
1. **FIN أولاً:** القاعدة، أو الـ mapping، أو الحساب، أو قيمة MDL، أو البُعد.
2. **Oracle ثانياً:** التريجر أو الحزمة.
3. **اختبار على النسخة التجريبية:** معاملة ثم ROLLBACK (§8.9).
4. **الحية:** في نافذة صيانة، بعد نسخة احتياطية، وبالأمر نفسه الذي نجح في التجربة.

لو عكست الترتيب، تُرفض الأحداث التي تصل في الفترة الفاصلة وتُحفظ FAILED. لا يضيع شيء، لكنها تحتاج replay.

### 8.1 إضافة حقل جديد إلى الـ payload
1. في التريجر: `v_fields.put(...)` عبر معامل جديد في `emit`، أو `v_amounts.put('newAmount', …)`. وإذا مرّرت `p_amounts` **فأضف `amount` صراحةً**، لأن الحزمة لا تضيفه تلقائياً في هذه الحالة.
2. أعد تطبيق ملف 3، وملف 2 أيضاً إن تغيّرت دالة `emit` نفسها.
3. **الأثر:** لا شيء حتى تستخدمه قاعدة. الخدمة تمرّره دون تعديل.
4. **إن كان الحقل سيُستخدم لاختيار حساب أو بُعد:** سجّله في MDL `FIN_EVENT_BUSINESS_FIELD` أولاً.
5. **ممنوع:** أي اسم يوحي بحساب (الثابت 1).

### 8.2 إعادة تسمية حقل أو حذفه
**لا تفعل ذلك إذا كانت قاعدة تستخدمه.** قواعد FIN لا تُعدَّل (F-8)، فكل أحداث ذلك النوع ستُرفض. البديل: أضف الحقل الجديد، وأبقِ القديم.

### 8.3 نوع مصروف أو إيراد جديد في النظام القديم
أضِف mapping عبر `POST /api/v1/fin/account-mappings` بالشكل `{eventTypeCode, businessFieldCode:"EXPENSE_TYPE_CODE", businessValue:"<id>", accountId}`، **للنوع وتوأمه `_REVERSED` كليهما**. إن نسيت، تُرفض الأحداث بـ `FIN-422-UNMAPPED-VALUE`، ويكفي بعد الإضافة replay.

### 8.4 طريقة دفع جديدة
1. عدّل `payment_method()` في الحزمة.
2. أضف القيمة في MDL `PAYMENT_METHOD`.
3. أضف mapping لكل نوع يستخدم `MAPPING(PAYMENT_METHOD)`، ولتوائمها.

### 8.5 نوع حدث جديد (جدول جديد، أو FL جديد)
1. **FIN:**
   - قيمة MDL في `ACCOUNTING_EVENT_TYPE` للنوع ولتوأمه `_REVERSED`.
   - `POST /api/v1/fin/event-rules` ثم `POST /event-rules/{id}/lines` لكل سطر، مع الأبعاد.
   - قاعدة التوأم بأسطر معكوسة الاتجاه.
   - الـ mappings.
2. **Oracle:** أضف الفرع إلى `type_of()` في التريجر، أو تريجراً جديداً على نمط الموجود:
   - `AFTER INSERT OR UPDATE OF <الأعمدة المراقبة> OR DELETE`.
   - عند UPDATE: إلغاء القديم وإثبات الجديد.
   - **لا استعلام** عن الجدول نفسه داخل تريجره (D-21).
3. **تحقّق أن الحقول التي تقرؤها القاعدة موجودة في الـ payload.**

### 8.6 تغيير حسابات نوع موجود
- **إن كان السطر MAPPING:** عطّل الـ mapping القديم (`PUT /account-mappings/{id}/deactivate`)، ثم أضف الجديد. سهل، ولا يمس القاعدة.
- **إن كان السطر CONSTANT أو كان المطلوب تغيير شكل القيد:** **لا يمكن عبر FIN** (F-8). الفهرس الفريد على `event_type_code` يمنع إنشاء قاعدة بديلة حتى بعد التعطيل، ولا يوجد API لحذف سطر أو تعديله. الحل يحتاج قراراً: إما تعديل FIN (فهرس جزئي `WHERE is_active_fl` مع فحص للقواعد الفعّالة فقط)، أو migration بـ SQL. **لا تُضف أسطراً على قاعدة مزروعة كحل مؤقت.**
- **الأثر على الإلغاء:** التوأم يعيد اشتقاق الحساب من الإعدادات **الحالية**، فإلغاء قيد قديم بعد تغيير الـ mapping لا يكون مرآة دقيقة (Q-3).

### 8.7 قيد مركّب (أكثر من سطرين)
- سطر أو أكثر من نوع `FIELD` بمبالغ مسمّاة، مع سطر `REMAINDER` واحد عند الحاجة، ويكون `distributionTypeCode=REMAINDER` و`isRemainderFl=true`.
- **تنبيه:** سطر الباقي لا يقبل صفراً (يعطي `FIN-422-REMAINDER-NOT-POSITIVE`). إن كان الجزء قد يكون صفراً، أرسله كمبلغ مسمّى في سطر `FIELD`، فيُحذف تلقائياً عند الصفر (D-22).
- مُختبر: بيع بالتقسيط، مدين العملاء بالإجمالي، ودائن المبيعات بالسعر النقدي، ودائن الأرباح المؤجلة بالباقي. التفاصيل في `docs/test-report.md` §Compound.

### 8.8 حل المشاكل حسب العَرَض

| العَرَض | افحص | السبب المعتاد والحل |
|---|---|---|
| عمق الطابور يزيد و`finReachable=false` | `lastError` في الصحة | FIN متوقف، أو كلمة المرور أو الدور (401/403). الأحداث آمنة في الطابور |
| عمق الطابور يزيد و`finReachable=true` ولا ترحيل | لوج الخدمة | خطأ Oracle في الحلقة، راجع صلاحيات `FIN_CONSUMER` |
| `parkedPending` يزيد | `/held-events?kind=PARKED` | فترة مقفلة: افتحها في FIN، ويُرحَّل وحده |
| `failedPending` > 0 | `/held-events?kind=FAILED`، العمودان `fin_error_code` و`fin_error_message` | mapping أو قاعدة أو بُعد ناقص: أصلحه ثم replay |
| حدث معيّن لم يصل FIN | `ACCOUNTING_EVENT_LOG` بالمصدر | `ENQUEUED_FL=0` يعني فشل الإرسال في Oracle، والخطأ في `ENQUEUE_ERROR`. أعد إرساله بعد الإصلاح |
| لا يوجد صف لوج أصلاً للعملية | حالة التريجرات، و`set_enabled` | التريجر معطّل، أو القيمة لم تتغيّر، أو النوع غير مُعرَّف في `type_of`، أو المبلغ صفر |
| حفظ المستخدم يفشل على أحد الجداول السبعة | حالة `PKG_ACCOUNTING_EVENT` | الحزمة **INVALID**: نفّذ §8.10 فوراً ثم أصلحها |
| `RETRYEXPIRED` في `AQ$ACCOUNTING_EVENT_Q` | — | رسائل أُرسلت بدون `exception_queue`، أي بحزمة أقدم من D-20. أعد تطبيق ملف 2 |
| `queueDepth = -1` | صلاحية EXECUTE على `FINC_QUEUE_DEPTH` | أعد تشغيل `fin-consumer/oracle/02` |
| FIN يرد `DATA_INTEGRITY_VIOLATION` | لوج الـ backend | قيد CHECK أو فهرس فريد في قاعدة البيانات. الحدث محفوظ FAILED، والإصلاح عادة في الإعدادات |

### 8.9 الاختبار الآمن على نسخة فيها بيانات حقيقية
- عدّل صفاً حقيقياً داخل معاملة، والتقط الـ payload من `ACCOUNTING_EVENT_LOG`، ثم **ROLLBACK**. البيانات لا تتغيّر.
- لتمرير الـ payload عبر المسار كاملاً: أرسله للطابور بمرجع اختبار (مثل `GEN:`)، وافحص قيد FIN وميزان المراجعة.
- قيود الاختبار تُعرَف ببادئة مراجعها.

### 8.10 الطوارئ: إيقاف الإرسال فوراً دون إيقاف المستخدمين
```sql
ALTER TRIGGER TRG_AE_LOAN_PAYMENT DISABLE;   ALTER TRIGGER TRG_AE_CONTRACT DISABLE;
ALTER TRIGGER TRG_AE_COMPLAINTS DISABLE;     ALTER TRIGGER TRG_AE_COMPLAINT_DT DISABLE;
ALTER TRIGGER TRG_AE_INVOICE_IMPORT DISABLE; ALTER TRIGGER TRG_AE_EXPENSE_TYPE_DT DISABLE;
ALTER TRIGGER TRG_AE_TRANSFER_SAFE DISABLE;
```
العمليات التي تُحفظ أثناء التعطيل **لا تصدر لها أحداث**، وتُسترجع لاحقاً بالـ replay من الجداول التجارية بـ `p_source_action => 'REPLAY'`.

### 8.11 إعادة تشغيل حدث محتجز
```bash
curl -s 'localhost:7373/held-events?kind=FAILED'
curl -s -X POST 'localhost:7373/held-events/<pk>/replay?by=<الاسم>'
curl -s -X POST 'localhost:7373/held-events/replay?kind=FAILED&eventTypeCode=EXPENSE_PAID&by=<الاسم>'
curl -s -X POST 'localhost:7373/held-events/<pk>/discard?by=<الاسم:السبب>'
```
الـ replay آمن للتكرار، فإن كان الحدث قد رُحِّل يأتي الرد «مكرر».

### 8.12 تطبيق ملفات Oracle
- **التشغيل:** بـ sqlplus فقط، لا بأداة تقسيم يدوية. الأمر: `docker exec <c> sqlplus -S USER/PWD@//host:1521/<service> @/tmp/file.sql`.
- **الترتيب:**
  1. صلاحيات SYS: `GRANT EXECUTE ON SYS.DBMS_AQ TO LOAN_SYS`. لا يمكن أن يمنحها المستخدم لنفسه (ORA-01749).
  2. ملف 1، ثم 2، ثم 3.
  3. تهيئة الخدمة: الملفات 01 ثم 02 ثم 03، ثم سكربت مستخدم FIN.
- **وضع الظل:** تبقى تريجرات المحاسبة القديمة **ENABLED** بجوار الجديدة، وتُقارن النتائج. **بعد** نجاح المقارنة فقط يُشغَّل `legacy-accounting-stop.sql`.
- **ملف 2:** يعيد تكميل التريجرات بنفسه، ويتوقف إن أصبحت الحزمة INVALID.

---

## 9. الحالة الحالية والقيود المعروفة

| البند | الحالة |
|---|---|
| **إصدار القاعدة الحية** | **Oracle 19c على Docker، وفيها بيانات حية.** الملفات الحالية بُنيت واختُبرت على 26ai (23.26)، وهي **غير جاهزة لـ 19c**، لأنها تستخدم TxEventQ ونوع البيانات `JSON` و`CREATE_EQ_EXCEPTION_QUEUE` و`.to_json()`، وكلها غير موجودة في 19c. **التحويل مطلوب:** طابور AQ كلاسيكي برسالة من نوع كائن فيه CLOB، وعمود `CLOB IS JSON` للوج، وقراءة CLOB في الخدمة، واسم PDB كمتغير. ثم **اختبار على 19c حقيقية**. الفحص الأوّلي بـ `oracle-19c-readiness-check.sql` (قراءة فقط) |
| نسخة الاختبار | `LOAN_SYS@FREEPDB1` على 26ai، وهي نسخة من بيانات الإنتاج. فيها بقايا اختبار بمراجع `E2E_`، `GEN:`، `CMP:`، `REG:` |
| الاختبارات | 11 سيناريو، والاختبار العام 54/54، واختبار القيود المركّبة، وكلها ناجحة على 26ai |
| F-8 | استبدال قاعدة غير ممكن عبر FIN، وينتظر قرار المالك أو الـ factory |
| Q-3 | الإلغاء بعد تغيير mapping لا يكون مرآة دقيقة للقيد الأصلي |
| D-11 | حسابات أنواع المصروفات 361 و362 و363 لم تُحدَّد بعد |
| الخطوة التالية | replay لعام 2026 من علامة التوقف (2026-09-23 14:17 UTC)، مع الأرصدة الافتتاحية |

## 10. سجل القرارات المرتبطة بالمسار (مختصر)

| القرار | المضمون |
|---|---|
| D-01 | القيود تُرحَّل على الحساب الورقة لا الأب، فـ 32 العملاء تصبح 54 عملاء محليين |
| D-07 | FL 8 و130 ملغاة، لأنها لم تُستخدم قط |
| D-14 | الإشارة الطبيعية سالبة لبعض الأنواع، والاتجاه يُعكس عبر `_REVERSED` |
| D-15 | `docDate` هو التاريخ التجاري للصف |
| D-16 | الإلغاء يحمل `reversesEventReference`، والتوأم هو المسار المعتمد |
| D-17 | تعديل التاريخ أو المؤسسة أو الفرع يعيد الإرسال |
| D-18 | طبيعة حسابات العملاء مدينة |
| D-19 | قاعدة الانعكاس XOR لا OR |
| D-20 | `exception_queue` تُسمّى في كل رسالة، والـ exception queue منشأة |
| D-21 | لا استعلام عن الجدول نفسه داخل تريجره (ORA-04091) |
| D-22 | FIN يحذف سطر FIELD الذي قيمته صفر، ويعطي كوداً واضحاً للمبلغ الناقص أو السالب |
| D-23 | أحداث العقد تحمل `cashPrice` و`totalContract` |
