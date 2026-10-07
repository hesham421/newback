# ADR-SEC-036 — The v2 backend plan is restated in full, and its v1 blocks are brought to the current P3.1 template
Status      : ACCEPTED (non-breaking)
Stage       : P3.1      Module: SEC        Version: v2
Lane        : analysis · round 1 · claude:opus
traces      : ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009, ENT-SEC-010, ENT-SEC-011, ENT-SEC-012, ENT-SEC-013, ENT-SEC-014, REQ-SEC-033, DBF-SEC-001, DBF-SEC-106

## Context
CS-SEC-001 is ADDITIVE, and a delta plan would normally carry only what changed. Three facts make that unsafe for this
plan:

1. `gov.py state` folds a marker-based plan phase by phase. A phase the delta re-emits gets back only its missing
   direct children, and nothing outside a marker is carried at all. That covers the index, the DB Alignment Manifest,
   the Query Reference Catalog, the Error Catalog, the self-check and the QR id list. A plan that re-emitted only
   SVC-API's changed atoms would lose every v1 atom in the SUBs it touched, and the catalog would lose all of v1.
2. SEC v1's own P3.1 report (`_state/analyze-stage-P3.1.md`, 2026-09-17) is BLOCKED with 59 findings against the
   contracts as they now stand. 55 are `required-writer`: v1's endpoint blocks name their DBF ids in `traces=` but not
   on their Request or Orchestration lines. The rest are no BOOTSTRAP DATA section, 20 of 27 endpoint blocks with no
   entity line (so `operation-resolves` could not run), and a hand-written verdict. The v2 gate reads the whole folded
   plan, so these v1 gaps would block v2 as well.
3. ADR-SEC-035 moved all 14 PKs from identity columns to named sequences. Every v1 BINDINGS line and the CORE
   type-mapping row named `GENERATED ALWAYS AS IDENTITY`, which the v2 migration drops.

## Decision
The v2 plan is the complete current plan. Every phase is re-emitted, with v1 content restated and v2 content marked.
v1 blocks are brought to the current template in this pass:
- each endpoint block gets an `Entity` line and a `Concurrency` line, and names every DBF it writes on its Request or
  Orchestration line;
- each entity block gets an `OPERATIONS` line and a sequence binding;
- a BOOTSTRAP DATA section and declared totals are added;
- the code format is declared verbatim from the profile (`{MOD}-{http}[-{SLUG}]`);
- ADR paths are corrected from `erp/decisions/SEC/` to `decisions/SEC/`;
- v1's prose self-check, whose rows no check could falsify, is replaced by the check-backed ALIGN block.

Every re-emitted v1 id whose text differs from v1 is listed MODIFIED in the version's change manifest. The Delta line
of the plan header says which changes are substantive.

Writing the Concurrency lines exposed four v1 read-then-write races. Each gets a guard, and the guard is new
behaviour:
- API-SEC-004 consumes the reset token with a conditional update;
- API-SEC-011 loads the sign-up request under a write lock;
- API-SEC-014 … API-SEC-017 lock the role row, so a module revoke and a concurrent screen or action grant are
  serialized;
- API-SEC-026 terminates a session with a conditional update.

## Known deviation, recorded and not changed
The profile places business decisions in dedicated domain classes (`conventions.domain_behaviour_placement:
domain_classes`). The v2 rules follow it (`UserDomain`, `ServiceAccountCredentialDomain`). The v1 rules RULE-SEC-001 …
RULE-SEC-007 keep their delivered placement. Re-homing working v1 code is not part of a change set about service
accounts. A later SEC change set should converge them.

## Consequences
- The folded `_state/current-backend-execution-plan.md` equals this file. No v1 block is appended out of order.
- The implementer builds the four race guards above and the v2 content. Every other v1 block describes what is
  already built.
- Non-breaking: no REQ, RULE, DBF or published path changes meaning.
