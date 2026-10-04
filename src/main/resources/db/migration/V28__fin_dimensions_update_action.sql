-- ============================================================
-- V28 — Register FIN_DIMENSIONS / UPDATE and grant it to SYS_ADMIN
-- ============================================================
-- Source: V24__fin_security_seed.sql (the SEC_ACTION_REG INSERT ... SELECT shape, column list,
--   sequence name and NAME_AR / NAME_EN composition are copied from it verbatim) and
--   V25__fin_role_grants.sql (the SEC_ROLE_ACTION_GRANT column list and sequence name, likewise).
--
-- Why this migration exists. API-FIN-035 (PUT /api/v1/fin/dimensions/values/{id}/deactivate,
--   DimensionController.deactivateDimensionValue -> DimensionValueService.deactivate) is the first
--   UPDATE-class endpoint on the FIN_DIMENSIONS screen. It gates on PERM_FIN_DIMENSIONS_UPDATE,
--   newly declared in com.erp.sec.permission.PermissionConstants. V24 registered FIN_DIMENSIONS
--   with VIEW and CREATE rows only (V24:112-114, whose own comment reads "No UPDATE cell") - no
--   such endpoint existed then. Without this migration the @PreAuthorize gate would reference a
--   permission code that resolves to no SEC_ACTION_REG row and can therefore never be granted.
--
--   Deactivate is modelled as UPDATE, not DELETE — the choice FIN already made for the analogous
--   endpoint on another screen. The grounded evidence, quoted from text actually read:
--     * srs-fin.md:1139-1141 (SCR-REQ-FIN-001 / FIN_ACCOUNTS, B4 Access): "Actions: VIEW, CREATE,
--       UPDATE (covers deactivate - there is no DELETE endpoint and no PERM_FIN_ACCOUNTS_DELETE;
--       deactivate is `PUT /{id}/deactivate` gated by `PERM_FIN_ACCOUNTS_UPDATE`)."
--     * srs-fin.md:1484-1487 ("DELETE column, as built"): "empty for every FIN screen. FIN
--       publishes no `DELETE` endpoint; deactivation, where it exists, is `PUT /{id}/deactivate`
--       gated by the screen's UPDATE permission, and V24 seeds no `PERM_FIN_*_DELETE` row." That
--       statement is module-wide, so it covers FIN_DIMENSIONS too.
--     * V24__fin_security_seed.sql:49-51 reasons identically about SEC-BE.md's FIN_ACCOUNTS /
--       DELETE cell and seeds an UPDATE row rather than a DELETE one.
--     * In code: AccountController's `PUT /api/v1/fin/accounts/{id}/deactivate` (API-FIN-004)
--       delegates to AccountService.deactivate, which gates on PERM_FIN_ACCOUNTS_UPDATE.
--   No DELETE row is invented here; FIN publishes no hard-delete endpoint on any screen.
--
--   What srs-fin.md says about THIS screen today, stated plainly because this migration
--   contradicts it: srs-fin.md:1165-1169 (SCR-REQ-FIN-002 / FIN_DIMENSIONS, B4 Access) records the
--   ABSENCE of the endpoint as the as-built state - "Actions: VIEW, CREATE. No UPDATE and no
--   DELETE: as built, FIN publishes no dimension or dimension-value update/deactivate endpoint,
--   V24 seeds no `PERM_FIN_DIMENSIONS_UPDATE`, and the 'deactivate' listed under B1 Operations is
--   therefore not delivered in v1 - the `isActiveFl` columns (DBF-FIN-018, DBF-FIN-029) exist and
--   default TRUE, but nothing can flip them through the API." That paragraph accurately describes
--   the state BEFORE this migration. API-FIN-035 and the row below are exactly what change it; the
--   SRS paragraph is being brought into line by the separate spec-alignment session, not by this
--   file. Scope of the change, so the correction is not overstated: API-FIN-035 delivers the
--   dimension VALUE deactivate only. There is deliberately no deactivate on the parent Dimension,
--   so DBF-FIN-018's flag stays unflippable through the API and that half of the SRS sentence
--   remains true.
--
-- Why statement 2 is not optional. V25 grants SYS_ADMIN its FIN action rows with a SELECT over
--   SEC_ACTION_REG (every FIN row except PERM_FIN_PERIODS_CLOSE_APPROVE). That migration has
--   ALREADY RUN on every environment, and Flyway will not re-run it, so its SELECT cannot
--   retroactively see a row inserted now. Registering without granting is precisely the failure
--   V19 shipped for MDL and V21 had to repair: V19__mdl_security_seed.sql:68-69 states outright
--   that it writes no SEC_ROLE_*_GRANT rows, and V21__mdl_role_grants.sql:4-10 records the
--   observable result - "MDL's endpoints answer 403 ACCESS_DENIED to every caller, including
--   SYS_ADMIN" - together with the same root cause as here, that V17's Tier-3 grant block "granted
--   every action that existed *at V17 time*" and later-arriving rows "were never covered". The
--   explicit Tier-3 grant below is what avoids repeating that.
--
--   Tiers 1 and 2 need nothing new, verified in V25__fin_role_grants.sql itself: its Tier-1
--   statement (V25:76-80) grants SYS_ADMIN the FIN module row, and its Tier-2 statement (V25:88-94)
--   grants every screen of module FIN by joining SEC_SCREEN_REG to SEC_MODULE_REG on CODE = 'FIN',
--   which includes FIN_DIMENSIONS. The gateway convention also holds: V24:113 seeds the
--   ('FIN_DIMENSIONS', 'VIEW') row and V25's Tier-3 statement (V25:103-111) granted it, so this new
--   non-VIEW permission sits behind an already-granted VIEW on the same screen. That is RULE-SEC-007,
--   stated in full at governance/modules/SEC/P3_1/backend-execution-plan-sec.md:665 - "The system
--   shall require a role to hold the VIEW action grant on a screen before any other action grant
--   on that screen takes effect for it."
--
--   FIN_CLOSE_APPROVER (V27) deliberately gets nothing here. Its grants are scoped to one screen
--   and two codes - V27:97 grants only the FIN_PERIODS screen and V27:110 filters to
--   PERM_FIN_PERIODS_VIEW and PERM_FIN_PERIODS_CLOSE_APPROVE - and nothing in the artifacts gives
--   that role dimension administration.
--
-- Style matches V24/V25/V27: plain INSERTs (run once, not idempotent), surrogate PKs from the V16
--   SEQ_SEC_* sequences, every FK resolved by natural key (PAGE_CODE / role CODE), never a
--   hardcoded id. Booleans are native (V16 uses BOOLEAN).
-- ============================================================

