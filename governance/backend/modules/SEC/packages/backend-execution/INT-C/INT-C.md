<!-- source: PHASE:INT-C -->
<!-- traces: REQ-SEC-016 -->
<!-- PHASE:INT-C:START traces=REQ-SEC-016 -->
## PHASE 5 — INT-C (cross-module consume)

No `XM-*` row exists for SEC (db-script-sec.md §2: "None — SEC is ROOT"). SEC consumes no
other module's entity, table or column and carries no cross-module FK — the XM REGISTER is a
schema-level statement and stays empty. This phase is intentionally near-empty, stated so per
engine §6.2 rather than omitted; no SUB is opened (XM count 0 < the split threshold of 5).

One in-process service call does cross the boundary, and it is sanctioned, not an omission:
`PasswordResetService.dispatchResetNotification` (API-SEC-003, `SUB:SVC-API-INT`) injects NOTIF's
`com.erp.notif.crossmodule.NotificationDispatchApi` and hands it the `DispatchCommand` read-model,
wrapped in `InternalCallerContext` because the caller is pre-authentication. That is REQ-SEC-029 —
srs-sec.md §A8's *External service* table, `SOFT / optional` — and it is not an `XM-*` row because
it registers no entity, table or column for db-script §2 to carry. Verified here against
build-create-service "Cross-Module Calls": only the `crossmodule` package is imported, the
reference is held by the service alone (never a Domain, mapper or controller), the argument is
NOTIF's own record, propagation intent is stated at the call site, and the failure is caught there
and logged — so REQ-SEC-006's generic 200 still answers when Notifications is unavailable.
Propagation is `REQUIRES_NEW`, declared on NOTIF's `dispatchIndependently` entry point: a failure
inside dispatch commits or rolls back on its own and never marks SEC's transaction rollback-only,
so the PasswordResetToken and audit rows still commit — execution-state.json gap #5 closed.

SEC's actual cross-module role runs in the opposite direction: every future module
*consumes* SEC through the plain REST surface documented in Phase 3 (registration:
API-SEC-018/019/020; authorization: the CORE interceptor + API-SEC-027) — that is standard
API consumption by another module's own P3.1, not an `XM-*` row inside SEC's own plan.
<!-- PHASE:INT-C:END -->
