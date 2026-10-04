# Step 07 — File storage SPI and public files — report

## Summary

File content can now live in the database (default), on local disk or in S3-compatible storage.
`erp.core.files.storage` selects which. Files can be made `PUBLIC` and served without
authentication at a stable, shareable URL. Private download keeps its single-use token flow.

- **SPI (`com.erp.file.storage`).** `StorageProvider` (`key/put/get/delete/publicUrl`, exactly as
  designed), with `StorageTarget(tenantId, category, documentId, filename)` and
  `StoredObject(storageRef, size)`. Three implementations:
  - `DbStorageProvider` keeps the bytes in `FILE_CONTENT` of the document's own row
    (`storageRef` = row id).
  - `LocalFsStorageProvider` writes `root/tenantId/category/yyyy/MM/documentId_filename`. Every
    segment is sanitised, and every reference must resolve inside the root (path traversal is
    refused). Writes go through a temporary file plus an atomic move.
  - `S3StorageProvider` uses the same key layout. `publicUrl` is `s3.public-base-url/key`.
- **Selection and startup validation.** The new `FileStorageAutoConfiguration` builds
  `StorageProviderRegistry`:
  - DB is always available, LOCAL is available when `local.root` is set, and S3 is available when
    the SDK is present and `s3.bucket` is set.
  - The setting chooses where new uploads go. Reads always use the provider recorded on the
    document.
  - Startup fails, with a message naming the property, when:
    - the value is unknown;
    - LOCAL has no existing, writable root;
    - S3 has no bucket;
    - S3 is selected but the SDK is missing.
  - `software.amazon.awssdk:s3` is an optional dependency. The S3 wiring sits in a nested
    `@ConditionalOnClass(name=...)` configuration.
- **`V12__file_storage.sql`** adds:
  - `STORAGE_PROVIDER`, `STORAGE_REF`, `VISIBILITY`, `PUBLIC_SLUG` and `CONTENT_HASH` on
    `FILE_DOCUMENT`, backfilled to `DB` / `ID::text` / `PRIVATE` / sha256;
  - `FILE_CONTENT` becomes nullable;
  - three CHECK constraints, and the partial unique index `(TENANT_ID, PUBLIC_SLUG) WHERE NOT NULL`;
  - `FILE_CATEGORY.ALLOW_PUBLIC BOOLEAN DEFAULT FALSE`;
  - the permission `FILE:DOCUMENT:PUBLISH` (screen FILE_BROWSER), granted to PLATFORM's
    SYS_ADMIN and FILE_ADMIN. Provisioning copies it to new tenants.
- **`FileService`** routes upload, download and delete through the registry:
  - Upload saves the row, `put`s the content, then records `storageRef`. A LOCAL/S3 object is
    deleted again if the transaction rolls back.
  - Download streams from the recorded provider.
  - Validation, size limits and the soft-delete lifecycle are unchanged.
- **`PATCH /api/v1/files/{id}/visibility`** (`FILE:DOCUMENT:PUBLISH`):
  - `PUBLIC` generates a random 32-character slug. An already public file keeps its slug.
  - `PRIVATE` clears the slug.
  - A category without `ALLOW_PUBLIC` (or no category) → 409 `FILE_PUBLIC_NOT_ALLOWED`, decided
    by `FileDocumentDomain.assertCanBePublic`.
- **`GET /api/v1/public/files/{tenantCode}/{publicSlug}`** (`PublicFileController`):
  - Answers 302 to the provider URL when the provider has one; otherwise streams the content —
    `inline` only for raster images and PDF, `attachment` for everything else (review round 1).
  - Headers: `Cache-Control: max-age=86400, public`, `ETag: "<sha256>"`,
    `X-Content-Type-Options: nosniff`, `Content-Security-Policy: sandbox; default-src 'none'`.
  - Only a PUBLIC, ACTIVE document in a category that allows public files is served; anything
    else is 404.
  - The tenant comes from the path: `TenantResolutionFilter` gained path-based resolution
    (`erp.core.tenant.path-tenant-paths`). The security chain permits `PUBLIC_FILE_PATHS`.
- **Cross-module.**
  - `FileDocumentLookupApi.publicUrl(Long)`.
  - New `com.erp.tenant.crossmodule.TenantLookupApi.codeOf(Long)`, needed to put the tenant code
    into URLs.
