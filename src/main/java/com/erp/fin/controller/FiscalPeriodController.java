package com.erp.fin.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.fin.dto.FiscalPeriodResponse;
import com.erp.fin.dto.FiscalPeriodSearchRequest;
import com.erp.fin.service.FiscalPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for ENT-FIN-008: the three guarded status transitions — API-FIN-024 (open),
 * API-FIN-025 (soft-close) and API-FIN-026 (hard-close), SVC-API-INT.md — plus API-FIN-033, the
 * period search (SCR-REQ-FIN-007 §B1 "search", §B2's {@code fiscalYearId}/{@code statusCode}
 * filters). A period is never created on its own: REQ-FIN-031 generates the whole set with its
 * fiscal year (API-FIN-023), so this resource carries no creating {@code POST} — the only
 * {@code POST} is {@code /search}, per A.6.6 — and, since SRS A7 allows no deletion, no
 * {@code DELETE} either.
 *
 * <p><b>Why {@code PATCH}.</b> Each endpoint is a named domain state transition over a single
 * column, exactly as its spec block states ({@code PATCH /api/v1/fin/fiscal-periods/{id}/open},
 * {@code /soft-close}, {@code /hard-close}); build-create-controller prescribes a template for
 * {@code activate}/{@code deactivate} on an active flag, which FIN_FISCAL_PERIOD does not have
 * (DBF-FIN-075..088 carries none) and which is not what these four PERIOD_STATE states are. There
 * is therefore no conflicting template to resolve, and the verbs stay as specified.
 *
 * <p>None of the three carries a request body: the target state is the endpoint, and the approving
 * principal comes from the security context, never from the caller.
 */
@RestController
@RequestMapping("/api/v1/fin/fiscal-periods")
@RequiredArgsConstructor
@Tag(name = "FIN Fiscal Period Management", description = "Fiscal periods - الفترات المحاسبية")
public class FiscalPeriodController {

    private final FiscalPeriodService service;
    private final OperationCode operationCode;

    @PatchMapping("/{id}/open")
    @Operation(summary = "Open a fiscal period", description = "فتح فترة محاسبية")
    public ResponseEntity<ApiResponse<FiscalPeriodResponse>> open(@PathVariable Long id) {
        return operationCode.craftResponse(service.open(id));
    }

    @PatchMapping("/{id}/soft-close")
    @Operation(summary = "Soft-close a fiscal period", description = "إغلاق ناعم لفترة محاسبية")
    public ResponseEntity<ApiResponse<FiscalPeriodResponse>> softClose(@PathVariable Long id) {
        return operationCode.craftResponse(service.softClose(id));
    }

    @PatchMapping("/{id}/hard-close")
    @Operation(summary = "Hard-close a fiscal period (approval)",
        description = "إغلاق صارم لفترة محاسبية باعتماد")
    public ResponseEntity<ApiResponse<FiscalPeriodResponse>> hardClose(@PathVariable Long id) {
        return operationCode.craftResponse(service.hardClose(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search fiscal periods",
        description = "البحث في الفترات المحاسبية")
    public ResponseEntity<ApiResponse<Page<FiscalPeriodResponse>>> search(
            @Valid @RequestBody FiscalPeriodSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }
}
