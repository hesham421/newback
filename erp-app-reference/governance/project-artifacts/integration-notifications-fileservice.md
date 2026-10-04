# Integration Guide: Notifications & File Service

How another module (in this deployable) or another service (over HTTP) integrates with NOTIF and
FILE. Verified against the code on 2026-10-01. Citations name classes, not line numbers (line
numbers drift). Section numbers are stable — other documents cite them.

Deeper per-module references (Arabic): [platform-integration-notif.md](platform-integration-notif.md),
[platform-integration-file.md](platform-integration-file.md); index and new-module checklist:
[PLATFORM-MODULES-INTEGRATION-INDEX.md](PLATFORM-MODULES-INTEGRATION-INDEX.md).

Base URL `http://<host>:7272` (`server.port`), no context path. Every JSON response uses the shared
envelope `com.erp.common.web.ApiResponse`:

```json
{ "success": true, "data": { ... }, "timestamp": "2026-10-01T12:00:00Z" }
{ "success": false, "error": { "code": "...", "message": "...", "fieldErrors": [ ... ] }, "timestamp": "..." }
```

---

## 1. Notifications Module (NOTIF)

### 1.1 How it is called

| Style | Entry point | When to use |
|---|---|---|
| In-process (preferred) | `com.erp.notif.crossmodule.NotificationDispatchApi` — `dispatch(DispatchCommand)` / `dispatchIndependently(DispatchCommand)` | Java code in this deployable |
| REST | `POST /api/v1/notifications/dispatch` (`DispatchController`) | A separate process, or the frontend |

- NOTIF listens to **no** application events. The sending module calls `NotificationDispatchApi`
  explicitly; the reference caller is `com.erp.sec.service.PasswordResetService`.
- `dispatch` uses REQUIRED propagation (joins the caller's transaction; a dispatch failure marks it
  rollback-only). `dispatchIndependently` resolves the recipient in the caller's transaction, then
  sends in a REQUIRES_NEW transaction — use it by default, inside a `try/catch`, so a notification
  problem never fails the business operation.
- Dispatch is synchronous; there is no queue or broker.
- Other crossmodule surfaces: `NotificationLogQueryApi.findByRecipientModuleAndReference(...)`
  (read back dispatch results) and `NotificationChannelAdminApi.setChannelEnabled(...)`.

### 1.2 Request / input

`DispatchRequest` (REST, Bean-Validated) and `DispatchCommand` (in-process record, not validated —
the caller supplies valid values) carry the same fields:

| Field | Type | Required | Notes |
|---|---|---|---|
| `recipientId` | Long | yes | SEC `userPk`. Must be an existing, ACTIVE user — otherwise nothing is sent (§1.3). |
| `templateCode` | String (≤80) | yes | Normalized to upper case. Unknown → 404. |
| `channelHint` | List\<String\> (each ≤20) | yes, non-empty | One `NOTIF_LOG` row per entry. |
| `moduleCode` | String (≤50) | yes | The sending module's code, e.g. `"SEC"`. |
| `referenceId` | Long | no | Source record id. |
| `referenceType` | String (≤100) | no | Source record type. |
| `variables` | Map\<String,String\> | no | Values for `{name}` placeholders (single braces). |

Variables the EMAIL channel reads: `email` (destination — **required for EMAIL**; NOTIF does not look
it up, get it from `SecUserDirectoryApi.findContact`), `lang` (`AR` selects the Arabic subject/body,
anything else English), `actionLink`, `ctaLabelAr` / `ctaLabelEn` (call-to-action button).

Channel codes (`MDL` lookup `NOTIF_CHANNEL`): `EMAIL`, `SMS`, `WHATSAPP`, `PUSH`, `INTERNAL`. Only
`EMAIL` has a channel config and a real provider (SMTP via `JavaMailSender`).

```json
{
  "recipientId": 42,
  "templateCode": "PASSWORD_RESET",
  "channelHint": ["EMAIL"],
  "moduleCode": "SEC",
  "referenceId": 42,
  "referenceType": "SEC_PWD_RESET_TOKEN",
  "variables": { "email": "user@example.com", "lang": "EN" }
}
```

### 1.3 Response / output

`DispatchResponse` → `{ "logIds": [1001, 1002] }`, HTTP **200**.

