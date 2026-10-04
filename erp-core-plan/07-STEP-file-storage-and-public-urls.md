# Step 07 — File storage SPI and public files

**Branch:** `step/07-file-storage`
**Goal:** file content can live in the database (default), on local disk, or in S3-compatible storage, selected by configuration; files can be marked `PUBLIC` and served without authentication (product images); private download keeps the single-use token flow with a Redis-optional token store.
**Why:** `FILE_DOCUMENT.FILE_CONTENT` is a `bytea`; downloads require an authenticated user to mint a Redis-bound token. A storefront needs stable public URLs, and a SaaS with many tenants should not keep images in PostgreSQL.

## Preconditions
- Step 05 merged (tenant). Step 02's `DownloadTokenStore` exists.

## Design (fixed decisions)
- `StorageProvider` SPI (`com.erp.file.storage`):
  ```java
  public interface StorageProvider {
      String key();                                   // "DB" | "LOCAL" | "S3"
      StoredObject put(StorageTarget target, InputStream in, long size, String contentType);
      InputStream get(String storageRef);
      void delete(String storageRef);
      Optional<String> publicUrl(String storageRef); // empty if provider cannot serve directly
  }
  ```
  `StorageTarget` = (tenantId, category, documentId, filename). `storageRef` is provider-specific (DB: the row id; LOCAL: relative path; S3: object key).
- Selection: `erp.core.files.storage=DB|LOCAL|S3` (default `DB`). `LOCAL` needs `erp.core.files.local.root`; `S3` needs `erp.core.files.s3.{bucket,region,endpoint?,access-key?,secret-key?,public-base-url?}` and the optional dependency `software.amazon.awssdk:s3` (`<optional>true</optional>`; provider is `@ConditionalOnClass(S3Client.class)`).
- `FILE_DOCUMENT` gets `STORAGE_PROVIDER VARCHAR(8)`, `STORAGE_REF VARCHAR(512)`, `VISIBILITY VARCHAR(8)` ∈ {`PRIVATE`,`PUBLIC`} (default PRIVATE), `PUBLIC_SLUG VARCHAR(64)` unique per tenant (random, non-guessable). `FILE_CONTENT` becomes nullable (used only by the DB provider).
- Public serving: `GET /api/v1/public/files/{tenantCode}/{publicSlug}` → if provider has a `publicUrl`, `302` redirect to it; else stream the content. Tenant resolved from the path (not header) so URLs are shareable. Cache headers: `Cache-Control: public, max-age=86400`, ETag = content hash.
- Private download: unchanged semantics (mint token → download) with `DownloadTokenStore` (in-memory default, Redis when present).
- Category policy: `FILE_CATEGORY` gets `ALLOW_PUBLIC BOOLEAN DEFAULT FALSE`; only categories allowing public may hold `PUBLIC` files (a storefront app will create category `PRODUCT_IMAGE` with `ALLOW_PUBLIC=true`).

## Tasks
1. **Migration `V12__file_storage.sql`**: columns above; backfill `STORAGE_PROVIDER='DB'`, `STORAGE_REF=ID::text`, `VISIBILITY='PRIVATE'`; unique `(TENANT_ID, PUBLIC_SLUG)` where not null; `FILE_CATEGORY.ALLOW_PUBLIC`.
2. Implement `DbStorageProvider` (wraps the existing bytea column), `LocalFsStorageProvider` (root/tenantId/category/yyyy/MM/documentId_filename; path traversal guarded), `S3StorageProvider` (optional).
3. `FileStorageProperties` under `ErpCoreProperties.files` (step 03) with validation at startup (`LOCAL` root must exist and be writable; `S3` bucket required).
4. Refactor `FileService` upload/download/delete to go through the selected `StorageProvider`; keep the existing validation (`FileValidationDomainService`), size limits and soft-delete lifecycle.
5. Add `PATCH /api/v1/file/documents/{id}/visibility` (STAFF, permission `FILE:DOCUMENT:PUBLISH` contributed by `FilePermissions`): sets `PUBLIC` (generates slug) or `PRIVATE` (clears slug); rejects if category disallows public.
6. `PublicFileController` as designed; add its path to the customer/public chain's permitAll list (step 06 chain) and to the tenant resolver's path-based resolution.
7. Cross-module API: extend `FileDocumentLookupApi` with `Optional<String> publicUrl(Long documentId)` (returns the `/api/v1/public/files/...` URL or the provider's direct URL).
8. **i18n**: `FILE_PUBLIC_NOT_ALLOWED`, `FILE_STORAGE_UNAVAILABLE`.
9. **Tests**: parametrized over `DB` and `LOCAL` providers (temp dir): upload → private download via token; publish → public GET without auth returns bytes + cache headers; unpublish → public GET 404; category without `ALLOW_PUBLIC` → 409; tenant B cannot publish or read tenant A's document; S3 provider: unit test with a fake `S3Client` only (no network).

## Acceptance
- Tests green for both providers; app starts with `erp.core.files.storage=LOCAL` and a temp root.
- Public URL works in a plain `curl` without headers.
- `grep -rn "FILE_CONTENT" erp-core/src/main/java` appears only in `DbStorageProvider` and the entity.

## Commit
`step(07): StorageProvider SPI (DB/LOCAL/S3), public files with slugs, Redis-optional download tokens`

## Out of scope
Image resizing/thumbnails, CDN signing, virus scanning.
