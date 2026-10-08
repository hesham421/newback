# TM-C5 — tenant data export

| | |
|---|---|
| Plan | `docs/plans/tenant-maturity-plan.md` §5 C.5 (item 16), §9, §10 |
| Branch | `tm/c5-tenant-export` (from local `main` @ 9bbf708: A, G, C3, D, B, E, C12, C6, C4 merged) |
| Migration | none in the first round; review round 1: `V22__file_document_required_authority.sql` (the reserved number) |
| Date | 2026-10-08 |

## Summary

`POST /api/v1/platform/tenants/{id}/export` (`PLATFORM_TENANT_MANAGE`, PLATFORM callers only) exports a tenant's data
synchronously. Inside `TenantContext.callAs(id)` one read-only `REPEATABLE READ` transaction gives one snapshot: every
`TenantExportContributor` counts its rows, a total above `erp.core.tenant.export.max-rows` (default 200 000) answers 422
`TENANT_EXPORT_TOO_LARGE` before anything is written, then each contributor streams its rows (JDBC fetch size 1 000) as
CSV into a ZIP on a temporary file, followed by `manifest.json`. In a PLATFORM transaction FILE stores the ZIP as a
PRIVATE, uncategorised `FILE_DOCUMENT` (`CORE_TENANT` / {id} / `TENANT`, `application/zip`, file type `ARCHIVE`) through
the new `FilePrivateStoreApi` (XM-FILE-003) and `TENANT_EXPORTED` is recorded in PLATFORM and (nested, committed first)
in the exported tenant; then FILE issues its existing single-use download token (AES-GCM, 10 minutes, bound to the
issuing username, consumed by the first successful `GET /api/v1/files/download?token=`). The answer is
`TenantExportResponse { tenantId, tenantCode, fileId, fileName, sizeBytes, rowCount, downloadToken,
downloadTokenExpiresAt }`. A second export of a tenant while one runs on the node answers 409
`TENANT_EXPORT_IN_PROGRESS` (in-memory guard, released on every path). The temporary file is deleted on every path.

The SPI (`com.erp.tenant`, public root package, XM-TENANT-004): `TenantExportContributor { String moduleCode(); long
countRows(Long tenantId); void export(TenantExport export) }`, `TenantExport.csv(fileName, columns, rows)` with the
`Rows` sink (`addRow(ResultSet)` = a `RowCallbackHandler`, `add(Object...)`), `TenantExportJdbc` (`streaming`,
`selectOfTenant`, `countOfTenant`). Eight contributors, each in its module's `tenant` package (rule 7's documented
raw-JDBC place): TENANT (`CORE_TENANT` row), SEC (9 files), MDL (2), CU (1), FILE (2), NOTIF (4), SEQUENCE (1), AUDIT (1)
— 21 files. Never exported: password hashes, token hashes and the two token tables, `SEC_ACTIVE_SESSION.TOKEN_REF`,
`NOTIF_CHANNEL_CONFIG.CONFIG_JSON`, `NOTIF_LOG.VARIABLES_JSON`, `FILE_CONTENT` / `STORAGE_REF` / `PUBLIC_SLUG`,
`TOKENS_INVALID_BEFORE`, `CORE_IDEMPOTENCY_KEY`, platform-wide rows, every `TENANT_ID` / `VERSION`. CSV: UTF-8 with BOM
(Excel/Arabic; the REPORT precedent), RFC 4180, CRLF, NULL = empty field vs empty text = `""`, formula guard
(`= + - @` TAB CR → `'`), ordered by primary key; a binary column is refused by the writer.

## Analysis entries written (commit 4dac933, before any code)

