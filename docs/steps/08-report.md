# Step 08 — Domain event bus and asynchronous notifications with channel SPI — report

## Summary

Core modules now publish domain events, and anything (core or application) can react to them without
being called directly. NOTIF dispatch is event-driven and asynchronous, with Spring Retry and a
pluggable channel SPI.

- **Event bus (`com.erp.events`, added to `CORE_PACKAGE_LIST`).**
  - `DomainEvent` carries `id` (UUID), `occurredAt`, `tenantId`, `actor` and `realm`
    (`STAFF`/`CUSTOMER`/`SYSTEM`). Its no-arg constructor captures them from the publishing thread.
  - `DomainEventPublisher` wraps `ApplicationEventPublisher`; `ErpCoreEvents.EXECUTOR` names the executor.
  - 10 core events: `UserCreatedEvent`, `UserStatusChangedEvent`, `CustomerRegisteredEvent`,
    `CustomerVerifiedEvent`, `PasswordResetRequestedEvent`, `TenantCreatedEvent`,
    `FileDocumentPublishedEvent`, `NotificationRequestedEvent`, `NotificationDispatchedEvent`, and
    `NotificationFailedEvent` (the step names the last one in its retry design).
  - `ErpCoreEventsAutoConfiguration` adds three things:
    - `@EnableAsync`;
    - the `DomainEventPublisher` bean (`@ConditionalOnMissingBean`);
    - `erpCoreEventExecutor`, a `ThreadPoolTaskExecutor` declared `defaultCandidate = false`. It
      leaves Boot's `applicationTaskExecutor` in charge of unqualified `@Async`. Its
      `TenantAndSecurityContextTaskDecorator` copies `TenantContext` and the `SecurityContext` into
      the worker thread and restores the worker's previous state afterwards.
  - Listener pattern: `@Async(ErpCoreEvents.EXECUTOR)` +
    `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`. Delivery happens
    after commit only, never on rollback, and immediately when the event is published outside a
    transaction.
- **Publishers.** Every event is published inside the writing transaction:
  - SEC `UserService.create` and `SignupRequestService.decide` (approve) → `UserCreatedEvent`;
  - SEC `UserService.deactivate`/`reactivate` → `UserStatusChangedEvent`;
  - SEC `PasswordResetService` → `PasswordResetRequestedEvent` (never the raw token);
  - `TenantService.create` → `TenantCreatedEvent` (its tenantId is the new tenant);
  - `FileService.store` → `FileDocumentPublishedEvent` (PRIVATE);
  - NOTIF → requested/dispatched/failed.
- **NOTIF, now event-driven.**
  - `DispatchService` persists one `NOTIF_LOG` per channel. An enabled channel's row is `QUEUED`,
    with its variables in `VARIABLES_JSON`, and gets a `NotificationRequestedEvent`. A disabled
    channel's row is `CHANNEL_DISABLED`. Nothing is sent synchronously.
  - `NotificationDeliveryListener` (async, after commit) calls `NotificationDeliveryWorker.deliver`,
    which carries `@Retryable`: 5 attempts, 2 s doubling, at most 32 s, all `erp.core.notif.retry.*`.
  - Each attempt runs inside `TenantContext.callAs(event tenant)`, wrapped around each of its own
    transactions:
    1. `prepare` (transaction): `ATTEMPTS`+1;
    2. the provider's `send`, with no transaction open;
    3. `recordOutcome` (transaction): `SENT`, `SKIPPED_NO_PROVIDER`, or, on failure, still `QUEUED`
       with `LAST_ERROR` and `NEXT_ATTEMPT_AT`.
  - After the last attempt fails, `@Recover` sets `FAILED` and publishes `NotificationFailedEvent`.
  - Every final status clears `VARIABLES_JSON`.
  - The `NotificationDispatchApi` signature is unchanged.
- **Channel SPI (`com.erp.notif.channel`).** `ChannelProvider { String channel(); DeliveryResult send(OutboundMessage) }`.
  - `EmailChannelProvider` is the old SMTP code. It is registered by `ErpCoreNotifAutoConfiguration`
    only when a `JavaMailSender` bean exists.
  - `InAppChannelProvider` writes `NOTIF_INBOX`.
  - `LoggingChannelProvider` stands in for every channel without a provider bean: it logs and
    returns `SKIPPED_NO_PROVIDER`.
  - `ChannelProviderRegistry` lets an application provider win over the core one.
