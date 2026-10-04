package com.erp.tenant.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.common.util.SecurityContextHelper;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantProvisioning;
import com.erp.tenant.TenantProvisioningContributor;
import com.erp.tenant.domain.TenantDomain;
import com.erp.tenant.dto.TenantCreateRequest;
import com.erp.tenant.dto.TenantResponse;
import com.erp.tenant.dto.TenantSearchRequest;
import com.erp.tenant.dto.TenantStatusUpdateRequest;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import com.erp.tenant.mapper.TenantMapper;
import com.erp.tenant.repository.TenantRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant provisioning and lifecycle — the platform-level API behind {@code /api/v1/platform/tenants}.
 * Every method requires {@code PLATFORM_TENANT_MANAGE}, which only the PLATFORM tenant's
 * {@code SYS_ADMIN} role holds (V10); the security chain additionally requires the caller's tenant
 * to be PLATFORM.
 *
 * <p>{@link #create} inserts the {@code CORE_TENANT} row and then lets every
 * {@link TenantProvisioningContributor} (SEC, MDL, NOTIF) set the new tenant up — its role catalog,
 * its first administrator, its reference data — in the <em>same</em> transaction, so a tenant is
 * either fully provisioned or not created at all.
 *
 * <p>No caching: {@code CORE_TENANT} is not on the caching approved-register
 * (gov-enforce-caching-rules), so this service carries no {@code @Cacheable}/{@code @CacheEvict}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "code", "nameAr", "nameEn", "statusCode", "createdAt"
    );

    private final TenantRepository repository;
    private final TenantMapper mapper;
    private final ObjectProvider<TenantProvisioningContributor> contributors;

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantResponse> create(TenantCreateRequest request) {
        log.info("Creating tenant with code: {}", request.getCode());

        boolean codeTaken = request.getCode() != null && repository.existsByCode(request.getCode());
        TenantDomain.create(request.getCode(), codeTaken);

        Tenant saved;
        try {
            saved = repository.saveAndFlush(mapper.toEntity(request));
        } catch (DataIntegrityViolationException e) {
            // A concurrent create won the race between the existsBy probe and the insert.
            throw new LocalizedException(Status.ALREADY_EXISTS,
                TenantErrorCodes.TENANT_CODE_DUPLICATE, request.getCode());
        }

        TenantProvisioning provisioning = new TenantProvisioning(
            saved.getId(), saved.getCode(), TenantConstants.PLATFORM_TENANT_ID,
            SecurityContextHelper.getCurrentUsername(),
            new TenantProvisioning.Administrator(request.getAdminUsername(), request.getAdminEmail(),
                request.getAdminPassword(), request.getAdminFullNameAr(), request.getAdminFullNameEn()));
        List<TenantProvisioningContributor> ordered = contributors.stream()
            .sorted(Comparator.comparingInt(TenantProvisioningContributor::order))
            .toList();
        for (TenantProvisioningContributor contributor : ordered) {
            contributor.provision(provisioning);
        }
        log.info("Created tenant ID: {}, code: {} ({} provisioning contributors)",
            saved.getId(), saved.getCode(), ordered.size());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantResponse> getById(Long id) {
        log.debug("Fetching tenant ID: {}", id);

        Tenant entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));

        return ServiceResult.success(mapper.toResponse(entity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<Page<TenantResponse>> search(TenantSearchRequest searchRequest) {
        log.debug("Searching tenants");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();

        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<Tenant> spec =
            SpecBuilder.build(commonRequest, allowedFields, DefaultFieldValueConverter.INSTANCE);
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<Tenant> page = repository.findAll(spec, pageable);

        return ServiceResult.success(page.map(mapper::toResponse));
    }

    /** {@code GET /api/v1/platform/tenants}: one page of all tenants, unfiltered, in id order. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<Page<TenantResponse>> list(int page, int size) {
        log.debug("Listing tenants, page {} size {}", page, size);
        return search(TenantSearchRequest.builder().sortField("id").page(page).size(size).build());
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantResponse> updateStatus(Long id, TenantStatusUpdateRequest request) {
        log.info("Changing status of tenant ID: {} to {}", id, request.getStatusCode());

        Tenant entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));

        TenantDomain.from(entity).assertCanChangeStatusTo(request.getStatusCode());

        if (TenantConstants.STATUS_SUSPENDED.equals(request.getStatusCode())) {
            entity.suspend();
        } else {
            entity.activate();
        }
        Tenant saved = repository.saveAndFlush(entity);
        log.info("Tenant ID: {} is now {}", saved.getId(), saved.getStatusCode());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }
}
