<!-- SRS — Governed by SRS Governance Engine (Project 1) | PART A + PART B -->

# وثيقة التحليل (SRS)
## خدمة الإشعارات | Notification Service (NOTIF)

---

# PART A — MODULE FOUNDATION

## A1 — معلومات الوثيقة (Document Information)

| البند | القيمة |
|---|---|
| **اسم المشروع** | منصة Foundation (Domain: ERP) |
| **الموديول** | خدمة الإشعارات (Notification Service) |
| **Feature Code** | NOTIF-001 |
| **Feature Type** | Transactional (NotificationLog) + Configuration (Template, ChannelConfig) |
| **الطبقة / النوع** | L1 · Service · dep: CU, SEC, FILE |
| **النسخة** | 1.2 (حُسِم OQ-NOTIF-001 + تطبيق حسم OQ-SEC-001) |
| **التاريخ** | 2026-09-02 |
| **الحالة** | Draft |
| **Open Questions** | None (OQ-NOTIF-001 RESOLVED) — see OQ Log |
| **Governed by** | SRS Governance Engine (Project 1) |
| **Deployment Surface** | **Backend + Frontend** — service + in-process event listener + شاشات إدارة |

## A2 — السياق الوظيفي (Functional Context)

### ما يشمله هذا الموديول
> أساس إشعارات متعدد القنوات: خمس قنوات مبنية ومُفعّلة (EMAIL/SMS/WHATSAPP/PUSH/INTERNAL)، تصميم موحّد بمميّز `channel_type`، والموديول المُرسِل يختار القنوات عبر `channelHint`، وتبقى الخدمة محايدة وتتفرّع لصفّ سجل لكل قناة — **مع شاشات إدارة للقوالب والقنوات وسجل الإشعارات**. المصدر: module-registry-NOTIF §SCOPE NOTE.

### ما لا يشمله هذا الموديول
> منطق أعمال المُرسِل · وسيط رسائل خارجي (RabbitMQ — تُستخدَم أحداث CU) · إطار تكامل ثقيل (Camel — JavaMailSender مباشرة) · التحقق من JWT ذاتياً · نصّ القوالب في ملفات خارجية (inline؛ file_id للمرفقات) · Workflow Engine (RULE-13=OFF).

### وظيفة الموديول
> استقبال طلب إشعار (حدث CU أو API)، حلّه مقابل قالب ثنائي اللغة، التفرّع لصفوف سجل لكل قناة، الإرسال عبر مزوّد كل قناة مع إعادة محاولات محكومة، وتسجيل الحالة النهائية — مع شاشات لإدارة القوالب/القنوات ومتابعة السجل.

### الوصف الوظيفي التفصيلي
> يستهلك NOTIF هوية المستلِم (UserAccount) من Security SOFT، وخدمة الملفات (file_id) للمرفقات. القناة المعطّلة → CHANNEL_DISABLED دون محاولة. عند الفشل تُعاد المحاولة حتى 5 مرات (2ث، تراجع 1.5×) ثم FAILED. **المزوّد الفعلي لكل قناة قرار P3** (تصميم مستقل عن المزوّد — OQ-NOTIF-001 محسوم). تعقيد متوسط.

### ملاحظات عامة
- **قرار P1:** NotificationLog حركي **تقني** — لا `fiscal_year_id`/`period_id` — انحراف موثّق عن 5.4.2.
- **قرار P1:** لا Business Code (كلها داخلية — BC-RULE-0 غير منطبق).
- **قرار P1:** اعتماد المزوّد في `configJson` لا في الكود (reg §SCOPE).
- **قرار Architect (2026-09-02) — حسم OQ-NOTIF-001:** التصميم يبقى **مستقلاً عن المزوّد**؛ اختيار المزوّد الفعلي (SMTP/SMS/WhatsApp/Push) مُفوَّض إلى P3 دون أي أثر على الجداول أو الـ SRS.
- **قرار Architect (2026-09-02) — تطبيق حسم OQ-SEC-001:** المستلِم غير النشط لا يُرسَل إليه (RULE-NOTIF-007)؛ سجلات الإشعارات التاريخية تُبقى.

## A3 — الكيانات والحقول (Entities & Fields)
*(POSTGRESQL_16 — audit مطوي: createdBy/At, updatedBy/At)*

### ENTITY-NOTIF-001 — NotificationLog (سجل الإشعار)
| PRIVATE · Transactional (صف/قناة/حدث) | BC: NO | module-registry-NOTIF; prd-NOTIF US-NOTIF-001/002 | Create(fan-out), Read/Query, Retry(internal) |
| Cross-Module: recipientId → UserAccount (SEC) SOFT-READ |

| الحقل | النوع | إلزامي | القيم/المصدر | ملاحظات | Label-AR | Label-EN |
|---|---|---|---|---|---|---|
| notificationLogPk | BIGINT (PK) | نظام | Sequence | — | المعرف | ID |
| recipientId | BIGINT | نعم | UserAccount (SEC) SOFT | هوية المستلِم | المستلِم | Recipient |
| channelTypeId | VARCHAR(20) | نعم | LOV-NOTIF-001 | code | القناة | Channel |
| notificationStatusId | VARCHAR(30) | نعم | LOV-NOTIF-002 | دورة الحياة (A6) | الحالة | Status |
| templateFk | BIGINT (FK) | نعم | ENTITY-NOTIF-002 | القالب | القالب | Template |
| moduleCode | VARCHAR(50) | نعم | — | الموديول المُرسِل | رمز الموديول | Module Code |
| referenceId | BIGINT | لا | — | مرجع الكيان المصدر | معرّف المرجع | Reference ID |
| referenceType | VARCHAR(100) | لا | — | نوعه | نوع المرجع | Reference Type |
| retryCount | SMALLINT | نظام | 0 | RULE-NOTIF-002 | عدد المحاولات | Retry Count |
| errorMessage | TEXT | لا | — | سبب الفشل | رسالة الخطأ | Error Message |
| sentAt | TIMESTAMP | لا | — | وقت الإرسال | تاريخ الإرسال | Sent At |

### ENTITY-NOTIF-002 — NotificationTemplate (قالب الإشعار)
| PRIVATE · Config/Master | BC: NO | module-registry-NOTIF; prd-NOTIF US-NOTIF-003 | Create, Read, Update, Deactivate |

| الحقل | النوع | إلزامي | القيم | ملاحظات | Label-AR | Label-EN |
|---|---|---|---|---|---|---|
| notificationTemplatePk | BIGINT (PK) | نظام | Sequence | — | المعرف | ID |
| templateCode | VARCHAR(80) | نعم | UNIQUE | مفتاح القالب | رمز القالب | Template Code |
| nameAr / nameEn | VARCHAR | نعم | — | — | الاسم | Name |
| subjectAr / subjectEn | VARCHAR(300) | لا | — | لعنوان البريد | العنوان | Subject |
| bodyAr | TEXT | نعم | — | نص عربي | المتن (عربي) | Body (AR) |
| bodyEn | TEXT | نعم | — | نص إنجليزي | المتن (إنجليزي) | Body (EN) |
| attachmentFileId | BIGINT | لا | File Service (API) | مرفق اختياري | مرفق | Attachment |
| isActiveFl | SMALLINT | نعم | 1/0 | ⚠ Fl | نشط | Active |

### ENTITY-NOTIF-003 — NotificationChannelConfig (تهيئة القناة)
| PRIVATE · Config | BC: NO | module-registry-NOTIF; prd-NOTIF US-NOTIF-004 | Create, Read, Update(enable/disable), Deactivate |

| الحقل | النوع | إلزامي | القيم | ملاحظات | Label-AR | Label-EN |
|---|---|---|---|---|---|---|
| notificationChannelConfigPk | BIGINT (PK) | نظام | Sequence | — | المعرف | ID |
| channelTypeId | VARCHAR(20) | نعم | LOV-NOTIF-001 · UNIQUE | قناة فريدة | القناة | Channel |
| isEnabledFl | SMALLINT | نعم | 1/0 | تفعيل وقت التشغيل | مُفعّلة | Enabled |
| configJson | TEXT | لا | — | اعتماد المزوّد (JSON) — المزوّد الفعلي قرار P3 | تهيئة المزوّد | Provider Config |

## A4 — قواعد التحقق (Business Rules)
| RULE-ID | Scope | Trigger | Statement | Message-AR | Message-EN | Source |
|---|---|---|---|---|---|---|
| RULE-NOTIF-001 | NOTIF-001 | Dispatch | fan out one log per requested channel (channelHint); no business routing inside. | سجل لكل قناة مطلوبة. | One log per requested channel. | POLICY-CLI-02 + reg |
| RULE-NOTIF-002 | NOTIF-001 | Failure | retry ≤5 (2s, ×1.5), then FAILED. | إعادة حتى ٥ ثم فشل. | Retry ≤5 then FAILED. | biz-pol POLICY-CLI-03 |
| RULE-NOTIF-003 | NOTIF-001 | Dispatch | disabled channel → CHANNEL_DISABLED, no retry. | القناة المعطّلة تُسجَّل دون محاولة. | Disabled channel logged, not retried. | biz-pol POLICY-CLI-03 |
| RULE-NOTIF-004 | NOTIF-002 | Compose | bilingual templates; attachment via File `file_id`. | قوالب ثنائية اللغة والمرفق عبر الملفات. | Bilingual templates; File attachment. | reg §AUTO |
| RULE-NOTIF-005 | NOTIF-001 | Access | delegate auth to Security filter. | — (معماري) | — (architecture) | reg §AUTO |
| RULE-NOTIF-006 | NOTIF-002/003 | إنشاء | unique `templateCode`; unique channel/config. | رمز القالب والقناة فريدان. | Template code & channel unique. | integrity |
| **RULE-NOTIF-007** | NOTIF-001 | Dispatch | MUST NOT dispatch to a recipient whose UserAccount is inactive (skip); historical logs retained. | لا يُرسَل إشعار لمستلِم حسابه غير نشط؛ تُبقى السجلات التاريخية. | Must not dispatch to an inactive recipient; history retained. | **حسم OQ-SEC-001 (consumer-side)** |

