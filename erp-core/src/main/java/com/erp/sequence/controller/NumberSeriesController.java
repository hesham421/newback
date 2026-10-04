package com.erp.sequence.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sequence.dto.NumberSeriesCreateRequest;
import com.erp.sequence.dto.NumberSeriesResponse;
import com.erp.sequence.dto.NumberSeriesSearchRequest;
import com.erp.sequence.dto.NumberSeriesUpdateRequest;
import com.erp.sequence.service.NumberSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API of the number series (erp-core step 09) — STAFF realm, {@code SEQUENCE:SERIES:MANAGE}
 * ({@code PERM_SEQUENCE_SERIES_MANAGE}; reads need the screen's VIEW gateway). No delete endpoint (issued
 * numbers must never be reissued — deactivate instead) and no usage endpoint (nothing references a series).
 */
@RestController
@RequestMapping("/api/v1/sequence/series")
@RequiredArgsConstructor
@Tag(name = "Number Series Management", description = "Document number series - سلاسل ترقيم المستندات")
public class NumberSeriesController {

    private final NumberSeriesService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create number series", description = "إنشاء سلسلة ترقيم")
    public ResponseEntity<ApiResponse<NumberSeriesResponse>> create(@Valid @RequestBody NumberSeriesCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update number series (all periods of its code)", description = "تعديل سلسلة ترقيم")
    public ResponseEntity<ApiResponse<NumberSeriesResponse>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody NumberSeriesUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get number series period by ID", description = "جلب فترة سلسلة ترقيم")
    public ResponseEntity<ApiResponse<NumberSeriesResponse>> getById(@PathVariable Long id) {
        return operationCode.craftResponse(service.getById(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search number series periods", description = "البحث في سلاسل الترقيم")
    public ResponseEntity<ApiResponse<Page<NumberSeriesResponse>>> search(
            @Valid @RequestBody NumberSeriesSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate number series", description = "تفعيل سلسلة ترقيم")
    public ResponseEntity<ApiResponse<NumberSeriesResponse>> activate(@PathVariable Long id) {
        return operationCode.craftResponse(service.activate(id));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate number series", description = "إلغاء تفعيل سلسلة ترقيم")
    public ResponseEntity<ApiResponse<NumberSeriesResponse>> deactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }
}
