package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.AdminPasswordSetRequest;
import com.erp.sec.dto.PasswordChangeResponse;
import com.erp.sec.dto.ProfilePhotoResponse;
import com.erp.sec.dto.UserCreateRequest;
import com.erp.sec.dto.UserResponse;
import com.erp.sec.dto.UserRoleAssignmentRequest;
import com.erp.sec.dto.UserSearchRequest;
import com.erp.sec.dto.UserStatusResponse;
import com.erp.sec.dto.UserUpdateRequest;
import com.erp.sec.service.StaffProfileService;
import com.erp.sec.service.UserPasswordService;
import com.erp.sec.service.UserRoleService;
import com.erp.sec.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    private final UserPasswordService passwordService;
    private final StaffProfileService profileService;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "Search users", description = "STAFF accounts only; customer accounts are never listed"
        + " - بحث المستخدمين (حسابات الموظفين فقط، لا تظهر حسابات العملاء)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> search(
            @Valid @RequestBody UserSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "STAFF accounts only; a customer account id answers 404 SEC-404-USER, like an unknown id"
        + " - جلب مستخدم بالمعرّف (حسابات الموظفين فقط)")
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
    @Operation(summary = "Update user", description = "STAFF accounts only; a customer account id answers 404 SEC-404-USER, like an unknown id"
        + " - تحديث مستخدم (حسابات الموظفين فقط)")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Assign roles to user", description = "STAFF accounts only (customers hold no roles); a customer account id answers 404 SEC-404-USER, like an unknown id"
        + " - إسناد أدوار إلى مستخدم (حسابات الموظفين فقط)")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleAssignmentRequest request) {
        return operationCode.craftResponse(userRoleService.assign(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate user", description = "STAFF accounts only; a customer account id answers 404 SEC-404-USER, like an unknown id"
        + " - تعطيل مستخدم وإنهاء جلساته النشطة (حسابات الموظفين فقط)")
    public ResponseEntity<ApiResponse<UserStatusResponse>> deactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Set a user's password", description = "STAFF accounts only (404 SEC-404-USER otherwise), never your own"
        + " (422 SEC-422-PASSWORD-SELF); ends every session of the user; by default the user must change it at the next sign-in"
        + " - تعيين كلمة مرور مستخدم وإنهاء جلساته")
    public ResponseEntity<ApiResponse<PasswordChangeResponse>> setPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordSetRequest request) {
        return operationCode.craftResponse(passwordService.setPassword(id, request));
    }

    @PutMapping("/{id}/photo")
    @Operation(summary = "Set a user's photo", description = "STAFF accounts only; PNG, JPEG or WebP, at most 1 MB; replaces the previous photo"
        + " - تعيين صورة مستخدم")
    public ResponseEntity<ApiResponse<ProfilePhotoResponse>> setUserPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return operationCode.craftResponse(profileService.setUserPhoto(id, file));
    }

    @DeleteMapping("/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a user's photo", description = "STAFF accounts only - إزالة صورة مستخدم")
    public void removeUserPhoto(@PathVariable Long id) {
        profileService.removeUserPhoto(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Reactivate user", description = "STAFF accounts only; a customer account id answers 404 SEC-404-USER, like an unknown id"
        + " - إعادة تفعيل مستخدم معطَّل (حسابات الموظفين فقط)")
    public ResponseEntity<ApiResponse<UserStatusResponse>> reactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.reactivate(id));
    }
}
