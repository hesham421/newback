# flow-diagram-NOTIF.md
## Notification Service (NOTIF) — Navigation & Sequencing

```
Produced by     : UI/UX Design Engine (Project 2.5)
Governed by     : CONTRACT-11 (PRD↔SRS Reconciliation Gate)
Inputs          : prd-NOTIF.md (US-NOTIF-001..005) + srs-NOTIF.md v1.2 (PART B: B1–B5)
Mode            : RECONCILE (both PRD and SRS attached)
Status          : RECONCILED — Gate PASSED — pending human design approval (CONTRACT-12)
Date            : 2026-09-02
```

---

## RECONCILIATION GATE — RESULT

| US-ID | Intent (PRD) | SRS counterpart | Reconciled |
|---|---|---|---|
| US-NOTIF-001 | Notify a user over one or more channels | API-NOTIF-001 · RULE-NOTIF-001 · service/dispatch behavior (no UI) | ✓ via API-ID (service) |
| US-NOTIF-002 | Sending module chooses channel(s) via channelHint | API-NOTIF-001 · RULE-NOTIF-001 · service behavior (no UI) | ✓ via API-ID (service) |
| US-NOTIF-003 | Bilingual message templates + optional attachment | SCR-NOTIF-001 · API-NOTIF-004 · RULE-NOTIF-004 | ✓ SCR-NOTIF-001 |
| US-NOTIF-004 | Enable/disable each channel + store provider config | SCR-NOTIF-002 · API-NOTIF-005 · RULE-NOTIF-003 | ✓ SCR-NOTIF-002 |
| US-NOTIF-005 | See each notification's status & delivery result | SCR-NOTIF-003 · API-NOTIF-002/003 | ✓ SCR-NOTIF-003 |

```
Reconciled, no rework needed : 5 / 5 user stories
Flagged for rework           : 0
Blocked (OQ)                 : 0
Contradictions (OQ)          : 0
```

**Grouping decision (this engine):** US-NOTIF-001 and US-NOTIF-002 describe the
**dispatch/fan-out service behavior**, not an admin screen — they are triggered by
a CU event or API-NOTIF-001, with the service staying business-logic-neutral
(RULE-NOTIF-001). Their user-visible OUTCOME (status per channel) surfaces on
SCR-NOTIF-003 (the log). No screen is invented for them.

**Reconciliation note — FIND-NOTIF-01 (informational):** the dispatch path is a
service flow (event listener + API-NOTIF-001), correctly SCR-less. It is documented
below as FLOW-NOTIF-00 for navigation context only, traced to API-NOTIF-001, with
SCR-ID = "— (service dispatch — no screen by SRS design)".

---

## MODULE NAVIGATION MAP

```
[App Shell]
  └─ Notifications (nav parent: الإشعارات)
       ├─ SCR-NOTIF-001  Templates          (NOTIF_TEMPLATES)
       ├─ SCR-NOTIF-002  Channel Config     (NOTIF_CHANNELS)
       └─ SCR-NOTIF-003  Notification Log   (NOTIF_LOG, read-only)

Service (no screen): dispatch/fan-out via CU event or API-NOTIF-001 → one log row
per requested channel (RULE-NOTIF-001).
```

---

## FLOWS

```
FLOW-NOTIF-00 — Dispatch / Fan-out (service — no screen)
  Screens involved : — (backend service; outcome visible on SCR-NOTIF-003)
  Sequence         : sending module emits NotificationEvent (CU) OR calls
                     API-NOTIF-001 with channelHint (single | list | ALL)
                     → NOTIF resolves template → fans out one NotificationLog row
                       per requested channel (RULE-NOTIF-001)
                     → per channel: enabled? send (retry ≤5, RULE-NOTIF-002) → SENT
                       / FAILED; disabled → CHANNEL_DISABLED (RULE-NOTIF-003);
                       inactive recipient → skip (RULE-NOTIF-007)
  Trigger          : A business event in a sending module
  Source US-ID(s)  : US-NOTIF-001, US-NOTIF-002
  Source SCR-ID(s) : — (service dispatch — no screen by SRS design)
                     Traceable counterpart: API-NOTIF-001 · RULE-NOTIF-001/002/003/007
  Priority         : —
  Status           : RECONCILED (via API-ID)
```

