# ADR-FILE-003 — Download token: 10-minute TTL, bound to the issuing user, consumed after open, in-memory or Redis store

Module  : FILE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (SRS) — recorded after the fact (steps 02, 03, 07)
Status  : ACCEPTED (non-breaking)

## Context
RULE-FILE-003 / POLICY-CLI-03 (from ARCH-REF AD-FILE-02) asked for a freshly generated encrypted
token for every upload and download, invalid after its TTL (~100 minutes) and not reusable, with a
payload `{action, ts, ownerId, ownerType, moduleCode, fileName, fileCategory}`. The reference design
assumed a single JVM. erp-core is a library deployed by applications that may run several instances,
and its downloads are initiated by an authenticated staff user through the File Browser.

## Decision
- Downloads only: no token gates an upload (upload = staff JWT + `PERM_FILE_BROWSER_CREATE`).
- Token = AES/GCM (12-byte IV, 128-bit tag, key = SHA-256 of the required
  `erp.core.files.access-token-secret`) over `fileId + expiry + 16 random bytes`, base64url. TTL 10
  minutes (`FileAccessTokenDomainService.TOKEN_TTL`), not ~100: the token is requested by the client
  immediately before the download it serves, and a shorter window bounds the exposure of a leaked
  link.
- The token is bound to the user who requested it: `issueAccessToken` (needs `PERM_FILE_BROWSER_VIEW`)
  stores `sha256(token) → username` in the `DownloadTokenStore` for the TTL; `retrieve`
  (`isAuthenticated()` only) refuses a token of another user with 401 `FILE_ACCESS_TOKEN_INVALID`
  without consuming it, so a leaked token cannot be replayed and the rightful user keeps the grant.
- Single use is enforced by the store (`consume` returns true once). The token is consumed only after
  the content was opened from the provider, so a failed or absent load does not burn it.
- The store is `InMemoryDownloadTokenStore` (per JVM, lazy expiry) when no Redis is configured and
  `RedisDownloadTokenStore` when a Redis template bean exists; a multi-instance deployment must
  provide Redis, otherwise a token issued by one instance is unknown to another.

## Consequences
- The analysed payload fields (owner triple, file name, category, action) are not in the token; the
  file id is enough because the token is tied to a user and a single download.
- A client must issue the token and download within 10 minutes and with the same principal.
- Without Redis the store is not shared: this is documented as a deployment requirement
  (`docs/CONSUMING.md`), not detected at runtime.
- `erp.core.files.access-token-secret` has no default; startup fails without it.

## Traces
RULE-FILE-003 · POLICY-CLI-03 · API-FILE-002 · API-FILE-003 · `FILE_ACCESS_TOKEN_INVALID` ·
module-registry-file.md §AUTO-DECISIONS (AES/GCM token) ·
erp-core/src/main/java/com/erp/file/domain/FileAccessTokenDomainService.java:17-57, 60-112 ·
erp-core/src/main/java/com/erp/file/service/FileService.java:108-111, 166-230, 486-490 ·
erp-core/src/main/java/com/erp/file/service/DownloadTokenStore.java ·
InMemoryDownloadTokenStore.java:10-60 · RedisDownloadTokenStore.java ·
erp-core/src/main/java/com/erp/autoconfigure/DownloadTokenStoreAutoConfiguration.java ·
erp-core/src/main/java/com/erp/autoconfigure/ErpCoreProperties.java:151-154 ·
docs/steps/07-report.md · docs/DEVIATIONS.md [02], [03], [07] (private download entry)
