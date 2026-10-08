# TM-Z — closure of the tenant-maturity plan (backend)

| | |
|---|---|
| Plan | `docs/plans/tenant-maturity-plan.md` §10 (DoD), §11 (migrations); state `docs/plans/tenant-maturity-state.json` step Z |
| Branch | `tm/z-closure` from `origin/main` `e643d91` (A, G, C3, D, B, E, C12, C6, C4, C5 merged) |
| Migrations | none (V16 … V22 checked) |
| Date | 2026-10-08 |

## Summary

The closure fixes two carry-overs, checks the whole repository for consistency after ten merges, verifies the merged
code end to end, and hands package F to the frontend.

- **Carry-overs.** `FileDocumentDomain.from()`'s Javadoc is restored above `from()` (comment only). The `LOCAL` / `S3`
  purge failure of RULE-FILE-012 is recorded as a known limitation: the object stays, the failure is only logged at
  WARN, nothing retries it (FILE `P1/srs.md` 1.3.0 §10, registry note, DEVIATIONS `[TM-Z]`).
- **Consistency.** All 32 analysis files with a 1.3.0 addendum have one heading and package blocks in execution
  order, and no id is defined twice. Two block boundaries damaged by the C6/C4 merges are repaired. Package E's four
  citations of the reserved RULE-TENANT-012 now point to REQ-TENANT-011. Every referenced ADR exists and is ACCEPTED,
  except ADR-TENANT-004, which is REJECTED as intended. Stale counts and pointers are corrected in `PROJECT-OVERVIEW.md`,
  `project-registry.md`, `governance/README.md`, the core migration README and `docs/RELEASE.md`. `docs/CHANGELOG.md`
  `[Unreleased]` is now one section in package order, with a "Behaviour changes" list.
- **Verification on the merged code.**
  - `mvn -q verify` from a clean `target/`: 669 + 10 tests.
  - api-docs: `review` shows no drift (125 operations), `check_completeness` PASS, `check` shows only the 5 known
    generator limitations, and the 71 generator unit tests pass.
  - The full HTTP suite passes in every profile: 226/226 (P-LIVE 204/204).
  - No product defect was found.
- **Handover and reports.** `docs/steps/tm-frontend-handover.md` (F1–F4) and `docs/steps/tm-plan-report.md` (per-package
  summary, totals, follow-ups, release recommendation).

## Analysis entries written

| File | Section | Content |
|---|---|---|
| `governance/analysis/modules/FILE/P1/srs.md` | 1.3.0 §10 "Closure (tenant-maturity Z)" | NOTE: known limitation of RULE-FILE-012 on `LOCAL` / `S3` (no id minted) |
| `governance/analysis/modules/FILE/P1/registry-srs-file.md` | 1.3.0 closure registry note | same |
| `governance/analysis/modules/TENANT/P1/srs-tenant.md` | E block (E5 note, REQ-TENANT-032 statement, E rules table, config row) | `RULE-TENANT-012` → `REQ-TENANT-011` (citation fix); blank line at the C6 → C4 boundary |
| `governance/analysis/modules/TENANT/P1/registry-srs-tenant.md` | E block | the same citation fix |
| `governance/analysis/modules/TENANT/P0/platform-summary.md` | C6 → C4 boundary | blank line (Markdown table repair) |

## Files changed

- Code (comments only): `erp-core/src/main/java/com/erp/file/domain/FileDocumentDomain.java` (Javadoc moved back above
  `from()`); `erp-app-reference/src/test/java/com/erp/app/ReferenceApplicationSmokeTest.java` (comment "V2..V21" →
  "V2..V22").
- Resource docs: `erp-core/src/main/resources/db/migration/core/README.md` (chain table + V16 … V22).
- Analysis: the five files above.
- Platform docs: `governance/README.md`, `governance/analysis/platform/PROJECT-OVERVIEW.md`,
  `governance/analysis/platform/project-registry.md`.
