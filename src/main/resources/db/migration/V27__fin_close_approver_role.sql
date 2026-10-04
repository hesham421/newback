-- ============================================================
-- V27 — FIN close-approval role (FIN_CLOSE_APPROVER) — the role V25 deliberately did not mint
-- ============================================================
-- What this closes. V24 registered PERM_FIN_PERIODS_CLOSE_APPROVE (FIN_PERIODS / CLOSE_APPROVE,
-- the gate on API-FIN-026 hard-close and API-FIN-027 year-end close) and V25 granted it to
-- NOBODY — deliberately, because the only role this schema seeds is SYS_ADMIN (V17 §4), SYS_ADMIN
-- must hold PERM_FIN_JOURNAL_ENTRIES_CREATE, and RULE-FIN-015 / REQ-FIN-038 forbid one holder of
-- both. V25's header prescribed the repair as a purely administrative act through SEC's live role
-- administration and minted no role, on the grounds that inventing a role code would be inventing
-- governance content. The consequence was that both endpoints answered 403 ACCESS_DENIED at
-- @PreAuthorize on every deployment, with no seeded path out.
--
-- Human decision recorded (final): the close-approval right gets a DEDICATED role that holds it
-- and, by construction, cannot hold entry creation. RULE-FIN-015 then holds structurally rather
-- than by administrative convention — there is no way to assign this role and accidentally also
-- grant journal-entry creation through it.
--
-- ------------------------------------------------------------
-- What the role holds, and what it deliberately does not
-- ------------------------------------------------------------
--   Tier-1  SEC_ROLE_MODULE_GRANT  : module FIN (RULE-SEC-001 — must precede any screen grant)
--   Tier-2  SEC_ROLE_SCREEN_GRANT  : screen FIN_PERIODS only (RULE-SEC-002)
--   Tier-3  SEC_ROLE_ACTION_GRANT  : exactly two action rows —
--             * PERM_FIN_PERIODS_VIEW          — the gateway row (RULE-SEC-007)
--             * PERM_FIN_PERIODS_CLOSE_APPROVE — the right this migration exists to make holdable
--
-- Why the VIEW row is not optional: MenuService.effectiveAuthorityCodes() keeps a granted
-- permission only if the SAME screen also carries a granted gateway (VIEW) action — see
-- RoleActionGrantDomain.isGatewayAction. A lone CLOSE_APPROVE grant would be filtered out of the
-- caller's authorities and @PreAuthorize would still answer 403. V24 registered
-- PERM_FIN_PERIODS_VIEW for exactly this purpose.
--
-- Nothing else is granted. In particular NOT PERM_FIN_JOURNAL_ENTRIES_CREATE, and not any other
-- FIN screen or action: the Tier-2 grant is restricted to FIN_PERIODS and the Tier-3 insert names
-- its two permission codes explicitly rather than selecting "all FIN actions".
--
-- ------------------------------------------------------------
-- OPERATIONAL PRECONDITIONS — this migration alone does NOT make the close work
-- ------------------------------------------------------------
-- This file creates and grants the role. It assigns it to NO USER, and both facts below must be
-- understood before anyone reports the close as still broken.
--
-- (1) A USER ASSIGNMENT IS STILL REQUIRED. FinSeparationOfDutiesService.resolveFacts() reports
--     closeApprovePermissionHeld = !approvers.isEmpty(), and
--     FiscalPeriodDomain.assertCanHardClose throws FIN-403-SOD-VIOLATION when
--     !approverHoldsCloseApprovePermission. So "nobody holds close-approval" fails the close just
--     as surely as "one user holds both". Until a real SEC_USER_ROLE row points some user at
--     FIN_CLOSE_APPROVER, API-FIN-026 and API-FIN-027 still fail.
--
-- (2) THE ASSIGNEE MUST NOT BE A JOURNAL-ENTRY CREATOR — and must NOT be the bootstrap 'admin'.
--     resolveFacts() reads both permissions' user sets across each user's ROLE UNION and reports
--     entryCreatePermissionShared = !Collections.disjoint(approvers, creators). The bootstrap
--     'admin' user (V17 §8/§9) holds SYS_ADMIN, which V25 granted PERM_FIN_JOURNAL_ENTRIES_CREATE.
--     Assigning FIN_CLOSE_APPROVER to 'admin' would therefore put the same user in both sets and
--     trip FIN-403-SOD-VIOLATION on EVERY hard-close and year-end close — defeating RULE-FIN-015
--     exactly as it is written. That is why no user assignment is seeded here: this migration
--     cannot know which real person is not a journal-entry creator, and picking 'admin' (the only
--     user a fresh database has) would be the one wrong answer.
--
--     Assign it, through SEC's own role administration (API-SEC-008 / API-SEC-014 surface), to a
--     user who holds NO role carrying PERM_FIN_JOURNAL_ENTRIES_CREATE — today that means no
--     SYS_ADMIN role. That user assignment is an environment-specific administrative act, not
--     schema, so it stays out of migration history on purpose.
--
-- Style matches V17/V21/V25: plain INSERTs (run once on a fresh schema, not idempotent),
-- surrogate PKs from the V16 SEQ_SEC_* sequences, every FK resolved by natural key, never a
-- hardcoded id. Role code shape follows the seeded convention <SCOPE>_<FUNCTION> (SYS_ADMIN,
-- CU_ADMIN, FILE_ADMIN, NOTIF_ADMIN); bilingual NAME_AR/NAME_EN + DESCRIPTION_AR/DESCRIPTION_EN
-- follow V17 §4, the one SEC_ROLE insert written against the V16 schema.
-- ============================================================

