package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.RoleCreateRequest;
import com.erp.sec.dto.RoleResponse;
import com.erp.sec.dto.RoleSearchRequest;
import com.erp.sec.dto.RoleUpdateRequest;
import com.erp.sec.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller for API-SEC-012/013 (SCR-REQ-SEC-005). */
@RestController
@RequestMapping("/api/v1/sec/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role management - إدارة الأدوار")
public class RoleController {

    private final RoleService service;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "Search roles", description = "بحث الأدوار")
    public ResponseEntity<ApiResponse<Page<RoleResponse>>> search(
            @Valid @RequestBody RoleSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "جلب دور بالمعرّف")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable Long id) {
        return operationCode.craftResponse(service.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create role", description = "إنشاء دور")
    public ResponseEntity<ApiResponse<RoleResponse>> create(
            @Valid @RequestBody RoleCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    /** {@code code} is not in the body — it is the immutable natural key. */
    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "تحديث دور")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }
}
