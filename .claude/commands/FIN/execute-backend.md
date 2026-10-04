# /FIN/execute-backend

Execute the current phase for FIN — with context safety check.

## Usage
/FIN/execute-backend [PHASE]

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
PHASE ASSESSMENT — FIN / [PHASE]
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
  `governance/shared/backend/modules/FIN/packages/backend-execution/[PHASE]/` if present —
  phase-level strategy, tables, and intro that the SUB files reference but
  don't repeat. Only `DATA-DOM` (`DATA-DOM-HEADER.md`) and `SVC-API`
  (`SVC-API-HEADER.md`) have one; CORE, DOC, INT-C, INT-R, SEC-BE, ALIGN-BE
  have no HEADER — each is a single undivided sub.
- `governance/shared/backend/modules/FIN/packages/backend-execution/_SECTIONS.md` if
  present — plan-level content that lives OUTSIDE every phase (Plan Index,
  DB Alignment Manifest, Error Catalog, Agent Handoff Summary). Read once for
  orientation; it is context, not a sub.

### Per sub:
1. Read `governance/shared/backend/modules/FIN/packages/backend-execution/[PHASE]/[SUB].md`
   completely (the SUB file is named by its phase-qualified label, e.g.
   `SVC-API-CRUD.md`)
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

## Weight Map — FIN

| Phase | Sub | Weight | Notes |
|---|---|---|---|
| CORE | CORE | LIGHT | Cross-cutting conventions only (error signalling `FIN-{http}[-{SLUG}]`, transaction scope incl. the single-transaction build→validate→post rule, search contract, audit fields incl. the dual `createdBy`/`closedBy` SoD-relevant principals, type mapping, lookup-field handling, docNo numbering-engine rule, workflow-engine-forbidden note) — no entity/API artifact of its own. |
| DATA-DOM | DATA-DOM-LOOKUP | HEAVY | 8 entities (ENT-FIN-002 Dimension, 003 DimensionValue, 009 EventTypeRule, 010 RuleLine, 011 RecurringTemplate, plus RecurringTemplateLine, AllocationRule, AllocationTarget) — entity + domain rules (RULE-FIN-002, -003) + repository layers across all 8. |
| DATA-DOM | DATA-DOM-MASTER | MEDIUM | 3 entities (ENT-FIN-001 Account, 007 FiscalYear, 008 FiscalPeriod) — entity + repository layers, one domain rule (RULE-FIN-001, leaf/parent invariant on Account). |
| DATA-DOM | DATA-DOM-TRANSACTIONAL | HEAVY | 3 entities (ENT-FIN-004 JournalEntry, 005 JournalLine, JournalLineDimension) but JournalEntry alone carries 7 domain rules (RULE-FIN-004/005/006/008/011/012/013/016) and ~15 repository ops (QR-FIN-023..037, the full posting + reversal pipeline) — multi-layer, clearly >10 tasks. |
| SVC-API | SVC-API-CRUD | HEAVY | 10 APIs (API-FIN-002, 003, 004, 006, 007, 010, 011, 012, 013, 015, 016) — controller + service + mapper + DTO per API, single-entity CRUD across Account/Dimension/EventTypeRule/RecurringTemplate/AllocationRule. |
| SVC-API | SVC-API-INT | HEAVY | 9 APIs (API-FIN-014, 017, 018, 019, 020, 021, 023, 024, 025, 026, 027 — the posting pipeline, event-build, template run, allocation run, reversal, and period-control actions) — the module's core business complexity: multi-rule validation chains, single-transaction build-through-post, cross-module reads (XM-FIN-001), SoD-checked period close. Chunk one or two APIs per pass. |
| SVC-API | SVC-API-SEARCH | HEAVY | 13 APIs (API-FIN-001, 005, 008, 009, and the remaining read-only search/report endpoints incl. ledger/trial-balance/balance-sheet/income-statement/dimension reports) — read-only, thinner per-API (service + controller only) than CRUD/INT, but the count exceeds the MEDIUM band; chunk in a few batches of similar entities. |
| DOC | DOC | LIGHT | Internal API contract summary table (32 endpoints) + DTO typing constraints + pagination/filter standard — documentation/self-check only, no code. |
| INT-C | INT-C | LIGHT | One `XM-*` row (XM-FIN-001, validate/read lookup-backed codes against MDL), below the split threshold (1 < 5) — no SUB opened. |
| INT-R | INT-R | LIGHT | XM-FIN-001 resolves ACTIVE (MDL v1 already gated) — no DEFERRED workaround needed; note only. |
| SEC-BE | SEC-BE | HEAVY | 12 screens + ~27 action rows (permission matrix across FIN_ACCOUNTS/DIMENSIONS/RULES/RECURRING_TEMPLATES/ALLOCATION_RULES/JOURNAL_ENTRIES/PERIODS/ledger+report screens) + SoD enforcement (RULE-FIN-015) — cross-cutting security config, single layer, but ~39 registration tasks exceeds the MEDIUM band; chunk by screen group. |
| ALIGN-BE | ALIGN-BE | LIGHT | Pointer to the plan's own Alignment self-check section — no code. |

## Phase Map — FIN

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
