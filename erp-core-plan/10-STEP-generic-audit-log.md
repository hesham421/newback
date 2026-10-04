# Step 10 — Generic audit log

**Branch:** `step/10-audit-log`
**Goal:** one cross-module, tenant-scoped audit event store that any module or app can write to explicitly, plus an opt-in entity listener that records field-level changes for annotated entities; queryable by staff.
**Why:** today only `SEC_AUDIT_LOG` exists and is written by `sec` services; `common/audit` only stamps created/updated columns. E-invoicing and SaaS operations need a "who changed what, when" trail for business documents.

## Preconditions
- Steps 05 and 08 merged (tenant context; events for async write).

## Design (fixed decisions)
- Package `com.erp.audit`; table `CORE_AUDIT_EVENT`: ID, TENANT_ID, OCCURRED_AT, ACTOR (username), ACTOR_REALM, ACTOR_USER_ID (nullable), ACTION VARCHAR(64) (`CREATE|UPDATE|DELETE|STATUS_CHANGE|LOGIN|...` free but validated `^[A-Z_]{3,64}$`), ENTITY_TYPE VARCHAR(128), ENTITY_ID VARCHAR(64), SUMMARY_AR, SUMMARY_EN, CHANGES JSONB (array of `{field, old, new}`), IP VARCHAR(64), USER_AGENT VARCHAR(256), REFERENCE (nullable correlation id), audit columns, version. Indexes: `(TENANT_ID, ENTITY_TYPE, ENTITY_ID)`, `(TENANT_ID, OCCURRED_AT)`.
- `AuditApi` (`com.erp.audit.crossmodule`): `record(AuditEntry)` — synchronous insert in the caller's transaction (so a rolled-back business change leaves no audit row); `AuditEntry` builder with sensible defaults from `SecurityContext`/`TenantContext`/request (via a `RequestInfoHolder` filter capturing IP + UA).
- Opt-in entity listener: annotate an entity with `@Audited(entityType = "SEC_USER", ignore = {"passwordHash"})` → a Hibernate `PostInsert/PostUpdate/PostDelete` listener diffs dirty properties and calls `AuditApi.record` with `CHANGES`. Sensitive fields are excluded by `ignore` and by a global denylist (`password`, `secret`, `token`, `hash`).
- `SEC_AUDIT_LOG` stays (security-specific); `sec` additionally writes `LOGIN`, `LOGOUT`, `PASSWORD_RESET` to the generic log so one timeline exists. No data migration.
- Query API: `GET /api/v1/audit/events` with the existing `SpecBuilder` filters (entityType, entityId, actor, action, date range), permission `AUDIT:EVENT:READ`. Customers never read audit.
- Retention: `erp.core.audit.retention-days` (default 0 = keep forever) + a `AuditRetentionJob` bean the app may schedule.

## Tasks
1. Migration `V15__audit_schema.sql` (table, indexes, seed permission via contributor instead).
2. Implement entity, repository, `AuditApi` + impl, `@Audited` annotation + Hibernate integrator registration (via `HibernatePropertiesCustomizer` adding the listener to `EventListenerRegistry`), `RequestInfoHolder` filter, controller, `AuditPermissions`.
3. Annotate core entities: `SecUser`, `SecRole`, `Tenant`, `FileDocument`, `FileCategory`, `NotifTemplate`, `LookupType`, `LookupValue`, `AppConfiguration`, `NumberSeries`.
4. Add explicit `record` calls in `sec` for login/logout/password events.
5. **i18n**: `AUDIT_ACTION_INVALID`.
6. **Tests**: creating a user produces one `CREATE` row with `CHANGES` excluding `passwordHash`; updating two fields produces one `UPDATE` row with exactly two change entries; rollback → no row; tenant isolation on query; customer token → 403; retention job deletes rows older than N days.

## Acceptance
- Tests green; every `@Audited` entity has a corresponding row in tests.
- No sensitive field name appears in any `CHANGES` JSON in tests (assert with a denylist scan).

## Commit
`step(10): generic tenant-scoped audit event log with AuditApi, @Audited entity listener, query API and retention job`

## Out of scope
Immutable/append-only storage guarantees, audit export, replacing `SEC_AUDIT_LOG`.
