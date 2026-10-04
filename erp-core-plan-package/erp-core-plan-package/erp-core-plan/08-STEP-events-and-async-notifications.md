# Step 08 — Domain event bus and asynchronous notifications with channel SPI

**Branch:** `step/08-events-notifications`
**Goal:** core modules publish domain events after commit; apps and other modules react without being called directly; notifications are dispatched asynchronously with retries; channels (EMAIL implemented; SMS, PUSH, IN_APP contracts) are pluggable.
**Why:** the audit found no `ApplicationEventPublisher`/`@EventListener`/`@Async` anywhere; `notif` is called synchronously by its users with an in-process retry loop; only EMAIL exists. The "add a new entity that listens to events" extension door from the vision needs a bus.

## Preconditions
- Step 05 merged (tenant context for async threads).

## Design (fixed decisions)
- Package `com.erp.events`:
  ```java
  public abstract class DomainEvent { final UUID id; final Instant occurredAt; final Long tenantId; final String actor; final String realm; }
  public interface DomainEventPublisher { void publish(DomainEvent e); }   // impl wraps ApplicationEventPublisher
  ```
  Listeners use Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async("erpCoreEventExecutor")`. The executor is a `ThreadPoolTaskExecutor` bean with a `TaskDecorator` that **copies `TenantContext` and `SecurityContext`** into the worker thread and clears them after. Events published outside a transaction are delivered immediately (fallback `@EventListener`).
- Core events (minimum set): `UserCreatedEvent`, `UserStatusChangedEvent`, `CustomerRegisteredEvent`, `CustomerVerifiedEvent`, `PasswordResetRequestedEvent`, `TenantCreatedEvent`, `FileDocumentPublishedEvent`, `NotificationRequestedEvent`, `NotificationDispatchedEvent`.
- `notif` dispatch becomes event-driven: callers may still call `NotificationDispatchApi.dispatch(...)` (kept for compatibility) — it now persists a `NOTIF_LOG` row with status `QUEUED` and publishes `NotificationRequestedEvent`; an async listener performs delivery and updates the row (`SENT`/`FAILED`, attempts, last error). Retry: Spring Retry (`spring-retry` + `@Retryable`) replacing the hand-written `RetryPolicy`; 5 attempts, exponential backoff 2s→32s; after exhaustion status `FAILED` and `NotificationFailedEvent`.
- `ChannelProvider` SPI (`com.erp.notif.channel`):
  ```java
  public interface ChannelProvider { String channel(); /* EMAIL|SMS|PUSH|IN_APP */ DeliveryResult send(OutboundMessage m); }
  ```
  `EmailChannelProvider` (existing JavaMail code, `@ConditionalOnBean(JavaMailSender.class)`); `LoggingChannelProvider` registered for any channel with no real provider (`@ConditionalOnMissingBean` per channel) that logs and returns `SKIPPED_NO_PROVIDER`. Apps add `SmsChannelProvider` etc. by defining a bean.
- `IN_APP` gets a real minimal implementation: table `NOTIF_INBOX` (ID, TENANT_ID, RECIPIENT_USER_ID, TITLE_AR/EN, BODY_AR/EN, READ_AT, REFERENCE_TYPE/ID, audit, version) + `GET /api/v1/notif/inbox`, `PATCH /api/v1/notif/inbox/{id}/read` (works for both realms).
- Recipient resolution stays via `RecipientDirectory` (sec user directory) and now also supports customers (realm-aware).
- Scheduling: `@EnableScheduling` is **not** turned on by core; a `NotificationRequeueJob` is provided as a bean the app may schedule (`erp.core.notif.requeue.enabled=false` default) to re-dispatch `QUEUED` rows older than N minutes (crash recovery).

## Tasks
1. Migration `V13__notif_async_inbox.sql`: `NOTIF_LOG` add `ATTEMPTS INT DEFAULT 0`, `NEXT_ATTEMPT_AT`, `LAST_ERROR TEXT`, status values `QUEUED`; create `NOTIF_INBOX`; seed lookup values for new statuses.
2. Implement `com.erp.events` (publisher, base event, executor + decorator, auto-config in `ErpCoreAutoConfiguration` or a new `ErpCoreEventsAutoConfiguration` registered in the imports file).
3. Publish the core events from the relevant services (sec, tenant, file, notif).
4. Refactor `notif`: queue+event dispatch, Spring Retry, `ChannelProvider` SPI, `EmailChannelProvider`, `LoggingChannelProvider`, `InAppChannelProvider` + inbox API, requeue job bean.
5. Delete the old `RetryPolicy` and the synchronous send path (keep the public `NotificationDispatchApi` signature).
6. Templates: no change to the `${var}` engine; add `CUSTOMER_*` templates if not already seeded in step 06.
7. **i18n**: `NOTIF_CHANNEL_UNAVAILABLE`, `INBOX_ITEM_NOT_FOUND`.
8. **Tests**: event delivered after commit only (rollback → no delivery); tenant context present in listener thread; dispatch via API → `QUEUED` → `SENT` with the email provider mocked (`JavaMailSender` mock bean) → `NOTIF_LOG` updated; provider throwing → retries → `FAILED` after 5; `SMS` with no provider → `SKIPPED_NO_PROVIDER` and no exception; inbox read/mark-read for a customer token; requeue job re-dispatches stale `QUEUED` rows.

## Acceptance
- All tests green; `grep -rn "Thread.sleep" erp-core/src/main` is empty (old retry loop removed).
- Calling `NotificationDispatchApi.dispatch` returns in < 50 ms in tests (no synchronous send).
- A test-only listener in `erp-app-reference` receives `UserCreatedEvent` (proves the app-side extension door).

## Commit
`step(08): domain event bus with tenant-propagating async executor; event-driven notifications with Spring Retry, ChannelProvider SPI, in-app inbox`

## Out of scope
Outbox table / message broker (Kafka/RabbitMQ), SMS/PUSH real providers, WebSocket push.
