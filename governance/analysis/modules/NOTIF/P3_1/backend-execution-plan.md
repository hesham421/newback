<!-- backend-execution-plan.md — Governed by Execution Plan Governance Engine (Project 3.1 / PASS 1) -->

# BACKEND EXECUTION PLAN — Notification Service (NOTIF)

## SECTION 0 — PLAN HEADER
══════════════════════════════════════════════════════════════════
Plan Name        : New Feature — Multi-Channel Notification Service — NOTIF — BE
Plan ID          : PLAN-NOTIF-001
Task Type        : 🆕 New Feature   |   Feature Code: NOTIF-001 (srs-NOTIF.md v1.2)
Module           : Notification Service (NOTIF) — L1 Service — dep: CU, SEC, FILE — Backend + Frontend
Platform         : Foundation (Domain: ERP)   |   Truth Layer: Layer 3.1
DB_TARGET        : POSTGRESQL_16    BACKEND_STACK: SPRING_BOOT_JAVA    DBS-ID: DBS-NOTIF-001
Output Mode      : SINGLE-FILE — Agent-Ready Specification
GOVERNANCE STATE : NORMAL (srs.md + db-script.md both PRESENT)
Open Questions   : None (OQ-NOTIF-001 RESOLVED — provider-agnostic design; provider choice delegated to P3/impl)
══════════════════════════════════════════════════════════════════

---

## SECTION 1 — PLAN INDEX — NOTIF — PLAN-ID: PLAN-NOTIF-001
══════════════════════════════════════════════════════════════════
ENTITY REGISTRY
ENTITY-NOTIF-001 │ NotificationLog           │ NOTIF_LOG            │ NO │ Create(fan-out),Read/Query,Retry(internal) │ Transactional
ENTITY-NOTIF-002 │ NotificationTemplate      │ NOTIF_TEMPLATE       │ NO │ Create,Read,Update,Deactivate               │ Config/Master
ENTITY-NOTIF-003 │ NotificationChannelConfig │ NOTIF_CHANNEL_CONFIG │ NO │ Create,Read,Update(enable/disable),Deactivate│ Config

FIELD REGISTRY (FIELD-ID ↔ DBF-ID, 1:1)
FIELD-0001..0011 → DBF-0001..0011 : NOTIF_LOG (ID, RECIPIENT_ID, CHANNEL_TYPE_ID, NOTIFICATION_STATUS_ID, MODULE_CODE, REFERENCE_ID, REFERENCE_TYPE, RETRY_COUNT, ERROR_MESSAGE, SENT_AT, TEMPLATE_FK)
FIELD-0012..0021 → DBF-0012..0021 : NOTIF_TEMPLATE (ID, TEMPLATE_CODE, NAME_AR, NAME_EN, SUBJECT_AR, SUBJECT_EN, BODY_AR, BODY_EN, ATTACHMENT_FILE_ID, IS_ACTIVE_FL)
FIELD-0022..0025 → DBF-0022..0025 : NOTIF_CHANNEL_CONFIG (ID, CHANNEL_TYPE_ID, IS_ENABLED_FL, CONFIG_JSON)
Audit fields (createdBy/At, updatedBy/At): no DBF-ID (AuditEntityListener). NOTIF_CHANNEL_CONFIG has IS_ENABLED_FL only — no IS_ACTIVE_FL invented.

API REGISTRY
API-NOTIF-001 Dispatch POST /api/v1/notifications/dispatch     | API-NOTIF-002 Query logs GET /api/v1/notifications/logs
API-NOTIF-003 Log by id GET /api/v1/notifications/logs/{id}    | API-NOTIF-004 Templates CRUD /api/v1/notifications/templates
API-NOTIF-005 Channels CRUD /api/v1/notifications/channels     | API-NOTIF-006 Lookups GET /api/v1/notifications/lookups/{lookupKey}
Event listener (in-process): NotificationEvent via CU Events (e.g. SEC reset/activation events).

