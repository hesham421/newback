package com.erp.tenant.controller;

import com.erp.common.idempotency.IdempotentResponses;
import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.tenant.dto.TenantAdminResetRequest;
import com.erp.tenant.dto.TenantAdminResetResponse;
import com.erp.tenant.dto.TenantBrandingUpdateRequest;
import com.erp.tenant.dto.TenantCreateRequest;
import com.erp.tenant.dto.TenantExportResponse;
import com.erp.tenant.dto.TenantResponse;
import com.erp.tenant.dto.TenantSearchRequest;
import com.erp.tenant.dto.TenantStatusUpdateRequest;
import com.erp.tenant.dto.TenantTokenRevocationResponse;
import com.erp.tenant.dto.TenantUpdateRequest;
import com.erp.tenant.dto.TenantUsageResponse;
import com.erp.tenant.service.TenantExportService;
import com.erp.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Platform-level tenant management (erp-core step 05). Every path under {@code /api/v1/platform/**}
 * requires an authenticated PLATFORM-tenant caller holding {@code PLATFORM_TENANT_MANAGE} (security
 * chain) and the service re-checks the authority. Tenants are never deleted (no DELETE) and their code
 * never changes; tenant-maturity B adds the names-and-profile PUT, the administrator recovery and the usage
 * figures, E the logo and the brand colour (D5), C4 the optional {@code Idempotency-Key} of the create (answered by the
 * common response helper {@link IdempotentResponses}), C5 the data export. Status changes go through one PATCH. Pure
 * delegation — zero logic.
 */
@RestController
@RequestMapping("/api/v1/platform/tenants")
@RequiredArgsConstructor
@Tag(name = "Platform Tenants", description = "Tenant provisioning and lifecycle - إدارة المستأجرين")
public class PlatformTenantController {

    /** tenant-maturity C4: the endpoint id under which the create's idempotency keys are stored. */
    private static final String CREATE_ENDPOINT = "POST /api/v1/platform/tenants";

    private final TenantService service;
    private final TenantExportService exportService;
    private final OperationCode operationCode;
    private final IdempotentResponses idempotentResponses;

    @PostMapping
    @Operation(summary = "Create (provision) a tenant with its first administrator",
        description = "adminPassword must meet the STAFF password policy: 400 SEC-400-PASSWORD-POLICY (fieldErrors[0].field ="
            + " adminPassword), raised by SEC's provisioning contributor; nothing is created."
            + " Optional header Idempotency-Key (1 to 64 characters of A-Z a-z 0-9 . _ : -): a retry with the same key and"
            + " the same body by the same user answers the stored 201 response with the response header"
            + " Idempotent-Replayed: true and creates nothing; the same key with another body, or by another user, answers"
            + " 409 IDEMPOTENCY_KEY_CONFLICT; an invalid key answers 400 IDEMPOTENCY_KEY_INVALID; only successful answers"
            + " are stored, for 24 hours (erp.core.idempotency.retention)"
            + " - إنشاء مستأجر وتجهيزه مع أول مدير له؛ يجب أن تستوفي كلمة مرور المدير سياسة كلمات المرور؛"
            + " ترويسة Idempotency-Key اختيارية: إعادة الطلب بالمفتاح نفسه تعيد الاستجابة المخزّنة دون إنشاء شيء")
    public ResponseEntity<ApiResponse<TenantResponse>> create(
            @Parameter(description = "Optional idempotency key of this create (e.g. a UUID reused for every retry of one"
                + " submission) - مفتاح عدم التكرار", schema = @Schema(maxLength = 64, pattern = "^[A-Za-z0-9._:-]{1,64}$"))
            @RequestHeader(name = IdempotentResponses.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody TenantCreateRequest request) {
        return idempotentResponses.craftResponse(idempotencyKey, CREATE_ENDPOINT, request, TenantResponse.class,
            () -> service.create(request));
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

    @PostMapping("/{id}/revoke-tokens")
    @Operation(summary = "Revoke every token of a tenant (sign all its users out)",
        description = "Sets the tenant's token cut-off to now and terminates its sessions (staff and customer); every token"
            + " issued before then answers 401 TENANT_TOKEN_REVOKED. Refused for the PLATFORM tenant: 422"
            + " TENANT_REVOKE_TOKENS_PLATFORM - إبطال رموز المستأجر")
    public ResponseEntity<ApiResponse<TenantTokenRevocationResponse>> revokeTenantTokens(@PathVariable Long id) {
        return operationCode.craftResponse(service.revokeTokens(id));
    }

    @PostMapping("/{id}/export")
    @Operation(summary = "Export a tenant's data (ZIP of CSV files, PRIVATE PLATFORM document, single-use download token)",
        description = "Synchronous: every module writes the tenant's rows as CSV (UTF-8 with BOM) into a ZIP with manifest.json,"
            + " never password or token hashes, credentials or file bytes; the ZIP is stored as a PRIVATE file document of"
            + " the PLATFORM tenant and downloaded once with GET /api/v1/files/download?token={downloadToken} (10 minutes, same"
            + " user). 422 TENANT_EXPORT_TOO_LARGE above erp.core.tenant.export.max-rows; 409 TENANT_EXPORT_IN_PROGRESS while"
            + " an export of the same tenant runs - تصدير بيانات المستأجر")
    public ResponseEntity<ApiResponse<TenantExportResponse>> exportTenant(@PathVariable Long id) {
        return operationCode.craftResponse(exportService.export(id));
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "Get a tenant's usage figures", description = "أرقام استخدام المستأجر")
    public ResponseEntity<ApiResponse<TenantUsageResponse>> getUsage(@PathVariable Long id) {
        return operationCode.craftResponse(service.getUsage(id));
    }

    @PutMapping("/{id}/logo")
    @Operation(summary = "Set or replace a tenant's logo",
        description = "Multipart part file: a PNG, JPEG, WebP or plain SVG image of at most 1 MB, detected from its bytes"
            + " (SVG without scripts, event attributes, external references, editor metadata, DOCTYPE or duplicate ids:"
            + " export it as plain or optimised SVG); otherwise 400 TENANT_LOGO_INVALID (fieldErrors[0].field = file)."
            + " Stored as a PUBLIC document of that tenant; the previous logo is discarded and its URL answers 404"
            + " - تعيين شعار المستأجر أو استبداله")
    public ResponseEntity<ApiResponse<TenantResponse>> setTenantLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return operationCode.craftResponse(service.setLogo(id, file));
    }

    @DeleteMapping("/{id}/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a tenant's logo",
        description = "The document is discarded and its URL answers 404; a tenant without a logo is not an error"
            + " - إزالة شعار المستأجر")
    public void removeTenantLogo(@PathVariable Long id) {
        service.removeLogo(id);
    }

    @PatchMapping("/{id}/branding")
    @Operation(summary = "Set or clear a tenant's brand colour",
        description = "brandColor #RRGGBB (stored upper case), null or blank clears it; otherwise 400"
            + " TENANT_BRAND_COLOR_INVALID - ضبط لون علامة المستأجر")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenantBranding(
            @PathVariable Long id,
            @Valid @RequestBody TenantBrandingUpdateRequest request) {
        return operationCode.craftResponse(service.updateBranding(id, request));
    }
}