-- ------------------------------------------------------------
-- 1. SEC_ROLE — the dedicated close-approval role.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE (ROLE_PK, CODE, NAME_AR, NAME_EN, DESCRIPTION_AR, DESCRIPTION_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ROLE'), 'FIN_CLOSE_APPROVER',
        'معتمد إقفال الفترات المالية', 'Fiscal Period Close Approver',
        'يملك اعتماد الإقفال النهائي للفترات والإقفال السنوي فقط، ولا يملك إنشاء قيود اليومية (RULE-FIN-015)',
        'Holds period hard-close and year-end close approval only; deliberately holds no journal-entry creation (RULE-FIN-015)',
        TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 2. Tier-1 — module grant: FIN (RULE-SEC-001).
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_MODULE_GRANT (ROLE_MODULE_GRANT_PK, ROLE_ID, MODULE_ID, GRANTED_BY, GRANTED_AT)
VALUES (nextval('SEQ_SEC_ROLE_MODULE_GRANT'),
        (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'FIN_CLOSE_APPROVER'),
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'FIN'),
        'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 3. Tier-2 — screen grant: FIN_PERIODS only (RULE-SEC-002). No other FIN screen.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_SCREEN_GRANT (ROLE_SCREEN_GRANT_PK, ROLE_ID, SCREEN_ID, GRANTED_BY, GRANTED_AT)
VALUES (nextval('SEQ_SEC_ROLE_SCREEN_GRANT'),
        (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'FIN_CLOSE_APPROVER'),
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'FIN_PERIODS'),
        'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 4. Tier-3 — action grants: the gateway VIEW row and CLOSE_APPROVE, and nothing else.
--    Named explicitly (never "all actions of the screen"): FIN_PERIODS also carries CREATE and
--    UPDATE rows, which this role must not acquire.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'),
       (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'FIN_CLOSE_APPROVER'),
       a.ACTION_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_ACTION_REG a
WHERE a.PERMISSION_CODE IN ('PERM_FIN_PERIODS_VIEW', 'PERM_FIN_PERIODS_CLOSE_APPROVE');

-- ------------------------------------------------------------
-- 5. SEC_USER_ROLE — intentionally EMPTY. See "OPERATIONAL PRECONDITIONS" in the header:
--    the assignee must be a user who does not create journal entries, which is an
--    environment-specific decision; assigning 'admin' would defeat RULE-FIN-015.
-- ------------------------------------------------------------
