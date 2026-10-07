<!-- source: PHASE:SEC-BE -->
<!-- traces: REQ-MDL-001 -->
<!-- PHASE:SEC-BE:START traces=REQ-MDL-001 -->
## PHASE 7 — SEC-BE (security, backend half)

| Screen (page code) | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|
| MDL_LOOKUPS | PERM_MDL_LOOKUPS_VIEW (API-MDL-001, 005, 011) | PERM_MDL_LOOKUPS_CREATE (API-MDL-002, 006) | PERM_MDL_LOOKUPS_UPDATE (API-MDL-003, 004, 007, 008, 009) | (deactivate only, modeled as UPDATE — no hard-delete endpoint exists) |
| MDL_TYPE_REGISTRY | PERM_MDL_TYPE_REGISTRY_VIEW (API-MDL-010) | — | — | — |

**Seed data**: 2 SEC_PAGES rows (MDL_LOOKUPS, MDL_TYPE_REGISTRY) registered into SEC via SEC's own screen-registration endpoint (MDL is a consuming module registering itself, exactly the pattern security-module-plan-en.md §7 describes); action rows VIEW/CREATE/UPDATE per secured screen via SEC's own action-registration endpoint, following `PERM_<PAGE_CODE>_<ACTION>`.

**Gateway**: every non-VIEW permission requires VIEW on the same screen first (platform
convention, enforced by SEC's own CORE interceptor — not restated as an MDL-owned RULE).

**Forbidden responses**: `MDL` endpoints reuse the same `LocalizedException` envelope; a
403 from the interceptor is not module-specific (see SEC's `SEC-403-FORBIDDEN` — the same
mechanism denies MDL requests, this module mints no separate forbidden code).
<!-- PHASE:SEC-BE:END -->
