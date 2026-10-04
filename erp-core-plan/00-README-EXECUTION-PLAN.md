# erp-core — Execution Plan (Phases 1–3, technical only)

**Source repo:** `https://github.com/hesham421/backend.git`, branch `newchanges`, commit `6f9f210`
**Target:** a NEW repository (fresh copy of the source) that becomes `erp-core` — a versioned Spring Boot library consumed as a Maven dependency by (a) the retail/wholesale + e-commerce SaaS app and (b) any future copied app.
**Scope:** technical conversion and platform gaps only. **No business logic** (no Party/Item/Documents/Inventory/Tax/e-commerce) — that is Phase 4 and runs through the Governance Factory later.
**Audit basis:** the gap analysis of 2026-10-04 (module inventory, tenant absent, single auth realm, DB-blob files, email-only notifications, no events/sequences/reporting, `fin` interleaved in migrations and permissions).

---

## 1. How to execute this plan (rules for the implementing agent)

1. **Work on a fresh clone** of the source repo in a new remote. Never push to `hesham421/backend`.
2. **One step = one branch = one PR-sized commit series.** Branch name `step/NN-<slug>`. Merge to `main` only when the step's *Acceptance* list is fully green. Do not start step N+1 before step N is merged.
3. **Order is mandatory** (see §3). Each step file lists its preconditions; verify them before starting.
4. **Do not ask questions.** Every decision is already made in the step files. If a step file is silent on a detail, apply the *Defaults* in §4. If something is impossible as written, do the closest thing that preserves the step's *Goal*, and write the deviation in `docs/DEVIATIONS.md` (create it in step 01) — then continue.
5. **The `fin` module is deleted in step 01 and never restored.** Nothing from `fin` is reused.
6. **Never invent governance content.** `governance/` in the source repo is a consumer of an external factory; it is removed from the library in step 03 (kept only in the reference app, untouched).
7. **Verification is mandatory** per step: the commands listed under *Verification* must pass before commit. Tests run against PostgreSQL via Testcontainers (set up in step 02); Docker must be available.
8. **Commit message format:** `step(NN): <summary>` + body listing what changed. Append the attribution lines required by the session.
9. **Keep the existing conventions** (`ServiceResult`, `ApiResponse`, `LocalizedException`, `SpecBuilder`, bilingual AR/EN, Flyway, Lombok). Do not reformat unrelated code.
10. **Additive only after step 04**: once the core migration chain is baselined, every later migration adds columns/tables with defaults — never renames, drops or changes types.

## 2. Final shape (what exists when the plan is done)

```
erp-platform/                      (root pom, packaging=pom)
├── erp-core/                      (library jar → published as com.erp:erp-core:1.0.0)
│   └── com.erp.{common,cu,mdl,sec,file,notif,audit,events,sequence,report,tenant,autoconfigure}
├── erp-app-reference/             (runnable Spring Boot app depending on erp-core; proves consumption)
│   └── com.erp.app
├── docs/                          (DEVIATIONS.md, RELEASE.md, per-step reports)
└── .github/workflows/ci.yml       (build + test + publish on tag)
```

Core modules at the end: Auth/Users/Permissions (2 realms) · Tenant · Audit log · Files (storage SPI, public URLs) · Notifications (events, async, channel SPI) · Reporting (minimal) · Lookups · Sequences · Settings · Events bus.

## 3. Step order and dependencies

