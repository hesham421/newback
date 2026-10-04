# Step 10 — Generic audit log — report

## Summary

One cross-module, tenant-scoped audit store, `CORE_AUDIT_EVENT`, now exists. Any module or app can write to it explicitly, entities can opt in to field-level change records, and staff can query it.

- **`V15__audit_schema.sql`** creates `CORE_AUDIT_EVENT` with:
  - the step's columns, the audit columns and `VERSION`;
  - `TENANT_ID NOT NULL` with no default, an FK to `CORE_TENANT` and a `(TENANT_ID)` index;
  - the step's indexes `(TENANT_ID, ENTITY_TYPE, ENTITY_ID)` and `(TENANT_ID, OCCURRED_AT)`;
  - CHECK constraints on `ACTION` (`^[A-Z_]{3,64}$`) and `ACTOR_REALM` (STAFF/CUSTOMER/SYSTEM).

  It seeds no permission.
- **`com.erp.audit`** is appended to `CORE_PACKAGE_LIST`. Its public surface is `crossmodule`:
  - `AuditApi.record(AuditEntry)` writes synchronously, in the caller's transaction.
  - `AuditEntry` is a builder. Defaults come from `TenantContext` (tenant), the `SecurityContext` (actor; realm follows the `DomainEvent` rule) and the `RequestInfoHolder` filter (IP and User-Agent).
  - `AuditChange` is one `{field, old, new}` entry.
  - The `@Audited(entityType, ignore)` annotation opts an entity in.
- **Write path.** `AuditRecordingService` validates the entry (`AuditEventDomain`), fills the defaults and drops sensitive changes. `AuditEventStore` then inserts the row with JDBC on the writer's own connection:
  - the `@Audited` listener uses the Hibernate session's connection (`doWork`);
  - explicit calls use the connection bound to the Spring transaction.

  So a rollback removes the audit row together with the change.
- **Entity listener.** `AuditedEntityListener` handles Hibernate `POST_INSERT`, `POST_UPDATE` and `POST_DELETE`. It is registered by `AuditHibernateConfiguration`, a `HibernatePropertiesCustomizer` that sets `hibernate.integrator_provider` to an Integrator, which appends the listener to the `EventListenerRegistry`.
  - It writes CREATE, UPDATE (changed fields only; nothing if no recordable field changed) and DELETE rows.
  - It never records: the `ignore` fields, the global denylist (`password, secret, token, hash` plus `credential, apikey, privatekey, salt`), the audit columns, `version`, `tenantId`, collections and binary values.
- **Annotated entities:** `User` (ignores `passwordHash` and `lastLoginAt`), `Role`, `Tenant`, `FileDocument`, `FileCategory`, `NotificationTemplate`, `LookupType`, `LookupValue`, `AppConfiguration`. `NumberSeries` comes with step 09.
- **SEC.** `SEC_AUDIT_LOG` stays unchanged. SEC additionally writes:
  - `LOGIN` for staff and customer logins;
  - `LOGOUT` for staff;
  - `PASSWORD_RESET` for staff and customer reset completions.

  The actor of these rows is the account itself.
- **Query API.** `GET /api/v1/audit/events` filters on `entityType`, `entityId`, `actor`, `action` and a `from`/`to` range, newest first, using `SpecBuilder` and `PageableBuilder`.
  - It requires `AUDIT:EVENT:READ`, contributed by `AuditPermissions`; every super role holds it.
  - Results are tenant-isolated.
  - A customer token gets 403 `REALM_MISMATCH`.
- **Retention.** `AuditRetentionJob.run()` uses `erp.core.audit.retention-days` (default 0 = keep forever). Its `@Scheduled` trigger uses `erp.core.audit.retention-cron` (default `-`, disabled). It deletes tenant by tenant.
- **i18n:** `AUDIT_ACTION_INVALID`, EN and AR.

`mvn -q verify` is green:
- erp-core: 237 tests (206 before, plus 31 new);
- erp-app-reference: 9 tests.

## Files changed

