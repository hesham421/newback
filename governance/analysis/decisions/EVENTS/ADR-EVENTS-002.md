# ADR-EVENTS-002 — Tenant- and security-propagating executor declared `defaultCandidate = false`

Module  : EVENTS     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (Requirements) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
An asynchronous listener runs on a pooled thread that knows nothing of the request that published the
event. Two things must reach that thread for a core listener to work: the tenant (`TenantContext` is a
`ThreadLocal`; a session opened without one fails fast with `TENANT_CONTEXT_MISSING`,
`erp-core/src/main/java/com/erp/tenant/TenantContext.java:47-53`) and the principal (the audit columns
and the audit log read `SecurityContextHelper`). At the same time erp-core is a library inside a Spring
Boot application that may have its own `TaskExecutor`, its own `@Async` methods and its own scheduling
(`docs/steps/08-report.md` deviation 17; `docs/DEVIATIONS.md` [08] "Executor wiring"). The
alternatives:
- **Use Boot's `applicationTaskExecutor`** and decorate it — would change the application's own
  asynchronous behaviour and race with an application that defines its own executor.
- **Register a core executor as a normal bean** — it would become a second `TaskExecutor` candidate
  and could be picked by an unqualified `@Async` or `TaskExecutor` injection of the application, or
  make Boot back off from creating `applicationTaskExecutor` at all.
- **Pass the tenant explicitly in every event and have every listener call `runAs`** — necessary
  anyway for correctness (the event's tenant is authoritative), but insufficient: the principal and
  the "normal" tenant of the publishing thread would still be lost, and a pooled thread could leak a
  tenant from one task into the next.
- **A named executor, reachable by name only, with a `TaskDecorator`** that copies the context at
  submission and restores the worker afterwards.

## Decision
**`erpCoreEventExecutor`: a `ThreadPoolTaskExecutor` declared `defaultCandidate = false`, decorated by
`TenantAndSecurityContextTaskDecorator`** (as built):
- `@Bean(name = ErpCoreEvents.EXECUTOR, defaultCandidate = false)` with
  `@ConditionalOnMissingBean(name = ErpCoreEvents.EXECUTOR)`, in `ErpCoreEventsAutoConfiguration`,
  ordered after Boot's `TaskExecutionAutoConfiguration` so the application's default executor is
  decided first; `@EnableAsync` on the same class; never `@EnableScheduling` (ArchUnit rule 6)
  (`erp-core/src/main/java/com/erp/autoconfigure/ErpCoreEventsAutoConfiguration.java:33-36`, `:44-55`;
  `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java` rule 6);
- pool from `erp.core.events.executor.*`: core 4, max 16 (clamped to ≥ core), queue 500, prefix
  `erp-event-` (`autoconfigure/ErpCoreProperties.java:291-304`); no wait for running tasks on shutdown;
- the decorator captures `TenantContext.current()` and a fresh copy of the `SecurityContext` on the
  submitting thread, applies both on the worker, and restores the worker's previous tenant and security
  context in a `finally` — normally "nothing", so a pooled thread never carries a tenant or a principal
  from one task into the next (`events/support/TenantAndSecurityContextTaskDecorator.java:8-17`, `:22-57`);
- listeners name it: `@Async(ErpCoreEvents.EXECUTOR)`; NOTIF's listener injects it
  `@Qualifier(ErpCoreEvents.EXECUTOR)` and submits the delivery itself (1.2.0), so that a rejected
  execution is caught (`notif/service/NotificationDeliveryListener.java:38-44`, `:59-70`);
- the event's own `tenantId` stays authoritative: a listener still wraps its tenant work in
  `TenantContext.runAs(event.getTenantId(), …)` (`events/DomainEvent.java:17-20`; ADR-EVENTS-003).

Reasons, from the step report and the deviations:
1. **A library must not change the application's asynchronous behaviour**: Boot's
   `applicationTaskExecutor` is still created and still serves unqualified `@Async`
   (`docs/DEVIATIONS.md` [08] "Executor wiring", asserted by `ErpCoreEventsAndNotifAutoConfigurationTest`).
2. **Context must travel with the task, not be rebuilt by every listener**: the decorator captures it
   when the task is submitted (right after the commit), not when it runs, and copies the security
   context so a later change on either thread never leaks (`TenantAndSecurityContextTaskDecorator.java:14-17`).
3. **No leak across pooled threads** is a tenant-isolation requirement (POL-TENANT-007), verified by
   `TenantAndSecurityContextTaskDecoratorTest`.

## Consequences
- An application that wants a different pool defines a bean named `erpCoreEventExecutor` itself (the
  core one backs off), or tunes `erp.core.events.executor.*`.
- Work submitted to the executor from a thread without a tenant or principal runs without them; the
  event's `tenantId` is what a listener must use.
- `@EnableAsync` is turned on for the whole application by the core (it also activates the
  application's own `@Async` methods); scheduling stays the application's decision.
- On shutdown, a running listener is interrupted and its event is lost (ADR-EVENTS-001); NOTIF
  recovers through its requeue job.
- The `ThreadLocal` behind `TenantContext` is what the decorator copies; the tenant-maturity plan's
  `ScopedValue` spike (C.6, ADR-TENANT-004 — owned by the implementing package; draft in docs/plans/tenant-maturity-analysis-reference.md) must keep this decorator and its tests green under virtual
  threads before any change.

## Traces
REQ-EVENTS-006, REQ-EVENTS-007, REQ-EVENTS-008, REQ-EVENTS-012, REQ-EVENTS-013 · RULE-EVENTS-004,
RULE-EVENTS-005, RULE-EVENTS-006, RULE-EVENTS-009 · POL-EVENTS-003, POL-EVENTS-005, POL-EVENTS-006 ·
XM-EVENTS-002 · docs/DEVIATIONS.md [08] (executor wiring), [12] (rule 6), [15] (executor rejection);
docs/steps/08-report.md deviation 17