| File | Section | Ids |
|---|---|---|
| `TENANT/P1/srs-tenant.md` | 1.3.0 addendum, package-C5 block X1–X12 (after C4's) | REQ/AC-TENANT-037, RULE-TENANT-027, -028, XM-TENANT-004; codes `TENANT_EXPORT_TOO_LARGE` 422, `TENANT_EXPORT_IN_PROGRESS` 409; X5 SPI; X6 archive format; **X7 exact files and columns of every module**; X8 property; X10 audit / secrets / memory; X11 decisions; X12 FE |
| `TENANT/P1/registry-srs-tenant.md` | package-C5 block | last REQ 037 · RULE 028 · XM 004 · US 016 · POL 017 · ADR 006 |
| `TENANT/P0/business-policies-tenant.md`, `module-registry-tenant.md`, `platform-summary.md` | package-C5 blocks | POL-TENANT-017; resolved decision 6; exposed SPI; config |
| `TENANT/P0_5/prd-tenant.md` | package-C5 block | US-TENANT-016 |
| `TENANT/P2/db-script-tenant.md`, `registry-db-tenant.md` | package-C5 blocks | no schema; XM register + XM-TENANT-004 |
| `decisions/TENANT/ADR-TENANT-006.md` | NEW | sync bounded export, PRIVATE PLATFORM document + single-use token, per-node guard (PROPOSED → ACCEPTED in 7e23775) |
| `FILE/P1/srs.md` §9, `registry-srs-file.md`, `P0/module-registry-file.md` | 1.3.0 package-C5 | XM-FILE-003 `FilePrivateStoreApi`, RULE-FILE-011, the FILE contributor |
| `SEC/P1/srs-sec.md` §13 + registry; `MDL/P1/srs-mdl.md` + registry (new 1.3.0 section); `CU/P1/srs-cu.md` + registry (new 1.3.0 section); `NOTIF/P1/srs.md` §6 + registry | package-C5 | the contributors and their exclusions (no id minted) |
| `governance/README.md`, `platform/project-registry.md` | counts | TENANT ADRs 4 (stale) → 6; tenant operations 14 → 15 |

Ids re-verified before minting against the tree, this repository's full history (`git log -p --all`) and the
read-only `governance-shared` checkout (no TENANT ids there): TENANT highest REQ/AC 036, RULE 026, POL 016, US 015,
XM 003, DBF 045, ADR 005; FILE RULE 010 / XM 002 (RULE-FILE-011 / XM-FILE-003 appeared only as "next free" notes).
Check commit 7e23775 added X13 (code check) to srs-tenant.md.

## Files changed

- Code (erp-core main): NEW `tenant/TenantExportContributor`, `TenantExport`, `TenantExportJdbc`;
  `tenant/export/TenantExportArchive`, `TenantExportGuard`, `TenantTenantExportContributor`;
  `tenant/service/TenantExportService`; `tenant/dto/TenantExportResponse`; `sec/tenant/SecTenantExportContributor`,
  `mdl/tenant/MdlTenantExportContributor`, `cu/tenant/CuTenantExportContributor`,
  `file/tenant/FileTenantExportContributor`, `notif/tenant/NotifTenantExportContributor`,
  `sequence/tenant/SequenceTenantExportContributor`, `audit/tenant/AuditTenantExportContributor`;
  `file/crossmodule/FilePrivateStoreApi(Impl)`, `PrivateFileStoreRequest`, `StoredPrivateFile`, `DownloadGrant`;
  `file/service/FilePrivateStoreService`. CHANGED: `tenant/controller/PlatformTenantController` (`exportTenant`),
  `tenant/domain/TenantDomain` (`assertExportWithinLimit`, `assertExportStartable`, `EXPORT_*`),
  `tenant/exception/TenantErrorCodes`, `tenant/mapper/TenantMapper` (`toExportResponse`),
  `file/service/FileService` (`tokenKey`, `safeFileName`, `deriveFileType` package-visible),
  `autoconfigure/ErpCoreProperties` (`Tenant.Export.maxRows`, `@Valid`), i18n (one `tenant-maturity C5` block each).
- Tests: NEW `tenant/TenantExportIntegrationTest` (4), `tenant/export/TenantExportArchiveTest` (4),
  `tenant/export/TenantExportGuardTest` (2); CHANGED `tenant/domain/TenantDomainTest` (+1), `tenant/TenantHttp`
  (`getBytes(token, path)`), `architecture/CoreLibraryRulesArchTest` (Javadoc of the raw-JDBC list names the export
  contributors). No new Spring context combination (the row-limit test lowers the bound property in-process and
  restores it; the 409 test takes the tenant's slot through the guard bean).
- Docs: `docs/api-docs/tenant/**` + `README.md` (regenerated), `docs/test-api/core-test-plan.md`, `core_api_verify.py`
  (+ `downloadToken` redaction), `docs/test-api/results/20261008T100036-P-LIVE{.json,-report.md}`, `docs/CHANGELOG.md`,
  `docs/CONSUMING.md` (§2 property, §3 "Tenant data export" with an application contributor example),
  `docs/DEVIATIONS.md` `[TM-C5]`, `governance/analysis/platform/PROJECT-OVERVIEW.md`, this report.

## Decisions & deviations (all in `docs/DEVIATIONS.md` `[TM-C5]` and srs-tenant.md X11/X13)

1. SPI + `countRows(Long tenantId)` (the plan's "refuse > N" needs a count first); the archive also caps while writing.
2. Response richer than `{ fileId, downloadToken }` (additive): `tenantId, tenantCode, fileName, sizeBytes, rowCount,
   downloadTokenExpiresAt`; 200 `Status.SUCCESS`.
3. New FILE cross-module API `FilePrivateStoreApi` (`FileImageStoreApi` stores PUBLIC images only): PRIVATE,
   uncategorised, declared type, SHA-256, no upload limits (the row limit bounds the file); the token is API-FILE-002's,
   issued without `PERM_FILE_BROWSER_VIEW` (the consumer's permission covers it).
4. **Download token binding (checked):** `FileService.retrieve` compares the token store's value with the caller's
   **username** before consuming it (another user → 401 `FILE_ACCESS_TOKEN_INVALID`, not consumed — proven by the
   integration test with a second PLATFORM operator); the document is looked up in the **caller's tenant**, so a
   same-named user of another tenant gets 404. In practice only the issuing platform operator can download the archive.
5. One snapshot (`REPEATABLE READ`, read-only) for count and files; storage + audit in a second (PLATFORM) transaction;
   the token after it. Not wrapped in C4's `IdempotentResponses` (several transactions; ADR-TENANT-003 Consequences).
6. CSV with BOM and formula guard (REPORT precedent); NULL vs `""` distinguished; manifest documents the conventions.
7. PLATFORM is exportable (one audit row); a suspended tenant is exportable (audited in it).
8. Guard per node (multi-node: two archives, harmless — RULE-TENANT-028, ADR-TENANT-006).
9. Idempotency rows (`CORE_IDEMPOTENCY_KEY`) are not exported (stored answers, not business data — C4's open point).
   REPORT owns no table (no contributor). CU values exported as stored (CU has no secret marking; srs-cu.md note).
10. Archives are kept until deleted (no retention job v1); the `DB` provider holds one archive in memory while storing.
11. HTTP suite: 422 and 409 are JUnit-only (§9 rows).
12. Reference snapshot "Package C5": adopted (endpoint, codes, property, PRIVATE PLATFORM document, guard, audit
    action, rule/policy/story texts) except: response shape (2), `countRows` (1), the exclusions list extended beyond
    "password hash / file bytes" (X7), the FILE side made explicit (3); the snapshot's "Facts" 1–9 are not affected by
    C5 (fact 2's sniffer: the private store does not sniff, the producer declares `application/zip`).

## Acceptance checklist

| # | Item (plan §5 C.5 + task brief) | Evidence |
|---|---|---|
| 1 | SPI `TenantExportContributor { moduleCode(); export(TenantExport) }` in `com.erp.tenant`, mirroring the provisioning SPI | `tenant/TenantExportContributor.java` (+ `countRows`, DEVIATIONS); XM-TENANT-004 |
| 2 | One contributor per core module (SEC, MDL, CU, FILE, NOTIF, SEQUENCE, AUDIT; TENANT = the tenant row); REPORT / idempotency decided | eight contributors; srs X7, DEVIATIONS 9 |
| 3 | Never password hashes, token hashes, secrets, channel credentials, file bytes, cut-offs; column lists exact in the analysis | srs X7 (compared mechanically with the code: equal); `TenantExportIntegrationTest` (seeded secrets of every kind absent; headers) ; TC-CORE-TENANT-054 |
| 4 | CSV escaping RFC 4180, UTF-8 BOM decided and recorded, deterministic order | X6; `TenantExportArchiveTest.fields_…`, `anArchive_…`; ORDER BY primary key in every statement |
| 5 | Contributors read via their own JDBC inside `callAs(id)` + `TransactionTemplate`; raw SQL names `TENANT_ID` (RULE-TENANT-011) | contributors; `TenantExportService.snapshot()`; `CoreLibraryRulesArchTest` rule 7 green |
| 6 | `POST /{id}/export`, `PLATFORM_TENANT_MANAGE`, PLATFORM only | controller + `@PreAuthorize`; TC-CORE-TENANT-056 (403 tenant admin, 401 anonymous) |
| 7 | Count first, refuse > N with 422 `TENANT_EXPORT_TOO_LARGE`; property in `ErpCoreProperties` + CONSUMING | `TenantDomain.assertExportWithinLimit`; `anExportOverTheRowLimit_is422_storesNothing_andFreesTheTenantForTheNextExport`; CONSUMING §2 |
| 8 | ZIP + manifest (tenant code, exported at, per-file counts, erp-core version) | `TenantExportArchive.finish`; integration test and TC-054 compare the manifest with the files |
| 9 | Stored as PRIVATE `FILE_DOCUMENT` in PLATFORM (`CORE_TENANT` / {id} / `TENANT`) via a FILE crossmodule API | `FilePrivateStoreApi`; integration test reads the row; TC-054 reads the metadata (A's admin 404) |
| 10 | `{ fileId, downloadToken }` with the existing single-use token, bound to the issuing user (checked, documented) | Decision 4; integration test (other operator 401, owner 200, again 401); TC-054 |
| 11 | Audit `TENANT_EXPORTED` (PLATFORM + target tenant) | integration test (1 + 1; PLATFORM once); TC-CORE-TENANT-055, -056 |
| 12 | 409 `TENANT_EXPORT_IN_PROGRESS`, in-memory guard keyed by tenant id; multi-node documented | `TenantExportGuard`; `aSecondExportOfTheSameTenant_is409_…`; `TenantExportGuardTest`; RULE-TENANT-028 |
| 13 | PLATFORM exportable? suspended exportable? decided | yes / yes (X11); tests; TC-056 (PLATFORM) |
| 14 | Memory: streamed rows, ZIP to a temp file, temp files cleaned on all paths | fetch size 1 000; `finally deleteQuietly`; integration test (no new `erp-tenant-export-*` file); after P-LIVE none in the temp dir |
| 15 | Tenant isolation: export of A has nothing of B | integration test (B's code and user absent); TC-054 (`$TB`, `$TC` absent) |
| 16 | Analysis first (TENANT 1.3.0 C5 block, FILE/other addenda, ADR) | commit 4dac933 precedes dc2b535 |
| 17 | §10 DoD: code = addendum | table below; X13 |
| 18 | §10 DoD: `mvn -q verify` green | below |
| 19 | §10 DoD: api-docs regenerated, completeness clean | below |
| 20 | §10 DoD: test plan + run archived | below |
| 21 | §10 DoD: CHANGELOG | `[TM-C5]` (Added) |
| 22 | §10 DoD frontend | n/a (backend package; FE rows in X12) |

22/22 (item 22 not applicable).

## Code ↔ addendum check

| Addendum item | Code | Match |
|---|---|---|
| X1 `POST /{id}/export`, `PLATFORM_TENANT_MANAGE`, 200, codes 404 / 409 / 422, order of checks | `PlatformTenantController.exportTenant`, `TenantExportService.export` | yes |
| X2 / X3 RULE-TENANT-027 snapshot, limit, exclusions, PRIVATE PLATFORM document, token | `TenantExportService`, `TenantExportArchive`, contributors, `FilePrivateStoreService` | yes |
| X3 RULE-TENANT-028 guard per node, released on every path | `TenantExportGuard`, `finally` | yes |
| X4 codes, statuses, i18n both bundles | `TenantErrorCodes`, `TenantDomain`; `messages*.properties` | yes |
| X5 SPI types and members | root package types | yes + `selectOfTenant` / `countOfTenant` (X13) |
| X6 archive format, manifest keys, file name | `TenantExportArchive`, `TenantExportService` | yes (manifest aligned in 7e23775) |
| X7 21 files, column lists, exclusions, order | the eight contributors | yes (script comparison: 21/21 equal) |
| X8 `erp.core.tenant.export.max-rows` 200 000, positive, read per export | `ErpCoreProperties.Tenant.Export` | yes |
| X9 transactions (snapshot in `callAs(id)`, PLATFORM `REQUIRES_NEW`, nested tenant audit, token outside) | `TenantExportService` | yes |
| X10 audit rows / summaries / no secret in logs | `storeAndRecord`; log lines name ids, counts, sizes | yes |
| FILE §9 XM-FILE-003, RULE-FILE-011 | `FilePrivateStoreApi(Impl)`, `FilePrivateStoreService` | yes |
| ADR-TENANT-006 Decision | code | yes (ACCEPTED) |

## Verification output

- `mvn -q -o verify` (clean `target/`, code of dc2b535 + 1990849; the check commit 7e23775 only drops one manifest key:
  `TenantExport*Test` + `TenantDomainTest` re-run on 7e23775, 58 tests, 0 failures; the P-LIVE run used 7e23775):
  BUILD SUCCESS, JaCoCo met (erp-core lines **82.78 %**).
  - erp-core: tests **661**, failures 0, errors 0, skipped 0 (C4 + C6 merged at 650: +4 integration, +4 archive,
    +2 guard, +1 domain).
  - erp-app-reference: tests **10**, failures 0, errors 0, skipped 0.
- HTTP suite: run **`261008100062`**, profile P-LIVE, port 18110, fresh DB `erp_tm_c5` (dropped afterwards), code
  7e23775: **202 PASS, 0 FAIL, 0 BLOCKED** (22 profile cases not run) — `docs/test-api/results/20261008T100036-P-LIVE.json`
  / `-report.md`. New TENANT-054 … 056 run before TENANT-046 (no public-branding call).
- api-docs: `review` → only `tenant/` changes (+ `POST /api/v1/platform/tenants/{id}/export`, the error table, the
  catalog) → `update` (`--base http://localhost:18110 --server-url http://localhost:7272`). The endpoint binds
  `TENANT_NOT_FOUND` 404, `TENANT_EXPORT_IN_PROGRESS` 409, `TENANT_EXPORT_TOO_LARGE` 422, `INTERNAL_ERROR` 500 and
  `PLATFORM_TENANT_MANAGE`. `check_completeness.py --base http://localhost:18110`: `per module: app=1, audit=1, cu=5,
  file=14, mdl=11, notif=18, report=4, sec=50, sequence=6, tenant=15 (sum 125) missing=0 duplicated=0 stale=0 RESULT:
  PASS`. `check`: TENANT PASS; FILE, NOTIF, CU, AUDIT, APP — the five known generator limitations, unchanged. README
  counts corrected (they were stale since C12: 123 → 125 operations, 99 → 101 paths). Generator unit tests: 71, OK.

## Skills checked

`gov-enforce-backend-contract` (LAYER 0: `TenantDomain` rules static, plain, `LocalizedException`; no entity /
repository / mapper-per-entity change beyond `toExportResponse`; service A.5.1/5.2/5.13–5.15/5.18 — every public method
`@PreAuthorize`, rules delegated, A.5.3 not `@Transactional` by design (the B/E/C12 precedent, recorded); controller
A.6.1–6.4, 6.10, 6.12 thin, unique method name `exportTenant`), `build-create-dto` (`@Data @Builder`, `@Schema` with
description + example, UTC `@JsonFormat`), `build-create-mapper` (null-safe, no cross-module types), `build-create-service`
(cross-module calls only through `file.crossmodule`; FILE adapter delegates to its own service), `build-create-controller`,
`gov-enforce-error-handling` (two registered codes, both bundles; I/O → `INTERNAL_ERROR` / `FILE_STORAGE_UNAVAILABLE`),
`gov-enforce-caching-rules` (no cache), `gov-validate-backend-feature` (raw-SQL rule RULE-TENANT-011 on every contributor
statement; Stage 0 entity / repository items n/a — no entity), `api-verify`. `build-create-entity` /
`build-create-repository`: n/a (no entity, no repository).

## Notes for later steps

- Next free ids (re-verify): TENANT REQ/AC-038, RULE-029 (012 … 015 reserved), POL-018, US-017, ENT-002, SCR-REQ-002,
  XM-005, DBF-046, ADR-TENANT-007; FILE RULE-012, XM-004, API-FILE-009, ADR-FILE-009; SEC, MDL, CU, NOTIF unchanged
  (SEC REQ-094, AC-100, RULE-063, ENT-015, DBF-124, XM-008, ADR-069; NOTIF RULE-025, XM-006). HTTP:
  TC-CORE-TENANT-057, TC-CORE-PLATFORM-006, TC-CORE-SEC-056, TC-CORE-NOTIF-017. Test-plan counts TENANT 56, total 224,
  P-LIVE 202.
- A new tenant-scoped core table must get a column list in a contributor **and** a row in srs-tenant.md X7 (or an
  explicit "not exported" row); `TenantExportIntegrationTest.FILES` lists the 21 files.
- `TenantExportIntegrationTest` asserts the archive's exact file set; a new contributor changes it.
- The closure step should re-check: the export endpoint's api-docs page after any later regeneration (binds four codes);
  the README operation count 125; that no later HTTP case relies on PLATFORM having no `TENANT_EXPORTED` row for entity 1
  (TENANT-056 asserts exactly one in a fresh run).

## Review round 1

Verdict FAIL on one HIGH finding (evidence `rev-c5/`). Fixed on the same branch, analysis first (885549e: srs-tenant X14,
FILE srs §9.5 + P2, registries, ADR-TENANT-006 amended), no rebase.

| # | Finding | Fix | Evidence |
|---|---|---|---|
| 1 HIGH | The archive was an ordinary PRIVATE PLATFORM document: a PLATFORM user holding only `PERM_FILE_BROWSER_VIEW` listed it, got an access token and downloaded tenant A's whole export; UPDATE / DELETE holders could archive or delete it | **Restricted documents** (FILE RULE-FILE-012): NEW `FILE_DOCUMENT.REQUIRED_AUTHORITY VARCHAR(100)` (`V22__file_document_required_authority.sql`, appended to `ReferenceApplicationSmokeTest`), set from `PrivateFileStoreRequest.requiredAuthority`; the export stores `PLATFORM_TENANT_MANAGE`. FILE's owner list filters such documents in the query (exact paging), and metadata, access token, download, visibility and archive / delete answer **404** `FILE_DOCUMENT_NOT_FOUND` to a caller without the authority (decided: 404, existence not revealed); `FilePrivateStoreApi.issueDownloadToken` checks it; `FileDocumentLookupApi.isAvailable` is false (no NOTIF attachment can name an archive). The alternative (TENANT-only re-download / delete endpoints) is weighed and rejected in ADR-TENANT-006 | `TenantExportIntegrationTest.theArchiveIsRestricted_aFileViewerSeesNothingOfIt_theOperatorUsesIt_andDeletingItRemovesItsBytes` (viewer: list empty, 404 metadata / token / delete, another user's token 401; ordinary document visible; operator: list, metadata, download, delete); `FileDocumentDomainRestrictedTest`; TC-CORE-TENANT-057, -058; reviewer probe: viewer list `ids=[]`, token **404** (was 200 / 200 / download 200) |
| 2 MEDIUM | "Deleting" an archive kept its bytes (6.7 MB) | Deleting a restricted document removes its content through its provider — `DB`: `FILE_CONTENT` NULL in the transaction; `LOCAL` / `S3`: the object is deleted after the commit — and keeps a `DELETED` tombstone (name, size, hash, owner); `ARCHIVE` keeps the bytes; ordinary documents unchanged (RULE-FILE-006). Retention job **not done** (follow-up, DEVIATIONS) | `Db/LocalStorageFileIntegrationTest.deletingARestrictedDocument_removesItsContent_archivingOrDeletingAnOrdinaryOneKeepsIt`; the restricted test above (`FILE_CONTENT` NULL); against the run's DB: the probe's 6 767 062-byte archive → `DELETED`, content length NULL, size and hash kept |
| 3 LOW | Public slugs (and object keys on LOCAL / S3) reached `AUDIT/CORE_AUDIT_EVENT.csv` through `CHANGES` | `@Audited(entityType = "FILE_DOCUMENT", ignore = {"storageRef", "publicSlug"})`; the AUDIT contributor removes both fields from every `FILE_DOCUMENT` row's `CHANGES` (JSON array, order kept, NULL when empty) — older rows included. `AuditedEntitiesCoverageIntegrationTest.fileCategoryAndFileDocument` asserted `publicSlug` **was** audited: changed to assert it (and `storageRef`) is not, while `visibility` still is | main export test seeds a logo (PUBLIC slug) and a legacy audit row with an object key `tenant/<id>/legacy-object-key-…` and a slug: neither in the archive, the row's `fileName` change kept; reviewer probe: "no public slug" now PASS |
| 4 LOW | A failed store still committed the tenant's `TENANT_EXPORTED` | both rows written in the PLATFORM storing transaction (the tenant's with an explicit `tenantId`) | `aFailedStore_answers500_recordsTenantExportedNowhere_andFreesTheSlot` (a test trigger refuses the insert); probe: audit rows after a failed storage `(1, 1, 1, 1)` (was `(1, 2, 1, 1)`) |
| 5 note | No global cap | `erp.core.tenant.export.max-concurrent` (2, per node) → 429 `TENANT_EXPORT_BUSY` (`TOO_MANY_REQUESTS`, no `Retry-After`: the envelope carries no headers — `CUSTOMER_LOGIN_RATE_LIMITED` precedent); the tenant's own running export answers 409 first; `max-rows` bounds rows, not width (ADR, X8) | `atTheConcurrencyLimit_anotherTenantsExportIs429Busy`; `TenantExportGuardTest` (3); `TenantDomainTest` |
| 6 notes | `TOKENS_INVALID_BEFORE` derivable from `UPDATED_AT`; TC-056 exact count | X7 note (not a secret; kept as is — the probe's two "tokens-invalid-before" FAIL lines are this, by design); TC-056 counts PLATFORM's rows before and after | X14; TC-CORE-TENANT-056 |

Verification after round 1:
- `mvn -q -o verify` (clean `target/`, code d265b24): BUILD SUCCESS, erp-core lines **82.92 %**; erp-core **669** tests, 0
  failures, 0 errors, 0 skipped (+3 export integration, +1 guard, +2 FILE domain, +2 storage — DB and LOCAL);
  erp-app-reference **10**, 0 / 0 / 0.
- HTTP suite run **`261008104981`**, P-LIVE, port 18110, fresh `erp_tm_c5` (dropped afterwards): **204 PASS / 0 FAIL /
  0 BLOCKED** (22 profile cases not run) — `docs/test-api/results/20261008T104926-P-LIVE.json` / `-report.md`, replacing
  run `261008100062`. New TENANT-057 / -058 run before TENANT-046.
- api-docs: `review` → `file/` six endpoints (+ the 404 `FILE_DOCUMENT_NOT_FOUND` row from `assertVisibleTo`, walk lists)
  and `tenant/` (+ 429 `TENANT_EXPORT_BUSY`, error table) → `update`; **0 table rows removed**. `check`: SEC, TENANT, MDL,
  SEQUENCE, REPORT PASS; FILE, NOTIF, CU, AUDIT, APP — the same five known FAIL lines as before (diffed).
  `check_completeness.py`: sum 125, missing 0, duplicated 0, stale 0, PASS.
- Reviewer probes against this app (DB `erp_tm_c5`, after the P-LIVE run): `probe.py` **166 PASS / 2 FAIL / 12 INFO**
  (was 165 / 3 / 15): the public-slug FAIL is gone, the viewer gets 404, the failed store leaves no audit row; the two
  remaining FAILs are the accepted `TOKENS_INVALID_BEFORE ≈ UPDATED_AT` note (6). `probe_rr.py`: one snapshot (late
  inbox rows not in the archive, manifest 190 129 = sum). `probe_large.py`: 199 000 rows in 1.46 s, 6.8 MB, same-tenant
  409, other tenant 200, 422 above the limit, no temporary file left.

Next free ids after round 1: TENANT unchanged (REQ/AC-038, RULE-029, POL-018, US-017, XM-005, DBF-046, ADR-007); FILE
**RULE-013**, XM-004, API-FILE-009, ADR-FILE-009. HTTP: **TC-CORE-TENANT-059**, PLATFORM-006, SEC-056, NOTIF-017; counts
TENANT 58, total 226, P-LIVE 204. Migration V22 used (next core migration V23).
