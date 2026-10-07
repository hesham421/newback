package com.erp.tenant.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.InstantFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.common.util.SecurityContextHelper;
import com.erp.events.DomainEventPublisher;
import com.erp.events.TenantCreatedEvent;
import com.erp.file.crossmodule.FileDocumentLookupApi;
import com.erp.notif.crossmodule.NotificationLogQueryApi;
import com.erp.sec.crossmodule.RecoveryTarget;
import com.erp.sec.crossmodule.SecAdminRecoveryApi;
import com.erp.sec.crossmodule.SecUserDirectoryApi;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import com.erp.tenant.TenantProvisioning;
import com.erp.tenant.TenantProvisioningContributor;
import com.erp.tenant.domain.TenantDomain;
import com.erp.tenant.dto.TenantAdminResetRequest;
import com.erp.tenant.dto.TenantAdminResetResponse;
import com.erp.tenant.dto.TenantCreateRequest;
import com.erp.tenant.dto.TenantResponse;
import com.erp.tenant.dto.TenantSearchRequest;
import com.erp.tenant.dto.TenantStatusUpdateRequest;
import com.erp.tenant.dto.TenantUpdateRequest;
import com.erp.tenant.dto.TenantUsageResponse;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import com.erp.tenant.mapper.TenantMapper;
import com.erp.tenant.repository.TenantRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
 * <p>tenant-maturity B: {@link #resetAdministratorPassword} and {@link #getUsage} work <em>inside</em> another
 * tenant through SEC, FILE and NOTIF. They are deliberately not {@code @Transactional}: a transaction opened in
 * this PLATFORM request would bind the PLATFORM Hibernate session, so each opens its own inside
 * {@code TenantContext.callAs(id)} (the {@code PermissionCatalogSynchronizer} precedent).
 *
 * <p>No caching: {@code CORE_TENANT} is not on the caching approved-register
 * (gov-enforce-caching-rules), so this service carries no {@code @Cacheable}/{@code @CacheEvict}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "code", "nameAr", "nameEn", "statusCode", "createdAt",
        "contactEmail", "countryCode", "suspendedAt"
    );

    /** Filter values of these fields are ISO-8601 instants. */
    private static final InstantFieldValueConverter INSTANT_FIELDS = new InstantFieldValueConverter(Set.of("suspendedAt"));

    /** REQ-TENANT-028: the window of {@code notificationsLast30Days}. */
    private static final Duration NOTIFICATION_WINDOW = Duration.ofDays(30);

    private final TenantRepository repository;
    private final TenantMapper mapper;
    private final ObjectProvider<TenantProvisioningContributor> contributors;
    private final DomainEventPublisher eventPublisher;
    // tenant-maturity B — the cross-module surfaces read or written inside another tenant
    private final SecUserDirectoryApi userDirectory;
    private final SecAdminRecoveryApi adminRecovery;
    private final FileDocumentLookupApi fileDocuments;
    private final NotificationLogQueryApi notificationLog;
    private final PlatformTransactionManager transactionManager;

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
        // erp-core step 08 — the event's tenant is the NEW tenant (see TenantCreatedEvent)
        eventPublisher.publish(new TenantCreatedEvent(saved.getId(), saved.getCode(),
            SecurityContextHelper.getCurrentUsername()));

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
        Specification<Tenant> spec = SpecBuilder.build(commonRequest, allowedFields, INSTANT_FIELDS);
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

    /**
     * REQ-TENANT-025 — {@code PUT /{id}}: names and profile, a full replacement; the code and the status never
     * change here (RULE-TENANT-003). A write racing another one fails on {@code VERSION} (409).
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantResponse> update(Long id, TenantUpdateRequest request) {
        log.info("Updating tenant ID: {}", id);

        Tenant entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));

        mapper.updateEntityFromRequest(entity, request);
        Tenant saved = repository.saveAndFlush(entity);
        log.info("Updated tenant ID: {}", saved.getId());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * {@code PATCH /{id}/status}: PLATFORM protection (RULE-TENANT-005), then the suspension reason
     * (RULE-TENANT-016); a real transition records or clears the suspension facts, re-applying changes nothing.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantResponse> updateStatus(Long id, TenantStatusUpdateRequest request) {
        log.info("Changing status of tenant ID: {} to {}", id, request.getStatusCode());

        Tenant entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));

        TenantDomain domain = TenantDomain.from(entity);
        domain.assertCanChangeStatusTo(request.getStatusCode());
        domain.assertSuspensionReasonGiven(request.getStatusCode(), request.getReason());

        if (domain.changesStatusTo(request.getStatusCode())) {
            Instant now = Instant.now();
            if (TenantConstants.STATUS_SUSPENDED.equals(request.getStatusCode())) {
                entity.suspend(now, SecurityContextHelper.getCurrentUsername(), request.getReason());
            } else {
                entity.activate(now);
            }
        }
        Tenant saved = repository.saveAndFlush(entity);
        log.info("Tenant ID: {} is now {}", saved.getId(), saved.getStatusCode());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * REQ-TENANT-027 — {@code POST /{id}/admin-reset}: inside tenant {@code id}, in one transaction, SEC finds the
     * user, {@code TenantDomain} decides (RULE-TENANT-017) and SEC resets the password, ends the user's sessions
     * and audits {@code ADMIN_PASSWORD_RESET}. Not {@code @Transactional} (see the class comment).
     */
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantAdminResetResponse> resetAdministratorPassword(Long id, TenantAdminResetRequest request) {
        log.info("Resetting the password of administrator {} of tenant ID: {}", request.getUsername(), id);

        Tenant tenant = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        TenantDomain domain = TenantDomain.from(tenant);

        Integer terminated = TenantContext.callAs(id, () -> new TransactionTemplate(transactionManager)
            .execute(status -> resetInsideTenant(request, domain)));
        log.info("Password of administrator {} of tenant ID: {} reset; sessions terminated: {}",
            request.getUsername(), id, terminated);

        return ServiceResult.success(mapper.toAdminResetResponse(request.getUsername(), terminated), Status.UPDATED);
    }

    /**
     * REQ-TENANT-028 — {@code GET /{id}/usage}: every figure counted inside tenant {@code id} by its owner module,
     * in one read-only transaction of that tenant. Not {@code @Transactional} (see the class comment).
     */
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantUsageResponse> getUsage(Long id) {
        log.debug("Collecting the usage figures of tenant ID: {}", id);

        Tenant tenant = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        Instant collectedAt = Instant.now();
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);

        TenantUsageResponse usage = TenantContext.callAs(tenant.getId(), () -> readOnly.execute(status ->
            mapper.toUsageResponse(tenant.getId(), userDirectory.countStaff(), userDirectory.countCustomers(),
                userDirectory.countActiveSessions(), fileDocuments.countDocuments(), fileDocuments.sumBytes(),
                notificationLog.countDispatchedSince(collectedAt.minus(NOTIFICATION_WINDOW)), collectedAt)));

        return ServiceResult.success(usage);
    }

    /** Runs inside tenant {@code domain}'s transaction: facts from SEC, decision here, write by SEC. */
    private int resetInsideTenant(TenantAdminResetRequest request, TenantDomain domain) {
        Optional<RecoveryTarget> target = adminRecovery.findRecoveryTarget(request.getUsername());
        domain.assertCanResetAdministrator(request.getUsername(), target.isPresent(),
            target.map(RecoveryTarget::superRole).orElse(false));
        return adminRecovery.resetSuperUserPassword(request.getUsername(), request.getNewPassword(),
            request.getRequireChangeAtNextLogin());
    }
}