- Docs: `docs/CHANGELOG.md`, `docs/RELEASE.md`, `docs/DEVIATIONS.md` (`## [TM-Z]`, 5 lines), `docs/test-api/core-test-plan.md` (TENANT-043 trace).
- HTTP run archive (new): `docs/test-api/results/20261008T112525-P-LIVE.json`, `-P-LIVE-report.md`,
  `-all-profiles-report.md`, `20261008T113405-P-MAIL.json`, `20261008T113441-P-MAIL-DOWN.json`,
  `20261008T113547-P-CAP.json` (cap 2), `20261008T113608-P-CAP.json` (cap 3), `20261008T113630-P-LOCAL.json`.
- New reports: `docs/steps/tm-frontend-handover.md`, `docs/steps/tm-plan-report.md`, this file.
- Not touched: `docs/plans/**`, `docs/api-docs/**` (no drift), `docs/test-api/core-verify-report.md` (phase-D history),
  `CLAUDE.md`, any migration script.

Commits: `ae76f20` Javadoc · `04fdd49` FILE limitation · `69f0f60` TENANT citations / boundary · `454be7a` consistency
docs · `d5b4ef6` HTTP run archive · `77a52bc` frontend handover · `42dfcb6` plan report · this report.

## Decisions & deviations (`docs/DEVIATIONS.md` `[TM-Z]`)

1. RULE-FILE-012's purge failure is recorded, not fixed. Closure adds no behaviour. The sweeper is a follow-up, to be
   built with the export-archive retention job.
2. One consolidated line maps the plan §11 numbers to the actual ones. It lists V16 … V22, including the unplanned V22.
3. Package E's citations of RULE-TENANT-012 are repointed to REQ-TENANT-011. RULE-TENANT-012 … 015 are reserved for the
   analysis-coverage work. E's report keeps the old wording, because reports are history.
4. The C6 → C4 merge damage gets blank lines only; no content changed.
5. Stale counts and pointers are corrected. The `[Unreleased]` CHANGELOG section is restructured: same bullets, in
   package order, under a new "Behaviour changes — read before upgrading" heading. No bullet text was dropped.

Not changed, reported instead:
- `CLAUDE.md`'s documentation map still names only the 1.2.0 addenda. Repository instructions are the owner's.
- `TenantService` Javadoc says "(SEC, MDL, NOTIF)", leaving out SEQUENCE. This was left so that no code changes after
  the final `mvn verify`.
- The comments in V17 and V19 are slightly stale. Shipped migrations are never edited.

## Acceptance checklist

| # | Item (task brief) | Evidence |
|---|---|---|
| 1 | `FileDocumentDomain` `from()` Javadoc restored (comment only) | `ae76f20` |
| 2 | RULE-FILE-012 LOCAL / S3 purge failure recorded (FILE addendum + DEVIATIONS) | `04fdd49` |
| 3 | Open follow-ups collected, none implemented | `tm-plan-report.md` §5 (21 items) |
| 4 | `mvn -q verify` from a clean `target/` (JDK 25), with totals | below |
| 5 | V16–V22 present, sequential, additive; each named exactly in a P2 entry; smoke test lists all; §11 mapping line | migration directory; `MigrationNamingTest` green; P2 grep (SEC / NOTIF / TENANT ×4 / FILE); `ReferenceApplicationSmokeTest` 2…22, 1000; DEVIATIONS `[TM-Z]` |
| 6 | 1.3.0 addenda: one heading per file, package order, no duplicated or conflicting rows, unique ids, RULE-TENANT-012..015 not defined or cited, ADRs exist and are decided | 32 files scanned by script; 2 boundary repairs; id-definition scan finds no duplicate NEW definition (repeated ids are CHANGED rows or ADR file + addendum row); 4 + 1 + 1 citation fixes; ADR statuses table below |
| 7 | README / PROJECT-OVERVIEW / project-registry counts match reality | ADRs SEC 16, MDL 12, TENANT 6, FILE 1 (files); operations 125 (completeness); events 13 (`com.erp.events`); tables 27 (23 with `TENANT_ID`, `TenantSchemaIntegrationTest`) |
| 8 | CHANGELOG `[Unreleased]` is coherent, behaviour changes are flagged, version suggested | `454be7a`; suggestion: MINOR 1.3.0 (plan report §6) |
| 9 | CONSUMING documents every new property once with defaults, the forward-headers guidance and the new SPI | already complete (lines 143, 153, 155, 157; proxy section; §3 export SPI); defaults checked against `ErpCoreProperties` |
| 10 | Jar built, reference app on 18111 with a fresh `erp_tm_z`; api-docs review → (update not needed); completeness clean; `check` shows only known limitations; generator tests | below |
| 11 | Full P-LIVE run with `--instance` / `--code-under-test`, archived, every case PASS | run `26100811257C`, 204/204 |
| 12 | Other profiles | P-MAIL 19/19, P-MAIL-DOWN 1/1, P-CAP 1/1 at cap 2 and at cap 3, P-LOCAL 1/1. All 22 profile cases ran, none left unrun |
| 13 | `core-test-plan.md` §6 counts | unchanged and correct: 226 = 204 P-LIVE + 22 profile (recounted from §5 and the `@tc` decorators: 226 / 226, no duplicate) |
| 14 | App stopped, DB dropped | java on 18111 stopped; `dropdb erp_tm_z` |
| 15 | Frontend handover | `docs/steps/tm-frontend-handover.md` (`77a52bc`) |
| 16 | Plan report | `docs/steps/tm-plan-report.md` (`42dfcb6`) |
| 17 | Commits with the co-author line; `git status` clean | `git log`; below |
| DoD §10 | analysis before code (the two analysis commits come before the docs that cite them; the only code changes are comments) · code = entry · `mvn verify` · api-docs · test plan + run archived · CHANGELOG | all of the above; frontend item → package F |

