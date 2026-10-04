# /MDL/execute-backend

Execute the current phase for MDL — with context safety check.

## Usage
/MDL/execute-backend [PHASE]

---

## STEP 0 — Context Safety Assessment (MANDATORY)

### 0.1 — Read state, identify PENDING subs in the requested phase
Read `governance/shared/backend/modules/MDL/execution-state.json`. Confirm the requested
PHASE matches `current_phase` (or is otherwise the next PENDING phase in
order). Identify all subs under it with `status: PENDING`.

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
PHASE ASSESSMENT — MDL / [PHASE]
══════════════════════════════════════════════════════
Subs pending : [list, weight + task count each]
Plan         : [one pass / chunked — list chunks]
══════════════════════════════════════════════════════
Proceed? [waits for confirmation]
```

---

## STEP 1 — Execution (after confirmation)

### 1.0 — Read shared context once (before the per-sub loop)
- The phase's `[PHASE]-HEADER.md` under
  `governance/shared/backend/modules/MDL/packages/backend-execution/[PHASE]/` if present —
  only `SVC-API` has one (`SVC-API-HEADER.md`); no other phase in this module
  has a HEADER file.
- `governance/shared/backend/modules/MDL/packages/backend-execution/_SECTIONS.md` — plan-level
  content that lives OUTSIDE every phase (Plan Index, DB Alignment Manifest,
  Error Catalog, Agent Handoff Summary). Read once for orientation; it is
  context, not a sub.

### Per sub:
1. Read `governance/shared/backend/modules/MDL/packages/backend-execution/[PHASE]/[SUB].md`
   completely (the SUB file is named by its phase-qualified label, e.g.
   `SVC-API-CRUD.md`; for a single-file phase the SUB file is `[PHASE].md`,
   e.g. `CORE/CORE.md`, `DATA-DOM/DATA-DOM.md`)
2. Identify all tasks
3. Match each task to the applicable skill(s) in `.claude/skills/`
   (`build-*` to generate, `gov-*` to validate — skills self-declare what
   they apply to; consult the ones whose scope matches the task)
4. Read those skills from `.claude/skills/<skill>/SKILL.md` before writing
5. Execute all tasks in order
6. Run the phase's validation skill (`gov-validate-backend-feature`) after
   the last task
7. Mark sub COMPLETE in `governance/shared/backend/modules/MDL/execution-state.json`

### Blocked items — OQ
OQ-blocked task → skip, add to `blocked[]`, mark in code:
`// TODO: OQ-[ID] — pending resolution`. Continue remaining tasks.

### XM DEFERRED
MDL declares one cross-module dependency, XM-MDL-001 (SOFT-READ → SEC's
ModuleRegistry). Per `INT-R/INT-R.md`, SEC is already gated (pass-1 APPROVE)
so XM-MDL-001 is ACTIVE from the moment MDL v1 is created — never DEFERRED.
No mock strategy is needed for this module as currently scanned; if a future
scan finds a DEFERRED XM, mark in code:
`// TODO: XM-MDL-[N] DEFERRED — replace when READY`.

---

## STEP 2 — Session Report

Print phase/sub completed, tasks executed, blocked items, any
api_doc_gaps entries added.

---

## Weight Map — MDL

| Phase | Sub | Weight | Tasks |
|---|---|---|---|
| CORE | CORE | LIGHT | 1 — apply platform conventions (layers, error-code format `MDL-{http}[-{SLUG}]`, transaction scope, search contract, audit fields, type mapping incl. the `sort_order` → `Integer` deviation, XM call placement, shared CORE authorization interceptor) |
| DATA-DOM | DATA-DOM | HEAVY | ~21 — 2 entities (ENT-MDL-001 LookupType, ENT-MDL-002 LookupValue), 4 domain rules (RULE-MDL-001, -002, -003, -004), ~15 repository query ops (QR-MDL-001..015); multi-layer (Entity + Domain + Repository) |
| SVC-API | SVC-API-CRUD | HEAVY | 7 APIs (API-MDL-002, -003, -004, -006, -007, -008, -009); multi-layer (DTO + Mapper + Service + Controller) per API |
| SVC-API | SVC-API-SEARCH | MEDIUM | 4 APIs (API-MDL-001, -005, -010, -011), read-only; multi-layer (Service + Controller + response DTO/Mapper) but thinner than CRUD |
| DOC | DOC | LIGHT | 1 — review/reconcile the API contract self-check table (11 endpoints, all `(proposed)` types) against what CORE/DATA-DOM/SVC-API actually produced |
| INT-C | INT-C | LIGHT | 1 — implement the XM-MDL-001 outbound client call (`SecModuleRegistryClient` or equivalent) against SEC's `GET /api/v1/sec/registry` |
| INT-R | INT-R | LIGHT | 1 — confirm XM-MDL-001 resolves ACTIVE (no DEFERRED workaround needed); note the inbound stub for future consumers (`XM-INBOUND-STUB-2`, formal id assigned by the first real consumer's own P2) |
| SEC-BE | SEC-BE | MEDIUM | 6 — register 2 screens (MDL_LOOKUPS, MDL_TYPE_REGISTRY) + register actions (MDL_LOOKUPS: VIEW/CREATE/UPDATE = 3; MDL_TYPE_REGISTRY: VIEW = 1) via SEC's screen/action-registration endpoints |
| ALIGN-BE | ALIGN-BE | LIGHT | 1 — run the Alignment self-check (see `_SECTIONS.md` / `ALIGN-BE.md`) |

---

## Phase Map — MDL

```
CORE       → CORE
DATA-DOM   → DATA-DOM
SVC-API    → SVC-API-CRUD, SVC-API-SEARCH   (SVC-API-INT omitted — no MDL endpoint is
                                              "INT"-shaped per SVC-API-HEADER.md)
DOC        → DOC
INT-C      → INT-C
INT-R      → INT-R
SEC-BE     → SEC-BE
ALIGN-BE   → ALIGN-BE
```

---

## Constraints (NON-NEGOTIABLE)

- NEVER skip STEP 0
- NEVER execute without confirmation after assessment
- NEVER invent field/column/route names — always look up db-script.md
- NEVER implement a blocked OQ item — mark and skip only
- NEVER advance phase without explicit instruction
- ALWAYS update execution-state.json after every sub