```
FLOW-NOTIF-01 — Templates
  Screens involved : SCR-NOTIF-001
  Sequence         : [Templates list + filters] → New / row-select
                     → [Side Drawer: create/edit incl. bilingual body + attachment
                       picked via File Service] → save → back to list
  Trigger          : Admin manages bilingual message templates
  Source US-ID(s)  : US-NOTIF-003
  Source SCR-ID(s) : SCR-NOTIF-001 (page_code NOTIF_TEMPLATES)
  Priority         : —
  Status           : RECONCILED
  Rules in flow    : save → RULE-NOTIF-004 (bilingual + File attachment), RULE-NOTIF-006
                     (unique templateCode)
```

```
FLOW-NOTIF-02 — Channel Configuration
  Screens involved : SCR-NOTIF-002
  Sequence         : [Channels list + filters (channelTypeId, isEnabledFl)] → row-select
                     → [Side Drawer: channelTypeId, isEnabledFl, configJson]
                     → save → back to list
  Trigger          : Operator enables/disables a channel or edits provider config
  Source US-ID(s)  : US-NOTIF-004
  Source SCR-ID(s) : SCR-NOTIF-002 (page_code NOTIF_CHANNELS)
  Priority         : —
  Status           : RECONCILED
  Rules in flow    : save → RULE-NOTIF-006 (unique channel/config) · disable →
                     RULE-NOTIF-003 (disabled channel logged, not retried at dispatch)
  Note             : the concrete provider behind configJson is a P3 decision
                     (OQ-NOTIF-001 resolved — provider-independent); the screen edits
                     the config payload, not the provider choice.
```

```
FLOW-NOTIF-03 — Notification Log (read-only)
  Screens involved : SCR-NOTIF-003
  Sequence         : [Log list + filters (recipient, moduleCode, channel, status,
                     referenceType, sentAt range)] → row-select
                     → [Side Drawer: read-only full record incl. errorMessage,
                       retryCount] (no edit)
  Trigger          : Operator tracks delivery status / troubleshoots failures
  Source US-ID(s)  : US-NOTIF-005
  Source SCR-ID(s) : SCR-NOTIF-003 (page_code NOTIF_LOG)
  Priority         : —
  Status           : RECONCILED
  Rules in flow    : system-generated log — screen is VIEW-only (SRS B4: CREATE/
                     UPDATE/DELETE not granted)
```

---

## CROSS-MODULE (UXD) NOTES

```
UXD-NOTIF-001 — Recipient identity display
  Displaying screen : SCR-NOTIF-003 (Notification Log, owned by NOTIF)
  Foreign data      : recipient name/identity — authoritative source = SEC UserAccount
                      API (SOFT-READ); recipientId is a SOFT reference, not a governed FK
  Rationale         : the log shows recipientId; a human-readable recipient requires
                      SEC's real API.

UXD-NOTIF-002 — Template attachment (file) reference
  Displaying screen : SCR-NOTIF-001 (Templates, owned by NOTIF)
  Foreign data      : attachmentFileId metadata/name — authoritative source = FILE
                      Service API (attachment picked/displayed via File Service)
  Rationale         : templates reference an optional attachment by file_id; showing/
                      selecting it uses the FILE provider API, not a local table.

Both lifecycle: assigned here (P2.5) → referenced by P3.2 → confirmed by P4.2
(a real SEC / FILE API must satisfy each before FE implementation clears).
```

---

*End of flow-diagram-NOTIF.md — RECONCILED, Gate PASSED, pending human approval.*
*No field, permission, or screen introduced beyond srs-NOTIF.md B1–B5 (CONTRACT-11).*