- **In-app inbox.** `NOTIF_INBOX` (tenant-scoped), `GET /api/v1/notif/inbox?unreadOnly&page&size` and
  `PATCH /api/v1/notif/inbox/{id}/read`.
  - Authenticated callers of any realm see only their own items.
  - The caller's `SEC_USER` id comes from the new `SecUserDirectoryApi.findCurrentUserId()`.
  - Another user's item → 404 `INBOX_ITEM_NOT_FOUND`.
- **Crash recovery.** `NotificationRequeueJob` is registered only with
  `erp.core.notif.requeue.enabled=true`. Its `@Scheduled` trigger fires only if the application
  enables scheduling. It re-dispatches stale `QUEUED` rows tenant by tenant.
- **Removed:** `RetryPolicy`, `DefaultChannelProvider`, `service.ChannelProvider`,
  `ChannelSendResult`, the synchronous send loop, and step 02's `DefaultChannelProviderTest` (its
  class is gone; see the deviations).
- **`V13__notif_async_inbox.sql`** (additive only):
  - `NOTIF_LOG` gets `ATTEMPTS`, `NEXT_ATTEMPT_AT`, `LAST_ERROR` and `VARIABLES_JSON`, plus an index;
  - `NOTIF_INBOX` is created (sequence, FK and index to `CORE_TENANT`, `VERSION`, audit columns);
  - every existing tenant is seeded with `NOTIF_STATUS` `QUEUED`/`SKIPPED_NO_PROVIDER`,
    `NOTIF_CHANNEL` `IN_APP`, and an enabled `IN_APP` channel configuration.

`mvn -q verify` is green:
- erp-core 138 tests: 111 before, −2 for the deleted `DefaultChannelProviderTest`, +29 new;
- erp-app-reference 8 tests: 7 before, +1.

`grep -rn "Thread.sleep" erp-core/src/main` is empty. Dispatch takes a median of 9–11 ms across runs, against
the 50 ms limit.

## Files changed

**Created**
- `erp-core/src/main/resources/db/migration/core/V13__notif_async_inbox.sql`
- `erp-core/src/main/java/com/erp/events/`:
  - `DomainEvent`, `DomainEventPublisher`, `ErpCoreEvents`;
  - `UserCreatedEvent`, `UserStatusChangedEvent`, `CustomerRegisteredEvent`, `CustomerVerifiedEvent`, `PasswordResetRequestedEvent`, `TenantCreatedEvent`, `FileDocumentPublishedEvent`, `NotificationRequestedEvent`, `NotificationDispatchedEvent`, `NotificationFailedEvent`;
  - `support/SpringDomainEventPublisher`, `support/TenantAndSecurityContextTaskDecorator`.
- `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreEventsAutoConfiguration.java`, `ErpCoreNotifAutoConfiguration.java`
- `erp-core/src/main/java/com/erp/notif/channel/`: `ChannelProvider`, `DeliveryResult`, `DeliveryStatus`, `OutboundMessage`, `NotifChannels`, `TemplateText`, `EmailChannelProvider`, `InAppChannelProvider`, `LoggingChannelProvider`
- `erp-core/src/main/java/com/erp/notif/`:
  - `entity/NotificationInboxItem`, `repository/NotificationInboxRepository`, `dto/InboxItemResponse`;
  - `mapper/NotificationInboxMapper`, `domain/NotificationInboxDomain`;
  - `service/NotificationInboxService`, `controller/NotificationInboxController`.