RULE REGISTRY : RULE-NOTIF-001..007 (RULE-NOTIF-002/003 carry ⚠ Client-Policy defaults; RULE-NOTIF-007 = OQ-SEC-001 consumer-side).
SCREEN REGISTRY (CORE-9)
SCR-NOTIF-001 │ Templates       │ COMPOSITE PATTERN-2 SIDE_DRAWER │ ENTITY-NOTIF-002 │ page_code NOTIF_TEMPLATES
SCR-NOTIF-002 │ Channel Config  │ COMPOSITE PATTERN-2 SIDE_DRAWER │ ENTITY-NOTIF-003 │ page_code NOTIF_CHANNELS
SCR-NOTIF-003 │ Notification Log │ COMPOSITE PATTERN-2 (read-only) │ ENTITY-NOTIF-001 │ page_code NOTIF_LOG (VIEW only)
LOV REGISTRY (NOTIF-local runtime codes)
LOV-NOTIF-001 │ NOTIF_CHANNEL │ channelTypeId          │ EMAIL, SMS, WHATSAPP, PUSH, INTERNAL
LOV-NOTIF-002 │ NOTIF_STATUS  │ notificationStatusId   │ PENDING, SENT, FAILED, CHANNEL_DISABLED
QRC SUMMARY : QR-NOTIF-0001..0013 (SECTION B). ⚠ AGENT REFERENCE only.
DB ALIGNMENT : SECTION 2 — ALIGNED ✓ (25 FIELD↔DBF)
XM STATUS    : XM-NOTIF-001 SOFT-READ → SEC_USER_ACCOUNT (READY ✓); XM-NOTIF-002 SOFT-READ → FILE_DOCUMENT via FileService API (READY ✓)
SECURITY     : 3 admin screens × 4 CORE-9 permissions (NOTIF_ADMIN; NOTIF_LOG is VIEW-only). Auth delegated to Security filter.
══════════════════════════════════════════════════════════════════

---

## SECTION 2 — DB ALIGNMENT MANIFEST — NOTIF — DBS-ID: DBS-NOTIF-001
══════════════════════════════════════════════════════════════════
FIELD-ID   │ DBF-ID    │ Plan Type │ FK/XM-ID                                  │ Match
───────────┼───────────┼───────────┼───────────────────────────────────────────┼──────
FIELD-0002 │ DBF-0002  │ Long      │ XM-NOTIF-001 SOFT-READ → SEC_USER_ACCOUNT │ ✓ (no FK)
FIELD-0011 │ DBF-0011  │ Long      │ FK TEMPLATE_FK → NOTIF_TEMPLATE           │ ✓
FIELD-0020 │ DBF-0020  │ Long      │ XM-NOTIF-002 SOFT-READ → FILE_DOCUMENT    │ ✓ (no FK, via FileService)
All other FIELD-IDs align 1:1 to their DBF-ID (see FIELD REGISTRY). No type mismatch.
══════════════════════════════════════════════════════════════════
Legend: ✓ aligned | ✗ mismatch | ⏸ XM deferred. CONTRACT-1: FIELD-ID/DBF-ID/Type/FK-XM/Status only. Audit cols: no DBF-ID.

---

## SECTION 3 — OPEN QUESTIONS LOG (continuation)
══════════════════════════════════════════════════════════════════
OQ-NOTIF-001 │ Actual provider per channel (SMS/WhatsApp/Push) │ RESOLVED (Architect, 2026-09-02)
  Resolution: design stays provider-agnostic; concrete provider choice delegated to P3/implementation via configJson —
  no impact on tables or SRS. No open questions remain.
══════════════════════════════════════════════════════════════════

---

## SECTION 4 — DERIVATION LOG
══════════════════════════════════════════════════════════════════
DRV-001 │ ERR-0004 NOT_FOUND (log/template/channel) │ PLATFORM │ Standard 404 by id / dispatch with unknown templateCode
DRV-002 │ QR-NOTIF-0012/0013 EXISTS templateCode/channel │ CRIT-2 │ RULE-NOTIF-006 pre-insert uniqueness checks
DRV-003 │ Retry with backoff (≤5, 2s ×1.5)          │ CRIT-3   │ RULE-NOTIF-002 dispatch failure workflow (internal, in-process; no external broker)
DRV-004 │ Provider abstraction (ChannelProvider iface)│ CRIT-3  │ RULE-NOTIF-001 provider-agnostic fan-out; concrete provider from configJson (OQ-NOTIF-001)
DRV-005 │ Recipient-active pre-check via XM-NOTIF-001 │ CRIT-2   │ RULE-NOTIF-007 skip inactive recipient (SOFT-READ of SEC UserAccount)
══════════════════════════════════════════════════════════════════

---

