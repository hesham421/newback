package com.erp.file.service;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.util.SecurityContextHelper;
import com.erp.common.util.TokenHasher;
import com.erp.events.DomainEventPublisher;
import com.erp.events.FileDocumentPublishedEvent;
import com.erp.file.domain.FileAccessTokenDomainService;
import com.erp.file.domain.FileDocumentDomain;
import com.erp.file.domain.FileValidationDomainService;
import com.erp.file.dto.AccessTokenResponse;
import com.erp.file.dto.FileMetadataResponse;
import com.erp.file.dto.UploadRequest;
import com.erp.file.dto.VisibilityUpdateRequest;
import com.erp.file.entity.FileCategory;
import com.erp.file.entity.FileDocument;
import com.erp.file.exception.FileErrorCodes;
import com.erp.file.mapper.FileMapper;
import com.erp.file.repository.FileCategoryRepository;
import com.erp.file.repository.FileDocumentRepository;
import com.erp.file.repository.FileMetadataView;
import com.erp.file.storage.StorageProvider;
import com.erp.file.storage.StorageProviderRegistry;
import com.erp.file.storage.StorageTarget;
import com.erp.file.storage.StoredObject;
import com.erp.tenant.TenantContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestration for ENTITY-FILE-001 (FileDocument) — API-FILE-001..006, SCR-FILE-002 File Browser.
 * Module-internal: another module reaches FILE over REST, or validates a stored fileId through
 * {@code com.erp.file.crossmodule.FileDocumentLookupApi} — never by injecting this service. Business
 * decisions are delegated: size/type to {@link FileValidationDomainService} (RULE-FILE-001/002),
 * token crypto to {@link FileAccessTokenDomainService} (RULE-FILE-003), the lifecycle state machine
 * to {@link FileDocumentDomain} (RULE-FILE-006). Metadata reads use the bytes-excluded projection
 * (DRV-003). Single-use of the download token is enforced here via a {@link DownloadTokenStore}
 * entry consumed on download (Redis when configured, otherwise in-memory).
 *
 * <p>erp-core step 07: content goes through the {@link StorageProviderRegistry} — uploads to the
 * provider selected by {@code erp.core.files.storage} (DB / LOCAL / S3), downloads from the provider
 * recorded on the document. A PUBLIC document (category with {@code ALLOW_PUBLIC}) is served without
 * authentication by {@link #openPublic} at the URL built by {@link PublicFileUrls}.
 *
 * <p>No caching annotations — FILE is absent from the caching approved-register, so per
 * gov-enforce-caching-rules this service carries zero {@code @Cacheable}/{@code @CacheEvict}. The
 * {@link DownloadTokenStore} below is a single-use token store, not the Spring cache abstraction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileDocumentRepository repository;
    private final FileCategoryRepository categoryRepository;
    private final FileMapper mapper;
    private final FileAccessTokenDomainService accessTokenService;
    private final DownloadTokenStore tokenStore;
    private final StorageProviderRegistry storageProviders;
    private final PublicFileUrls publicFileUrls;
    /** Upload size limits ({@code erp.core.files.max-content-bytes} / {@code max-request-bytes}). */
    private final ErpCoreProperties erpCoreProperties;
    private final DomainEventPublisher eventPublisher;

    /** Owner-list sort whitelist (entity property names) per API-FILE-005. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("fileName", "createdAt", "fileSize");

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String TOKEN_KEY_PREFIX = "file:dl-token:";

    /** Download payload handed to the controller for streaming — carries the content stream, not JSON. */
    public record FileDownload(InputStream content, Long size, String contentType, String fileName) {
    }

    /**
     * Public file payload (erp-core step 07): either a {@code redirectUrl} (the provider serves the
     * content directly) or the content stream with its metadata; {@code etag} is the quoted content hash;
     * {@code inline} is true only for the safe content types of
     * {@link FileDocumentDomain#INLINE_SAFE_CONTENT_TYPES} (everything else is an attachment).
     */
    public record PublicFile(String redirectUrl, InputStream content, Long size, String contentType,
                             String fileName, String etag, boolean inline) {
    }

    /** API-FILE-001 — upload: validate ownership → resolve limits → detect+enforce type → enforce size → store ACTIVE. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.file.permission.FilePermissions)"
        + ".PERM_FILE_BROWSER_CREATE)")
    public ServiceResult<FileMetadataResponse> store(UploadRequest request, MultipartFile file) {
        log.info("Storing file for owner {}/{} in module {}",
            request != null ? request.getOwnerType() : null,
            request != null ? request.getOwnerId() : null,
            request != null ? request.getModuleCode() : null);

        // RULE-FILE-005 — ownership is mandatory (guards the in-process provider path too, which
        // bypasses controller bean-validation). Structural null/blank check, not a business rule.
        requireOwnership(request);

        byte[] content = readBytes(file);
        long size = content.length;
        String originalName = file.getOriginalFilename();

        // DRV-004 — per-category effective limits (null when no category supplied).
        FileCategory category = resolveCategory(request.getFileCategoryFk());
        Long categoryMaxSize = category != null ? category.getMaxSizeBytes() : null;
        String categoryAllowedTypes = category != null ? category.getAllowedContentTypes() : null;

        // RULE-FILE-002 — the allow-list decision uses ONLY the content-sniffed MIME (magic bytes);
        // the client filename is never trusted for it. Name-based detection supplies the stored
        // display type when the category imposes no restriction.
        String verifiedType = sniffContentType(content);
        String contentType = verifiedType != null ? verifiedType : fallbackContentType(originalName);
        FileValidationDomainService.assertContentTypeAllowed(verifiedType, contentType, categoryAllowedTypes);

        // RULE-FILE-001 — content size (category override) then whole-request ceiling.
        FileValidationDomainService.assertContentSizeAllowed(size, categoryMaxSize,
            erpCoreProperties.getFiles().getMaxContentBytes());
        FileValidationDomainService.assertRequestSizeAllowed(size,
            erpCoreProperties.getFiles().getMaxRequestBytes());

        StorageProvider provider = storageProviders.active();
        String fileName = safeFileName(originalName);
        FileDocument entity = mapper.toEntity(request, fileName, contentType, size, sha256Hex(content),
            deriveFileType(contentType), FileDocumentDomain.STATUS_ACTIVE, category, provider.key());

        // The id is assigned on persist (sequence), so the provider can name the object after it; the
        // row itself is written at flush, together with the storage reference.
        FileDocument saved = repository.save(entity);
        StoredObject stored = provider.put(
            new StorageTarget(TenantContext.require(),
                category != null ? category.getCategoryCode() : StorageTarget.NO_CATEGORY, saved.getId(), fileName),
            new ByteArrayInputStream(content), size, contentType);
        saved.setStorageRef(stored.storageRef());
        deleteOnRollback(provider, stored.storageRef());
        log.info("Stored file ID: {} ({} bytes, {}) with provider {}", saved.getId(), size, contentType, provider.key());
        eventPublisher.publish(new FileDocumentPublishedEvent(saved.getId(), saved.getModuleCode(),
            saved.getOwnerType(), saved.getOwnerId(), saved.getFileName(), saved.getContentType(),
            FileDocumentPublishedEvent.VISIBILITY_PRIVATE));

        return ServiceResult.success(mapper.toMetadataResponse(saved, null), Status.CREATED);
    }

    /** API-FILE-002 — issue a fresh single-use download token; store its nonce in the token store for the TTL. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.file.permission.FilePermissions)"
        + ".PERM_FILE_BROWSER_VIEW)")
    public ServiceResult<AccessTokenResponse> issueAccessToken(Long id) {
        log.info("Issuing access token for file ID: {}", id);

        FileMetadataView view = repository.findMetadataTupleById(id)
            .map(FileMetadataView::from)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, id));

        // RULE-FILE-006 — a soft-deleted file is treated as gone: no download token may be minted.
        if (FileDocumentDomain.STATUS_DELETED.equals(view.getFileStatusId())) {
            throw new LocalizedException(Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, id);
        }

        String token = accessTokenService.issueToken(id);
        Instant expiresAt = Instant.now().plus(FileAccessTokenDomainService.TOKEN_TTL);
        // Bind the single-use token to the issuing user so a leaked/shared token cannot be replayed
        // by a different authenticated caller (retrieve verifies this identity before serving bytes).
        tokenStore.put(tokenKey(token),
            SecurityContextHelper.getCurrentUsername(), FileAccessTokenDomainService.TOKEN_TTL);

        return ServiceResult.success(
            AccessTokenResponse.builder().accessToken(token).expiresAt(expiresAt).build());
    }

    /** API-FILE-003 — validate & atomically consume the token (single-use), then load bytes. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public FileDownload retrieve(String token) {
        log.debug("Retrieving file by access token");

        long fileId = accessTokenService.validateAndExtractFileId(token);
        String key = tokenKey(token);

        // Verify the token belongs to the current caller BEFORE consuming it: a mismatched (leaked or
        // shared) token is rejected without burning the grant, so the rightful owner can still use it.
        String boundUser = tokenStore.get(key);
        if (boundUser == null || !boundUser.equals(SecurityContextHelper.getCurrentUsername())) {
            throw new LocalizedException(Status.UNAUTHORIZED, FileErrorCodes.FILE_ACCESS_TOKEN_INVALID);
        }

        // Open the content BEFORE consuming the token so a failed/absent load does not burn the
        // single-use grant — the caller can retry with the same token until a download succeeds.
        FileMetadataView view = repository.findMetadataTupleById(fileId)
            .map(FileMetadataView::from)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, fileId));

        // RULE-FILE-006 — a soft-deleted file is treated as gone and is never downloadable.
        if (FileDocumentDomain.STATUS_DELETED.equals(view.getFileStatusId())) {
            throw new LocalizedException(Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, fileId);
        }

        InputStream content = storageProviders.forKey(view.getStorageProvider()).get(view.getStorageRef());

        // Single-use: delete returns true only for the first consumer; a gone/expired key ⇒ ERR-0003.
        if (!tokenStore.consume(key)) {
            closeQuietly(content);
            throw new LocalizedException(Status.UNAUTHORIZED, FileErrorCodes.FILE_ACCESS_TOKEN_INVALID);
        }

        return new FileDownload(content, view.getFileSize(), view.getContentType(), view.getFileName());
    }

    /** API-FILE-004 — metadata by id (bytes excluded, DRV-003). */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.file.permission.FilePermissions)"
        + ".PERM_FILE_BROWSER_VIEW)")
    public ServiceResult<FileMetadataResponse> getMetadata(Long id) {
        log.debug("Fetching file metadata ID: {}", id);

        FileMetadataView view = repository.findMetadataTupleById(id)
            .map(FileMetadataView::from)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, id));

        return ServiceResult.success(mapper.toMetadataResponse(view, publicFileUrls.of(view).orElse(null)));
    }

    /** API-FILE-005 — owner list (bytes excluded, DRV-003). Empty result is a 200 empty page. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.file.permission.FilePermissions)"
        + ".PERM_FILE_BROWSER_VIEW)")
    public ServiceResult<Page<FileMetadataResponse>> listByOwner(Long ownerId, String ownerType,
            String moduleCode, String fileTypeId, String fileStatusId, int page, int size, String sort) {
        log.debug("Listing files for owner {}/{} in module {}", ownerType, ownerId, moduleCode);

        // Route through the shared builder so page/size are clamped (page>=0, 1<=size<=200) — a raw
        // PageRequest.of with an out-of-range size/page would throw and surface as a 500.
        String sortField = (sort != null && ALLOWED_SORT_FIELDS.contains(sort)) ? sort : "createdAt";
        SearchRequest pageRequest = SearchRequest.builder()
            .page(page)
            .size(size)
            .sortField(sortField)
            .sortDirection(Sort.Direction.DESC)
            .build();
        Pageable pageable = PageableBuilder.from(pageRequest, ALLOWED_SORT_FIELDS);

        Page<FileMetadataView> result = repository.findMetadataTupleByOwner(
            ownerId, ownerType, moduleCode, fileTypeId, fileStatusId, pageable)
            .map(FileMetadataView::from);

        return ServiceResult.success(result.map(view ->
            mapper.toMetadataResponse(view, publicFileUrls.of(view).orElse(null))));
    }

    /**
     * API-FILE-006 — archive (ARCHIVE→ARCHIVED, UPDATE perm) or soft-delete (DELETE→DELETED, DELETE
     * perm). Bytes are retained (RULE-FILE-006). The lifecycle transition is guarded by
     * FileDocumentDomain; no physical removal. The gate picks the permission by the action
     * argument; any other action value passes it so resolveTargetStatus still answers 400, never 403.
     */
    @Transactional
    @PreAuthorize("(#action == 'ARCHIVE' and hasAuthority(T(com.erp.file.permission.FilePermissions)"
        + ".PERM_FILE_BROWSER_UPDATE))"
        + " or (#action == 'DELETE' and hasAuthority(T(com.erp.file.permission.FilePermissions)"
        + ".PERM_FILE_BROWSER_DELETE))"
        + " or (#action != 'ARCHIVE' and #action != 'DELETE')")
    public ServiceResult<FileMetadataResponse> softDelete(Long id, String action) {
        log.info("Soft-deleting file ID: {} with action {}", id, action);

        String targetStatus = resolveTargetStatus(action);

        FileDocument entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, id));

        // RULE-FILE-006 — the state machine decides whether the transition is legal.
        FileDocumentDomain.from(entity).assertCanTransitionTo(targetStatus);
        entity.setFileStatusId(targetStatus);

        FileDocument saved = repository.save(entity);
        log.info("File ID: {} status set to {}", saved.getId(), targetStatus);

        return ServiceResult.success(
            mapper.toMetadataResponse(saved, publicFileUrls.of(saved).orElse(null)), Status.UPDATED);
    }

    /**
     * erp-core step 07 — {@code PATCH /api/v1/files/{id}/visibility}: PUBLIC publishes the document
     * under a fresh random slug (an already public document keeps its slug, so its URL stays stable);
     * PRIVATE withdraws it and drops the slug. Only a category with {@code ALLOW_PUBLIC} may hold
     * PUBLIC files (409 {@code FILE_PUBLIC_NOT_ALLOWED}, decided by FileDocumentDomain); a soft-deleted
     * or foreign-tenant document is 404.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.file.permission.FilePermissions).DOCUMENT_PUBLISH)")
    public ServiceResult<FileMetadataResponse> updateVisibility(Long id, VisibilityUpdateRequest request) {
        log.info("Setting visibility of file ID: {} to {}", id, request.getVisibility());

        FileDocument entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, id));

        FileDocumentDomain domain = FileDocumentDomain.from(entity);
        domain.assertNotDeleted(id);

        if (FileDocumentDomain.VISIBILITY_PUBLIC.equals(request.getVisibility())) {
            FileCategory category = entity.getFileCategoryFk();
            domain.assertCanBePublic(category != null && Boolean.TRUE.equals(category.getAllowPublic()));
            if (entity.getPublicSlug() == null) {
                entity.publish(PublicFileUrls.newSlug());
            }
        } else {
            entity.unpublish();
        }

        FileDocument saved = repository.save(entity);
        log.info("File ID: {} visibility is now {}", saved.getId(), saved.getVisibility());

        return ServiceResult.success(
            mapper.toMetadataResponse(saved, publicFileUrls.of(saved).orElse(null)), Status.UPDATED);
    }

    /**
     * erp-core step 07 — {@code GET /api/v1/public/files/{tenantCode}/{publicSlug}}, unauthenticated.
     * The tenant was set from the path by the tenant filter, so the lookup only sees that tenant's
     * documents: a PUBLIC, ACTIVE document in a category allowing public files, else 404. When the
     * provider can serve the content itself the caller is redirected there.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PublicFile openPublic(String publicSlug) {
        log.debug("Serving public file {}", publicSlug);

        FileMetadataView view = repository.findPublicMetadataTupleBySlug(publicSlug,
                FileDocumentDomain.VISIBILITY_PUBLIC, FileDocumentDomain.STATUS_ACTIVE)
            .map(FileMetadataView::from)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, publicSlug));

        StorageProvider provider = storageProviders.forKey(view.getStorageProvider());
        String etag = view.getContentHash() != null ? "\"" + view.getContentHash() + "\"" : null;
        Optional<String> direct = provider.publicUrl(view.getStorageRef());
        if (direct.isPresent()) {
            return new PublicFile(direct.get(), null, null, null, null, etag, false);
        }
        return new PublicFile(null, provider.get(view.getStorageRef()), view.getFileSize(),
            view.getContentType(), view.getFileName(), etag, FileDocumentDomain.isInlineSafe(view.getContentType()));
    }

    /**
     * A LOCAL/S3 object written for a transaction that then rolls back would be orphaned (the row that
     * names it never commits), so it is removed again. DB content rolls back with the row.
     */
    private static void deleteOnRollback(StorageProvider provider, String storageRef) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    provider.delete(storageRef);
                }
            }
        });
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            log.debug("Closing a download stream failed", e);
        }
    }

    private static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new LocalizedException(Status.INTERNAL_ERROR, CommonErrorCodes.INTERNAL_ERROR);
        }
    }

    private FileCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        FileCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_CATEGORY_NOT_FOUND, categoryId));
        // A deactivated category is retired — it must not gate new uploads.
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FileErrorCodes.FILE_CATEGORY_INACTIVE, categoryId);
        }
        return category;
    }

    private static void requireOwnership(UploadRequest request) {
        if (request == null || request.getOwnerId() == null
                || isBlank(request.getOwnerType()) || isBlank(request.getModuleCode())) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                FileErrorCodes.FILE_DOCUMENT_OWNERSHIP_REQUIRED);
        }
    }

    private static String resolveTargetStatus(String action) {
        // Only "ARCHIVE"/"DELETE" are reachable — the softDelete @PreAuthorize admits no other action.
        if ("ARCHIVE".equals(action)) {
            return FileDocumentDomain.STATUS_ARCHIVED;
        }
        if ("DELETE".equals(action)) {
            return FileDocumentDomain.STATUS_DELETED;
        }
        throw new LocalizedException(Status.VALIDATION_ERROR,
            FileErrorCodes.FILE_DOCUMENT_INVALID_TRANSITION, "ACTIVE", action);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            // Infrastructure failure reading the multipart part — surface as a structured 500.
            throw new LocalizedException(Status.INTERNAL_ERROR, CommonErrorCodes.INTERNAL_ERROR);
        }
    }

    /** Content-based (magic-byte) MIME detection only — never the client filename. Null if undetectable. */
    private static String sniffContentType(byte[] content) {
        try {
            return URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(content));
        } catch (IOException e) {
            return null;
        }
    }

    /** Best-effort display type when content sniffing fails — used only where no allow-list applies. */
    private static String fallbackContentType(String fileName) {
        String byName = fileName != null ? URLConnection.guessContentTypeFromName(fileName) : null;
        return byName != null ? byName : DEFAULT_CONTENT_TYPE;
    }

    /** Derives the LOV-FILE-001 bucket from the detected MIME. Codes are owned by FileLookupService. */
    private static String deriveFileType(String mime) {
        String m = mime.toLowerCase();
        if (m.startsWith("image/")) {
            return FileLookupService.TYPE_IMAGE;
        }
        if (m.contains("spreadsheet") || m.contains("excel") || m.contains("csv")) {
            return FileLookupService.TYPE_SPREADSHEET;
        }
        if (m.contains("pdf") || m.contains("msword") || m.contains("wordprocessing") || m.startsWith("text/")) {
            return FileLookupService.TYPE_DOCUMENT;
        }
        if (m.contains("zip") || m.contains("x-tar") || m.contains("x-7z") || m.contains("x-rar")) {
            return FileLookupService.TYPE_ARCHIVE;
        }
        return FileLookupService.TYPE_OTHER;
    }

    private static String safeFileName(String originalName) {
        return (originalName != null && !originalName.isBlank()) ? originalName : "file";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String tokenKey(String token) {
        // Reuse the canonical at-rest token hasher (SHA-256 hex) so the stored key form can never
        // drift from the rest of the platform.
        return TOKEN_KEY_PREFIX + TokenHasher.sha256Hex(token);
    }
}