- `erp-core/src/main/java/com/erp/notif/service/`: `ChannelProviderRegistry`, `DispatchVariables`, `DeliveryAttemptFailedException`, `NotificationDeliveryListener`, `NotificationDeliveryWorker`, `NotificationDeliveryProcessor`, `NotificationRequeueJob`
- Tests (erp-core):
  - `events/DomainEventBusIntegrationTest` (3), `events/DomainEventTest` (3), `events/support/TenantAndSecurityContextTaskDecoratorTest` (2);
  - `notif/NotificationAsyncDeliveryIntegrationTest` (7), `notif/NotificationInboxApiIntegrationTest` (3);
  - `notif/channel/EmailChannelProviderTest` (3), `notif/service/ChannelProviderRegistryTest` (2), `notif/domain/NotificationDomainsTest` (3);
  - `autoconfigure/ErpCoreEventsAndNotifAutoConfigurationTest` (3);
  - helpers `notif/NotifTestFixtures`, `testsupport/AbstractAsyncIntegrationTest`, `testsupport/DomainEventProbe`, `testsupport/ProbeEvent`.
- `erp-app-reference/src/test/java/com/erp/app/UserCreatedEventProbe.java`
- `docs/steps/08-report.md`

**Modified**
- `erp-core/pom.xml`: `spring-retry` 2.0.11; the mail comment.
- `autoconfigure/ErpCoreAutoConfiguration.java`: `,com.erp.events` appended to `CORE_PACKAGE_LIST`.
- `autoconfigure/ErpCoreProperties.java`: `events.executor.*` and `notif.retry.*`/`notif.requeue.*`, appended.
- `META-INF/spring/...AutoConfiguration.imports`: 2 lines appended.
- `notif/`:
  - `service/DispatchService` (queue + event);
  - `entity/NotificationLog` (4 fields), `domain/NotificationLogDomain` (states);
  - `repository/NotificationLogRepository` (2 queries);
  - `dto/NotificationLogResponse` + `mapper/NotificationLogMapper` (attempts, nextAttemptAt, lastError);
  - `exception/NotifErrorCodes` (2 codes);
  - `crossmodule/RecipientDirectory` + `SecRecipientDirectory` (`currentRecipientId`);
  - `crossmodule/NotificationDispatchApi` (javadoc only).
- `sec/`:
  - `crossmodule/SecUserDirectoryApi` + `Impl` (`findCurrentUserId`);
  - `service/UserService` (2 events + `findCurrentUserId`);
  - `service/SignupRequestService`, `service/PasswordResetService` (1 event each; javadoc names `EmailChannelProvider`).
- `tenant/service/TenantService` and `file/service/FileService`: 1 event each.
- `resources/i18n/messages.properties`, `messages_ar.properties`: a step-08 block with 2 codes.
- `resources/db/migration/core/README.md`: V13 row.
- Tests:
  - `architecture/CrossModuleBoundaryArchTest`: `events` module added; `notif.channel` made public;
  - `autoconfigure/ErpCoreAutoConfigurationTest`: `com.erp.events` expected;
  - `tenant/TenantSchemaIntegrationTest`: 18 → 19;
  - `test/resources/application-test.properties`: fast retries, requeue bean on.
- `erp-app-reference/src/test/.../ReferenceApplicationSmokeTest.java`: probe import, Flyway `…10, 13, 1000`, 1 new test.
- `erp-app-reference/src/main/resources/application.yml`: comment only.
- `docs/DEVIATIONS.md`: 23 `[08]` entries.

**Deleted**
- `notif/domain/RetryPolicy.java`, `notif/service/ChannelProvider.java`, `notif/service/ChannelSendResult.java`, `notif/service/DefaultChannelProvider.java`
- `erp-core/src/test/java/com/erp/notif/service/DefaultChannelProviderTest.java`

**Untouched:** `erp-core-plan/execution-state.json`, `erp-app-reference/governance/`.

## Decisions & deviations

These mirror the 23 `[08]` entries in `docs/DEVIATIONS.md`:

1. **Spring Retry 2.0.11** with an explicit version, because Boot 4 does not manage it. It works on
   Spring Framework 7. `@EnableRetry` is on `ErpCoreNotifAutoConfiguration`.
2. **Backoff "2s→32s"** = initial 2 s, ×2, cap 32 s, 5 attempts, so the waits are 2/4/8/16 s. It is
   configurable, and tests use 20 ms → 80 ms.
3. **Per-channel `LoggingChannelProvider`** is a registry fallback, not a conditional bean. An
   application provider beats a core one.
4. **`EmailChannelProvider` is registered by an auto-configuration** after Boot's mail configuration.
   - Without a mail sender, EMAIL ends `SKIPPED_NO_PROVIDER`; it used to be `FAILED`/`NO_MAIL_SENDER`.
   - Non-EMAIL channels no longer fake success.
