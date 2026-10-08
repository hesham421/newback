# TM-B — tenant level 1: edit, suspension facts, platform admin-reset, usage

| | |
|---|---|
| Plan | `docs/plans/tenant-maturity-plan.md` §4 B.1–B.5 |
| Branch | `tm/b-tenant-level-1` (from `main` @ 5c8541c, which includes A, G, C3, D) |
| Migrations | `V18__tenant_profile.sql`, `V19__tenant_lifecycle.sql` (plan V16 / V17) |
| Date | 2026-10-08 |

## Summary

The platform operator can now edit a tenant's names and a small profile (`PUT /api/v1/platform/tenants/{id}`:
contact e-mail and phone, country, default language `ar`/`en`, time zone, notes; the code never changes),
must give a reason (3..500 characters) to suspend a tenant — the suspension records when, by whom and why,
and re-activation clears those facts and stores a token cut-off (`TOKENS_INVALID_BEFORE`, enforced by
package C.2) — can reset the password of a tenant's super administrator (`POST /{id}/admin-reset`: STAFF
user holding an active super role, password policy, every session ended, forced change by default,
`ADMIN_PASSWORD_RESET` audited in the target tenant, `STAFF_PASSWORD_CHANGED` mail), and can read a
tenant's usage figures (`GET /{id}/usage`). The admin-reset and the usage run inside the target tenant
(`TenantContext.callAs` + one `TransactionTemplate` transaction) through new cross-module methods of SEC
(`SecUserDirectoryApi` counts, new `SecAdminRecoveryApi`), FILE (`FileDocumentLookupApi.countDocuments /
sumBytes`) and NOTIF (`NotificationLogQueryApi.countDispatchedSince`); TENANT reads no other module's table.

## Analysis entries written (commit 899aac4, before any code)

| File | Section | Ids |
|---|---|---|
| `TENANT/P1/srs-tenant.md` | 1.3.0 addendum, package-B block B1–B11 | REQ/AC-TENANT-025 … 028, RULE-TENANT-016, -017; CHANGED RULE-TENANT-003, -004, REQ-TENANT-007; 3 error codes; ENT-TENANT-001 fields; lifecycle; dependencies; SCR-REQ-TENANT-001 |
| `TENANT/P1/registry-srs-tenant.md` | 1.3.0, package-B block | registry deltas, last sequences |
| `TENANT/P0/business-policies-tenant.md` | new 1.3.0 addendum | POL-TENANT-012, -013; CHANGED 001, 002, 006, 007 |
| `TENANT/P0/module-registry-tenant.md`, `platform-summary.md` | new 1.3.0 addenda | entity, lookup (`DEFAULT_LOCALE`), dependencies |
| `TENANT/P0_5/prd-tenant.md` | new 1.3.0 addendum | US-TENANT-009, -010, -011; CHANGED US-TENANT-002, -003 |
| `TENANT/P2/db-script-tenant.md`, `registry-db-tenant.md` | new 1.3.0 addenda | DBF-TENANT-033 … 042, `CHK_CORE_TENANT_LOCALE`, the two scripts |
| `SEC/P1/srs-sec.md` §10, `registry-srs-sec.md`, `P0/business-policies-sec.md` #9, `P0/module-registry-sec.md` | 1.3.0 | REQ/AC-SEC-090 (counts), REQ/AC-SEC-091 (recovery) |
| `FILE/P1/srs.md` §7, `registry-srs-file.md`, `P0/module-registry-file.md` | 1.3.0 | CHANGED XM-FILE-001 (+ counts) |
| `NOTIF/P1/srs.md` §4, `registry-srs-notif.md`, `P0/module-registry-notif.md` (new 1.3.0 section) | 1.3.0 | `NotificationLogQueryApi.countDispatchedSince` |
No ADR: no alternative needed one (the choices are in srs-tenant.md B10 and DEVIATIONS).

## Files changed

