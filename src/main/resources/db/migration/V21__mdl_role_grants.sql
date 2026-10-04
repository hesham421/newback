-- ============================================================
-- V21 — Grant SYS_ADMIN the MDL screens and actions
-- ============================================================
-- Why this migration exists: V19 registered MDL's 2 screens and 4 action rows into SEC's
-- registry but deliberately granted them to nobody ("registration is not a grant"). V17's own
-- Tier-3 grant block is `FROM SEC_ACTION_REG a` with no WHERE — it granted every action that
-- existed *at V17 time*, and MDL's actions only arrived later in V19, so they were never
-- covered. The observable result on a fresh database: MDL's endpoints answer 403 ACCESS_DENIED
-- to every caller, including SYS_ADMIN, while NOTIF/FILE's lookups still work because they
-- reach MDL through the crossmodule Spring interface rather than over HTTP.
--
-- Scope: SYS_ADMIN only, MDL only. This does not grant NOTIF or FILE anything — V20 registered
-- those two as modules in SEC_MODULE_REG, but neither has a screen or action row to grant.
--
-- Grant tiers and the rules they satisfy (same three-tier shape as V17's own seed):
--   Tier-1 SEC_ROLE_MODULE_GRANT — must precede the screen grants (RULE-SEC-001)
--   Tier-2 SEC_ROLE_SCREEN_GRANT — every screen whose actions are granted below (RULE-SEC-002)
--   Tier-3 SEC_ROLE_ACTION_GRANT — the 4 PERM_MDL_* codes. RULE-SEC-007 holds: MDL_LOOKUPS
--          carries CREATE/UPDATE *and* its VIEW, and the filter drops a non-VIEW permission
--          whose screen VIEW is absent, so granting VIEW is what makes the other two count.
--
-- Note on what actually gates a request: JwtAuthenticationFilter builds its authorities from
-- MenuService.effectivePermissionCodes(), which reads SEC_ROLE_ACTION_GRANT alone (joined to
-- action -> screen -> module only to test their active flags). Tier-3 is therefore the row that
-- lifts the 403; Tiers 1-2 are what put MDL into the caller's navigation menu and keep the
-- registry self-consistent. All three are seeded so the two views never disagree.
--
-- Style matches V17/V19/V20: plain INSERTs (run once on a fresh schema, not idempotent),
-- surrogate PKs from the V16 sequences, every FK resolved by natural key, never a hardcoded id.
-- ============================================================

-- ------------------------------------------------------------
-- Tier-1 — module grant (RULE-SEC-001: before any screen grant)
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_MODULE_GRANT (ROLE_MODULE_GRANT_PK, ROLE_ID, MODULE_ID, GRANTED_BY, GRANTED_AT)
VALUES (nextval('SEQ_SEC_ROLE_MODULE_GRANT'),
        (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'MDL'),
        'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- Tier-2 — screen grants (both MDL screens, per SEC-BE.md's matrix)
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_SCREEN_GRANT (ROLE_SCREEN_GRANT_PK, ROLE_ID, SCREEN_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_SCREEN_GRANT'),
       (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
       s.SCREEN_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_SCREEN_REG s
WHERE s.PAGE_CODE IN ('MDL_LOOKUPS', 'MDL_TYPE_REGISTRY');

-- ------------------------------------------------------------
-- Tier-3 — action grants: PERM_MDL_LOOKUPS_VIEW/CREATE/UPDATE + PERM_MDL_TYPE_REGISTRY_VIEW.
--          Selected by the MDL screens they belong to rather than by listing the four codes,
--          so the set stays correct if a later migration adds an action to either screen.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'),
       (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
       a.ACTION_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_ACTION_REG a
JOIN SEC_SCREEN_REG s ON s.SCREEN_REG_PK = a.SCREEN_ID
WHERE s.PAGE_CODE IN ('MDL_LOOKUPS', 'MDL_TYPE_REGISTRY');
