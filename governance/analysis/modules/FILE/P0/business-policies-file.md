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
Steps          : 02, 03, 05, 06, 07, 08, 10
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. Policy ids are not minted here.

| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 1 | Every file category and document belongs to one tenant; category codes are unique per tenant. | NEW | docs/steps/05-report.md; V10__tenant_schema.sql |
| 2 | Storage location is configurable: file content lives in the database (default), on local disk or in S3-compatible storage (`erp.core.files.storage` = DB / LOCAL / S3). The setting decides only where NEW uploads go; every read uses the provider recorded on the document, so switching never strands existing files. Analysis said "bytes live in the DB (BYTEA); external filesystem storage excluded"; implemented a storage SPI because the erp-core step-07 plan prescribes it (DB stays the default). | CHANGED | 07-STEP; docs/steps/07-report.md; DEVIATIONS [07] (provider selection) |
| 3 | Public files: a document may be published (`PUBLIC`) only if its category allows public files (`ALLOW_PUBLIC`); it is then served without authentication at a stable, non-guessable URL `/api/v1/public/files/{tenantCode}/{publicSlug}`. Withdrawing it (`PRIVATE`) stops that URL. Only ACTIVE documents of a still-public category are served. | NEW | 07-STEP; DEVIATIONS [07] |
| 4 | Public content never runs active content on the platform origin: inline display only for raster images and PDF; every other type is served as an attachment with `nosniff` and a sandboxing Content-Security-Policy. | NEW | DEVIATIONS [07] (review round 1) |
| 5 | POLICY-CLI-03 (time-limited private links): unchanged — private download still needs a fresh, single-use encrypted token; the token store is in-memory by default and Redis when available. | unchanged (infrastructure CHANGED) | DEVIATIONS [02], [07] (private download entry) |
| 6 | POLICY-CLI-01 limits: unchanged defaults, now configurable as `erp.core.files.max-content-bytes` / `max-request-bytes`. | CHANGED (config) | DEVIATIONS [03] |
| 7 | Every document carries a SHA-256 content hash (the public ETag). Uploads and publications raise `FileDocumentPublishedEvent`; document and category changes are recorded in the platform audit log (the content hash is not recorded). | NEW | DEVIATIONS [07], [08], [10] |
| 8 | Soft delete keeps the content (RULE-FILE-006 unchanged); a LOCAL / S3 object written by a rolled-back upload is removed again. | unchanged / NEW | DEVIATIONS [07] (`StoredObject` entry) |

Scope exceptions: "external filesystem storage" is no longer excluded (see #2). PDF processing and an
async file pipeline remain excluded.

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