**Created**
- `erp-core/src/main/resources/db/migration/core/V15__audit_schema.sql`
- `erp-core/src/main/java/com/erp/audit/`:
  - `crossmodule/{AuditApi, AuditApiImpl, AuditEntry, AuditChange, Audited}.java`
  - `domain/AuditEventDomain.java`, `entity/AuditEvent.java`, `repository/AuditEventRepository.java`
  - `dto/AuditEventResponse.java`, `mapper/AuditEventMapper.java`, `exception/AuditErrorCodes.java`
  - `service/{AuditRecordingService, AuditEventStore, AuditEventService, AuditRetentionJob}.java`
  - `listener/AuditedEntityListener.java`, `config/AuditHibernateConfiguration.java`, `web/RequestInfoHolder.java`
  - `controller/AuditEventController.java`, `permission/AuditPermissions.java`
- `erp-core/src/main/java/com/erp/sec/service/SecAuditEntries.java`
- Tests:
  - `erp-core/src/test/java/com/erp/audit/{AuditApiIntegrationTest (7), AuditLogApiIntegrationTest (8), AuditedEntitiesCoverageIntegrationTest (7), AuditRetentionJobIntegrationTest (1)}.java`
  - `domain/AuditEventDomainTest (5)`
  - helpers `AuditHttp`, `AuditRows`
  - `erp-core/src/test/java/com/erp/sec/SecAuditIntegrationTest.java` (3)
- `docs/steps/10-report.md`

**Modified**
- `autoconfigure/ErpCoreAutoConfiguration.java`: `,com.erp.audit` appended.
- `autoconfigure/ErpCoreProperties.java`: an `audit` field and an `Audit` class appended.
- `@Audited` and its import added to `sec/entity/User`, `sec/entity/Role`, `tenant/entity/Tenant`, `file/entity/FileDocument`, `file/entity/FileCategory`, `notif/entity/NotificationTemplate`, `mdl/entity/LookupType`, `mdl/entity/LookupValue` and `cu/entity/AppConfiguration`.
- `sec/service/AuthService`, `PasswordResetService`, `CustomerAccountService`: one `AuditApi` field and one `record` call per event.
- `resources/i18n/messages.properties`, `messages_ar.properties`: a step-10 block at the end.
- `resources/db/migration/core/README.md`: V15 row.
- `docs/DEVIATIONS.md`: 17 `[10]` entries.
- Tests:
  - `architecture/CrossModuleBoundaryArchTest`: `audit` module appended;
  - `autoconfigure/ErpCoreAutoConfigurationTest`: `com.erp.audit` expected;
  - `tenant/TenantSchemaIntegrationTest`: 20 → 21 tables and entities;
  - `sec/PermissionCatalogIntegrationTest`: the seeded-catalog row-for-row check excludes the audit contributor;
  - `erp-app-reference/.../ReferenceApplicationSmokeTest`: Flyway `2..13, 15, 1000`.

**Deleted:** none.

**Untouched:** `erp-core-plan/execution-state.json`, `erp-app-reference/governance/`, and `SEC_AUDIT_LOG` with its code and tests.

## Decisions & deviations

These mirror the 17 `[10]` entries in `docs/DEVIATIONS.md`:

1. **`SEC_AUDIT_LOG` is kept unchanged.** SEC also records LOGIN, LOGOUT and PASSWORD_RESET in the generic log. Failed logins and reset requests stay in `SEC_AUDIT_LOG` only.
2. **The write path is JDBC on the writer's connection.** Persisting through the session during a flush is unsafe. `AuditEvent` is an `@Immutable` read model.
3. **The listener calls `AuditRecordingService.record(entry, connection)`**, the code behind `AuditApi.record`, so that it can use the session's connection.
4. **No asynchronous or event-based writer.** The step fixes a synchronous insert in the caller's transaction, so only committed changes are audited.
5. **Nine entities are annotated.** `NumberSeries` (step 09) is added when this step is rebased onto 09.
6. **`@Audited` lives in `crossmodule`.** `SEC_USER` also ignores `lastLoginAt`.
7. **Diff rules:**
   - technical fields, collections and binary values are skipped;
   - an association is recorded as the referenced id;
   - values become JSON scalars, and text is cut at 2000 characters;
   - an UPDATE with no recordable change writes no row.
