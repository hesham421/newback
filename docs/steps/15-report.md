# Step 15 — Hardening for erp-core 1.2.0 (post-plan) — report

## Summary

This post-plan hardening step was approved by the owner. It ships as **erp-core 1.2.0**, a MINOR
release, because it adds public API:
- the constant `CommonErrorCodes.NOT_FOUND`;
- the public handler method `GlobalExceptionHandler.handleNoResource`.

Both are in `com.erp.common`. Nothing in any `crossmodule` package or SPI changed. There is no migration
and no new `erp.core.*` property. There is no version bump and no CHANGELOG edit; those belong to the
release commits.

**Wire-visible behaviour changes**

| Request | Before | Now |
|---|---|---|
| Unknown path (authenticated, or anonymous under a permitted prefix) | 500 `INTERNAL_ERROR` | 404 `NOT_FOUND` |
| Search with a `page` whose offset overflows `int` (e.g. `2147483647`) | 500 `INTERNAL_ERROR` | 400 `VALIDATION_ERROR`, `fieldErrors[0].field = page` |
| An exception that wraps a `LocalizedException` (e.g. `TENANT_CONTEXT_MISSING` inside `CannotCreateTransactionException`) | 500 `INTERNAL_ERROR` | that exception's own code and status (`TENANT_CONTEXT_MISSING` stays 500) |

An anonymous request for an unknown path under a protected prefix still gets 401, because
authentication runs before routing.

**1. NOTIF delivery robustness**
- **Claim/lease before send.** `prepare` reuses `NOTIF_LOG.NEXT_ATTEMPT_AT`:
  - It claims a `QUEUED` row by setting the column to now + lease. The lease is
    `erp.core.notif.requeue.stale-after-minutes`, at least 1 minute.
  - It skips a row that someone else has claimed.
  - A failed attempt with attempts left sets the column to when the worker's next retry is due.
  - The requeue job (older than now − stale-after) therefore never matches a row in flight or between
    retries.
- **Claim ownership.** A delivery run remembers the row `VERSION` its own last write produced, in the
  Spring Retry context. It treats a claimed row with that `VERSION` as its own:
  - If recording an outcome fails after a send (a database error), the run retries at once
    (at-least-once). It no longer stalls behind its own lease until a requeue.
  - A retry that fires early because the clock moved is not skipped.
  - Any other write changes `VERSION`. A duplicate run never takes over a claimed row.
  - Concurrent claims are serialized by the optimistic lock. The loser ends quietly.
- **Executor queue.** The new internal `NotificationDeliveryTracker` holds the ids this node handed to
  `erpCoreEventExecutor`. The listener now submits to the executor itself instead of using `@Async`.
  The requeue job skips rows that are still tracked.
- **Rejection.** When the executor's queue is full, the rejection is caught. The row stays `QUEUED` and
  untouched, and the requeue job delivers it once it is stale. Before, the rejection escaped into the
  dispatcher.
- **Attempts bounded.**
  - When the last allowed attempt fails, the row becomes `FAILED` at once.
  - A requeued row that already used every attempt is failed without another send.
- **Docs.** `docs/CONSUMING.md` §7 says to enable the requeue job in production and explains why. The
  default stays `false`, as the step 08 plan specifies.

**2. Error responses** (see the table above)
- New `NoResourceFoundException` handler.
- The catch-all handler unwraps a `LocalizedException` from the cause chain.
- `PageableBuilder` rejects an overflowing page rather than clamping it.

**3. Tenant context hardening**
- `TenantHibernateConfiguration` is also a `SmartLifecycle` with phase `DEFAULT_PHASE - 2049`, one below
  Boot's web-server lifecycle. The resolver therefore becomes strict before any request is accepted:
  - Repository bootstrap still uses the `-1` sentinel.
  - The `ContextRefreshedEvent` switch remains as a fallback.
- `JwtAuthenticationFilter` handles a tenant already on the thread when a request arrives:
  - It logs it and clears it.
  - A rejected token clears the tenant.
  - The previous value is restored in the `finally`.

## Files changed

- `erp-core/src/main/java/com/erp/notif/service/`
  - `NotificationDeliveryProcessor`: claim, claim ownership by `VERSION` (`Claim` and `Recorded`
    records), retry time, `Outcome.EXHAUSTED`.
  - `NotificationDeliveryWorker`: owned claim in the retry context, round, optimistic-lock branch.
  - `NotificationDeliveryListener`: manual submit, tracking, rejection.
  - `NotificationRequeueJob`: skips tracked rows; new constructor, old one kept.
  - `NotificationDeliveryTracker`: new.
