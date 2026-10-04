package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.AuditLogEntryResponse;
import com.erp.sec.dto.AuditLogEntrySearchRequest;
import com.erp.sec.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-SEC-023/024 (SCR-REQ-SEC-008). The export is a {@code text/csv} document, not
 * a JSON envelope (Response line), so it is the one SEC endpoint not crafted through
 * {@code OperationCode}.
 */
@RestController
@RequestMapping("/api/v1/sec/audit-log")
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "Immutable security audit log - سجل التدقيق الأمني غير القابل للتعديل")
public class AuditLogController {

    private static final String EXPORT_FILENAME = "attachment; filename=\"sec-audit-log.csv\"";

    private final AuditLogService service;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "Search the audit log", description = "بحث سجل التدقيق")
    public ResponseEntity<ApiResponse<Page<AuditLogEntryResponse>>> search(
            @Valid @RequestBody AuditLogEntrySearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @GetMapping("/export")
    @Operation(summary = "Export the audit log", description = "تصدير سجل التدقيق بصيغة CSV")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String eventTypeCode,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredTo) {
        return ResponseEntity
            .ok()
            // The charset is declared explicitly: without it Excel opens the document as ANSI and
            // renders the Arabic detailsAr column as mojibake. The service prepends the matching
            // UTF-8 BOM, which is what Excel actually reads.
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION, EXPORT_FILENAME)
            .body(service.export(eventTypeCode, actorUserId, occurredFrom, occurredTo).getData());
    }
}
