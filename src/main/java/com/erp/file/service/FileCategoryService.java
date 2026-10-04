package com.erp.file.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.file.domain.FileCategoryDomain;
import com.erp.file.dto.CategoryCreateRequest;
import com.erp.file.dto.CategoryResponse;
import com.erp.file.dto.CategorySearchRequest;
import com.erp.file.dto.CategoryUpdateRequest;
import com.erp.file.entity.FileCategory;
import com.erp.file.exception.FileErrorCodes;
import com.erp.file.mapper.FileCategoryMapper;
import com.erp.file.repository.FileCategoryRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration for ENTITY-FILE-002 (FileCategory) — API-FILE-007 (Categories CRUD), SCR-FILE-001.
 * Addresses the resource by surrogate id; DELETE is a soft deactivate (204). RULE-FILE-007 (unique
 * categoryCode) delegates to FileCategoryDomain — never inlined here.
 *
 * <p>No caching annotations — FILE is absent from the caching approved-register, so per
 * gov-enforce-caching-rules this service carries zero {@code @Cacheable}/{@code @CacheEvict}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileCategoryService {

    private final FileCategoryRepository repository;
    private final FileCategoryMapper mapper;

    /**
     * Entity property names (isActive, NOT the DTO's isActiveFl) — the combined filter + sort
     * whitelist consumed by both SpecBuilder and PageableBuilder. Must include every field the
     * search contract documents as filterable/sortable (categoryCode/nameAr/nameEn LIKE, isActive
     * EXACT); a field omitted here is silently dropped from filtering AND sorting.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "categoryCode", "nameAr", "nameEn", "isActive", "createdAt"
    );

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FILE_CATEGORIES_CREATE)")
    public ServiceResult<CategoryResponse> create(CategoryCreateRequest request) {
        log.info("Creating FileCategory with code: {}", request.getCategoryCode());

        // RULE-FILE-007 pre-check against the normalized (uppercase) code — FileCategory.onCreate()
        // uppercases categoryCode before insert, so the probe must match the stored form.
        boolean codeTaken = repository.existsByCategoryCode(normalize(request.getCategoryCode()));

        // Delegate the decision (RULE-FILE-007 uniqueness) to the Domain
        FileCategoryDomain.create(request.getCategoryCode(), codeTaken);

        FileCategory saved;
        try {
            saved = repository.saveAndFlush(mapper.toEntity(request));
        } catch (DataIntegrityViolationException e) {
            // TOCTOU: a concurrent create won the race between the existsBy probe and the insert —
            // the DB unique constraint is the real guard; surface it as the duplicate business error.
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FileErrorCodes.FILE_CATEGORY_CODE_DUPLICATE, request.getCategoryCode());
        }
        log.info("Created FileCategory ID: {}, code: {}", saved.getId(), saved.getCategoryCode());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FILE_CATEGORIES_VIEW)")
    public ServiceResult<Page<CategoryResponse>> search(CategorySearchRequest searchRequest) {
        log.debug("Searching FileCategory");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();

        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<FileCategory> spec =
            SpecBuilder.build(commonRequest, allowedFields, DefaultFieldValueConverter.INSTANCE);
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<FileCategory> page = repository.findAll(spec, pageable);

        return ServiceResult.success(page.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FILE_CATEGORIES_VIEW)")
    public ServiceResult<CategoryResponse> getById(Long id) {
        log.debug("Fetching FileCategory ID: {}", id);

        FileCategory entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_CATEGORY_NOT_FOUND, id));

        return ServiceResult.success(mapper.toResponse(entity));
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FILE_CATEGORIES_UPDATE)")
    public ServiceResult<CategoryResponse> update(Long id, CategoryUpdateRequest request) {
        log.info("Updating FileCategory ID: {}", id);

        FileCategory entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_CATEGORY_NOT_FOUND, id));

        // categoryCode immutability (RULE-FILE-007) needs no guard — it is absent from the request.
        mapper.updateEntityFromRequest(entity, request);

        FileCategory saved = repository.save(entity);
        log.info("Updated FileCategory ID: {}", saved.getId());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * API-FILE-007 DELETE — soft deactivate (isActiveFl = false). A category is a config record with
     * no active-child guard, so it simply flips the flag. Returns void so the controller responds 204.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FILE_CATEGORIES_DELETE)")
    public void deactivate(Long id) {
        log.info("Deactivating FileCategory ID: {}", id);

        FileCategory entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_CATEGORY_NOT_FOUND, id));

        entity.deactivate();
        repository.save(entity);
        log.info("Deactivated FileCategory ID: {}", id);
    }

    /**
     * Normalizes a caller-supplied categoryCode to the canonical uppercase form
     * FileCategory.onCreate() always applies, so the uniqueness check matches the stored value.
     */
    private static String normalize(String categoryCode) {
        return categoryCode == null ? null : categoryCode.trim().toUpperCase();
    }
}
