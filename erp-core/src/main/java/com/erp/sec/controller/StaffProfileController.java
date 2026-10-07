package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.PasswordChangeRequest;
import com.erp.sec.dto.PasswordChangeResponse;
import com.erp.sec.dto.ProfilePhotoResponse;
import com.erp.sec.dto.StaffProfileResponse;
import com.erp.sec.dto.StaffProfileUpdateRequest;
import com.erp.sec.service.StaffProfileService;
import com.erp.sec.service.UserPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * tenant-maturity D — the STAFF caller's own profile, password and photo (REQ-SEC-085/086/087).
 * Authentication only (STAFF chain; a customer token answers 403 {@code REALM_MISMATCH}); no page code.
 * {@code GET /me} and {@code PUT /me/password} stay reachable while a forced password change is pending
 * (RULE-SEC-059). The password change takes the caller's own session from the bearer token, like logout.
 */
@RestController
@RequestMapping("/api/v1/sec/me")
@RequiredArgsConstructor
@Tag(name = "My Profile", description = "The signed-in staff user's own profile, password and photo - الملف الشخصي للمستخدم الحالي")
public class StaffProfileController {

    private final StaffProfileService service;
    private final UserPasswordService passwordService;
    private final OperationCode operationCode;

    @GetMapping
    @Operation(summary = "Get my profile", description = "No roles or permissions (the menu is the client's authority);"
        + " reachable while a password change is pending - جلب ملفي الشخصي (دون أدوار أو صلاحيات)")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> get() {
        return operationCode.craftResponse(service.getMyProfile());
    }

    @PatchMapping
    @Operation(summary = "Update my profile", description = "Only the supplied fields; null keeps, empty clears the optional ones"
        + " - تحديث ملفي الشخصي (الحقول المرسلة فقط)")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> update(
            @Valid @RequestBody StaffProfileUpdateRequest request) {
        return operationCode.craftResponse(service.updateMyProfile(request));
    }

    @PutMapping("/password")
    @Operation(summary = "Change my password", description = "Needs the current password; ends my other sessions; clears a pending"
        + " forced change - تغيير كلمة المرور الخاصة بي")
    public ResponseEntity<ApiResponse<PasswordChangeResponse>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {
        return operationCode.craftResponse(
            passwordService.changeOwnPassword(request, httpRequest.getHeader(HttpHeaders.AUTHORIZATION)));
    }

    @PutMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Set my photo", description = "PNG, JPEG or WebP, at most 1 MB; replaces the previous photo"
        + " - تعيين صورتي (PNG أو JPEG أو WebP بحد أقصى 1 ميغابايت)")
    public ResponseEntity<ApiResponse<ProfilePhotoResponse>> setPhoto(@RequestParam("file") MultipartFile file) {
        return operationCode.craftResponse(service.setMyPhoto(file));
    }

    @DeleteMapping("/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove my photo", description = "إزالة صورتي")
    public void removePhoto() {
        service.removeMyPhoto();
    }
}
