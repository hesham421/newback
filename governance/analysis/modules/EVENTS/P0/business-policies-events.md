## BUSINESS POLICIES — أحداث المجال / Events (EVENTS)
══════════════════════════════════════════════════════════════════
Module   : EVENTS  Source of truth : the code at main @ 19b19a4 (erp-core 1.2.0 behaviour);
           erp-core-plan/08-STEP-events-and-async-notifications.md; docs/steps/08-report.md; docs/DEVIATIONS.md [08], [15]; docs/CONSUMING.md §5
Read by  : P0.5 (every user story cites the policies it serves)
══════════════════════════════════════════════════════════════════

As-built baseline: every policy below is CONFIRMED by the code location in its `Source` line, not by a
dialogue. Java paths are relative to `erp-core/src/main/java/com/erp/`.

CLIENT-SPECIFIC POLICIES

POL-EVENTS-001 — الحدث حقيقة بعد الالتزام / An event is a fact delivered after the commit
  Statement (ar) : يجب على النظام تسليم الحدث المنشور داخل معاملة إلى مستمعيه بعد التزام تلك المعاملة فقط، وألا يسلّمه أبدًا عند التراجع، وأن يسلّم الحدث المنشور خارج أي معاملة فورًا.
  Statement (en) : The system shall deliver an event published inside a transaction to its listeners only after that transaction commits, shall never deliver it on rollback, and shall deliver an event published outside any transaction at once.
  Pattern   : ubiquitous
  Trigger   : Any publish
  Rationale : a listener must never act on a change that was not persisted (the NOTIF row would not exist)
  Source    : events/DomainEventPublisher.java:8-19; notif/service/NotificationDeliveryListener.java:46; docs/steps/08-report.md "Listener pattern"
  Status    : CONFIRMED (as built)

POL-EVENTS-002 — داخل العملية، بلا وسيط، مرة واحدة على الأكثر / In-process, no broker, at-most-once
  Statement (ar) : يجب على النظام نقل الأحداث عبر ناشر أحداث Spring داخل العملية نفسها دون وسيط رسائل أو جدول صندوق صادر؛ الحدث الذي يموت بعده التطبيق قبل تسليمه لا يُعاد.
  Statement (en) : The system shall carry events through Spring's in-process event publisher, with no message broker and no outbox table; an event lost because the process dies after the commit is not replayed.
  Pattern   : ubiquitous
  Trigger   : Any publish
  Rationale : medium complexity (POLICY-CLI-01); durable work (NOTIF delivery) recovers from its own table through the requeue job, not from the bus
  Source    : events/support/SpringDomainEventPublisher.java:24-29; autoconfigure/ErpCoreEventsAutoConfiguration.java:29-31; docs/DEVIATIONS.md [08] "Executor wiring" (no wait on shutdown); ADR-EVENTS-001
  Status    : CONFIRMED (as built)

POL-EVENTS-003 — المستأجر يسافر مع الحدث / The tenant travels with the event
  Statement (ar) : يجب على كل حدث أن يحمل مستأجره وفاعله ونطاقه ملتقطةً عند النشر، ويجب على المستمع غير المتزامن أن يعمل داخل `TenantContext.runAs(event.getTenantId())`؛ قيمة الحدث هي الحجة، لا سياق الخيط.
  Statement (en) : Every event shall carry its tenant, actor and realm captured at publish time, and an asynchronous listener shall do its tenant work inside `TenantContext.runAs(event.getTenantId())`; the event's own value is authoritative, not the worker thread's context.
  Pattern   : ubiquitous
  Trigger   : Publish; asynchronous handling
  Rationale : POL-TENANT-007 — a pooled thread must never act in the wrong tenant; `TenantCreatedEvent` even carries a tenant different from the publisher's
  Source    : events/DomainEvent.java:17-20, :57-70; events/TenantCreatedEvent.java:6-18; notif/service/NotificationDeliveryListener.java:48-53
  Status    : CONFIRMED (as built)

POL-EVENTS-004 — قيم بسيطة فقط، لا أسرار / Plain values only, no secrets
  Statement (ar) : يجب على كل حدث أن يحمل قيمًا بسيطة فقط (معرّفات، رموز، أوقات)، وألا يحمل كيان JPA أو كلمة مرور أو رمزًا خامًا أو أي سر؛ حدث إعادة تعيين كلمة المرور يحمل معرّف صف الرمز وانتهاءه، لا الرمز.
  Statement (en) : Every event shall carry plain values only (ids, codes, instants) and never a JPA entity, a password, a raw token or any secret; the password-reset event carries the token row id and its expiry, never the token.
  Pattern   : unwanted
  Trigger   : Event design
  Rationale : events are handed to arbitrary core and application listeners
  Source    : events/DomainEvent.java:25-26; events/PasswordResetRequestedEvent.java:6-9, :13-15; docs/CONSUMING.md §5
  Status    : CONFIRMED (as built)

