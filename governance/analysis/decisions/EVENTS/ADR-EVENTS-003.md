# ADR-EVENTS-003 — Event payload contract: plain values, no secrets, tenant captured at publish; all events in the root package

Module  : EVENTS     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (Requirements) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
Events are handed to arbitrary listeners — other core modules and the consuming application — on
threads the publisher does not control. Three questions had no answer in the step file and were
settled by the implementation (`docs/DEVIATIONS.md` [08] "Where the core events live", "`DomainEvent`
realm"; `docs/steps/08-report.md` deviations 15–16):
1. **What may an event carry?** A JPA entity would drag a detached object (and its lazy associations)
   into a foreign thread; a password-reset token in clear would hand a secret to every listener.
2. **Which tenant and actor does an event belong to?** The publishing thread's context is the usual
   answer, but `TenantCreatedEvent` is published by the PLATFORM operator about a tenant that did not
   exist a moment ago, and customer registration is published by a public endpoint with no principal.
3. **Where do the event classes live?** The `build-create-service` skill's rule "listen only to your
   own module's events" suggests one package per module; but `UserCreatedEvent` is published by SEC
   and listened to by applications, `NotificationRequestedEvent` by NOTIF and listened to by NOTIF, and
   ArchUnit only lets other modules see a module's public surface.

## Decision
**Plain values only, no secrets; tenant, actor and realm captured at publish (or set explicitly on
behalf of someone else); every core event in the root package `com.erp.events`** (as built):
- `DomainEvent` carries `id` (random UUID — the idempotency key), `occurredAt`, `tenantId`, `actor`
  (`system` when none) and `realm` (`STAFF` / `CUSTOMER` / `SYSTEM`); subclasses add only plain values
  (ids, codes, instants, booleans) — never a JPA entity, a password, a raw token or another secret
  (`erp-core/src/main/java/com/erp/events/DomainEvent.java:12-33`, `:51-70`);
  `PasswordResetRequestedEvent` carries the token row id and its expiry, not the token
  (`events/PasswordResetRequestedEvent.java:6-15`);
- the protected no-arg constructor captures `TenantContext.current()`,
  `SecurityContextHelper.currentActorOrSystem()` and `currentRealm()` (`ROLE_CUSTOMER` → `CUSTOMER`,
  any other authenticated caller → `STAFF`, none → `SYSTEM`); the explicit `(tenantId, actor, realm)`
  constructor is for an event published on behalf of another tenant or realm — `TenantCreatedEvent`
  (tenant = the NEW tenant, realm `STAFF`) and the two customer events (realm `CUSTOMER`)
  (`events/DomainEvent.java:57-70`; `events/TenantCreatedEvent.java:15-18`;
  `events/CustomerRegisteredEvent.java:21-26`; `common/util/SecurityContextHelper.java:54-82`);
- the event's own `tenantId` is authoritative: an asynchronous listener runs its work inside
  `TenantContext.runAs(event.getTenantId(), …)` even though the executor also propagates the
  publisher's context (ADR-EVENTS-002);
- all ten core events are `final` classes in `com.erp.events`, whose root package is the module's
  public surface in ArchUnit (`support` internal); NOTIF's listener reacts to
  `NotificationRequestedEvent` from that shared package
  (`erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:57-61`;
  `docs/DEVIATIONS.md` [08] "Where the core events live").

Reasons:
1. **Listeners are untrusted neighbours**: a plain-value payload is safe to serialize, log and hand to
   an application; a secret in an event would leak through every listener's logs.
2. **The fact, not the thread, names the tenant**: provisioning and public endpoints prove the
   publishing thread is not always the fact's owner.
3. **One shared contract**: a module cannot listen to another module's internal package; the root
   package is the one place every publisher and listener may reference, and `docs/RELEASE.md` lists it
   as public API.

## Consequences
- Adding a field to an event, or a new event, is additive (a MINOR); removing or retyping a field is
  a breaking change of the public API and is not done.
- A listener needing an entity reloads it by id inside `runAs(event.getTenantId())` — never from the
  event.
- A listener on a public endpoint's event (customer registration) sees realm `CUSTOMER` and the
  customer's username as actor although no principal existed on the publishing thread.
- The 1.3.0 events of the tenant-maturity plan follow the same shape: `TenantSuspendedEvent(tenantId,
  code, reason, actor)` and `TenantActivatedEvent(tenantId, code, actor)` are published on behalf of
  the affected tenant (explicit constructor, like `TenantCreatedEvent`); `UserPasswordChangedEvent(userId,
  byAdmin)` carries no password (`docs/plans/tenant-maturity-plan.md` C.1, D.3, §1 rule 7).
- The `build-create-service` rule about module-internal event types does not apply to the shared
  catalogue; step 08 recorded this as a named deviation (`docs/steps/08-report.md` Skills checked).

## Traces
ENT-EVENTS-001 · REQ-EVENTS-004, REQ-EVENTS-005, REQ-EVENTS-008, REQ-EVENTS-009, REQ-EVENTS-010 ·
RULE-EVENTS-006, RULE-EVENTS-007, RULE-EVENTS-008 · POL-EVENTS-003, POL-EVENTS-004 · XM-EVENTS-001 ·
docs/DEVIATIONS.md [08] (where the core events live; `DomainEvent` realm); docs/steps/08-report.md deviations 15, 16