<!-- PHASE:CORE:START -->
## PHASE CORE — Architectural Policies
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
CANONICAL ARCHITECTURE (backend): controller/ service/ mapper/ domain/ repository/ entity/ dto/ exception/ config/
Domain behavior placement: separate classes in domain/ (dispatch fan-out, retry-with-backoff, provider abstraction, recipient-active check are non-trivial — e.g. DispatchDomainService, RetryPolicy, ChannelProvider strategy).
PROJECT-STANDARD CONSTRAINTS:
  Entity base      : AuditableEntity on all 3 tables (audit via AuditEntityListener). ✗ orgUnitId never in any DTO.
  Error signaling  : LocalizedException — NotFoundException BANNED.
  Error catalog    : every ERR-ID registered 4× (ErrorCodes + messages.properties + i18n JSON + ErpErrorMapperService).
  Search contract  : SearchRequest extends BaseSearchContractRequest; ALLOWED_SORT_FIELDS per search.
  Deactivation     : NotificationTemplate isActiveFl=false. NotificationChannelConfig uses IS_ENABLED_FL only (enable/disable) — no IS_ACTIVE_FL.
  Auth             : delegate to Security filter — NOTIF never self-verifies JWT (RULE-NOTIF-005).
  Events           : consumes CU in-process ApplicationEvents (NotificationEvent). No external broker (RabbitMQ/Kafka not used).
  Providers        : ChannelProvider is an interface; concrete provider resolved from NOTIF_CHANNEL_CONFIG.configJson (provider-agnostic — OQ-NOTIF-001, DRV-004).
TYPE MAPPING (POSTGRESQL_16): BIGINT→Long · VARCHAR(N)→String · TEXT→String(+@Lob) · SMALLINT(_FL)→Boolean · TIMESTAMP→LocalDateTime.
MODULE-SPECIFIC NOTES:
  - Dispatch fans out ONE NOTIF_LOG row per requested channel (RULE-NOTIF-001); the service is business-neutral (no sender routing logic).
  - Disabled channel → row with notificationStatusId=CHANNEL_DISABLED, no retry (RULE-NOTIF-003).
  - Failure → retry ≤5 (2s, ×1.5 backoff) then FAILED (RULE-NOTIF-002).
  - Inactive recipient → skip dispatch (RULE-NOTIF-007); historical logs retained.
  - LOV values runtime codes (no ENUM, no lookup table). No Business Code. No Workflow Engine (RULE-13 = OFF).
─────────────────────────────────────────────────────────────────
<!-- PHASE:CORE:END -->

<!-- PHASE:DATA-DOM:START -->
## PHASE DATA+DOM — Entity & Domain Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
(3 entities < 5 → no SUB split)

### ENTITY-NOTIF-002 — NotificationTemplate (parent — built first)
  DB Table NOTIF_TEMPLATE | PK ID | Sequence SEQ_NOTIF_TEMPLATE. BUSINESS CODE: NONE.
  FIELDS 0012..0021: templateCode(UNIQUE), nameAr, nameEn, subjectAr, subjectEn, bodyAr(NOT NULL), bodyEn(NOT NULL), attachmentFileId(SOFT→FILE), isActiveFl.
  DOMAIN RULES: RULE-NOTIF-004 (bilingual bodyAr+bodyEn required; attachment via FILE file_id — Message-AR: قوالب ثنائية اللغة والمرفق عبر الملفات.);
       RULE-NOTIF-006 (unique templateCode — Message-AR: رمز القالب والقناة فريدان.).
  CROSS-MODULE: XM-NOTIF-002 SOFT-READ → FILE_DOCUMENT (attachmentFileId, via FileService API, no FK). QR: QR-NOTIF-0007..0010,0012.

### ENTITY-NOTIF-003 — NotificationChannelConfig (parent)
  DB Table NOTIF_CHANNEL_CONFIG | PK ID | Sequence SEQ_NOTIF_CHANNEL_CONFIG. FIELDS 0022..0025: channelTypeId(LOV-NOTIF-001, UNIQUE), isEnabledFl, configJson(provider config).
  DOMAIN RULES: RULE-NOTIF-006 (unique channelTypeId config); RULE-NOTIF-003 (isEnabledFl=0 → CHANNEL_DISABLED at dispatch). QR: QR-NOTIF-0011,0013.

### ENTITY-NOTIF-001 — NotificationLog (child — FK → NOTIF_TEMPLATE)
  DB Table NOTIF_LOG | PK ID | Sequence SEQ_NOTIF_LOG.
  FIELDS 0001..0011: recipientId(XM-NOTIF-001 SOFT→SEC), channelTypeId(LOV-NOTIF-001), notificationStatusId(LOV-NOTIF-002, A6), moduleCode, referenceId?, referenceType?, retryCount(sys), errorMessage?, sentAt?, templateFk(FK→NOTIF_TEMPLATE).
  STATE MACHINE (notificationStatusId — LOV-NOTIF-002): PENDING → SENT | PENDING → FAILED (after retries) | PENDING → CHANNEL_DISABLED. Initial: PENDING.
  DOMAIN RULES: RULE-NOTIF-001 (fan-out one row per channel), RULE-NOTIF-002 (retry ≤5 then FAILED), RULE-NOTIF-003 (disabled → CHANNEL_DISABLED),
       RULE-NOTIF-005 (delegate auth), RULE-NOTIF-007 (skip inactive recipient; history retained — Message-AR: لا يُرسَل إشعار لمستلِم حسابه غير نشط؛ تُبقى السجلات التاريخية.).
  CROSS-MODULE: XM-NOTIF-001 SOFT-READ → SEC_USER_ACCOUNT (recipientId, no FK). QR: QR-NOTIF-0001..0006.

