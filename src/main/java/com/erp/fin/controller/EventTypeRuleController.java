package com.erp.fin.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.fin.dto.EventTypeRuleCreateRequest;
import com.erp.fin.dto.EventTypeRuleResponse;
import com.erp.fin.dto.EventTypeRuleSearchRequest;
import com.erp.fin.dto.RuleLineCreateRequest;
import com.erp.fin.dto.RuleLineResponse;
import com.erp.fin.service.EventTypeRuleService;
import com.erp.fin.service.RuleLineService;
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
 * Thin controller for API-FIN-010 and API-FIN-011 (SVC-API-CRUD.md).
 *
 * <p>A.6.9 — adding a rule line is {@code createRuleLine} on this class, delegating to
 * {@link RuleLineService}. There is deliberately no {@code RuleLineController}.
 *
 * <p>Rule search (API-FIN-009) is {@code POST /api/v1/fin/event-rules/search}, owned by the
 * SVC-API-SEARCH sub.
 *
 * <p>Deactivate (API-FIN-034) is {@code PUT /{id}/deactivate} returning 200 with the entity body,
 * per build-create-controller step 6 — not {@code DELETE}, which this project reserves for a real
 * hard delete. There is deliberately no {@code activate} counterpart: A.6.7 forbids a single
 * toggle endpoint but does not require an activate to exist, and no requirement, screen or QR id
 * anchors one — the same deliberate choice {@code AccountController} records.
 */
@RestController
@RequestMapping("/api/v1/fin/event-rules")
@RequiredArgsConstructor
@Tag(name = "FIN Event Rule Management",
    description = "Accounting event-type rules - قواعد أنواع الأحداث المحاسبية")
public class EventTypeRuleController {

    private final EventTypeRuleService service;
    private final RuleLineService ruleLineService;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create event-type rule", description = "إنشاء قاعدة نوع حدث محاسبي")
    public ResponseEntity<ApiResponse<EventTypeRuleResponse>> create(
            @Valid @RequestBody EventTypeRuleCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @PostMapping("/{id}/lines")
    @Operation(summary = "Add rule line", description = "إضافة سطر إلى قاعدة نوع الحدث")
    public ResponseEntity<ApiResponse<RuleLineResponse>> createRuleLine(
            @PathVariable("id") Long eventTypeRuleId,
            @Valid @RequestBody RuleLineCreateRequest request) {
        return operationCode.craftResponse(ruleLineService.create(eventTypeRuleId, request));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate event-type rule",
        description = "إلغاء تفعيل قاعدة نوع حدث محاسبي")
    public ResponseEntity<ApiResponse<EventTypeRuleResponse>> deactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search event-type rules", description = "بحث في قواعد أنواع الأحداث")
    public ResponseEntity<ApiResponse<Page<EventTypeRuleResponse>>> search(
            @Valid @RequestBody EventTypeRuleSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }
}
