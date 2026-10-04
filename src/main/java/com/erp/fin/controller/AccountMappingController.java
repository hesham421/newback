package com.erp.fin.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.fin.dto.AccountMappingCreateRequest;
import com.erp.fin.dto.AccountMappingResponse;
import com.erp.fin.dto.AccountMappingSearchRequest;
import com.erp.fin.dto.AccountMappingUpdateRequest;
import com.erp.fin.service.AccountMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-FIN-038 (SVC-API-SEARCH.md), API-FIN-039, API-FIN-040 and API-FIN-041
 * (SVC-API-CRUD.md, v2) — pure delegation, service plus the shared response helper only (A.6.3).
 * Deactivate is {@code PUT /{id}/deactivate} under UPDATE; FIN publishes no DELETE and no
 * activate (ADR-FIN-024).
 */
@RestController
@RequestMapping("/api/v1/fin/account-mappings")
@RequiredArgsConstructor
@Tag(name = "FIN Account Mapping Management",
    description = "Account mappings for event-driven posting - روابط الحسابات للترحيل بالأحداث")
public class AccountMappingController {

    private final AccountMappingService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create account mapping", description = "إنشاء ربط حساب")
    public ResponseEntity<ApiResponse<AccountMappingResponse>> create(
            @Valid @RequestBody AccountMappingCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update account mapping (the account only)",
        description = "تغيير حساب الربط")
    public ResponseEntity<ApiResponse<AccountMappingResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountMappingUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate account mapping", description = "إلغاء تفعيل ربط حساب")
    public ResponseEntity<ApiResponse<AccountMappingResponse>> deactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search account mappings", description = "البحث في روابط الحسابات")
    public ResponseEntity<ApiResponse<Page<AccountMappingResponse>>> search(
            @Valid @RequestBody AccountMappingSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }
}
