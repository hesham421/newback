# Tenant-maturity package C3 — automated tenant-isolation tests — report

## Summary

Package C3 (plan §5 C.3, item 12) turns the row-level isolation guarantee (REQ-TENANT-016, RULE-TENANT-008)
into checks that fail the build or a review:

- **ArchUnit `TenantScopedEntityTest`**: every production `@Entity` under `com.erp` extends `AuditableEntity`
  and carries `@TenantId`, except an explicit global list. A listed class must exist and carry no
  `@TenantId`. The failure message tells the developer to add a truly global entity to the list explicitly,
  with an analysis entry.
- **HTTP isolation matrix**: one row per core module in each of two freshly provisioned tenants, all through
  the real security chain. Modules: SEC role, MDL lookup type, FILE document, NOTIF template, CU configuration
  override, SEQUENCE number series, AUDIT event. Each tenant's search or list returns only its own rows, a
  filter on the other tenant's code finds nothing, and the other tenant's id answers 404 with the module's
  not-found code.
- **Governance rule** (RULE-TENANT-011): every `JdbcTemplate` / native SQL statement on a tenant-scoped table
  names `TENANT_ID`. It is a Governance Rules item and an automatic-rejection item of
  `gov-validate-backend-feature`. An audit of the current code found 15 such statements, all compliant (table
  below).

The package adds tests and governance only. There is no change to `src/main`, no endpoint, no migration and no
api-docs change.

## Analysis entries written

| File | Section | Ids |
|---|---|---|
| `governance/analysis/modules/TENANT/P1/srs-tenant.md` | `## Implementation Addendum — erp-core 1.3.0` (opened by C3): §1 requirement, §2 rules, §3 verified as-built facts (21 tenant-scoped entities; 15 raw SQL statements), §4 tests/rules, §5 frontend impact (none) | REQ-TENANT-024, AC-TENANT-024, RULE-TENANT-010, RULE-TENANT-011 |
| `governance/analysis/modules/TENANT/P1/registry-srs-tenant.md` | `## Implementation Addendum — erp-core 1.3.0`: registry deltas, counts, last sequence | same |