5. **`DefaultChannelProviderTest` was deleted with its class.** Replacement tests cover the new
   behaviour. No other assertion changed.
6. **Extra column `VARIABLES_JSON`.** Asynchronous delivery and requeue need it. It is cleared on
   final statuses and never exposed or logged. There is also an extra index. `RETRY_COUNT` and
   `ERROR_MESSAGE` are kept.
7. **State machine:** `PENDING → QUEUED | CHANNEL_DISABLED`, `QUEUED → SENT | FAILED | SKIPPED_NO_PROVIDER`.
   Lookup values and the `IN_APP` configuration are seeded for every tenant; new tenants copy them
   from PLATFORM.
8. **The provider runs outside any database transaction** (prepare → send → record). Delivery is
   at-least-once.
9. **`NOTIF_INBOX` has no realm column.** Both realms are `SEC_USER` ids.
10. **Inbox API shape:** a GET list with query parameters and an idempotent PATCH read, gated by
    `isAuthenticated()` and limited to the caller's own items. There is no CRUD, usage or search
    DTO, which are named controller/DTO deviations. Paths follow the step (`/api/v1/notif/inbox`).
11. **Caller id via `SecUserDirectoryApi.findCurrentUserId()`.** Step 06 must make it realm-aware.
12. **No customer token exists yet** (step 06), so the inbox is tested with two staff users.
13. **No realm field on `DispatchCommand`.** Keeping the record unchanged keeps step 06's calls
    compiling, and both realms are `SEC_USER` ids. The active-customer rule is step 06's decision.
14. **`CUSTOMER_*` templates are not seeded.** Step 06 seeds them in V11.
15. **All core events live in the `com.erp.events` root package**, which is public in ArchUnit;
    `support` is internal.
    - The two customer events are defined here but published by step 06.
    - `FileDocumentPublishedEvent` fires on upload; step 07 adds `PUBLIC`.
    - `TenantCreatedEvent.tenantId` is the new tenant.
16. **Realm mapping:** `ROLE_CUSTOMER` → `CUSTOMER`, any other authenticated caller → `STAFF`, no
    caller → `SYSTEM`.
17. **Executor `defaultCandidate = false`**, ordered after `TaskExecutionAutoConfiguration`. Core
    enables `@EnableAsync` but not scheduling. The executor does not wait for running tasks on
    shutdown.
18. **Requeue job:** opt-in bean with `@Scheduled`. A cross-tenant `DISTINCT TENANT_ID` JDBC read
    finds the tenants, then each tenant runs `callAs` with the tenant-filtered query.
19. **Delivery infrastructure is `@Component` without `@PreAuthorize`.** The internal retry signal
    `DeliveryAttemptFailedException` carries no error code.
20. **Error codes:** `NOTIF_CHANNEL_UNAVAILABLE` (403 on the inbox, and the stored skip reason) and
    `INBOX_ITEM_NOT_FOUND` (404).
21. **`setFrom` only with a non-blank `spring.mail.username`.** An empty From was rejected as an
    illegal address — a latent bug in the moved code.
22. **Under 50 ms** = the median of 7 timed dispatches after 3 warm-ups, with the mocked send
    blocking.
23. **Shared assertions updated:** schema counts 18 → 19, `CORE_PACKAGES` plus `com.erp.events` (the
    test-method name is kept), smoke-test Flyway list `2..10, 13, 1000`.

## Acceptance checklist

3/3 ✅

- ✅ **All tests green; `grep -rn "Thread.sleep" erp-core/src/main` is empty (old retry loop removed).**
  - `mvn -q verify` EXIT=0: erp-core 138/0/0/0, erp-app-reference 8/0/0/0.
  - The grep prints nothing (exit code 1).
  - `RetryPolicy.java` is deleted, and retries are Spring Retry's `@Retryable` on `NotificationDeliveryWorker.deliver`.
