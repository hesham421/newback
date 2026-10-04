# /FILE/execute-backend

Execute the current phase for FILE — with context safety check.

## Usage
/FILE/execute-backend [PHASE]

---

## STEP 0 — Context Safety Assessment (MANDATORY)

### 0.1 — Read state, identify PENDING subs in the requested phase
### 0.2 — Look up each sub's weight from the Weight Map below
### 0.3 — Classify and decide chunking

| Total weight in phase | Action |
|---|---|
| All LIGHT/MEDIUM | Execute the whole phase in one pass |
| Any HEAVY present | Chunk — one sub (or a few LIGHT subs) per pass |
| Any XL present | That sub alone is one full pass |

### 0.4 — Print assessment, wait for confirmation
```
══════════════════════════════════════════════════════
PHASE ASSESSMENT — FILE / [PHASE]
══════════════════════════════════════════════════════
Subs pending : [list, weight + task count each]
Plan         : [one pass / chunked — list chunks]
══════════════════════════════════════════════════════
Proceed? [waits for confirmation]
```

---

## STEP 1 — Execution (after confirmation)

### 1.0 — Read shared context once (before the per-sub loop)
- The phase's `[PHASE]-HEADER.md` under `packages/backend-execution/[PHASE]/`
  if present — phase-level strategy, tables, and intro that the SUB files
  reference but don't repeat. FILE has ONE HEADER file: `SVC-API/SVC-API-HEADER.md`
  (phase-level registry table + intro shared by SVC-API-CATEGORIES,
  SVC-API-FILES, and SVC-API-LOOKUP — read once before looping over those
  three subs; no other phase in FILE has a HEADER file).
- `packages/backend-execution/_SECTIONS.md` if present — plan-level content
  that lives OUTSIDE every phase (Plan Index, DB Alignment Manifest, Error
  Catalog, Query Reference Catalog, Derivation Log, TC Coverage Matrix
  Summary, Agent Handoff Summary). Read once for orientation; it is context,
  not a sub. FILE's `_SECTIONS.md` carries the full Error Catalog
  (ERR-0001..0006) and Query Reference Catalog (QR-FILE-0001..0011) that
  every SVC-API sub references by ID rather than repeating.

### Per sub:
1. Read `packages/backend-execution/[PHASE]/[SUB].md` completely
   (the SUB file is named by its phase-qualified label, e.g. `SVC-API-CATEGORIES.md`)
2. Identify all tasks
3. Map each task to the skill routing table in `GOVERNANCE-RULES.md`
4. Read required skills from `.claude/skills/` (`build-*` to generate, `gov-*` to validate)
5. Execute all tasks in order
6. Run the phase's validation skill after the last task
7. Mark sub COMPLETE in `execution-state.json`

### Blocked items — OQ
OQ-blocked task → skip, add to `blocked[]`, mark in code:
`// TODO: OQ-[ID] — pending resolution`. Continue remaining tasks.
(FILE's Plan Header records "Open Questions: None" — no OQ-blocked items are
expected at generation time; this section applies only if one surfaces during
execution.)

---

## STEP 2 — Session Report

Print phase/sub completed, tasks executed, blocked items, any
api_doc_gaps entries added.

---

## Weight Map — FILE

| Phase | Sub | Weight | Notes |
|---|---|---|---|
| CORE | CORE | LIGHT | Architecture/policy context only — no code artifact of its own. Establishes canonical layers, AuditableEntity base, LocalizedException/error-catalog convention, auth-delegation-to-Security rule (RULE-FILE-004), and the separate AES/GCM access-token model (RULE-FILE-003) that later phases build against. |
| DATA-DOM | DATA-DOM | MEDIUM | 2 entities (ENTITY-FILE-001 FileDocument — 11 fields incl. BYTEA content; ENTITY-FILE-002 FileCategory — 7 fields), below the 5-entity SUB-split threshold. Domain rules (RULE-FILE-001..007) are placed in dedicated domain/ classes per CORE policy — FileValidationDomainService (size/MIME, RULE-FILE-001/002) and FileAccessTokenDomainService (AES/GCM single-use token, RULE-FILE-003) — so this sub spans 2 layers (entity + domain), ~7-9 tasks. |
| SVC-API | SVC-API-CATEGORIES | XL | Full CRUD+search feature (API-FILE-007: POST/GET/PUT/DELETE + search) for FileCategory in one undivided sub — repository (incl. EXISTS categoryCode QR-FILE-0011) + DTO set (Create/Update/Response/Search) + mapper + service + controller, all layers, one complete vertical slice. |
| SVC-API | SVC-API-FILES | XL | The module's core feature: 6 APIs (API-FILE-001..006) — multipart upload with ownership/size/MIME validation orchestration (RULE-FILE-001/002/005), AES/GCM access-token issuing (RULE-FILE-003), token-gated binary streaming download (@Lob, never eager-loaded), owner-scoped metadata + paged list (bytes excluded, DRV-003), and archive/soft-delete (RULE-FILE-006). Spans repository + DTO + mapper + service + controller + the 2 domain services from DATA-DOM — highest complexity and task count in the module; execute alone. |
| SVC-API | SVC-API-LOOKUP | LIGHT | Single read-only endpoint (API-FILE-008) resolving 2 fixed runtime code lists (LOV-FILE-001/002) — no persistence layer, no lookup table, controller + trivial service only. |
| DOC | DOC | LIGHT | Internal contract-stabilization summary only (API-FILE-001..008 marked STABLE) — no code. |
| INT-C | INT-C | LIGHT | Single outbound XM (XM-FILE-001 SOFT-READ → SEC UserAccount, app-layer identity read via the Security auth filter — no physical FK) already CONTRACTED — declaration/gate check only, no code. |
| INT-R | INT-R | LIGHT | Runtime activation status for XM-FILE-001 (READY ✓, SEC built before FILE) — gate check only, no code. |
| SEC-BE | SEC-BE | MEDIUM | 2 admin screens (SCR-FILE-001 File Categories, SCR-FILE-002 File Browser) × 4 CORE-9 permissions each, enforced across all 8 API-FILE endpoints (FILE_ADMIN role), plus RULE-FILE-005 owner-visibility enforced in the service layer using the authenticated principal from XM-FILE-001 — spans controller (@PreAuthorize placement) + service (ownership check) layers. |
| ALIGN-BE | ALIGN-BE | LIGHT | Traceability/consistency gate check only — no code. |

## Phase Map — FILE

```
CORE → DATA-DOM → SVC-API → DOC → INT-C → INT-R → SEC-BE → ALIGN-BE
```

| Phase | Subs (filesystem order) |
|---|---|
| CORE | CORE |
| DATA-DOM | DATA-DOM |
| SVC-API | SVC-API-CATEGORIES, SVC-API-FILES, SVC-API-LOOKUP |
| DOC | DOC |
| INT-C | INT-C |
| INT-R | INT-R |
| SEC-BE | SEC-BE |
| ALIGN-BE | ALIGN-BE |

---

## Constraints (NON-NEGOTIABLE)

- NEVER skip STEP 0
- NEVER execute without confirmation after assessment
- NEVER invent field/column/route names — always look up db-script.md
- NEVER implement a blocked OQ item — mark and skip only
- NEVER advance phase without explicit instruction
- ALWAYS update execution-state.json after every sub