POL-EVENTS-005 — المنفّذ ينقل السياق ولا يسرّبه / The executor propagates the context and never leaks it
  Statement (ar) : يجب على المنفّذ `erpCoreEventExecutor` أن ينسخ سياق المستأجر وسياق الأمان من الخيط الناشر إلى خيط العمل عند الإرسال، وأن يعيد حالة خيط العمل السابقة بعد المهمة، حتى لا يحمل خيط مجمّع مستأجرًا أو مستخدمًا من مهمة إلى أخرى.
  Statement (en) : The executor `erpCoreEventExecutor` shall copy the tenant and security context from the submitting thread into the worker thread at submission, and restore the worker's previous state after the task, so that a pooled thread never carries a tenant or a principal from one task into the next.
  Pattern   : ubiquitous
  Trigger   : Every task submitted to the core executor
  Rationale : listeners run on pooled threads; a leaked tenant would be a cross-tenant defect
  Source    : events/support/TenantAndSecurityContextTaskDecorator.java:8-17, :22-41; autoconfigure/ErpCoreEventsAutoConfiguration.java:53
  Status    : CONFIRMED (as built)

POL-EVENTS-006 — المنفّذ لا يحلّ محلّ منفّذ التطبيق / The core executor never replaces the application's own
  Statement (ar) : يجب على النظام إعلان المنفّذ `erpCoreEventExecutor` غير مرشّح افتراضيًا، فلا يصل إليه إلا بالاسم، ولا يحلّ محلّ `applicationTaskExecutor` ولا يستقبل `@Async` غير المؤهَّل.
  Statement (en) : The system shall declare `erpCoreEventExecutor` a non-default candidate, reachable by name only, so that it neither replaces Boot's `applicationTaskExecutor` nor becomes the target of an unqualified `@Async` or `TaskExecutor` injection.
  Pattern   : ubiquitous
  Trigger   : Application start-up
  Rationale : a library must not change the application's own asynchronous behaviour; the core enables `@EnableAsync` but never `@EnableScheduling`
  Source    : autoconfigure/ErpCoreEventsAutoConfiguration.java:24-29, :44-45; events/ErpCoreEvents.java:10-16; `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java` rule 6; ADR-EVENTS-002
  Status    : CONFIRMED (as built)

CUSTOM LOOKUP VALUES
| Lookup key | Added values | Source |
|---|---|---|
None — the module owns no MDL lookup. Its one value set is the realm of the envelope: `STAFF`,
`CUSTOMER`, `SYSTEM` (String constants aliased from `SecurityContextHelper`, events/DomainEvent.java:36-43).

SCOPE EXCEPTIONS
| Excluded / Deferred | Statement | Activation trigger | Source |
|---|---|---|---|
| Broker, outbox, durable / exactly-once delivery, replay | not built | explicit future request | ADR-EVENTS-001; docs/steps/08-report.md |
| Audit listener on `DomainEvent` | not built (audit writes synchronously) | — | docs/DEVIATIONS.md [10] "events for async write" |
| `TenantSuspendedEvent`, `TenantActivatedEvent`, `UserPasswordChangedEvent` | not built in 1.2.0 | tenant-maturity plan C.1, D.3 (1.3.0) | docs/plans/tenant-maturity-plan.md |
| `ScopedValue`-based tenant propagation | spike only | tenant-maturity plan C.6 (ADR-TENANT-004: decision owned by the implementing package; draft in `docs/plans/tenant-maturity-analysis-reference.md`) | docs/plans/tenant-maturity-plan.md |

RESOLVED DECISIONS
| # | Question | Answer | Confirmed by | Sources |
|---|---|---|---|---|
| 1 | Synchronous CU bus or dedicated asynchronous module? | dedicated `com.erp.events`, after-commit listeners, at-most-once, no broker | erp-core plan step 08 (fixed design), as built; CU's analysis recorded the delta | ADR-EVENTS-001; `../../CU/P0/business-policies-cu.md` addendum row 6 |
| 2 | Which executor runs asynchronous listeners? | a named, non-default-candidate executor that propagates tenant and security context | step 08, as built | ADR-EVENTS-002 |
| 3 | What may an event carry and where do the classes live? | plain values, no secrets, tenant captured at publish; every core event in the root package | step 08, as built (docs/DEVIATIONS.md [08]) | ADR-EVENTS-003 |
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 08
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none recorded here yet — the three events of the tenant-maturity plan (C.1, D.3) are documented by the implementing run as each package lands on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