- ✅ **Calling `NotificationDispatchApi.dispatch` returns in < 50 ms in tests (no synchronous send).**
  - `NotificationAsyncDeliveryIntegrationTest.dispatch_returnsInUnder50ms_withoutWaitingForTheSend`:
    the mocked `JavaMailSender.send` blocks until the end of the test, and every row is still
    `QUEUED` right after dispatch.
  - Sorted durations in the final run: `[8, 8, 9, 9, 11, 28, 34]` ms, median 9 ms.
  - Earlier runs: `[7, 8, 8, 9, 13, 13, 19]` and `[8, 10, 10, 11, 12, 12, 12]`.
  - Every row then reaches `SENT` once the send is released.
- ✅ **A test-only listener in `erp-app-reference` receives `UserCreatedEvent` (the app-side extension door).**
  - `ReferenceApplicationSmokeTest.anApplicationListener_receivesTheCoreUserCreatedEvent`.
  - The bootstrap admin creates a user over HTTP. The application's `UserCreatedEventProbe` bean
    (`@Async(ErpCoreEvents.EXECUTOR)` + `@TransactionalEventListener(AFTER_COMMIT)`) receives the
    event with userId, username, tenant 1 and actor `admin`, on an `erp-event-*` thread running as
    tenant 1.

The step's task-8 test list:
- **After-commit only, nothing on rollback:** `DomainEventBusIntegrationTest.eventPublishedInATransaction_isDeliveredOnlyAfterCommit_andNeverOnRollback`.
- **Tenant context in the listener thread:**
  - `...eventPublishedOutsideATransaction_isDeliveredImmediately_withTheTenantAndPrincipalOfThePublisher` (tenant 777 and the principal on the worker);
  - `NotificationAsyncDeliveryIntegrationTest.theWorkerRunsAsTheDispatchingTenant` (a provisioned tenant B; the row and event carry B; the worker thread is B);
  - no leak across pooled threads: `TenantAndSecurityContextTaskDecoratorTest`.
- **`QUEUED` → `SENT`** with a mock `JavaMailSender` and the log updated (attempts 1, `sent_at`, variables cleared, `NotificationDispatchedEvent`): `emailDispatch_isQueuedThenSentAsynchronously_andLogUpdated`.
- **A throwing provider is retried, then `FAILED` after 5:** attempts 5, retry_count 4, last_error and error_message set, 5 `send` calls, `NotificationFailedEvent`. Test: `providerThrowing_isRetriedFiveTimes_thenFailed_withNotificationFailedEvent`.
- **SMS with no provider → `SKIPPED_NO_PROVIDER`**, no exception, mail sender untouched: `smsWithoutAProvider_endsSkippedNoProvider_withoutAnException`.
- **Inbox read and mark-read:** `NotificationInboxApiIntegrationTest`, 3 tests, using staff tokens (see deviation 12).
- **The requeue job re-dispatches stale `QUEUED` rows only:** `requeueJob_redispatchesStaleQueuedRows_only`.

## Verification output

