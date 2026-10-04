package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.UserCreateRequest;
import com.erp.sec.dto.UserResponse;
import com.erp.sec.dto.UserRoleAssignmentRequest;
import com.erp.sec.dto.UserSearchRequest;
import com.erp.sec.dto.UserStatusResponse;
import com.erp.sec.dto.UserUpdateRequest;
import com.erp.sec.service.UserRoleService;
import com.erp.sec.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-SEC-005/006/007/008/009/010 (SCR-REQ-SEC-004). DELETE is a deactivation that
 * answers 200 with a status confirmation — SEC exposes no hard delete for a user — and PATCH is its
 * reactivation counterpart; both verbs and bodies are the plan's, not the generic CRUD template's.
 */
@RestController
@RequestMapping("/api/v1/sec/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management - إدارة المستخدمين")
public class UserController {

    private final UserService service;
    private final UserRoleService userRoleService;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "Search users", description = "بحث المستخدمين")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> search(
            @Valid @RequestBody UserSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "جلب مستخدم بالمعرّف")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return operationCode.craftResponse(service.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create user", description = "إنشاء مستخدم")
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody UserCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "تحديث مستخدم")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Assign roles to user", description = "إسناد أدوار إلى مستخدم")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleAssignmentRequest request) {
        return operationCode.craftResponse(userRoleService.assign(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate user", description = "تعطيل مستخدم وإنهاء جلساته النشطة")
    public ResponseEntity<ApiResponse<UserStatusResponse>> deactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Reactivate user", description = "إعادة تفعيل مستخدم معطَّل")
    public ResponseEntity<ApiResponse<UserStatusResponse>> reactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.reactivate(id));
    }
}
