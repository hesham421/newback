<!-- source: PHASE:TEST-PLAN-FE — preamble before the first SUB -->
<!-- traces: REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010, REQ-MDL-013, AC-MDL-001, AC-MDL-002, AC-MDL-003, AC-MDL-004, AC-MDL-005, AC-MDL-006, AC-MDL-007, AC-MDL-008, AC-MDL-009, AC-MDL-010, AC-MDL-013 -->
## PHASE — TEST-PLAN-FE

TC count is 11, above the split threshold of 8, so this phase is split into the two groups the
engine names: the per-screen flows, and the flows whose assertion spans a state change across
both levels of the composite screen.

Every TC below runs in both languages where a message is asserted: the locale resolves
session → browser → `ar` (`profile.languages.primary`), and the client keys its displayed text on
`error.code` against the module's catalog, which carries the ar and the en wording.
