package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.DashboardResponse;
import com.erp.sec.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-SEC-022 (SCR-REQ-SEC-007). The endpoint takes no input: which widgets
 * come back is decided by the caller's own grants inside the service (REQ-SEC-023).
 */
@RestController
@RequestMapping("/api/v1/sec/dashboard")
@RequiredArgsConstructor
@Tag(name = "Security Dashboard", description = "Live security dashboard - لوحة تحكم الأمان الحيّة")
public class DashboardController {

    private final DashboardService service;
    private final OperationCode operationCode;

    @GetMapping
    @Operation(summary = "Get the dashboard summary", description = "عرض أرقام لوحة تحكم الأمان محسوبة حيًا")
    public ResponseEntity<ApiResponse<DashboardResponse>> summary() {
        return operationCode.craftResponse(service.summary());
    }
}
