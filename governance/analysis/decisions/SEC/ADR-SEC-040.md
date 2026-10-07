# ADR-SEC-040 — SEC's in-process cross-module read surfaces get API ids
Status      : ACCEPTED (non-breaking)
Stage       : P3.1      Module: SEC        Version: v2
Lane        : operator (human decision at the P3.1 orphan findings)
Decided     : 2026-09-23
traces      : REQ-SEC-034, REQ-SEC-035, REQ-SEC-016, REQ-SEC-017, QR-SEC-039, QR-SEC-035, QR-SEC-040, ENT-SEC-001, ENT-SEC-004

## Decision
The two in-process read surfaces in `com.erp.sec.crossmodule` are contract surfaces like any other and
are given `API` ids:

- **API-SEC-037** — `SecUserDirectoryApi.findContact` (REQ-SEC-034, QR-SEC-040);
- **API-SEC-038** — `SecUserDirectoryApi.findUserIdsHoldingPermission` (REQ-SEC-035, QR-SEC-039);
- **API-SEC-039** — `SecModuleRegistryApi` (REQ-SEC-016, REQ-SEC-017, QR-SEC-035).

Each block states `verb: — (no HTTP surface)` and an in-process `Endpoint`. Nothing about the code
changes: these are the operations SEC already publishes.

This supersedes the "not endpoints, so not ids" reading of ADR-SEC-039. That ADR's finding stands — the
contracts have no atom kind for an in-process operation — and the answer taken here is that `API` already
is that kind. An `API` is the module's published contract surface; it was never defined as "an HTTP route".

## Why
ADR-SEC-039 recorded three findings and left them open: C7.6 wanted REQ-SEC-034 and REQ-SEC-035 covered
by an API or a DBF, C7.22 wanted QR-SEC-039 cited by an API. Recording them keeps the module blocked at
every gate, for a surface that genuinely exists and genuinely is a contract another module depends on.

The alternative readings were weighed at the human stop and not taken: retiring REQ-SEC-035 and QR-SEC-039
(the read has had no caller since FIN retired its separation-of-duties reader on 2026-09-12) would make
the change set BREAKING and drop a v1 commitment; leaving them as a platform finding blocks B1 on a
contract change.

Giving them ids also fixes something ADR-SEC-039 only noted: MDL's plan observed that SEC's artifacts
never wrote `SecModuleRegistryApi` down at all. API-SEC-039 is that correction.

## Consequence
- Three API ids added, no endpoint, no route, no code change. The change set stays ADDITIVE.
- A future in-process surface is checkable by the same clauses as every HTTP one, instead of needing
  another ADR to explain why it is exempt.
- API-SEC-038 is kept although it has no caller today, because REQ-SEC-035 states it. If that
  requirement is retired later, the id retires with it in that change set.

Source : ADR-SEC-039; the P3.1 orphan findings C7.6 and C7.22; human decision 2026-09-23