- Inactive or unknown recipient (RULE-NOTIF-007, checked through SEC's `SecUserDirectoryApi`):
  `logIds` is `[]`, no rows are written, no error.
- Per channel, the final `NOTIF_LOG.NOTIFICATION_STATUS_ID` is `SENT`, `FAILED` (after up to 5
  immediate attempts, with `errorMessage`) or `CHANNEL_DISABLED` (no enabled config). None of these is
  an HTTP error — read the log (`NotificationLogQueryApi`, or `POST /api/v1/notifications/logs/search`)
  to know whether delivery happened.

### 1.4 Authentication / authorization

- REST: `Authorization: Bearer <JWT>` (validated by `com.erp.sec.security.JwtAuthenticationFilter`).
- Both dispatch entry points are gated `@PreAuthorize("isAuthenticated()")` — no page permission.
- An in-process caller without an authenticated principal (an anonymous flow) must run the call under
  `com.erp.sec.security.InternalCallerContext.call(...)`, as `PasswordResetService` does. That class
  is not in `sec.crossmodule`, so a module other than SEC cannot import it without breaking
  `CrossModuleBoundaryArchTest` (open item, see the index).
- Template/channel/log administration needs `PERM_NOTIF_TEMPLATES_*`, `PERM_NOTIF_CHANNELS_*`,
  `PERM_NOTIF_LOG_VIEW`.

### 1.5 Errors

| Code | HTTP | When |
|---|---|---|
| `NOTIF_TEMPLATE_NOT_FOUND` | 404 | Unknown `templateCode` |
| `NOTIF_TEMPLATE_INACTIVE` | 422 | Template deactivated |
| `NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND` | 404 | Template create/update with an `attachmentFileId` that is not an available FILE document |
| `VALIDATION_ERROR` | 400 | Missing/invalid request fields (`GlobalExceptionHandler`) |
| `SEC-401-INVALID-CREDENTIALS` | 401 | Missing/invalid/expired token (security filter chain) |
| `ACCESS_DENIED` | 403 | A `@PreAuthorize` gate refused |
| `INTERNAL_ERROR` | 500 | Unexpected exception |

### 1.6 Minimal "how to call it" snippet

```java
// In a service of the consuming module — inject com.erp.notif.crossmodule.NotificationDispatchApi
try {
    notificationDispatchApi.dispatchIndependently(new DispatchCommand(
        userPk, "XYZ_ORDER_APPROVED", List.of("EMAIL"), "XYZ", orderId, "XYZ_ORDER",
        Map.of("email", email, "lang", "AR", "orderNo", orderNo)));
} catch (RuntimeException e) {
    log.warn("Notification dispatch failed for order {}", orderId, e);
}
```

```bash
curl -X POST http://localhost:7272/api/v1/notifications/dispatch \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"recipientId":42,"templateCode":"PASSWORD_RESET","channelHint":["EMAIL"],"moduleCode":"SEC",
       "variables":{"email":"user@example.com"}}'
```

Templates are seeded per module with a forward Flyway migration (pattern:
`V11__notif_email_channel_seed.sql`; copy changes as new `UPDATE` migrations, as V12/V32 do).

---

## 2. File Service Module (FILE)

### 2.1 How it is called

| Style | Entry point | When to use |
|---|---|---|
| REST | `FileController` at `/api/v1/files` (+ `FileCategoryController`, `FileLookupController`) | Upload, list, download, archive — normally from the frontend |
| In-process | `com.erp.file.crossmodule.FileDocumentLookupApi.isAvailable(Long fileId)` | A module validating a `fileId` it stores as a soft reference |

| Operation | Endpoint | Method |
|---|---|---|
| Upload | `/api/v1/files` (multipart) | `POST` |
| Issue download token | `/api/v1/files/{id}/access-token` | `POST` |
| Download | `/api/v1/files/download?token=...` | `GET` |
| Get metadata | `/api/v1/files/{id}` | `GET` |
| List by owner | `/api/v1/files?ownerId=&ownerType=&moduleCode=` | `GET` |
| Archive / soft-delete | `/api/v1/files/{id}?action=ARCHIVE\|DELETE` | `DELETE` |
| Categories | `/api/v1/files/categories` (`POST`, `POST /search`, `GET/PUT/DELETE /{id}`) | — |
| Lookups | `/api/v1/files/lookups/{FILE_FILE_TYPE\|FILE_FILE_STATUS}` | `GET` |

Never inject `FileService` from another module: it is module-internal (ArchUnit) and its
`@PreAuthorize` gates still apply to the caller.

### 2.2 Request / input

Upload (`multipart/form-data`): `file` + `UploadRequest`:

| Field | Type | Required | Notes |
|---|---|---|---|
| `ownerId` | Long | yes | Id of the owning business record |
| `ownerType` | String (≤100) | yes | Constant name of the owning entity type, e.g. `XYZ_ORDER` |
| `moduleCode` | String (≤50) | yes | The owning module's code |
| `fileCategoryFk` | Long | no | Numeric id of a `FILE_CATEGORY` (limits profile) |

Ownership is polymorphic with no FK; FILE does not check that the owner exists.

Validation: content type is detected from magic bytes (no extension rule); a category's
`allowedContentTypes` (CSV of MIME types), when set, must contain it → else 415. Size limit is the
category's `maxSizeBytes` when > 0, else 5 MB, and never above 10 MB → else 413.

### 2.3 Response / output

`FileMetadataResponse`: `id, ownerId, ownerType, moduleCode, fileName, contentType, fileSize,
fileTypeId, fileStatusId, fileCategoryId, createdAt, createdBy, updatedAt, updatedBy` — never the
bytes. Upload returns 201; the owner list returns `Page<FileMetadataResponse>` (sort: `fileName`,
`createdAt`, `fileSize`, always DESC).

Download is a raw body (not enveloped) with the stored `Content-Type` and
`Content-Disposition: attachment`. The access token (`{accessToken, expiresAt}`) is AES-GCM, valid
10 minutes, single-use, bound to the requesting username via Redis. ARCHIVED files remain
downloadable; DELETED files return 404. Bytes live in `FILE_DOCUMENT.FILE_CONTENT` (`BYTEA`).

### 2.4 Authentication / authorization

All endpoints need a JWT; there is no public download path, so the frontend fetches with the
`Authorization` header and saves a blob.

| Operation | Gate |
|---|---|
| Upload | `PERM_FILE_BROWSER_CREATE` |
| Token, metadata, list | `PERM_FILE_BROWSER_VIEW` |
| Download | `isAuthenticated()` (+ token bound to the same user) |
| Archive / delete | `PERM_FILE_BROWSER_UPDATE` / `PERM_FILE_BROWSER_DELETE` |
| Categories | `PERM_FILE_CATEGORIES_*` |
| Lookups | `isAuthenticated()` |

A consuming module's roles need these grants (pattern: `V31__cu_notif_file_security_seed.sql`).

### 2.5 Errors

| Code | HTTP | When |
|---|---|---|
| `FILE_DOCUMENT_SIZE_EXCEEDED` | 413 | Over the size limit |
| `FILE_DOCUMENT_TYPE_NOT_ALLOWED` | 415 | Detected type not in the category allow-list |
| `FILE_ACCESS_TOKEN_INVALID` | 401 | Token tampered, expired, other user, or already used |
| `FILE_DOCUMENT_NOT_FOUND` / `FILE_CATEGORY_NOT_FOUND` | 404 | Unknown id (or DELETED file) |
| `FILE_CATEGORY_INACTIVE` | 422 | Upload references a deactivated category |
| `FILE_CATEGORY_CODE_DUPLICATE` | 409 | Duplicate category code |
| `FILE_DOCUMENT_INVALID_TRANSITION` | 422 / 400 | Illegal lifecycle transition / unknown `action` |
| `FILE_LOOKUP_KEY_UNKNOWN` | 404 | Unknown lookup key |
| `VALIDATION_ERROR` | 400 | Missing ownership fields or other invalid input |

### 2.6 Minimal "how to call it" snippet

```bash
# upload
curl -X POST http://localhost:7272/api/v1/files -H "Authorization: Bearer <token>" \
  -F "file=@invoice.png" -F "ownerId=1001" -F "ownerType=XYZ_ORDER" -F "moduleCode=XYZ"
# download: issue a token, then fetch with the same JWT
curl -X POST http://localhost:7272/api/v1/files/55/access-token -H "Authorization: Bearer <token>"
# -> { "data": { "accessToken": "...", "expiresAt": "..." } }
curl -H "Authorization: Bearer <token>" -o invoice.png \
  "http://localhost:7272/api/v1/files/download?token=<accessToken>"
```

```java
// validating a stored soft reference from another module
if (attachmentFileId != null && !fileDocumentLookupApi.isAvailable(attachmentFileId)) {
    throw new LocalizedException(Status.NOT_FOUND, XyzErrorCodes.XYZ_404_ATTACHMENT, attachmentFileId);
}
```
