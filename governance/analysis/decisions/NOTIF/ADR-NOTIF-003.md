# ADR-NOTIF-003 — Retry back-off ×2 (5 attempts, 2 s → 32 s) instead of ×1.5

Module  : NOTIF    Version : erp-core 1.2.0    Stage raised : P3 (implementation — step 08)
Status  : ACCEPTED (non-breaking)

## Context
POLICY-CLI-03 and RULE-NOTIF-002 fix the retry policy as "up to 5 retries, 2 s initial, 1.5× back-off, then
FAILED", taken from the ARCH-REF reference default and marked "tune at P1". The step-08 plan, which moved
delivery onto Spring Retry, prescribed "5 attempts, exponential backoff 2s→32s" — a doubling curve with a
cap — and the implementation followed the plan. The two curves cannot both be the default.

## Decision
The worker's `@Retryable` back-off is `initial-delay-ms` 2000, `multiplier` 2.0, `max-delay-ms` 32000 with
`max-attempts` 5 (the first attempt included), all four exposed as `erp.core.notif.retry.*` properties and
read by the `@Retryable` placeholders and by `NotificationDeliveryProcessor.backoffAfter`. The default waits
are therefore 2, 4, 8 and 16 s (about 30 s in total before `FAILED`); the 32 s cap only matters when an
application raises `max-attempts`. An application that wants the analysed curve sets
`erp.core.notif.retry.multiplier=1.5`.

## Consequences
- The analysed "5 retries" became "5 attempts in total" (4 retries): `ATTEMPTS` counts every try and
  `RETRY_COUNT` = `ATTEMPTS` − 1, so a row that exhausts its attempts shows `RETRY_COUNT = 4`.
- `NEXT_ATTEMPT_AT` between retries is computed from the same properties, so the requeue job and the
  worker agree on when a retry is due.
- The deviation is recorded in DEVIATIONS [08] and in the business-policies and SRS addenda; the P0/P1
  bodies keep the ×1.5 text as history.

## Traces
POLICY-CLI-03 · RULE-NOTIF-002, RULE-NOTIF-015 · srs.md addendum §2 · business-policies-notif.md addendum
row 3 · DBF-0008 · code: `erp-core/src/main/java/com/erp/notif/service/NotificationDeliveryWorker.java:48-54`,
`NotificationDeliveryProcessor.java:122-124, 238-243`, `com/erp/autoconfigure/ErpCoreProperties.java:315-336` ·
docs/steps/08-report.md · DEVIATIONS [08], [14], [15]
