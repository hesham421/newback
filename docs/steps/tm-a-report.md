# Tenant-maturity package A — TENANT module analysis (as built from erp-core 1.2.0) — report

## Summary

The tenant module (`com.erp.tenant`, built by erp-core plan step 05 and extended by steps 07, 09 and 15)
had no analysis folder. Package A writes one, from the code only, in the pattern of SEC's analysis:
`governance/analysis/modules/TENANT/{P0,P0_5,P1,P2}` (8 files) and
`governance/analysis/decisions/TENANT/ADR-TENANT-001.md`. Every id is traced to a `file:line` of the code at
main @ 2274f86 (the tenant code there behaves as tag v1.2.0; only the helper move 6b01816 touched it).
The files are the as-built baseline: they carry no 1.2.0 and no 1.3.0 addendum. Packages B, C and E append
their `## Implementation Addendum — erp-core 1.3.0` sections to them.

Documentation only: no Java, SQL, i18n, api-docs or test change; no app run, no `mvn verify`, no HTTP suite
(plan §3, orchestrator instruction). No `docs/CHANGELOG.md` line: an analysis of existing behaviour changes
nothing in the library, and the changelog records library changes only.

## Analysis entries written

| File | Section(s) | Ids |
|---|---|---|
| `governance/analysis/modules/TENANT/P0/platform-summary.md` | tenant in the platform, realms interplay, dependency map, deferred | — (refers to SEC's platform summary for the whole platform) |
| `governance/analysis/modules/TENANT/P0/business-policies-tenant.md` | client-specific policies, scope exceptions, resolved decisions | POL-TENANT-001 … 011 |
| `governance/analysis/modules/TENANT/P0/module-registry-tenant.md` | entities owned, lookups, dependencies, exposed surface, permission module → screen → actions | (names; `PLATFORM` / `PLATFORM_TENANTS` / `PERM_PLATFORM_TENANTS_VIEW`, `PLATFORM_TENANT_MANAGE`) |
| `governance/analysis/modules/TENANT/P0_5/prd-tenant.md` | user stories, the provisioning story | US-TENANT-001 … 008 |
| `governance/analysis/modules/TENANT/P1/srs-tenant.md` | A1–A8, PART B, STANDALONE (resolution order, error codes, traceability, decisions, access) | ENT-TENANT-001; REQ-TENANT-001 … 023; AC-TENANT-001 … 023; RULE-TENANT-001 … 009; XM-TENANT-001, 002; SCR-REQ-TENANT-001 |
| `governance/analysis/modules/TENANT/P1/registry-srs-tenant.md` | registry + "Id → code location" + error codes | same ids, each with its code location |
| `governance/analysis/modules/TENANT/P2/db-script-tenant.md` | §1 traceability (CORE_TENANT + 22 discriminator columns), §2 XM, §3 script extract, §4 decisions, §6 index | DBF-TENANT-001 … 032; XM-TENANT-001, 002 |
| `governance/analysis/modules/TENANT/P2/registry-db-tenant.md` | tables, DBF → code location, XM index, sequences, cascade | DBF-TENANT-001 … 032 |
| `governance/analysis/decisions/TENANT/ADR-TENANT-001.md` | "Discriminator (row-level) multi-tenancy over schema-per-tenant" | ADR-TENANT-001 |

Last ids (next free in brackets): POL 011 (012) · US 008 (009) · ENT 001 (002) · REQ / AC 023 (024) ·
RULE 009 (010) · SCR-REQ 001 (002) · XM 002 (003) · DBF 032 (033) · ADR 001 (ADR-TENANT-002).

## Files changed

**Created (commit afa510e)** — the 9 files above.

**Modified (commit afa510e)**
- `governance/analysis/platform/project-registry.md` — TENANT row points to `modules/TENANT/` and `decisions/TENANT/` 1 ADR; TENANT row in "Analysis status per analysed module".
- `governance/analysis/platform/PROJECT-OVERVIEW.md` — documentation-map row "How does multi-tenancy work".
- `governance/README.md` — layout: ADR counts `SEC 13, MDL 12, TENANT 1`; TENANT P0–P2 described as a non-verbatim as-built baseline; documentation-map row. The README holds no total file count (`git ls-files governance | wc -l`: 264 → 273).
- `governance/rules/GOVERNANCE-RULES.md` — the "Module analysis" row lists TENANT (same list as CLAUDE.md; not named in the package text, changed so the two lists agree).
- `CLAUDE.md` — repository-structure line of the analysed modules gains TENANT.
- `docs/DEVIATIONS.md` — `## [TM-A] tenant-maturity A`, one entry.

**Created (this commit)** — `docs/steps/tm-a-report.md`.

Not edited: `docs/governance-vendoring-report.md` (history); the SEC 1.2.0 addendum's "Tenant | analysis folder: none"
row (`governance/analysis/modules/SEC/P0/platform-summary.md`) stays as written — addenda are append-only.

## Decisions & deviations

1. **`SCR-REQ-TENANT-001` instead of the plan's `SCR-TENANT-001`** — every analysed module numbers screen
   requirements `SCR-REQ-<MOD>-NNN`; recorded in `docs/DEVIATIONS.md` `[TM-A]`.
2. **XM ids for the exposed direction** — SEC's analysis lets the consuming module mint XM ids
   (SEC `P2/db-script-sec.md` §2). The plan names `XM-TENANT-001` / `002` for the two exposed surfaces, so they
   are minted here, with a note in `registry-srs-tenant.md` (no deviation: plan-prescribed).
3. **The 22 `TENANT_ID` columns are registered as DBF-TENANT-011 … 032** although they live in other
   modules' tables — the tenant-side register of the same columns; the owning modules' 1.2.0 addenda keep
   theirs. `CORE_NUMBER_SERIES` and `CORE_AUDIT_EVENT` (no analysis folder) are registered only here.
4. **Lookup naming** — no lookup key was invented for the tenant status; it is "the value set of
   `CORE_TENANT.STATUS_CODE` (`CHK_CORE_TENANT_STATUS`)".
5. **No `P2_5` folder and no `.gitkeep` files** — SEC has no `P2_5`; the folders are not empty.

## Acceptance checklist

Plan §3 (package A) — 10/10
- ✅ `P0/platform-summary.md` — shared schema, `TENANT_ID` on the tenant-scoped tables (22; the 4 global tables named), `PLATFORM` id 1, realms interplay.
- ✅ `P0/business-policies-tenant.md` — POL-TENANT-001 code `^[A-Z0-9_]{3,32}$` and immutable; 002 ACTIVE/SUSPENDED only; 003 PLATFORM never suspended; 004 provisioning atomic; 005 no delete; 006 platform-only management; + 007–011 (isolation, tenant per request, token never crosses tenants, starting catalog, admin password never in clear).
- ✅ `P0/module-registry-tenant.md` — module `PLATFORM`, screen `PLATFORM_TENANTS`, actions `PERM_PLATFORM_TENANTS_VIEW` + `PLATFORM_TENANT_MANAGE` (V10 §6 seed; `TenantPermissions` since step 06).
- ✅ `P0_5/prd-tenant.md` — the five operations (US-TENANT-001…003) and the provisioning story with the SEC / MDL / NOTIF / SEQUENCE contributors (orders 0 / 10 / 20 / 40).
- ✅ `P1/srs-tenant.md`, `P1/registry-srs-tenant.md` — REQ/AC/RULE ids, ENT-TENANT-001, XM-TENANT-001 (`TenantLookupApi`), XM-TENANT-002 (`TenantProvisioningContributor`), SCR-REQ-TENANT-001 = `PLATFORM_TENANTS`, the resolution order and every error code with its HTTP status as the code has it (`TENANT_REQUIRED` 400, `TENANT_NOT_FOUND` 404, `TENANT_SUSPENDED` 403, `TENANT_CONTEXT_MISSING` 500, `TENANT_CODE_INVALID` 400, `TENANT_CODE_DUPLICATE` 409, `TENANT_PLATFORM_PROTECTED` 422 — each checked at its throw/write site and `Status` mapping).
- ✅ `P2/db-script-tenant.md`, `P2/registry-db-tenant.md` — `CORE_TENANT` exactly as V10 (columns, types, widths, defaults, `PK_CORE_TENANT`, `UQ_CORE_TENANT_CODE`, `CHK_CORE_TENANT_STATUS`, `CHK_CORE_TENANT_CODE`, `SEQ_CORE_TENANT`); every `TENANT_ID` column with FK and index as DBF-TENANT-… (counted: 22, not 18).
- ✅ `decisions/TENANT/ADR-TENANT-001.md` — as-built decision, step-05 reasons, existing ADR template (ADR-SEC-001 layout).
- ✅ `platform/project-registry.md` — TENANT row points to the folder + ADR count.
- ✅ `platform/PROJECT-OVERVIEW.md` — documentation-map row.
- ✅ Plan §3 DoD — the folder exists; every id is traceable to a code location (path check below: 0 errors); `governance/README.md` counts updated; one commit `docs(governance): TENANT module analysis — as-built from erp-core 1.2.0` (afa510e).

Plan §10 DoD (applies partly to a documentation-only package)
- ✅ Analysis entries exist — this package is the analysis.
- n/a Code ↔ entry check — no code written; the entries were checked against the code instead (table below, path check).
- n/a `mvn -q verify`, api-docs regeneration, `core-test-plan.md` / api-verify run, CHANGELOG line, frontend items — documentation only, per the package instruction.

## Code ↔ analysis check (id → code location, summary)

The analysis is read from the code, so the check runs the other way: every cited location exists and its
line range is inside the file (path check below), and each location was read when the entry was written.

| Id group | Count | Primary code locations |
|---|---|---|
| POL-TENANT | 11 | `tenant/domain/TenantDomain.java`, `tenant/security/TenantResolutionFilter.java`, `tenant/service/TenantService.java`, `autoconfigure/ErpCoreSecurityAutoConfiguration.java:130-131, :199-205`, the four `*TenantProvisioningContributor.java`, `V10__tenant_schema.sql` |
| US-TENANT | 8 | `tenant/controller/PlatformTenantController.java:41-75`, `tenant/TenantProvisioningContributor.java`, `tenant/crossmodule/TenantLookupApi.java`, `tenant/TenantContext.java` |
| ENT-TENANT-001 | 1 | `tenant/entity/Tenant.java:32-95`; `V10__tenant_schema.sql:31-51` |
| REQ / AC-TENANT | 23 / 23 | service methods (`TenantService.java:68-160`), filter branches (`TenantResolutionFilter.java:83-128, :131-158`), JWT `tid` (`JwtAuthenticationFilter.java:129-136`), resolver (`TenantIdentifierResolver.java:36-42`), context (`TenantContext.java:47-92`); ACs cite `TC-CORE-TENANT-001…026`, `TC-CORE-PLATFORM-001…004` and the tenant JUnit classes |
| RULE-TENANT | 9 | `TenantDomain.java:18, :34-59`; `Tenant.java:50`; `TenantStatusUpdateRequest.java:20`; `TenantResolutionFilter.java:89-147`; `SecTenantProvisioningContributor.java:84-121`; `MenuService.java:142-143` |
| Error codes | 7 | `TenantErrorCodes.java:14-32`; statuses at `TenantResolutionFilter.java:94, :106, :110, :124, :136, :140`, `TenantContext.java:50`, `TenantDomain.java:36, :38, :56`, `TenantService.java:81, :111, :147`; `Status.java:10-20`; i18n `messages.properties:143-149`, `messages_ar.properties:140-146` |
| XM-TENANT | 2 | `tenant/crossmodule/TenantLookupApi.java:10-14`; `tenant/TenantProvisioningContributor.java:22-31` |
| SCR-REQ-TENANT-001 | 1 | `tenant/permission/TenantPermissions.java:28, :43-44`; `V10__tenant_schema.sql:216-228` |
| DBF-TENANT | 32 | `V10__tenant_schema.sql:29-51` (CORE_TENANT), `:63-138` (18 columns, FKs, indexes); `V11__sec_realms.sql:68, :84, :86`; `V13__notif_async_inbox.sql:40, :61, :62`; `V14__sequence_and_settings.sql:33, :58, :61, :66`; `V15__audit_schema.sql:18, :40, :44`; counts = `TenantSchemaIntegrationTest.java:51, :98, :113` |
| ADR-TENANT-001 | 1 | `erp-core-plan/05-STEP-multi-tenancy.md`; `docs/steps/05-report.md`; `AuditableEntity.java:35-37` |

(Java paths relative to `erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`.)

## Verification output

Path check (scratch script; extracts every repo path, relative link, `Class.java:line`, `Vnn__*.sql:line`
and `Vnn:line` reference, resolves it, checks the file exists and the line range is inside it; self-tested
against five planted bad references, all five reported):

```
$ python pathcheck.py . governance/analysis/modules/TENANT/*/*.md governance/analysis/decisions/TENANT/*.md
references checked: 1171
errors: 0

$ python pathcheck.py . <the 9 files> governance/analysis/platform/PROJECT-OVERVIEW.md governance/analysis/platform/project-registry.md
references checked: 1224
errors: 1
  PROJECT-OVERVIEW.md: MISSING docs/steps/NN-report.md      <- pre-existing placeholder pattern, not a path

Id definitions: REQ 23 · AC 23 · RULE 9 (srs-tenant.md headings); DBF 32 rows + 32 index lines
(db-script-tenant.md); POL 11; US 8.
$ git ls-files governance | wc -l   → 273 (264 before)
```

No `mvn` totals, HTTP suite or api-docs run: documentation-only package.

## Skills checked

None routes to an analysis-only change (`governance/rules/GOVERNANCE-RULES.md` routing table covers code
generation and validation). Read for the facts: `governance/rules/GOVERNANCE-RULES.md` (layout table),
the brief, plan §0 D1 and §3.

## Notes for later steps

- **Where to write.** B, C, E append `## Implementation Addendum — erp-core 1.3.0` sections to these TENANT
  files (none exists yet) and continue the ids above (next REQ-TENANT-024, RULE-TENANT-010, DBF-TENANT-033,
  XM-TENANT-003, ADR-TENANT-002). New `CORE_TENANT` columns continue at DBF-TENANT-033.
- **Facts the plan's later packages rely on (as built today):**
  - every `/api/v1/platform/tenants` endpoint, reads included, needs `PLATFORM_TENANT_MANAGE`; `PERM_PLATFORM_TENANTS_VIEW` grants no endpoint (F3, E's public read);
  - `TenantMapper` has no update mapping and `Tenant.code` is `updatable = false` (B's PUT);
  - `TenantLookupApi` has only `codeOf(Long)` — no `isActive` (C.1);
  - the JWT filter attaches only `AuthRealm` as authentication details; `iat` is not exposed (C.2);
  - the global-entity set is `Tenant`, `ModuleRegistry`, `ScreenRegistry`, `ActionRegistry` **and** `AppConfiguration` (`TenantSchemaIntegrationTest.java:32`) (C.3);
  - a suspended (or missing) token tenant answers 403 `TENANT_SUSPENDED`, never 404;
  - `TenantService` Javadoc names the contributors "(SEC, MDL, NOTIF)" — SEQUENCE (order 40) is missing from that comment (doc drift only).
- **HTTP cases in use:** `TC-CORE-TENANT-001…026`, `TC-CORE-PLATFORM-001…004`.
- **Frontend:** TENANT has no `P2_5` folder; the frontend's F3 addendum for `PLATFORM_TENANTS` needs one
  (`governance/analysis/modules/TENANT/P2_5/`), created by the frontend write set.
