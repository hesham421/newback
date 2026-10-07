<!-- Source: PHASE:SVC-API / SUB:SVC-API-FILES -->
<!-- Context: see SVC-API-HEADER.md for phase-level strategy, registry table, and intro -->

<!-- API:API-FILE-001:START -->
### API-FILE-001 — Upload file
POST /api/v1/files (multipart) | FileController.upload → FileService.store
REQUEST UploadRequest{file(multipart), ownerId, ownerType, moduleCode, fileCategoryFk?} | RESPONSE 201 FileMetadataResponse (no bytes)
VALIDATIONS: RULE-FILE-005 (ownerId, ownerType, moduleCode required — Message-AR: بيانات الملكية إلزامية.);
             RULE-FILE-001 (size ≤ limit; category override — Message-AR: حجم الملف يتجاوز المسموح.);
             RULE-FILE-002 (auto-detect MIME; restrict to allowed types — Message-AR: نوع الملف غير مسموح.)
ERRORS: ERR-0004 → RULE-FILE-005 → 400; ERR-0001 → RULE-FILE-001 size → 413; ERR-0002 → RULE-FILE-002 type → 415
ORCHESTRATION: validate ownership (RULE-FILE-005) → resolve category limits (DRV-004) → auto-detect MIME & enforce type (RULE-FILE-002) → enforce size (RULE-FILE-001) → store bytes (QR-FILE-0001) with fileStatusId=ACTIVE.
REPO: QR-FILE-0001 SAVE — READ_WRITE — Sequence SEQ_FILE_DOCUMENT | SECURITY: Security filter (RULE-FILE-004); upload contextual to owner module.
<!-- API:API-FILE-001:END -->
<!-- API:API-FILE-002:START -->
### API-FILE-002 — Issue access token
POST /api/v1/files/{id}/access-token | FileController.issueToken → FileAccessTokenDomainService.issue
REQUEST path {id} | RESPONSE 200 {accessToken, expiresAt}
VALIDATIONS: RULE-FILE-003 (fresh AES/GCM single-use token, ~100m TTL); RULE-FILE-004 (owner visibility)
ERRORS: ERR-0006 → NOT_FOUND → 404
REPO: QR-FILE-0002 FIND_ONE — READ_ONLY | SECURITY: Security filter + ownership (RULE-FILE-005).
<!-- API:API-FILE-002:END -->
<!-- API:API-FILE-003:START -->
### API-FILE-003 — Download file
GET /api/v1/files/download?token= | FileController.download → FileService.retrieve
REQUEST query token | RESPONSE 200 binary stream (Content-Type from stored contentType)
VALIDATIONS: RULE-FILE-003 (token valid, unexpired, single-use); RULE-FILE-004 (delegate auth)
ERRORS: ERR-0003 → RULE-FILE-003 invalid/expired token → 401
ORCHESTRATION: validate & consume token (RULE-FILE-003) → stream FILE_CONTENT (QR-FILE-0003, @Lob stream).
REPO: QR-FILE-0003 FIND_ONE (bytes) — READ_ONLY | SECURITY: token-gated (separate from JWT).
<!-- API:API-FILE-003:END -->
<!-- API:API-FILE-004:START -->
### API-FILE-004 — File metadata
GET /api/v1/files/{id} | FileController.metadata → FileService.getMetadata
RESPONSE 200 FileMetadataResponse (no bytes)
VALIDATIONS: RULE-FILE-004 (delegate auth); ownership (RULE-FILE-005)
ERRORS: ERR-0006 → NOT_FOUND → 404
REPO: QR-FILE-0004 FIND_ONE (metadata projection, no bytes) — READ_ONLY | SECURITY: Security filter.
<!-- API:API-FILE-004:END -->
<!-- API:API-FILE-005:START -->
### API-FILE-005 — List files by owner
GET /api/v1/files?ownerId=&ownerType=&moduleCode= | FileController.listByOwner → FileService.listByOwner
REQUEST params ownerId,ownerType,moduleCode (+ fileTypeId?, fileStatusId?, page,size); ALLOWED_SORT_FIELDS={fileName,createdAt,fileSize}
RESPONSE 200 Page<FileMetadataResponse> (empty → 200 [], never 404)
VALIDATIONS: RULE-FILE-004 (delegate auth) | ERRORS: none (empty → 200)
REPO: QR-FILE-0005 FIND_BY_CRITERIA (metadata only, bytes excluded — DRV-003) — READ_ONLY — Join NONE | SECURITY: Security filter.
<!-- API:API-FILE-005:END -->
<!-- API:API-FILE-006:START -->
### API-FILE-006 — Archive / soft-delete file
DELETE /api/v1/files/{id} | FileController.archiveOrDelete → FileService.softDelete
REQUEST path {id} (+ action=ARCHIVE|DELETE) | RESPONSE 200/204
VALIDATIONS: RULE-FILE-006 (soft delete → fileStatusId=DELETED; ARCHIVE → ARCHIVED; bytes retained — Message-AR: حذف منطقي دون إزالة فيزيائية.)
ERRORS: ERR-0006 → NOT_FOUND → 404
ORCHESTRATION: load (QR-FILE-0004) → set fileStatusId (QR-FILE-0006). No physical byte removal.
REPO: QR-FILE-0006 UPDATE — READ_WRITE | SECURITY: SCR-FILE-002 UPDATE (archive) / DELETE.
<!-- API:API-FILE-006:END -->
