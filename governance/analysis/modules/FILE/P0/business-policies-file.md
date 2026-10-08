## BUSINESS POLICIES — FILE SERVICE
══════════════════════════════════════════════════════════════════
Module      : File Service (FILE)
P0 Date     : 2026-09-01
Domain KB   : none supplied — derived from domain-profile-ERP.md + ARCH-REF-1.10
P1 reads    : CLIENT-SPECIFIC entries → RULE-IDs marked "Source: Client"
              Standard rules → applied by P1 directly
══════════════════════════════════════════════════════════════════

CLIENT-SPECIFIC POLICIES
──────────────────────────────────────────────────────────────────
Concrete defaults carried from the ARCH-REF (reviewable per consumer);
the cross-cutting design policies (POLICY-CLI-01..03 in business-policies-
CU.md) also apply.

POLICY-CLI-01: File size limits
  Rule   : Content ≤ 5MB (application enforced); HTTP request ≤ 10MB
           (multipart). Overridable per FileCategory.
  Trigger: Upload.
  Source : ARCH-REF AD-FILE-05 (reference default).

POLICY-CLI-02: Accepted file types
  Rule   : JPG/JPEG/PNG, PDF, DOC/DOCX, TXT, XLS/XLSX, ZIP/RAR/7Z;
           others → application/octet-stream. MIME auto-detected (not
           trusted from client header).
  Trigger: Upload.
  Source : ARCH-REF AD-FILE-06 (reference default).

POLICY-CLI-03: Time-limited access links
  Rule   : Every upload/download requires a freshly generated encrypted
           token; a link is invalid after its TTL (~100 min) — not reusable.
  Trigger: Upload / Download.
  Source : ARCH-REF AD-FILE-02 (reference default).

──────────────────────────────────────────────────────────────────
CUSTOM LOV VALUES
──────────────────────────────────────────────────────────────────
FileType   : IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER
FileStatus : ACTIVE, ARCHIVED, DELETED
(owned locally by File Service — see module-registry-FILE.md)

──────────────────────────────────────────────────────────────────
SCOPE EXCEPTIONS
──────────────────────────────────────────────────────────────────
Excluded : PDF processing/preview (PDFBox) — ARCH-REF RESOLUTION-03.
Excluded : Async file pipeline / message broker (RabbitMQ) — RESOLUTION-04;
           all file ops are synchronous via @Service injection.
Excluded : External filesystem storage — bytes live in the DB (BYTEA).
Deferred : Delete semantics (permanent vs soft via status=DELETED) — a P1
           decision; the status LOV supports either. Not deferred WORK,
           just a design choice for P1 to fix.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 02, 03, 05, 06, 07, 08, 10, 15
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository (`com/erp/…` = `erp-core/src/main/java/com/erp/…`).
Policy ids are not minted here.