- Migrations: `erp-core/src/main/resources/db/migration/core/V18__tenant_profile.sql`, `V19__tenant_lifecycle.sql` (new).
- TENANT: `entity/Tenant.java`, `domain/TenantDomain.java`, `exception/TenantErrorCodes.java`, `dto/TenantResponse.java`,
  `dto/TenantStatusUpdateRequest.java`, new `dto/TenantUpdateRequest.java`, `TenantAdminResetRequest.java`,
  `TenantAdminResetResponse.java`, `TenantUsageResponse.java`, `mapper/TenantMapper.java`, `service/TenantService.java`,
  `controller/PlatformTenantController.java`.
- SEC: `crossmodule/SecUserDirectoryApi(Impl).java`, new `crossmodule/SecAdminRecoveryApi(Impl).java`, `RecoveryTarget.java`;
  `service/UserService.java`, `UserPasswordService.java`, `UserSessionTerminator.java`; `domain/UserDomain.java`;
  `permission/SecPermissions.java`; `repository/UserRepository.java`, `ActiveSessionRepository.java`.
- FILE: `crossmodule/FileDocumentLookupApi(Impl).java`, `repository/FileDocumentRepository.java`.
- NOTIF: `crossmodule/NotificationLogQueryApi(Impl).java`, `repository/NotificationLogRepository.java`.
- i18n: one `tenant-maturity B` block in `messages.properties` and `messages_ar.properties`.
- Tests: new `tenant/TenantProfileIntegrationTest`, `TenantAdminResetIntegrationTest`, `TenantUsageIntegrationTest`;
  `tenant/domain/TenantDomainTest` (+5); `PlatformTenantApiIntegrationTest`, `audit/AuditedEntitiesCoverageIntegrationTest`
  (suspension reason; audit field list); `erp-app-reference/.../ReferenceApplicationSmokeTest` (V18, V19).
- Docs: `docs/api-docs/**` (regenerated), `docs/api-docs/README.md`, `docs/test-api/core-test-plan.md`,
  `core_api_verify.py`, `docs/test-api/results/20261008T042452-P-LIVE{.json,-report.md}` (review round 1; replaces `20261008T035752`), `docs/CHANGELOG.md`,
  `docs/DEVIATIONS.md`, `governance/analysis/platform/PROJECT-OVERVIEW.md`, `project-registry.md`, this report.

## Decisions & deviations (all in `docs/DEVIATIONS.md` `[TM-B]` and srs-tenant.md B10)

1. Migrations V18 / V19 for the plan's V16 / V17 (reserved numbers).
2. Admin-reset body + optional `requireChangeAtNextLogin` (null = TRUE): **the reset forces a change at next login by
   default**, consistent with ADR-SEC-063 / RULE-SEC-058 (an operator-chosen password is an administrator-chosen one).
3. `SecAdminRecoveryApi` = `findRecoveryTarget` (facts) + `resetSuperUserPassword(username, raw, Boolean)`: the rule
   lives in `TenantDomain` (plan B.3), both calls share one transaction of the target tenant (check and write atomic).
4. NOTIF count on `NotificationLogQueryApi` (the read surface), not on `NotificationDispatchApi`.
5. `resetAdministratorPassword` / `getUsage` are not `@Transactional` (a PLATFORM transaction would bind the PLATFORM
   Hibernate session); each opens one transaction inside `callAs(id)` (`PermissionCatalogSynchronizer` precedent).
6. `TenantUsageResponse` = the plan's figures + `id`, not the eligibility `UsageResponse`.
7. `SecPermissions.PLATFORM_TENANT_MANAGE` (non-catalog constant, like `ROLE_CUSTOMER`) gates the SEC recovery
   methods — ArchUnit rule 5 forbids a literal or another module's `T(...)`.
8. `UserSessionTerminator` overload with an explicit actor: inside the target tenant the operator's username could
   name another user; the recovery records no actor user.
9. No `version` in the PUT body (erp-core convention); 409 only on a racing write.
10. Audit: `ADMIN_PASSWORD_RESET` is written by SEC in the **target tenant** (`TenantContext` = {id}), actor = the
    operator's username, realm STAFF, `actorUserId` null, entity `SEC_USER` / user id — the target tenant's own
    administrators see who recovered their account. The audit API records the tenant of the current context; an
    explicit `tenantId` was not needed because the call runs inside `callAs(id)`.
