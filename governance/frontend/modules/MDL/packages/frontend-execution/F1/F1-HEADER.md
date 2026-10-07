<!-- source: PHASE:F1 — preamble before the first SUB -->
<!-- traces: SCR-MDL-001, SCR-MDL-002, UXD-MDL-001, REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010, REQ-MDL-011, REQ-MDL-012, REQ-MDL-013, AC-MDL-001, AC-MDL-003, AC-MDL-005, AC-MDL-006, AC-MDL-008, AC-MDL-010, AC-MDL-011, AC-MDL-013, API-MDL-001, API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-005, API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009, API-MDL-010, API-MDL-011 -->
## PHASE 1 — F1 — Models & Types

Per entity and per screen: the source DTO with each property's type and its read-only /
system-only status, then the screen's search model, form model and container. Names are carried
per language (ar, en) wherever a label is modelled. Nothing is modelled that the api-docs do not
return, no internal identifier is invented, and **no enum and no lookup-backed field appears
anywhere in this phase** — MDL owns no coded list of its own (SRS §A6).

Field and DTO binding: see `_inputs/api-docs-mdl.md` — the published request and response shapes
for this module are the source and are not restated here. What is stated here is the shape's
consequence for the client: which properties a form may write, and which it may only display.
Every `maxLength` below is the deployed db-script column width (db-script §1, ADR-MDL-010), per
the correction in API SURFACE above (G1, a prior pass); a divergence from the api-docs is
PF-MDL-002, not a number restated from them.
