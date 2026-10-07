<!-- source: content outside every PHASE block (leading / between / trailing sections) -->
# FRONTEND TEST PLAN — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp   Track : frontend   Plan : test
Scope  : **module** — modules MDL (`gov.py run-standalone test-gen --module MDL`)
Sources: `_state/current-srs.md` (v1) · `_state/current-frontend-execution-plan.md` (v1) ·
         `_state/current-registry-srs.md` (v1) · `_state/current-registry-exec-fe.md` (v1)
Framework : **agnostic** (`profile.stack.testing`). Every block below is the framework-agnostic
            TC form; no tool, selector, annotation or file layout is named anywhere in this plan.
            The consumer repo chooses its framework and turns each TC into a test.
REDUCED   : no — the frontend-execution-plan is present, so every step binds to a real `SCR-*`
            and a real route.
TC ids    : TC-MDL-015 … TC-MDL-025 — the continuation of the one module sequence
            `backend-test-plan-mdl.md` opened at TC-MDL-001 and closed at TC-MDL-014. The
            sequence does not restart here and no id is reused across the two files.
Open ADRs : 2 raised by this run — ADR-MDL-023 (this file's AC denominator), ADR-MDL-024.
            Applied and not re-derived: ADR-MDL-002, ADR-MDL-005, ADR-MDL-006, ADR-MDL-007,
            ADR-MDL-012, ADR-MDL-013, ADR-MDL-014, ADR-MDL-015.
══════════════════════════════════════════════════════════════════

**Two ACs have no screen, and that is not a gap (ADR-MDL-023).** AC-MDL-011 and AC-MDL-012 are
the consumer read by key: `API-MDL-011` is bound by the frontend plan and **called by no screen**
— a consuming module's backend performs it over the platform's in-process interface (ADR-MDL-007),
and registry-exec-fe-mdl.md records the same 11-of-13 ratio for exactly this reason. Both are
covered on the backend track (TC-MDL-011, TC-MDL-012). This plan therefore states its AC coverage
against the **screen-bearing** AC set of eleven, and lists the two out-of-track ACs by id rather
than marking them ✗ — a ✗ would assert a missing test for a screen that does not exist.
Module-level AC coverage across both tracks is 13/13.

**No PERMISSION TC appears here**, and none is owed: no `AC-*` in the SRS states a denied VIEW or
a denied action. Action-level grants are readable from no published surface, so the screens render
their affordances and the server's `ACCESS_DENIED` is the authority (ADR-MDL-012, PF-MDL-001) —
there is no acceptance criterion describing either half, and inventing one is out of this engine's
boundary.

**No BOUNDARY TC appears here.** No AC and no RULE this module carries states a numeric limit;
the column widths are field constraints stated in F1/F3, not an AC's assertion.



## TC TRACEABILITY INDEX

**AC → TC**

| AC | TC | AC | TC |
|---|---|---|---|
| AC-MDL-001 | TC-MDL-015 | AC-MDL-008 | TC-MDL-021 |
| AC-MDL-002 | TC-MDL-016 | AC-MDL-009 | TC-MDL-025 |
| AC-MDL-003 | TC-MDL-017 | AC-MDL-010 | TC-MDL-022 |
| AC-MDL-004 | TC-MDL-024 | AC-MDL-011 | — no screen (ADR-MDL-007) → TC-MDL-011, backend |
| AC-MDL-005 | TC-MDL-018 | AC-MDL-012 | — no screen (ADR-MDL-007) → TC-MDL-012, backend |
| AC-MDL-006 | TC-MDL-019 | AC-MDL-013 | TC-MDL-023 |
| AC-MDL-007 | TC-MDL-020 | — | — |

**REQ → TC**

| REQ | TC | REQ | TC |
|---|---|---|---|
| REQ-MDL-001 | TC-MDL-015 | REQ-MDL-008 | TC-MDL-021 |
| REQ-MDL-002 | TC-MDL-016 | REQ-MDL-009 | TC-MDL-025 |
| REQ-MDL-003 | TC-MDL-017 | REQ-MDL-010 | TC-MDL-022 |
| REQ-MDL-004 | TC-MDL-024 | REQ-MDL-011 | — no screen → backend track |
| REQ-MDL-005 | TC-MDL-018 | REQ-MDL-012 | — no screen → backend track |
| REQ-MDL-006 | TC-MDL-019 | REQ-MDL-013 | TC-MDL-023 |
| REQ-MDL-007 | TC-MDL-020 | — | — |

**SCR → TC**

| SCR | route(s) exercised | TC |
|---|---|---|
| SCR-MDL-001 | `/reference-data/lookups` · `/new` · `/:typeId` · `/:typeId/edit` · `/:typeId/values/new` · `/:typeId/values/:valueId/edit` | TC-MDL-015, TC-MDL-016, TC-MDL-017, TC-MDL-018, TC-MDL-019, TC-MDL-020, TC-MDL-021, TC-MDL-022, TC-MDL-024, TC-MDL-025 |
| SCR-MDL-002 | `/reference-data/type-registry` | TC-MDL-023, TC-MDL-024 (the registry assertion) |

**RULE / catalog code → TC**

| RULE | code | TC |
|---|---|---|
| RULE-MDL-001 | MDL-409-MODULE-NOT-REGISTERED (409) | TC-MDL-016 |
| RULE-MDL-002 | MDL-409-VALUE-DUP (409) | TC-MDL-020 · TC-MDL-021 (not raisable on edit, asserted as such) |
| RULE-MDL-003 | — (expressed by the field's absence) | TC-MDL-017 |
| RULE-MDL-004 | — on this track (the code is answered to a calling module, not to a screen) | TC-MDL-024 — the rule's text as the confirmation and the state label |

**UXD → TC** — UXD-MDL-001 (`ownerModuleCode`, owned by SEC) is cited by TC-MDL-015 (the select
that must offer `FIN`), TC-MDL-016 (the field the refusal routes to) and TC-MDL-023 (the group
headings and their fallback). No **integration** TC is derived from it: at `scope: module` the
`INT-UXD` phase is skipped entirely (§2 rule 3), and SEC — the owner module — is not in this
selection. Nothing about UXD-MDL-001 is recorded as a gap here; it is not this run's concern.

## COVERAGE

```
AC  covered   11/11   ✓ 0 gaps   of the screen-bearing AC set (ADR-MDL-023)
                      AC-MDL-011, AC-MDL-012 — out of track, no screen implements them
                      (ADR-MDL-007); both covered by TC-MDL-011 / TC-MDL-012 on the backend
AC  (module)  13/13   ✓ 0 gaps   across both tracks
REQ covered   11/13   REQ-MDL-011, REQ-MDL-012 have no screen — the same two, the same reason
SCR covered    2/2    ✓ each of SCR-MDL-001 and SCR-MDL-002 carries ≥1 TC
TC count       11     ✓ 1.0× the screen-bearing AC count — under the ~2× over-engineering guard
Integration    n/a    scope = module: no INT-UXD phase is emitted and none is owed (§2 rule 3)
```
══════════════════════════════════════════════════════════════════