11. Re-applying a status changes nothing (facts and cut-off untouched); `TOKENS_INVALID_BEFORE` is set only on a real
    SUSPENDED → ACTIVE transition.
12. Code ↔ generator: `updateTenant` method name, `TenantDomain` as last helper parameter (generator limits).

Reference snapshot (package B): adopted after checking each row against the code; disagreements: the snapshot cites
RULE-TENANT-015 (not on `main`) for the allow-list → REQ-TENANT-007 CHANGED; its AUDIT / COMMON / EVENTS rows went to
the TENANT / SEC addenda (no such folders on `main`); its plan-literal `int resetSuperUserPassword(String, String)` →
item 3; "existing dispatch API" → item 4; `TOKENS_INVALID_BEFORE = now()` on every activation → only on a real
transition (item 11). Its facts section (TenantLookupApi has no `isActive`, the JWT filter exposes no `iat`) is
unchanged by B and left to C.1 / C.2.

## Acceptance checklist

| # | Item (plan §4 B.1–B.5, §10 DoD) | Evidence |
|---|---|---|
| 1 | B.1 V18 profile columns + `CHK_CORE_TENANT_LOCALE`, V19 lifecycle columns, additive | scripts; `MigrationNamingTest`; smoke test lists 2..19, 1000 |
| 2 | B.2 `PUT /{id}` (no code/status, 404, 409 lock) | `TenantProfileIntegrationTest` (3 update tests; the lock by `anUpdateFromAStaleCopy_failsTheOptimisticLock`, added in review round 1 — the first version of this report claimed it without a test); TC-028, -029 |
| 3 | B.2 PATCH status + reason, facts set/cleared, cut-off on activation, `TENANT_SUSPENSION_REASON_REQUIRED` | `TenantProfileIntegrationTest.suspension_…`, `TenantDomainTest`; TC-030 … 032 |
| 4 | B.2 admin-reset (STAFF super target, policy, sessions, audit, 404/422) | `TenantAdminResetIntegrationTest` (3 tests); TC-033 … 035 |
| 5 | B.2 usage via cross-module APIs inside `callAs`, never another module's tables | `TenantUsageIntegrationTest` (3 tests); TC-027, -036; ArchUnit `CrossModuleBoundaryArchTest` green |
| 6 | B.2 responses + search/sort allow-list (`contactEmail`, `countryCode`, `suspendedAt`) | TC-028, -031; JUnit search asserts |
| 7 | B.3 `TenantDomain` rules (reason; admin-reset target from SEC facts) | `TenantDomain.assertSuspensionReasonGiven`, `changesStatusTo`, `assertCanResetAdministrator`; `TenantDomainTest` |
| 8 | B.4 SEC / FILE / NOTIF cross-module additions; ArchUnit boundaries pass | crossmodule packages; `mvn verify` |
| 9 | B.5 TC-CORE-TENANT-* cases (update, code ignored, suspend 400, facts, cleared, 404, 422, sessions, fresh usage = 1 staff) | TC-CORE-TENANT-027 … 036 PASS |
| 10 | B.5 api-verify tenant; CHANGELOG; PROJECT-OVERVIEW tenancy paragraph | P-LIVE 182/182; `[TM-B]` lines; overview "Tenancy" bullet |
| 11 | DoD: analysis before code | commit 899aac4 precedes the first code commit 5572194 |
| 12 | DoD: code matches the entry (deviations in addendum + DEVIATIONS) | table below; `[TM-B]` |
| 13 | DoD: `mvn -q verify` green (ArchUnit, MigrationNamingTest, JaCoCo) | below |
| 14 | DoD: api-docs regenerated, `check_completeness.py` clean, every addendum endpoint present | 118/118, tenant = 8 |
| 15 | DoD: test plan extended, run archived | §4 TM-B row, §5.2, §6 (TENANT 37, total 204, P-LIVE 182), §9; `results/20261008T042452-P-LIVE*` |
| 16 | DoD: CHANGELOG `[Unreleased]` | Added (2 lines), Changed (1 line) |
| 17 | DoD frontend items | not in this package (plan §8 F3) |
16/16 backend items met (item 17 belongs to package F).

