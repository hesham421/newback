# ADR-NOTIF-002 — Asynchronous at-least-once delivery: NEXT_ATTEMPT_AT claim lease, node-local tracker, requeue job off by default

Module  : NOTIF    Version : erp-core 1.2.0    Stage raised : P3 (implementation — steps 08, 15)
Status  : ACCEPTED (non-breaking)

## Context
Step 02 sent synchronously inside the dispatching transaction: a database connection was held while the
provider talked to SMTP, the caller waited for every retry, and a provider failure surfaced in the caller.
The step-08 plan prescribed event-driven, asynchronous delivery with Spring Retry. Step 15 (1.2.0) then
had to close the gaps that appeared under a crash-recovery job: a duplicate delivery of a row (requeue of
a row still in flight on the same or another node), unbounded `ATTEMPTS` across requeues, and an executor
rejection escaping from the after-commit callback into the dispatcher.

## Decision
- Dispatch persists one `NOTIF_LOG` row per requested channel (`QUEUED`, or `CHANNEL_DISABLED`) and
  publishes `NotificationRequestedEvent`; it never sends. HTTP answers 200 (the shared `Status` taxonomy
  has no 202).
- `NotificationDeliveryListener` reacts after commit (or at once outside a transaction, for the requeue
  job), records the row in the node-local `NotificationDeliveryTracker` and submits the delivery to
  `erpCoreEventExecutor` itself; a rejected submission leaves the row `QUEUED` and untracked.
- `NotificationDeliveryWorker` (`@Retryable`, `erp.core.notif.retry.*`) runs each attempt inside
  `TenantContext.callAs(tenant)` as `prepare` (own transaction) → provider `send` (no transaction) →
  `recordOutcome` (own transaction).
- `prepare` claims the row: `NEXT_ATTEMPT_AT` = now + max(1, `erp.core.notif.requeue.stale-after-minutes`)
  minutes, `ATTEMPTS` + 1, committed before the send. Ownership is the row `VERSION` the claim wrote; a
  row claimed by someone else (future `NEXT_ATTEMPT_AT`, different `VERSION`) is skipped, a concurrent
  claim loses on the optimistic lock, and a row that has already used `max-attempts` is failed without a
  try.
- `NotificationRequeueJob` is a bean only with `erp.core.notif.requeue.enabled=true` (default false) and
  fires only under an application's `@EnableScheduling` (`interval-ms`, default 60000); it re-publishes the
  event for `QUEUED` rows whose `NEXT_ATTEMPT_AT` (or `CREATED_AT`) is older than `stale-after-minutes`
  and that this node is not already delivering.

## Consequences
- Delivery is at-least-once: a send whose outcome could not be recorded is repeated once its lease expires.
  Exactly-once is not offered; a provider that must not duplicate has to be idempotent on
  `notificationLogId`.
- Without the requeue job a row that stalls (JVM stop, executor rejection, unrecorded outcome) stays
  `QUEUED` forever; production must enable the job and scheduling (docs/CONSUMING.md §7).
- `ATTEMPTS` is bounded by `max-attempts` however often a row is requeued; `RETRY_COUNT` = `ATTEMPTS` − 1.
- `VARIABLES_JSON` (possibly single-use link tokens) is at rest while a row is `QUEUED` and is cleared at
  every final status; it is never exposed by the API.
- `stale-after-minutes` doubles as the lease, so it must stay well above the slowest provider call; a
  crashed attempt is picked up between one and two periods after it started.
- Across nodes only the lease and the optimistic lock prevent a double send; the tracker is per JVM.

## Traces
RULE-NOTIF-002, RULE-NOTIF-008, RULE-NOTIF-015, RULE-NOTIF-016, RULE-NOTIF-017, RULE-NOTIF-018,
RULE-NOTIF-020 · srs.md addendum §2, §3 · DBF-0008 (RETRY_COUNT), V13 columns ATTEMPTS / NEXT_ATTEMPT_AT /
LAST_ERROR / VARIABLES_JSON · code: `erp-core/src/main/java/com/erp/notif/service/DispatchService.java:126-162`,
`NotificationDeliveryListener.java:46-70`, `NotificationDeliveryWorker.java:48-104`,
`NotificationDeliveryProcessor.java:103-236`, `NotificationDeliveryTracker.java`, `NotificationRequeueJob.java:73-101`,
`com/erp/autoconfigure/ErpCoreNotifAutoConfiguration.java:41-52`, `ErpCoreProperties.java:307-355` ·
docs/steps/08-report.md, docs/steps/15-report.md · docs/CONSUMING.md §7 · DEVIATIONS [08], [14], [15]
