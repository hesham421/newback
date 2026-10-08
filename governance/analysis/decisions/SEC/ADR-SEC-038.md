# ADR-SEC-038 — Five endpoints the SRS screens demand are declared; role and registry-row deactivation stay DEFERRED
Status      : ACCEPTED (non-breaking)
Stage       : P3.1      Module: SEC        Version: v2
Lane        : analysis · round 1 · claude:opus
traces      : SCR-REQ-SEC-004, SCR-REQ-SEC-005, SCR-REQ-SEC-006, REQ-SEC-004, REQ-SEC-005, REQ-SEC-009, REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, ENT-SEC-001, ENT-SEC-002, ENT-SEC-004, ENT-SEC-013

## Context
`gov.py analyze` → `operation-resolves` (C7.23) reads each SRS screen's `Operations` line and requires every operation
on it to be built by an endpoint naming the screen's entity, or excluded with a stated reason. The v1 plan never ran
this clause: its endpoint blocks carried no entity line (ADR-SEC-036). Run against the v2 SRS, it finds operations no
v1 endpoint declares:
- SCR-REQ-SEC-004 Users — `read`: the user detail and its roles tab, and the list behind the "Pending sign-ups" tab.
- SCR-REQ-SEC-005 Roles & permissions — `read`, `update`, `deactivate` for a role, and the read of the 3-level grant
  tree the editor shows.
- SCR-REQ-SEC-006 Registry — `deactivate` for a registry row. The v1 matrix already marked it "reserved — no v1
  endpoint".

The as-built SEC surface already serves five of these without a contract id: `GET /users/{id}`, `GET /roles/{id}`,
`PUT /roles/{id}`, `GET /roles/{id}/grants` and `POST /signup-requests/search`. They appear in the implementation's
published api-docs under `backend/modules/SEC/api-docs/` with no contract id. Deactivating a role and deactivating a
registry row have no endpoint at all.

## Decision
- The five served operations are declared with contract ids and full blocks: API-SEC-032 read user, API-SEC-033 read
  role, API-SEC-034 update role, API-SEC-035 read a role's grant tree, API-SEC-036 search sign-up requests. Each
  follows the base path, the screen's existing permission (VIEW, or UPDATE for API-SEC-034) and the v1 conventions.
  The implementer reconciles each block with the as-built method rather than writing a second one.
- API-SEC-032 and API-SEC-035 join for parent data: the role's code and names beside an assignment (QR-SEC-050), and
  the registry codes and names beside each grant (QR-SEC-053). Every join stays inside SEC, and none resolves a
  lookup label.
- Deactivating a role (SCR-REQ-SEC-005) and deactivating a registry row (SCR-REQ-SEC-006) are **DEFERRED**. They are v1
  scope that was never built, and neither is part of CS-SEC-001, whose subject is machine callers. `PERM_SEC_ROLES_DELETE`
  and `PERM_SEC_MODULE_REGISTRY_UPDATE` stay registered. The plan states each exclusion on the entity block and in the
  SEC-BE matrix.

## Consequences
- The frontend binds the five endpoints by contract id instead of by path alone.
- Two SRS operations stay unbuilt until a SEC change set takes them up. The screens must not offer those buttons as
  working.
- Non-breaking: no REQ, RULE or DBF changes. Declaring an endpoint that already exists changes no behaviour.

## Note — 2026-10-08 (vendored analysis)
The contract ids this ADR declares — API-SEC-032, API-SEC-033, API-SEC-034, API-SEC-035, API-SEC-036 —
and the change set it refers to (CS-SEC-001, SEC v2, never implemented) exist nowhere in the vendored
analysis: `governance/analysis/modules/SEC/` carries no API-SEC catalogue (it lived in the dropped
P3_1 stage), and CS-SEC-001 is mentioned only in `governance/analysis/platform/project-registry.md`
as unimplemented. The five endpoints themselves are as built and are recorded by path in
`P1/srs-sec.md` → "Implementation Addendum — erp-core 1.2.0" §1 (`GET /api/v1/sec/users/{id}`,
`GET /api/v1/sec/roles/{id}`, `PUT /api/v1/sec/roles/{id}`, `GET /api/v1/sec/roles/{id}/grants`,
`POST /api/v1/sec/signup-requests/search`, each with the permission this ADR names). The two
DEFERRED operations are still unbuilt at erp-core 1.2.0: `PERM_SEC_ROLES_DELETE` is registered and
consumed by no gate, and `PERM_SEC_MODULE_REGISTRY_UPDATE` gates the three registration endpoints
rather than a deactivation (ADR-SEC-067).
