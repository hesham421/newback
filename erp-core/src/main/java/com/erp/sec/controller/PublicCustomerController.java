package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.ConfirmationResponse;
import com.erp.sec.dto.CustomerLoginRequest;
import com.erp.sec.dto.CustomerProfileResponse;
import com.erp.sec.dto.CustomerRegisterRequest;
import com.erp.sec.dto.CustomerVerifyRequest;
import com.erp.sec.dto.LoginResponse;
import com.erp.sec.dto.PasswordResetCompleteRequest;
import com.erp.sec.dto.PasswordResetRequest;
import com.erp.sec.service.CustomerAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * erp-core step 06 — the CUSTOMER realm's public (unauthenticated) endpoints, served by the customer
 * security chain ({@code erp.core.security.customer-public-paths}). Every call names its tenant with the
 * {@code X-Tenant-Code} header (400 {@code TENANT_REQUIRED} otherwise). Thin: everything is delegated to
 * {@link CustomerAccountService}.
 */
@RestController
@RequestMapping("/api/v1/public/customers")
@RequiredArgsConstructor
@Tag(name = "Customer accounts (public)",
    description = "Customer self-registration, e-mail verification, login and password reset - تسجيل العملاء وتأكيد البريد والدخول وإعادة تعيين كلمة المرور")
public class PublicCustomerController {

    private static final String TENANT_HEADER_DOC = "Tenant code - رمز المستأجر";

    private final CustomerAccountService service;
    private final OperationCode operationCode;

    @PostMapping("/register")
    @SecurityRequirements
    @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Code", required = true, description = TENANT_HEADER_DOC)
    @Operation(summary = "Register a customer account", description = "تسجيل حساب عميل جديد وإرسال رابط تأكيد البريد")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> register(
            @Valid @RequestBody CustomerRegisterRequest request) {
        return operationCode.craftResponse(service.register(request));
    }

    @PostMapping("/verify")
    @SecurityRequirements
    @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Code", required = true, description = TENANT_HEADER_DOC)
    @Operation(summary = "Verify a customer e-mail address", description = "تأكيد البريد الإلكتروني وتفعيل الحساب")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> verify(
            @Valid @RequestBody CustomerVerifyRequest request) {
        return operationCode.craftResponse(service.verify(request));
    }

    /** The caller's address is read off the servlet request (stored on the session row). */
    @PostMapping("/login")
    @SecurityRequirements
    @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Code", required = true, description = TENANT_HEADER_DOC)
    @Operation(summary = "Customer login", description = "دخول العميل وإصدار رمز وصول")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody CustomerLoginRequest request,
            HttpServletRequest httpRequest) {
        return operationCode.craftResponse(service.login(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/password-reset/request")
    @SecurityRequirements
    @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Code", required = true, description = TENANT_HEADER_DOC)
    @Operation(summary = "Request a customer password reset", description = "طلب إعادة تعيين كلمة مرور العميل")
    public ResponseEntity<ApiResponse<ConfirmationResponse>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        return operationCode.craftResponse(service.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/complete")
    @SecurityRequirements
    @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Code", required = true, description = TENANT_HEADER_DOC)
    @Operation(summary = "Complete a customer password reset", description = "إتمام إعادة تعيين كلمة مرور العميل")
    public ResponseEntity<ApiResponse<ConfirmationResponse>> completePasswordReset(
            @Valid @RequestBody PasswordResetCompleteRequest request) {
        return operationCode.craftResponse(service.completePasswordReset(request));
    }
}
