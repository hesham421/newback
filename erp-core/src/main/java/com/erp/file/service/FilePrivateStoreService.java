package com.erp.file.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.SecurityContextHelper;
import com.erp.events.DomainEventPublisher;
import com.erp.events.FileDocumentPublishedEvent;
import com.erp.file.crossmodule.DownloadGrant;
import com.erp.file.crossmodule.PrivateFileStoreRequest;
import com.erp.file.crossmodule.StoredPrivateFile;
import com.erp.file.domain.FileAccessTokenDomainService;
import com.erp.file.domain.FileDocumentDomain;
import com.erp.file.dto.UploadRequest;
import com.erp.file.entity.FileDocument;
import com.erp.file.exception.FileErrorCodes;
import com.erp.file.mapper.FileMapper;
import com.erp.file.repository.FileDocumentRepository;
import com.erp.file.repository.FileMetadataView;
import com.erp.file.storage.StorageProvider;
import com.erp.file.storage.StorageProviderRegistry;
import com.erp.file.storage.StorageTarget;
import com.erp.file.storage.StoredObject;
import com.erp.tenant.TenantContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * XM-FILE-003 (tenant-maturity C5) — the private store behind {@code FilePrivateStoreApi}: a server-generated file becomes
 * a PRIVATE, uncategorised {@code FILE_DOCUMENT} of the current tenant (RULE-FILE-011), and FILE's single-use download token
 * is issued for it exactly as API-FILE-002 does. Reached only through the cross-module adapter; {@code isAuthenticated()}
 * here, the consuming service owns the permission. No caching (FILE is absent from the approved register).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FilePrivateStoreService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileDocumentRepository repository;
    private final FileMapper mapper;
    private final StorageProviderRegistry storageProviders;
    private final FileAccessTokenDomainService accessTokenService;
    private final DownloadTokenStore tokenStore;
    private final DomainEventPublisher eventPublisher;

    /** RULE-FILE-011 — size and SHA-256 of the local file, then store ACTIVE, PRIVATE, uncategorised in the current tenant. */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<StoredPrivateFile> store(PrivateFileStoreRequest request) {
        requireOwnership(request);
        log.info("Storing a private file for owner {}/{} in module {}",
            request.ownerType(), request.ownerId(), request.moduleCode());

        StorageProvider provider = storageProviders.active();
        String fileName = FileService.safeFileName(request.fileName());
        String contentType = StringUtils.hasText(request.contentType()) ? request.contentType() : DEFAULT_CONTENT_TYPE;
        long size = sizeOf(request.content(), provider);
        String contentHash = sha256Of(request.content(), provider);
        UploadRequest owner = UploadRequest.builder()
            .ownerType(request.ownerType())
            .ownerId(request.ownerId())
            .moduleCode(request.moduleCode())
            .build();
        FileDocument entity = mapper.toEntity(owner, fileName, contentType, size, contentHash,
            FileService.deriveFileType(contentType), FileDocumentDomain.STATUS_ACTIVE, null, provider.key());

        FileDocument saved = repository.save(entity);
        try (InputStream content = new BufferedInputStream(Files.newInputStream(request.content()))) {
            StoredObject stored = provider.put(
                new StorageTarget(TenantContext.require(), StorageTarget.NO_CATEGORY, saved.getId(), fileName),
                content, size, contentType);
            saved.setStorageRef(stored.storageRef());
            FileService.deleteOnRollback(provider, stored.storageRef());
        } catch (IOException e) {
            throw unavailable(provider, e);
        }
        log.info("Stored private file ID: {} ({} bytes, {}) with provider {}", saved.getId(), size, contentType,
            provider.key());
        eventPublisher.publish(new FileDocumentPublishedEvent(saved.getId(), saved.getModuleCode(),
            saved.getOwnerType(), saved.getOwnerId(), saved.getFileName(), saved.getContentType(),
            FileDocumentPublishedEvent.VISIBILITY_PRIVATE));

        return ServiceResult.success(new StoredPrivateFile(saved.getId(), fileName, size, contentHash), Status.CREATED);
    }

    /** RULE-FILE-003 / -011 — API-FILE-002's single-use token for a live document of the current tenant, bound to the caller. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<DownloadGrant> issueDownloadToken(Long documentId) {
        log.info("Issuing a download token for private file ID: {}", documentId);

        FileMetadataView view = repository.findMetadataTupleById(documentId)
            .map(FileMetadataView::from)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, documentId));
        // RULE-FILE-006 — a soft-deleted file is gone: no token (the API-FILE-002 check)
        if (FileDocumentDomain.STATUS_DELETED.equals(view.getFileStatusId())) {
            throw new LocalizedException(Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, documentId);
        }

        String token = accessTokenService.issueToken(documentId);
        Instant expiresAt = Instant.now().plus(FileAccessTokenDomainService.TOKEN_TTL);
        tokenStore.put(FileService.tokenKey(token), SecurityContextHelper.getCurrentUsername(),
            FileAccessTokenDomainService.TOKEN_TTL);

        return ServiceResult.success(new DownloadGrant(token, expiresAt));
    }

    private static long sizeOf(Path content, StorageProvider provider) {
        try {
            return Files.size(content);
        } catch (IOException e) {
            throw unavailable(provider, e);
        }
    }

    private static String sha256Of(Path content, StorageProvider provider) {
        try (InputStream in = new DigestInputStream(new BufferedInputStream(Files.newInputStream(content)),
                MessageDigest.getInstance("SHA-256"))) {
            in.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(((DigestInputStream) in).getMessageDigest().digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw unavailable(provider, e);
        }
    }

    private static LocalizedException unavailable(StorageProvider provider, Exception cause) {
        LocalizedException failure = new LocalizedException(Status.INTERNAL_ERROR,
            FileErrorCodes.FILE_STORAGE_UNAVAILABLE, provider.key());
        failure.initCause(cause);
        return failure;
    }

    /** RULE-FILE-005 — a caller bug, not a user error: every stored file has an owner and a content file. */
    private static void requireOwnership(PrivateFileStoreRequest request) {
        if (request == null || request.ownerId() == null || request.content() == null
            || !StringUtils.hasText(request.ownerType()) || !StringUtils.hasText(request.moduleCode())) {
            throw new LocalizedException(Status.VALIDATION_ERROR, FileErrorCodes.FILE_DOCUMENT_OWNERSHIP_REQUIRED);
        }
    }
}
