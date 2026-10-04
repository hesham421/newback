package com.erp.fin.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.RecurringTemplateCreateRequest;
import com.erp.fin.dto.RecurringTemplateResponse;
import com.erp.fin.dto.RecurringTemplateSearchRequest;
import com.erp.fin.service.RecurringTemplateService;
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
 * Thin controller for API-FIN-013 (SVC-API-CRUD.md). The template's lines travel inside the create
 * body, so they need no endpoint of their own. SVC-API-INT adds API-FIN-014 (run a template);
 * searching templates (API-FIN-012) belongs to SVC-API-SEARCH.
 *
 * <p>Deactivate (API-FIN-036) is {@code PUT /{id}/deactivate} returning 200 with the entity body,
 * per build-create-controller step 6 — not {@code DELETE}, which this project reserves for a real
 * hard delete ({@code @ResponseStatus(NO_CONTENT)} + {@code void}). ENT-FIN-011 has no hard delete,
 * so this controller declares no {@code DELETE} mapping at all. There is deliberately no
 * {@code activate} counterpart: A.6.7 forbids a single toggle endpoint but does not require an
 * activate to exist, and no requirement, screen or QR id anchors one — the same deliberate choice
 * {@code AccountController} records and that API-FIN-034 and API-FIN-035 both follow.
 *
 * <p>There is also no template {@code update} endpoint. Its absence is recorded in srs-fin.md
 * SCR-REQ-FIN-004 §B4 as a known gap with no derivable requirement, and a correct one would first
 * have to decide what happens to the template's existing child lines — a design question, not a
 * bolt-on.
 */
@RestController
@RequestMapping("/api/v1/fin/recurring-templates")
@RequiredArgsConstructor
@Tag(name = "FIN Recurring Template Management",
    description = "Recurring journal templates - قوالب القيود المتكررة")
public class RecurringTemplateController {

    private final RecurringTemplateService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create recurring template", description = "إنشاء قالب قيد متكرر")
    public ResponseEntity<ApiResponse<RecurringTemplateResponse>> create(
            @Valid @RequestBody RecurringTemplateCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    /**
     * API-FIN-014 — running a template is an update-class custom action (SEC-BE.md matrix,
     * FIN_RECURRING_TEMPLATES / UPDATE). The same endpoint serves an internal scheduler.
     */
    @PostMapping("/{id}/run")
    @Operation(summary = "Run a recurring template", description = "تشغيل قالب قيد متكرر")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> run(@PathVariable Long id) {
        return operationCode.craftResponse(service.run(id));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate recurring template",
        description = "إلغاء تفعيل قالب قيد متكرر")
    public ResponseEntity<ApiResponse<RecurringTemplateResponse>> deactivate(
            @PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search recurring templates", description = "بحث في القوالب المتكررة")
    public ResponseEntity<ApiResponse<Page<RecurringTemplateResponse>>> search(
            @Valid @RequestBody RecurringTemplateSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }
}