| # | Policy | Kind | Policy-level delta | Source |
|---|---|---|---|---|
| 1 | (tenancy) | NEW | Every file category and document belongs to one tenant; category codes are unique per tenant. | docs/steps/05-report.md; V10__tenant_schema.sql |
| 2 | SCOPE EXCEPTIONS "bytes live in the DB" | CHANGED | Storage location is configurable: file content lives in the database (default), on local disk or in S3-compatible storage (`erp.core.files.storage` = DB / LOCAL / S3). The setting decides only where NEW uploads go; every read uses the provider recorded on the document, so switching never strands existing files. Analysis said "bytes live in the DB (BYTEA); external filesystem storage excluded"; implemented a storage SPI because the erp-core step-07 plan prescribes it (DB stays the default). The provider keys are closed (DB / LOCAL / S3). | 07-STEP; docs/steps/07-report.md; DEVIATIONS [07] (provider selection); ADR-FILE-001 |
| 3 | (public files) | NEW | A document may be published (`PUBLIC`) only if its category allows public files (`ALLOW_PUBLIC`); it is then served without authentication at a stable, non-guessable URL `/api/v1/public/files/{tenantCode}/{publicSlug}`. Withdrawing it (`PRIVATE`) stops that URL; archiving or deleting it also stops the URL but keeps the slug. Only ACTIVE documents of a category still allowing public files are served; the category's own active flag is not checked. | 07-STEP; DEVIATIONS [07]; com/erp/file/repository/FileDocumentRepository.java:74-82 |
| 4 | (public content safety) | NEW | Public content never runs active content on the platform origin: inline display only for raster images and PDF; every other type is served as an attachment with `nosniff` and a sandboxing Content-Security-Policy. A direct S3 / CDN URL is outside these headers and is not revoked by withdrawing the document. | DEVIATIONS [07] (review round 1); ADR-FILE-002 |
| 5 | POLICY-CLI-01 (size limits) | CHANGED | Defaults kept at 5 MB / 10 MB, configurable as `erp.core.files.max-content-bytes` / `max-request-bytes`. A category limit REPLACES the content default — it may be larger, not only tighter ("overridable per FileCategory" holds in both directions); the request ceiling is not overridable. The host application's servlet multipart limits must be at least as large. | com/erp/file/domain/FileValidationDomainService.java:39-61; DEVIATIONS [03] |
| 6 | POLICY-CLI-02 (accepted file types) | CHANGED | The platform list JPG/JPEG/PNG, PDF, DOC/DOCX, TXT, XLS/XLSX, ZIP/RAR/7Z is NOT enforced: a category without an allow-list accepts any type (stored as the sniffed type, else the filename type, else `application/octet-stream`). Only a category `allowedContentTypes` allow-list restricts uploads, and it is matched against the content-sniffed MIME (never the client header or filename). Because the JDK sniffer does not recognise PDF, Office or ZIP content, an allow-list naming those types rejects them — a known limitation to be covered by a test case. | com/erp/file/service/FileService.java:130-135, 445-458; FileValidationDomainService.java:72-86 |
| 7 | POLICY-CLI-03 (time-limited links), download | CHANGED | Private download still needs a fresh, single-use encrypted token, but the TTL is 10 minutes (not ~100), the token is bound to the user it was issued to (another user's token is refused and not consumed), and it is consumed only after the content was opened. The token store is in-memory by default and Redis when available. | com/erp/file/domain/FileAccessTokenDomainService.java:28; FileService.java:182-187, 199-227; DEVIATIONS [02], [07] (private download entry); ADR-FILE-003 |
| 8 | POLICY-CLI-03 (time-limited links), upload | REMOVED | No token gates an upload: an upload is a staff JWT call with `PERM_FILE_BROWSER_CREATE`. | FileService.java:108-111 |
| 9 | (content hash, events, audit) | NEW | Every document carries a SHA-256 content hash (the public ETag; no de-duplication). Uploads and publications raise `FileDocumentPublishedEvent`; document and category changes are recorded in the platform audit log (the content hash is not recorded). | DEVIATIONS [07], [08], [10] |
| 10 | SCOPE EXCEPTIONS "delete semantics" (RULE-FILE-006) | CHANGED | Soft delete keeps the content; a DELETED document is refused (404) for tokens, downloads and publication but stays visible in metadata and lists; no un-archive. A LOCAL / S3 object written by a rolled-back upload is removed again. | FileService.java:177-180, 216-219, 322-323; DEVIATIONS [07] (`StoredObject` entry) |
| 11 | CUSTOM LOV VALUES "owned locally by File Service" | CHANGED | The two value sets are unchanged but are MDL lookup types (`FILE_FILE_TYPE`, `FILE_FILE_STATUS`, owner FILE) seeded by V8, not FILE-local tables. | V8__mdl_seed.sql; ADR-FILE-005 |
| 12 | SCOPE EXCEPTIONS "External filesystem storage" | REMOVED | LOCAL and S3 providers exist (see #2). PDF processing and an async file pipeline remain excluded. | docs/steps/07-report.md |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.4 — shared image store
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Policy ids are not minted here; full text in `P1/srs.md` → "Implementation Addendum — erp-core 1.3.0".

| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 1 | Other core modules store small public images (profile photos, tenant logos) through one FILE image store: the type is taken from the content (PNG, JPEG, WebP; SVG only where the caller allows it and only when it carries no active content), the size limit from the caller. | NEW | RULE-FILE-008/009 |
| 2 | Such images are public at a non-guessable URL as soon as they are stored and stop being served the moment they are replaced or removed (ADR-FILE-008). They need no public category: the publication decision is the image store's, not tenant data. | NEW | RULE-FILE-010; ADR-FILE-008 |
| 3 | 1.2.0 policy #4 (no active content on the platform origin) is unchanged: SVG stays an attachment on the public path. | unchanged | srs.md 1.3.0 §3 |
