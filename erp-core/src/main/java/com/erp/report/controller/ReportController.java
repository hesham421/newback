package com.erp.report.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.report.dto.ReportDefinitionResponse;
import com.erp.report.dto.ReportExportRequest;
import com.erp.report.dto.ReportRunRequest;
import com.erp.report.dto.ReportRunResponse;
import com.erp.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The reporting endpoints (erp-core step 11), STAFF realm. JSON endpoints use the shared response
 * envelope; the export answers the raw file (like FILE's download) with an attachment disposition.
 * Pure delegation — zero business logic.
 */
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report definitions, run and export - تعريفات التقارير وتشغيلها وتصديرها")
public class ReportController {

    private final ReportService service;
    private final OperationCode operationCode;

    @GetMapping("/definitions")
    @Operation(summary = "List the reports the caller may run", description = "عرض التقارير المسموح للمستخدم بتشغيلها")
    public ResponseEntity<ApiResponse<List<ReportDefinitionResponse>>> listDefinitions() {
        return operationCode.craftResponse(service.listDefinitions());
    }

    @GetMapping("/definitions/{code}")
    @Operation(summary = "Get one report definition", description = "جلب تعريف تقرير")
    public ResponseEntity<ApiResponse<ReportDefinitionResponse>> getDefinition(@PathVariable String code) {
        return operationCode.craftResponse(service.getDefinition(code));
    }

    @PostMapping("/{code}/run")
    @Operation(summary = "Run a report (one page, JSON)", description = "تشغيل تقرير (صفحة واحدة بصيغة JSON)")
    public ResponseEntity<ApiResponse<ReportRunResponse>> run(
            @PathVariable String code,
            @Valid @RequestBody(required = false) ReportRunRequest request) {
        return operationCode.craftResponse(service.run(code, request));
    }

    @PostMapping("/{code}/export")
    @Operation(summary = "Export a report as CSV (UTF-8 BOM, headers by Accept-Language) or JSON",
        description = "تصدير تقرير بصيغة CSV أو JSON")
    public ResponseEntity<byte[]> export(
            @PathVariable String code,
            @RequestParam(name = "format", defaultValue = "csv") String format,
            @Valid @RequestBody(required = false) ReportExportRequest request) {
        ReportService.ReportExport export = service.export(code, format, request).getData();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(export.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName(), StandardCharsets.UTF_8).build().toString())
            .body(export.content());
    }
}
