# TEST EXECUTION MANIFEST — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp   Scope : module
Emitted because `profile.stack.testing.manifest` is ON. A **derived view** for the `api-verify`
standalone: it introduces no id, defines nothing, gates nothing, and every cell below is an
`ID` or a value one of the cited artifacts already states.
Derived from : `backend-test-plan-mdl.md` (this run) · `_state/current-backend-execution-plan.md`
               (v1) · `_state/current-srs.md` (v1)
Regenerate whenever the backend execution plan or the backend test plan changes — a stale
manifest is a defect.
══════════════════════════════════════════════════════════════════

## DEPENDENCY ORDER

The order in which entities must be built for any TC of this module to run. It is read off the
FK and XM relations of the backend plan and the "must belong to" rules, and nothing else.

```
0.  SEC · ModuleRegistry row for the owner module code   (ENT-SEC-004, through XM-MDL-001)
      ── not this module's data and not created by any endpoint of it: it is the precondition
         RULE-MDL-001 reads at create time, a SOFT-READ with no foreign key. TC-MDL-001 needs
         the row present (FIN); TC-MDL-002 needs it absent (XYZ); TC-MDL-014 needs it present
         and then removed.
1.  ENT-MDL-001 · LookupType   (MDL_LOOKUP_TYPE)         ← API-MDL-002
      ── no intra-module parent. Its PK comes from SEQ_MDL_LOOKUP_TYPE (QR-MDL-016).
2.  ENT-MDL-002 · LookupValue  (MDL_LOOKUP_VALUE)        ← API-MDL-006
      ── depends on 1 through FK_LOOKUP_VALUE_TYPE (DBF-MDL-012); the parent type arrives as the
         path id, never in the body. Its PK comes from SEQ_MDL_LOOKUP_VALUE (QR-MDL-017).
```

No other order constraint exists in this module: there is no numbered document, no status
workflow, no period binding and no second cross-module dependency. Both tables start empty —
every required column is written by an endpoint or by the platform, so nothing is seeded.

## RULE → CODE → TC

Runtime code format `{MOD}-{http}[-{SLUG}]`; the envelope is
`LocalizedException → {code, messageAr, messageEn}`. Informational-only rules are excluded —
this module has none: all four are enforced, and the two that raise no code say why.

| RULE | catalog code | TC | HTTP | API |
|---|---|---|---|---|
| RULE-MDL-001 | MDL-409-MODULE-NOT-REGISTERED | TC-MDL-002 · TC-MDL-016 (frontend) | 409 | API-MDL-002 |
| RULE-MDL-001 | — (create-only limit: no code on any later path) | TC-MDL-014 | — | API-MDL-001, API-MDL-003, API-MDL-011 |
| RULE-MDL-002 | MDL-409-VALUE-DUP | TC-MDL-007 · TC-MDL-020 (frontend) | 409 | API-MDL-006 |
| RULE-MDL-002 | MDL-409-VALUE-DUP (unreachable through the update request shape) | TC-MDL-008 · TC-MDL-021 (frontend) | 409 | API-MDL-007 |
| RULE-MDL-003 | — (no catalog row exists: `key` is absent from the update request) | TC-MDL-003 · TC-MDL-017 (frontend) | — | API-MDL-003 |
| RULE-MDL-004 | MDL-404-TYPE-KEY | TC-MDL-004, TC-MDL-012 | 404 | API-MDL-011 |
| RULE-MDL-004 | — (read-time exclusion, no code: the type is active, the value is not returned) | TC-MDL-009, TC-MDL-011 | — | API-MDL-011 |

**Catalog rows no TC exercises**, listed so `api-verify` does not read this table as complete
coverage of the catalog: MDL-409-TYPE-DUP, MDL-404-TYPE, MDL-404-VALUE, MDL-400-REORDER-MISMATCH,
and the three platform rows VALIDATION_ERROR (400), ACCESS_DENIED (403), INTERNAL_ERROR (500).
No `AC-*` states any of them, and this engine derives from the ACs; the absence is a
requirements-side gap, not a test-generation one.

## ENTITY CRUD CHECKLIST

Each cell is ✓ only where an `API-*` of the backend plan serves it; a `—` carries its reason.

| ENT | create | read (by id) | search | update | deactivate (soft) | activate |
|---|---|---|---|---|---|---|
| ENT-MDL-001 · LookupType | ✓ API-MDL-002 | — none published (SRS names it, no REQ requires it) | ✓ API-MDL-001, API-MDL-010 | ✓ API-MDL-003 | ✓ API-MDL-004 | — out of scope (SRS A2: no reactivation in v1) |
| ENT-MDL-002 · LookupValue | ✓ API-MDL-006 | — none published (same reason) | ✓ API-MDL-005, API-MDL-011 | ✓ API-MDL-007, API-MDL-009 (rank) | ✓ API-MDL-008 | — out of scope (same) |

`deactivate` is the `DELETE` verb with `soft` semantics (`profile.stack.db.delete_semantics`): no
row is removed by any endpoint of this module. The two `—` in the `read` column are the operations
ADR-MDL-005 records as omitted rather than faked, and the two in `activate` are the SRS's own
scope statement — neither is an untested endpoint.
══════════════════════════════════════════════════════════════════
