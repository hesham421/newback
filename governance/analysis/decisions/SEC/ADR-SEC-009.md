# ADR-SEC-009 — three published endpoints are bound but deliberately not called by this frontend

Module  : SEC     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
The api-docs publish three registration endpoints:

| API id | Endpoint | Permission |
|---|---|---|
| API-SEC-018 | POST `/api/v1/sec/registry/modules` | PERM_SEC_MODULE_REGISTRY_UPDATE |
| API-SEC-019 | POST `/api/v1/sec/registry/screens` | PERM_SEC_MODULE_REGISTRY_UPDATE |
| API-SEC-020 | POST `/api/v1/sec/registry/actions` | PERM_SEC_MODULE_REGISTRY_UPDATE |

SRS `SCR-REQ-SEC-006` B3 is explicit about who calls them: "registration itself happens via the
registering module's own onboarding call (REQ-SEC-016/017/019); this screen's own edit surface
is limited to deactivating a stale row" — and that deactivation has no endpoint at all
(ADR-SEC-008). The caller is therefore a consuming module's own integration path, not SEC's
administrative UI. The ALIGN-FE rule "every documented endpoint the module uses has an RF2
block" leaves an endpoint the module does not use needing an explicit account.

## Decision
All three are bound and blocked out in F2 of `SUB:F2-SCR-SEC-006`, each marked **"mutation —
not called by this frontend"**, carrying its request shape, response shape and error codes. No
affordance, route or facade operation invokes them. `SCR-SEC-006` stays read-only.

## Consequences
- The registry screen shows what has been registered and never registers anything, matching the
  SRS exactly rather than adding an operator-driven registration the SRS does not ask for.
- The blocks are on record for the integrator reading this plan — the shapes their own
  onboarding call must send — so nothing is re-derived per consuming module. `RULE-SEC-004`'s
  contract (message and catalog code) travels with `API-SEC-019` in that block, ready for the
  screen that will enforce it.
- The ALIGN-FE and registry API-coverage rows carry these three as ✗ with this ADR, so the
  27-of-27 binding and the 24-of-27 call count are both visible and neither is a silent gap.
- If a later version adds an operator-driven registration screen, these blocks already hold a
  reconciled contract; only an affordance and a facade operation are added.

## Traces
REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019 · AC-SEC-016, AC-SEC-017, AC-SEC-018,
AC-SEC-019 · RULE-SEC-004 · API-SEC-018, API-SEC-019, API-SEC-020 · SCR-SEC-006
