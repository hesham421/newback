package com.erp.audit.controller;

import com.erp.audit.dto.AuditEventResponse;
import com.erp.audit.service.AuditEventService;
import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff read access to the generic audit log (erp-core step 10). Pure delegation; the step fixes the
 * shape — a single {@code GET} with query filters, no write endpoints (rows are written in-process
 * only). The path is on the staff security chain, so a customer token gets 403 {@code REALM_MISMATCH};
 * the service requires {@code AUDIT:EVENT:READ}.
 */
@RestController
@RequestMapping("/api/v1/audit/events")
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "Generic tenant-scoped audit events - سجل أحداث التدقيق العام")
public class AuditEventController {

    private final AuditEventService service;
    private final OperationCode operationCode;

    @GetMapping
    @Operation(summary = "Search audit events (newest first)",
        description = "البحث في أحداث التدقيق حسب نوع الكيان ومعرفه والمنفذ والإجراء والفترة الزمنية")
    public ResponseEntity<ApiResponse<Page<AuditEventResponse>>> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return operationCode.craftResponse(service.search(entityType, entityId, actor, action, from, to, page, size));
    }
}
