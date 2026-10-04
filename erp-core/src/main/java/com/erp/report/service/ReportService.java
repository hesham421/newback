package com.erp.report.service;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.util.SecurityContextHelper;
import com.erp.mdl.crossmodule.LookupOptionView;
import com.erp.mdl.crossmodule.MdlLookupApi;
import com.erp.report.ReportAuthorities;
import com.erp.report.ReportProvider;
import com.erp.report.ReportResult;
import com.erp.report.domain.ReportParametersDomain;
import com.erp.report.domain.ReportRunDomain;
import com.erp.report.domain.ReportRunDomain.ExportFormat;
import com.erp.report.dto.ReportDefinitionResponse;
import com.erp.report.dto.ReportExportRequest;
import com.erp.report.dto.ReportRunRequest;
import com.erp.report.dto.ReportRunResponse;
import com.erp.report.exception.ReportErrorCodes;
import com.erp.report.export.CsvReportWriter;
import com.erp.report.mapper.ReportMapper;
import com.erp.report.registry.ReportRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lists, runs and exports the registered reports (erp-core step 11). Orchestration only: the run
 * permission, the parameter rules, the export cap and the export format are decided by
 * {@link ReportRunDomain} / {@link ReportParametersDomain}.
 *
 * <p><b>Authorization.</b> Every method is gated {@code isAuthenticated()} (the staff chain already
 * rejects customer tokens with 403 {@code REALM_MISMATCH}); the per-report permission
 * {@code <MODULE>:REPORT:<CODE>} depends on the path variable, which a fixed {@code @PreAuthorize}
 * constant cannot express, so it is checked with {@link SecurityContextHelper#hasAuthority(String)}
 * and decided by {@link ReportRunDomain#assertCanRun(boolean)} (403 {@code ACCESS_DENIED}); the
 * definitions list shows only reports the caller holds.
 *
 * <p><b>Tenant.</b> Providers run inside this read-only transaction on the request thread, whose tenant
 * the security chain has set; their JPA queries are tenant-filtered by Hibernate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRegistry registry;
    private final ReportMapper mapper;
    private final MdlLookupApi mdlLookupApi;
    private final ErpCoreProperties properties;
    private final JsonMapper jsonMapper;

    /** An exported file. */
    public record ReportExport(String fileName, String contentType, byte[] content) {
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<List<ReportDefinitionResponse>> listDefinitions() {
        log.debug("Listing the report definitions the caller may run");
        return ServiceResult.success(registry.all().stream()
            .filter(provider -> SecurityContextHelper.hasAuthority(ReportAuthorities.of(provider)))
            .map(mapper::toDefinitionResponse)
            .toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<ReportDefinitionResponse> getDefinition(String code) {
        log.debug("Fetching report definition {}", code);
        return ServiceResult.success(mapper.toDefinitionResponse(permittedProvider(code)));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<ReportRunResponse> run(String code, ReportRunRequest request) {
        log.debug("Running report {}", code);
        ReportProvider provider = permittedProvider(code);
        ReportRunRequest body = request == null ? new ReportRunRequest() : request;
        Map<String, Object> params = validatedParams(provider, body.getParams());

        Pageable pageable = PageableBuilder.from(SearchRequest.builder()
            .page(body.getPage() == null ? 0 : body.getPage())
            .size(body.getSize() == null ? 0 : body.getSize())
            .build(), Set.of());
        ReportResult result = provider.run(params, pageable);
        return ServiceResult.success(
            mapper.toRunResponse(code, result, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<ReportExport> export(String code, String format, ReportExportRequest request) {
        log.debug("Exporting report {} as {}", code, format);
        ReportProvider provider = permittedProvider(code);
        ExportFormat exportFormat = ReportRunDomain.exportFormat(format);
        Map<String, Object> params = validatedParams(provider, request == null ? null : request.getParams());

        int maxRows = properties.getReport().getMaxExportRows();
        // one page of cap + 1 rows: enough to detect an export over the cap without loading more
        ReportResult result = provider.run(params, PageRequest.of(0, maxRows == Integer.MAX_VALUE ? maxRows : maxRows + 1));
        ReportRunDomain.assertWithinExportCap(result.rows().size(), result.totalRows(), maxRows);

        byte[] content = exportFormat == ExportFormat.CSV
            ? CsvReportWriter.toBytes(result.columns(), result.rows(), isArabic())
            : jsonMapper.writeValueAsBytes(mapper.toRunResponse(code, result, null, null));
        return ServiceResult.success(new ReportExport(code + "." + exportFormat.extension(),
            exportFormat.contentType(), content));
    }

    /** The registered report {@code code} (404 otherwise) that the caller may run (403 otherwise). */
    private ReportProvider permittedProvider(String code) {
        ReportProvider provider = registry.find(code)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, ReportErrorCodes.REPORT_NOT_FOUND, code));
        ReportRunDomain.assertCanRun(SecurityContextHelper.hasAuthority(ReportAuthorities.of(provider)));
        return provider;
    }

    private Map<String, Object> validatedParams(ReportProvider provider, Map<String, Object> raw) {
        return ReportParametersDomain.from(provider.params()).validate(raw, activeLookupCodes());
    }

    /**
     * Active codes of a lookup type, read through MDL's cross-module API (read-only, joins this
     * read-only transaction). MDL answers an unknown or inactive type with its own 404; here that means
     * "no valid value", so the parameter is reported as {@code REPORT_PARAM_INVALID} instead.
     */
    private Function<String, Set<String>> activeLookupCodes() {
        Map<String, Set<String>> cache = new HashMap<>();
        return key -> cache.computeIfAbsent(key, typeKey -> {
            try {
                return mdlLookupApi.readActiveValuesByKey(typeKey).stream()
                    .map(LookupOptionView::code).collect(Collectors.toUnmodifiableSet());
            } catch (LocalizedException unknownType) {
                log.warn("Report parameter lookup type {} is unknown or inactive", typeKey);
                return Set.of();
            }
        });
    }

    /** CSV headers in Arabic for an Arabic {@code Accept-Language}, otherwise English. */
    private static boolean isArabic() {
        return "ar".equals(LocaleContextHolder.getLocale().getLanguage());
    }
}
