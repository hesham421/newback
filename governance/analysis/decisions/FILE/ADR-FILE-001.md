# ADR-FILE-001 — Storage provider SPI (DB / LOCAL / S3) with closed keys, reversing the BYTEA-only AUTO-DECISION

Module  : FILE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P0 → P2 — recorded after the fact (step 07)
Status  : ACCEPTED (non-breaking)

## Context
`module-registry-file.md` fixed, as an AUTO-DECISION, "File bytes stored in PostgreSQL BYTEA (not BLOB,
not Large Objects)", with "IF WRONG: revisit only if max file size grows to GB-scale". The business
policies excluded "External filesystem storage — bytes live in the DB (BYTEA)", the SRS A2 excluded
"تخزين على نظام ملفات خارجي", and `db-script.md` declared `FILE_CONTENT BYTEA NOT NULL`.

The erp-core execution plan's step 07 prescribed a storage SPI so that an application can keep file
content outside the database (local disk, S3-compatible object store) without changing the FILE API,
and prescribed the public-file feature that benefits from a CDN-served object store. The trigger the
AUTO-DECISION named (GB-scale files) did not occur; the plan did.

## Decision
`com.erp.file.storage.StorageProvider` is a public SPI (`key`, `put(StorageTarget, InputStream, size,
contentType) → StoredObject`, `get`, `delete`, `publicUrl`). erp-core ships three providers: `DB`
(`DbStorageProvider`, the row's `FILE_CONTENT`, always registered, the default), `LOCAL`
(`LocalFsStorageProvider`, when `erp.core.files.local.root` is set) and `S3` (`S3StorageProvider`, when
`software.amazon.awssdk:s3` is on the classpath and `erp.core.files.s3.bucket` is set).
`erp.core.files.storage` selects only where NEW uploads go; every document records its provider and
reference (`STORAGE_PROVIDER`, `STORAGE_REF`), and every read uses the recorded provider, so changing
the setting never strands a file. `FILE_CONTENT` became nullable (V12).

The key set is closed: `CHK_FILE_DOCUMENT_STORAGE_PROVIDER` admits only `DB`, `LOCAL`, `S3`. An
application may replace a built-in provider by contributing a `StorageProvider` bean with the same key
(or `erpDbStorageProvider` for DB), or replace the whole registry; it cannot register a fourth key.
Providers report failures only as `LocalizedException(FILE_STORAGE_UNAVAILABLE)`, never a raw I/O
exception; the DB provider's `put` must run inside the transaction that persisted the row. The LOCAL and
S3 key layout is `<tenantId>/<category>/<yyyy>/<MM>/<documentId>_<filename>` with every segment
sanitised, a path-traversal guard on LOCAL, and deletion of the object when the upload's transaction
rolls back.

## Consequences
- The BYTEA-only AUTO-DECISION, the two scope exclusions and DBF-0008's `NOT NULL` are superseded;
  DB storage stays the default, so an application that configures nothing behaves as analysed.
- Startup validation: an unknown `storage` value, LOCAL without an existing writable root, S3 without
  a bucket or without the SDK each fail the start with a message naming the property.
- A new provider key needs a core migration extending the CHECK (additive, fixed forward) before an
  application can use it; this is deliberate — the key is persisted on every row and the set must be
  known to every reader.
- With S3 and `s3.public-base-url`, public documents redirect to the bucket / CDN: ADR-FILE-002
  records the exposure constraints of that mode.
- `FILE_STORAGE_UNAVAILABLE` answers 500 because `Status` has no 503 (ADR-FILE-007).

## Traces
ENTITY-FILE-001 · DBF-0008 · module-registry-file.md §AUTO-DECISIONS (BYTEA) ·
business-policies-file.md §SCOPE EXCEPTIONS · srs.md A2 ·
erp-core/src/main/java/com/erp/file/storage/StorageProvider.java:14-33 ·
StorageProviderRegistry.java:23-46 · StorageKeys.java · StoragePaths.java:21-36 ·
LocalFsStorageProvider.java:47-65, 100-111 · DbStorageProvider.java:14-40 ·
erp-core/src/main/java/com/erp/autoconfigure/FileStorageAutoConfiguration.java:30-46, 58-87 ·
erp-core/src/main/resources/db/migration/core/V12__file_storage.sql L16-35 ·
docs/steps/07-report.md · docs/DEVIATIONS.md [07] (provider selection, `StoredObject`,
`FileStorageProperties`)
