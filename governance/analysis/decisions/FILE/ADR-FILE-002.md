# ADR-FILE-002 — Unauthenticated public files: inline allow-list, CSP sandbox, S3 direct URLs not revocable

Module  : FILE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (SRS) — recorded after the fact (step 07, review round 1)
Status  : ACCEPTED (non-breaking)

## Context
The analysis knew only token-protected downloads (RULE-FILE-003, POLICY-CLI-03). Step 07 added public
files — a document a storefront can show without a token — at
`GET /api/v1/public/files/{tenantCode}/{publicSlug}`, outside both the staff and the customer
authentication, with the tenant taken from the path. Serving tenant-uploaded content without
authentication on the platform origin raises two risks the review of step 07 named: a file that a
browser renders (HTML, SVG, scripts) could run on the platform origin, and an object store exposed
through a CDN serves copies the platform no longer controls.

## Decision
1. Only a PUBLIC, ACTIVE document in a category whose `ALLOW_PUBLIC` is still true is served;
   anything else is 404 `FILE_DOCUMENT_NOT_FOUND` (the category's own active flag is not consulted).
   The endpoint allows GET and HEAD only; other methods are not permitted.
2. Content is served, never refused, but made inert: `Content-Disposition: inline` only when the
   stored, content-sniffed type is one of `image/png`, `image/jpeg`, `image/gif`, `image/webp`,
   `image/avif`, `image/bmp`, `application/pdf` (`FileDocumentDomain.INLINE_SAFE_CONTENT_TYPES`;
   `image/svg+xml` deliberately excluded); everything else is `attachment`. Every answer (200 and 302)
   carries `X-Content-Type-Options: nosniff`, `Content-Security-Policy: sandbox; default-src 'none'`,
   `Cache-Control: max-age=86400, public` and `ETag: "<sha256>"`. No `If-None-Match` / 304 handling.
3. When the provider can serve the content itself (S3 with `erp.core.files.s3.public-base-url`), the
   endpoint answers 302 to `<base>/<key>`. Objects are written without an ACL; private and public
   objects share the bucket under a predictable key layout; the operator must expose only a public
   prefix on the CDN, serve it on an origin other than the platform's, and set the disposition /
   nosniff / CSP headers there. Withdrawing a document (PRIVATE, archive, delete) stops the platform
   URL but does not revoke a direct S3 / CDN URL already handed out.
4. On the public path the path tenant always wins: a token of another tenant is dropped (treated as
   anonymous); an unknown tenant answers 404 `TENANT_NOT_FOUND`, a suspended one 403
   `TENANT_SUSPENDED`.

## Consequences
- A storefront may publish a downloadable spec sheet or any other type; it simply downloads instead
  of rendering. Publishing unsafe types is not refused.
- A sanitised SVG (ADR-FILE-008, logos — package D's ADR, draft in docs/plans/tenant-maturity-analysis-reference.md) is still served as an attachment unless the inline list is
  extended for it — to be settled with that ADR.
- The S3 direct-URL mode trades revocability for CDN delivery; an operator who needs revocation keeps
  `s3.public-base-url` unset (the platform streams the object and applies rule 1 on every request).
- `FileDocumentLookupApi.publicUrl` and the metadata `publicUrl` apply exactly the conditions of rule
  1 (`FileDocumentDomain.isPubliclyServable`), so no URL is handed out that would answer 404.

## Traces
RULE-FILE-003 · RULE-FILE-004 · publish rule, public serving, public URL and S3 constraint rows
(`srs.md` → 1.2.0 addendum §2) · `FILE_DOCUMENT_NOT_FOUND`, `TENANT_NOT_FOUND`, `TENANT_SUSPENDED`
(§3) · erp-core/src/main/java/com/erp/file/controller/PublicFileController.java:25-84 ·
erp-core/src/main/java/com/erp/file/domain/FileDocumentDomain.java:73-102 ·
erp-core/src/main/java/com/erp/file/repository/FileDocumentRepository.java:74-82 ·
erp-core/src/main/java/com/erp/file/service/PublicFileUrls.java:45-61 ·
erp-core/src/main/java/com/erp/file/storage/S3StorageProvider.java:17-28 ·
erp-core/src/main/java/com/erp/autoconfigure/ErpCoreSecurityAutoConfiguration.java:98, 180-181 ·
erp-core/src/main/java/com/erp/autoconfigure/ErpCoreProperties.java:252-261 ·
docs/steps/07-report.md · docs/DEVIATIONS.md [07] (public GET details; review round 1: security,
redirect path, S3 `public-base-url` risk, public path methods, `publicUrl` consistency; rebase onto 06)