8. **The denylist matches substrings, case-insensitively, and is extended.** `contentHash` is therefore excluded. `configValue` is recorded, because its name is not sensitive.
9. **Tenant of a row:** the entity's own tenant, or the current tenant for global entities and explicit entries. With no tenant at all, recording fails fast.
10. **`ACTOR_USER_ID` has no default**, which avoids a module cycle with SEC. The realm default follows `DomainEvent`.
11. **Column sizes, nullability, CHECK constraints and the extra `(TENANT_ID)` index** were chosen where the step was silent.
12. **The query is a GET with parameters**, as the step says. Malformed dates → 400. There are no write endpoints (named controller/DTO deviations).
13. **`AUDIT:EVENT:READ` comes only from the contributor**, as the screen's VIEW gateway. The catalog test is scoped to seeded modules.
14. **The retention job is always a bean.** `run()` is a no-op at 0 days, and its cron is optional.
15. **`RequestInfoHolder`** is a filter bean that captures `getRemoteAddr()` and the User-Agent.
16. **The infrastructure beans carry no `@PreAuthorize`** (named A.5.2 deviation).
17. **Shared test counts and lists were updated additively.**

## Acceptance checklist

2/2 ✅

- ✅ **Tests green; every `@Audited` entity has a corresponding row in tests.**
  - `mvn -q verify` EXIT=0: erp-core 237/0/0/0, erp-app-reference 9/0/0/0.
  - `AuditedEntitiesCoverageIntegrationTest` creates and updates each of the 9 audited entities over HTTP and asserts exactly one CREATE row and one UPDATE row whose changes are exactly the edited fields: SEC_USER, SEC_ROLE, CORE_TENANT, FILE_CATEGORY, NOTIF_TEMPLATE, MDL_LOOKUP_TYPE, MDL_LOOKUP_VALUE, CU_APP_CONFIGURATION. For FILE_DOCUMENT it asserts the CREATE row plus the publish UPDATE.
  - The step's task-6 tests:
    - user create → one CREATE row without `passwordHash` (`AuditLogApiIntegrationTest.creatingAUser_...`);
    - two-field update → one UPDATE row with exactly 2 changes (`updatingTwoFields_...`);
    - rollback → no row, both for the listener (`SecAuditIntegrationTest.auditedChange_inARolledBackTransaction_...`) and for the API (`AuditApiIntegrationTest.record_inARolledBackTransaction_...`);
    - tenant isolation on query (`query_isTenantIsolated`);
    - customer token → 403 `REALM_MISMATCH` (`query_customerToken_is403`);
    - retention deletes rows older than N days in every tenant and keeps younger ones (`AuditRetentionJobIntegrationTest`).
- ✅ **No sensitive field name appears in any `CHANGES` JSON in tests (denylist scan).**
  - `AuditRows.assertNoSensitiveFieldAnywhere` runs after every test of the two HTTP classes and in `record_dropsSensitiveChanges_...`.
  - It scans every `CHANGES` array in the table, across all tenants, against: password, passwordhash, token, tokenhash, secret, hash, contenthash, credential, apikey, privatekey, salt, configjson.

## Verification output

```
$ java -version → openjdk version "21.0.7" 2025-04-15
$ rm -rf target erp-core/target erp-app-reference/target; mvn -o -q verify
EXIT=0 secs=197
== erp-core (new classes; full list in target/surefire-reports)
com.erp.audit.AuditApiIntegrationTest                          7 0 0 0
com.erp.audit.AuditLogApiIntegrationTest                       8 0 0 0
com.erp.audit.AuditRetentionJobIntegrationTest                 1 0 0 0
com.erp.audit.AuditedEntitiesCoverageIntegrationTest           7 0 0 0
com.erp.audit.domain.AuditEventDomainTest                      5 0 0 0
com.erp.sec.SecAuditIntegrationTest                            3 0 0 0
TOTAL tests=237 failures=0 errors=0 skipped=0
== erp-app-reference
com.erp.app.ReferenceApplicationSmokeTest                      9 0 0 0
TOTAL tests=9 failures=0 errors=0 skipped=0

Failed attempt on the way (fixed):
- full verify #1: PlatformTenantApiIntegrationTest expected 4 lookup types per new tenant, got 5. The audit
  coverage test had created a lookup type (and a template) in PLATFORM, which tenant provisioning copies.
  → the MDL/NOTIF coverage cases now run in a tenant provisioned by the audit test itself; the existing
  assertion is untouched.
```

## Skills checked

- **`build-create-entity`:** `AuditEvent` is compliant except named items.
  - Compliant: it extends `AuditableEntity` (tenant), uses `@SuperBuilder`, a SEQUENCE `SEQ_CORE_AUDIT_EVENT` with `allocationSize = 1`, and declares its indexes inside `@Table`.
  - PK `ID` follows README §4.
  - It is `@Immutable` with no setters, because it is append-only. No activate/deactivate (A.1.18, named).
  - `AuditEventDomain` is a plain class (`create`) that throws `LocalizedException`.
