package com.erp.tenant.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.tenant.dto.TenantAdminResetRequest;
import com.erp.tenant.dto.TenantAdminResetResponse;
import com.erp.tenant.dto.TenantCreateRequest;
import com.erp.tenant.dto.TenantResponse;
import com.erp.tenant.dto.TenantSearchRequest;
import com.erp.tenant.dto.TenantStatusUpdateRequest;
import com.erp.tenant.dto.TenantUpdateRequest;
import com.erp.tenant.dto.TenantUsageResponse;
import com.erp.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-level tenant management (erp-core step 05). Every path under {@code /api/v1/platform/**}
 * requires an authenticated PLATFORM-tenant caller holding {@code PLATFORM_TENANT_MANAGE} (security
 * chain) and the service re-checks the authority. Tenants are never deleted (no DELETE) and their code
 * never changes; tenant-maturity B adds the names-and-profile PUT, the administrator recovery and the usage
 * figures. Status changes go through one PATCH, as the step file specifies. Pure delegation — zero business logic.
 */
@RestController
@RequestMapping("/api/v1/platform/tenants")
@RequiredArgsConstructor
@Tag(name = "Platform Tenants", description = "Tenant provisioning and lifecycle - إدارة المستأجرين")
public class PlatformTenantController {

    private final TenantService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create (provision) a tenant with its first administrator",
        description = "adminPassword must meet the STAFF password policy: 400 SEC-400-PASSWORD-POLICY (fieldErrors[0].field ="
            + " adminPassword), raised by SEC's provisioning contributor; nothing is created"
            + " - إنشاء مستأجر وتجهيزه مع أول مدير له؛ يجب أن تستوفي كلمة مرور المدير سياسة كلمات المرور")
    public ResponseEntity<ApiResponse<TenantResponse>> create(@Valid @RequestBody TenantCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @GetMapping
    @Operation(summary = "List tenants (paged)", description = "عرض المستأجرين")
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return operationCode.craftResponse(service.list(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by ID", description = "جلب مستأجر حسب المعرف")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantById(@PathVariable Long id) {
        return operationCode.craftResponse(service.getById(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search tenants", description = "البحث في المستأجرين")
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> search(
            @Valid @RequestBody TenantSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a tenant's names and profile",
        description = "The code and the status never change here - تعديل أسماء المستأجر وملفه")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody TenantUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or suspend a tenant",
        description = "A suspension needs a reason of 3 to 500 characters - تفعيل أو تعليق مستأجر")
    public ResponseEntity<ApiResponse<TenantResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TenantStatusUpdateRequest request) {
        return operationCode.craftResponse(service.updateStatus(id, request));
    }

    @PostMapping("/{id}/admin-reset")
    @Operation(summary = "Reset the password of a tenant administrator (a staff user holding a super role)",
        description = "newPassword must meet the STAFF password policy: 400 SEC-400-PASSWORD-POLICY (fieldErrors[0].field ="
            + " newPassword), raised by SEC inside the tenant; the user's sessions end and, unless"
            + " requireChangeAtNextLogin is false, the password must be changed at the next sign-in"
            + " - إعادة تعيين كلمة مرور مدير المستأجر")
    public ResponseEntity<ApiResponse<TenantAdminResetResponse>> resetAdministratorPassword(
            @PathVariable Long id,
            @Valid @RequestBody TenantAdminResetRequest request) {
        return operationCode.craftResponse(service.resetAdministratorPassword(id, request));
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "Get a tenant's usage figures", description = "أرقام استخدام المستأجر")
    public ResponseEntity<ApiResponse<TenantUsageResponse>> getUsage(@PathVariable Long id) {
        return operationCode.craftResponse(service.getUsage(id));
    }
}