```
$ java -version → openjdk version "21.0.7" 2025-04-15
$ rm -rf target erp-core/target erp-app-reference/target; mvn -o -q verify
EXIT=0 secs=168
[TestPostgres] integration-test database backend: EMBEDDED      (once per module JVM)
[step08] NotificationDispatchApi.dispatch durations (ms, sorted): [8, 8, 9, 9, 11, 28, 34]

surefire totals (target/surefire-reports/TEST-*.xml)                tests fail err skip
== erp-core
com.erp.architecture.CrossModuleBoundaryArchTest                    2 0 0 0
com.erp.autoconfigure.DownloadTokenStoreAutoConfigurationTest       2 0 0 0
com.erp.autoconfigure.ErpCoreAutoConfigurationTest                  9 0 0 0
com.erp.autoconfigure.ErpCoreEventsAndNotifAutoConfigurationTest    3 0 0 0
com.erp.autoconfigure.ErpCoreFlywayAutoConfigurationTest            3 0 0 0
com.erp.autoconfigure.MigrationNamingTest                           2 0 0 0
com.erp.events.DomainEventBusIntegrationTest                        3 0 0 0
com.erp.events.DomainEventTest                                      3 0 0 0
com.erp.events.support.TenantAndSecurityContextTaskDecoratorTest    2 0 0 0
com.erp.file.service.InMemoryDownloadTokenStoreTest                 3 0 0 0
com.erp.notif.NotificationAsyncDeliveryIntegrationTest              7 0 0 0
com.erp.notif.NotificationInboxApiIntegrationTest                   3 0 0 0
com.erp.notif.channel.EmailChannelProviderTest                      3 0 0 0
com.erp.notif.domain.NotificationDomainsTest                        3 0 0 0
com.erp.notif.service.ChannelProviderRegistryTest                   2 0 0 0
com.erp.sec.BootstrapAdminPasswordIntegrationTest                   4 0 0 0
com.erp.sec.MenuServiceGatewayIntegrationTest                       2 0 0 0
com.erp.sec.SecCoverageIntegrationTest                              6 0 0 0
com.erp.sec.SecFrontendGapIntegrationTest                           5 0 0 0
com.erp.sec.SecLogoutIntegrationTest                                3 0 0 0
com.erp.sec.SecReadOneIntegrationTest                               6 0 0 0
com.erp.sec.SecSearchFilterIntegrationTest                         10 0 0 0
com.erp.sec.TenantScopedQueryIntegrationTest                        3 0 0 0
com.erp.sec.UserRolesInResponseIntegrationTest                      8 0 0 0
com.erp.tenant.PlatformTenantApiIntegrationTest                     8 0 0 0
com.erp.tenant.TenantContextIntegrationTest                         1 0 0 0
com.erp.tenant.TenantContextTest                                    4 0 0 0
com.erp.tenant.TenantIsolationIntegrationTest                       6 0 0 0
com.erp.tenant.TenantSchemaIntegrationTest                          4 0 0 0
com.erp.tenant.domain.TenantDomainTest                             16 0 0 0
com.erp.testsupport.TestProfileWiringIntegrationTest                2 0 0 0
erp-core tests=138 failures=0 errors=0 skipped=0
== erp-app-reference
com.erp.app.ReferenceApplicationSmokeTest                           8 0 0 0
erp-app-reference tests=8 failures=0 errors=0 skipped=0

$ grep -rn "Thread.sleep" erp-core/src/main ; echo "grep-exit=$?"
grep-exit=1                                     (no match)

Failed attempts on the way (fixed):
- ErpCoreEventsAndNotifAutoConfigurationTest: context runner lacked the two required secrets → added.
- EMAIL never SENT ("Illegal address"): the moved mail code called setFrom("") with the test profile's
  blank spring.mail.username → setFrom only when non-blank (deviation 21).
- requeue test found no stale row: the test inserted CREATED_AT in local time while Hibernate stores
  Instant as UTC wall-clock → test inserts timezone('UTC', now()).
- dispatch timing [9, 9, 40, 49, 50] ms while providers ran inside the attempt transaction (blocked
  sends held pooled connections) → provider moved outside any transaction (deviation 8); since then
  medians 9–11 ms.
- full verify #1: BootstrapAdminPasswordIntegrationTest counted 'admin' accounts across all tenants
  and saw the one my notif test provisioned (notif tests now run before sec) → the notif fixture names
  its tenant administrator 'ntf-admin' (existing assertion untouched).
```

## Skills checked

- **`build-create-entity`: `NotificationInboxItem` is compliant except named items.**
  - Compliant: `AuditableEntity` (tenant), `@SuperBuilder`, `SEQUENCE` `SEQ_NOTIF_INBOX` with `allocationSize = 1`, indexes in `@Table` named `IDX_<TABLE>_*`, no normalization needed.
  - PK `ID`: README §4 overrides the skill for new tables.
  - A.1.18 deviation: no active flag, so no `activate()`/`deactivate()`; the pure mutation is `markRead`.
  - `NotificationInboxDomain` is a plain class built with `from`, throws `LocalizedException` and has no repository.
  - `NotificationLog` gained four fields; `NotificationLogDomain` keeps the transition guard.