Id check: the highest TENANT ids ever issued were REQ/AC-023 and RULE-009. This was checked in the current tree,
in `git log -p --all` of newback (other branches only carry the tm-a report's "next free" note), and in the full
history of `governance-shared`, which has no REQ/AC/RULE-TENANT ids (only GAP-TENANT-00x and CORE-TENANT-001).
**Next free after C3: REQ/AC-TENANT-025, RULE-TENANT-012** (others unchanged: POL 012, US 009, ENT 002,
SCR-REQ 002, XM 003, DBF 033, ADR-TENANT-002).

Global set, each class verified in the code (`extends GlobalAuditableEntity`, no `@TenantId` field):
`com.erp.tenant.entity.Tenant` (Tenant.java:40), `com.erp.sec.entity.ModuleRegistry` (:36), `ScreenRegistry`
(:45), `ActionRegistry` (:45), and `com.erp.cu.entity.AppConfiguration` (:41). `AppConfiguration` has a plain,
nullable `TENANT_ID` column (:50) and no `@TenantId`.

## Files changed

| Commit | Files |
|---|---|
| ff4121a docs(analysis) | `governance/analysis/modules/TENANT/P1/srs-tenant.md`, `registry-srs-tenant.md` |
| efcf437 test(architecture) | NEW `erp-core/src/test/java/com/erp/architecture/TenantScopedEntityTest.java`; `CoreLibraryRulesArchTest.java` (rule 2 reads the same list) |
| bcb40f4 test(tenant) | `erp-core/src/test/java/com/erp/tenant/TenantIsolationIntegrationTest.java` (+7 tests, helpers); `TenantHttp.java` (+`put`, +`uploadPng`) |
| f4ddda9 docs(governance) | `governance/rules/GOVERNANCE-RULES.md` (Governance Rules +1 item); `.claude/skills/gov-validate-backend-feature/SKILL.md` (automatic rejection +1 item) |
| 9f28a87 test(api) | `docs/test-api/core-test-plan.md` (§9 +1 row, §4/§6 unchanged); NEW `docs/test-api/results/20261008T003132-P-LIVE.json`, `…-P-LIVE-report.md` |
| 67fe979 docs(check) | `docs/CHANGELOG.md` (`[TM-C3]` under Unreleased → Added); `docs/DEVIATIONS.md` (`## [TM-C3]`, 5 entries) |
| 0e50353 docs(steps) | NEW `docs/steps/tm-c3-report.md` |
| b1aafc0 docs(analysis), review round 1 | `srs-tenant.md` (N1 wording) |
| (this commit) | review round 1: run files replaced, this report |

No migration used (C3 has no reserved number). Nothing deleted.

## Decisions & deviations

All are recorded in `docs/DEVIATIONS.md` under `[TM-C3]`.

1. The plan's `TenantIsolationIT` → the existing step-05 `TenantIsolationIntegrationTest` is extended. The repo
   names integration tests `*IntegrationTest`, and a class with exactly that name already existed.
2. Cross-tenant 404 where a module has no read-by-id endpoint:
   - MDL: asserted on `PUT /lookup-types/{id}` (`MDL-404-TYPE`), and B's row is checked to be unchanged.
   - AUDIT: has no by-id endpoint, so the test asserts that the `entityId` filter answers an empty page to the
     other tenant.
   - CU: addresses rows by key, so B's key answers 404 `APP_CONFIGURATION_NOT_FOUND`.
3. The review rule went into `gov-validate-backend-feature`'s "Automatic rejection" list, not into a scored
   Stage 3 list, so the 148-point scale is unchanged.
4. One global-entity list: `CoreLibraryRulesArchTest.GLOBAL_ENTITIES` now refers to
   `TenantScopedEntityTest.GLOBAL_ENTITIES` (same set, same assertion). `TenantSchemaIntegrationTest` keeps its
   runtime list of simple names.
5. No TC-CORE cases, §4 row or §6 counts: there is no HTTP behaviour change (orchestrator instruction). §9 names
   both classes.

## Acceptance checklist

| # | Item (plan §5 C.3 + orchestrator brief) | Status | Evidence |
|---|---|---|---|
| 1 | ArchUnit `TenantScopedEntityTest`: every `@Entity` extends `AuditableEntity` / carries `@TenantId`, except the documented global set; the message says to add a global entity explicitly | ✅ | `TenantScopedEntityTest` 2/2 green; global list = 5 classes verified in the code |
| 2 | The rule bites | ✅ | temporary probe, not committed (output below) |
| 3 | Integration test: two tenants, one row per module in SEC, MDL, FILE, NOTIF, CU, SEQUENCE, AUDIT; search/getById sees only its own rows; cross-tenant id → 404 | ✅ | `TenantIsolationIntegrationTest` 13/13 green (6 step-05 + 7 new) |
| 4 | Review rule in GOVERNANCE-RULES.md + `gov-validate-backend-feature` checklist item | ✅ | commit f4ddda9 |
| 5 | Audit of current `JdbcTemplate` / native SQL | ✅ | 15 statements, 0 violations (table below) |
| 6 | Full P-LIVE HTTP suite, no regression, archived | ✅ | RUN 261008003190, 156/156 PASS |
| 7 | `core-test-plan.md` §9 row naming the two classes; §6 untouched | ✅ | commit 9f28a87; the run report's JUnit verification: 0 names not found |
| 8 | CHANGELOG `[TM-C3]`, DEVIATIONS, this report | ✅ | 67fe979, this commit |
| DoD 1 | Analysis entries before the first code commit | ✅ | ff4121a precedes efcf437 |
| DoD 2 | Code matches the entry; deviations in addendum + DEVIATIONS | ✅ | table below; the addendum already states the MDL/AUDIT/CU 404 forms |
| DoD 3 | `mvn -q verify` green (ArchUnit incl. `TenantScopedEntityTest`, `MigrationNamingTest`, JaCoCo ≥ 60 %) | ✅ | erp-core 408/0/0/0, erp-app-reference 10/0/0/0; lines 78.83 % |
| DoD 4 | api-docs regenerated, `check_completeness.py` clean | n/a | no endpoint/DTO change (orchestrator: no regeneration) |
| DoD 5 | test plan extended; api-verify run archived | ✅ | §9 row; run archived under `docs/test-api/results/` |
| DoD 6 | CHANGELOG line | ✅ | `[Unreleased]` → Added |
| DoD 7 | Frontend items | n/a | backend-only package; addendum §5: no frontend impact |

Proof that the rule bites. The test was run against two temporary probe entities in `com.erp.cu.entity`: one
plain `@Entity`, and one extending `GlobalAuditableEntity` but not listed. `GLOBAL_ENTITIES` also temporarily
held `com.erp.sec.entity.Role` and `com.erp.nope.Missing`. All of this was reverted before the commit.

```
Architecture violated — every @Entity extends AuditableEntity (@TenantId on TENANT_ID), except [...] (2 times):
  com.erp.cu.entity.C3ProbePlainEntity is not tenant-scoped: extend AuditableEntity (TENANT_ID as @TenantId) — or, only if it is truly global, add it explicitly to TenantScopedEntityTest.GLOBAL_ENTITIES with an analysis entry (TENANT RULE-TENANT-010) that says why
  com.erp.cu.entity.C3ProbeGlobalEntity is not tenant-scoped: extend AuditableEntity (...) [same message]
Architecture violated — GLOBAL_ENTITIES lists only existing entities without @TenantId (2 times):
  com.erp.sec.entity.Role is listed as a global entity but carries @TenantId — remove it from GLOBAL_ENTITIES
  com.erp.nope.Missing is listed as a global entity but is no @Entity — remove it from GLOBAL_ENTITIES
```

## Code ↔ addendum check

| Addendum item | Code | Match |
|---|---|---|
| RULE-TENANT-010 global set (5 FQCNs) | `TenantScopedEntityTest.GLOBAL_ENTITIES` | ✅ identical |
| RULE-TENANT-010 message ("add it explicitly … with an analysis entry") | `entitiesWithoutTenantId` message | ✅ |
| AC-TENANT-024 (second And): listed class with `@TenantId` or no entity fails | `staleGlobalEntries` | ✅ |
| §3: 21 tenant-scoped entities | ArchUnit passes; `TenantSchemaIntegrationTest` asserts 21 | ✅ |
| AC-TENANT-024: SEC `GET /api/v1/sec/roles/{id}` → 404 `SEC-404-ROLE` | `secRoles_…` | ✅ |
| MDL `PUT /api/v1/mdl/lookup-types/{id}` → 404 `MDL-404-TYPE`, B unchanged | `mdlLookupTypes_…` | ✅ |
| FILE `GET /api/v1/files/{id}` → 404 `FILE_DOCUMENT_NOT_FOUND` | `fileDocuments_…` | ✅ |
| NOTIF `GET /api/v1/notifications/templates/{id}` → 404 `NOTIF_TEMPLATE_NOT_FOUND` | `notifTemplates_…` | ✅ |
| CU `GET /api/v1/common/configurations/{key}` → 404 `APP_CONFIGURATION_NOT_FOUND` | `cuConfigurations_…` | ✅ |
| SEQUENCE `GET /api/v1/sequence/series/{id}` → 404 `NUMBER_SERIES_NOT_FOUND` | `sequenceNumberSeries_…` | ✅ |
| AUDIT `GET /api/v1/audit/events?entityId=<B>` → empty for A, B finds its row | `auditEvents_…` | ✅ |
| RULE-TENANT-011 text | GOVERNANCE-RULES.md item; skill automatic-rejection item | ✅ |
| §3: 15 raw SQL statements, each names TENANT_ID | audit below | ✅ |
| Cited line ranges (AuditableEntity:35-37, AuditEventStore:43-46/:98-103, NotificationRequeueJob:43-44/:77-81, entity class lines) | re-read at the branch head | ✅ |

## JdbcTemplate / native SQL audit (current code, erp-core `src/main`; erp-app-reference has none)

There is no `@Query(nativeQuery = true)`, no `@NativeQuery`, no `createNativeQuery`, and no `JdbcClient` or
`NamedParameterJdbcTemplate`. `com.erp.autoconfigure.ErpCoreNotifAutoConfiguration` only passes a `JdbcTemplate`
into a bean, with no SQL.

| # | Location | Statement | Tenant-scoped tables | Names TENANT_ID | Verdict |
|---|---|---|---|---|---|
| 1 | sec/tenant/SecTenantProvisioningContributor.java:75 | INSERT … SELECT roles | SEC_ROLE | insert value + `r.TENANT_ID = ?` (source) | ✅ |
| 2 | :84 | INSERT … SELECT module grants | SEC_ROLE_MODULE_GRANT, SEC_ROLE (×2) | value; `g.`, `sr.`, `tr.TENANT_ID = ?` (global SEC_MODULE_REG joined) | ✅ |
| 3 | :96 | INSERT … SELECT screen grants | SEC_ROLE_SCREEN_GRANT, SEC_ROLE (×2) | value; `g.`, `sr.`, `tr.TENANT_ID` | ✅ |
| 4 | :109 | INSERT … SELECT action grants | SEC_ROLE_ACTION_GRANT, SEC_ROLE (×2) | value; `g.`, `sr.`, `tr.TENANT_ID` | ✅ |
| 5 | :124 | INSERT administrator | SEC_USER | value | ✅ |
| 6 | :131 | INSERT … SELECT admin role | SEC_USER_ROLE, SEC_USER, SEC_ROLE | value; `u.TENANT_ID = ?`, `r.TENANT_ID = u.TENANT_ID` | ✅ |
| 7 | mdl/tenant/MdlTenantProvisioningContributor.java:33 | INSERT … SELECT lookup types | MDL_LOOKUP_TYPE | value; `t.TENANT_ID = ?` | ✅ |
| 8 | :42 | INSERT … SELECT lookup values | MDL_LOOKUP_VALUE, MDL_LOOKUP_TYPE (×2) | value; `v.`, `st.`, `tt.TENANT_ID` | ✅ |
| 9 | notif/tenant/NotifTenantProvisioningContributor.java:34 | INSERT … SELECT channels | NOTIF_CHANNEL_CONFIG | value; `c.TENANT_ID = ?` | ✅ |
| 10 | :42 | INSERT … SELECT templates | NOTIF_TEMPLATE | value; `t.TENANT_ID = ?` | ✅ |
| 11 | sequence/tenant/SequenceTenantProvisioningContributor.java:34 | INSERT … SELECT series (first row per code) | CORE_NUMBER_SERIES (×2) | value; `s.TENANT_ID = ?`, `x.TENANT_ID = ?` | ✅ |
| 12 | audit/service/AuditEventStore.java:43-46 (run at :55/:63) | INSERT audit event | CORE_AUDIT_EVENT | value (`entry.getTenantId()`) | ✅ |
| 13 | :98-99 | SELECT DISTINCT TENANT_ID … OCCURRED_AT < ? | CORE_AUDIT_EVENT | selected column: deliberate cross-tenant retention scan, then per-tenant delete (#14) | ✅ (cross-tenant scan form) |
| 14 | :102-103 | DELETE … WHERE TENANT_ID = ? AND OCCURRED_AT < ? | CORE_AUDIT_EVENT | predicate | ✅ |
| 15 | notif/service/NotificationRequeueJob.java:43-44 (run at :77) | SELECT DISTINCT TENANT_ID FROM NOTIF_LOG WHERE status = ? | NOTIF_LOG | selected column: cross-tenant scan, then JPA per tenant via `TenantContext.callAs` | ✅ (cross-tenant scan form) |

Findings: **no violation, no isolation bug**, so nothing needed fixing. Observation, not a defect: #13 and #15
are system-job discovery scans across all tenants by design (step 08/10). RULE-TENANT-011 names this form
explicitly so a reviewer does not flag it.

## Verification output

- `mvn -q verify` from the repo root, after deleting every `target/` in the worktree: exit 0.
  - erp-core: tests 408, failures 0, errors 0, skipped 0.
  - erp-app-reference: tests 10, failures 0, errors 0, skipped 0.
  - JaCoCo erp-core lines 78.83 % (gate 60 %).
- HTTP suite, profile P-LIVE, full: app built from this branch, port 18104, fresh database `erp_tm_c3` (dropped
  afterwards), bootstrap password `Test1234`.
  - RUN `261008003190`: **156 PASS / 0 FAIL / 0 BLOCKED** (the 22 cases of other profiles show as SKIPPED in
    the report).
  - Archived: `docs/test-api/results/20261008T003132-P-LIVE.json` and `…-P-LIVE-report.md`. The report was
    written with G's `--instance` / `--code-under-test` / `--report-out` flags (after the rebase onto
    origin/main); `core-verify-report.md` is untouched.
  - §9 JUnit names not found: 0.
- `check_completeness.py`: not run. There was no api-docs regeneration (no endpoint change).

## Skills checked

`gov-validate-backend-feature` (extended by this package). `gov-enforce-backend-contract` and the `build-*` lane
were not applicable: there is no production code. `api-verify` conventions were followed for the run archive.
Repository rules followed: `GOVERNANCE-RULES.md`, `CLAUDE.md` (analysis first; tests explicitly requested by the
plan owner).

## Notes for later steps

- Next free TENANT ids: **REQ/AC-TENANT-025, RULE-TENANT-012**. The `## Implementation Addendum — erp-core 1.3.0`
  is now open in `srs-tenant.md` and `registry-srs-tenant.md`; B, E and C append to it.
- A new global entity (for example a later platform-wide table) must be added to
  `TenantScopedEntityTest.GLOBAL_ENTITIES`, which is the one list rule 2 also reads, with an analysis entry under
  RULE-TENANT-010. `TenantSchemaIntegrationTest` keeps its own simple-name list and the counts 21 / 22 / 14.
- A new tenant-scoped entity needs no test change. If its module gains a list and a by-id endpoint, add its row
  to the `TenantIsolationIntegrationTest` matrix with the `assertSearchSeesOnlyItsOwnRows` / `assertNotFound`
  helpers.
- `TenantHttp` now offers `put()` and `uploadPng()` for tenant-package tests.
- C.6 (`ScopedValue` spike) must pass `TenantIsolationIntegrationTest`. That is this class; the plan calls it
  `TenantIsolationIT`.
- Archive a package's HTTP run with `--instance` / `--code-under-test`, then `--report <json> --report-out
  docs/test-api/results/<ts>-P-LIVE-report.md`. Without them the report carries stale defaults (review round 1).

## Review round 1

Verdict: FAIL on one docs finding (code, tests and analysis passed) plus nit N1. Fixes:

1. **Rebased onto origin/main** (now includes package G). Conflicts arose only in the append blocks, and both
   sides were kept, TM-G first: `docs/test-api/core-test-plan.md` §9 (the TM-G row, then the TM-C3 row),
   `docs/CHANGELOG.md` (the `[TM-G]` line, then `[TM-C3]`) and `docs/DEVIATIONS.md` (`## [TM-G]`, then
   `## [TM-C3]`). There were no other conflicts.