## A5 — قوائم القيم (LOV / Lookup)
> لا MD_MASTER_LOOKUP مركزي — قوائم **محلية** لـ NOTIF. القيمة = code، runtime-loaded، لا ENUMs.

**LOV-NOTIF-001 — NotificationChannel** (`channelTypeId`, NOTIF-001/003, Dropdown, `NOTIF_CHANNEL`): EMAIL=بريد · SMS=رسالة نصية · WHATSAPP=واتساب · PUSH=إشعار فوري · INTERNAL=داخلي.

**LOV-NOTIF-002 — NotificationStatus** (`notificationStatusId`, NOTIF-001, Dropdown, `NOTIF_STATUS`): PENDING=قيد الانتظار · SENT=مُرسَل · FAILED=فشل · CHANNEL_DISABLED=القناة معطّلة.

## A6 — دورة الحالة (Status Lifecycle) — NotificationLog
```
[PENDING] ──ناجح──► [SENT] ✓ | ──فشل بعد المحاولات──► [FAILED] ✗ | ──قناة معطّلة──► [CHANNEL_DISABLED] ⊘
```
> RULE-13 — لا Workflow.

## A7 — تبعيات الموديولات (Module Dependencies)
### الكيانات المُستهلَكة
| الكيان | ENTITY-ID | المالك | الاعتمادية | XM Candidate |
|---|---|---|---|---|
| UserAccount | ENTITY-SEC-001 | SEC | SOFT-READ (المستلِم) | نعم → XM-NOTIF-N (MODE 1.5) |
> سلوك المستلِم غير النشط محسوم: RULE-NOTIF-007 (لا إرسال؛ تُبقى السجلات) — تطبيق حسم OQ-SEC-001.

### الخدمات والتكاملات
| CU: Events/config/exceptions — USES(library) · SEC: هوية المستلِم — SOFT · FILE: مرفقات (file_id) — SOFT/service (XM candidate) · مزوّدو القنوات: عبر configJson — المزوّد الفعلي قرار P3 (OQ-NOTIF-001 محسوم) |
> master-registry §8: «NOTIF → SEC : SOFT»، «NOTIF → FILE : SOFT/service».

---

# ══════════════════════════════════════════════════════════
# PART B — SCREEN SPECIFICATIONS (Frontend: React/TS/Vite)
# ══════════════════════════════════════════════════════════

---

## SCR-NOTIF-001 — إدارة قوالب الإشعارات (Templates)
### B1
| SCR-NOTIF-001 | PATTERN-2 (SIDE_DRAWER) | كيان config بسيط (5.8.2) | ONE (CORE-9) | ENTITY-NOTIF-002 | page_code `NOTIF_TEMPLATES` (parent: الإشعارات) |

### B3
- قائمة/فلاتر: templateCode, nameAr, isActiveFl.
- تحرير (Drawer): templateCode, nameAr, nameEn, subjectAr, subjectEn, bodyAr, bodyEn, attachmentFileId (عبر خدمة الملفات), isActiveFl → A3.
- القواعد: حفظ → RULE-NOTIF-004, 006.

### B4 (CORE-9 / SEC-3) — `page_code=NOTIF_TEMPLATES`
> Security Engine يولّد الصلاحيات الأربع تلقائياً. لا seed لـ PERM_*.

| SCR-NOTIF-001 | VIEW NOTIF_ADMIN | CREATE NOTIF_ADMIN | UPDATE NOTIF_ADMIN | DELETE NOTIF_ADMIN |

### B5
API-NOTIF-004, 006.

---

## SCR-NOTIF-002 — تهيئة القنوات (Channel Configuration)
### B1
| SCR-NOTIF-002 | PATTERN-2 (SIDE_DRAWER) | كيان config (تفعيل + JSON) | ONE (CORE-9) | ENTITY-NOTIF-003 | page_code `NOTIF_CHANNELS` (parent: الإشعارات) |

### B3
- قائمة/فلاتر: channelTypeId (LOV-NOTIF-001), isEnabledFl.
- تحرير (Drawer): channelTypeId, isEnabledFl, configJson → A3.
- القواعد: حفظ → RULE-NOTIF-006 · التعطيل → RULE-NOTIF-003.

### B4 (CORE-9 / SEC-3) — `page_code=NOTIF_CHANNELS`
| SCR-NOTIF-002 | VIEW NOTIF_ADMIN | CREATE NOTIF_ADMIN | UPDATE NOTIF_ADMIN | DELETE NOTIF_ADMIN |

### B5
API-NOTIF-005, 006.

---

## SCR-NOTIF-003 — سجل الإشعارات (Notification Log — Read-only)
### B1
| SCR-NOTIF-003 | PATTERN-2 (SIDE_DRAWER) | سجل عرض فقط — قائمة + Drawer تفاصيل | ONE (CORE-9) | ENTITY-NOTIF-001 | page_code `NOTIF_LOG` (parent: الإشعارات) |

### B3
- قائمة/فلاتر: recipientId, moduleCode, `channelTypeId` (LOV-NOTIF-001), `notificationStatusId` (LOV-NOTIF-002), referenceType, نطاق تاريخ (sentAt).
- Drawer (read-only): كل الحقول + errorMessage + retryCount → A3.
- ملاحظة: السجل نظامي — الشاشة **عرض فقط**.

### B4 (CORE-9 / SEC-3) — `page_code=NOTIF_LOG`
> توليد تلقائي للصلاحيات الأربع؛ عملياً VIEW فقط (سجل نظامي). لا seed لـ PERM_*.

| SCR-NOTIF-003 | VIEW NOTIF_ADMIN | CREATE — | UPDATE — | DELETE — |

### B5
API-NOTIF-002, 003, 006.

---

# MODULE-LEVEL FUNCTIONAL APIs
> STACK-1: `/api/v1/notifications/...`. POSTGRESQL_16.

| API-ID | العملية | HTTP | المسار | RULE-IDs |
|---|---|---|---|---|
| API-NOTIF-001 | إرسال/توزيع | POST | /api/v1/notifications/dispatch | RULE-NOTIF-001, 002, 003, 004, 007 |
| API-NOTIF-002 | استعلام السجل | GET | /api/v1/notifications/logs | — |
| API-NOTIF-003 | سجل بالمعرّف | GET | /api/v1/notifications/logs/{id} | — |
| API-NOTIF-004 | CRUD القوالب | POST/GET/PUT/DELETE | /api/v1/notifications/templates | RULE-NOTIF-004, 006 |
| API-NOTIF-005 | CRUD/تفعيل القنوات | POST/GET/PUT/DELETE | /api/v1/notifications/channels | RULE-NOTIF-003, 006 |
| API-NOTIF-006 | قوائم القيم | GET | /api/v1/notifications/lookups/{lookupKey} | — |

> **Event listener (in-process):** يستمع لـ `NotificationEvent` عبر CU Events (مثال: أحداث SEC لإعادة التعيين/التفعيل).

---

# STANDALONE

## Permissions Summary & Registry Update
> CORE-9: كل شاشة = SCR-ID واحد = صف SEC_PAGES واحد. Security Engine يولّد الصلاحيات الأربع لكل page_code (لا seed أسماء PERM_* — SEC-3).

| الشاشة (page_code) | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|
| SCR-NOTIF-001 (NOTIF_TEMPLATES) | NOTIF_ADMIN | NOTIF_ADMIN | NOTIF_ADMIN | NOTIF_ADMIN |
| SCR-NOTIF-002 (NOTIF_CHANNELS) | NOTIF_ADMIN | NOTIF_ADMIN | NOTIF_ADMIN | NOTIF_ADMIN |
| SCR-NOTIF-003 (NOTIF_LOG) | NOTIF_ADMIN | — | — | — |

