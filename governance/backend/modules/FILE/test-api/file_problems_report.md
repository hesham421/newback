# FILE — legacy API suite: problems report (erp-core 1.2.0)

- Run date: 2026-10-05 · RUN_ID `200330`
- Target: erp-core 1.2.0 @ newback main (reference app jar, dev profile, DB storage provider) at `http://localhost:7303`
- Script: `backend/modules/FILE/test-api/test_file_apis.py` (adapted 2026-10-05; regenerates this report on every run)
- Sources of truth for intentional changes: newback `docs/DEVIATIONS.md`, `docs/CHANGELOG.md`, `docs/api-docs/file/**`, and the "Implementation Addendum — erp-core 1.2.0" sections of `analysis/modules/FILE/P0..P2`.

## Baseline (original script)

- **Original script, unmodified** (only BASE_URL pointed at the instance via argv[1]): **0 passed · 0 failed · 0 run — aborted at login** with `FATAL: could not authenticate — ENVIRONMENT_FAILURE` (exit 2). `POST /api/v1/sec/auth/login {admin/admin}` without a tenant header answers 400 `TENANT_REQUIRED`; with `X-Tenant-Code: PLATFORM` it answers 401 `SEC-401-INVALID-CREDENTIALS` (admin/admin no longer exists).
- **Diagnostic run** (original script, login call alone shimmed to send `X-Tenant-Code: PLATFORM` + the bootstrap password; nothing else changed): **42 passed · 0 failed**, 7 observations. Every behaviour assertion of the legacy suite holds on erp-core 1.2.0.

## Classification of baseline failures and adaptations

Classes: (a) intended change, (b) test defect, (c) suspected app defect.

| Test / spot | Class | Reference |
|---|---|---|
| ALL (login, suite aborted before any TC) — 400 TENANT_REQUIRED without tenant header | (a) INTENDED CHANGE | DEVIATIONS [05] TenantResolutionFilter, resolution rule (3); smoke test logs in with `X-Tenant-Code: PLATFORM` |
| ALL (login) — admin/admin → 401 SEC-401-INVALID-CREDENTIALS | (a) INTENDED CHANGE | DEVIATIONS [04] Task 2 (admin seeded PENDING, password from `ERP_BOOTSTRAP_ADMIN_PASSWORD`) and [04] admin password in the reference app (admin/admin asserted 401) |
| Hard-coded BASE_URL / credentials (not a failure; brief hygiene) | (b) TEST-DEFECT | secrets must not be hard-coded; BASE_URL now from env `ERP_BASE_URL`; the fixture user's password is generated at random per run |
| TC-BE-FILE-019 permission half — was a declared GAP (`TODO: SEC-PENDING`), now executed (enabled gap; assertions as the TC states them) | (b) TEST-DEFECT (stale gap) | the gap's premise was stale: FILE's @PreAuthorize checks were already live before erp-core (newback initial commit a9ed097); erp-core 1.2.0 keeps them (`PERM_FILE_*` code-defined by `FilePermissions`, P1/srs.md addendum §4); 403 wire code `ACCESS_DENIED` per docs/api-docs/file/index.md |
| Cleanup purge note (FILE_CONTENT always holds the bytes) | (a) INTENDED CHANGE (text only) | P2/db-script.md addendum (`FILE_CONTENT` nullable, DB provider only); DEVIATIONS [07] provider selection |

Counts: (a) 3 · (b) 2 · (c) 0. No assertion was weakened; every TC id is kept.

## Final results (this run)

**46 passed · 0 failed** across 8 suites · 7 observation(s), which never affect the totals.

| Suite | Passed | Failed |
|---|---|---|
| PREFLIGHT (stage A0) | 2 | 0 |
| FileCategory | 8 | 0 |
| FileDocument — upload (API-FILE-001) | 10 | 0 |
| FileDocument — metadata & list | 5 | 0 |
| FileDocument — access token & download | 5 | 0 |
| FileDocument — lifecycle (API-FILE-006) | 6 | 0 |
| FileLookups (API-FILE-008) | 3 | 0 |
| Auth delegation (RULE-FILE-004) | 7 | 0 |

## Suspected app defects — class (c)

_None._ Every remaining assertion passes on erp-core 1.2.0.

## Findings carried over from the legacy report (not failures)

- **TC-BE-FILE-009 — ERR-0004 `FILE_DOCUMENT_OWNERSHIP_REQUIRED` is unreachable over HTTP** (still true on 1.2.0). `@Valid @ModelAttribute UploadRequest` refuses a missing `moduleCode` with the platform `VALIDATION_ERROR` (400, `fieldErrors` naming the field) before the domain guard runs. The test asserts that documented platform shape, so it passes; whether the plan's ERR-0004 expectation should be narrowed to the in-process path is an owner decision. Not an erp-core 1.2.0 regression.
- **Upload multipart part names are documented as a nested `request.*` object** in `docs/api-docs/file/endpoints/file-documents.md`; the controller binds flat fields and the nested spelling answers 400 `VALIDATION_ERROR` (observation below). Generator rendering gap.
- **The `action` query parameter on `DELETE /api/v1/files/{id}` is documented without its allowed values** (`ARCHIVE` default, `DELETE`); any other value answers 400 `FILE_DOCUMENT_INVALID_TRANSITION`, a code whose documented status is 422.
- **FILE error codes are `FILE_*` SCREAMING_SNAKE**, not the `{MOD}-{http}-{SLUG}` format of `api-verify-config.md` §3 — Legacy Path convention, flagged for a human decision.
- Resolved since the legacy report: the api-doc now documents `201 Created` for `POST /api/v1/files` and `POST /api/v1/files/categories`.