## Code ↔ addendum check

| Addendum item | Code | Match |
|---|---|---|
| `PUT /api/v1/platform/tenants/{id}`, `PLATFORM_TENANT_MANAGE`, 404 / 400 / 409 | `PlatformTenantController.updateTenant`, `TenantService.update` (409 = the shared handler's answer to the `VERSION` lock, tested at repository level) | yes |
| `PATCH /{id}/status` + `reason`, order: validation → tenant → PLATFORM 422 → reason 400 | `TenantService.updateStatus` | yes |
| `POST /{id}/admin-reset`, body `username` (≤100), `newPassword` (≤200), `requireChangeAtNextLogin`; response `username`, `sessionsTerminated`; order tenant → exists → super → policy | `TenantService.resetAdministratorPassword` / `resetInsideTenant`, `UserPasswordService.resetSuperUserPassword` | yes |
| `GET /{id}/usage` → `id, staffUsers, customerUsers, activeSessions, fileDocuments, fileBytes, notificationsLast30Days, collectedAt` | `TenantUsageResponse`, `TenantService.getUsage` | yes |
| error codes / statuses: `TENANT_SUSPENSION_REASON_REQUIRED` 400, `TENANT_ADMIN_NOT_FOUND` 404, `TENANT_ADMIN_NOT_SUPER` 422 (args username, code) | `TenantErrorCodes`, `TenantDomain`, both bundles; api-docs bind all three | yes |
| DBF-TENANT-033 … 042: names, types, widths, nullability; `CHK_CORE_TENANT_LOCALE` | V18, V19, `Tenant` `@Column` lengths | yes |
| migration numbers V18 / V19 | files | yes |
| profile formats (email, phone pattern, `^[A-Z]{2}$`, `ar`/`en`, IANA format, ≤ 1000), empty → NULL, trim, upper-case country | `TenantUpdateRequest`, `Tenant.normalize` | yes |
| allow-list + `contactEmail`, `countryCode`, `suspendedAt` (ISO instant, malformed 400) | `TenantService.ALLOWED_SORT_FIELDS`, `InstantFieldValueConverter` | yes |
| SEC counts (STAFF / CUSTOMER any status; open sessions either realm), gate `isAuthenticated()` | `UserService.countStaff/countCustomers/countActiveSessions`, repositories | yes |
| `SecAdminRecoveryApi` signatures, gate `SecPermissions.PLATFORM_TENANT_MANAGE`, `RecoveryTarget` | crossmodule + `UserPasswordService` | yes (addendum amended before the first code commit) |
| audit `ADMIN_PASSWORD_RESET` (target tenant, actor operator, `actorUserId` null, `SEC_USER`), sessions without actor user, `UserPasswordChangedEvent` | `UserPasswordService`, `UserSessionTerminator` overload | yes |
| FILE counts (not DELETED; sum `FILE_SIZE`, 0) / NOTIF `CREATED_AT >= since` | repositories + impls | yes |
| status lifecycle (re-apply = nothing) | `TenantDomain.changesStatusTo` | yes |
| PRD success metric US-TENANT-010 | pointed at TC-034 in the check commit | fixed |

## Verification output

- `mvn -q verify` (clean `target/`, code `a923eb4`): BUILD SUCCESS, JaCoCo met. erp-core **572** tests / 0 failures /
  0 errors / 0 skipped (86 suites; D ended at 543 / 82); erp-app-reference **10** / 0 / 0 / 0. The run before the
  generator-readable rename gave the same totals.
- HTTP suite (first delivery): run `26100803579D`, 181 PASS / 0 FAIL — superseded by the review-round-1 run below.
- api-docs: whole app regenerated (`update`, `--base http://localhost:18105 --server-url http://localhost:7272`).
  `check_completeness.py`: `per module: app=1, audit=1, cu=5, file=14, mdl=11, notif=18, report=4, sec=50, sequence=6,
  tenant=8 (sum 118) missing=0 duplicated=0 stale=0 RESULT: PASS`. `check`: SEC, TENANT, MDL, SEQUENCE, REPORT PASS;
  FILE, NOTIF (permissions), CU (unique-constraints), AUDIT (business-errors), APP — the five known generator
  limitations, unchanged. Removed diff lines are only the tenant `getById` id, the PATCH description / example and
  the `TENANT_NOT_FOUND` endpoint count (2 → 5): no row lost. Generator unit tests: `Ran 65 tests … OK`.

## Skills checked

`gov-enforce-backend-contract` (Domain: one `TenantDomain`, no annotations, facts passed in; deviations A.3.12,
A.5.3/A.5.4 recorded), `build-create-entity` (fields, `@PrePersist`/`@PreUpdate` normalisation, field-only
transitions), `build-create-repository` (count queries with callers), `build-create-dto`, `build-create-mapper`
(update mapping skips code/status), `build-create-service` (cross-module only through `crossmodule`, gates,
`ServiceResult`), `build-create-controller` (thin, `@Valid`, `@Operation`), `gov-enforce-error-handling` (codes in
`TenantErrorCodes`, both bundles), `gov-enforce-caching-rules` (no caching), `gov-validate-backend-feature` (no raw SQL
added), `api-verify` (cases in the plan's format).

## Notes for later steps

- **C.2** can enforce `CORE_TENANT.TOKENS_INVALID_BEFORE` (set on every SUSPENDED → ACTIVE transition; NULL for
  tenants never re-activated). `PlatformTenantApiIntegrationTest.suspend_…andActivateRestoresThem` asserts that a token
  issued before a suspension works again after re-activation — C.2 must change that expectation (behaviour change,
  analysis first). `Tenant.activate(Instant)` is where a revoke-tokens endpoint can reuse the field write.
- **C.1** can publish `TenantSuspendedEvent(tenantId, code, reason, actor)` from `TenantService.updateStatus` inside the
  `domain.changesStatusTo(..)` branch (the reason is `entity.getSuspensionReason()`); SEC can end the tenant's sessions
  with `UserSessionTerminator` per user, or a new bulk query.
- **E (branding)** reuses: `TenantContext.callAs(id, () -> new TransactionTemplate(tm).execute(...))` for writes inside
  another tenant (the generator reads a helper's `TenantDomain` parameter only when it is the last one);
  `TenantMapper.toResponse` / `TenantResponse` to add `logoUrl`, `brandColor`; `TenantUpdateRequest` stays the
  profile-only PUT (logo has its own endpoints). Name controller methods uniquely (`updateTenant`, …) to keep
  springdoc ids stable.
- HTTP suite: tenant D (`$TD`, `td-admin`) exists from TENANT-027 on; it is the only tenant besides C that is suspended
  (and re-activated). Never add staff users to tenant A.
- Open: the generator does not walk
  cross-module calls, so admin-reset's `SEC-400-PASSWORD-POLICY` lives in the `@Operation` description.
- Next free ids: TENANT REQ/AC-029, RULE-018 (012 … 015 reserved), POL-014, US-012, ENT-002, SCR-REQ-002, XM-003,
  DBF-043, ADR-TENANT-002 (reserved for C.2); SEC REQ-092, AC-098, RULE-063, ENT-015, DBF-124, XM-007, ADR-069
  (065 spare, 066 … 068 other session); FILE RULE-011, XM-003, ADR-009; NOTIF RULE-024, XM-004. HTTP: TC-CORE-TENANT-038,
  TC-CORE-SEC-056, TC-CORE-PLATFORM-005; test-plan counts TENANT 37, total 204, P-LIVE 182 (after review round 1).

## Review round 1

Verdict PASS with five LOW findings (evidence `rev-b/`, `probe_b.py`); fixed on the same branch, analysis first
(commit `01c4b82` edits the package-B block, not yet on `main`), no rebase.

| # | Finding | Fix | Evidence |
|---|---|---|---|
| 1 | LOW — admin-reset on PLATFORM could target the caller's own account (200, `sessionsTerminated` 3), bypassing SEC RULE-SEC-057 and the current-password check | RULE-TENANT-017 CHANGED: `TenantDomain.assertAdminResetAllowed` refuses tenant id 1 with 422 **`TENANT_ADMIN_RESET_PLATFORM`** (AR + EN), checked after the tenant lookup and before any SEC call; platform operators use `PUT /api/v1/sec/users/{id}/password`. A dedicated code because `TENANT_PLATFORM_PROTECTED`'s message is "cannot be suspended". The "open" note left B10 and DEVIATIONS | `TenantDomainTest.adminReset_isNeverAllowedOnThePlatformTenant`; `TenantAdminResetIntegrationTest.unknownOrNonSuperTargets_…` (operator's own name and another name on PLATFORM → 422, operator still signs in with the old password); HTTP TC-CORE-TENANT-037 |
| 2 | LOW — no PLATFORM audit trace of an admin-reset | `TenantService.resetAdministratorPassword` records **`TENANT_ADMIN_RESET`** through `AuditApi` in PLATFORM after the target tenant's transaction committed (outside `callAs`, own commit): actor the operator, entity `CORE_TENANT` / {id}, summaries with the tenant code, the target username and `sessionsTerminated`, no secret. Addendum B8 (2), B7, REQ/AC-TENANT-027, POL-TENANT-013, module registry | `TenantAdminResetIntegrationTest.aReset_…`: one PLATFORM row with those fields, no secret, and the target tenant still has exactly one `ADMIN_PASSWORD_RESET` row; the refusal test: no PLATFORM row; HTTP TC-CORE-TENANT-037 |
| 3 | LOW — springdoc `getById_N` ids of FILE / NOTIF shifted | `PlatformTenantController.getById` → **`getTenantById`**; api-docs regenerated. Operation-id diff against 5c8541c (all 115 pre-existing operations): unchanged except tenant `GET /{id}` (`getById_6` → `getTenantById`, the rename itself) and NOTIF `GET /notifications/logs/{id}` (`getById_7` → `getById_6`: the tenant method no longer takes a place before it in springdoc's `getById` numbering; keeping `_7` would need a pinned `operationId` freezing a generated suffix, not done). File categories, notification channels and templates are back to `getById_5` / `_4` / `_3` | `docs/api-docs/**`; DEVIATIONS `[TM-B]` |
| 4 | LOW — the 409 lock of `PUT /{id}` was claimed tested | `TenantProfileIntegrationTest.anUpdateFromAStaleCopy_failsTheOptimisticLock`: two copies read, the first saved through the PUT's mapping, the stale one fails with `ObjectOptimisticLockingFailureException` (the `TenantScopedQueryIntegrationTest` repository-level pattern; `GlobalExceptionHandler` maps it to 409 `CONCURRENT_MODIFICATION`); the report rows corrected | the test |
| 5 | LOW — usernames in logs | `TenantService` logs the tenant id only on the admin-reset path; `UserPasswordService.findRecoveryTarget` logs no name (the reset itself already logged the user id) | code |

Verification after the fixes:
- `mvn -q verify` (clean `target/`): BUILD SUCCESS, JaCoCo met. erp-core **574** / 0 / 0 / 0 (86 suites); erp-app-reference
  **10** / 0 / 0 / 0.
- P-LIVE run **`2610080424DF`**, port 18105, fresh `erp_tm_b` (dropped afterwards): **182 PASS, 0 FAIL, 0 BLOCKED** (22 profile
  cases not run) — `docs/test-api/results/20261008T042452-P-LIVE.json` / `-report.md`, replacing run `26100803579D`.
  Test plan: TENANT 37, total 204, P-LIVE 182.
- api-docs regenerated; `check_completeness`: 118/118, 0 missing / duplicated / stale; `check` verdicts unchanged (SEC,
  TENANT, MDL, SEQUENCE, REPORT pass; the five known limitations). The admin-reset endpoint binds
  `TENANT_ADMIN_RESET_PLATFORM`, `TENANT_ADMIN_NOT_FOUND`, `TENANT_ADMIN_NOT_SUPER`.