- **`build-create-repository`: compliant.** `@Repository`, `JpaRepository` + `JpaSpecificationExecutor`, every method has a caller (an unused count method was removed before the final run), and the log queries use `JOIN FETCH`.
- **`build-create-dto`:** `InboxItemResponse` is compliant: `@Data @Builder`, bilingual `@Schema` with examples, audit fields with UTC `@JsonFormat`. Named deviation: no Create/Update/Search/Usage DTOs (items are written by the provider only; the list is `GET` with parameters).
- **`build-create-mapper`: compliant.** Manual and null-safe, maps audit fields, read-only surface; there is no `toUsageResponse` (A.4.7 n/a).
- **`build-create-service`:** `NotificationInboxService` is compliant.
  - `@Service @RequiredArgsConstructor @Slf4j`.
  - `@PreAuthorize("isAuthenticated()")` on both methods, the same RULE-NOTIF-005 precedent as dispatch, because there is no screen.
  - `@Transactional`/`readOnly`, `ServiceResult`, `PageableBuilder` with `ALLOWED_SORT_FIELDS`, rules delegated to `NotificationInboxDomain`, `log.info` for writes and `log.debug` for reads.
  - Cross-module access only through `RecipientDirectory` → `SecUserDirectoryApi` (crossmodule).
  - Events are published through `DomainEventPublisher`, which wraps `ApplicationEventPublisher` as the step file prescribes.
  - Named deviation: the delivery infrastructure `@Component`s carry no `@PreAuthorize`.
- **`build-create-controller`:** thin, `OperationCode.craftResponse`, `@Operation` on both endpoints, bilingual `@Tag`. Named deviations A.6.5–A.6.8: no delete, toggle, usage or `POST /search`.
- **`gov-enforce-error-handling`: compliant, 23/23 for the new code.**
  - Every throw is a `LocalizedException` with a registered code (`NotifErrorCodes`, private throwing constructor), present in both bundles.
  - Not-found → `NOT_FOUND`, an illegal transition → `BUSINESS_RULE_VIOLATION`, no user account → `FORBIDDEN`.
  - `DeliveryAttemptFailedException` is an internal retry signal that never reaches a client (named).
  - Provider failures are returned values, not swallowed: they are recorded in `LAST_ERROR`.
- **`gov-enforce-caching-rules`: compliant.** No cache annotations were added; NOTIF is not on the register.
- **`gov-enforce-backend-contract`, inbox feature: 76/85.**
  - Fails, all named: A.1.18, A.3.9, A.3.12, A.4.7, A.5.2 (literal `isAuthenticated()`), and A.6.5–A.6.8.
  - CU.1–CU.8 pass.
- **`gov-validate-backend-feature`, inbox feature:**
  - Stage 0: 7/9 (DTO set; four permissions).
  - Stage 1: 10/15 (Create/Update/Search/Usage DTOs, permission constants).
  - Stage 2: 76/85.
  - Stage 3: 32/37. Lost: the four-permission security checks, and eventing — the bus is a step-mandated `DomainEventPublisher`, and its events are defined in the shared `com.erp.events` package rather than the listening module.
  - Stage 4: 2/2.
  - Score **127/148 (85.8%)**, CONDITIONAL by the raw threshold. Every lost point is a shape the step file prescribes: an authenticated-only inbox for both realms, a provider-written read model, and the `DomainEventPublisher`/`com.erp.events` contract. No automatic-rejection trigger applies once those step-mandated designs are taken as named deviations.

## Notes for later steps

- **Event bus contract (step 10 audit, apps).**
  - Publish with `DomainEventPublisher.publish(new XxxEvent(...))` from the service, inside its
    transaction, after the write.
  - Subclass `DomainEvent`; the no-arg super constructor captures tenant, actor and realm. Carry
    plain values only: no entities, no secrets.
  - Listen with `@Async(ErpCoreEvents.EXECUTOR)` plus
    `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`.
  - On the worker thread, do tenant work inside `TenantContext.runAs(event.getTenantId(), ...)`,
    around a `@Transactional` call and never inside an open transaction. The executor also
    propagates the publisher's tenant and security context and clears them afterwards.
  - A listener for all events listens to `DomainEvent` itself; step 10's audit recorder can do this.
  - `getId()` is the idempotency key.
  - Synchronous listeners still work with a plain `@EventListener` or a `@TransactionalEventListener`
    without `@Async`.
