# Step 15 — Hardening for erp-core 1.1.1 (post-plan) — report

## Summary

This post-plan hardening step was approved by the owner. It is a PATCH release, so the public contract
does not change. It makes three fixes, each with tests. There is no migration, no new `erp.core.*`
property, no version bump and no CHANGELOG edit (those belong to the release commits).

**1. NOTIF delivery robustness**
- **Claim/lease before send.** `NotificationDeliveryProcessor.prepare` reuses `NOTIF_LOG.NEXT_ATTEMPT_AT`:
  - A row whose `NEXT_ATTEMPT_AT` is in the future is claimed, and the attempt skips it.
  - Otherwise the attempt claims the row with now + lease. The lease is
    `erp.core.notif.requeue.stale-after-minutes`, at least 1 minute.
  - A failed attempt with attempts left sets the column to when the worker's next Spring Retry attempt
    is due. That time comes from Spring Retry's own counter.
  - Concurrent claims are serialized by the existing `VERSION`. The loser ends quietly.
  - The requeue job's predicate (older than now − stale-after) therefore never matches a row in flight
    or between retries.
- **Executor queue.** The new internal `NotificationDeliveryTracker` holds the ids this node handed to
  `erpCoreEventExecutor`. `NotificationDeliveryListener` now submits to the executor itself instead of
  using `@Async`. `NotificationRequeueJob` skips rows that are still tracked.
- **Rejection.** A `RejectedExecutionException` (queue full) is caught. The row stays `QUEUED`, untouched
  and untracked, and the requeue job delivers it once it is stale. Before, the exception escaped into the
  dispatching caller's after-commit callback.
- **Attempts bounded.**
  - When the last allowed attempt fails, the row becomes `FAILED` at once.
  - A requeued row that already used every attempt is failed without another send.
  - Before, a requeue restarted Spring Retry's count, so `ATTEMPTS` could reach 2 × max.
- **Docs.** `docs/CONSUMING.md` §7 says to enable the requeue job in production
  (`erp.core.notif.requeue.enabled=true` + `@EnableScheduling`) and explains why. The default stays
  `false`, as the step 08 plan specifies.

**2. Correct error responses**
- An unknown path (`NoResourceFoundException`) answers 404 with the new code `CommonErrorCodes.NOT_FOUND`
  (AR + EN) instead of 500 `INTERNAL_ERROR`:
  - for an authenticated caller;
  - for an anonymous caller under a permitted prefix.
  - An anonymous caller under a protected prefix still gets 401: authentication happens before routing.
- The catch-all handler unwraps a `LocalizedException` from the cause chain. `TENANT_CONTEXT_MISSING`
  wrapped in `CannotCreateTransactionException` now carries its own code. The status stays 500, because
  the code's status is `INTERNAL_ERROR`.
- `PageableBuilder` rejects a page whose first row would lie past `Integer.MAX_VALUE` with 400
  `VALIDATION_ERROR` (`fieldErrors[0].field = page`). It rejects rather than clamps.

**3. Tenant context hardening**
- `TenantHibernateConfiguration` is also a `SmartLifecycle` with phase `DEFAULT_PHASE - 2049`. That is
  one below Boot's `WebServerStartStopLifecycle`. It makes the resolver strict before the web server
  accepts requests:
  - Repository bootstrap runs during singleton creation, so it keeps the `-1` sentinel.
  - The `ContextRefreshedEvent` switch remains as a fallback.
- `JwtAuthenticationFilter`, the first tenant-aware filter of both core chains, handles a tenant already
  on the thread:
  - It logs it and clears it before anything else.
  - A rejected token now clears the tenant instead of restoring the previous one.
  - The previous value is still restored in the `finally`.

## Files changed

- `erp-core/src/main/java/com/erp/notif/service/`
  - `NotificationDeliveryProcessor`: claim, retry time from the worker's round, `Outcome.EXHAUSTED`,
    fails a row with no attempts left.
  - `NotificationDeliveryWorker`: passes the round, treats an optimistic-lock loss on `prepare` as
    "not queued".
  - `NotificationDeliveryListener`: manual submit, tracking, rejection handling.
  - `NotificationRequeueJob`: skips tracked rows; new constructor with the tracker, old one kept.
  - `NotificationDeliveryTracker`: new.
