<!-- Source: PHASE:INT-C -->

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