- **i18n.** `FILE_PUBLIC_NOT_ALLOWED` and `FILE_STORAGE_UNAVAILABLE`, in EN and AR (own block at the
  end of each bundle).

`mvn -q verify` is green (after review round 1):
- erp-core: 148 tests (111 pre-existing with unchanged assertions, plus 37 new);
- erp-app-reference: 8 tests (7 existing, one of them with an adapted expected list, plus 1 new).

## Files changed

**Created**
- `erp-core/src/main/resources/db/migration/core/V12__file_storage.sql`
- `erp-core/src/main/java/com/erp/file/storage/`:
  - `StorageProvider`, `StorageTarget`, `StoredObject`, `StorageKeys`, `StoragePaths`
  - `DbStorageProvider`, `LocalFsStorageProvider`, `S3StorageProvider`, `StorageProviderRegistry`
- `erp-core/src/main/java/com/erp/autoconfigure/FileStorageAutoConfiguration.java`
- `erp-core/src/main/java/com/erp/file/controller/PublicFileController.java`
- `erp-core/src/main/java/com/erp/file/dto/VisibilityUpdateRequest.java`
- `erp-core/src/main/java/com/erp/file/permission/FilePermissions.java`
- `erp-core/src/main/java/com/erp/file/service/PublicFileUrls.java`
- `erp-core/src/main/java/com/erp/tenant/crossmodule/TenantLookupApi.java`, `TenantLookupApiImpl.java`
- Tests:
  - `erp-core/src/test/java/com/erp/file/AbstractFileStorageIntegrationTest.java`, `DbStorageFileIntegrationTest.java` (7), `LocalStorageFileIntegrationTest.java` (7), `FileHttp.java` (helper)
  - `erp-core/src/test/java/com/erp/file/storage/LocalFsStorageProviderTest.java` (4), `S3StorageProviderTest.java` (3)
  - `erp-core/src/test/java/com/erp/file/domain/FileDocumentDomainVisibilityTest.java` (3)
  - `erp-core/src/test/java/com/erp/autoconfigure/FileStorageAutoConfigurationTest.java` (7)
- `docs/steps/07-report.md`

**Modified**
- `pom.xml`: `aws-sdk.version` 2.55.11 and the managed `software.amazon.awssdk:s3`.
- `erp-core/pom.xml`: optional `s3`, with `netty-nio-client` excluded.
- `autoconfigure/ErpCoreProperties.java`:
  - `files.storage`, `files.public-base-url`, `files.local.root`, `files.s3.*`;
  - `tenant.path-tenant-paths`.
- `autoconfigure/ErpCoreSecurityAutoConfiguration.java`: the `PUBLIC_FILE_PATHS` constant plus one `permitAll` line, and the path-tenant list passed to the filter.
- `tenant/security/TenantResolutionFilter.java`: path-based tenant resolution, with a new 5-argument constructor (the old one is kept).
- `file/entity/FileDocument.java`:
  - five new columns, and `FILE_CONTENT` is nullable;
  - `publish(slug)` / `unpublish()`.
- `file/entity/FileCategory.java`: `allowPublic`.
- `file/domain/FileDocumentDomain.java`: `VISIBILITY_*`, `assertCanBePublic`, `assertNotDeleted`.
- `file/repository/FileDocumentRepository.java`:
  - the metadata select includes the storage and visibility columns;
  - new `findContentTupleById` and `findPublicMetadataTupleBySlug`;
  - `findWithContentById` removed.
- `file/repository/FileMetadataView.java`: five new getters.
- `file/service/FileService.java`: storage SPI, `updateVisibility`, `openPublic`, rollback cleanup, `FileDownload` carries a stream.
- `file/controller/FileController.java`: streaming download, `PATCH /{id}/visibility`.
- `file/crossmodule/FileDocumentLookupApi{,Impl}.java`: `publicUrl`.
- `file/mapper/FileMapper.java`, `file/mapper/FileCategoryMapper.java`.
- `file/dto/FileMetadataResponse.java`, `CategoryCreateRequest.java`, `CategoryUpdateRequest.java`, `CategoryResponse.java`.
- `file/exception/FileErrorCodes.java`: two new codes.
- `resources/i18n/messages.properties`, `messages_ar.properties`.
- `resources/META-INF/spring/...AutoConfiguration.imports`: `FileStorageAutoConfiguration` appended.
- `resources/db/migration/core/README.md`: the V12 row.
- `erp-app-reference/src/test/java/com/erp/app/ReferenceApplicationSmokeTest.java`:
  - Flyway list `2..10, 12, 1000`;
  - new `publicFilePathNeedsNoTokenOrTenantHeader`.