- **Core event inventory.** Who publishes each event:
  - `UserCreatedEvent`: SEC user create and sign-up approval;
  - `UserStatusChangedEvent`: deactivate/reactivate;
  - `PasswordResetRequestedEvent`: reset issuance;
  - `TenantCreatedEvent`: tenant create; its tenantId is the new tenant;
  - `FileDocumentPublishedEvent`: upload, `PRIVATE`;
  - `NotificationRequestedEvent` / `NotificationDispatchedEvent` / `NotificationFailedEvent`: NOTIF.
- **Events to publish when steps 06 and 07 merge:**
  - `CustomerRegisteredEvent` / `CustomerVerifiedEvent` are defined but not published. Step 06
    publishes them, using the `(tenantId, actor, ...)` constructors on public endpoints.
  - Step 07 should publish `FileDocumentPublishedEvent(..., VISIBILITY_PUBLIC)` when a document is
    made public.
- **Merge with step 06 (realms):**
  - make `UserService.findCurrentUserId()` realm-aware, since usernames become unique per tenant and
    realm;
  - decide whether `SecRecipientDirectory.isActive` treats `PENDING_VERIFICATION` customers as active
    for the verification mail (today they would be skipped as inactive);
  - add a customer-token inbox test;
  - `DomainEvent` derives `CUSTOMER` from the `ROLE_CUSTOMER` authority;
  - update `PlatformTenantApiIntegrationTest`'s template count for V11's templates. That is step 06's
    own concern, but note that V13 seeds no templates.
- **Channel SPI.** An application adds SMS/PUSH (or replaces EMAIL/IN_APP) by defining a
  `ChannelProvider` bean.
  - `send` runs on the event executor, with the message's tenant set and no transaction open.
  - Return `DeliveryResult.failed(..)` (or throw) to retry, and `sent()` when delivered.
  - An application provider always wins over the core one for its channel.
- **NOTIF statuses:** `QUEUED`, `SENT`, `FAILED`, `SKIPPED_NO_PROVIDER`, `CHANNEL_DISABLED`.
  - Dispatch never sends synchronously.
  - A caller that needs the outcome listens to `NotificationDispatchedEvent` / `NotificationFailedEvent`
    or reads `NOTIF_LOG` through `NotificationLogQueryApi`.
- **Configuration:**
  - `erp.core.notif.retry.{max-attempts,initial-delay-ms,multiplier,max-delay-ms}`;
  - `erp.core.notif.requeue.{enabled,stale-after-minutes,interval-ms}` (schedule it with
    `@EnableScheduling` in the application);
  - `erp.core.events.executor.{core-pool-size,max-pool-size,queue-capacity,thread-name-prefix}`.
- **Tests:**
  - Extend `com.erp.testsupport.AbstractAsyncIntegrationTest` for asynchronous tests. It provides a
    Mockito `JavaMailSender`, the `DomainEventProbe` listener, executor draining after each test,
    and the Awaitility timeout `ASYNC_TIMEOUT`.
  - Await database state; never sleep.
  - Raw-JDBC timestamps must be UTC wall-clock (`timezone('UTC', now())`), because Hibernate binds
    `Instant` in UTC.
  - Tenant fixtures must not create accounts named `admin`: `BootstrapAdminPasswordIntegrationTest`
    counts `admin` accounts across tenants.
- **Shared files touched**, for merge planning:
  - `ErpCoreAutoConfiguration.CORE_PACKAGE_LIST` (`,com.erp.events` appended);
  - `ErpCoreProperties` (two groups appended);
  - the `AutoConfiguration.imports` file (2 lines appended);
  - the i18n bundles (a step-08 block at the end);
  - `docs/DEVIATIONS.md`;
  - `CrossModuleBoundaryArchTest` (`notif` line changed, `events` module appended);
  - `TenantSchemaIntegrationTest` (18 → 19);
  - `ErpCoreAutoConfigurationTest` (`CORE_PACKAGES` list);
  - `MigrationNamingTest` is unchanged (`events` already listed);
  - `ReferenceApplicationSmokeTest` (Flyway list, probe import);
  - `db/migration/core/README.md` (V13 row);
  - `application-test.properties` (a NOTIF block);
  - SEC `UserService`/`SignupRequestService`/`PasswordResetService`/`SecUserDirectoryApi(+Impl)`,
    `TenantService` and `FileService` (one injected field plus one publish line each).