ADR statuses (files): ADR-FILE-008, ADR-MDL-002…007/012…016/043, ADR-SEC-001…011/035/038/062/063/064,
ADR-TENANT-001/002/003/005/006: ACCEPTED. ADR-TENANT-004: REJECTED (no-go, intended). None is PROPOSED.

## Code ↔ addendum check

| Item | Addendum | Code / tree | Match |
|---|---|---|---|
| Migrations | V16 (SEC P2), V17 (NOTIF P2), V18 / V19 / V20 / V21 (TENANT P2), V22 (FILE P2) | `db/migration/core/V16…V22` | yes |
| RULE-FILE-012 purge | DB in the transaction; LOCAL / S3 after commit; failure → logged only (§10 NOTE) | `FileService.purgeContent`, `LocalFsStorageProvider.delete`, `S3StorageProvider.delete` | yes |
| Path-tenant source of the public branding | REQ-TENANT-011 + `path-tenant-paths` default | `ErpCoreProperties.Tenant` default list | yes |
| API surface | 125 operations of the addenda | api-docs `review`: 0 added / 0 removed / 0 updated in every module | yes |
| Properties | `password-policy.*` 8 / 72 / true / true; `public-branding-rate-limit.*` 60 / 1m; `idempotency.*` true / 24h / `-`; `tenant.export.*` 200000 / 2 | `ErpCoreProperties` | yes |
| Events | 13 | `com.erp.events` (13 `*Event` classes besides `DomainEvent`) | yes |

## Verification output

