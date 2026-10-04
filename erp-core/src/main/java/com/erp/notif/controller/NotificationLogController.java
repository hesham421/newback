package com.erp.notif.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.notif.dto.NotificationLogResponse;
import com.erp.notif.dto.NotificationLogSearchRequest;
import com.erp.notif.service.NotificationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-NOTIF-002 (query logs) and API-NOTIF-003 (log by id), SCR-NOTIF-003 (read
 * only). Query is POST /search per build-create-controller A.6.6 (the shared search contract is
 * POST-body-only). No create/update/delete surface — the log is a system record. Pure delegation.
 */
@RestController
@RequestMapping("/api/v1/notifications/logs")
@RequiredArgsConstructor
@Tag(name = "Notification Logs", description = "Notification log query (read-only) - استعلام سجل الإشعارات")
public class NotificationLogController {

    private final NotificationLogService service;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "Search notification logs", description = "البحث في سجل الإشعارات")
    public ResponseEntity<ApiResponse<Page<NotificationLogResponse>>> search(
            @Valid @RequestBody NotificationLogSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification log by ID", description = "جلب سجل إشعار حسب المعرف")
    public ResponseEntity<ApiResponse<NotificationLogResponse>> getById(@PathVariable Long id) {
        return operationCode.craftResponse(service.getById(id));
    }
}
