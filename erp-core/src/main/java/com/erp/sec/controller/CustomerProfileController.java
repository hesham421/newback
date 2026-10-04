package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.CustomerProfileResponse;
import com.erp.sec.dto.CustomerProfileUpdateRequest;
import com.erp.sec.service.CustomerAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * erp-core step 06 — the authenticated customer's own account ({@code /api/v1/customers/me}), served by
 * the customer security chain: a CUSTOMER token is required, a staff token is refused (403
 * {@code REALM_MISMATCH}). Thin: everything is delegated to {@link CustomerAccountService}.
 */
@RestController
@RequestMapping("/api/v1/customers/me")
@RequiredArgsConstructor
@Tag(name = "Customer accounts (self)", description = "The signed-in customer's own profile - الملف الشخصي للعميل")
public class CustomerProfileController {

    private final CustomerAccountService service;
    private final OperationCode operationCode;

    @GetMapping
    @Operation(summary = "Get my customer profile", description = "عرض الملف الشخصي للعميل")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> me() {
        return operationCode.craftResponse(service.me());
    }

    @PatchMapping
    @Operation(summary = "Update my customer profile", description = "تعديل الملف الشخصي للعميل")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateMe(
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return operationCode.craftResponse(service.updateMe(request));
    }
}
