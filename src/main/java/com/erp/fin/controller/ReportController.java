package com.erp.fin.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.fin.dto.AccountLedgerResponse;
import com.erp.fin.dto.BalanceSheetResponse;
import com.erp.fin.dto.DimensionReportResponse;
import com.erp.fin.dto.IncomeStatementResponse;
import com.erp.fin.dto.TrialBalanceResponse;
import com.erp.fin.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for FIN's five reports — API-FIN-028 … API-FIN-032 (SVC-API-SEARCH.md). Pure
 * delegation, zero business logic, service plus the shared response helper only (A.6.3, A.6.12).
 *
 * <p><b>Why these are {@code GET} with query parameters and not {@code POST /search}.</b> A.6.6
 * governs an entity SEARCH — a paged, filterable listing of one entity, which every one of this
 * sub's seven search endpoints is and which each of them implements as {@code POST /search} with a
 * {@code SearchRequest} body. A report is not that: it is a named, parameterised read whose
 * response shape is an aggregation, not a {@code Page<Entity>Response}, and the controller skill
 * prescribes no template for it. The plan's own {@code Endpoint:} lines state {@code GET} for all
 * five, so there is no conflict between the skill and the plan to resolve here.
 *
 * <p>No request body exists on any of them, so A.6.11's {@code @Valid @RequestBody} has nothing to
 * apply to; every parameter is a scalar the framework binds and the service validates.
 */
@RestController
@RequestMapping("/api/v1/fin/reports")
@RequiredArgsConstructor
@Tag(name = "FIN Financial Reports",
    description = "Live-derived financial reports - التقارير المالية المُشتقة حيًا")
public class ReportController {

    private final ReportService service;
    private final OperationCode operationCode;

    @GetMapping("/account-ledger")
    @Operation(summary = "Account ledger", description = "دفتر الحساب المُشتق حيًا من القيود المُرحَّلة")
    public ResponseEntity<ApiResponse<AccountLedgerResponse>> accountLedger(
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(required = false) Long dimensionId,
            @RequestParam(required = false) Long dimensionValueId) {
        return operationCode.craftResponse(
            service.accountLedger(accountId, fromDate, toDate, dimensionId, dimensionValueId));
    }

    @GetMapping("/trial-balance")
    @Operation(summary = "Trial balance", description = "ميزان المراجعة المُشتق حيًا")
    public ResponseEntity<ApiResponse<TrialBalanceResponse>> trialBalance(
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) String accountTypeCode) {
        return operationCode.craftResponse(service.trialBalance(periodId, accountTypeCode));
    }

    @GetMapping("/balance-sheet")
    @Operation(summary = "Balance sheet", description = "الميزانية العمومية")
    public ResponseEntity<ApiResponse<BalanceSheetResponse>> balanceSheet(
            @RequestParam Long fiscalYearId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate asOfDate) {
        return operationCode.craftResponse(service.balanceSheet(fiscalYearId, asOfDate));
    }

    @GetMapping("/income-statement")
    @Operation(summary = "Income statement", description = "قائمة الدخل")
    public ResponseEntity<ApiResponse<IncomeStatementResponse>> incomeStatement(
            @RequestParam Long fiscalYearId,
            @RequestParam(required = false) Long fromPeriodId,
            @RequestParam(required = false) Long toPeriodId) {
        return operationCode.craftResponse(
            service.incomeStatement(fiscalYearId, fromPeriodId, toPeriodId));
    }

    @GetMapping("/dimension")
    @Operation(summary = "Dimension report",
        description = "تقرير الأبعاد حسب تركيبة الحساب وقيمة البُعد")
    public ResponseEntity<ApiResponse<DimensionReportResponse>> dimensionReport(
            @RequestParam Long dimensionId,
            @RequestParam(required = false) Long dimensionValueId,
            @RequestParam(required = false) Long periodId) {
        return operationCode.craftResponse(
            service.dimensionReport(dimensionId, dimensionValueId, periodId));
    }
}
