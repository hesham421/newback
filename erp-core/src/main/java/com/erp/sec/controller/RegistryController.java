package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.ActionRegistryCreateRequest;
import com.erp.sec.dto.ActionRegistryResponse;
import com.erp.sec.dto.ModuleRegistryCreateRequest;
import com.erp.sec.dto.ModuleRegistryResponse;
import com.erp.sec.dto.RegistryRowResponse;
import com.erp.sec.dto.RegistrySearchRequest;
import com.erp.sec.dto.ScreenRegistryCreateRequest;
import com.erp.sec.dto.ScreenRegistryResponse;
import com.erp.sec.service.RegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller for API-SEC-018/019/020 plus the API-SEC-021 tree search (SCR-REQ-SEC-006). */
@RestController
@RequestMapping("/api/v1/sec/registry")
@RequiredArgsConstructor
@Tag(name = "Module Registry", description = "Module, screen and action registration - تسجيل الوحدات والشاشات والإجراءات")
public class RegistryController {

    private final RegistryService service;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "Search the registry", description = "بحث سجل الوحدات والشاشات والإجراءات")
    public ResponseEntity<ApiResponse<Page<RegistryRowResponse>>> search(
            @Valid @RequestBody RegistrySearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @PostMapping("/modules")
    @Operation(summary = "Register a module", description = "تسجيل وحدة جديدة")
    public ResponseEntity<ApiResponse<ModuleRegistryResponse>> registerModule(
            @Valid @RequestBody ModuleRegistryCreateRequest request) {
        return operationCode.craftResponse(service.registerModule(request));
    }

    @PostMapping("/screens")
    @Operation(summary = "Register a screen", description = "تسجيل شاشة ضمن وحدة مسجَّلة")
    public ResponseEntity<ApiResponse<ScreenRegistryResponse>> registerScreen(
            @Valid @RequestBody ScreenRegistryCreateRequest request) {
        return operationCode.craftResponse(service.registerScreen(request));
    }

    @PostMapping("/actions")
    @Operation(summary = "Register an action", description = "تسجيل إجراء ضمن شاشة مسجَّلة")
    public ResponseEntity<ApiResponse<ActionRegistryResponse>> registerAction(
            @Valid @RequestBody ActionRegistryCreateRequest request) {
        return operationCode.craftResponse(service.registerAction(request));
    }
}