- `docs/DEVIATIONS.md`: 16 `[07]` entries + 5 review-round-1 entries.

**Deleted**: none.

**Untouched**: `erp-core-plan/execution-state.json`, `erp-app-reference/governance/`, `application.yml`.

## Decisions & deviations

These mirror the 16 `[07]` entries in `docs/DEVIATIONS.md`:

1. **The visibility endpoint is `/api/v1/files/{id}/visibility`**, the existing prefix, instead of `/api/v1/file/documents/...`.
2. **Task 6 has no step-06 chain on this branch.**
   - The wiring is isolated in `PUBLIC_FILE_PATHS` (one `permitAll` line) plus `erp.core.tenant.path-tenant-paths` in `TenantResolutionFilter`.
   - It moves into step 06's customer chain on rebase, and that chain must run the tenant filter.
3. **Path tenant rules.**
   - The path tenant wins: unknown → 404, suspended → 403.
   - The authentication of a foreign-tenant token is dropped on that path.
   - The path is not added to the exempt list.
4. **`FilePermissions` holds only `DOCUMENT_PUBLISH`.**
   - The action is seeded in V12 (SYS_ADMIN and FILE_ADMIN of PLATFORM, copied to new tenants).
   - Expect an add/add conflict with step 06.
5. **Extra columns and constraints.**
   - Extra column `CONTENT_HASH`, used as the ETag.
   - CHECK constraints on provider, visibility, and slug ⇔ PUBLIC.
   - The slug uniqueness is a partial unique index, so the unique-constraint count stays 13.
6. **No separate `FileStorageProperties` class.** The settings are nested in `ErpCoreProperties.Files` and validated when the registry bean is built.
7. **Provider selection.** The setting chooses the write target only; reads use the document's own provider. Apps can override providers or the registry.
8. **Content hash and `delete`.** `StoredObject(ref, size)`; the hash is computed in the service. `delete` is used only to clean up after a rollback.
9. **`FILE_STORAGE_UNAVAILABLE` is answered 500**, because `Status` has no 503.
10. **Public GET details.**
    - Served only when PUBLIC, ACTIVE and the category still allows public files.
    - `Content-Disposition` is inline.
    - There is no 304 handling.
    - The slug is stable when a file is published again, and is 32 base64url characters.
11. **`publicUrl` is relative unless `files.public-base-url` is set**, or it is the provider's direct URL. `TenantLookupApi` was added, and `PublicFileUrls` is shared.
12. **The token store needed no change** (Redis-optional since steps 02/03). Download now streams.
13. **The S3 SDK is optional**, with Netty excluded. No class outside the nested configuration links against the SDK.
14. **"Parametrized" means an abstract class with DB and LOCAL subclasses**, one context each. The test tenant admin is `file-admin`.
15. **Smoke test.** The Flyway list now includes `12`, and one test was added.
16. **The manual acceptance ran on the reference jar** against a native PostgreSQL scratch DB.

## Review round 1 fixes

Finding 1 (blocking, anonymous stored XSS via public files) and three notes, fixed in commit
`step(07): review round 1 fixes — safe inline types, CSP/nosniff on public files, GET-only public path`:

- **Safe inline types.** `FileDocumentDomain.INLINE_SAFE_CONTENT_TYPES` (png, jpeg, gif, webp, avif,
  bmp, pdf — no SVG) + `isInlineSafe`. `FileService.openPublic` sets `PublicFile.inline`;
  `PublicFileController` uses `inline` only then, otherwise `attachment`. Unsafe types may still be
  published (chosen over refusing; recorded).
- **Headers.** Every public answer (200 and 302) carries `X-Content-Type-Options: nosniff` and
  `Content-Security-Policy: sandbox; default-src 'none'`.
- **Redirect path.** Direct provider URLs are outside our headers — documented in DEVIATIONS (serve the
  CDN on a separate origin with its own headers).