DATA+DOM Governance: BIND-RULE-1/2/3/4 — exact column/sequence/LOOKUP_CODE/RULE text from srs-NOTIF/db-script-NOTIF.
─────────────────────────────────────────────────────────────────
<!-- PHASE:DATA-DOM:END -->

<!-- PHASE:SVC-API:START -->
## PHASE SVC+API — Service & API Contract Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
(6 APIs < 8 → no SUB split; atomic API markers applied)

<!-- API:API-NOTIF-001:START -->
### API-NOTIF-001 — Dispatch notification (fan-out)
POST /api/v1/notifications/dispatch | DispatchController.dispatch → DispatchDomainService.dispatch
REQUEST DispatchRequest{recipientId, templateCode, channelHint[EMAIL,SMS,...], moduleCode, referenceId?, referenceType?, variables?} | RESPONSE 202 {logIds[]}
VALIDATIONS: RULE-NOTIF-004 (template must exist & be bilingual); RULE-NOTIF-007 (recipient UserAccount must be active — else skip, Message-AR: لا يُرسَل إشعار لمستلِم حسابه غير نشط؛ تُبقى السجلات التاريخية.);
             RULE-NOTIF-001 (fan out one NOTIF_LOG per requested channel); RULE-NOTIF-003 (disabled channel → CHANNEL_DISABLED); RULE-NOTIF-002 (retry ≤5 then FAILED)
ERRORS: ERR-0004 → PLATFORM NOT_FOUND (unknown templateCode) → 404
BEHAVIORAL OUTCOMES (recorded as NOTIF_LOG.notificationStatusId, NOT HTTP errors): inactive recipient → skipped (RULE-NOTIF-007);
             disabled channel → CHANNEL_DISABLED (RULE-NOTIF-003); send failure → retried then FAILED (RULE-NOTIF-002).