- `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreNotifAutoConfiguration.java`: hands the tracker
  to the requeue job (`ObjectProvider`).
- `erp-core/src/main/java/com/erp/common/`
  - `exception/CommonErrorCodes` (`NOT_FOUND`).
  - `web/GlobalExceptionHandler`: 404 handler, unwrapping.
  - `search/PageableBuilder`: page bound.
- `erp-core/src/main/resources/i18n/messages.properties`, `messages_ar.properties`: `NOT_FOUND`.
- `erp-core/src/main/java/com/erp/tenant/config/TenantHibernateConfiguration.java`: `SmartLifecycle`.
- `erp-core/src/main/java/com/erp/sec/security/JwtAuthenticationFilter.java`: defensive clear.
- Tests:
  - new `notif/service/NotificationDeliveryListenerTest`, `common/search/PageableBuilderTest`,
    `common/web/GlobalExceptionHandlerTest`, `common/web/ErrorResponseHardeningIntegrationTest`,
    `tenant/config/TenantHibernateConfigurationTest`, `tenant/TenantBootstrapWindowIntegrationTest`,
    `sec/security/JwtAuthenticationFilterTenantLeakTest`, `testsupport/WebServerStartTenantProbe`;
  - 5 new tests in `notif/NotificationAsyncDeliveryIntegrationTest`.
- Docs: `docs/CONSUMING.md` (§2 table row, §7 production note), `docs/DEVIATIONS.md` (`[15]` entries),
  this report.

## Decisions & deviations

All decisions are recorded as `[15]` entries in `docs/DEVIATIONS.md`:
- `NEXT_ATTEMPT_AT` is reused as the lease, so there is no migration.
- The lease equals `stale-after-minutes`. A separate lease property would be a MINOR change.
- A crashed attempt is recovered between 1× and 2× `stale-after-minutes` after it started.
- The tracker covers only the local node. Across nodes, the lease and the optimistic lock make a
  duplicate harmless.
- The 404 adds one constant to `CommonErrorCodes` and one wire code. Both replace a 500 response, as the
  hardening brief authorized.
- Every wrapped `LocalizedException` is unwrapped, not only the tenant one.
- An overflowing page is rejected, not clamped.
- The JWT filter restores the previous tenant in its `finally`, so a caller that runs the filter on its
  own thread keeps its context.

Public API check:
`git diff main --stat -- '**/crossmodule/**' '**/notif/channel/**' '**/file/storage/**' 'erp-core/src/main/java/com/erp/report/*.java' '**/com/erp/events/**' '**/tenant/TenantContext.java' '**/TenantProvisioningContributor.java' '**/permission/**' '**/db/migration/**'`
is empty. Under `com.erp.common` the only API addition is the `CommonErrorCodes.NOT_FOUND` constant.

## Verification output

- Targeted run (all new and touched tests, plus `SecLogoutIntegrationTest` and
  `TenantContextIntegrationTest`): 44 tests, 0 failures.
- Regression check: setting the lifecycle phase back above the web server's makes
  `TenantBootstrapWindowIntegrationTest` fail ("a session without a tenant is refused once the web server
  accepts requests"). It was restored afterwards.
- Full `mvn verify` from clean `target/` dirs (JDK 21, embedded PostgreSQL): see the totals below.

```
mvn verify   (clean target/ dirs, JDK 21.0.7, embedded PostgreSQL 16)
erp-core:          Tests run: 388, Failures: 0, Errors: 0, Skipped: 0
erp-app-reference: Tests run: 10,  Failures: 0, Errors: 0, Skipped: 0
JaCoCo (erp-core): All coverage checks have been met — 4759 of 6216 lines = 76.56 % (gate 60 %)
BUILD SUCCESS
```

## Notes

- The first full run found two problems, both fixed before the final run:
  - The auto-configuration unit context (`ErpCoreEventsAndNotifAutoConfigurationTest`) has no component
    scan, so the tracker is now optional (`ObjectProvider`).
  - The first version of the web-server probe reached into `com.erp.tenant.config`, which
    `CrossModuleBoundaryArchTest` forbids. It now checks the behaviour: it opens an `EntityManager` with
    no tenant at web-server start.
- Docker is unavailable on this machine, so the Testcontainers path was not run. CI runs it.