### Registry Update — MODE 1
```
Source Mode  : MODE 1 | Feature Code: NOTIF-001 | v1.2 (OQ-NOTIF-001 resolved; OQ-SEC-001 applied)
New Entities : NOTIF-001 NotificationLog, NOTIF-002 NotificationTemplate, NOTIF-003 NotificationChannelConfig — PRIVATE
New Lookups  : NOTIF_CHANNEL, NOTIF_STATUS — local
New Screens  : SCR-NOTIF-001 (NOTIF_TEMPLATES), SCR-NOTIF-002 (NOTIF_CHANNELS), SCR-NOTIF-003 (NOTIF_LOG)
New Rules    : +RULE-NOTIF-007 (no dispatch to inactive recipient)
New APIs     : API-NOTIF-001 → API-NOTIF-006 (+ NotificationEvent listener)
XM-IDs Open  : NOTIF → SEC (SOFT UserAccount), NOTIF → FILE (SOFT/service) — MODE 1.5
OQ-IDs Open  : None ✓ (OQ-NOTIF-001 RESOLVED)
Gate Status  : PASSED ✓ | Next: MODE 1.5 (Project 2)
```
> لمشرف السجل: master-registry §10 → NOTIF·P1=✓؛ §5 NOTIF-001/002/003؛ §6 UserAccount SOFT؛ SEC_PAGES: NOTIF_TEMPLATES/NOTIF_CHANNELS/NOTIF_LOG.

## OQ Log
```
OPEN QUESTIONS LOG — Notification Service (NOTIF) — 2026-09-02
OQ-NOTIF-001 │ اعتماد مزوّد فعلي لكل قناة (SMS/WhatsApp/Push) │ RESOLVED │ MODE 1 │ 2026-09-02 │ P3-TECH
   القرار (Architect): التصميم مستقل عن المزوّد؛ اختيار المزوّد مُفوَّض إلى P3 (بلا أثر على الجداول/الـ SRS).
(مرجعي) OQ-SEC-001 (SEC) │ حُسِم — طُبِّق هنا عبر RULE-NOTIF-007 (لا إرسال لمستلِم غير نشط؛ تُبقى السجلات).
```

---
*End of srs-NOTIF.md | NOTIF-001 | v1.2 | Backend + Frontend | OQ-NOTIF-001 RESOLVED | Next: MODE 1.5*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 02, 05, 06, 08, 10, 11, 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag; a bare `file.java:line` is under
`erp-core/src/main/java/com/erp/notif/`, a `V<N>__…sql:line` under `erp-core/src/main/resources/db/migration/core/`.
Endpoint paths and methods are taken from `docs/api-docs/notif/`. Every row is labelled NEW / CHANGED / REMOVED /
NOT IMPLEMENTED. New rules continue the A4 sequence from RULE-NOTIF-007; no API / ENTITY / LOV id is minted (new
endpoints and the inbox entity are named). The decisions behind the deltas are
`governance/analysis/decisions/NOTIF/ADR-NOTIF-001..007`.

### 1. Endpoints (نقاط النهاية)
| Kind | Method | Path | Access | Notes | Source |
|---|---|---|---|---|---|
| NEW | GET | `/api/v1/notif/inbox?unreadOnly&page&size` | `isAuthenticated()` (staff chain) | caller's own items, newest first, `Page<InboxItemResponse>`, page size ≤ 200; a customer token on this path → 403 `REALM_MISMATCH` (SEC chain); the `/notif` prefix differs from the module's `/notifications` convention | controller/NotificationInboxController.java:32-47; service/NotificationInboxService.java:48-61; docs/api-docs/notif/endpoints/notification-inbox.md; DEVIATIONS [08] |
| NEW | PATCH | `/api/v1/notif/inbox/{id}/read` | `isAuthenticated()` (staff chain) | idempotent; keeps the first `READ_AT`; returns the item; an unknown or another user's id → 404 `INBOX_ITEM_NOT_FOUND` | controller/NotificationInboxController.java:49-53; service/NotificationInboxService.java:64-80 |
| NEW | GET | `/api/v1/customers/me/inbox` | `ROLE_CUSTOMER` (customer chain `/api/v1/customers/**`) | same behaviour for customers | controller/NotificationInboxController.java:35, 40; com/erp/autoconfigure/ErpCoreSecurityAutoConfiguration.java:82, 182; DEVIATIONS [08] (rebase onto 06) |
| NEW | PATCH | `/api/v1/customers/me/inbox/{id}/read` | `ROLE_CUSTOMER` | same | same |
| CHANGED | POST | `/api/v1/notifications/dispatch` (API-NOTIF-001) | `isAuthenticated()` — no permission (ADR-NOTIF-005) | answers 200 (not the planned 202) with `logIds`; `templateCode` and every `channelHint` element trimmed + upper-cased; unknown template → 404 `NOTIF_TEMPLATE_NOT_FOUND`; inactive template → 422 `NOTIF_TEMPLATE_INACTIVE`; inactive or unknown recipient → 200 `logIds: []`, no row written; one row per `channelHint` element — `QUEUED` when an enabled `NOTIF_CHANNEL_CONFIG` row exists, else `CHANNEL_DISABLED` (no row ≡ disabled); nothing is sent synchronously; `variables.email` overrides the EMAIL address | service/DispatchService.java:72-73, 109-124, 129-158, 174-178; controller/DispatchController.java:22-25; docs/api-docs/notif/endpoints/notification-dispatch.md |
| CHANGED | POST | `/api/v1/notifications/logs/search` (API-NOTIF-002, was `GET /logs`) | `PERM_NOTIF_LOG_VIEW` | `BaseSearchContractRequest` envelope; filter whitelist `recipientId`, `moduleCode`, `channelTypeId`, `notificationStatusId`, `referenceType`, `sentAt`; sort whitelist `createdAt`, `sentAt`, `notificationStatusId`; page ≤ 200; no match → 200 empty page | service/NotificationLogService.java:44-70; docs/api-docs/notif/endpoints/notification-logs.md; ADR-SEC-003 (search-contract precedent) |
| CHANGED | GET | `/api/v1/notifications/logs/{id}` (API-NOTIF-003) | `PERM_NOTIF_LOG_VIEW` | response gains `attempts`, `nextAttemptAt`, `lastError`; the dispatch variables are never exposed; unknown → 404 `NOTIF_LOG_NOT_FOUND` | service/NotificationLogService.java:75-82; DEVIATIONS [08] |
| CHANGED | POST | `/api/v1/notifications/templates` (API-NOTIF-004 create) | `PERM_NOTIF_TEMPLATES_CREATE` | 201; `templateCode` trimmed + upper-cased at persist; duplicate in the tenant → 409 `NOTIF_TEMPLATE_CODE_DUPLICATE`; `attachmentFileId` must name an available FILE document → 404 `NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND`; blank code / names / bodies → 400 (`VALIDATION_ERROR` from the DTO; `NOTIF_TEMPLATE_BILINGUAL_REQUIRED` from the Domain for in-process callers) | service/NotificationTemplateService.java:57-74, 153-158; entity/NotificationTemplate.java:87-95; domain/NotificationTemplateDomain.java:30-37 |
| CHANGED | POST | `/api/v1/notifications/templates/search` (was the `GET` list) | `PERM_NOTIF_TEMPLATES_VIEW` | filter + sort whitelist `templateCode`, `nameAr`, `nameEn`, `isActive`, `createdAt` | service/NotificationTemplateService.java:50-52, 79-92 |
| CHANGED | GET | `/api/v1/notifications/templates/{id}` | `PERM_NOTIF_TEMPLATES_VIEW` | unknown → 404 `NOTIF_TEMPLATE_NOT_FOUND` | service/NotificationTemplateService.java:97-105 |
| CHANGED | PUT | `/api/v1/notifications/templates/{id}` | `PERM_NOTIF_TEMPLATES_UPDATE` | full replace: an omitted `attachmentFileId` clears `ATTACHMENT_FILE_ID`; `isActiveFl` is applied only when supplied, and reactivation happens only through `isActiveFl: true` (there is no activate endpoint); `templateCode` immutable (absent from the body); attachment re-validated | mapper/NotificationTemplateMapper.java:34-48; service/NotificationTemplateService.java:110-127 |
| CHANGED | DELETE | `/api/v1/notifications/templates/{id}` | `PERM_NOTIF_TEMPLATES_DELETE` | 204, soft deactivate (`IS_ACTIVE_FL = 0`); a deactivated template refuses dispatch (422) | service/NotificationTemplateService.java:137-146; controller/NotificationTemplateController.java:72-77 |
| CHANGED | POST | `/api/v1/notifications/channels` (API-NOTIF-005 create) | `PERM_NOTIF_CHANNELS_CREATE` | 201; `channelTypeId` is any code of ≤ 20 characters, trimmed + upper-cased — it is not checked against `NOTIF_CHANNEL`; `configJson` is stored as text, not validated as JSON and read by no core provider; duplicate in the tenant → 409 `NOTIF_CHANNEL_CONFIG_DUPLICATE`; blank code → 400 | service/NotificationChannelConfigService.java:55-69; dto/ChannelCreateRequest.java:23-33; entity/NotificationChannelConfig.java:59-74 |
| CHANGED | POST | `/api/v1/notifications/channels/search` (was the `GET` list) | `PERM_NOTIF_CHANNELS_VIEW` | filter + sort whitelist `channelTypeId`, `isEnabled`, `createdAt` | service/NotificationChannelConfigService.java:48-50, 74-87 |
| CHANGED | GET | `/api/v1/notifications/channels/{id}` | `PERM_NOTIF_CHANNELS_VIEW` | unknown → 404 `NOTIF_CHANNEL_CONFIG_NOT_FOUND` | service/NotificationChannelConfigService.java:92-100 |
| CHANGED | PUT | `/api/v1/notifications/channels/{id}` | `PERM_NOTIF_CHANNELS_UPDATE` | full replace: an omitted `configJson` sets `CONFIG_JSON` to NULL; `isEnabledFl` applied only when supplied (re-enable = `isEnabledFl: true`); `channelTypeId` immutable | mapper/NotificationChannelConfigMapper.java:28-36; service/NotificationChannelConfigService.java:105-118 |
| CHANGED | DELETE | `/api/v1/notifications/channels/{id}` | `PERM_NOTIF_CHANNELS_DELETE` | 204, soft disable (`IS_ENABLED_FL = 0`); RULE-NOTIF-003 applies from the next dispatch | service/NotificationChannelConfigService.java:128-138; controller/NotificationChannelController.java:72-77 |
| CHANGED | GET | `/api/v1/notifications/lookups/{lookupKey}` (API-NOTIF-006) | `isAuthenticated()` | values are read live from MDL (`MdlLookupApi.readActiveValuesByKey`) for `NOTIF_CHANNEL` and `NOTIF_STATUS` only; any other key, or a type not seeded for the tenant → 404 `NOTIF_LOOKUP_KEY_UNKNOWN`; items `{code, labelAr, labelEn}` in MDL sort order | service/NotificationLookupService.java:36-49; ADR-NOTIF-006 |
| NEW (report) | POST | `/api/v1/report/NOTIF_LOG_SUMMARY/run`, `/export` | `NOTIF:REPORT:NOTIF_LOG_SUMMARY` | params `channel`, `status` (LOOKUP `NOTIF_CHANNEL` / `NOTIF_STATUS`), `dateFrom`, `dateTo`; columns day, channel, status, count; documented under `docs/api-docs/report/` | report/NotifLogSummaryReport.java:35-47, 72-95; DEVIATIONS [11] |
| REMOVED | — | "Event listener (in-process): listens to `NotificationEvent` via CU Events" (MODULE-LEVEL FUNCTIONAL APIs note; Registry Update "+ NotificationEvent listener") | — | NOTIF consumes no event of another module; the inbound integration is the in-process `NotificationDispatchApi` (`dispatch`, `dispatchIndependently`), which SEC calls directly | crossmodule/NotificationDispatchApi.java:5-12, 25-34; com/erp/sec/service/PasswordResetService.java:226; com/erp/sec/service/CustomerAccountService.java:309; ADR-NOTIF-001 |

