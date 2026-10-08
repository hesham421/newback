# ADR-FILE-007 — `FILE_STORAGE_UNAVAILABLE` answers 500 (no 503 in `Status`)

Module  : FILE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (SRS) — recorded after the fact (step 07)
Status  : ACCEPTED (non-breaking)

## Context
The storage SPI (ADR-FILE-001) introduced a class of failure the analysis had no code for: the
provider recorded on a document is not configured in this deployment, the provider's I/O fails (disk,
bucket), a stored reference is malformed or would escape the LOCAL root, or a DB-provider row carries
no content. Semantically these are "service unavailable" conditions (HTTP 503), but
`com.erp.common.domain.status.Status` — the single mapping every `LocalizedException` resolves through
— offers `INTERNAL_ERROR` (500) and no 503, and step 07's scope did not include changing `common`.

## Decision
`FileErrorCodes.FILE_STORAGE_UNAVAILABLE` is raised with `Status.INTERNAL_ERROR` and therefore answers
HTTP 500, with the provider key as its message argument (`File storage ''{0}'' is unavailable.` /
`مخزن الملفات ''{0}'' غير متاح`). It is the only exception a `StorageProvider` may throw (the SPI's
contract), and it is reachable from upload, download, the public endpoint, the visibility change and —
through `PublicFileUrls.of` for a servable public document whose provider is gone — from metadata and
the owner list.

## Consequences
- Clients distinguish a storage outage from a programming error by the error code, not by the HTTP
  status; monitoring that keys on 5xx sees both alike.
- Adding `Status.SERVICE_UNAVAILABLE` (503) to `common` is an additive MINOR change; if it is added,
  switching this code to it changes the HTTP status of an existing error response — the kind of
  change `docs/RELEASE.md` records explicitly in the changelog, as the 1.2.0 release did for three
  500 → 4xx responses.
- The startup validation of `FileStorageAutoConfiguration` prevents the most common cause (a selected
  provider that cannot be built) from ever reaching a request.

## Traces
`FILE_STORAGE_UNAVAILABLE` (`srs.md` → 1.2.0 addendum §3) · storage SPI contract (§6) ·
erp-core/src/main/java/com/erp/file/exception/FileErrorCodes.java:48-49 ·
erp-core/src/main/java/com/erp/file/storage/StorageProvider.java:14-16 ·
StorageProviderRegistry.java:40-46 · LocalFsStorageProvider.java:61-64, 100-115 ·
DbStorageProvider.java:48-50, 65-79 · S3StorageProvider.java ·
erp-core/src/main/java/com/erp/file/service/PublicFileUrls.java:55 ·
erp-core/src/main/resources/i18n/messages.properties L160 · messages_ar.properties L157 ·
docs/api-docs/file/index.md (Known Error Codes) · docs/steps/07-report.md ·
docs/DEVIATIONS.md [07] (`FILE_STORAGE_UNAVAILABLE` status) · docs/RELEASE.md · docs/CHANGELOG.md [1.2.0]
