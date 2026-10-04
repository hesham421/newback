# /SEC/execute-backend

Execute the current phase for SEC — with context safety check.

## Usage
/SEC/execute-backend [PHASE]

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
PHASE ASSESSMENT — SEC / [PHASE]
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
  reference but don't repeat. (DATA-DOM and SVC-API each have one:
  `DATA-DOM-HEADER.md`, `SVC-API-HEADER.md`. CORE, DOC, INT-C, INT-R,
  SEC-BE, ALIGN-BE have no HEADER — each is a single undivided sub.)
- `packages/backend-execution/_SECTIONS.md` if present — plan-level content
  that lives OUTSIDE every phase (Plan Index, DB Alignment Manifest, Error
  Catalog, Agent Handoff Summary). Read once for orientation; it is context,
  not a sub.

### Per sub:
1. Read `packages/backend-execution/[PHASE]/[SUB].md` completely
   (the SUB file is named by its phase-qualified label, e.g. `SVC-API-CRUD.md`)
2. Identify all tasks
3. Match each task to the applicable skill(s) in `.claude/skills/`
   (`build-*` to generate, `gov-*` to validate — skills self-declare what
   they apply to; consult the ones whose scope matches the task)
4. Read those skills from `.claude/skills/<skill>/SKILL.md` before writing
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

## Weight Map — SEC

| Phase | Sub | Weight | Notes |
|---|---|---|---|
| CORE | CORE | LIGHT | Cross-cutting conventions only (error signalling, transaction defaults, search contract, audit fields, type mapping) — no entity/API artifact of its own. |
| DATA-DOM | DATA-DOM-LOOKUP | LIGHT | Explicitly not applicable — SEC owns no standalone `kind: lookup` entity this version (value sets are CHECK-constrained columns on other entities per ADR-SEC-001); stub sub, 0 tasks. |
| DATA-DOM | DATA-DOM-MASTER | MEDIUM | 5 entities (ENT-SEC-001 User, 002 Role, 004 ModuleRegistry, 005 ScreenRegistry, 006 ActionRegistry) — entity + repository layers. |
| DATA-DOM | DATA-DOM-TRANSACTIONAL | HEAVY | 8 entities (ENT-SEC-003 UserRoleAssignment, 007 RoleModuleGrant, 008 RoleScreenGrant, 009 RoleActionGrant, 010 ActiveSession, 011 AuditLogEntry, 012 PasswordResetToken, 013 SignupRequest) — entity + repository + domain rules (RULE-SEC-003, RULE-SEC-005). |
| SVC-API | SVC-API-CRUD | HEAVY | 12 APIs (API-SEC-006, 007, 008, 009, 010, 011, 013, 014, 015, 016, 017, 026) — controller + service, single-entity and role/grant mutations. |
| SVC-API | SVC-API-INT | MEDIUM | 8 APIs (API-SEC-001, 002, 003, 004, 018, 019, 020, 024) — auth flows, onboarding registration, registry self-registration, audit export. |
| SVC-API | SVC-API-SEARCH | MEDIUM | 7 APIs (API-SEC-005, 012, 021, 022, 023, 025, 027) — read-only search/list/dashboard/menu endpoints. |
| DOC | DOC | LIGHT | Internal API contract summary table (27 APIs) — documentation/self-check only, no code. |
| INT-C | INT-C | LIGHT | No `XM-*` rows — SEC is ROOT, consumes no other module. Near-empty stub. |
| INT-R | INT-R | LIGHT | No `XM-*` rows to resolve — same basis as INT-C. Near-empty stub (inbound-consumer note only). |
| SEC-BE | SEC-BE | MEDIUM | Permission-matrix mapping across 9 screens + seed-data spec (9 page rows + per-screen action rows) + gateway rule (RULE-SEC-007) — cross-cutting security config, single layer. |
| ALIGN-BE | ALIGN-BE | LIGHT | Pointer to the plan's own Alignment self-check section — no code. |

## Phase Map — SEC

```
CORE → DATA-DOM → SVC-API → DOC → INT-C → INT-R → SEC-BE → ALIGN-BE
```

| Phase | Subs (filesystem order) |
|---|---|
| CORE | CORE |
| DATA-DOM | DATA-DOM-LOOKUP, DATA-DOM-MASTER, DATA-DOM-TRANSACTIONAL |
| SVC-API | SVC-API-CRUD, SVC-API-INT, SVC-API-SEARCH |
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