ORCHESTRATION: resolve template (QR-NOTIF-0007) → check recipient active via XM-NOTIF-001 (RULE-NOTIF-007) → for each channelHint: check channel enabled (QR-NOTIF-0011, RULE-NOTIF-003) → create PENDING log (QR-NOTIF-0001) → ChannelProvider send with retry (DRV-003) → set SENT/FAILED.
REPO: QR-NOTIF-0001 SAVE + QR-NOTIF-0006 UPDATE status — READ_WRITE — Sequence SEQ_NOTIF_LOG | SECURITY: Security filter (RULE-NOTIF-005); also invokable via NotificationEvent listener.
<!-- API:API-NOTIF-001:END -->
<!-- API:API-NOTIF-002:START -->
### API-NOTIF-002 — Query notification logs
GET /api/v1/notifications/logs | NotificationLogController.search → NotificationLogService.search
REQUEST params: recipientId?, moduleCode?, channelTypeId?(EXACT), notificationStatusId?(EXACT), referenceType?, sentAtFrom?/sentAtTo?(DATE_RANGE), page,size; ALLOWED_SORT_FIELDS={createdAt,sentAt,notificationStatusId}
RESPONSE 200 Page<NotificationLogResponse> (empty → 200 [], never 404) | ERRORS: none
REPO: QR-NOTIF-0002 FIND_BY_CRITERIA — READ_ONLY — Join NONE | SECURITY: SCR-NOTIF-003 VIEW.
<!-- API:API-NOTIF-002:END -->
<!-- API:API-NOTIF-003:START -->
### API-NOTIF-003 — Log by id
GET /api/v1/notifications/logs/{id} | NotificationLogController.getById → NotificationLogService.getById
RESPONSE 200 NotificationLogResponse (incl. errorMessage, retryCount)
ERRORS: ERR-0004 → NOT_FOUND → 404
REPO: QR-NOTIF-0003 FIND_ONE — READ_ONLY | SECURITY: SCR-NOTIF-003 VIEW.
<!-- API:API-NOTIF-003:END -->
<!-- API:API-NOTIF-004:START -->
### API-NOTIF-004 — Templates CRUD
POST/GET/PUT/DELETE /api/v1/notifications/templates(/{id}) | TemplateController → TemplateService
REQUEST TemplateCreate/UpdateRequest{templateCode(create-only), nameAr, nameEn, subjectAr?, subjectEn?, bodyAr, bodyEn, attachmentFileId?, isActiveFl}
RESPONSE 201/200 TemplateResponse; search → Page<TemplateResponse>
VALIDATIONS: RULE-NOTIF-004 (bodyAr & bodyEn required — bilingual — Message-AR: قوالب ثنائية اللغة والمرفق عبر الملفات.); RULE-NOTIF-006 (unique templateCode — Message-AR: رمز القالب والقناة فريدان.)
ERRORS: ERR-0001 → RULE-NOTIF-004 missing bilingual body → 400; ERR-0002 → RULE-NOTIF-006 dup templateCode → 409; ERR-0004 → NOT_FOUND → 404
NOTE: attachmentFileId validated via FileService (XM-NOTIF-002) — unresolved file id → 400 (delegated to FILE).
REPO: QR-NOTIF-0007..0010,0012 — mixed — Sequence SEQ_NOTIF_TEMPLATE | ALLOWED_SORT_FIELDS={templateCode,nameAr,createdAt}
SECURITY: SCR-NOTIF-001 (VIEW/CREATE/UPDATE/DELETE — NOTIF_ADMIN).
<!-- API:API-NOTIF-004:END -->
<!-- API:API-NOTIF-005:START -->
### API-NOTIF-005 — Channels CRUD / enable-disable
POST/GET/PUT/DELETE /api/v1/notifications/channels(/{id}) | ChannelController → ChannelService
REQUEST ChannelCreate/UpdateRequest{channelTypeId(LOV-NOTIF-001, unique), isEnabledFl, configJson?}
RESPONSE 201/200 ChannelResponse; search → Page<ChannelResponse>
VALIDATIONS: RULE-NOTIF-006 (unique channelTypeId config — Message-AR: رمز القالب والقناة فريدان.); RULE-NOTIF-003 (isEnabledFl drives dispatch behavior)
ERRORS: ERR-0003 → RULE-NOTIF-006 dup channel config → 409; ERR-0004 → NOT_FOUND → 404
REPO: QR-NOTIF-0011,0013 — mixed — Sequence SEQ_NOTIF_CHANNEL_CONFIG | ALLOWED_SORT_FIELDS={channelTypeId,createdAt}
SECURITY: SCR-NOTIF-002 (VIEW/CREATE/UPDATE/DELETE — NOTIF_ADMIN).
<!-- API:API-NOTIF-005:END -->
<!-- API:API-NOTIF-006:START -->
### API-NOTIF-006 — Lookups
GET /api/v1/notifications/lookups/{lookupKey} | NotificationLookupController.get → NotificationLookupService.get
REQUEST path lookupKey ∈ {NOTIF_CHANNEL, NOTIF_STATUS} | RESPONSE 200 [{code,labelAr,labelEn}]
VALIDATIONS: none | ERRORS: ERR-0004 → unknown lookupKey → 404
BINDING: LOV-NOTIF-001 NOTIF_CHANNEL (EMAIL,SMS,WHATSAPP,PUSH,INTERNAL); LOV-NOTIF-002 NOTIF_STATUS (PENDING,SENT,FAILED,CHANNEL_DISABLED).
REPO: (runtime code resolution — no lookup table) — READ_ONLY | SECURITY: Security filter.
<!-- API:API-NOTIF-006:END -->

API Governance: RULE-ERR-CARRY ✓ (validation rules with user-facing errors carry ERR-IDs; RULE-NOTIF-001/002/003/007 are dispatch behaviors recorded as log STATUS, not HTTP errors — documented above, DRV-003/005);
RULE-PLATFORM-ERR ✓ (ERR-0004 = PLATFORM-STD, DRV-001); LOC ✓ (AR+EN on every error). RULE-NOTIF-005 internal (auth delegation).
─────────────────────────────────────────────────────────────────
<!-- PHASE:SVC-API:END -->

<!-- PHASE:DOC:START -->
## PHASE DOC — Contract Stabilization (INTERNAL-ONLY, v2.0)
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
DOC-1: API-NOTIF-001..006 all STABLE (dispatch 202 async-style; logs read-only). DOC-2: LOV fields String code; no Business Code; configJson is String (TEXT).
DOC-3: JPA Page<T>; empty → 200; filters channel/status EXACT, sentAt DATE_RANGE, moduleCode/recipientId EXACT.
DOC GATE: PASSED ✓  ⚠ INTERNAL-ONLY — PASS 2 gates on real API Docs (CONTRACT-12).
─────────────────────────────────────────────────────────────────
<!-- PHASE:DOC:END -->

