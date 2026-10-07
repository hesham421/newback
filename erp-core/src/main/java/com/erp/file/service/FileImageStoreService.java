package com.erp.file.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.TokenHasher;
import com.erp.events.DomainEventPublisher;
import com.erp.events.FileDocumentPublishedEvent;
import com.erp.file.crossmodule.ImageStoreRequest;
import com.erp.file.crossmodule.ImageStoreResult;
import com.erp.file.crossmodule.StoredImage;
import com.erp.file.domain.FileDocumentDomain;
import com.erp.file.domain.ImageValidationDomainService;
import com.erp.file.dto.UploadRequest;
import com.erp.file.entity.FileDocument;
import com.erp.file.exception.FileErrorCodes;
import com.erp.file.mapper.FileMapper;
import com.erp.file.repository.FileDocumentRepository;
import com.erp.file.storage.StorageProvider;
import com.erp.file.storage.StorageProviderRegistry;
import com.erp.file.storage.StorageTarget;
import com.erp.file.storage.StoredObject;
import com.erp.tenant.TenantContext;
import java.io.ByteArrayInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * XM-FILE-002 (tenant-maturity D.4) — the image store behind {@code FileImageStoreApi}: validates
 * (RULE-FILE-008/009, {@link ImageValidationDomainService}), stores through the active storage provider
 * and publishes at once with a random slug, uncategorised (RULE-FILE-010, ADR-FILE-001). Reached only
 * through the cross-module adapter; {@code isAuthenticated()} here, the consuming service owns the
 * permission. No caching (FILE is absent from the approved register).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileImageStoreService {

    private static final String DEFAULT_FILE_NAME = "image";

    private final FileDocumentRepository repository;
    private final FileMapper mapper;
    private final StorageProviderRegistry storageProviders;
    private final PublicFileUrls publicFileUrls;
    private final DomainEventPublisher eventPublisher;

    /** Validate → store ACTIVE, IMAGE, uncategorised → publish with a fresh slug; a rejection writes nothing. */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<ImageStoreResult> store(ImageStoreRequest request) {
        requireOwnership(request);
        log.info("Storing a public image for owner {}/{} in module {}",
            request.ownerType(), request.ownerId(), request.moduleCode());

        byte[] content = request.content();
        ImageValidationDomainService.Verdict verdict =
            ImageValidationDomainService.check(content, request.maxBytes(), request.allowedTypes());
        if (!verdict.accepted()) {
            log.info("Public image rejected: {}", verdict.rejection());
            return ServiceResult.success(ImageStoreResult.rejected(verdict.rejection()));
        }

        StorageProvider provider = storageProviders.active();
        String fileName = StringUtils.hasText(request.fileName()) ? request.fileName() : DEFAULT_FILE_NAME;
        UploadRequest owner = UploadRequest.builder()
            .ownerType(request.ownerType())
            .ownerId(request.ownerId())
            .moduleCode(request.moduleCode())
            .build();
        FileDocument entity = mapper.toEntity(owner, fileName, verdict.contentType(), content.length,
            TokenHasher.sha256Hex(content), FileLookupService.TYPE_IMAGE, FileDocumentDomain.STATUS_ACTIVE,
            null, provider.key());
        entity.publish(PublicFileUrls.newSlug());

        FileDocument saved = repository.save(entity);
        StoredObject stored = provider.put(
            new StorageTarget(TenantContext.require(), StorageTarget.NO_CATEGORY, saved.getId(), fileName),
            new ByteArrayInputStream(content), content.length, verdict.contentType());
        saved.setStorageRef(stored.storageRef());
        FileService.deleteOnRollback(provider, stored.storageRef());
        log.info("Stored public image ID: {} ({} bytes, {}) with provider {}",
            saved.getId(), content.length, verdict.contentType(), provider.key());
        eventPublisher.publish(new FileDocumentPublishedEvent(saved.getId(), saved.getModuleCode(),
            saved.getOwnerType(), saved.getOwnerId(), saved.getFileName(), saved.getContentType(),
            FileDocumentPublishedEvent.VISIBILITY_PUBLIC));

        return ServiceResult.success(ImageStoreResult.stored(new StoredImage(saved.getId(),
            publicFileUrls.of(saved).orElse(null), verdict.contentType(), content.length)));
    }

    /** RULE-FILE-010 — DELETED and PRIVATE (slug dropped); bytes retained (RULE-FILE-006); idempotent. */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<Void> discard(Long documentId) {
        if (documentId == null) {
            return ServiceResult.success(null);
        }
        repository.findById(documentId)
            .filter(document -> !FileDocumentDomain.STATUS_DELETED.equals(document.getFileStatusId()))
            .ifPresent(document -> {
                FileDocumentDomain.from(document).assertCanTransitionTo(FileDocumentDomain.STATUS_DELETED);
                document.unpublish();
                document.setFileStatusId(FileDocumentDomain.STATUS_DELETED);
                repository.save(document);
                log.info("Discarded public image ID: {}", documentId);
            });
        return ServiceResult.success(null);
    }

    /** RULE-FILE-005 — a caller bug, not a user error: every image has an owner. */
    private static void requireOwnership(ImageStoreRequest request) {
        if (request == null || request.ownerId() == null
            || !StringUtils.hasText(request.ownerType()) || !StringUtils.hasText(request.moduleCode())) {
            throw new LocalizedException(Status.VALIDATION_ERROR, FileErrorCodes.FILE_DOCUMENT_OWNERSHIP_REQUIRED);
        }
    }
}
