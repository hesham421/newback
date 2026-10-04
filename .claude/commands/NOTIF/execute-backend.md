# /NOTIF/execute-backend

Execute the current phase for NOTIF — with context safety check.

## Usage
/NOTIF/execute-backend [PHASE]

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
PHASE ASSESSMENT — NOTIF / [PHASE]
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
  reference but don't repeat. (NOTIF has no HEADER files — no phase was split.)
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
(NOTIF has no open OQ items — OQ-NOTIF-001 was RESOLVED at plan time, provider
choice delegated to configJson/implementation, not blocking any task.)

---

## STEP 2 — Session Report

Print phase/sub completed, tasks executed, blocked items, any
api_doc_gaps entries added.

---

## Weight Map — NOTIF

| Phase | Sub | Weight | Notes |
|---|---|---|---|
| CORE | CORE | LIGHT | Architecture/policy context only — no code artifact of its own; sets canonical layering (controller/service/mapper/domain/repository/entity/dto/exception/config), AuditableEntity base, LocalizedException error signaling, and the provider-agnostic ChannelProvider constraint. |
| DATA-DOM | DATA-DOM | MEDIUM | 3 entities (ENTITY-NOTIF-001 NotificationLog, -002 NotificationTemplate, -003 NotificationChannelConfig; 25 fields total), below the 5-entity SUB-split threshold, but above LIGHT: 2 layers (entity + domain), a NOTIF_LOG state machine (PENDING→SENT/FAILED/CHANNEL_DISABLED), and 7 domain rules (RULE-NOTIF-001..007) split across 3 domain companion classes — roughly 6-8 tasks (3 entity classes + domain rule enforcement per entity). |
| SVC-API | SVC-API | XL | Full 6-API feature (API-NOTIF-001..006: dispatch fan-out, log query, log-by-id, template CRUD, channel CRUD, lookups) in one undivided sub — repository + DTO set + mapper + service + controller across all 3 entities, plus the dedicated domain classes (DispatchDomainService, RetryPolicy, ChannelProvider strategy) called out in CORE — below the 8-API SUB-split threshold per the file's own note ("6 APIs < 8 → no SUB split"), but clearly a full-feature, multi-layer, >10-task sub. |
| DOC | DOC | LIGHT | Internal contract-stabilization summary only — no code (API-NOTIF-001..006 STABLE, LOV/DTO type notes). |
| INT-C | INT-C | LIGHT | 2 outbound XM SOFT-READ contracts to declare/verify (XM-NOTIF-001 → SEC UserAccount, XM-NOTIF-002 → FILE attachment) — both already CONTRACTED, gate-check only, no code. |
| INT-R | INT-R | LIGHT | Runtime-readiness gate for the same 2 XM dependencies — both already READY ✓ (SEC and FILE built before NOTIF), no code. |
| SEC-BE | SEC-BE | LIGHT | 3 screens (SCR-NOTIF-001 Templates, SCR-NOTIF-002 Channel Config, SCR-NOTIF-003 Notification Log VIEW-only) with CORE-9 auto-generated permissions under a single NOTIF_ADMIN role — declarative security-seed/permission verification, no dedicated code layer beyond the @PreAuthorize annotations already produced in SVC-API. |
| ALIGN-BE | ALIGN-BE | LIGHT | Traceability/consistency gate check only — no code (already PASSED ✓ at plan time with 0 gaps). |

## Phase Map — NOTIF

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