<!-- PHASE:INT-C:START -->
## PHASE INT-C — Integration Contract Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
## INT-C SUMMARY — NOTIF — PLAN-ID: PLAN-NOTIF-001
XM-ID        │ Classification │ Target Module │ Interface          │ Contract Status
─────────────┼────────────────┼───────────────┼────────────────────┼────────────────
XM-NOTIF-001 │ SOFT-READ      │ Security (SEC)│ App-layer identity │ CONTRACTED ✓
XM-NOTIF-002 │ SOFT-READ      │ File Service  │ FileService API    │ CONTRACTED ✓

<!-- XM:XM-NOTIF-001:START -->
### XM-NOTIF-001 — SOFT-READ of SEC UserAccount (recipient identity)
Target: Security (SEC) → SEC_USER_ACCOUNT (ENTITY-SEC-001) | Local: NOTIF_LOG.RECIPIENT_ID (DBF-0002)
Classification: SOFT-READ (app layer, NO FK). Data required: recipient identity + active status.
Contract: read recipient via SEC (auth filter / user lookup); if UserAccount inactive → skip dispatch (RULE-NOTIF-007), record retained.
Fallback if absent: dispatch skipped for that recipient; log reflects skip. Blocks: none (SEC gated ACTIVE).
Unblock condition: n/a (READY). Idempotency: read-only. Retry/Timeout: n/a (in-process).
<!-- XM:XM-NOTIF-001:END -->

<!-- XM:XM-NOTIF-002:START -->
### XM-NOTIF-002 — SOFT-READ of FILE attachment (via FileService API)
Target: File Service (FILE) → FILE_DOCUMENT (ENTITY-FILE-001) | Local: NOTIF_TEMPLATE.ATTACHMENT_FILE_ID (DBF-0020)
Classification: SOFT-READ (app layer via FileService provider, NO FK).
Contract: optional template attachment resolved through FileService.retrieve/issueAccessToken (FILE provider @Service, in-process).
Fallback if absent: attachment omitted; template still valid (attachmentFileId nullable). Blocks: none (FILE gated ACTIVE, built before NOTIF).
Unblock condition: n/a (READY). Idempotency: read-only. Retry/Timeout: per FileService.
<!-- XM:XM-NOTIF-002:END -->

INT-C GATE CHECK: [✓] all XM from DB Register accounted (XM-NOTIF-001, XM-NOTIF-002) [✓] classification declared [✓] no new XM invented [✓] Open RXEs none [✓] all DEFERRED have unblock (none deferred)
INT-C Gate: PASSED ✓
─────────────────────────────────────────────────────────────────
<!-- PHASE:INT-C:END -->

<!-- PHASE:INT-R:START -->
## PHASE INT-R — Runtime Activation Status
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
## INT-R STATUS — NOTIF — PLAN-ID: PLAN-NOTIF-001
<!-- XM:XM-NOTIF-001-RT:START -->
XM-NOTIF-001 │ READY ✓ │ — (SEC_USER_ACCOUNT ACTIVE under DBS-SEC-001; SEC built before NOTIF; recipient identity read via Security at runtime)
<!-- XM:XM-NOTIF-001-RT:END -->
<!-- XM:XM-NOTIF-002-RT:START -->
XM-NOTIF-002 │ READY ✓ │ — (FILE_DOCUMENT ACTIVE under DBS-FILE-001; FILE built before NOTIF; attachment resolved via FileService @Service; nullable so absence is non-blocking)
<!-- XM:XM-NOTIF-002-RT:END -->
─────────────────────────────────────────────────────────────────
<!-- PHASE:INT-R:END -->

<!-- PHASE:SEC-BE:START -->
## PHASE SEC-BE — Backend Security Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
CORE-9 — one SCR-ID = one SEC_PAGES row = 4 permissions (generated by Security, not seeded by name).

### SEC-BE — SCR-NOTIF-001 — Templates (page_code NOTIF_TEMPLATES)
  API enforcement: API-NOTIF-004 CRUD → VIEW/CREATE/UPDATE/DELETE. Roles: NOTIF_ADMIN (all).
### SEC-BE — SCR-NOTIF-002 — Channel Config (page_code NOTIF_CHANNELS)
  API enforcement: API-NOTIF-005 CRUD → VIEW/CREATE/UPDATE/DELETE. Roles: NOTIF_ADMIN.
### SEC-BE — SCR-NOTIF-003 — Notification Log (page_code NOTIF_LOG, read-only)
  API enforcement: API-NOTIF-002/003 → VIEW only (system log; CREATE/UPDATE/DELETE not exposed). Roles: NOTIF_ADMIN VIEW.
Dispatch (API-NOTIF-001) is a service/event endpoint behind the Security filter (RULE-NOTIF-005), not tied to a management screen.
SECURITY SEED DATA REQUIREMENTS:
  SEC_PAGE rows (owned by SEC, registered for NOTIF): NOTIF_TEMPLATES, NOTIF_CHANNELS, NOTIF_LOG (parent: Notifications).
  Permissions auto-generated 4-per-page by Security (RULE-SEC-011); NOTIF_LOG effectively grants VIEW only → NOTIF_ADMIN.