| # | File | Goal | Depends on |
|---|---|---|---|
| 01 | `01-STEP-decouple-fin.md` | Remove `fin` completely (code, migrations, seeds, permissions, OpenAPI, tests) | — |
| 02 | `02-STEP-test-infrastructure.md` | Tests run anywhere: Testcontainers PostgreSQL, no localhost/Redis/SMTP dependency; green baseline | 01 |
| 03 | `03-STEP-library-split-and-autoconfig.md` | Multi-module Maven; `erp-core` as auto-configured library; reference app; Java 21; optional Redis/SMTP | 02 |
| 04 | `04-STEP-flyway-core-chain.md` | Squash non-fin migrations into a clean core chain `V1..V999`; apps use `V1000+` | 03 |
| 05 | `05-STEP-multi-tenancy.md` | `tenant_id` everywhere, Hibernate `@TenantId` discriminator, `TenantContext`, tenant provisioning | 04 |
| 06 | `06-STEP-auth-realms-and-permission-registry.md` | STAFF + CUSTOMER realms (self-registration), pluggable permission catalog replacing static constants | 05 |
| 07 | `07-STEP-file-storage-and-public-urls.md` | `StorageProvider` SPI (DB default, local FS, S3 optional), public files, Redis-optional tokens | 05 |
| 08 | `08-STEP-events-and-async-notifications.md` | Domain event bus, async dispatch, `ChannelProvider` SPI (EMAIL impl; SMS/PUSH/IN_APP contracts) | 05 |
| 09 | `09-STEP-sequences-and-settings-api.md` | Generic number series + typed/cached tenant-aware settings API | 05 |
| 10 | `10-STEP-generic-audit-log.md` | Cross-module audit event table + recorder API + optional entity listener | 05, 08 |
| 11 | `11-STEP-reporting-minimal.md` | `ReportProvider` SPI + tabular result + CSV/JSON export endpoint | 05 |
| 12 | `12-STEP-arch-rules-ci-and-release.md` | ArchUnit rules for the library, CI, GitHub Packages publish, release `1.0.0` | 06–11 |
| 13 | `13-STEP-factory-registration-as-given.md` | Register `erp-core` in the Governance Factory as a dependency ("given"), not as governed modules | 12 |
| 99 | `99-LATER-per-module-docs.md` | (Later phase, not executed now) one doc per module: what it is, integration, analysis | 12 |

Steps 06–11 are independent of each other and may run in parallel branches after 05, but merge them in numeric order to keep migration numbering deterministic (each step file reserves its migration numbers).

## 4. Defaults (apply when a step file is silent)

- Group id `com.erp`; library artifact `erp-core`; reference app `erp-app-reference`; root `erp-platform`. Version `1.0.0-SNAPSHOT` until step 12.
- Java **21** (`<maven.compiler.release>21</maven.compiler.release>`), Spring Boot stays at the version in the source pom (4.0.1) unless the build fails on JDK 21, in which case use the latest Boot 4.x that supports JDK 21.
- Database PostgreSQL 16; Flyway; `ddl-auto=none`.
- Core packages (scanned by the auto-configuration): `com.erp.common, com.erp.cu, com.erp.mdl, com.erp.sec, com.erp.file, com.erp.notif, com.erp.tenant, com.erp.audit, com.erp.events, com.erp.sequence, com.erp.report`.
- All core configuration properties are prefixed `erp.core.*` and bound with `@ConfigurationProperties`. Existing `app.*` / `file.*` keys are renamed in step 03 (mapping table in that file).
- Table naming: `<MODULE>_<NAME>` upper-case (existing convention). New core tables use the module prefixes `CORE_` (tenant, audit, sequence, report).
- Column naming: `*_PK` is NOT adopted for new tables; use `ID BIGINT` + per-table sequence (existing majority convention). Booleans: `BOOLEAN` (not SMALLINT) for new columns.
- Every new table: `ID, TENANT_ID, CREATED_BY, CREATED_AT, UPDATED_BY, UPDATED_AT` (+ `VERSION BIGINT` optimistic lock, added in step 05).
- Endpoint prefix stays `/api/v1/<module>/...`; public (unauthenticated) endpoints live under `/api/v1/public/...`.
- i18n: every new error code gets AR + EN entries in `i18n/messages_ar.properties` and `messages_en.properties`.
- Tests: JUnit 5, `@SpringBootTest` against Testcontainers PostgreSQL, profile `test`. Minimum per step: one happy-path and one failure-path API test for each new endpoint, plus unit tests for domain services.

## 5. Definition of Done for the whole plan

- `mvn -q verify` green on a clean machine with Docker (no localhost services).
- `erp-core-1.0.0.jar` published to GitHub Packages; `erp-app-reference` builds against the published artifact (not the reactor) in CI's final job.
- ArchUnit: core has zero dependencies on `com.erp.app..`; no class named `*Fin*` remains; every `@Entity` extends `TenantAwareEntity`.
- A fresh database is created from the core chain alone (`V1..V<n>`) with no errors; the reference app adds `V1000__app_smoke.sql` successfully.
- `docs/` contains a one-page report per step (`docs/steps/NN-report.md`: what changed, decisions, deviations, how verified).

## 6. Out of scope (do not do)

Business modules of any kind; e-commerce storefront; POS; multi-DB/schema-per-tenant; PostgreSQL RLS; OAuth/OIDC; PDF reporting; Oracle support; frontend changes; anything under `governance/` beyond deleting it from the library module.
