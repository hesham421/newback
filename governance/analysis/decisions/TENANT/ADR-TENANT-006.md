# ADR-TENANT-006 — Synchronous, row-bounded tenant export stored as a PRIVATE PLATFORM document behind a single-use download token

Module  : TENANT (export SPI implemented by every core module; storage by FILE)     Version : erp-core 1.3.0 (tenant-maturity plan, package C.5)     Stage raised : P1 (Requirements) — before the code
Status  : ACCEPTED (erp-core 1.3.0, package C5; written PROPOSED-before-code in the analysis commit 4dac933, accepted
          after the code check)

## Context
The plan (§5 C.5, item 16) asks for a platform operation that hands a tenant its data: every core module writes the
tenant's rows as CSV, the CSVs are zipped, and the archive reaches the platform operator — never a password hash,
never file bytes. The shared schema (ADR-TENANT-001) makes a tenant's data a `TENANT_ID`-filtered copy of 20
tenant-scoped tables plus its `CORE_TENANT` row, so the work is bounded by the tenant's row count. Three questions
had real alternatives:
- **When the archive is built**: inside the request (synchronous), or by a background job the client polls.
- **How the archive reaches the operator**: streamed as the HTTP response body, or stored and downloaded afterwards.
- **How a second export of the same tenant is prevented**: an in-memory guard per node, or a database lock / status row.
The plan proposes "synchronous v1, bounded by `erp.core.tenant.export.max-rows`, stored as a PRIVATE `FILE_DOCUMENT`
in PLATFORM, `{ fileId, downloadToken }`, in-memory guard keyed by tenant id".

## Decision
- **Synchronous v1, bounded.** `POST /api/v1/platform/tenants/{id}/export` builds the archive inside the request.
  Inside `TenantContext.callAs(id)` one read-only `REPEATABLE READ` transaction gives one snapshot: every
  `TenantExportContributor` first counts its rows (`countRows`), and a total above
  `erp.core.tenant.export.max-rows` (default 200 000) answers 422 `TENANT_EXPORT_TOO_LARGE` before anything is
  written; then each contributor streams its rows (JDBC fetch size 1 000) into a ZIP on a temporary file.
- **Stored, then downloaded with FILE's token.** The ZIP becomes a PRIVATE, uncategorised `FILE_DOCUMENT` of the
  PLATFORM tenant (`CORE_TENANT` / {id} / `TENANT`, file type `ARCHIVE`) through the new FILE cross-module API
  `FilePrivateStoreApi` (XM-FILE-003); the answer carries the document id and FILE's existing single-use download
  token (AES-GCM, 10 minutes, bound to the issuing username, consumed by the first successful download —
  RULE-FILE-003), downloaded with `GET /api/v1/files/download?token=…`. `TENANT_EXPORTED` is recorded in PLATFORM and
  in the tenant.
- **One export of a tenant at a time, per node.** An in-memory set of tenant ids (`TenantExportGuard`); a second
  export of a tenant whose id is in the set answers 409 `TENANT_EXPORT_IN_PROGRESS`; the id is removed on every path.
- **Contents.** One CSV per table, written by the owning module (the SPI mirrors `TenantProvisioningContributor`),
  UTF-8 with a byte-order mark, RFC 4180, formula guard, ordered by primary key, plus `manifest.json`; the exclusions
  of RULE-TENANT-027 (password and token hashes, session token references, channel credentials, notification
  variables, file bytes and storage internals, the token cut-off, idempotency records).

Reasons:
1. **The bound makes synchronous safe.** 200 000 rows stream in seconds and the ZIP of such an export is a few
   megabytes; a job framework (status table, worker, polling endpoint, clean-up of abandoned jobs) would be the
   largest part of the package for a rare, operator-driven action. The limit is a property, so an installation with
   larger tenants raises it knowingly.
2. **One snapshot.** The count and the files come from the same `REPEATABLE READ` transaction: the limit check and the
   manifest's counts describe exactly the archive, and a row inserted during the export appears in neither.
3. **Stored, so a failed download costs nothing.** A response stream that breaks half-way has to be rebuilt; a stored
   document can be fetched again (a fresh token through `POST /api/v1/files/{id}/access-token`) and is audited as an
   object. FILE already owns storage providers, PRIVATE documents and single-use tokens — nothing new to secure.