- `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreNotifAutoConfiguration.java`: tracker
  (`ObjectProvider`).
- `erp-core/src/main/java/com/erp/common/`: `exception/CommonErrorCodes` (`NOT_FOUND`),
  `web/GlobalExceptionHandler`, `search/PageableBuilder`.
- `erp-core/src/main/resources/i18n/messages.properties`, `messages_ar.properties`: `NOT_FOUND`.
- `erp-core/src/main/java/com/erp/tenant/config/TenantHibernateConfiguration.java`,
  `erp-core/src/main/java/com/erp/sec/security/JwtAuthenticationFilter.java`.
- Tests:
  - new `notif/service/NotificationDeliveryListenerTest`, `notif/service/NotificationClaimIntegrationTest`,
    `common/search/PageableBuilderTest`, `common/web/GlobalExceptionHandlerTest`,
    `common/web/ErrorResponseHardeningIntegrationTest`, `tenant/config/TenantHibernateConfigurationTest`,
    `tenant/TenantBootstrapWindowIntegrationTest`, `sec/security/JwtAuthenticationFilterTenantLeakTest`,
    `testsupport/WebServerStartTenantProbe`;
  - 5 new tests in `notif/NotificationAsyncDeliveryIntegrationTest`.
- Docs: `docs/CONSUMING.md`, `docs/DEVIATIONS.md` (`[15]`), this report.

## Decisions & deviations

All are recorded as `[15]` entries in `docs/DEVIATIONS.md`:
- `NEXT_ATTEMPT_AT` is reused as the lease, so there is no migration.
- The lease equals `stale-after-minutes`; there is no new property.
- Claim ownership uses the row `VERSION` the run itself wrote. It does not depend on the clock.
- A crashed attempt is recovered between 1× and 2× `stale-after-minutes` after it started.
- The tracker covers the local node only. Across nodes, the lease and the optimistic lock make a
  duplicate harmless.
- Every wrapped `LocalizedException` is unwrapped, not only the tenant one.
- An overflowing page is rejected, not clamped.
- The JWT filter restores the previous tenant in its `finally`.

Public API check:
`git diff main --stat -- '**/crossmodule/**' '**/notif/channel/**' '**/file/storage/**' 'erp-core/src/main/java/com/erp/report/*.java' '**/com/erp/events/**' '**/tenant/TenantContext.java' '**/TenantProvisioningContributor.java' '**/permission/**' '**/db/migration/**'`
is empty. The only additions are in `com.erp.common` (listed above), hence MINOR.

## Verification output

```
mvn -q verify   (clean target/ dirs, JDK 21.0.7, embedded PostgreSQL 16) — EXIT 0
erp-core:          390 tests, 0 failures, 0 errors, 0 skipped   (surefire reports)
erp-app-reference:  10 tests, 0 failures, 0 errors, 0 skipped
JaCoCo (erp-core): 4781 of 6230 lines = 76.74 % (gate 60 %, met)
```
- NOTIF classes run three times in a row (`NotificationAsyncDeliveryIntegrationTest`,
  `NotificationClaimIntegrationTest`, `NotificationDeliveryListenerTest`,
  `NotificationInboxApiIntegrationTest`): 26 / 26 green each time.

- Regression checks:
  - With the lifecycle phase moved after the web server, `TenantBootstrapWindowIntegrationTest` fails.
  - Without claim ownership (the first hardening round), the recorded-outcome failure leaves the row
    `QUEUED`, which `aFailedOutcomeRecord_afterASuccessfulSend_isRetriedByTheRunItself_andEndsSent`
    rejects.

## Notes

- Review round 1 (coordinator) found that a database error in `recordOutcome` stalled the row behind
  the run's own lease. That is fixed by claim ownership. Round 1 also asked for a test of the two-worker
  race (`twoWorkersPreparingTheSameRow_exactlyOneSends_andTheOtherEndsQuietly`) and for the 1.2.0 wording.
- The first full run of round 0 found two problems, both fixed:
  - The tracker is optional in the auto-configuration unit context.
  - The web-server probe now checks behaviour instead of reaching into `com.erp.tenant.config`
    (`CrossModuleBoundaryArchTest`).
- Docker is unavailable on this machine, so the Testcontainers path was not run. CI runs it.
