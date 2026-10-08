package com.erp.tenant.service;

import com.erp.audit.crossmodule.AuditApi;
import com.erp.audit.crossmodule.AuditEntry;
import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.InstantFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.common.util.SecurityContextHelper;
import com.erp.events.DomainEventPublisher;
import com.erp.events.TenantActivatedEvent;
import com.erp.events.TenantCreatedEvent;
import com.erp.events.TenantSuspendedEvent;
import com.erp.file.crossmodule.FileDocumentLookupApi;
import com.erp.file.crossmodule.FileImageStoreApi;
import com.erp.file.crossmodule.ImageStoreRequest;
import com.erp.file.crossmodule.ImageStoreResult;
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
import com.erp.tenant.dto.TenantBrandingUpdateRequest;
import com.erp.tenant.dto.TenantCreateRequest;
import com.erp.tenant.dto.TenantResponse;
import com.erp.tenant.dto.TenantSearchRequest;
import com.erp.tenant.dto.TenantStatusUpdateRequest;
import com.erp.tenant.dto.TenantTokenRevocationResponse;
import com.erp.tenant.dto.TenantUpdateRequest;
import com.erp.tenant.dto.TenantUsageResponse;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import com.erp.tenant.mapper.TenantMapper;
import com.erp.tenant.repository.TenantRepository;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

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
 * {@code TenantContext.callAs(id)} (the {@code PermissionCatalogSynchronizer} precedent). tenant-maturity E's
 * {@link #setLogo} and {@link #removeLogo} do the same (the logo document belongs to the tenant's own rows), and
 * every {@code TenantResponse} resolves its {@code logoUrl} inside the tenant ({@link TenantLogoUrls}). tenant-maturity
 * C12's {@link #revokeTokens} writes the cut-off in a PLATFORM transaction first, then ends the sessions inside the tenant.
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

    /** REQ-TENANT-027 (review round 1): the PLATFORM-side audit action of an admin-reset. */
    static final String ACTION_TENANT_ADMIN_RESET = "TENANT_ADMIN_RESET";

    /** The {@code @Audited} entity type of {@code CORE_TENANT}. */
    private static final String ENTITY_TYPE_TENANT = "CORE_TENANT";

    /** REQ-TENANT-029 (tenant-maturity E): the audit action of a logo set or removed (target tenant and PLATFORM). */
    static final String ACTION_TENANT_LOGO_CHANGED = "TENANT_LOGO_CHANGED";

    /** REQ-TENANT-035 (tenant-maturity C12): the audit action of revoke-tokens (target tenant and PLATFORM). */
    static final String ACTION_TOKENS_REVOKED = "TOKENS_REVOKED";

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
    private final AuditApi auditApi;
    // tenant-maturity E — the logo: FILE's image store, written and read inside the tenant (XM-TENANT-003)
    private final FileImageStoreApi fileImageStore;
    private final TenantLogoUrls logoUrls;

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

        return ServiceResult.success(mapper.toResponse(saved, null), Status.CREATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantResponse> getById(Long id) {
        log.debug("Fetching tenant ID: {}", id);

        Tenant entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));

        return ServiceResult.success(mapper.toResponse(entity, logoUrls.of(entity)));
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
        Map<Long, String> logos = logoUrls.of(page.getContent());

        return ServiceResult.success(page.map(tenant -> mapper.toResponse(tenant, logos.get(tenant.getId()))));
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

        return ServiceResult.success(mapper.toResponse(saved, logoUrls.of(saved)), Status.UPDATED);
    }

    /**
     * {@code PATCH /{id}/status}: PLATFORM protection (RULE-TENANT-005), then the suspension reason (RULE-TENANT-016);
     * a real transition records or clears the suspension facts and publishes {@code TenantSuspendedEvent} /
     * {@code TenantActivatedEvent} (REQ-TENANT-033, delivered after commit); re-applying changes nothing.
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

        boolean transition = domain.changesStatusTo(request.getStatusCode());
        String operator = SecurityContextHelper.getCurrentUsername();
        if (transition) {
            Instant now = Instant.now();
            if (TenantConstants.STATUS_SUSPENDED.equals(request.getStatusCode())) {
                entity.suspend(now, operator, request.getReason());
            } else {
                entity.activate(now);
            }
        }
        Tenant saved = repository.saveAndFlush(entity);
        log.info("Tenant ID: {} is now {}", saved.getId(), saved.getStatusCode());
        if (transition) {
            // tenant-maturity C12: the event's tenant is the changed tenant; listeners run after commit
            eventPublisher.publish(TenantConstants.STATUS_SUSPENDED.equals(saved.getStatusCode())
                ? new TenantSuspendedEvent(saved.getId(), saved.getCode(), saved.getSuspensionReason(), operator)
                : new TenantActivatedEvent(saved.getId(), saved.getCode(), operator));
        }

        return ServiceResult.success(mapper.toResponse(saved, logoUrls.of(saved)), Status.UPDATED);
    }

    /**
     * REQ-TENANT-027 — {@code POST /{id}/admin-reset}: never on PLATFORM; inside tenant {@code id}, in one
     * transaction, SEC finds the user, {@code TenantDomain} decides (RULE-TENANT-017), SEC resets the password, ends
     * the user's sessions and audits {@code ADMIN_PASSWORD_RESET}; then PLATFORM records {@code TENANT_ADMIN_RESET}.
     * Not {@code @Transactional} (see the class comment). Logs name ids only, never the username (plan §1.7).
     */
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantAdminResetResponse> resetAdministratorPassword(Long id, TenantAdminResetRequest request) {
        log.info("Resetting the password of an administrator of tenant ID: {}", id);

        Tenant tenant = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        TenantDomain domain = TenantDomain.from(tenant);
        domain.assertAdminResetAllowed();

        Integer terminated = TenantContext.callAs(id, () -> new TransactionTemplate(transactionManager)
            .execute(status -> resetInsideTenant(request, domain)));
        // the PLATFORM trace of the recovery: this request's tenant, its own commit (no transaction is open here)
        auditApi.record(AuditEntry.builder()
            .action(ACTION_TENANT_ADMIN_RESET)
            .tenantId(TenantConstants.PLATFORM_TENANT_ID)
            .entityType(ENTITY_TYPE_TENANT)
            .entityId(String.valueOf(tenant.getId()))
            .summaryAr("إعادة تعيين كلمة مرور المدير " + request.getUsername() + " في المستأجر " + tenant.getCode()
                + "؛ الجلسات المنتهية: " + terminated)
            .summaryEn("Password of administrator " + request.getUsername() + " of tenant " + tenant.getCode()
                + " reset; sessions terminated: " + terminated)
            .build());
        log.info("Password of an administrator of tenant ID: {} reset; sessions terminated: {}", id, terminated);

        return ServiceResult.success(mapper.toAdminResetResponse(request.getUsername(), terminated), Status.UPDATED);
    }

    /**
     * REQ-TENANT-035 — {@code POST /{id}/revoke-tokens}: never on PLATFORM (RULE-TENANT-024); the cut-off (the next whole
     * second) commits first in this PLATFORM request and alone refuses every earlier token; then inside tenant {@code id}
     * SEC ends every session and {@code TOKENS_REVOKED} is recorded there and in PLATFORM. A failure of that step is
     * recorded in PLATFORM and answered 500 {@code TENANT_REVOKE_SESSIONS_FAILED} (retryable). Not {@code @Transactional}.
     */
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantTokenRevocationResponse> revokeTokens(Long id) {
        log.info("Revoking the tokens of tenant ID: {}", id);

        Tenant tenant = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        TenantDomain.from(tenant).assertTokenRevocationAllowed();

        Instant cutOff = TenantDomain.revocationCutOff(Instant.now());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> writeCutOff(id, cutOff));
        Integer terminated;
        try {
            terminated = TenantContext.callAs(id, () -> writeInTenant().execute(status -> endSessions(tenant)));
        } catch (RuntimeException e) {
            log.error("Tokens of tenant ID: {} revoked, but its sessions could not be terminated", id, e);
            recordSessionsNotTerminated(tenant, e);
            LocalizedException failure = new LocalizedException(Status.INTERNAL_ERROR,
                TenantErrorCodes.TENANT_REVOKE_SESSIONS_FAILED, tenant.getCode());
            failure.initCause(e);
            throw failure;
        }
        log.info("Tokens of tenant ID: {} revoked; sessions terminated: {}", id, terminated);

        return ServiceResult.success(mapper.toTokenRevocationResponse(tenant, terminated), Status.UPDATED);
    }

    /**
     * The PLATFORM trace of a revoke-tokens whose session step failed (its own commit). A failure here too is logged and
     * attached to {@code cause}, so the caller still answers {@code TENANT_REVOKE_SESSIONS_FAILED} (C4, C12 follow-up).
     */
    private void recordSessionsNotTerminated(Tenant tenant, RuntimeException cause) {
        try {
            auditApi.record(AuditEntry.builder()
                .action(ACTION_TOKENS_REVOKED)
                .tenantId(TenantConstants.PLATFORM_TENANT_ID)
                .entityType(ENTITY_TYPE_TENANT)
                .entityId(String.valueOf(tenant.getId()))
                .summaryAr("إبطال رموز الدخول للمستأجر " + tenant.getCode() + "؛ لم تُنهَ الجلسات: أعد الطلب")
                .summaryEn("Tokens of tenant " + tenant.getCode() + " revoked; the sessions were NOT terminated: call again")
                .build());
        } catch (RuntimeException auditFailure) {
            log.error("The PLATFORM audit row of the failed revoke-tokens of tenant ID: {} could not be written",
                tenant.getId(), auditFailure);
            cause.addSuppressed(auditFailure);
        }
    }

    /** Runs in a PLATFORM transaction: the global {@code CORE_TENANT} row gets its new cut-off (RULE-TENANT-023). */
    private void writeCutOff(Long id, Instant cutOff) {
        Tenant tenant = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        tenant.revokeTokens(cutOff);
        repository.saveAndFlush(tenant);
    }

    /** Runs inside the tenant's transaction: SEC ends every session, then {@code TOKENS_REVOKED} (never the instant). */
    private int endSessions(Tenant tenant) {
        int ended = adminRecovery.terminateAllSessions();
        recordInTenantAndPlatform(tenant.getId(), AuditEntry.builder()
            .action(ACTION_TOKENS_REVOKED)
            .entityType(ENTITY_TYPE_TENANT)
            .entityId(String.valueOf(tenant.getId()))
            .summaryAr("إبطال رموز الدخول للمستأجر " + tenant.getCode() + "؛ الجلسات المنتهية: " + ended)
            .summaryEn("Tokens of tenant " + tenant.getCode() + " revoked; sessions terminated: " + ended)
            .build());
        return ended;
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

    /**
     * REQ-TENANT-029 — {@code PUT /{id}/logo}: inside tenant {@code id}, in one transaction, FILE validates and stores the
     * image (RULE-TENANT-018), the tenant points at it, the previous logo is discarded and {@code TENANT_LOGO_CHANGED} is
     * recorded; a refused image stores nothing. Platform-only (RULE-TENANT-020). Not {@code @Transactional} (class comment).
     */
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantResponse> setLogo(Long id, MultipartFile file) {
        log.info("Setting the logo of tenant ID: {}", id);

        repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        byte[] content = readBytes(file);

        TenantResponse response = TenantContext.callAs(id, () -> writeInTenant().execute(status -> replaceLogo(id, content)));
        log.info("Logo of tenant ID: {} set", id);

        return ServiceResult.success(response, Status.UPDATED);
    }

    /**
     * REQ-TENANT-029 — {@code DELETE /{id}/logo}: inside tenant {@code id}, the reference is cleared, the document discarded
     * and {@code TENANT_LOGO_CHANGED} recorded; a tenant without a logo is left as it is. Not {@code @Transactional}.
     */
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public void removeLogo(Long id) {
        log.info("Removing the logo of tenant ID: {}", id);

        repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));

        TenantContext.runAs(id, () -> writeInTenant().executeWithoutResult(status -> clearLogo(id)));
    }

    /** REQ-TENANT-030 — {@code PATCH /{id}/branding}: the brand colour (RULE-TENANT-021); null or blank clears it. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantResponse> updateBranding(Long id, TenantBrandingUpdateRequest request) {
        log.info("Updating the branding of tenant ID: {}", id);

        Tenant entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        TenantDomain.assertBrandColorValid(request.getBrandColor());

        mapper.updateBrandingFromRequest(entity, request);
        Tenant saved = repository.saveAndFlush(entity);
        log.info("Updated the branding of tenant ID: {}", saved.getId());

        return ServiceResult.success(mapper.toResponse(saved, logoUrls.of(saved)), Status.UPDATED);
    }

    /** Runs inside tenant {@code id}'s transaction: store (FILE decides), verdict, point, discard the previous, audit. */
    private TenantResponse replaceLogo(Long id, byte[] content) {
        ImageStoreResult result = fileImageStore.storePublicImage(new ImageStoreRequest(TenantDomain.LOGO_OWNER_TYPE, id,
            TenantDomain.LOGO_MODULE_CODE, content, TenantDomain.LOGO_BASE_NAME, TenantDomain.LOGO_MAX_BYTES,
            TenantDomain.LOGO_TYPES));
        TenantDomain.assertLogoAccepted(result.isStored());

        Tenant tenant = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        Long previous = tenant.getLogoFileId();
        tenant.setLogoFileId(result.image().documentId());
        Tenant saved = repository.saveAndFlush(tenant);
        fileImageStore.discard(previous);
        recordLogoChange(saved, "تعيين شعار المستأجر " + saved.getCode() + " (المستند " + saved.getLogoFileId() + ")",
            "Logo of tenant " + saved.getCode() + " set (document " + saved.getLogoFileId() + ")");
        return mapper.toResponse(saved, result.image().publicUrl());
    }

    /** Runs inside tenant {@code id}'s transaction. */
    private void clearLogo(Long id) {
        Tenant tenant = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        Long previous = tenant.getLogoFileId();
        if (previous == null) {
            log.debug("Tenant ID: {} has no logo to remove", id);
            return;
        }
        tenant.setLogoFileId(null);
        repository.saveAndFlush(tenant);
        fileImageStore.discard(previous);
        recordLogoChange(tenant, "إزالة شعار المستأجر " + tenant.getCode() + " (المستند " + previous + ")",
            "Logo of tenant " + tenant.getCode() + " removed (document " + previous + ")");
        log.info("Logo of tenant ID: {} removed", id);
    }

    /**
     * {@code TENANT_LOGO_CHANGED} in the current tenant (the one whose logo changed) and, in the same transaction, in
     * PLATFORM — the operator's trail (B's {@code TENANT_ADMIN_RESET} precedent); one row when the tenant is PLATFORM.
     */
    private void recordLogoChange(Tenant tenant, String summaryAr, String summaryEn) {
        recordInTenantAndPlatform(tenant.getId(), AuditEntry.builder()
            .action(ACTION_TENANT_LOGO_CHANGED)
            .entityType(ENTITY_TYPE_TENANT)
            .entityId(String.valueOf(tenant.getId()))
            .summaryAr(summaryAr)
            .summaryEn(summaryEn)
            .build());
    }

    /** {@code entry} in the current tenant and, unless that is PLATFORM, once more in PLATFORM (same transaction). */
    private void recordInTenantAndPlatform(Long tenantId, AuditEntry entry) {
        auditApi.record(entry);
        if (!Long.valueOf(TenantConstants.PLATFORM_TENANT_ID).equals(tenantId)) {
            auditApi.record(entry.toBuilder().tenantId(TenantConstants.PLATFORM_TENANT_ID).build());
        }
    }

    /** A new read-write transaction: inside {@code callAs(id)} its Hibernate session is bound to that tenant. */
    private TransactionTemplate writeInTenant() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private static byte[] readBytes(MultipartFile file) {
        if (file == null) {
            return new byte[0];
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new LocalizedException(Status.INTERNAL_ERROR, CommonErrorCodes.INTERNAL_ERROR);
        }
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