SEC-BE Rules: SEC-IMPL-RULE-1/3/4 applied.
─────────────────────────────────────────────────────────────────
<!-- PHASE:SEC-BE:END -->

<!-- PHASE:ALIGN-BE:START -->
## PHASE ALIGN-BE — Backend Internal Self-Consistency Gate (auto-run)
─────────────────────────────────────────────────────────────────
## ALIGN-BE GATE — NOTIF — PLAN-ID: PLAN-NOTIF-001
Traceability: all FIELD/API/RULE/ERR/QR/XM-IDs appear in Plan Index ✓ | Derivation Log complete ✓ | DB field coverage ✓
Business Code: N/A ✓ | Localization: all RULE Message-AR ✓ | error responses AR+EN ✓
Security: every screen-serving API-ID has permission declared ✓ | SCR-NOTIF-001/002/003 have SEC-BE blocks ✓ | CORE-9 ✓
QRC: every DB-op API has QR-ID ✓ | agent-reference labels ✓ | no ENUM for LOV ✓ | no join to lookups ✓ | exact sequence names on SAVE ✓
TEST-BE: SECTION D present ✓ | no GAP without DEFERRED ✓
Artifact binding: no placeholders ✓ | RULE text inline ✓ | every column→DBF-ID ✓ | Message-AR exact ✓ | Manifest CONTRACT-1 ✓
Plan completeness: CORE arch ✓ | domain placement ✓ | no orgUnitId in DTO ✓ | no audit in Create/Update ✓ | LocalizedException ✓ | ERR 4-registration ✓ | ALLOWED_SORT_FIELDS ✓ | empty search→200 ✓ | IS_ENABLED_FL only for channel (no invented IS_ACTIVE_FL) ✓
CROSS-MODULE: XM-NOTIF-001/002 SOFT-READ declared + READY ✓ | RULE-NOTIF-007 (skip inactive recipient) applied ✓ | INBOUND stubs n/a
═══════════════════════════════════════════════════════════════════
ALIGN-BE GATE RESULT: PASSED ✓  |  Auto-correction: None
═══════════════════════════════════════════════════════════════════

Table 3 — XM Dependency Gate:
XM-NOTIF-001 │ SOFT-READ │ READY ✓ │ — │ —
XM-NOTIF-002 │ SOFT-READ │ READY ✓ │ — │ —
─────────────────────────────────────────────────────────────────
<!-- PHASE:ALIGN-BE:END -->

---

## SECTION A — ERROR CATALOG (canonical)
══════════════════════════════════════════════════════════════════════════════════
ERR-ID   │ RULE-ID        │ API-ID              │ HTTP │ Trigger                 │ Message-AR                                          │ Message-EN
─────────┼────────────────┼─────────────────────┼──────┼─────────────────────────┼──────────────────────────────────────────────────────┼──────────────────────────
ERR-0001 │ RULE-NOTIF-004 │ API-NOTIF-004       │ 400  │ Missing bilingual body   │ قوالب ثنائية اللغة والمرفق عبر الملفات.               │ Bilingual template body required.
ERR-0002 │ RULE-NOTIF-006 │ API-NOTIF-004       │ 409  │ Duplicate template code  │ رمز القالب والقناة فريدان.                            │ Template code must be unique.
ERR-0003 │ RULE-NOTIF-006 │ API-NOTIF-005       │ 409  │ Duplicate channel config │ رمز القالب والقناة فريدان.                            │ Channel config must be unique.
ERR-0004 │ PLATFORM-STD   │ API-NOTIF-001/003/004/005/006 │ 404 │ Resource not found │ العنصر غير موجود.                                    │ Resource not found.
══════════════════════════════════════════════════════════════════════════════════
Total Errors: 4 (ERR-0004 = PLATFORM-STD, DRV-001).
Behavioral rules RULE-NOTIF-001/002/003/007 produce NOTIF_LOG status outcomes (SENT/FAILED/CHANNEL_DISABLED/skipped), not HTTP errors — no ERR-ID by design (documented in SVC+API). RULE-NOTIF-005 internal (auth delegation).
Every ERR-ID registered in 4 places.

---

