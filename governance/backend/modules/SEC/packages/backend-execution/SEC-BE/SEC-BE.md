<!-- source: PHASE:SEC-BE -->
<!-- traces: REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-020, REQ-SEC-023, REQ-SEC-030, REQ-SEC-033 -->
<!-- PHASE:SEC-BE:START traces=REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-020,REQ-SEC-023,REQ-SEC-030,REQ-SEC-033 -->
## PHASE 7 — SEC-BE (security, backend half)

One block per screen (R7); every API serving a screen verifies its permission via the CORE
interceptor (Phase 1) before its controller method body runs.

| Screen (page code) | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|
| SEC_LOGIN | public | — | — | — |
| SEC_SIGNUP | public | — | — | — |
| SEC_PWD_RESET | public | — | — | — |
| SEC_USERS | PERM_SEC_USERS_VIEW (API-SEC-005) | PERM_SEC_USERS_CREATE (API-SEC-006) | PERM_SEC_USERS_UPDATE (API-SEC-007, 008, 009, 010, 011) | — |
| SEC_ROLES | PERM_SEC_ROLES_VIEW (API-SEC-012) | PERM_SEC_ROLES_CREATE (API-SEC-013) | PERM_SEC_ROLES_UPDATE (API-SEC-014..017) | PERM_SEC_ROLES_DELETE (reserved — no delete-role endpoint in v1, deactivate only, which is UPDATE) |
| SEC_MODULE_REGISTRY | PERM_SEC_MODULE_REGISTRY_VIEW (API-SEC-021) | (via registering module's own call, API-SEC-018/019/020) | PERM_SEC_MODULE_REGISTRY_UPDATE (deactivate — no v1 endpoint yet, reserved) | — |
| SEC_DASHBOARD | PERM_SEC_DASHBOARD_VIEW (API-SEC-022) + per-widget source VIEW | — | — | — |
| SEC_AUDIT_LOG | PERM_SEC_AUDIT_LOG_VIEW (API-SEC-023, 024) | — | — | — |
| SEC_SESSIONS | PERM_SEC_SESSIONS_VIEW (API-SEC-025) | — | — | PERM_SEC_SESSIONS_DELETE (API-SEC-026) |

**Seed data** (one SEC_PAGES row per composite screen, one action row per action per §7.1):
9 page rows (SEC_LOGIN, SEC_SIGNUP, SEC_PWD_RESET, SEC_USERS, SEC_ROLES,
SEC_MODULE_REGISTRY, SEC_DASHBOARD, SEC_AUDIT_LOG, SEC_SESSIONS) inserted via
API-SEC-019/registered by SEC's own bootstrap (SEC registers itself into itself — the one
module for which no external caller is needed); action rows: VIEW/CREATE/UPDATE/DELETE per
secured screen above, per PERM_<PAGE_CODE>_<ACTION> (profile.conventions.security_model).
Column names come from the db-script (SEC_SCREEN_REG, SEC_ACTION_REG), not restated here.

**Gateway**: every non-VIEW permission requires VIEW on the same screen first
(RULE-SEC-007, enforced by the CORE interceptor + at grant time by API-SEC-017/QR-SEC-030).

**Forbidden responses**: any denial from the CORE interceptor maps to
`LocalizedException{code: "SEC-403-FORBIDDEN", messageAr: "غير مصرح بهذا الإجراء",
messageEn: "You are not authorized to perform this action"}` (§Error Catalog `SEC-403`).
<!-- PHASE:SEC-BE:END -->
