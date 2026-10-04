package com.erp.fin.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.fin.dto.AccountCreateRequest;
import com.erp.fin.dto.AccountResponse;
import com.erp.fin.dto.AccountSearchRequest;
import com.erp.fin.dto.AccountUpdateRequest;
import com.erp.fin.service.AccountService;
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
 * Thin controller for API-FIN-002, API-FIN-003 and API-FIN-004 (SVC-API-CRUD.md) — pure
 * delegation, zero business logic, service plus the shared response helper only (A.6.3).
 *
 * <p>Deactivate is {@code PUT /{id}/deactivate} returning 200 with the entity body, per
 * build-create-controller step 6 — not {@code DELETE}, which this project reserves for a real hard
 * delete ({@code @ResponseStatus(NO_CONTENT)} + {@code void}). ENT-FIN-001 has no hard delete, so
 * this controller declares no {@code DELETE} mapping at all. There is deliberately no
 * {@code activate} endpoint either: no requirement, screen or QR id anchors one.
 *
 * <p>Account search (API-FIN-001) is {@code POST /api/v1/fin/accounts/search}, added by the
 * SVC-API-SEARCH sub — POST with a body, never GET with query params (A.6.6).
 */
@RestController
@RequestMapping("/api/v1/fin/accounts")
@RequiredArgsConstructor
@Tag(name = "FIN Account Management", description = "Chart of accounts - دليل الحسابات")
public class AccountController {

    private final AccountService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create account", description = "إنشاء حساب في دليل الحسابات")
    public ResponseEntity<ApiResponse<AccountResponse>> create(
            @Valid @RequestBody AccountCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update account", description = "تعديل حساب")
    public ResponseEntity<ApiResponse<AccountResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate account", description = "إلغاء تفعيل حساب")
    public ResponseEntity<ApiResponse<AccountResponse>> deactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search accounts", description = "بحث في دليل الحسابات")
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> search(
            @Valid @RequestBody AccountSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }
}
