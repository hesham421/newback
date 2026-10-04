package com.erp.fin.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.fin.dto.AllocationRuleCreateRequest;
import com.erp.fin.dto.AllocationRuleResponse;
import com.erp.fin.dto.AllocationRuleSearchRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.service.AllocationRuleService;
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
 * Thin controller for API-FIN-016 (SVC-API-CRUD.md). The rule's targets travel inside the create
 * body, so they need no endpoint of their own. SVC-API-INT adds API-FIN-017 (run a rule);
 * searching rules (API-FIN-015) belongs to SVC-API-SEARCH.
 *
 * <p>Deactivate (API-FIN-037) is {@code PUT /{id}/deactivate} returning 200 with the entity body,
 * per build-create-controller step 6 — not {@code DELETE}, which this project reserves for a real
 * hard delete ({@code @ResponseStatus(NO_CONTENT)} + {@code void}). ENT-FIN-013 has no hard delete,
 * so this controller declares no {@code DELETE} mapping at all. There is deliberately no
 * {@code activate} counterpart: A.6.7 forbids a single toggle endpoint but does not require an
 * activate to exist, and no requirement, screen or QR id anchors one — the same deliberate choice
 * {@code AccountController} records and that API-FIN-034 and API-FIN-035 both follow.
 *
 * <p>There is also no rule {@code update} endpoint. Its absence is recorded in srs-fin.md
 * SCR-REQ-FIN-005 §B4 as a known gap with no derivable requirement, and a correct one would first
 * have to decide what happens to the rule's existing targets — including whether RULE-FIN-003's
 * exactly-one-remainder guarantee is re-evaluated over the replacement set. That is a design
 * question, not a bolt-on.
 */
@RestController
@RequestMapping("/api/v1/fin/allocation-rules")
@RequiredArgsConstructor
@Tag(name = "FIN Allocation Rule Management",
    description = "Cost-allocation rules - قواعد توزيع التكلفة")
public class AllocationRuleController {

    private final AllocationRuleService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create allocation rule", description = "إنشاء قاعدة توزيع تكلفة")
    public ResponseEntity<ApiResponse<AllocationRuleResponse>> create(
            @Valid @RequestBody AllocationRuleCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    /**
     * API-FIN-017 — running a rule is an update-class custom action (SEC-BE.md matrix,
     * FIN_ALLOCATION_RULES / UPDATE).
     */
    @PostMapping("/{id}/run")
    @Operation(summary = "Run an allocation rule", description = "تشغيل قاعدة توزيع تكلفة")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> run(@PathVariable Long id) {
        return operationCode.craftResponse(service.run(id));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate allocation rule",
        description = "إلغاء تفعيل قاعدة توزيع تكلفة")
    public ResponseEntity<ApiResponse<AllocationRuleResponse>> deactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search allocation rules", description = "بحث في قواعد التوزيع")
    public ResponseEntity<ApiResponse<Page<AllocationRuleResponse>>> search(
            @Valid @RequestBody AllocationRuleSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }
}
