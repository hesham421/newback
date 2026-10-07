<!-- source: PHASE:SVC-API — preamble before the first SUB -->
<!-- traces: REQ-MDL-001, REQ-MDL-006, REQ-MDL-011 -->
## PHASE 3 — SVC-API

API count = 11 ≥ 8 → split by threshold. Only two of the three standard groups are
populated (no MDL endpoint is "INT"-shaped in the SEC sense — no auth flow, no onboarding
registration, no export); `SVC-API-INT` is therefore omitted rather than opened empty,
consistent with engine §6.2 ("Content: the roles whose words appear... otherwise as the
profile describes this phase" — an empty SUB with no atoms would violate "every atom then
sits inside a SUB — no orphan atoms beside SUBs" trivially, since there would be none to
place; omitting an unneeded SUB is the correct reading, not a violation).
