package com.erp.fin.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.fin.dto.EventEntryBuildRequest;
import com.erp.fin.dto.JournalEntryCreateRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalEntrySearchRequest;
import com.erp.fin.service.EventEntryService;
import com.erp.fin.service.JournalEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-FIN-019 (SVC-API-CRUD.md). The entry's lines and their dimension tags
 * travel inside the create body, so they need no endpoint of their own.
 *
 * <p>SVC-API-INT adds API-FIN-020 (build from an event) and API-FIN-021 (reverse). Reading
 * (API-FIN-022) and searching (API-FIN-018) belong to SVC-API-SEARCH.
 *
 * <p>RULE-FIN-016 — no {@code PUT} and no {@code DELETE} mapping exists on this resource, and none
 * may be added: a posted entry is locked, and correction is only through a reversal
 * (API-FIN-021).
 */
@RestController
@RequestMapping("/api/v1/fin/journal-entries")
@RequiredArgsConstructor
@Tag(name = "FIN Journal Entry Management", description = "Journal entries - قيود اليومية")
public class JournalEntryController {

    private final JournalEntryService service;
    private final EventEntryService eventEntryService;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create manual journal entry",
        description = "إنشاء وترحيل قيد يومية يدوي")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> createManual(
            @Valid @RequestBody JournalEntryCreateRequest request) {
        return operationCode.craftResponse(service.createManual(request));
    }

    /**
     * API-FIN-020 — a system-to-system call from the Event consumer's own service principal,
     * gated by the same interceptor as any authenticated caller. It delegates to
     * {@code EventEntryService}, the service SVC-API-INT.md's own {@code Layers:} line names, while
     * keeping the endpoint on this resource where the plan puts it.
     */
    @PostMapping("/from-event")
    @Operation(summary = "Build and post a journal entry from an accounting event",
        description = "بناء وترحيل قيد يومية من حدث محاسبي. The consumer branches on the full error "
            + "code, never on the HTTP status alone. FIN-409-DUPLICATE-EVENT: already posted, acknowledge "
            + "the message. FIN-409-PERIOD-NOT-OPEN: retry later, the rejection clears once the target "
            + "period is opened (only a SOFT_CLOSE period can return to OPEN, so a retry policy needs an "
            + "upper bound). Every other code listed below: needs a fix, mark the message failed "
            + "(FIN-422-UNMAPPED-VALUE add a mapping, FIN-422-MISSING-BUSINESS-FIELD fix the producer, "
            + "FIN-422-UNRESOLVED-DIMENSION-VALUE define the dimension value, FIN-422-NO-DEFINED-PERIOD "
            + "define the calendar).")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> buildFromEvent(
            @Valid @RequestBody EventEntryBuildRequest request) {
        return operationCode.craftResponse(eventEntryService.build(request));
    }

    /** API-FIN-021 — the custom Reverse action, gated by PERM_FIN_JOURNAL_ENTRIES_REVERSE. */
    @PostMapping("/{id}/reverse")
    @Operation(summary = "Reverse a posted journal entry",
        description = "عكس قيد يومية مُرحَّل")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> reverse(@PathVariable Long id) {
        return operationCode.craftResponse(service.reverse(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search journal entries", description = "بحث في قيود اليومية")
    public ResponseEntity<ApiResponse<Page<JournalEntryResponse>>> search(
            @Valid @RequestBody JournalEntrySearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read a journal entry with its lines",
        description = "عرض قيد يومية مع سطوره وأبعاده")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> read(@PathVariable Long id) {
        return operationCode.craftResponse(service.read(id));
    }
}
