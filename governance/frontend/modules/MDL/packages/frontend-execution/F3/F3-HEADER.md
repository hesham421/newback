<!-- source: PHASE:F3 — preamble before the first SUB -->
<!-- traces: SCR-MDL-001, SCR-MDL-002, UXD-MDL-001, REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-010, REQ-MDL-011, REQ-MDL-013, AC-MDL-001, AC-MDL-002, AC-MDL-003, AC-MDL-004, AC-MDL-006, AC-MDL-007, AC-MDL-008, AC-MDL-013, API-MDL-001, API-MDL-002, API-MDL-003, API-MDL-005, API-MDL-006, API-MDL-007, API-MDL-010 -->
## PHASE 3 — F3 — Forms & Validators

One block per `RULE-*` a form enforces, plus the field constraints the published DTOs state. No
frontend-only validation the SRS does not state; every message is read from its catalog code
rather than hard-coded; the locale resolves session → browser → `ar` (`profile.languages.primary`);
and a caller without the write permission is answered by the server, not by a pre-emptively
disabled field (ADR-MDL-012). Schemas are written with `zod` and bound with `react-hook-form`.
Every `maxLength` below is the deployed column width (db-script §1, ADR-MDL-010), per the
correction in API SURFACE (G1, a prior pass).

**No option-set validator exists anywhere in this module.** MDL owns no coded list (SRS §A6), so
no field binds to a set of lookup values. The one field with a constrained set is
`ownerModuleCode`, whose set is another module's registry: its validator binds to the
runtime-loaded list of UXD-MDL-001, never to a static list of module codes.
