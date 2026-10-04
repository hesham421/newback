# /CU/execute-backend

Execute the current phase for CU — with context safety check.

## Usage
/CU/execute-backend [PHASE]

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
PHASE ASSESSMENT — CU / [PHASE]
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
  reference but don't repeat. (CU has no HEADER files — no phase was split.)
- `packages/backend-execution/_SECTIONS.md` if present — plan-level content
  that lives OUTSIDE every phase (Plan Index, DB Alignment Manifest, Error
  Catalog, Agent Handoff Summary). Read once for orientation; it is context,
  not a sub.

### Per sub:
1. Read `packages/backend-execution/[PHASE]/[SUB].md` completely
   (the SUB file is named by its phase-qualified label, e.g. `SVC-API-CRUD.md`)
2. Identify all tasks
3. Map each task to the skill routing table in `GOVERNANCE-RULES.md`
4. Read required skills from `.claude/skills/` (`build-*` to generate, `gov-*` to validate)
5. Execute all tasks in order
6. Run the phase's validation skill after the last task
7. Mark sub COMPLETE in `execution-state.json`

### Blocked items — OQ
OQ-blocked task → skip, add to `blocked[]`, mark in code:
`// TODO: OQ-[ID] — pending resolution`. Continue remaining tasks.

---

## STEP 2 — Session Report

Print phase/sub completed, tasks executed, blocked items, any
api_doc_gaps entries added.

---

## Weight Map — CU

| Phase | Sub | Weight | Notes |
|---|---|---|---|
| CORE | CORE | LIGHT | Architecture/policy context only — no code artifact of its own; CU is the ROOT cross-cutting library, single simple entity. |
| DATA-DOM | DATA-DOM | LIGHT | 1 entity (ENTITY-CU-001 AppConfiguration, 5 fields), single layer (entity), below the 5-entity SUB-split threshold. |
| SVC-API | SVC-API | XL | Full CRUD+search feature (5 APIs: API-CU-001..005) in one undivided sub — repository + DTO set + mapper + service + controller, all layers, below the 8-API SUB-split threshold. |
| DOC | DOC | LIGHT | Internal contract-stabilization summary only — no code. |
| INT-C | INT-C | LIGHT | No outbound XM dependencies (CU is the ROOT library) — no-op gate. |
| INT-R | INT-R | LIGHT | No runtime cross-module dependencies — no-op gate. |
| SEC-BE | SEC-BE | LIGHT | Single API-level authorization rule (config-admin authority) across the 5 endpoints — no screens/SCR-IDs (backend-only). |
| ALIGN-BE | ALIGN-BE | LIGHT | Traceability/consistency gate check only — no code. |

## Phase Map — CU

```
CORE → DATA-DOM → SVC-API → DOC → INT-C → INT-R → SEC-BE → ALIGN-BE
```

| Phase | Subs (filesystem order) |
|---|---|
| CORE | CORE |
| DATA-DOM | DATA-DOM |
| SVC-API | SVC-API |
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
