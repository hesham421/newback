<!-- Source: PHASE:SVC-API -->

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
