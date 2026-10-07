# ADR-SEC-062 — Revoking a screen's VIEW grant cascades the role's other action grants on that screen
Status      : ACCEPTED (non-breaking)
Stage       : implementation (erp-core 1.3.0)      Module: SEC        Version: erp-core 1.3.0
Lane        : tenant-maturity plan package G
traces      : REQ-SEC-081, AC-SEC-087, RULE-SEC-055, RULE-SEC-007, REQ-SEC-030, RULE-SEC-003, ENT-SEC-009, SCR-REQ-SEC-005

Numbering: the tenant-maturity plan (`docs/plans/tenant-maturity-plan.md` §9) names this decision
ADR-SEC-041. ADR-SEC numbers up to 061 were issued historically (`docs/governance-vendoring-report.md`
Appendix A; 041 was a sign-up index decision), so it takes the next free number, 062
(`docs/DEVIATIONS.md` `[TM-G]`).

## Context
erp-core 1.3.0 adds `DELETE /api/v1/sec/roles/{id}/actions/{actionId}` (REQ-SEC-081). RULE-SEC-007
makes VIEW the screen-level gateway: a role's other action grants on a screen take effect only while
the role also holds VIEW on it (`MenuService.effectiveAuthorityCodes` drops them otherwise), and
`RoleActionGrantDomain.create` refuses to grant a non-VIEW action without VIEW
(`SEC-409-NO-VIEW-GRANT`). Revoking VIEW while other action grants remain therefore needs a rule.
There are three options:
- **Cascade**: delete the role's other action grants on that screen together with VIEW, and say how
  many went in the response.
- **Refuse**: answer 409 while other action grants exist on the screen; the administrator removes
  them first, one call each, and VIEW last.
- **Leave them**: delete VIEW only. The remaining grants stay in the tables with no effect.

## Decision
**Cascade**, with a counted response (`ActionGrantRevokeResponse.revokedActionGrants` = every action
grant the call removed, VIEW included) and one `ACTION_REVOKED` SEC audit entry per removed grant.
The decision is `RoleActionGrantDomain.cascadeOnRevoke(...)` (RULE-SEC-055); the role's screen grant
is kept.

Reasons:
- It matches the module revoke (RULE-SEC-003) and the new screen revoke (RULE-SEC-054): removing a
  gate removes what depends on it, in one transaction, and the response counts the cascade. An
  administrator meets one behaviour at every level of the tree.
- "Leave them" keeps dormant grants that come back silently the day VIEW is granted again — access
  the administrator believes was removed. The grant tree would also show checked actions under an
  unchecked VIEW, a state `create` never allows.
- "Refuse" costs N extra calls for the same end state and puts the ordering burden on every client.
  The frontend can warn before the call (it has the tree) and the response confirms the count after.

## Consequences
- Revoking VIEW is destructive for that screen's action level: re-granting needs VIEW first and then
  each action again. The frontend warns before unchecking VIEW (plan §8 F4).
- The screen grant stays after a VIEW revoke, and the menu is built from screen grants alone
  (`ScreenRegistryRepository.findEffectiveScreensForUser`), so the role's users still see the screen in
  `GET /api/v1/sec/menu` while its endpoints answer 403. To remove the menu entry, revoke the screen
  (`DELETE /roles/{id}/screens/{screenId}`); the frontend (F4) says so when VIEW is unchecked, and administrators
  should prefer the screen revoke when the goal is to take the screen away.
- Non-VIEW revokes remove exactly one row (`revokedActionGrants = 1`).
- A super role (`IS_SUPER`) still holds every authority after the cascade; only its menu changes
  (srs-sec.md 1.3.0 addendum §6).
- Non-breaking: two new endpoints; no existing endpoint, rule, table or error code changes.
