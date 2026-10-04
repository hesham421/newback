package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.ActiveSessionResponse;
import com.erp.sec.dto.ActiveSessionSearchRequest;
import com.erp.sec.dto.SessionTerminationResponse;
import com.erp.sec.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-SEC-025/026 (SCR-REQ-SEC-009). DELETE terminates the session and answers 200
 * with {@code {activeSessionPk, terminatedAt}} — the row is stamped, never removed.
 */
@RestController
@RequestMapping("/api/v1/sec/sessions")
@RequiredArgsConstructor
@Tag(name = "Active Sessions", description = "Active session management - إدارة الجلسات النشطة")
public class SessionController {

    private final SessionService service;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "List active sessions", description = "عرض الجلسات غير المنتهية")
    public ResponseEntity<ApiResponse<Page<ActiveSessionResponse>>> search(
            @Valid @RequestBody ActiveSessionSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Terminate session", description = "إنهاء جلسة نشطة")
    public ResponseEntity<ApiResponse<SessionTerminationResponse>> terminate(@PathVariable Long id) {
        return operationCode.craftResponse(service.terminate(id));
    }
}
