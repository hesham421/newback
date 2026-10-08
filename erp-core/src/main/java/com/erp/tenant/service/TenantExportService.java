package com.erp.tenant.service;

import com.erp.audit.crossmodule.AuditApi;
import com.erp.audit.crossmodule.AuditEntry;
import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.SecurityContextHelper;
import com.erp.file.crossmodule.DownloadGrant;
import com.erp.file.crossmodule.FilePrivateStoreApi;
import com.erp.file.crossmodule.PrivateFileStoreRequest;
import com.erp.file.crossmodule.StoredPrivateFile;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import com.erp.tenant.TenantExportContributor;
import com.erp.tenant.domain.TenantDomain;
import com.erp.tenant.dto.TenantExportResponse;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import com.erp.tenant.export.TenantExportArchive;
import com.erp.tenant.export.TenantExportGuard;
import com.erp.tenant.mapper.TenantMapper;
import com.erp.tenant.repository.TenantRepository;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * REQ-TENANT-037 (tenant-maturity C5) — {@code POST /api/v1/platform/tenants/{id}/export}: inside the tenant, one read-only
 * snapshot transaction counts (RULE-TENANT-027) and streams every {@link TenantExportContributor}'s CSV files into a ZIP on
 * a temporary file; in PLATFORM, FILE stores it as a PRIVATE document and {@code TENANT_EXPORTED} is recorded; then FILE
 * issues the single-use download token. One export per tenant, at most {@code max-concurrent} in all, per node (RULE-TENANT-028). Not
 * {@code @Transactional}: each step opens its own transaction in its own tenant (the {@code TenantService} precedent).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantExportService {

    /** REQ-TENANT-037: the audit action of an export (target tenant and PLATFORM). */
    static final String ACTION_TENANT_EXPORTED = "TENANT_EXPORTED";

    private static final String ENTITY_TYPE_TENANT = "CORE_TENANT";
    private static final String CONTENT_TYPE_ZIP = "application/zip";
    private static final String UNKNOWN_VERSION = "unknown";
    private static final DateTimeFormatter FILE_STAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final TenantRepository repository;
    private final TenantMapper mapper;
    private final ObjectProvider<TenantExportContributor> contributors;
    private final TenantExportGuard guard;
    private final FilePrivateStoreApi filePrivateStore;
    private final AuditApi auditApi;
    private final PlatformTransactionManager transactionManager;
    private final ErpCoreProperties properties;

    @PreAuthorize("hasAuthority(T(com.erp.tenant.permission.TenantPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<TenantExportResponse> export(Long id) {
        log.info("Exporting the data of tenant ID: {}", id);

        Tenant tenant = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, id));
        int maxConcurrent = properties.getTenant().getExport().getMaxConcurrent();
        TenantExportGuard.Start start = guard.tryStart(id, maxConcurrent);
        TenantDomain.assertExportStartable(start != TenantExportGuard.Start.ALREADY_RUNNING,
            start != TenantExportGuard.Start.BUSY, tenant.getCode(), maxConcurrent);
        Path archive = null;
        try {
            archive = createArchiveFile();
            Path target = archive;
            Instant exportedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            String operator = SecurityContextHelper.getCurrentUsername();
            long maxRows = properties.getTenant().getExport().getMaxRows();
            List<TenantExportContributor> ordered = orderedContributors();

            Long rowCount = TenantContext.callAs(id, () -> snapshot().execute(status ->
                writeArchive(tenant, ordered, target, maxRows, exportedAt, operator)));
            String fileName = "tenant-export-" + tenant.getCode() + "-" + FILE_STAMP.format(exportedAt) + ".zip";
            StoredPrivateFile stored = TenantContext.callAs(TenantConstants.PLATFORM_TENANT_ID, () ->
                requiresNew().execute(status -> storeAndRecord(tenant, target, fileName, rowCount)));
            DownloadGrant grant = TenantContext.callAs(TenantConstants.PLATFORM_TENANT_ID, () ->
                filePrivateStore.issueDownloadToken(stored.documentId()));
            log.info("Exported tenant ID: {} — {} rows, document ID: {} ({} bytes)", id, rowCount, stored.documentId(),
                stored.size());

            return ServiceResult.success(mapper.toExportResponse(tenant, stored.documentId(), stored.fileName(),
                stored.size(), rowCount, grant.token(), grant.expiresAt()));
        } finally {
            guard.finish(id);
            deleteQuietly(archive);
        }
    }

    /** Runs inside the tenant's snapshot: count first (RULE-TENANT-027), then every contributor's files, then the manifest. */
    private long writeArchive(Tenant tenant, List<TenantExportContributor> ordered, Path target, long maxRows,
                              Instant exportedAt, String operator) {
        long counted = 0;
        for (TenantExportContributor contributor : ordered) {
            counted += contributor.countRows(tenant.getId());
        }
        TenantDomain.assertExportWithinLimit(counted, maxRows);

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(target));
             TenantExportArchive zip = new TenantExportArchive(out, tenant.getId(), tenant.getCode(), maxRows, exportedAt)) {
            for (TenantExportContributor contributor : ordered) {
                zip.startModule(contributor.moduleCode());
                contributor.export(zip);
            }
            zip.finish(operator, erpCoreVersion());
            if (zip.rowCount() != counted) {
                log.warn("Export of tenant ID: {} wrote {} rows for {} counted", tenant.getId(), zip.rowCount(), counted);
            }
            return zip.rowCount();
        } catch (IOException | UncheckedIOException e) {
            throw internalError(e);
        }
    }

    /**
     * Runs in a PLATFORM transaction: FILE stores the ZIP as a restricted document (RULE-FILE-012); {@code TENANT_EXPORTED}
     * in PLATFORM and, with an explicit tenant id, in the tenant — all three commit or roll back together.
     */
    private StoredPrivateFile storeAndRecord(Tenant tenant, Path archive, String fileName, long rowCount) {
        StoredPrivateFile stored = filePrivateStore.storePrivateFile(new PrivateFileStoreRequest(
            TenantDomain.EXPORT_OWNER_TYPE, tenant.getId(), TenantDomain.EXPORT_MODULE_CODE, fileName, CONTENT_TYPE_ZIP,
            archive, TenantDomain.EXPORT_REQUIRED_AUTHORITY));
        AuditEntry entry = AuditEntry.builder()
            .action(ACTION_TENANT_EXPORTED)
            .entityType(ENTITY_TYPE_TENANT)
            .entityId(String.valueOf(tenant.getId()))
            .summaryAr("تصدير بيانات المستأجر " + tenant.getCode() + ": " + rowCount + " سجلًا، المستند "
                + stored.documentId())
            .summaryEn("Data of tenant " + tenant.getCode() + " exported: " + rowCount + " rows, document "
                + stored.documentId())
            .build();
        auditApi.record(entry.toBuilder().tenantId(TenantConstants.PLATFORM_TENANT_ID).build());
        if (!Long.valueOf(TenantConstants.PLATFORM_TENANT_ID).equals(tenant.getId())) {
            auditApi.record(entry.toBuilder().tenantId(tenant.getId()).build());
        }
        return stored;
    }

    /** Every contributor in {@code moduleCode} order (the archive refuses a malformed or duplicate code). */
    private List<TenantExportContributor> orderedContributors() {
        return contributors.stream()
            .sorted(Comparator.comparing(TenantExportContributor::moduleCode, Comparator.nullsFirst(Comparator.naturalOrder())))
            .toList();
    }

    /** RULE-TENANT-027: one read-only {@code REPEATABLE READ} transaction, so the count and the files see one snapshot. */
    private TransactionTemplate snapshot() {
        TransactionTemplate template = requiresNew();
        template.setReadOnly(true);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return template;
    }

    /** A new transaction: inside {@code callAs(id)} its Hibernate session is bound to that tenant. */
    private TransactionTemplate requiresNew() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private static Path createArchiveFile() {
        try {
            return Files.createTempFile("erp-tenant-export-", ".zip");
        } catch (IOException e) {
            throw internalError(e);
        }
    }

    private static void deleteQuietly(Path archive) {
        if (archive == null) {
            return;
        }
        try {
            Files.deleteIfExists(archive);
        } catch (IOException e) {
            log.warn("The temporary export archive {} could not be deleted", archive, e);
        }
    }

    private static String erpCoreVersion() {
        String version = TenantExportService.class.getPackage().getImplementationVersion();
        return version == null ? UNKNOWN_VERSION : version;
    }

    private static LocalizedException internalError(Exception cause) {
        LocalizedException failure = new LocalizedException(Status.INTERNAL_ERROR, CommonErrorCodes.INTERNAL_ERROR);
        failure.initCause(cause);
        return failure;
    }
}
