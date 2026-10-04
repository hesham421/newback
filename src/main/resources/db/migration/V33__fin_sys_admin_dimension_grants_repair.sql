-- ============================================================
-- V33 — Repair: SYS_ADMIN is missing the two FIN_DIMENSIONS action grants
-- ============================================================
-- What is wrong, measured rather than assumed
-- ------------------------------------------------------------
-- Observed live on 2026-09-22 against the dev database (erp_db) while running FIN's frontend
--   test phase: POST /api/v1/fin/dimensions and POST /api/v1/fin/dimensions/{id}/values both
--   answer 403 FIN-403-FORBIDDEN for the bootstrap `admin` user, while the SAME token is
--   accepted by POST /fin/accounts (201) and POST /fin/fiscal-years (201) and reaches business
--   validation on POST /fin/event-rules (400 FIN-400-INVALID-LOOKUP). So the module grant, the
--   screen grants and the session are all sound; only two Tier-3 action rows are absent.
--
--   SELECT over SEC_ROLE_ACTION_GRANT joined to SEC_ACTION_REG for module FIN returned 25 of the
--   27 registered permission codes granted to SYS_ADMIN, the two absent being exactly
--   PERM_FIN_DIMENSIONS_CREATE (registered by V24) and PERM_FIN_DIMENSIONS_UPDATE (registered by
--   V28).
--
-- Why it is a DATA drift and not a code defect
-- ------------------------------------------------------------
-- The migrations are correct and were never the source of these rows in this database. Every FIN
--   action grant present carries GRANTED_BY = 'admin' and GRANTED_AT = 2026-09-21 15:15 — 25 rows
--   created by hand through the SEC screens when FIN was granted for frontend testing. The only
--   GRANTED_BY = 'SYSTEM' rows in the table predate FIN entirely (2026-09-11). So V25's and V28's
--   INSERTs are not represented here, and a hand-grant of 25 of 27 rows is what left the gap.
--
--   Nothing about the contract changed, so no api-docs entry changes: the endpoints, their
--   request and response shapes and their @PreAuthorize codes are all exactly as published.
--   DimensionService.create gates on PERM_FIN_DIMENSIONS_CREATE and DimensionValueService.create
--   on the same code, symmetrically with AccountService — this migration only supplies the grant
--   rows those gates have always expected.
--
-- ⚠ WHY THIS ONE IS GUARDED WHEN V25/V28/V30 ARE NOT
-- ------------------------------------------------------------
-- V24/V25/V27/V28/V30 are plain, non-idempotent INSERTs, correct because each introduces rows
--   that cannot already exist on the fresh schema it runs against. This migration is different in
--   kind: it REPAIRS a row that V25 and V28 already insert on any database built from scratch. On
--   such a database both rows are present when V33 runs, and SEC_ROLE_ACTION_GRANT carries
--   UNIQUE (ROLE_ID, ACTION_ID) — `uq_sec_role_action_grant_role_action`. An unguarded INSERT
--   would therefore not merely duplicate; it would RAISE and fail the migration, breaking every
--   clean deployment to repair one drifted one.
--
--   Hence NOT EXISTS: a no-op wherever V25/V28 already did their work, and the repair only where
--   the rows are genuinely absent.
--
-- What is deliberately NOT touched
-- ------------------------------------------------------------
-- PERM_FIN_PERIODS_CLOSE_APPROVE is left exactly as it stands. V25 withheld it for RULE-FIN-015
--   and V30 granted it back with its reasoning recorded in full; this migration is scoped to two
--   permission codes by name and expresses no opinion on that decision.
--
-- No screen, module or role row is created, and no grant is revoked. Style otherwise matches
--   V28/V30: surrogate PK from the V16 SEQ_SEC_* sequence, FKs resolved by natural key
--   (role CODE / PERMISSION_CODE), never a hardcoded id.
-- ============================================================

INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'),
       r.ROLE_PK,
       a.ACTION_REG_PK,
       'SYSTEM',
       CURRENT_TIMESTAMP
FROM SEC_ACTION_REG a
CROSS JOIN (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN') r
WHERE a.PERMISSION_CODE IN ('PERM_FIN_DIMENSIONS_CREATE', 'PERM_FIN_DIMENSIONS_UPDATE')
  AND NOT EXISTS (
        SELECT 1
        FROM SEC_ROLE_ACTION_GRANT g
        WHERE g.ROLE_ID = r.ROLE_PK
          AND g.ACTION_ID = a.ACTION_REG_PK
      );