- **`build-create-repository`:** compliant (`@Repository`, JPA + Specification). It has no extra methods, so there is no dead code.
- **`build-create-dto`:** `AuditEventResponse` is compliant: bilingual `@Schema` with examples, and UTC `@JsonFormat` on the audit timestamps. Named deviation: no Create/Update/Search/Usage DTOs (read-only, GET parameters).
- **`build-create-mapper`:** compliant, manual and null-safe. It has no `toEntity`/`update`/`usage`, because the model is read-only.
- **`build-create-service`:** `AuditEventService` is compliant: `@Transactional(readOnly)`, `@PreAuthorize` with its own module's constant, `ServiceResult`, `SpecBuilder`/`PageableBuilder` with allowed fields, `log.debug`. Named deviation: the in-process infrastructure carries no `@PreAuthorize` (A.5.2).
- **`build-create-controller`:** thin, `OperationCode.craftResponse`, `@Operation`, bilingual `@Tag`. Named A.6.5–A.6.8 deviations (step-mandated GET, no write endpoints).
- **`gov-enforce-error-handling`:** compliant.
  - Every throw is a `LocalizedException`.
  - `AUDIT_ACTION_INVALID` is registered in `AuditErrorCodes` (private constructor that throws) and in both bundles with `''{0}''`.
  - Malformed dates use the common `VALIDATION_ERROR`.
- **`gov-enforce-caching-rules`:** compliant. No cache annotations.
- **`gov-enforce-backend-contract`**, audit feature: 77/85. Failures, all named: A.1.18, A.3.9, A.3.12, A.4.7, A.5.2 (infrastructure), and A.6.5–A.6.8 (step-defined shape). CU.1–CU.8 pass.
- **`gov-validate-backend-feature`:**
  - Stage 0: 7/9 (DTO set; one READ permission instead of four).
  - Stage 1: 11/15.
  - Stage 2: 77/85.
  - Stage 3: 33/37.
  - Stage 4: 2/2.
  - Score **130/148 (87.8%)**, CONDITIONAL by the raw threshold. Every lost point is a shape the step prescribes: read-only append-only log, single READ permission, GET query. There is no automatic-rejection trigger once those are taken as named deviations.

## Notes for later steps

- **Rebase onto step 09.**
  - Annotate `NumberSeries` with `@Audited(entityType = "<its table>")` and add a create/update case to `AuditedEntitiesCoverageIntegrationTest`. If 09's settings entity is the step's `AppConfiguration`, it is already annotated.
  - Smoke-test Flyway list → `..., "13", "14", "15", "1000"`.
  - `TenantSchemaIntegrationTest` counts → 09's numbers + 1.
  - If 09 adds a `PermissionContributor` that no migration seeds, exclude it in `PermissionCatalogIntegrationTest` the same way.
- **Auditing new entities (apps, Phase 4).** Annotate an entity with `@Audited(entityType = "TABLE", ignore = {...})`. Explicit facts (approvals, status changes, exports) go through `AuditApi.record(AuditEntry.builder().action(...)...)`, inside the business transaction.
- **Tests that create reference data in PLATFORM** (lookup types, templates, channels) change what newly provisioned tenants receive. Create them in your own provisioned tenant instead.
- **The audit row of a global entity** (`CORE_TENANT`) belongs to the acting tenant.
- **ACTOR_USER_ID** is filled only when the caller passes it.
- **Shared files touched**, for merge planning:
  - `ErpCoreAutoConfiguration.CORE_PACKAGE_LIST` (appended);
  - `ErpCoreProperties` (field and class appended);
  - the i18n bundles (step-10 block at the end);
  - `docs/DEVIATIONS.md`;
  - `db/migration/core/README.md` (V15 row);
  - `CrossModuleBoundaryArchTest` (module appended);
  - `TenantSchemaIntegrationTest` (counts);
  - `ErpCoreAutoConfigurationTest` (package list);
  - `PermissionCatalogIntegrationTest` (filter);
  - `ReferenceApplicationSmokeTest` (Flyway list);
  - the 9 annotated entities;
  - `AuthService`, `PasswordResetService`, `CustomerAccountService`.
  - Not touched: `AutoConfiguration.imports`, `ErpCoreSecurityAutoConfiguration`, `MigrationNamingTest`.
