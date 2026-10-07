<!-- source: PHASE:F3 / SUB:F3-SCR-MDL-002 -->
<!-- context: F3-HEADER.md — phase-level preamble -->
<!-- traces: AC-MDL-013, API-MDL-010, REQ-MDL-011, REQ-MDL-013, SCR-MDL-002, UXD-MDL-001 -->
<!-- SUB:F3-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-013,AC-MDL-013,API-MDL-010 -->
### F3 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

This screen has **no form**: SRS §B3 reads "read-only browse; no create/update here", and every
property of the response is read-only.

#### F3-FIELD — SCR-MDL-002 (filters — not a form)
ownerModuleCode · optional · the select offers the UXD-MDL-001 list; when that read is refused
                  or fails the select falls back to the distinct `ownerModuleCode` values in the
                  current API-MDL-010 response, never to free text (ADR-MDL-013, ADR-MDL-015, G6)
                  — a code that is no longer registered but still owns types is still shown in
                  the results either way, because the grouping is the server's
key             · optional · LENGTH (maxLength 50) · a LIKE filter

#### F3-VALIDATION — none on this screen
No `RULE-*` is enforced here, because nothing is written. RULE-MDL-004's effect is visible — a
deactivated type leaves this registry's active set — but the rule fires on the consumer read
(API-MDL-011), not on this screen.
Shape  : filter validation only, written with `zod` over the route's search params, so an address
         someone shared is validated exactly as a typed filter is.
Locale : session → browser → `ar`.
Permission-driven behaviour: a caller without VIEW never reaches this screen — the navigation
guard of SEC-FE stops the route before any of this runs.
<!-- SUB:F3-SCR-MDL-002:END -->