- `mvn -q verify` (JDK 25.0.4.1, every `target/` deleted first; code at `77a52bc`, which equals this branch's code):
  exit 0, BUILD SUCCESS, JaCoCo met.
  - erp-core: 100 suites, **669 tests**, 0 failures, 0 errors, 0 skipped. JaCoCo lines 82.92 %.
  - erp-app-reference: 1 suite, **10 tests**, 0 failures, 0 errors, 0 skipped.
  - The baseline run on `e643d91` before any closure commit gave the same totals.
- Reference app: plain jar on port 18111, profile dev, fresh PostgreSQL 16 DB `erp_tm_z`, bootstrap password
  `Test1234` (passes the policy).
- api-docs:
  - `generate_all.py --function review --base http://localhost:18111 --server-url http://localhost:7272`: added 0,
    removed 0, updated 0 in every module; unchanged sec 50, tenant 15, file 14, notif 18, mdl 11, cu 5, sequence 6,
    audit 1, report 4, app 1. No `update` was needed and nothing was written.
  - `check_completeness.py --base http://localhost:18111`: `sum 125 missing=0 duplicated=0 stale=0 RESULT: PASS`.
  - `--function check`: SEC, TENANT, MDL, SEQUENCE and REPORT PASS. FILE and NOTIF fail on permissions
    (`CONTROLLER_NOT_MATCHED`), CU on unique-constraints, AUDIT on business-errors, and APP on permissions and
    business-errors. These are the five known limitations in `docs/api-docs/README.md`; there is no new FAIL.
  - Generator unit tests: `Ran 71 tests … OK`.
- HTTP suite (`docs/test-api/core_api_verify.py`):

  | Profile | Run | Result | Archive |
  |---|---|---|---|
  | P-LIVE | `26100811257C` | 204 PASS / 0 FAIL / 0 BLOCKED | `20261008T112525-P-LIVE.json` + `-report.md` |
  | P-MAIL | `261008113403` | 19 PASS | `20261008T113405-P-MAIL.json` |
  | P-MAIL-DOWN | — | 1 PASS | `20261008T113441-P-MAIL-DOWN.json` |
  | P-CAP (cap 2, then cap 3) | — | 1 PASS each | `20261008T113547-P-CAP.json`, `20261008T113608-P-CAP.json` |
  | P-LOCAL | — | 1 PASS | `20261008T113630-P-LOCAL.json` |

  Merged report: `20261008T112525-all-profiles-report.md`: 226 PASS, 0 FAIL, 0 SKIPPED, 0 BLOCKED.

  The profile instances followed the recipe in `start_profile_instance.sh` on the same DB, after the P-LIVE run:
  - P-MAIL and P-MAIL-DOWN used a variant jar with `spring-boot-starter-mail` added to `erp-app-reference/pom.xml`. The
    pom change was reverted and never committed. The jar was built with `-am`: the first attempt without it picked an
    older installed `erp-core` and Flyway refused it, so nothing ran against that instance.
  - P-MAIL captured mail with the in-repo `smtp_sink.py` on port 1025.
  - P-MAIL-DOWN pointed at port 2525, where nothing listens.
  - P-CAP set `erp.core.report.max-export-rows` to 2, then 3.
  - P-LOCAL used `erp.core.files.storage=LOCAL` with a scratch root, deleted afterwards.

  The app was stopped and `erp_tm_z` dropped.

## Skills checked

`api-verify` (run conventions, archive layout per `[TM-G]`), `/generate-api-docs` (review first, no update needed,
completeness gate), `gov-validate-backend-feature` / `gov-enforce-backend-contract`: not applicable (no code beyond
comments). Rules followed: `CLAUDE.md` "Analysis first" (the analysis notes precede the docs that cite them),
"Database migrations" (no script touched), Housekeeping (no `target/` or `__pycache__` committed).

## Notes for later steps

- Release: MINOR **1.3.0** is recommended. The reasoning and the owner's steps are in `docs/steps/tm-plan-report.md` §6:
  merge, set the poms, tag `v1.3.0`, let CI publish, move `main` to `1.4.0-SNAPSHOT`.
- Frontend: `docs/steps/tm-frontend-handover.md`. SEC and TENANT have no `P2_5` folder yet, so F creates them first.
- Open follow-ups (21) are in `docs/steps/tm-plan-report.md` §5. The most actionable:
  - the export-archive retention job, with a purge-failure sweeper;
  - `LoginRateLimiter` eviction;
  - SEC password history, throttling of the current-password check, and a super-role guard on admin-set.
- Next free ids are unchanged by Z:
  - TENANT: REQ/AC-038, RULE-029, POL-018, US-017, XM-005, DBF-046, ADR-TENANT-007.
  - FILE: RULE-013, XM-004, ADR-009.
  - SEC: REQ-094, AC-100, RULE-063, ADR-069.
  - NOTIF: RULE-025, XM-006.
  - HTTP: TC-CORE-TENANT-059, PLATFORM-006, SEC-056, NOTIF-017.
  - Next core migration: V23.
