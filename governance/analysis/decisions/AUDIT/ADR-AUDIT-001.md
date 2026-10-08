# ADR-AUDIT-001 — Synchronous JDBC write on the caller's connection, including from inside the Hibernate flush

Module  : AUDIT     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1/P2 (write path) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
The step-10 file (`erp-core-plan/10-STEP-generic-audit-log.md`) fixed two things and left one open: `AuditApi.record`
is a synchronous insert in the caller's transaction, so that "only committed changes are audited"; the
`@Audited` listener records field-level changes from Hibernate's `PostInsert` / `PostUpdate` / `PostDelete`
events; and the write path of `CORE_AUDIT_EVENT` itself was unspecified (docs/DEVIATIONS.md [10] "Write
path of `CORE_AUDIT_EVENT` (unspecified)"). Step 08 had introduced the asynchronous event bus, which raised
the question whether audit rows should be written by an `AFTER_COMMIT` listener instead.

Three ways were available:
- **JPA — persist an `AuditEvent` entity through the repository.** Natural for explicit calls, but the
  `@Audited` listener runs *during* a flush: Hibernate's action queue is being executed and an insert queued
  from a `PostInsertEventListener` may never be flushed (Hibernate 7.2); the listener would also need a
  second session or `REQUIRES_NEW`, breaking the "same transaction" rule.
- **Asynchronous — publish a `DomainEvent` and insert from an `AFTER_COMMIT` listener on the event
  executor.** Only committed changes, but at-most-once: a crash after the commit loses the row; a retry
  could duplicate it; and the row would be written outside the business transaction, in another tenant
  context that must be restored by hand.
- **Plain JDBC on the writer's own connection.** One `INSERT` statement; for explicit calls on the
  connection bound to the current Spring transaction (`JdbcTemplate`), for the listener on the flushing
  session's connection (`Session.doWork`); the row commits or rolls back with the change.

## Decision
`CORE_AUDIT_EVENT` is written by `AuditEventStore` with **one JDBC `INSERT` on the writer's own connection**
(`erp-core/src/main/java/com/erp/audit/service/AuditEventStore.java:23-32`, `:43-46`):
- explicit `AuditApi.record(entry)` → `AuditRecordingService.record(entry)` → `store.insert(entry)` →
  `JdbcTemplate.execute(ConnectionCallback)` on the Spring-transaction-bound connection
  (`AuditEventStore.java:54-59`); called outside a transaction it commits on its own
  (`audit/crossmodule/AuditApi.java:10-13`);
- the `@Audited` listener → `AuditRecordingService.record(entry, connection)` → `store.insert(connection,
  entry)` inside `session.doWork(connection -> …)` of the flushing session
  (`audit/listener/AuditedEntityListener.java:125-139`; `AuditEventStore.java:62-90`); the listener declares
  `requiresPostCommitHandling = false` (`:120-123`), so it runs in the flush, before the commit;
- the listener is handed to Hibernate by `AuditHibernateConfiguration`, a `HibernatePropertiesCustomizer`
  that sets `hibernate.integrator_provider` to an `Integrator` appending the listener to `POST_INSERT`,
  `POST_UPDATE` and `POST_DELETE` — composed with an application's existing provider, never replacing it
  (`audit/config/AuditHibernateConfiguration.java:36-68`);
- `AuditEvent` is an `@Immutable` JPA **read model** with no setters; JPA never inserts or updates the
  table (`audit/entity/AuditEvent.java:19-30`); `AuditEventRepository` serves the query API and the report
  only (`audit/repository/AuditEventRepository.java:8-13`);
- timestamps are bound as UTC wall-clock `LocalDateTime`s, exactly what Hibernate writes for an `Instant`
  into a `TIMESTAMP` column, so the read model reads them back unchanged (`AuditEventStore.java:34-36`,
  `:125-127`; ADR-AUDIT-003);
- every statement names `TENANT_ID` explicitly (step-05 rule); `com.erp.audit..` is on the ArchUnit raw-JDBC
  allow-list (`erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:77`).

No asynchronous writer and no listener on `DomainEvent` exist (docs/DEVIATIONS.md [10] "Step 08 events for
async write"); an application that wants events mirrored into the log calls `AuditApi` from its own
`@TransactionalEventListener` inside `TenantContext.runAs(event.getTenantId(), …)`.

Verification: a rolled-back change leaves no row, both for the listener
(`SecAuditIntegrationTest.auditedChange_inARolledBackTransaction_…`) and for the API
(`AuditApiIntegrationTest.record_inARolledBackTransaction_leavesNoRow_andInACommittedOne_staysWritten`);
docs/steps/10-report.md "Acceptance checklist".

## Consequences
- An audit row exists if and only if the change it describes committed; nothing is lost on a crash and
  nothing is duplicated by a retry (POL-AUDIT-002).
- Every audited write costs one extra `INSERT` on the same connection, synchronously; `nextValue`-style
  hot fields must be excluded with `@Audited(ignore)` to avoid one row per allocation (ADR-SEQUENCE-003;
  docs/DEVIATIONS.md [10] rebase entry).
- `AuditRecordingService` carries neither `@Transactional` nor `@PreAuthorize`: it must join the caller's
  transaction and is reached in-process only, also on anonymous paths such as a login
  (`audit/service/AuditRecordingService.java:20-29`; named deviation from `build-create-service` A.5.2/A.5.3).
- The listener cannot use JPA, so an association is recorded as the referenced id and a value becomes a
  JSON scalar (ADR-AUDIT-002); the `CHANGES` JSON is produced by a private `JsonMapper` independent of the
  application's Jackson settings (`AuditEventStore.java:48-49`).
- A failure inside the audit insert fails the business transaction (no "best effort"): the step-10 design
  values a complete log over a tolerant one.

## Traces
ENT-AUDIT-001 · REQ-AUDIT-001, REQ-AUDIT-004, REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-021 ·
RULE-AUDIT-002, RULE-AUDIT-003, RULE-AUDIT-011 · POL-AUDIT-002, POL-AUDIT-004 · DBF-AUDIT-001 …
DBF-AUDIT-020 (all written by the one insert) · XM-AUDIT-002, XM-AUDIT-003 · docs/DEVIATIONS.md [10]
(write path; listener calls `record(entry, connection)`; no async writer); docs/steps/10-report.md
"Decisions & deviations" 2, 3, 4
