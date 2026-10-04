# /FIN/v2/execute-backend

Execute the current **v2** phase for FIN — with context safety check.

> **Version 2.** FIN is on the IFA increment path. Every path below is the v2 path;
> the frozen v1 artifacts and the v1 commands under `.claude/commands/FIN/` are
> history and are never written to from here.
>
> | | Path | This repo may |
> |---|---|---|
> | Analysis (P0…P3_1, manifest) | `governance/shared/analysis/modules/FIN/v2/` | read only |
> | Delivered packages | `governance/shared/backend/modules/FIN/packages/v2/` | read only |
> | This repo's partition | `governance/shared/backend/modules/FIN/v2/` | read and write |
> | api-docs (NOT version-suffixed) | `governance/shared/backend/modules/FIN/api-docs/` | read and write |

## Usage
/FIN/v2/execute-backend [PHASE]

---

## STEP 0 — Context Safety Assessment (MANDATORY)

### 0.1 — Read state, identify PENDING subs in the requested phase
Read `governance/shared/backend/modules/FIN/v2/execution-state.json`.
Confirm the requested phase matches `current_phase`.

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
PHASE ASSESSMENT — FIN v2 / [PHASE]
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
  `governance/shared/backend/modules/FIN/packages/v2/backend-execution/[PHASE]/`
  if present — phase-level strategy, tables, and intro that the SUB files
  reference but don't repeat. **Present for DATA-DOM and SVC-API only**
  (`DATA-DOM-HEADER.md`, `SVC-API-HEADER.md`).
- `governance/shared/backend/modules/FIN/packages/v2/backend-execution/_SECTIONS.md`
  — plan-level content that lives OUTSIDE every phase (Plan Index, DB Alignment
  Manifest, Error Catalog, Agent Handoff Summary). Read once for orientation;
  it is context, not a sub.

### Per sub:
1. Read
   `governance/shared/backend/modules/FIN/packages/v2/backend-execution/[PHASE]/[SUB].md`
   completely (the SUB file is named by its phase-qualified label, e.g.
   `SVC-API-CRUD.md`; a single-sub phase's file is named for the phase itself,
   e.g. `CORE/CORE.md`)
2. Identify all tasks — v2 plan files mark them with HTML markers:
   `<!-- API:API-FIN-nnn:START -->` in the SVC-API subs, `#### ENT-FIN-nnn`
   blocks in the DATA-DOM subs, `<!-- XM:…:START -->` in INT-C. A block tagged
   `(ADDED v2)` / `(MODIFIED v2)` is the delta; an untagged block is v1 as built
   and is restated for context — do NOT rebuild it unless the sub says so.
3. Match each task to the applicable skill(s) in `.claude/skills/`
   (`build-*` to generate, `gov-*` to validate — skills self-declare what
   they apply to; consult the ones whose scope matches the task)
4. Read those skills from `.claude/skills/<skill>/SKILL.md` before writing
5. Execute all tasks in order
6. Run `gov-validate-backend-feature` after the last task
7. Mark sub COMPLETE in
   `governance/shared/backend/modules/FIN/v2/execution-state.json`

### Blocked items — OQ
OQ-blocked task → skip, add to `blocked[]`, mark in code:
`// TODO: OQ-[ID] — pending resolution`. Continue remaining tasks.

### Deferred cross-module — XM
XM DEFERRED → implement the mock strategy the sub names, add to
`deferred_xm[]`, mark in code:
`// TODO: XM-FIN-[N] DEFERRED — replace when READY`.
For v2 as delivered, INT-R records **no DEFERRED row**: XM-FIN-001 is ACTIVE and
XM-FIN-002 was RETIRED 2026-09-12. Do not resurrect XM-FIN-002.

---

## STEP 2 — Session Report

Print phase/sub completed, tasks executed, blocked items, any
`api_doc_gaps[]` entries added.

---

## Weight Map — FIN v2

| Phase | Sub | Weight | Tasks | Layers touched |
|---|---|---|---|---|
| CORE | CORE | MEDIUM | 8 | cross-cutting (advisors, error catalog, conventions) |
| DATA-DOM | DATA-DOM-LOOKUP | HEAVY | 10 entities | entity + domain + repository |
| DATA-DOM | DATA-DOM-MASTER | MEDIUM | 3 entities | entity + domain + repository |
| DATA-DOM | DATA-DOM-TRANSACTIONAL | MEDIUM | 3 entities | entity + domain + repository |
| SVC-API | SVC-API-CRUD | HEAVY | 17 APIs | dto + mapper + service + controller |
| SVC-API | SVC-API-INT | MEDIUM | 9 APIs | dto + mapper + service + controller |
| SVC-API | SVC-API-SEARCH | HEAVY | 15 APIs | dto + mapper + service + controller |
| DOC | DOC | MEDIUM | 6 | contract self-check, no code layer |
| INT-C | INT-C | LIGHT | 1 XM block | service (cross-module consume) |
| INT-R | INT-R | LIGHT | 0 (status table only) | none |
| SEC-BE | SEC-BE | HEAVY | 13 + bootstrap seed | security annotations + SEC_* seed data |
| ALIGN-BE | ALIGN-BE | LIGHT | 3 coverage checks | verification only |

Boundary note: DATA-DOM-MASTER and DATA-DOM-TRANSACTIONAL have few tasks but touch
three layers each, so they are MEDIUM rather than LIGHT. DATA-DOM-LOOKUP sits on the
MEDIUM/HEAVY line at exactly 10 tasks and is recorded HEAVY because of the same
three-layer reach — chunk it.

Heavy phases (require chunking): **DATA-DOM, SVC-API, SEC-BE**.
SVC-API-CRUD (17) and SVC-API-SEARCH (15) each warrant one full pass alone.

## Phase Map — FIN v2

Order is the profile's `tracks.backend.plans.exec.phases`, not the filesystem's.

```
1. CORE        → CORE
2. DATA-DOM    → DATA-DOM-LOOKUP, DATA-DOM-MASTER, DATA-DOM-TRANSACTIONAL
3. SVC-API     → SVC-API-CRUD, SVC-API-INT, SVC-API-SEARCH
4. DOC         → DOC
5. INT-C       → INT-C
6. INT-R       → INT-R
7. SEC-BE      → SEC-BE
8. ALIGN-BE    → ALIGN-BE
```

---

## Constraints (NON-NEGOTIABLE)

- NEVER skip STEP 0
- NEVER execute without confirmation after assessment
- NEVER invent field/column/route names — always look up
  `governance/shared/analysis/modules/FIN/v2/P2/db-script-fin.md`
  (the **v2** db-script, not v1's)
- NEVER write to `governance/shared/analysis/modules/FIN/**` or
  `governance/shared/backend/modules/FIN/packages/**` — both are read-only here
- NEVER implement a blocked OQ item — mark and skip only
- NEVER advance phase without explicit instruction
- ALWAYS update `governance/shared/backend/modules/FIN/v2/execution-state.json`
  after every sub
- When the v2 plan is wrong, record it in that file's `api_doc_gaps[]` with a
  `resolution` starting OPEN / DEFERRED / PENDING / RESOLVED / CLOSED /
  IMPLEMENTED / HUMAN / ADR — never patch the plan itself
