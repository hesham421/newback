# ADR-AUDIT-003 — Coexistence with SEC_AUDIT_LOG (dual write), opt-in app-scheduled retention, OCCURRED_AT as TIMESTAMP without zone

Module  : AUDIT     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P2 (Database) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
Three independent choices were left to step 10 and are recorded together because each shapes
`CORE_AUDIT_EVENT`'s place in the platform:

1. **`SEC_AUDIT_LOG` already existed** (ENT-SEC-011, REQ-SEC-024..026, screen `SEC_AUDIT_LOG`, its own
   service, tests and frontend screen) as the security-specific log of logins, failed logins, resets and
   grant changes. The step said it "stays" (docs/DEVIATIONS.md [10] first entry). Options: migrate it into
   the generic log and delete it (a data migration plus the removal of a shipped SEC screen and API — a
   MAJOR change); keep both with no link (two timelines for one account); or keep both and have SEC write
   the account events that matter to a generic reader into `CORE_AUDIT_EVENT` as well.
2. **Retention.** Options: core schedules a purge with a default period; core keeps everything for ever
   with no job; or core ships the job and the properties but never schedules it.
3. **Timestamp type.** Every other core table uses `TIMESTAMPTZ` (`CORE_TENANT` `V10:38`,
   `CORE_NUMBER_SERIES` `V14:42`); the rows of this table are written by raw JDBC (ADR-AUDIT-001), not by
   Hibernate, and read back by Hibernate as `Instant`.

## Decision
1. **Coexistence with dual write.** `SEC_AUDIT_LOG` is unchanged — table, entity `AuditLogEntry`,
   `AuditLogService`, the SEC audit screen, every existing write and test; no data migration
   (`V15__audit_schema.sql:7-8`). SEC additionally records, through `AuditApi`, `LOGIN` (staff and customer
   login, on success), `LOGOUT` (staff) and `PASSWORD_RESET` (staff and customer reset completion), with the
   **account itself as actor** (username, realm, user id) and `ENTITY_TYPE = SEC_USER`
   (`erp-core/src/main/java/com/erp/sec/service/SecAuditEntries.java:12-35`; `sec/service/AuthService.java:117`,
   `:166`; `sec/service/CustomerAccountService.java:206`, `:266`; `sec/service/PasswordResetService.java:146`).
   Failed logins and reset *requests* stay in `SEC_AUDIT_LOG` only. User and Role changes are recorded field
   by field by `@Audited` (`passwordHash`, `lastLoginAt` ignored). Result: one timeline per account exists in
   the generic log without touching the security log (REQ-SEC-024 CHANGED in the SEC addendum).
2. **Opt-in, application-scheduled retention.** `AuditRetentionJob` is always a bean; `run()` applies
   `erp.core.audit.retention-days` (default `0` = keep forever → no-op) and deletes tenant by tenant with
   JDBC (`SELECT DISTINCT TENANT_ID … WHERE OCCURRED_AT < ?`, then `DELETE … WHERE TENANT_ID = ? AND
   OCCURRED_AT < ?`); its `@Scheduled(cron = "${erp.core.audit.retention-cron:-}")` trigger fires only when the
   application enables scheduling and sets a cron (default `-`, disabled). Core never enables scheduling
   (`audit/service/AuditRetentionJob.java:11-46`; `audit/service/AuditEventStore.java:92-108`;
   `autoconfigure/ErpCoreProperties.java:358-374`; docs/CONSUMING.md §2 "Scheduling"). Same precedent as the
   step-08 notification requeue job.
3. **`OCCURRED_AT` (and `CREATED_AT`, `UPDATED_AT`) are `TIMESTAMP` without time zone**
   (`V15__audit_schema.sql:19`, `:33`, `:35`). The JDBC writer binds UTC wall-clock `LocalDateTime`s
   (`LocalDateTime.ofInstant(instant, ZoneOffset.UTC)`, `AuditEventStore.java:125-127`), which is exactly
   the value Hibernate writes for an `Instant` into a `TIMESTAMP` column, so `AuditEvent.occurredAt`
   (`Instant`) reads the same instant back and the API renders it as UTC ISO-8601
   (`audit/dto/AuditEventResponse.java:21`); the retention cut-off and the `from` / `to` filters compare in
   the same representation (`AuditEventStore.java:97`; `common/search/InstantFieldValueConverter.java`).
   The departure from the chain's `TIMESTAMPTZ` is therefore invisible to callers but **permanent**: the
   additive-only rule forbids a column type change (`MigrationNamingTest`; docs/RELEASE.md).

## Consequences
- A security reviewer reads two logs: `SEC_AUDIT_LOG` for the complete security feed (including failures)
  and `CORE_AUDIT_EVENT` for the cross-module timeline; the success events appear in both, by design.
- `ACTOR_USER_ID` has no default: SEC's entries pass it; other callers leave it null unless they know it,
  which avoids a module cycle (audit → SEC → audit) (docs/DEVIATIONS.md [10] `ACTOR_USER_ID` default).
- Without retention the table grows without bound; an application that enables it must either call
  `AuditRetentionJob.run()` from its own scheduler or add `@EnableScheduling` and set
  `erp.core.audit.retention-cron`. The job runs outside any tenant and request, across all tenants.
- Any raw SQL or external tool reading `OCCURRED_AT` must treat it as **UTC**; a session time zone does not
  apply. A future column with zone semantics must be a new column, never a retype of this one.
- The audit columns of this table are nullable (`V15:32-35`) and `VERSION` is always 0: the base-class
  conventions are satisfied syntactically, but the row is never updated (POL-AUDIT-004).

## Traces
ENT-AUDIT-001 · REQ-AUDIT-012, REQ-AUDIT-013, REQ-AUDIT-016, REQ-AUDIT-018 · RULE-AUDIT-009,
RULE-AUDIT-010 · POL-AUDIT-004, POL-AUDIT-008, POL-AUDIT-010 · DBF-AUDIT-003, DBF-AUDIT-006,
DBF-AUDIT-017, DBF-AUDIT-019 · XM-AUDIT-002 · REQ-SEC-024 (SEC addendum, CHANGED) · docs/DEVIATIONS.md
[10] (`SEC_AUDIT_LOG` vs the generic log; `ACTOR_USER_ID` default; column details; retention job);
docs/steps/10-report.md "Decisions & deviations" 1, 10, 11, 14