2. **Stale run report replaced.** The first archived report (`20261008T001118-P-LIVE-report.md`, RUN
   `261008001171`) carried `core_verify_report.py`'s defaults: "erp-core 1.0.0", branch `step/14-api-verify`,
   port 7272, database `erp_phase_d2`. Both files of that run were removed with `git rm`. The full P-LIVE suite
   was then re-run on the rebased branch:
   - setup: fresh `erp_tm_c3` on port 18104 (dropped afterwards), with
     `--instance "erp-app-reference jar, port 18104, fresh PostgreSQL 16 database erp_tm_c3, profile dev, P-LIVE"`
     and `--code-under-test "branch tm/c3-tenant-isolation-tests at b1aafc0, 1.3.0-SNAPSHOT"`;
   - result: RUN `261008003190`, **156/156 PASS** (150 + G's 6), 0 FAIL, 0 BLOCKED;
   - §9 JUnit names not found: 0;
   - archived as `docs/test-api/results/20261008T003132-P-LIVE.json` + `-P-LIVE-report.md` (via `--report-out`).
3. **N1:** REQ-TENANT-024 and AC-TENANT-024 now say "every JPA entity of erp-core" (the scan covers erp-core's
   classpath; the reference application declares no entity), aligned with RULE-TENANT-010. The edit is inside
   C3's own 1.3.0 section.
4. **`mvn -q verify`** on the rebased branch, with every `target/` deleted first: exit 0.
   - erp-core: 408 tests, 0 failures, 0 errors, 0 skipped.
   - erp-app-reference: 10 tests, 0 failures, 0 errors, 0 skipped.
   - JaCoCo erp-core lines: 78.83 %.
