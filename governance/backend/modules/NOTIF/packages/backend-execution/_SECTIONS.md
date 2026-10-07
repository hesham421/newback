<!-- Source: content OUTSIDE all PHASE markers (trailing / between-phase sections — e.g. Plan Index, DB Alignment Manifest, Error Catalog, Agent Handoff Summary) -->

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