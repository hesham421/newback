# ADR-FILE-001 — Profile photos and logos are PUBLIC documents with non-guessable slugs
Status      : ACCEPTED (non-breaking)
Stage       : implementation (erp-core 1.3.0)      Module: FILE       Version: erp-core 1.3.0
Lane        : tenant-maturity plan package D (D.4, shared with package E)
traces      : XM-FILE-002, RULE-FILE-008, RULE-FILE-009, RULE-FILE-010, XM-SEC-006, REQ-SEC-087

## Context
erp-core 1.3.0 stores two kinds of small images that every screen renders: a staff user's profile photo
(package D, `SEC_USER.PHOTO_FILE_ID`) and a tenant's logo (package E, `CORE_TENANT.LOGO_FILE_ID`, shown on
the login page before any token exists). FILE offers two ways to serve a document:
- **PRIVATE + per-request token**: `POST /api/v1/files/{id}/access-token` (permission
  `PERM_FILE_BROWSER_VIEW`), then `GET /api/v1/files/download?token=` — single-use, bound to the caller,
  about 100 minutes.
- **PUBLIC**: step 07's `GET /api/v1/public/files/{tenantCode}/{publicSlug}`, no authentication, a
  24-byte random slug (base64url, 192 bits), cached for a day, `inline` for raster images.

A second question is how the image store may publish at all: step 07 lets only a document whose
**category** has `ALLOW_PUBLIC = TRUE` become (and stay) public. Options: seed a system category per
tenant (`V1x` seed for existing tenants plus a FILE provisioning contributor for new ones), create it
lazily on first use, or let image-store documents carry no category.

## Decision
1. Photos and logos are **PUBLIC** documents with a random slug, served on step 07's public path; no new
   table, endpoint or token mechanism. `DELETE` (photo removal, logo removal, replacement) discards the
   document — soft-deleted and unpublished, so its URL answers 404 at once — and a replacement gets a new
   slug, hence a new URL.
2. Image-store documents carry **no category**. The public lookup serves a PUBLIC, ACTIVE document whose
   category allows public files **or that has no category** (RULE-FILE-010). Only `FileImageStoreApi`
   can produce such a document: `PATCH /api/v1/files/{id}/visibility` still refuses PUBLIC for a document
   without an `ALLOW_PUBLIC` category (`FILE_PUBLIC_NOT_ALLOWED`), as before.

Reasons:
- Every avatar in a list and the logo on the login page would otherwise need a token round-trip per image
  per render, the tokens are single-use (no browser caching), and the login page has no caller to bind a
  token to.
- The slug is not guessable and is per tenant; the information (a face, a logo) is the kind users expect
  others in the organisation to see.
- A seeded category would be a tenant-editable row (an administrator could deactivate it or switch
  `ALLOW_PUBLIC` off and break every avatar), would need a seed migration and a new FILE provisioning
  contributor; a lazily created one adds a race and the same editability. No category keeps the
  publication decision in code (the image store), not in tenant data.

## Consequences
- The URL is unauthenticated: anyone holding it sees the image until it is replaced or removed. Removal
  is immediate on the origin; a CDN or browser cache may keep it up to `max-age` (1 day) — the same trade-off
  as every public file (FILE 1.2.0 addendum, S3 operational constraint).
- PNG, JPEG and WebP are served `inline` (step 07's allow-list). **SVG is not** on that list: an SVG logo
  (package E) is served as `attachment` with `X-Content-Type-Options: nosniff` and
  `Content-Security-Policy: sandbox; default-src 'none'`. An `<img src>` renders it regardless of the
  disposition; opening the URL directly downloads it. E keeps it so (open point recorded in the FILE 1.3.0
  addendum, not changed by D).
- Image-store documents appear in FILE's owner list (`GET /api/v1/files?ownerType=SEC_USER&ownerId=…&moduleCode=SEC`)
  with their `publicUrl`, like any document.
- Non-breaking: no schema change; existing category-gated public files behave exactly as before.
