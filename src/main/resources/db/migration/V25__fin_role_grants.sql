-- ============================================================
-- V25 — Grant SYS_ADMIN the FIN module, screens and actions — except the SoD-reserved one
-- ============================================================
-- Why this migration exists at all: V19 registered MDL's screens and actions and granted them to
-- nobody. "Registration is not a grant" — the observable result on a fresh database was every MDL
-- endpoint answering 403 ACCESS_DENIED to every caller, SYS_ADMIN included, until V21 repaired it.
-- V24 registers FIN; this file lands with it so FIN never spends a release in that state.
--
-- What actually lifts a 403: JwtAuthenticationFilter builds its authorities from
-- MenuService.effectivePermissionCodes(), which reads SEC_ROLE_ACTION_GRANT (joined to
-- action -> screen -> module only to test their active flags). Tier-3 is therefore the row that
-- matters to @PreAuthorize; Tiers 1-2 put FIN into the caller's navigation menu
-- (ScreenRegistryRepository.findEffectiveScreensForUser) and keep the registry self-consistent.
-- All three are seeded so the two views never disagree — same shape as V17 and V21.
--
-- ------------------------------------------------------------
-- The separation-of-duties conflict, and how it is resolved here
-- ------------------------------------------------------------
-- V17's and V21's habit is "grant SYS_ADMIN everything". Applied blindly to FIN that would be a
-- silent, permanent breakage rather than a convenience:
--
--   * SEC-BE.md: "PERM_FIN_PERIODS_CLOSE_APPROVE and PERM_FIN_JOURNAL_ENTRIES_CREATE must never
--     be held by the same role ... FIN's service layer additionally checks at
--     hard-close/year-end-close time that no single *user* holds both, per role union."
--   * srs-fin.md §"Access summary", FIN_PERIODS row: "Close-approve
--     (PERM_FIN_PERIODS_CLOSE_APPROVE — RULE-FIN-015, held by a role distinct from
--     PERM_FIN_JOURNAL_ENTRIES_CREATE)".
--   * RULE-FIN-015 / REQ-FIN-038: "The system shall gate the period-close-approval action behind
--     a permission distinct from the journal-entry-creation permission, enforced by the Security
--     module." AC-FIN-038 spells out only the negative case (a role holding solely entry-creation
--     is denied the approval action).
--   * Runtime: FinSeparationOfDutiesService.resolveFacts() asks SEC for the user sets holding each
--     of the two codes and reports entryCreatePermissionShared = !Collections.disjoint(...);
--     FiscalPeriodDomain.assertCanHardClose throws FIN-403-SOD-VIOLATION when that is true.
--
-- Granting both codes to SYS_ADMIN — the one role the bootstrap admin user (V17 §8/§9) holds —
-- would put the same *user* in both sets. API-FIN-026 (hard-close) and API-FIN-027 (year-end
-- close) would then fail FOREVER with FIN-403-SOD-VIOLATION, and the failure would read as a
-- forbidden response rather than as the misconfiguration it is. That is the worse outcome.
--
-- What the artifacts do NOT say: which role holds the close-approval permission. The SRS requires
-- only that it be "a role distinct from" the entry-creation holder; it names no role code, and no
-- FIN artifact (P1, P3_1, SEC-BE.md, _SECTIONS.md) defines one. Minting a role code here would be
-- inventing governance content, which this repo forbids.
--
-- Decision taken, therefore: grant SYS_ADMIN the FIN module, all 12 screens, and 25 of the 26
-- action rows — every one EXCEPT PERM_FIN_PERIODS_CLOSE_APPROVE, which is left GRANTED TO NOBODY
-- pending a human decision on which role owns period-close approval.
--
-- Consequence, stated plainly so nobody is surprised by it: on a fresh database,
-- PATCH /api/v1/fin/fiscal-periods/{id}/hard-close and POST /api/v1/fin/fiscal-years/{id}/
-- year-end-close are UNUSABLE. They answer 403 ACCESS_DENIED at @PreAuthorize because no
-- principal holds the code. This is deliberate: a deliberately unusable endpoint that fails at
-- the authorization gate is better than one that fails deep inside the close flow with a
-- confusing SoD violation, and it cannot be repaired by accident. Repair is a pure administrative
-- act through SEC's own live role-management surface, no migration needed:
--   1. create a role (e.g. via API-SEC role administration) that holds FIN_PERIODS VIEW +
--      CLOSE_APPROVE and NOT PERM_FIN_JOURNAL_ENTRIES_CREATE;
--   2. assign it to a user who does not also hold SYS_ADMIN (or any role carrying
--      PERM_FIN_JOURNAL_ENTRIES_CREATE) — otherwise resolveFacts() will see the two sets meet and
--      the close will be denied with FIN-403-SOD-VIOLATION, exactly as RULE-FIN-015 intends.
-- The gap is recorded in governance/modules/FIN/execution-state.json.
--
-- Gateway convention ("every non-VIEW permission requires VIEW on the same screen first"): every
-- screen granted below has its own VIEW action row granted in the same statement, FIN_PERIODS
-- included (V24 registers PERM_FIN_PERIODS_VIEW precisely so this holds). No write permission is
-- granted whose screen VIEW is absent.
--
-- Style matches V17/V19/V21: plain INSERTs (run once on a fresh schema, not idempotent),
-- surrogate PKs from the V16 sequences, every FK resolved by natural key, never a hardcoded id.
-- ============================================================

-- ------------------------------------------------------------
-- Tier-1 — module grant (RULE-SEC-001: must precede any screen grant)
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_MODULE_GRANT (ROLE_MODULE_GRANT_PK, ROLE_ID, MODULE_ID, GRANTED_BY, GRANTED_AT)
VALUES (nextval('SEQ_SEC_ROLE_MODULE_GRANT'),
        (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'FIN'),
        'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- Tier-2 — screen grants: all 12 FIN screens (RULE-SEC-002). Selected by their module rather than
--          by listing page codes, so the set stays correct if FIN gains a screen later.
--          FIN_PERIODS is included: the screen grant is navigation/consistency, not the
--          close-approval right — that right is the Tier-3 action row deliberately omitted below.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_SCREEN_GRANT (ROLE_SCREEN_GRANT_PK, ROLE_ID, SCREEN_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_SCREEN_GRANT'),
       (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
       s.SCREEN_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_SCREEN_REG s
JOIN SEC_MODULE_REG m ON m.MODULE_REG_PK = s.MODULE_ID
WHERE m.CODE = 'FIN';

-- ------------------------------------------------------------
-- Tier-3 — action grants: every FIN action row EXCEPT PERM_FIN_PERIODS_CLOSE_APPROVE.
--          25 rows: the 24 remaining registered codes that PermissionConstants declares, plus
--          PERM_FIN_PERIODS_VIEW (the gateway row V24's header explains).
--          RULE-SEC-007 holds on every screen: each granted non-VIEW permission's screen VIEW is
--          granted by this same statement.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'),
       (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
       a.ACTION_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_ACTION_REG a
JOIN SEC_SCREEN_REG s ON s.SCREEN_REG_PK = a.SCREEN_ID
JOIN SEC_MODULE_REG m ON m.MODULE_REG_PK = s.MODULE_ID
WHERE m.CODE = 'FIN'
  AND a.PERMISSION_CODE <> 'PERM_FIN_PERIODS_CLOSE_APPROVE';   -- RULE-FIN-015 — see header
