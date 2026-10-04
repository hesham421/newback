package com.erp.notif.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.file.crossmodule.FileDocumentLookupApi;
import com.erp.notif.domain.NotificationTemplateDomain;
import com.erp.notif.dto.TemplateCreateRequest;
import com.erp.notif.dto.TemplateResponse;
import com.erp.notif.dto.TemplateSearchRequest;
import com.erp.notif.dto.TemplateUpdateRequest;
import com.erp.notif.entity.NotificationTemplate;
import com.erp.notif.exception.NotifErrorCodes;
import com.erp.notif.mapper.NotificationTemplateMapper;
import com.erp.notif.repository.NotificationTemplateRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration for ENTITY-NOTIF-002 (NotificationTemplate) — API-NOTIF-004 (Templates CRUD),
 * SCR-NOTIF-001. Addresses the resource by surrogate id; DELETE is a soft deactivate (204). Business
 * rules (RULE-NOTIF-004 bilingual, RULE-NOTIF-006 unique templateCode) delegate to
 * NotificationTemplateDomain.
 *
 * <p>No caching annotations — NOTIF is absent from the caching approved-register, so per
 * gov-enforce-caching-rules this service carries zero {@code @Cacheable}/{@code @CacheEvict}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;
    private final NotificationTemplateMapper mapper;
    private final FileDocumentLookupApi fileDocumentLookupApi;

    /** Entity property names (isActive, NOT the DTO's isActiveFl) — filter + sort whitelist. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "templateCode", "nameAr", "nameEn", "isActive", "createdAt"
    );

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.notif.permission.NotifPermissions)"
        + ".PERM_NOTIF_TEMPLATES_CREATE)")
    public ServiceResult<TemplateResponse> create(TemplateCreateRequest request) {
        log.info("Creating NotificationTemplate with code: {}", request.getTemplateCode());

        // RULE-NOTIF-006 pre-check against the normalized (uppercase) code — NotificationTemplate
        // .onCreate() uppercases templateCode before insert, so the probe must match the stored form.
        boolean codeTaken = repository.existsByTemplateCode(normalize(request.getTemplateCode()));

        // Delegate the decision (RULE-NOTIF-004 bilingual + RULE-NOTIF-006 uniqueness) to the Domain
        NotificationTemplateDomain.create(request.getTemplateCode(), request.getNameAr(),
            request.getNameEn(), request.getBodyAr(), request.getBodyEn(), codeTaken);

        assertAttachmentAvailable(request.getAttachmentFileId());

        NotificationTemplate saved = repository.save(mapper.toEntity(request));
        log.info("Created NotificationTemplate ID: {}, code: {}", saved.getId(), saved.getTemplateCode());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.notif.permission.NotifPermissions)"
        + ".PERM_NOTIF_TEMPLATES_VIEW)")
    public ServiceResult<Page<TemplateResponse>> search(TemplateSearchRequest searchRequest) {
        log.debug("Searching NotificationTemplate");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();

        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<NotificationTemplate> spec =
            SpecBuilder.build(commonRequest, allowedFields, DefaultFieldValueConverter.INSTANCE);
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<NotificationTemplate> page = repository.findAll(spec, pageable);

        return ServiceResult.success(page.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.notif.permission.NotifPermissions)"
        + ".PERM_NOTIF_TEMPLATES_VIEW)")
    public ServiceResult<TemplateResponse> getById(Long id) {
        log.debug("Fetching NotificationTemplate ID: {}", id);

        NotificationTemplate entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_TEMPLATE_NOT_FOUND, id));

        return ServiceResult.success(mapper.toResponse(entity));
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.notif.permission.NotifPermissions)"
        + ".PERM_NOTIF_TEMPLATES_UPDATE)")
    public ServiceResult<TemplateResponse> update(Long id, TemplateUpdateRequest request) {
        log.info("Updating NotificationTemplate ID: {}", id);

        NotificationTemplate entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_TEMPLATE_NOT_FOUND, id));

        // templateCode immutability (RULE-NOTIF-006) needs no guard — it is absent from the request.
        // RULE-NOTIF-004 (bilingual body) re-validated on update.
        NotificationTemplateDomain.from(entity).assertBilingualBody(request.getBodyAr(), request.getBodyEn());
        assertAttachmentAvailable(request.getAttachmentFileId());

        mapper.updateEntityFromRequest(entity, request);
        NotificationTemplate saved = repository.save(entity);
        log.info("Updated NotificationTemplate ID: {}", saved.getId());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * API-NOTIF-004 DELETE — soft deactivate (isActiveFl = false). A template is a config record with
     * no active-child guard, so it simply flips the flag. Returns void so the controller responds 204.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.notif.permission.NotifPermissions)"
        + ".PERM_NOTIF_TEMPLATES_DELETE)")
    public void deactivate(Long id) {
        log.info("Deactivating NotificationTemplate ID: {}", id);

        NotificationTemplate entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_TEMPLATE_NOT_FOUND, id));

        entity.deactivate();
        repository.save(entity);
        log.info("Deactivated NotificationTemplate ID: {}", id);
    }

    /**
     * Normalizes a caller-supplied templateCode to the canonical uppercase form
     * NotificationTemplate.onCreate() always applies, so the uniqueness check matches the stored value.
     */
    /** XM-NOTIF-002 — an optional attachmentFileId must name an available FILE document. */
    private void assertAttachmentAvailable(Long attachmentFileId) {
        if (attachmentFileId != null && !fileDocumentLookupApi.isAvailable(attachmentFileId)) {
            throw new LocalizedException(Status.NOT_FOUND,
                NotifErrorCodes.NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND, attachmentFileId);
        }
    }

    private static String normalize(String templateCode) {
        return templateCode == null ? null : templateCode.trim().toUpperCase();
    }
}