## Out of scope of this legacy suite

- erp-core 1.2.0's new FILE capabilities (storage SPI DB/LOCAL/S3, `PATCH /api/v1/files/{id}/visibility` with `FILE:DOCUMENT:PUBLISH`, category `allowPublic`, public URLs `/api/v1/public/files/{tenantCode}/{publicSlug}` with the inline allow-list and `nosniff`/CSP headers, tenant confinement) have no TC in this legacy plan; they are covered by newback `docs/test-api/core_api_verify.py` (TC-CORE-FILE-*). The new metadata fields `storageProvider`, `visibility`, `publicUrl` do not disturb any legacy assertion (none of them carries bytes).

## Preconditions (stage A0)

_All preconditions satisfied._

## Observations (stage E — not pass/fail)

- **stage A0 note — moduleCode/ownerType are unvalidated free strings** — owner triple (920330/PURCHASE_ORDER/PROC) readable: HTTP 200, totalElements=0 — moduleCode is an unconstrained string here (no FK / registry validation on upload), so it is not a precondition row
- **stage E — deactivate an already-deactivated category (idempotency)** — second DELETE on an already-inactive category -> HTTP 204 /None (no artifact states the expected outcome)
- **stage E — api-docs show a nested `request.*` multipart spelling** — documented nested spelling (request.ownerId/...) -> HTTP 400/VALIDATION_ERROR; the flat spelling the controller binds is what succeeds
- **stage E — two access tokens issued for the same file** — two concurrently-issued tokens: first -> HTTP 200, second -> HTTP 200 (no artifact states whether issuing a new token invalidates an unconsumed one)
- **stage E — a DELETED file's bytes cannot be re-read over HTTP** — access token for a DELETED file: HTTP 404/FILE_DOCUMENT_NOT_FOUND — a soft-deleted file is treated as gone, so the download route cannot re-prove byte retention for this path
- **stage E — an undocumented `action` value on DELETE /files/{id}** — undocumented action value -> HTTP 400/FILE_DOCUMENT_INVALID_TRANSITION (api-docs do not enumerate the allowed `action` values)
- **stage E — an MDL key owned by another module** — a real MDL key FILE does not own -> HTTP 404/FILE_LOOKUP_KEY_UNKNOWN (FILE is not a generic MDL pass-through)

## Coverage gaps (not executed, not claimed as passing)

- FileCategory has no documented activate endpoint (api-docs lists only GET/PUT/DELETE/POST/search), so the deactivate->activate round trip named in the ENTITY CRUD CHECKLIST cannot be exercised over HTTP; deactivated rows stay inactive.
- TC-BE-FILE-006 (expired half) — the access token TTL is 10 minutes (FileAccessTokenDomainService.TOKEN_TTL), so proving expiry needs a 10-minute idle wait inside the run. The REUSED half of the same TC is exercised end-to-end (issue -> download -> download again -> 401); expiry is NOT executed and is not claimed as passing.

## Surviving records

- FILE_DOCUMENT rows [1, 2, 3, 4, 5] — set to fileStatusId=DELETED (soft). FILE documents no hard delete, and RULE-FILE-006 RETAINS the bytes; with the default DB storage provider the BYTEA FILE_CONTENT column of each of these rows still holds this run's uploaded payload (~2048 KB for the largest; with LOCAL/S3 the object stays in that store). Purge SQL: DELETE FROM FILE_DOCUMENT WHERE ID IN (1, 2, 3, 4, 5);
- FILE_CATEGORY rows [1, 2, 3] (codes CONTRACTS_200330, IMAGES_200330, RETIRED_200330) — deactivated (soft); FILE documents no hard delete and no activate endpoint. Purge SQL: DELETE FROM FILE_CATEGORY WHERE ID IN (1, 2, 3);
- SEC_USER rows [2] (username file-noperm-200330, tenant PLATFORM, no role) — deactivated; SEC exposes no hard delete for users.

### Permanent residue (reference tables other modules' rules read)

_None. FILE_CATEGORY and FILE_DOCUMENT are module-local; FILE reads MDL's lookups but never writes to them._

## Privileges

No permission grant was created or revoked by this run. The run authenticates as the PLATFORM bootstrap `admin` (SYS_ADMIN, a super role holding every active catalog authority — DEVIATIONS [06]). TC-BE-FILE-019's fixture user is created with no role at all and deactivated by cleanup. No standing privilege was left behind.

