<!-- Source: PHASE:DATA-DOM -->

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
