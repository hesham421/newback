# ui-ux-spec-NOTIF.md
## Notification Service (NOTIF) — Component-Level Design Intent

```
Produced by     : UI/UX Design Engine (Project 2.5)
Governed by     : CONTRACT-11 · fields/permissions are SRS B3/B4 reference only
Inputs          : srs-NOTIF.md v1.2 (PART B: B1/B3/B4) + prd-NOTIF.md
Status          : RECONCILED — pending human design approval (CONTRACT-12)
Date            : 2026-09-02
Scope note      : Component NAMES/CSS/routing are PROPOSAL/intent only (P3.2 F1/F4
                  decides binding spec). React/TS/Vite; t() for all strings; CSS
                  logical properties only.
```

---

```
Screen           : SCR-NOTIF-001 — Templates (إدارة قوالب الإشعارات)
UI Pattern       : PATTERN-2 (Search list + Side Drawer entry) — from SRS B1, unchanged
Create/Edit Container Pattern : SIDE_DRAWER
                   (config entity with bilingual long-text bodies + one attachment
                    reference; bounded, no repeating child rows → SIDE_DRAWER per
                    AMEND-P3-O; matches SRS B1. Propose a wide drawer for the bodies.)
Fields shown     : List/filters — templateCode, nameAr, isActiveFl.
                   Drawer (create/edit) — templateCode, nameAr, nameEn, subjectAr,
                   subjectEn, bodyAr, bodyEn, attachmentFileId (via File Service),
                   isActiveFl.
                   [SRS B3 — no additions, no omissions]
Permissions      : NOTIF_TEMPLATES → VIEW/CREATE/UPDATE/DELETE = NOTIF_ADMIN
                   (SRS B4 / CORE-9 — reference only)
Empty state      : "No templates yet." + "Create template".
Loading state    : List skeleton; drawer field skeletons on edit-load; attachment
                   picker shows its own loading state while querying File Service.
Error state      : templateCode uniqueness (RULE-NOTIF-006) inline; bilingual bodies
                   required (RULE-NOTIF-004) as inline field errors.
Design intent note (PROPOSAL): bodyAr/bodyEn as paired multiline editors (RTL for AR,
                   LTR for EN) — both required. attachmentFileId is chosen through the
                   File Service picker (UXD-NOTIF-002), stored as a file_id reference,
                   optional. templateCode read-only after create.
```

---

```
Screen           : SCR-NOTIF-002 — Channel Configuration (تهيئة القنوات)
UI Pattern       : PATTERN-2 (Search list + Side Drawer entry) — from SRS B1
Create/Edit Container Pattern : SIDE_DRAWER
                   (config entity: one row per channel, bounded fields → SIDE_DRAWER;
                    matches SRS B1)
Fields shown     : List/filters — channelTypeId (LOV-NOTIF-001), isEnabledFl.
                   Drawer (create/edit) — channelTypeId (LOV-NOTIF-001), isEnabledFl,
                   configJson.
                   [SRS B3]
Permissions      : NOTIF_CHANNELS → VIEW/CREATE/UPDATE/DELETE = NOTIF_ADMIN (SRS B4)
Empty state      : "No channels configured." Since the five channels are all in scope,
                   propose the list pre-populated per channel with an Enabled toggle.
Loading state    : List skeleton; drawer skeleton on edit-load.
Error state      : unique channel/config (RULE-NOTIF-006) inline; malformed configJson
                   surfaced as a validation notice on the JSON field.
Design intent note (PROPOSAL): isEnabledFl presented as a clear Enabled/Disabled
                   control; disabling communicates the dispatch consequence
                   (CHANNEL_DISABLED, no retry — RULE-NOTIF-003). configJson edited in a
                   monospace/JSON field; provider credentials live here as data, never
                   in code, and the concrete provider stays a P3 decision (provider-
                   independent — OQ-NOTIF-001 resolved). channelTypeId read-only after
                   create (it is the unique key).
```

---

```
Screen           : SCR-NOTIF-003 — Notification Log (سجل الإشعارات — read-only)
UI Pattern       : PATTERN-2 (Search list + read-only Side Drawer) — from SRS B1
Create/Edit Container Pattern : N/A — read-only screen (no ENTRY container)
                   (system-generated log; the drawer is a read-only detail panel, not
                    a create/edit form)
Fields shown     : List/filters — recipientId, moduleCode, channelTypeId (LOV-NOTIF-001),
                   notificationStatusId (LOV-NOTIF-002), referenceType, sentAt (date range).
                   Drawer (read-only) — all fields incl. errorMessage, retryCount,
                   referenceId, referenceType, templateFk, sentAt.
                   [SRS B3]
Permissions      : NOTIF_LOG → VIEW = NOTIF_ADMIN · CREATE/UPDATE/DELETE = — (none;
                   system-generated log). (SRS B4)
Empty state      : "No notifications logged for these filters."
Loading state    : List skeleton; drawer detail skeleton on open.
Error state      : query failures as a generic banner; no write paths to fail.
Design intent note (PROPOSAL): status shown as a clear state indicator
                   (PENDING / SENT / FAILED / CHANNEL_DISABLED) with FAILED rows
                   surfacing errorMessage + retryCount for troubleshooting. Recipient
                   rendered by name via SEC API (UXD-NOTIF-001). Strictly read-only —
                   no row actions that mutate the log.
```

---

## CROSS-MODULE (UXD) REFERENCE

- `UXD-NOTIF-001` — SCR-NOTIF-003 displays recipient identity from the **SEC
  UserAccount API** (SOFT-READ; recipientId is a SOFT reference).
- `UXD-NOTIF-002` — SCR-NOTIF-001 references an optional attachment via the **FILE
  Service API** (attachmentFileId).

Both recorded per Section 3A; confirmed against real SEC / FILE APIs by P4.2 before
FE implementation clears.

---

*End of ui-ux-spec-NOTIF.md — every field/permission traced to srs-NOTIF.md B3/B4.*
*Component names/CSS/routing are PROPOSAL intent, not binding (CONTRACT-11).*
