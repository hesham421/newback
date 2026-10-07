# flow-diagram-FILE.md
## File Service (FILE) — Navigation & Sequencing

```
Produced by     : UI/UX Design Engine (Project 2.5)
Governed by     : CONTRACT-11 (PRD↔SRS Reconciliation Gate)
Inputs          : prd-FILE.md (US-FILE-001..005) + srs-FILE.md v1.1 (PART B: B1–B5)
Mode            : RECONCILE (both PRD and SRS attached)
Status          : RECONCILED — Gate PASSED — pending human design approval (CONTRACT-12)
Date            : 2026-09-02
```

---

## RECONCILIATION GATE — RESULT

| US-ID | Intent (PRD) | SRS counterpart | Reconciled |
|---|---|---|---|
| US-FILE-001 | Upload & store a file securely | API-FILE-001 · RULE-FILE-001/002/005 · upload is contextual in owner module; SCR-FILE-002 optional CREATE | ✓ via API-ID (contextual) |
| US-FILE-002 | Access file via secure time-limited link | API-FILE-002/003 · RULE-FILE-003 · SCR-FILE-002 (download action) | ✓ SCR-FILE-002 |
| US-FILE-003 | Define file categories with types & size limits | SCR-FILE-001 · API-FILE-007 · RULE-FILE-007 | ✓ SCR-FILE-001 |
| US-FILE-004 | Generic ownership — attach files across modules | provider pattern · API-FILE-001/005 · SCR-FILE-002 (list by owner) | ✓ via API-ID + SCR-FILE-002 |
| US-FILE-005 | Archive / remove files (lifecycle) | SCR-FILE-002 · API-FILE-006 · RULE-FILE-006 (soft delete) | ✓ SCR-FILE-002 |

```
Reconciled, no rework needed : 5 / 5 user stories
Flagged for rework           : 0
Blocked (OQ)                 : 0
Contradictions (OQ)          : 0
```

**Grouping decision (this engine):** US-FILE-002, US-FILE-004 and US-FILE-005 all
resolve onto SCR-FILE-002 (the File Browser) — secure download, list-by-owner, and
archive/soft-delete are all actions on the same FileDocument list. US-FILE-001
(upload) is, per SRS PART B preamble, primarily **contextual inside the owning
module's screen** via the injected FileService provider; SCR-FILE-002 surfaces an
OPTIONAL create/upload affordance only.

**Reconciliation note — FIND-FILE-01 (informational):** file content upload is a
provider/service concern (FileService @Service injection). No standalone "upload
page" exists — upload happens where the owning record is edited. This is faithful
to SRS (API-FILE-001 + provider note), not an omission.

---

## MODULE NAVIGATION MAP

```
[App Shell]
  └─ File Service (nav parent: خدمة الملفات)
       ├─ SCR-FILE-001  File Categories  (FILE_CATEGORIES)
       └─ SCR-FILE-002  File Browser     (FILE_BROWSER)

Contextual (not a FILE screen): file upload/attach inside the OWNING module's
edit screen, via injected FileService provider (API-FILE-001).
```

---

## FLOWS

```
FLOW-FILE-01 — File Categories
  Screens involved : SCR-FILE-001
  Sequence         : [Categories list + filters] → New / row-select
                     → [Side Drawer: create/edit] → save → back to list
  Trigger          : Admin / consuming module defines document categories & limits
  Source US-ID(s)  : US-FILE-003
  Source SCR-ID(s) : SCR-FILE-001 (page_code FILE_CATEGORIES)
  Priority         : —
  Status           : RECONCILED
  Rules in flow    : save → RULE-FILE-007 (unique categoryCode); category limits feed
                     RULE-FILE-001 (size) and RULE-FILE-002 (accepted types) at upload
```

```
FLOW-FILE-02 — File Browser (list · download · archive · soft-delete)
  Screens involved : SCR-FILE-002
  Sequence         : [Files list + filters (fileName, moduleCode, ownerType/ownerId,
                     fileTypeId, fileStatusId)] → row-select
                     → [Side Drawer: read-only metadata + actions]
                        ├ Download → issue AES/GCM access token (API-FILE-002)
                        │           → download via token (API-FILE-003)
                        ├ Archive  → fileStatusId = ARCHIVED (API-FILE-006)
                        └ Delete   → soft-delete, fileStatusId = DELETED (RULE-FILE-006)
  Trigger          : Admin browses / manages stored files
  Source US-ID(s)  : US-FILE-002, US-FILE-004, US-FILE-005 (+ US-FILE-001 optional CREATE)
  Source SCR-ID(s) : SCR-FILE-002 (page_code FILE_BROWSER)
  Priority         : —
  Status           : RECONCILED
  Rules in flow    : download → RULE-FILE-003 (fresh single-use token, ~100m TTL),
                     RULE-FILE-004 (auth delegated to Security filter) · archive/delete
                     → RULE-FILE-006 · visibility bounded by ownership RULE-FILE-005
  Note             : fileContent (BYTEA) is never rendered as a field — it is fetched
                     only through the secure download link.
```

---

## CROSS-MODULE (UXD) NOTES

```
UXD-FILE-001 — createdBy / owner identity display
  Displaying screen : SCR-FILE-002 (File Browser, owned by FILE)
  Foreign data      : user identity (createdBy audit / owner display) — authoritative
                      source = SEC UserAccount API (SOFT-READ)
  Rationale         : FILE stores ownerId/ownerType/moduleCode as a polymorphic
                      app-layer reference (no governed FK); showing a human-readable
                      "created by" name requires SEC's real API.
  Lifecycle         : assigned here (P2.5) → referenced by P3.2 → confirmed by P4.2
                      (a real SEC API must satisfy it before FE implementation clears)
```

---

*End of flow-diagram-FILE.md — RECONCILED, Gate PASSED, pending human approval.*
*No field, permission, or screen introduced beyond srs-FILE.md B1–B5 (CONTRACT-11).*