- **GET/HEAD only.** `ErpCoreSecurityAutoConfiguration` permits only `GET` and `HEAD` on
  `PUBLIC_FILE_PATHS`; other methods → 401 anonymous.
- **No 404 URLs handed out.** `PublicFileUrls.of` now uses `FileDocumentDomain.isPubliclyServable`
  (PUBLIC + slug, ACTIVE, category allows public) — the same conditions as the public lookup. The
  metadata select `LEFT JOIN`s the category (`categoryAllowPublic` on `FileMetadataView` and the entity).
- **S3 risk** (shared key layout, whole-bucket exposure reveals private objects, unpublish does not
  revoke a direct URL) recorded in DEVIATIONS.
- **Tests added** (per provider, DB and LOCAL):
  `publicHtmlAndSvg_areServedAsAttachments_withCspAndNosniff_whilePngStaysInline` (HTML → `text/html`
  attachment, SVG → `application/xml` attachment, both with nosniff + CSP; PNG inline) and
  `publicPath_otherMethodsNeedAuthentication_andPublicUrlFollowsTheCategoryPolicy` (HEAD 200, POST/DELETE
  401; category switched to `allowPublic=false` → public GET 404 and metadata `publicUrl` null); the
  publish test now asserts `inline`, nosniff and CSP; the archived test asserts `publicUrl` null.
  Unit: `isInlineSafe_onlyRasterImagesAndPdf_neverHtmlOrSvg`, `isPubliclyServable_...`.

## Rebase onto 06

`git rebase main` (main = step 06 merged, `3016338`). Conflicts and resolutions:

- `erp-core/src/main/java/com/erp/file/permission/FilePermissions.java` (add/add): took 06's contributor;
  added `DOCUMENT_PUBLISH = "FILE:DOCUMENT:PUBLISH"` and its `PermissionDef` (explicit code, BROWSER
  screen, action PUBLISH) so the synchronizer reproduces the V12 row (matched by code, same names).
- `ReferenceApplicationSmokeTest.java`: Flyway list → `2..12, 1000`.
- `db/migration/core/README.md`, `i18n/messages.properties`, `i18n/messages_ar.properties`,
  `docs/DEVIATIONS.md`: both sides kept, 06 first, then 07.
- `ErpCoreSecurityAutoConfiguration.java` (auto-merged, then reworked): the public file wiring moved
  from the staff chain into 06's `@Order(90)` customer chain — `GET`/`HEAD` `permitAll` on
  `PUBLIC_FILE_PATHS`, and the path in the public lists of that chain's `TenantResolutionFilter`
  (with `path-tenant-paths`) and `RealmEnforcementFilter`; staff chain lines removed. Kept out of
  `customer-public-paths` because that list is permitted for every method.

Follow-ups: `PermissionCatalogIntegrationTest` 40 → 41 core actions (V12's action, verified row for
row); `FileHttp` operator insert gains `REALM = 'STAFF'`; new test
`publicFile_withCustomerTokens_isServedLikeAnonymous_andTheyCannotWriteToThePath` (same- and
foreign-tenant customer tokens get 200 on the public URL, no `REALM_MISMATCH`; customer POST → 405;
customer token on `/api/v1/files/{id}` → 403 `REALM_MISMATCH`). `TenantSchemaIntegrationTest` counts
unchanged (19 / 14 / 19).

```
$ rm -rf target erp-core/target erp-app-reference/target; mvn -q verify     (after rebase onto 06)
EXIT=0 secs=184
com.erp.file.DbStorageFileIntegrationTest        10 0 0 0
com.erp.file.LocalStorageFileIntegrationTest     10 0 0 0
com.erp.sec.PermissionCatalogIntegrationTest      4 0 0 0
com.erp.tenant.TenantSchemaIntegrationTest        4 0 0 0
erp-core tests=177 failures=0 errors=0 skipped=0
erp-app-reference tests=8 failures=0 errors=0 skipped=0
```

## Acceptance checklist

3/3 ✅