4. **PLATFORM owns the archive.** Stored in the PLATFORM tenant, the archive is invisible to the tenant's own users
   (who could otherwise read other users' data in it) and to every other tenant; the token is bound to the operator.
5. **The guard is cheap and its failure mode is harmless.** The export only reads the tenant; two concurrent exports
   on two nodes produce two documents, nothing worse. A database lock would need a lease and a recovery for a crashed
   node to protect against a duplicate that does no damage.

## Alternatives rejected
- **Asynchronous job + status resource** (`202 Accepted`, `GET /exports/{jobId}`): no request held open and no limit
  needed, but a new table, a worker, a polling contract for the frontend and the clean-up of orphaned jobs. Kept as
  the 1.4.0 path if tenants outgrow the limit; the SPI does not change (a job would call the same contributors).
- **Streaming the ZIP as the response body** (`application/zip` from the POST): no storage, but no retry without
  re-exporting, no audit object, and a JSON error cannot be answered once the body has started (a failing contributor
  would truncate the archive silently).
- **A PUBLIC document or a tenant-owned document**: a PUBLIC slug is a bearer URL for a whole tenant's data; a
  document in the tenant's own rows would be readable by the tenant's administrators (`PERM_FILE_BROWSER_VIEW`).
- **A database lock / "export running" row**: needs its own commit and a recovery for a crash; the in-memory guard
  plus the harmless multi-node duplicate is enough for v1.
- **No pre-count, only a cap while streaming**: the plan asks to refuse before exporting; a cap alone would build and
  throw away a large ZIP before refusing (the cap stays as a second line, for a contributor whose count disagrees).

## Review round 1 (package C5) — amended Decision
- **The archive is a restricted document.** Stored as an ordinary PRIVATE document, it was readable by any PLATFORM
  user holding `PERM_FILE_BROWSER_VIEW` (owner list → access token → download). The private store now records the
  authority its producer names (`FILE_DOCUMENT.REQUIRED_AUTHORITY`, V22; the export names `PLATFORM_TENANT_MANAGE`)
  and FILE refuses every generic operation on such a document — list (filtered), metadata, token, download,
  visibility, archive, delete — with **404** to a caller without it (existence not revealed), FILE RULE-FILE-012.
  Alternative weighed: keep private-store documents out of the FILE API and give TENANT its own re-download and
  delete endpoints — rejected: it duplicates FILE's token, download and delete paths, and the column protects every
  present and future FILE endpoint, for any producer.
- **Deleting the archive removes its bytes** (a `DELETED` tombstone keeps the metadata); FILE's soft delete still
  keeps the bytes of ordinary documents. A retention job (`erp.core.tenant.export.retention`) is a follow-up.
- **Both `TENANT_EXPORTED` rows in one transaction** with the stored document; **at most
  `erp.core.tenant.export.max-concurrent` exports** (default 2) at once per node, else 429 `TENANT_EXPORT_BUSY`.
- **Audit changes scrubbed.** `FileDocument` no longer audits `storageRef` / `publicSlug`; the AUDIT contributor
  removes them from older `FILE_DOCUMENT` rows' `CHANGES` when exporting.

## Consequences
- `max-rows` bounds the number of rows, not their width (text columns, audit `CHANGES`): the archive's size follows the
  data. Each running export holds one connection, an open snapshot and (with the `DB` provider) the archive in memory
  while storing it — hence `max-concurrent`.
- The request is held for the duration of the export (seconds; tens of seconds near the limit); the frontend shows
  progress and never retries automatically (srs-tenant.md X12). Proxies with short timeouts must allow it.
- The snapshot transaction holds one connection and an MVCC snapshot for that duration.
- The `DB` storage provider reads the archive into one `BYTEA` value (bounded by the limit); `LOCAL` / `S3` stream it.
  FILE's upload limits do not apply (the archive is not an upload).
- Archives stay as PRIVATE PLATFORM documents until deleted (`DELETE /api/v1/files/{id}`); no retention job in v1.
- The SPI has a third method, `countRows`, beside the plan's two; an application adding its own contributor
  implements all three (`docs/CONSUMING.md` §3).
- Behind a load balancer the guard is per node (RULE-TENANT-028).
- A tenant with more rows than the limit cannot be exported until the property is raised.

## Traces
ENT-TENANT-001 · REQ-TENANT-037 · RULE-TENANT-011, RULE-TENANT-027, RULE-TENANT-028 · POL-TENANT-017 · US-TENANT-016 ·
XM-TENANT-004 · FILE XM-FILE-003, RULE-FILE-003, RULE-FILE-011 · ADR-TENANT-001, ADR-TENANT-003 (not wrapped in the
idempotency helper) · plan §5 C.5, §9
Accepted after the code check of package C5 (code commit dc2b535, tests 1990849).
