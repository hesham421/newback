# ADR-NOTIF-001 — Inbound integration is the in-process NotificationDispatchApi (dispatch / dispatchIndependently), not a CU event listener

Module  : NOTIF    Version : erp-core 1.2.0    Stage raised : P3 (implementation — steps 02, 06, 08)
Status  : ACCEPTED (non-breaking)

## Context
The P1 SRS (MODULE-LEVEL FUNCTIONAL APIs note; Registry Update "+ NotificationEvent listener") and the P0
module registry (DEPENDENCIES: "Common Utils — Events (NotificationEvent)") planned NOTIF to be triggered by
an in-process `NotificationEvent` raised through CU Events by the sending module. Two things stood against
it when the module was built: the service skill forbids a module to listen to another module's internal
events, and the ArchUnit suite (`CrossModuleBoundaryArchTest`) allows cross-module calls only through each
module's `crossmodule` package. SEC also needed its password-reset mail to be sent from inside a transaction
that must commit even when the dispatch fails (API-SEC-003), and its customer flows (step 06) dispatch to a
user account that the calling transaction has not committed yet.

## Decision
NOTIF exposes one inbound surface, `com.erp.notif.crossmodule.NotificationDispatchApi`, taking the narrow
read-model `DispatchCommand(recipientId, templateCode, channelHint, moduleCode, referenceId, referenceType,
variables)` and returning the created `NOTIF_LOG` ids:
- `dispatch(command)` runs in the caller's transaction (`DispatchService.dispatch`);
- `dispatchIndependently(command)` resolves the recipient (RULE-NOTIF-007) in the caller's transaction and
  then runs `DispatchService.dispatchSystem` with `REQUIRES_NEW`, so a failure never marks the caller's
  transaction rollback-only and the queued rows commit — and publish their `NotificationRequestedEvent` —
  on their own.
Both delegates are gated by `isAuthenticated()` (RULE-NOTIF-005); a principal-less in-process caller wraps the
call in SEC's `InternalCallerContext`. No listener for a foreign event exists in NOTIF; its only listener
handles its own `NotificationRequestedEvent`. The planned `NotificationEvent` relay is REMOVED from the
analysis.

## Consequences
- A module that wants a notification calls the API: SEC does so in `PasswordResetService` (line 226) and
  `CustomerAccountService` (line 309), both through `dispatchIndependently`.
- The events NOTIF publishes (`NotificationRequestedEvent`, `NotificationDispatchedEvent`,
  `NotificationFailedEvent`) are outbound only; consumers read outcomes from them or from
  `NotificationLogQueryApi`.
- The generic "any module raises an event, NOTIF reacts" coupling is gone; a new dispatching module takes a
  compile-time dependency on `com.erp.notif.crossmodule`, which the ArchUnit suite permits.
- The 1.3.0 plan (D.3) adds a NOTIF listener for SEC's public `UserPasswordChangedEvent` on the shared
  `com.erp.events` bus. That is a subscription to a published domain event, not to a module-internal one,
  and does not reopen this decision; it is recorded in the 1.3.0 addenda.

## Traces
RULE-NOTIF-001, RULE-NOTIF-005, RULE-NOTIF-007, RULE-NOTIF-019 · API-NOTIF-001 · srs.md addendum §1 (REMOVED
row), §7 · module-registry-notif.md addendum DEPENDENCIES / EXPOSED SURFACE · code:
`erp-core/src/main/java/com/erp/notif/crossmodule/NotificationDispatchApi.java:5-34`,
`NotificationDispatchApiImpl.java:23-36`, `service/DispatchService.java:77-102`,
`com/erp/sec/service/PasswordResetService.java:226`, `com/erp/sec/service/CustomerAccountService.java:309` ·
docs/steps/08-report.md · DEVIATIONS [08], [14], [15]