- ✅ **Tests are green for both providers, and the app starts with `erp.core.files.storage=LOCAL` and a temp root.**
  - `DbStorageFileIntegrationTest` 9/9 and `LocalStorageFileIntegrationTest` 9/9 (after review round 1), each covering:
    - upload → private download via token;
    - single-use token;
    - publish → anonymous GET with bytes, `Cache-Control` and ETag;
    - unpublish → 404;
    - a category without `ALLOW_PUBLIC`, or no category → 409;
    - tenant B can neither publish A's document nor read it by slug;
    - the failure paths;
    - HTML/SVG served as attachments with nosniff + CSP, PNG inline; GET/HEAD-only path (round 1).
  - The S3 provider has a fake-`S3Client` unit test (3/3).
  - The reference jar started on port 1807 with `--erp.core.files.storage=LOCAL --erp.core.files.local.root=<scratch dir>`: "Started ReferenceApplication in 19.014 seconds"; Flyway applied V2..V10 and V12.
  - With a missing root the startup fails: `erp.core.files.local.root must be an existing, writable directory`.
- ✅ **The public URL works in a plain `curl` without headers.**
  - `curl -i http://localhost:1807/api/v1/public/files/PLATFORM/93h8...` → `200`, `ETag: "7522ea9d…"` (the upload's sha256), `Cache-Control: max-age=86400, public`, `Content-Type: image/png`, and the body equals the upload byte for byte.
  - After `PRIVATE` the same URL → 404 `FILE_DOCUMENT_NOT_FOUND`.
- ✅ **`grep -rn "FILE_CONTENT" erp-core/src/main/java` appears only in `DbStorageProvider` and the entity.** See the output below.

## Verification output

```
$ java -version → openjdk 21.0.7
$ rm -rf target erp-core/target erp-app-reference/target; mvn -q verify     (review round 1)
EXIT=0 secs=158
com.erp.file.DbStorageFileIntegrationTest                          9 0 0 0   (+2 round 1)
com.erp.file.LocalStorageFileIntegrationTest                       9 0 0 0   (+2 round 1)
com.erp.file.domain.FileDocumentDomainVisibilityTest               5 0 0 0   (+2 round 1)
erp-core tests=148 failures=0 errors=0 skipped=0
erp-app-reference tests=8 failures=0 errors=0 skipped=0

--- initial submission (before review round 1):
$ rm -rf target erp-core/target erp-app-reference/target; mvn -q verify
EXIT=0 secs=154
(earlier runs fixed: BootstrapAdminPasswordIntegrationTest counted 'admin' across tenants -> test tenant admin
 renamed 'file-admin'; ReferenceApplicationSmokeTest Flyway list -> added "12")

== erp-core                                                        tests fail err skip
com.erp.architecture.CrossModuleBoundaryArchTest                   2 0 0 0
com.erp.autoconfigure.DownloadTokenStoreAutoConfigurationTest      2 0 0 0
com.erp.autoconfigure.ErpCoreAutoConfigurationTest                 9 0 0 0
com.erp.autoconfigure.ErpCoreFlywayAutoConfigurationTest           3 0 0 0
com.erp.autoconfigure.FileStorageAutoConfigurationTest             7 0 0 0   (new)
com.erp.autoconfigure.MigrationNamingTest                          2 0 0 0
com.erp.file.DbStorageFileIntegrationTest                          7 0 0 0   (new)
com.erp.file.LocalStorageFileIntegrationTest                       7 0 0 0   (new)
com.erp.file.domain.FileDocumentDomainVisibilityTest               3 0 0 0   (new)
com.erp.file.service.InMemoryDownloadTokenStoreTest                3 0 0 0
com.erp.file.storage.LocalFsStorageProviderTest                    4 0 0 0   (new)
com.erp.file.storage.S3StorageProviderTest                         3 0 0 0   (new)
com.erp.notif.service.DefaultChannelProviderTest                   2 0 0 0
com.erp.sec.* (9 classes)                                         47 0 0 0
com.erp.tenant.* (6 classes)                                      39 0 0 0
com.erp.testsupport.TestProfileWiringIntegrationTest               2 0 0 0
erp-core tests=142 failures=0 errors=0 skipped=0
== erp-app-reference
com.erp.app.ReferenceApplicationSmokeTest                          8 0 0 0   (+1)
erp-app-reference tests=8 failures=0 errors=0 skipped=0

$ grep -rn "FILE_CONTENT" erp-core/src/main/java
erp-core/src/main/java/com/erp/file/entity/FileDocument.java:87:    // No @Lob: FILE_CONTENT is a plain BYTEA column ...
erp-core/src/main/java/com/erp/file/entity/FileDocument.java:92:    @Column(name = "FILE_CONTENT")
erp-core/src/main/java/com/erp/file/storage/DbStorageProvider.java:15: * ... {@code FILE_DOCUMENT.FILE_CONTENT} (BYTEA) ...
erp-core/src/main/java/com/erp/file/storage/DbStorageProvider.java:20: * know the {@code FILE_CONTENT} column ...

Manual check — reference jar (no AWS SDK in it), scratch DB erp_s07_app, port 1807, LOCAL root = scratch dir:
upload: {"id":1,"storageProvider":"LOCAL","visibility":"PRIVATE","contentType":"image/png"}
files on disk under the LOCAL root: ./1/product_photo/2026/10/1_logo.png
publish: {"visibility":"PUBLIC","publicUrl":"/api/v1/public/files/PLATFORM/93h8JVpM_A0FLPhgwHV27QBpaNd8kZhZ"}
$ curl -i http://localhost:1807/api/v1/public/files/PLATFORM/93h8JVpM_A0FLPhgwHV27QBpaNd8kZhZ   (no headers, no token)
HTTP/1.1 200
ETag: "7522ea9dcfbbf64f697772b0de031b15e509188b207c95e6e310e3fac52f4d52"
Cache-Control: max-age=86400, public
Content-Disposition: inline; filename="=?UTF-8?Q?logo.png?="; filename*=UTF-8''logo.png
Content-Type: image/png
Content-Length: 36
sha256 of upload: 7522ea9dcfbbf64f697772b0de031b15e509188b207c95e6e310e3fac52f4d52
(re-published) curl → HTTP 200 image/png 36 bytes; public body equals upload: yes
after PRIVATE: curl -i <same url> → HTTP/1.1 404 {"error":{"code":"FILE_DOCUMENT_NOT_FOUND",...}}
private download via token: HTTP 200 image/png; private body equals upload: yes
unknown tenant: curl /api/v1/public/files/NOPE/x → HTTP 404 {"error":{"code":"TENANT_NOT_FOUND",...}}
DB row: 1|LOCAL|1/product_photo/2026/10/1_logo.png|PUBLIC|file_content null=t|7522ea9d...
startup with --erp.core.files.local.root=<missing dir> → exit=1,
  "erp.core.files.local.root must be an existing, writable directory: ...\no-such-dir"
$ dropdb erp_s07_app; select count(*) from pg_database where datname like 'erp_s07%' → 0
(first manual attempt sent an Arabic nameAr through Git Bash curl and got a non-201; re-run with ASCII names)
```

## Skills checked

- **`build-create-entity`: compliant.**
  - New columns follow the README §4 naming. `ALLOW_PUBLIC` is a native BOOLEAN with no converter (A.1.6) and has `@Builder.Default`.
  - Normalization happens only in `@PrePersist`.
  - `publish`/`unpublish` are pure mutations, and `FileDocumentDomain` decides (A.0.x).
  - `FileDocumentDomain` stays plain: no Spring/JPA annotations, built through `from()`, throws `LocalizedException`.
- **`build-create-repository`: compliant.** The new queries are JPQL tuple projections, each with a caller (A.2.9). The removed `findWithContentById` had no caller left.
- **`build-create-dto`: compliant.** `VisibilityUpdateRequest` has `@Schema` in both languages and i18n validation keys (`{validation.required}`, `{validation.pattern}`). The new fields carry `@Schema` with examples.
- **`build-create-mapper`: compliant.** Mapping is manual and null-safe, uses `Boolean.TRUE.equals`, and the mapper does no lookups (the service resolves `publicUrl`).
- **`build-create-service`: compliant.**
  - Every new public method has `@PreAuthorize`: `FilePermissions.DOCUMENT_PUBLISH`, or `permitAll()` for the deliberately public read.
  - `@Transactional` / `readOnly` as appropriate.
  - `log.info` for writes, `log.debug` for reads.
  - Decisions go to `FileDocumentDomain`.
  - Other modules are reached only through `TenantLookupApi` (tenant `crossmodule`) and `TenantContext` (tenant root package).
  - Named deviation: `openPublic` returns a `PublicFile` record instead of a `ServiceResult`, because it is a binary or redirect payload, the same precedent as the existing `retrieve`/`FileDownload`.
- **`build-create-controller`: compliant, with one named nuance.**
  - Thin, with `@Operation`, `@Valid @RequestBody` and bilingual `@Tag`.
  - `PublicFileController` maps the service result onto HTTP (302 vs. 200, cache headers). That is HTTP presentation, not business logic (A.6.12).
- **`gov-enforce-error-handling`: compliant.**
  - Every throw is a `LocalizedException` with a code registered in `FileErrorCodes`.
  - Both new codes exist in EN and AR, with `''{0}''` where an argument is passed.
  - 409 `CONFLICT` for "category disallows public", 404 `NOT_FOUND` for missing, deleted or foreign-tenant documents.
  - Raw I/O and SDK exceptions are caught in the providers and mapped to `FILE_STORAGE_UNAVAILABLE`.
  - Startup failures are `IllegalStateException`. They are configuration errors at boot, not request errors, the same precedent as step 03's `@NotBlank` property messages.
- **`gov-enforce-caching-rules`: compliant.** No `@Cacheable`/`@CacheEvict` was added. HTTP `Cache-Control` on the public responses is not the Spring cache abstraction.
- **`gov-enforce-backend-contract` (FILE storage/visibility changes): no new violations.**
  - Remaining fails are pre-existing FILE shapes: no Update/Usage DTO for documents, and `softDelete` via DELETE with an action.
  - CU.1–CU.8 pass.
- **`gov-validate-backend-feature`: approved with notes.**
  - Stage 4 compiles cleanly.
  - Cross-module: only the tenant `crossmodule` and root package are used, and ArchUnit is green, including the SpEL rule (`T(com.erp.file.permission.FilePermissions)` is same-module).
  - Notes: `openPublic` returns a binary/redirect payload instead of `ServiceResult`, and the permission is a single PUBLISH action, as the step prescribes.

## Notes for later steps

- **Rebase onto step 06 — DONE** (see "Rebase onto 06"; the list below was the plan).
  1. Move `auth.requestMatchers(PUBLIC_FILE_PATHS).permitAll()` from `erpCoreSecurityFilterChain` into 06's `@Order(90)` customer/public chain. That chain's matcher `/api/v1/public/**` already covers the path.
  2. Make sure 06's chain runs `TenantResolutionFilter` built with `properties.getTenant().getPathTenantPaths()` (5-argument constructor). Without it a public file request has no tenant and fails with `TENANT_CONTEXT_MISSING`.
  3. `FilePermissions`: add/add conflict. Keep 06's class, add `DOCUMENT_PUBLISH = "FILE:DOCUMENT:PUBLISH"`, and contribute it. V12 already seeds and grants it; never edit V12.
  4. Smoke test Flyway list → `..., "10", "11", "12", "1000"`.
  5. `ErpCoreProperties` and `.imports` are appended blocks only.
- **Storage.**
  - Inject `StorageProviderRegistry` (FILE-internal) to read or write content.
  - Other modules use `FileDocumentLookupApi.isAvailable/publicUrl`.
  - New keys need a migration that widens `CHK_FILE_DOCUMENT_STORAGE_PROVIDER`.
  - LOCAL content is not deleted on soft delete (RULE-FILE-006). There is no purge job yet.
- **Storefront.** Create category `PRODUCT_IMAGE` with `allowPublic=true`, upload into it, then `PATCH /api/v1/files/{id}/visibility {"visibility":"PUBLIC"}`. Use `data.publicUrl`, or `FileDocumentLookupApi.publicUrl(id)` from another module.
- **Public file safety.** Public content is inline only for raster images/PDF and always carries
  nosniff + `sandbox` CSP; keep that when step 06's chain takes over the path (the headers are set by
  `PublicFileController`, not by the chain). A CDN origin used via `s3.public-base-url` must be a
  separate domain with its own headers.
- **Path-tenant mechanism.** It is generic. Any future public URL that carries `{tenantCode}` can be added to `erp.core.tenant.path-tenant-paths`, and the property list must keep the public files default.
- **S3 behind a CDN.** Objects are written without an ACL, and keys contain the document id and file name. If `s3.public-base-url` exposes the whole bucket, private objects are reachable by key too. Restrict public exposure to the bucket/CDN side, or add a public prefix later.
- **Tests.** FILE tests run before `com.erp.sec`. Never provision test tenants with admin username `admin`, because some SEC tests count `admin` rows across tenants.