-- ------------------------------------------------------------
-- 1. SEC_ACTION_REG — the FIN_DIMENSIONS / UPDATE row.
--    PERMISSION_CODE is synthesised the same way V24 does it, 'PERM_' || page_code || '_' ||
--    action_code, which yields PERM_FIN_DIMENSIONS_UPDATE verbatim.
-- ------------------------------------------------------------
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_ACTION_REG'),
       'PERM_' || v.page_code || '_' || v.action_code,
       s.SCREEN_REG_PK,
       v.action_code,
       s.NAME_AR || ' - ' || v.action_ar,
       s.NAME_EN || ' - ' || v.action_code,
       TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    -- FIN_DIMENSIONS — UPDATE (API-FIN-035, deactivate a dimension value)
    ('FIN_DIMENSIONS',          'UPDATE',        'تعديل')
) AS v(page_code, action_code, action_ar)
JOIN SEC_SCREEN_REG s ON s.PAGE_CODE = v.page_code;

-- ------------------------------------------------------------
-- 2. SEC_ROLE_ACTION_GRANT — grant the new row to SYS_ADMIN.
--    Scoped to the one PERMISSION_CODE this migration introduced, so it cannot disturb the 25
--    grants V25 already made, and so it never touches PERM_FIN_PERIODS_CLOSE_APPROVE (RULE-FIN-015
--    keeps that one out of SYS_ADMIN's hands — see V25's and V27's headers).
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'),
       (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
       a.ACTION_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_ACTION_REG a
WHERE a.PERMISSION_CODE = 'PERM_FIN_DIMENSIONS_UPDATE';