Contract note: the four inbox endpoints carry "Authorization: not extracted" in the generated docs (the controller maps
two paths per method, which the extractor does not resolve) and list no business responses, so
`NOTIF_CHANNEL_UNAVAILABLE` and `INBOX_ITEM_NOT_FOUND` are "unbound" in `docs/api-docs/notif/index.md:100-101`; the
access column above is taken from the code.

### 2. Business rules (قواعد الأعمال)
| Kind | Rule | Source |
|---|---|---|
| CHANGED | RULE-NOTIF-002 (retry): 5 attempts in total, waits 2, 4, 8, 16 s — keys `erp.core.notif.retry.max-attempts` (5), `erp.core.notif.retry.initial-delay-ms` (2000), `erp.core.notif.retry.multiplier` (2.0), `erp.core.notif.retry.max-delay-ms` (32000), read by the worker's `@Retryable`; then `FAILED` + `NotificationFailedEvent`. Analysis said ×1.5; implemented ×2 per the step-08 plan (ADR-NOTIF-003). `RETRY_COUNT` (= `ATTEMPTS` − 1) and `ERROR_MESSAGE` (final reason) are still maintained. | service/NotificationDeliveryWorker.java:48-54; com/erp/autoconfigure/ErpCoreProperties.java:323-336; service/NotificationDeliveryProcessor.java:122-124; DEVIATIONS [08] |
| CHANGED | RULE-NOTIF-003 (channel enablement): a channel with no `NOTIF_CHANNEL_CONFIG` row in the tenant counts as disabled → row `CHANNEL_DISABLED`, never delivered; so does a row with `IS_ENABLED_FL = 0`. `SKIPPED_NO_PROVIDER` is reached only by an enabled configuration whose channel has no `ChannelProvider`. Only EMAIL and IN_APP are seeded with a configuration (V9, V13): SMS, WHATSAPP, PUSH and INTERNAL end `CHANNEL_DISABLED` until an operator creates their row. | service/DispatchService.java:133-141; domain/NotificationChannelConfigDomain.java:43-45; V9__notif_seed.sql:19-20; V13__notif_async_inbox.sql:85-89 |
| CHANGED | RULE-NOTIF-004 (attachment): `attachmentFileId` is validated at create / update (`FileDocumentLookupApi.isAvailable`, else 404 `NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND`) and stored, but never sent — `OutboundMessage` carries no attachment and no core provider reads the column. The bilingual half of the rule is enforced by the DTOs (`@NotBlank`) and the Domain. | service/NotificationTemplateService.java:153-158; channel/OutboundMessage.java:31-46; channel/EmailChannelProvider.java:49-86 |
| CHANGED | RULE-NOTIF-007 (recipient eligibility): "active" = `UserContact.active`, i.e. `ACTIVE` or `PENDING_VERIFICATION` (the customer verification mail); recipients may be of either realm (one `SEC_USER` id space). | crossmodule/SecRecipientDirectory.java:20-23; com/erp/sec/crossmodule/SecUserDirectoryApiImpl.java:119-126; DEVIATIONS [06], [08]; ADR-NOTIF-007 |
| CHANGED | P0 AUTO-DECISION "provider creds in `NotificationChannelConfig.config_json`": `CONFIG_JSON` is stored and returned by the API but read by no provider; the EMAIL provider is configured through `spring.mail.*`, an application provider through its own properties; tenant provisioning copies the configurations without it. | channel/EmailChannelProvider.java:38-41; com/erp/autoconfigure/ErpCoreNotifAutoConfiguration.java:59-64; tenant/NotifTenantProvisioningContributor.java:34-40; ADR-NOTIF-004 |
| NEW | RULE-NOTIF-008 Asynchronous delivery: dispatch persists the rows and publishes `NotificationRequestedEvent`; after commit the listener hands the row to `erpCoreEventExecutor`; each attempt runs `prepare` (own transaction: claims the row, `ATTEMPTS` + 1) → provider `send` with no transaction open → `recordOutcome` (own transaction), all inside `TenantContext.callAs(tenant)`. Delivery is at-least-once. | service/NotificationDeliveryListener.java:46-70; service/NotificationDeliveryWorker.java:55-70, 79-104; service/NotificationDeliveryProcessor.java:103-128, 150-200; docs/steps/08-report.md; ADR-NOTIF-002 |
| NEW | RULE-NOTIF-009 Provider resolution: an application `ChannelProvider` bean for the channel → the core provider (`EmailChannelProvider`, registered only when a `JavaMailSender` bean exists; `InAppChannelProvider`) → a `LoggingChannelProvider` stand-in that answers `SKIPPED_NO_PROVIDER` with reason `NOTIF_CHANNEL_UNAVAILABLE` (final, never retried). | service/ChannelProviderRegistry.java:26-46; channel/LoggingChannelProvider.java:31-35; com/erp/autoconfigure/ErpCoreNotifAutoConfiguration.java:55-65; DEVIATIONS [08]; ADR-NOTIF-004 |
| NEW | RULE-NOTIF-010 Template rendering: `{name}` placeholders (pattern `\{(\w+)}`) are replaced by the dispatch variable of that name; an unknown placeholder stays as written; the title of a message is the template subject of that language, or the template name when the subject is blank (the DTO examples `Hello {0}` are not the syntax). | channel/TemplateText.java:13, 20-32; channel/OutboundMessage.java:61-68; dto/TemplateCreateRequest.java:48-53 |
| NEW | RULE-NOTIF-011 EMAIL composition and address: one language per mail — Arabic when `variables.lang` = `AR` (case-insensitive), English otherwise; `To` = `variables.email` (the request's own value, else the recipient's account e-mail filled in by dispatch; none at all → `REJECTED` → row `FAILED` after one attempt, `lastError = missing recipient email address`, no retry — 1.1.0); `From` = `spring.mail.username` (empty → the mail session's default); subject and body rendered; a multipart with a plain-text part and an HTML part in which the template text and every substituted value are HTML-escaped; `actionLink` becomes a CTA button labelled `ctaLabelAr` / `ctaLabelEn` (default: the URL) plus a copyable link. | channel/EmailChannelProvider.java:49-86, 91-117; com/erp/autoconfigure/ErpCoreNotifAutoConfiguration.java:62-63; service/DispatchService.java:144-151, 171-180; DEVIATIONS [14]; CHANGELOG [1.1.0] |
| NEW | RULE-NOTIF-012 IN_APP composition and inbox: one `NOTIF_INBOX` row per delivered IN_APP row, both languages rendered, titles truncated to 300 characters, `REFERENCE_TYPE` / `REFERENCE_ID` copied, no link back to `NOTIF_LOG`; a caller lists and marks only own items (`RECIPIENT_USER_ID` = own `SEC_USER` id); another user's or an unknown id → 404 `INBOX_ITEM_NOT_FOUND` (never revealed which); a principal that is no user account → 403 `NOTIF_CHANNEL_UNAVAILABLE`. | channel/InAppChannelProvider.java:31-49; domain/NotificationInboxDomain.java:35-44; service/NotificationInboxService.java:82-85; DEVIATIONS [08] |
| NEW | RULE-NOTIF-013 Code normalisation: `TEMPLATE_CODE` and `CHANNEL_TYPE_ID` are trimmed and upper-cased at persist (`@PrePersist` / `@PreUpdate`); dispatch normalises `templateCode` and each `channelHint` element the same way before lookup; the uniqueness pre-checks probe the normalised value. | entity/NotificationTemplate.java:87-102; entity/NotificationChannelConfig.java:59-74; service/DispatchService.java:109, 131, 204-206; service/NotificationTemplateService.java:62, 160-162 |
| NEW | RULE-NOTIF-014 `channelHint` semantics: a non-empty list of channel codes (each ≤ 20 characters); the `ALL` wildcard of the P0 AUTO-DECISION is not supported (it is treated as a channel code with no configuration → `CHANNEL_DISABLED`); duplicates are not removed — each element yields its own row and event. | dto/DispatchRequest.java:38-40; service/DispatchService.java:129-160 |
| NEW | RULE-NOTIF-015 Exhaustion: when the last allowed attempt fails the row is `FAILED` at once (`Outcome.EXHAUSTED`); a `QUEUED` row that has already used every attempt when it is claimed again (requeue after a crash) is `FAILED` without a new try; Spring Retry's `@Recover` (`markFailed`) is the fallback when the worker's own run gives up; the stored reason is `LAST_ERROR`, else `delivery attempts exhausted`; `ATTEMPTS` never exceeds `max-attempts`, also across requeues. | service/NotificationDeliveryProcessor.java:69-70, 116-121, 184-191, 206-214; service/NotificationDeliveryWorker.java:72-77; DEVIATIONS [15] |
| NEW | RULE-NOTIF-016 Claim lease (1.2.0): `prepare` claims a `QUEUED` row by setting `NEXT_ATTEMPT_AT` = now + max(1, `erp.core.notif.requeue.stale-after-minutes`) minutes; a row whose `NEXT_ATTEMPT_AT` lies in the future is skipped unless its `VERSION` equals the one this run last wrote (ownership by row version, no clock comparison); two concurrent claims are serialised by the optimistic lock — the loser's attempt ends `NOT_QUEUED`; between retries the column holds the time the next retry is due. | service/NotificationDeliveryProcessor.java:103-128, 192-197, 233-236; service/NotificationDeliveryWorker.java:79-123; docs/steps/15-report.md; DEVIATIONS [15]; ADR-NOTIF-002 |
| NEW | RULE-NOTIF-017 Node-local de-duplication: `NotificationDeliveryTracker` (an in-memory set) records a row from the moment the listener submits it until its delivery returns; a second `NotificationRequestedEvent` for the same row on the same node is ignored; the listener submits to the executor itself (not `@Async`); an executor rejection (queue full) releases the row, which stays `QUEUED` for the requeue job; across nodes only the lease and the optimistic lock prevent a double send. | service/NotificationDeliveryListener.java:46-70; service/NotificationDeliveryTracker.java:22-39; DEVIATIONS [15] |
| NEW | RULE-NOTIF-018 Requeue job: the `NotificationRequeueJob` bean exists only with `erp.core.notif.requeue.enabled=true` (default false) and fires only under an application's `@EnableScheduling` (`@Scheduled` fixed delay and initial delay `erp.core.notif.requeue.interval-ms`, default 60000); it reads the tenants holding `QUEUED` rows with plain JDBC (cross-tenant), then per tenant, inside `TenantContext.callAs`, re-publishes `NotificationRequestedEvent` (outside a transaction) for every row whose `NEXT_ATTEMPT_AT` — or `CREATED_AT` when none — is older than `stale-after-minutes` (default 10) and that this node is not already delivering. | service/NotificationRequeueJob.java:43-44, 73-101; repository/NotificationLogRepository.java:36-39; com/erp/autoconfigure/ErpCoreProperties.java:345-355; com/erp/autoconfigure/ErpCoreNotifAutoConfiguration.java:41-52; docs/CONSUMING.md §7 |
| NEW | RULE-NOTIF-019 Recipient resolution: `RecipientDirectory.isActive` = `SecUserDirectoryApi.findContact(id).active`; an unknown id is inactive; an inactive or unknown recipient answers 200 with `logIds: []` and writes no row (history is kept); `dispatchIndependently` resolves the recipient in the caller's transaction before the REQUIRES_NEW dispatch opens, so a user the caller has not committed yet is seen. | crossmodule/SecRecipientDirectory.java:20-23; service/DispatchService.java:93-102, 120-124; crossmodule/NotificationDispatchApiImpl.java:28-36 |
| NEW | RULE-NOTIF-020 Variables at rest: `VARIABLES_JSON` holds the dispatch variables (for EMAIL rows including the resolved `email`) only while `QUEUED`; every final status clears it; it is never returned by the API. | service/DispatchService.java:126-152; service/NotificationDeliveryProcessor.java:161, 173, 222; DEVIATIONS [08] |
| NEW | RULE-NOTIF-021 Tenant confinement: every template, channel configuration, log row and inbox item belongs to one tenant (`@TenantId`, optimistic `VERSION`); a new tenant receives copies of the source (PLATFORM) tenant's channel configurations without `CONFIG_JSON` and templates without `ATTACHMENT_FILE_ID` (`NotifTenantProvisioningContributor`, order 20). | tenant/NotifTenantProvisioningContributor.java:27-53; DEVIATIONS [05] |
| NEW | RULE-NOTIF-022 Limits: dispatch has no rate limit; the length of `channelHint` and the size of `variables` are unbounded (beyond `VARIABLES_JSON` being TEXT); every paged endpoint, the inbox included, serves at most 200 items per page. | service/DispatchService.java:68-75; dto/DispatchRequest.java:38-60; com/erp/common/search/PageableBuilder.java (docs/api-docs/notif/index.md:60-68) |
| NEW (decision) | Dispatch exposure: `POST /dispatch` is gated by `isAuthenticated()` only — any staff principal may send any active template of the tenant to any eligible recipient, and `variables.email` redirects an EMAIL to an arbitrary address. Accepted for 1.2.0 with the risk and the mitigation candidates recorded in ADR-NOTIF-005. | service/DispatchService.java:69-73, 174-178; ADR-NOTIF-005 |
| REMOVED | Step-02 interim `NO_MAIL_SENDER` failure reason and the synchronous send path (`DefaultChannelProvider`, `RetryPolicy`); without a mail sender an EMAIL row now ends `SKIPPED_NO_PROVIDER`. | DEVIATIONS [02], [08] |
| REMOVED | `channelHint = ALL` (P0 AUTO-DECISION "single \| list \| ALL") — see RULE-NOTIF-014. | service/DispatchService.java:129-131 |

### 3. Status lifecycle (A6) — implemented (دورة الحالة)
| Kind | Transition | Trigger | Source |
|---|---|---|---|
| CHANGED | `PENDING` | transient only: the state of a row while dispatch builds it; never persisted (MDL still offers the value — it is never stored) | domain/NotificationLogDomain.java:21-37 |
| NEW | `PENDING → QUEUED` | an enabled channel configuration exists for the channel | service/DispatchService.java:143 |
| CHANGED | `PENDING → CHANNEL_DISABLED` | no configuration row, or `IS_ENABLED_FL = 0` | service/DispatchService.java:138-141 |
| NEW | `QUEUED → SENT` | the provider answers `SENT`; `SENT_AT` set; `NotificationDispatchedEvent` | service/NotificationDeliveryProcessor.java:157-167 |
| CHANGED | `QUEUED → FAILED` | last allowed attempt failed; attempts already used when claimed; `@Recover` fallback; or the provider answered `REJECTED` (permanent) — `NotificationFailedEvent` in every case | service/NotificationDeliveryProcessor.java:116-121, 178-191, 206-227 |
| NEW | `QUEUED → SKIPPED_NO_PROVIDER` | the provider answers `SKIPPED_NO_PROVIDER` (no `ChannelProvider` for the channel); final | service/NotificationDeliveryProcessor.java:168-177 |
| NEW | any other transition | 422 `NOTIF_LOG_INVALID_TRANSITION` (internal invariant, `StatusTransitions`) | domain/NotificationLogDomain.java:30-37, 54-56 |
```
[PENDING] (transient, never persisted) ──► [QUEUED] | [CHANNEL_DISABLED] ⊘
[QUEUED] ──► [SENT] ✓ | [FAILED] ✗ (attempts exhausted, or REJECTED) | [SKIPPED_NO_PROVIDER] ⊘
```

### 4. Error codes (رموز الأخطاء)
The analysis above carries no error-code catalogue, so every NOTIF code is NEW here. All 13 are bilingual
(`erp-core/src/main/resources/i18n/messages.properties:69-81, 163-164`; `messages_ar.properties:66-78, 160-161`).
| Kind | Code | HTTP | Raised when | Source |
|---|---|---|---|---|
| NEW | `NOTIF_TEMPLATE_BILINGUAL_REQUIRED` | 400 | blank `templateCode`, `nameAr`, `nameEn`, `bodyAr` or `bodyEn` on create, blank body on update (Domain); over HTTP the DTO's `@NotBlank` answers `VALIDATION_ERROR` first, so the code reaches a client only from an in-process caller; the name is misleading (also raised for the code and the names) | domain/NotificationTemplateDomain.java:33-34, 57-59; dto/TemplateCreateRequest.java:24-53 |
| NEW | `NOTIF_TEMPLATE_CODE_DUPLICATE` | 409 | `templateCode` already used in the tenant | domain/NotificationTemplateDomain.java:35 |
| NEW | `NOTIF_CHANNEL_CONFIG_DUPLICATE` | 409 | `channelTypeId` already configured in the tenant | domain/NotificationChannelConfigDomain.java:29 |
| NEW | `NOTIF_LOG_INVALID_TRANSITION` | 422 | illegal status transition (internal invariant) | domain/NotificationLogDomain.java:30-37 |
| NEW | `NOTIF_CHANNEL_TYPE_REQUIRED` | 400 | blank `channelTypeId` on create (Domain; shadowed over HTTP by the DTO's `@NotBlank`) | domain/NotificationChannelConfigDomain.java:28 |
| NEW | `NOTIF_TEMPLATE_INACTIVE` | 422 | dispatch against a deactivated template; no analysed rule covers it (the constant's Javadoc cites RULE-NOTIF-007, which is the inactive-recipient rule) | domain/NotificationTemplateDomain.java:45-54; service/DispatchService.java:113-116 |
| NEW | `NOTIF_TEMPLATE_NOT_FOUND` | 404 | template id unknown (get / update / deactivate), or `templateCode` unknown on dispatch | service/NotificationTemplateService.java:100-102; service/DispatchService.java:110-111 |
| NEW | `NOTIF_CHANNEL_CONFIG_NOT_FOUND` | 404 | channel-config id unknown (get / update / disable); also `NotificationChannelAdminApi.setChannelEnabled` with an unknown code | service/NotificationChannelConfigService.java:95-97; crossmodule/NotificationChannelAdminApiImpl.java:23-26 |
| NEW | `NOTIF_LOG_NOT_FOUND` | 404 | log id unknown | service/NotificationLogService.java:78-80 |
| NEW | `NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND` | 404 | `attachmentFileId` names no available FILE document (create / update) | service/NotificationTemplateService.java:153-158 |
| NEW | `NOTIF_LOOKUP_KEY_UNKNOWN` | 404 | lookup key other than `NOTIF_CHANNEL` / `NOTIF_STATUS`, or the type not seeded for the tenant | service/NotificationLookupService.java:37, 47-48 |
| NEW | `NOTIF_CHANNEL_UNAVAILABLE` | 403 | the inbox used by a principal that is no user account; also the stored reason of a `SKIPPED_NO_PROVIDER` row (unbound in the generated contract) | service/NotificationInboxService.java:82-85; channel/LoggingChannelProvider.java:34; docs/api-docs/notif/index.md:100 |
| NEW | `INBOX_ITEM_NOT_FOUND` | 404 | inbox item unknown or not the caller's (breaks the `NOTIF_` prefix convention; unbound in the generated contract) | domain/NotificationInboxDomain.java:35-39; service/NotificationInboxService.java:70-71; docs/api-docs/notif/index.md:101 |
| NEW (SEC, surfaced on NOTIF paths) | `REALM_MISMATCH` | 403 | a token of the other realm on `/api/v1/notif/inbox` or `/api/v1/customers/me/inbox` | controller/NotificationInboxController.java:22-24; docs/api-docs/sec/index.md |
| NEW (common) | `CONCURRENT_MODIFICATION` | 409 | optimistic-lock conflict on any NOTIF row | DEVIATIONS [05] |
| NEW (common) | `VALIDATION_ERROR` | 400 | DTO constraint violation (`@NotBlank`, `@Size`, `@NotEmpty channelHint`) | docs/api-docs/notif/index.md:102 |

### 5. Permissions (الصلاحيات — exact authority strings)
| Kind | Authority / grant | Meaning | Source |
|---|---|---|---|
| CHANGED | `PERM_NOTIF_TEMPLATES_VIEW/CREATE/UPDATE/DELETE`, `PERM_NOTIF_CHANNELS_VIEW/CREATE/UPDATE/DELETE`, `PERM_NOTIF_LOG_VIEW` | the nine analysed authorities; seeded as catalog rows by V7 (B4 said "no seed of PERM_*") and re-declared in code (`NotifPermissions`, upserted at every start by the catalog synchroniser) | permission/NotifPermissions.java:21-32, 50-62; V7__sec_seed.sql:119-127 |
| CHANGED | module grants | `NOTIF_ADMIN` **and** `SYS_ADMIN` hold the NOTIF module (B4 / Permissions Summary name NOTIF_ADMIN only); `SYS_ADMIN` is the super role and holds every NOTIF authority without grant rows | V7__sec_seed.sql:152, 173, 176; V11__sec_realms.sql:57-59 |
| NEW | `NOTIF:REPORT:NOTIF_LOG_SUMMARY` + gateway `PERM_NOTIF_REPORTS_VIEW` (screen `NOTIF_REPORTS`) | the report; no seeded role carries an explicit grant for it — only `SYS_ADMIN` holds it, through the super role | report/NotifLogSummaryReport.java:38; DEVIATIONS [11] |
| NEW (no catalog entry) | `isAuthenticated()` | `POST /dispatch` (ADR-NOTIF-005), the four inbox endpoints (own items only), `GET /lookups/{key}` | service/DispatchService.java:72; service/NotificationInboxService.java:49, 65; service/NotificationLookupService.java:42 |
| NEW | `ROLE_CUSTOMER` | the customer inbox paths (customer chain) | com/erp/autoconfigure/ErpCoreSecurityAutoConfiguration.java:182 |

### 6. Entities and LOVs (الكيانات وقوائم القيم)
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | ENTITY-NOTIF-001 NotificationLog | + `attempts`, `nextAttemptAt` (retry due time and, since 1.2.0, the claim lease), `lastError`, `variablesJson` (internal), `tenantId`, `version`; `retryCount` = `attempts` − 1, `errorMessage` = final reason; the entity's `@Index` list omits `IDX_NOTIF_LOG_STATUS_NEXT_ATTEMPT` and `IDX_NOTIF_LOG_TENANT` (both exist in the schema — code drift without runtime effect) | entity/NotificationLog.java:36-43, 89-106; V13 §1; V10 |
| CHANGED | ENTITY-NOTIF-002 NotificationTemplate | + `tenantId`, `version`; `templateCode` unique per tenant; `@Audited(entityType = "NOTIF_TEMPLATE")`; `attachmentFileId` validated at write (XM-NOTIF-002), never sent | entity/NotificationTemplate.java:33-38; DEVIATIONS [10] |
| CHANGED | ENTITY-NOTIF-003 NotificationChannelConfig | + `tenantId`, `version`; `channelTypeId` unique per tenant; `configJson` read by no provider; only EMAIL and IN_APP rows are seeded (analysis: all five enabled) | entity/NotificationChannelConfig.java:32-36; V9__notif_seed.sql:19-20; V13__notif_async_inbox.sql:85-89 |
| NEW | ENTITY-NOTIF-004 NotificationInboxItem (`NOTIF_INBOX`) | `recipientUserId` (`SEC_USER` id of either realm, SOFT-READ), `titleAr/En` (300), `bodyAr/En`, `readAt`, `referenceType`, `referenceId`, `tenantId`, `version`; no link to `NOTIF_LOG`; `PK_NOTIF_INBOX`, `FK_NOTIF_INBOX_TENANT`, `IDX_NOTIF_INBOX_TENANT`, `IDX_NOTIF_INBOX_RECIPIENT` | entity/NotificationInboxItem.java:29-80; V13__notif_async_inbox.sql:36-63 |
| CHANGED | XM-NOTIF-001 (A7 UserAccount SOFT-READ) | the target is the physical table `SEC_USER` (both realms), read through `RecipientDirectory` → `SecUserDirectoryApi.findContact` / `findCurrentUserId`; no FK | crossmodule/SecRecipientDirectory.java:16-34; V13__notif_async_inbox.sql:57 |
| CHANGED | XM-NOTIF-002 (FILE attachment) | still no FK, but validated at write time through `FileDocumentLookupApi.isAvailable` (404 when unavailable); the file is never attached to a message | service/NotificationTemplateService.java:47, 153-158 |
| CHANGED | A5 "local LOVs, no central lookup" | `NOTIF_CHANNEL` and `NOTIF_STATUS` are MDL lookup types owned by NOTIF (V8), served through `MdlLookupApi`; the report's channel / status params are MDL LOOKUP params | V8__mdl_seed.sql:28-29, 42-52; service/NotificationLookupService.java:39; ADR-NOTIF-006 |
| CHANGED | LOV-NOTIF-001 NotificationChannel | + `IN_APP` (V13); `NotifChannels` exposes the constants `EMAIL`, `SMS`, `PUSH`, `IN_APP` only (no `WHATSAPP` / `INTERNAL` constant) | V13__notif_async_inbox.sql:75; channel/NotifChannels.java:13-16 |
| CHANGED | LOV-NOTIF-002 NotificationStatus | + `QUEUED`, `SKIPPED_NO_PROVIDER`; `PENDING` is offered by MDL but never stored | V13__notif_async_inbox.sql:73-74 |
| NEW | seeded templates | `PASSWORD_RESET` and `ACCOUNT_ACTIVATION` (V9, PLATFORM tenant; `ACCOUNT_ACTIVATION` is dispatched by no core code), `CUSTOMER_VERIFY_EMAIL` and `CUSTOMER_PASSWORD_RESET` (V11 §6, every tenant existing then; later tenants copy PLATFORM); placeholders `{actionLink}`, `{expiresAt}` | V9__notif_seed.sql:25-52; V11__sec_realms.sql:90-118 |
| NEW (note) | stale column comments | V6 still says `PENDING/SENT/FAILED/CHANNEL_DISABLED` and "RETRY_COUNT ≤ 5"; V13 says `NEXT_ATTEMPT_AT` is "NULL otherwise" (it is the lease while in flight); comments only — this addendum is the data dictionary | V6__notif_schema.sql:75-76; V13__notif_async_inbox.sql:26 |

### 7. Dependencies (A7) and exposed surface (التبعيات والواجهات)
| Kind | Direction | Item | Source |
|---|---|---|---|
| CHANGED | consumed | events: the shared `com.erp.events` bus (`DomainEventPublisher`, executor `erpCoreEventExecutor`) replaces "CU Events" | service/NotificationDeliveryListener.java:39; DEVIATIONS [08] |
| CHANGED | consumed | SEC: the `RecipientDirectory` port (`isActive`, `emailOf` — 1.1.0, no default implementation —, `currentRecipientId`) backed by `SecUserDirectoryApi.findContact` / `findCurrentUserId` | crossmodule/RecipientDirectory.java:16-30; crossmodule/SecRecipientDirectory.java:14-34; CHANGELOG [1.1.0] |
| NEW | consumed | tenant: `CORE_TENANT` FK on every table, `TenantContext`, `TenantProvisioningContributor` (order 20) | tenant/NotifTenantProvisioningContributor.java:27-30; DEVIATIONS [05] |
| NEW | consumed | MDL: `MdlLookupApi.readActiveValuesByKey` for API-NOTIF-006; the values are seeded by V8 (five channels, four statuses) and V13 (`IN_APP`, `QUEUED`, `SKIPPED_NO_PROVIDER`) per tenant | service/NotificationLookupService.java:39, 47-48; ADR-NOTIF-006 |
| NEW | consumed | FILE: `FileDocumentLookupApi.isAvailable` (attachment validation) | service/NotificationTemplateService.java:47, 153-158 |
| NEW | consumed | audit: `@Audited` on `NotificationTemplate`; report: `ReportProvider` (`NotifLogSummaryReport`) | DEVIATIONS [10], [11] |
| NEW | exposed | `NotificationDispatchApi.dispatch(DispatchCommand)` → `List<Long>` (caller's transaction, `isAuthenticated()`) and `dispatchIndependently(DispatchCommand)` (REQUIRES_NEW through `DispatchService.dispatchSystem`; the adapter resolves the recipient in the caller's transaction; a failure never marks the caller rollback-only). Consumers: SEC password reset (`PasswordResetService.java:226`) and the customer verify / reset mails (`CustomerAccountService.java:309`), both inside `InternalCallerContext` | crossmodule/NotificationDispatchApi.java:25-34; crossmodule/NotificationDispatchApiImpl.java:23-36; service/DispatchService.java:98-102 |
| NEW | exposed | `DispatchCommand(recipientId, templateCode, channelHint, moduleCode, referenceId, referenceType, variables)` — the only read-model crossing the boundary inbound | crossmodule/DispatchCommand.java:12-20 |
| NEW | exposed | `NotificationChannelAdminApi.setChannelEnabled(channelTypeId, enabled)`: flips `IS_ENABLED_FL` by code and keeps `CONFIG_JSON`; unknown code → `NOTIF_CHANNEL_CONFIG_NOT_FOUND` | crossmodule/NotificationChannelAdminApi.java:11-15; crossmodule/NotificationChannelAdminApiImpl.java:21-34 |
| NEW | exposed | `NotificationLogQueryApi.findByRecipientModuleAndReference(recipientId, moduleCode, referenceType)` → `List<DispatchLogRecord(id, templateCode, notificationStatusId)>`, newest first | crossmodule/NotificationLogQueryApi.java:13-18; crossmodule/DispatchLogRecord.java:9; docs/CONSUMING.md §7 |
| NEW | exposed (SPI) | `ChannelProvider { channel(); send(OutboundMessage) → DeliveryResult }` with `DeliveryResult.sent()` / `failed(error)` (retried) / `skippedNoProvider(reason)` (final) / `rejected(reason)` (1.1.0, final — omitted from CONSUMING §7); `send` runs on the event executor under the message's tenant with no transaction open; an application bean for a channel wins over the core one | channel/ChannelProvider.java:27-34; channel/DeliveryResult.java:12-27; channel/DeliveryStatus.java:4-21; docs/CONSUMING.md §7 |
| NEW | exposed (SPI) | `OutboundMessage(notificationLogId, tenantId, channel, recipientId, templateCode, nameAr, nameEn, subjectAr, subjectEn, bodyAr, bodyEn, moduleCode, referenceType, referenceId, variables)` with `titleAr()` / `titleEn()`; conventional variables `email`, `lang`, `actionLink`, `ctaLabelEn`, `ctaLabelAr`; `toString` never prints the variables | channel/OutboundMessage.java:31-80 |
| NEW | exposed | `NotifChannels` (`EMAIL`, `SMS`, `PUSH`, `IN_APP`), `TemplateText.render` | channel/NotifChannels.java:13-16; channel/TemplateText.java:20 |
| REMOVED | consumed | the `NotificationEvent` listener: NOTIF consumes no event of another module; its only listener handles its own `NotificationRequestedEvent` | service/NotificationDeliveryListener.java:46-47; ADR-NOTIF-001 |
| NEW | published | `NotificationRequestedEvent(notificationLogId, channel, recipientId, templateCode)` — after commit by dispatch for every `QUEUED` row, and at once (outside a transaction) by the requeue job; the listener drops an event that carries no tenant | service/DispatchService.java:156-158; service/NotificationRequeueJob.java:97-98; service/NotificationDeliveryListener.java:50-53 |
| NEW | published | `NotificationDispatchedEvent(notificationLogId, channel, recipientId, templateCode, attempts)` on `SENT` | service/NotificationDeliveryProcessor.java:164-165 |
| NEW | published | `NotificationFailedEvent(notificationLogId, channel, recipientId, templateCode, attempts, lastError)` on every path to `FAILED`: exhausted (last attempt or `@Recover`), `REJECTED`, and pre-claim exhaustion | service/NotificationDeliveryProcessor.java:225-226 (via `fail`: 116-120, 178-191, 206-214) |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.3 — password-change e-mail (`STAFF_PASSWORD_CHANGED`)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Ids continue from the highest ever issued for NOTIF (tree, this repository's history and `governance-shared`):
RULE-NOTIF-008, XM-NOTIF-002; RULE-NOTIF-001..022 are the as-built NOTIF rules of the analysis-coverage work (agreed), so this addendum adds RULE-NOTIF-023 and XM-NOTIF-003 (review round 1; first written as RULE-NOTIF-009). The `V17__notif_seed_password_changed.sql` header comment still says RULE-NOTIF-009: a migration is not edited for a comment. No endpoint, entity field,
error code or permission changes.

### 1. Business rules — NEW
| RULE-ID | Scope | Trigger | Statement | Source |
|---|---|---|---|---|
| RULE-NOTIF-023 | ENTITY-NOTIF-001 | `UserPasswordChangedEvent` (core event bus, after commit) | NOTIF dispatches the template `STAFF_PASSWORD_CHANGED` on channel `EMAIL` to the event's user (`recipientId = userId`, `moduleCode = SEC`, `referenceType = SEC_USER`, `referenceId = userId`), with variables `changedAt` (the event's `occurredAt`, UTC, `yyyy-MM-dd HH:mm 'UTC'`) and `changedBy` (the event's actor: the administrator for an admin-set, the user for a self-change). Runs asynchronously on the core event executor inside the event's tenant; never on rollback. A failure (template missing or inactive, recipient inactive, channel disabled) is logged and never affects the password change. The usual rules then apply (RULE-NOTIF-007 recipient eligibility, async delivery, retries). | SEC REQ-SEC-089; plan §6 D.3 |

The listener is `com.erp.notif.service.StaffPasswordChangedNotifier` (`@Async(ErpCoreEvents.EXECUTOR)` +
`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`, the step-08 listener pattern), calling
NOTIF's own `DispatchService`. It is the first NOTIF reaction to a core event other than its own
`NotificationRequestedEvent`; the event is a public `com.erp.events` type (the bus module every module may
depend on), not an internal SEC type, so the build-create-service rule "never listen for another module's
internal event" is respected. The earlier SEC mails (password reset, customer verification) keep calling
`NotificationDispatchApi` directly because they carry a secret link the event must not carry.

### 2. Cross-module (A7) — NEW
| XM-ID | Type | From | To | What |
|---|---|---|---|---|
| XM-NOTIF-003 | EVENT-CONSUME | NOTIF | events (published by SEC) | `com.erp.events.UserPasswordChangedEvent(userId, byAdmin)` → RULE-NOTIF-023 |

### 3. Templates (seed) — NEW
| Code | Channel | Variables | Seeded by |
|---|---|---|---|
| `STAFF_PASSWORD_CHANGED` | EMAIL (any channel may use it) | `{changedAt}`, `{changedBy}` | `V17__notif_seed_password_changed.sql` for every tenant existing at migration time (skipped where the tenant already has a template of that code); later tenants copy it from PLATFORM through `NotifTenantProvisioningContributor` (unchanged, it copies every PLATFORM template) |

### 4. Package B — a usage count for the platform (tenant-maturity plan §4 B.4)
Change         : tenant-maturity plan package B — `NotificationLogQueryApi.countDispatchedSince(Instant)`, consumed by TENANT's `GET /api/v1/platform/tenants/{id}/usage` (`notificationsLast30Days`)
Statement      : Sections 1–3 above (package D.3) are unchanged; §4 records package B's implemented delta.

No NOTIF id is minted (the count decides nothing; NOTIF's exposed surfaces carry no XM id — XM-NOTIF ids are
NOTIF's own consumptions). No endpoint, entity field, error code, permission or migration changes.

| Kind | Interface | Method | Contract |
|---|---|---|---|
| CHANGED | `com.erp.notif.crossmodule.NotificationLogQueryApi` (the dispatch-history read surface) | + `long countDispatchedSince(Instant since)` | the current tenant's `NOTIF_LOG` rows (one per channel and dispatch, any status) whose `CREATED_AT` is at or after `since`; read-only, tenant-filtered by the `@TenantId` discriminator, no `@PreAuthorize` (like `findByRecipientModuleAndReference`) |
Plan delta: the plan names "the existing dispatch API"; `NotificationDispatchApi` is NOTIF's write surface, so
the read went to `NotificationLogQueryApi` (TENANT srs-tenant.md 1.3.0 B10). Consumer: TENANT
(`TenantService.getUsage`, `since` = 30 days before `collectedAt`), inside `TenantContext.callAs(tenantId)`.

### 5. Package C12 — a suspended tenant's queued notifications wait (tenant-maturity plan §5 C.1)
Change         : tenant-maturity plan package C12 — the delivery claim and the requeue job skip the `QUEUED` rows of a tenant that is not ACTIVE (`TenantLookupApi.isActive`); `TenantActivatedEvent` re-dispatches them
Statement      : Sections 1–4 above (packages D.3 and B) are unchanged; §5 records package C12's implemented deltas.

Ids continue from the highest ever issued for NOTIF (tree, this repository's history and `governance-shared`):
RULE-NOTIF-023, XM-NOTIF-003. This section adds **RULE-NOTIF-024, XM-NOTIF-004, XM-NOTIF-005**. No endpoint,
entity field, status, error code, permission or migration changes (the status set LOV-NOTIF-002 is unchanged: no
`DEFERRED`).

#### 5.1 Business rules — NEW
| RULE-ID | Scope | Trigger | Statement | Source |
|---|---|---|---|---|
| RULE-NOTIF-024 | ENTITY-NOTIF-001 | a delivery attempt (`NotificationDeliveryProcessor.prepare`), the requeue job (`NotificationRequeueJob`), `TenantActivatedEvent` | The system shall not claim, attempt or re-dispatch a `QUEUED` row whose tenant is not ACTIVE (`TenantLookupApi.isActive(tenantId)`, XM-NOTIF-004): the attempt ends without touching the row (no claim, `ATTEMPTS` and `NEXT_ATTEMPT_AT` unchanged — the outcome `NOT_QUEUED`, "nothing was done") and the requeue job skips the tenant. The row keeps `QUEUED`; once the tenant is ACTIVE again it is delivered: on `TenantActivatedEvent` (XM-NOTIF-005) NOTIF re-dispatches that tenant's `QUEUED` rows that are not claimed (`NEXT_ATTEMPT_AT` null or past) and not pending on this node, and the requeue job (when enabled) picks any later stale one. A send already under way when the suspension commits is finished and recorded normally. This covers a dispatch made just before the suspension (its after-commit delivery finds the tenant suspended), a retry falling due during the suspension, and dispatches made inside a suspended tenant by system code. | TENANT REQ-TENANT-033; plan §5 C.1 |

Decided by `NotificationLogDomain.isDeliverable(boolean tenantActive)` (a `QUEUED` row of an ACTIVE tenant) in
`prepare`; the job's per-tenant skip reads the same fact (`TenantLookupApi.isActive`) before it loads any row.

#### 5.2 Cross-module (A7) — NEW
| XM-ID | Type | From | To | What |
|---|---|---|---|---|
| XM-NOTIF-004 | CROSSMODULE-READ | NOTIF | TENANT | `com.erp.tenant.crossmodule.TenantLookupApi.isActive(Long tenantId)` (TENANT XM-TENANT-001 CHANGED) — uncached, so a status change is seen by the next attempt |
| XM-NOTIF-005 | EVENT-CONSUME | NOTIF | events (published by TENANT) | `com.erp.events.TenantActivatedEvent(tenantId, tenantCode, actor)` → RULE-NOTIF-024 re-dispatch |

The listener is `com.erp.notif.service.NotificationTenantActivationListener`
(`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`) which submits the re-dispatch to the core event
executor itself, as `NotificationDeliveryListener` does, so a rejected task is logged at WARN (the held rows stay
`QUEUED` for the requeue job or a later activation) instead of failing silently (review round 1); the task reads inside
`TenantContext.callAs(event.getTenantId())` and publishes one `NotificationRequestedEvent` per row outside a
transaction, as the requeue job does. NOTIF registers no listener for
`TenantSuspendedEvent`: the claim-time check is the mechanism (a status read per attempt is enough; no state to keep).
`NotificationRequeueJob` gains a constructor taking `TenantLookupApi` (used by `ErpCoreNotifAutoConfiguration`); the
existing constructors keep working without the job-level skip (the claim still refuses).

#### 5.3 Plan delta
| Kind | Note |
|---|---|
| CHANGED (plan) | The plan: "the job simply does not claim them while suspended" — implemented at the claim (`prepare`), which every delivery path goes through (the in-process listener and the requeue job), plus the job's per-tenant skip. |
| NEW (decision) | Re-dispatch on `TenantActivatedEvent`: the requeue job is off by default (`erp.core.notif.requeue.enabled=false`), so without it a held row would wait for an application that enables the job. |

### 6. Package C5 — NOTIF's part of a tenant data export (tenant-maturity plan §5 C.5)
Change         : tenant-maturity plan package C5 — `NotifTenantExportContributor` implements TENANT's export SPI (XM-TENANT-004) for `POST /api/v1/platform/tenants/{id}/export`
Statement      : Sections 1–5 above (packages D.3, B and C12) are unchanged; §6 records package C5's implemented deltas.

No NOTIF id is minted (the export rules are TENANT's RULE-TENANT-027 / -028; the SPI is TENANT's XM-TENANT-004). No
endpoint, entity field, status, error code, permission or migration changes.

| Kind | Item | Delta | Source |
|---|---|---|---|
| NEW | `com.erp.notif.tenant.NotifTenantExportContributor` (`moduleCode` `NOTIF`) | writes `NOTIF/NOTIF_TEMPLATE`, `NOTIF_CHANNEL_CONFIG`, `NOTIF_LOG`, `NOTIF_INBOX` of the exported tenant with plain SQL naming `TENANT_ID` (RULE-TENANT-011), ordered by `ID` | `../../TENANT/P1/srs-tenant.md` 1.3.0 X5, X7 |
| NEW (rule applied) | never exported | `NOTIF_CHANNEL_CONFIG.CONFIG_JSON` (provider settings and credentials), `NOTIF_LOG.VARIABLES_JSON` (a queued row's template variables: password-reset and verification links carry raw tokens), every `TENANT_ID` / `VERSION` | TENANT RULE-TENANT-027 |

### Refactors without behaviour change — shared helpers moved to `com.erp.common`
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no NOTIF behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

| Kind | Section | Delta | Source |
|---|---|---|---|
| CHANGED | API-NOTIF-006 types | `com.erp.notif.dto.LookupOptionResponse` removed; the endpoint returns `com.erp.common.lookup.LookupOptionResponse` through `OwnedLookups.read` (same JSON `{code, labelAr, labelEn}`, same 404 `NOTIF_LOOKUP_KEY_UNKNOWN`) — a public type removal for in-JVM consumers only. | service/NotificationLookupService.java:4-5, 36-49 (main); CHANGELOG [Unreleased] |
| CHANGED | internals | Domain classes use `DomainRules.assertNotBlank` / `assertUnique` and `StatusTransitions`; `InAppChannelProvider` uses `Strings.truncate`; `DispatchVariables` uses `PlainJson.MAPPER`; `NotifLogSummaryReport` uses `UtcDates.startOfDay` — no behaviour change, no new error code. | git diff v1.2.0 -- erp-core/src/main/java/com/erp/notif; CHANGELOG [Unreleased] |