## SECTION B — QUERY REFERENCE CATALOG (agent reference)
══════════════════════════════════════════════════════════════════
⚠ AGENT REFERENCE ONLY — rewrite every query using actual JPA entity/field names.
QR-NOTIF-0001 SAVE notification log row (SEQ_NOTIF_LOG) — READ_WRITE
QR-NOTIF-0002 FIND_BY_CRITERIA logs (recipient/module/channel/status EXACT, sentAt DATE_RANGE) — READ_ONLY, paged
QR-NOTIF-0003 FIND_ONE log by ID — READ_ONLY
QR-NOTIF-0004 FIND logs by templateFk (referential checks) — READ_ONLY
QR-NOTIF-0005 FIND retryable logs (status/retryCount) for retry processing — READ_ONLY
QR-NOTIF-0006 UPDATE log status/retryCount/sentAt/errorMessage — READ_WRITE
QR-NOTIF-0007 FIND_ONE template by code/ID (dispatch resolution) — READ_ONLY
QR-NOTIF-0008 FIND_BY_CRITERIA templates — READ_ONLY, paged
QR-NOTIF-0009 SAVE template (SEQ_NOTIF_TEMPLATE) — READ_WRITE
QR-NOTIF-0010 UPDATE template — READ_WRITE
QR-NOTIF-0011 FIND_ONE channel config by channelTypeId (enabled check) — READ_ONLY
QR-NOTIF-0012 EXISTS templateCode (RULE-NOTIF-006) — READ_ONLY
QR-NOTIF-0013 SAVE/UPDATE channel config; EXISTS channelTypeId (RULE-NOTIF-006) (SEQ_NOTIF_CHANNEL_CONFIG) — READ_WRITE/READ_ONLY
Join governance: NEVER join to a lookups table (none). NEVER join to SEC_USER_ACCOUNT or FILE_DOCUMENT (SOFT-READ via service, not SQL join).
══════════════════════════════════════════════════════════════════

---

## SECTION C — REGISTRY UPDATE BLOCK
══════════════════════════════════════════════════════════════════
## REGISTRY UPDATE — 2026-09-02
Source: Project 3.1 PASS 1 | Feature Code NOTIF-001 | DBS-NOTIF-001 | Plan PLAN-NOTIF-001
New APIs: API-NOTIF-001..006 | QR-IDs: QR-NOTIF-0001..0013 (13)
XM-IDs: XM-NOTIF-001 SOFT-READ → SEC (READY ✓); XM-NOTIF-002 SOFT-READ → FILE (READY ✓)
OQ-IDs Open: None (OQ-NOTIF-001 RESOLVED) | Gate Status: ALIGN-BE PASSED ✓ | Next: Project 4.1 → Pipeline Grid NOTIF · P3.1 = done
══════════════════════════════════════════════════════════════════

---

## SECTION D — TC COVERAGE MATRIX SUMMARY (backend)
══════════════════════════════════════════════════════════════════
RULE-ID COVERAGE:
RULE-NOTIF-001 │ TC-BE-NOTIF-001 │ —              │ COVERED ✓ (fan-out count assertion)
RULE-NOTIF-002 │ TC-BE-NOTIF-002 │ TC-BE-NOTIF-003 │ COVERED ✓ (retry then FAILED)
RULE-NOTIF-003 │ TC-BE-NOTIF-004 │ —              │ COVERED ✓ (disabled → CHANNEL_DISABLED)
RULE-NOTIF-004 │ TC-BE-NOTIF-005 │ TC-BE-NOTIF-006 │ COVERED ✓
RULE-NOTIF-005 │ TC-BE-NOTIF-007 │ —              │ COVERED ✓ (auth-delegation assertion)
RULE-NOTIF-006 │ TC-BE-NOTIF-008 │ TC-BE-NOTIF-009 │ COVERED ✓
RULE-NOTIF-007 │ TC-BE-NOTIF-010 │ —              │ COVERED ✓ (skip inactive recipient; history retained)
Rule coverage: 7/7 — 0 gaps.
API-ID COVERAGE: API-NOTIF-001..006 each ≥1 happy-path TC (TC-BE-NOTIF-011..016) — 6/6 covered.
DEFERRED TC REGISTRY: (none)
══════════════════════════════════════════════════════════════════
Gate SECTION D: PASSED ✓

---

## AGENT HANDOFF SUMMARY (BACKEND) — not a phase
Agent-ready. Rewrite QRC from scratch; implement ChannelProvider abstraction (provider from configJson, provider-agnostic);
fan-out one log per channel; retry ≤5 (2s ×1.5) then FAILED; disabled channel → CHANNEL_DISABLED (no retry); skip inactive recipient (RULE-NOTIF-007, history retained);
consume CU NotificationEvent (no external broker); resolve attachments via FileService (XM-NOTIF-002); delegate auth to Security filter. Run api-doc-generator before PASS 2.

*End of backend-execution-plan.md — NOTIF — PLAN-NOTIF-001 — ALIGN-BE ✓*
