# ADR-SEQUENCE-001 — Anchor-row pessimistic lock in REQUIRES_NEW; gaps accepted, not gap-free

Module  : SEQUENCE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1/P2 (allocation design) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
Document numbers must be unique and, within a period, consecutive, under concurrent callers of
`NumberSeriesApi.next(code)` from different modules and different threads; the series are tenant-scoped
rows of one shared table (`CORE_NUMBER_SERIES`, `V14__sequence_and_settings.sql:31-61`). The step-09 file
(`erp-core-plan/09-STEP-sequences-and-settings-api.md`) fixed `next` as "atomic under concurrency" and accepted
that "a rolled-back caller leaves a gap" (docs/steps/09-report.md Summary).

Four ways were available:
- **A database sequence per series** — gap-free is impossible anyway (a `nextval` consumed by a rolled-back
  transaction is lost), the period reset (YEARLY / MONTHLY) would need `ALTER SEQUENCE` or one sequence per
  period created at runtime, and nothing would be tenant-scoped.
- **Optimistic locking with retry** — every collision costs a round trip and a retry loop in the library; under
  50 concurrent callers the retries dominate.
- **Allocation inside the caller's transaction** — the row lock would be held for the whole business
  transaction, serialising every caller of a code behind the slowest one, and numbers would be gap-free only
  if every caller committed.
- **Pessimistic lock in a transaction of its own (`REQUIRES_NEW`)** — the lock is held for the allocation
  only; the number commits before the caller continues; a rollback of the caller leaves a gap.

## Decision
`NumberAllocationService.next` runs in `@Transactional(propagation = REQUIRES_NEW)`
(`erp-core/src/main/java/com/erp/sequence/service/NumberAllocationService.java:54-78`) and:
1. locks the series' **anchor row** — the row with the lowest id of the code in the current tenant —
   with `@Lock(PESSIMISTIC_WRITE)` `findFirstByCodeOrderByIdAsc` (`sequence/repository/NumberSeriesRepository.java:27-32`);
   Hibernate emits `… where tenant_id = ? and code = ? order by id fetch first ? rows only for no key update`
   (docs/steps/09-report.md "Verification output");
2. then locks the current period's row (`findByCodeAndPeriodKey`, `PESSIMISTIC_WRITE`,
   `NumberSeriesRepository.java:34-36`), or creates it at 1 when the period is new — race-free, because the
   anchor lock already serialises every allocation of the code;
3. takes `nextValue`, increments it (`sequence/entity/NumberSeries.java:112-116`), renders the number and
   commits; the `UPDATE … where id = ? and version = ?` is flushed while the lock is still held.

Consequences accepted by the plan: the values of one period are consecutive and never duplicated; a
caller whose own transaction later rolls back does not give its number back (**gap**); a caller inside a
transaction holds **two connections** for the duration of the call (`NumberAllocationService.java:23-32`).
`preview` takes no lock and writes nothing (`:80-97`). The row is `@Audited` with `nextValue` ignored, so an
allocation writes no audit row (ADR-AUDIT-002 is not touched; docs/DEVIATIONS.md [10] rebase entry).

Verification: `NumberSeriesConcurrencyIntegrationTest.fiftyConcurrentCalls_produceFiftyUniqueConsecutiveNumbers`
— 50 threads released by a latch produce exactly `T-0001..T-0050`, `NEXT_VALUE = 51`, one row; green on
three consecutive runs (docs/steps/09-report.md "Acceptance checklist").

## Consequences
- Strict gap-free numbering (fiscal requirements in some jurisdictions) is **out of scope** of the core
  module: a business module that needs it must allocate inside its own transaction with its own lock, or
  reconcile gaps; the core API documents the gap (`sequence/crossmodule/NumberSeriesApi.java:14-19`).
- Connection pools must be sized for the second connection of callers inside a transaction
  (docs/CONSUMING.md §3 "size the connection pool for it").
- The anchor row is a per-code hot spot; the lock is held only for one row read, one optional insert and one
  update, never for the caller's work.
- Tenant isolation of the lock is Hibernate's `@TenantId` predicate on the JPQL finders — no native SQL
  in the allocation (`NumberSeriesRepository.java:13-15`); the ArchUnit raw-JDBC allow-list for
  `com.erp.sequence..` exists for the provisioning contributor only.

## Traces
ENT-SEQUENCE-001 · REQ-SEQUENCE-010, REQ-SEQUENCE-011, REQ-SEQUENCE-013, REQ-SEQUENCE-020 ·
RULE-SEQUENCE-005, RULE-SEQUENCE-006, RULE-SEQUENCE-012 · POL-SEQUENCE-004 · DBF-SEQUENCE-001,
DBF-SEQUENCE-008, DBF-SEQUENCE-014 · docs/DEVIATIONS.md [09] (row model entry); docs/steps/09-report.md
"Decisions & deviations" 3
