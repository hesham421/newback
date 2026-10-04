package com.erp.mdl.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.mdl.dto.LookupTypeByOwnerSearchRequest;
import com.erp.mdl.dto.LookupTypeCreateRequest;
import com.erp.mdl.dto.LookupTypeResponse;
import com.erp.mdl.dto.LookupTypeSearchRequest;
import com.erp.mdl.dto.LookupTypeUpdateRequest;
import com.erp.mdl.dto.OwnerGroupResponse;
import com.erp.mdl.service.LookupTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-MDL-002/003/004 (SVC-API-CRUD.md) plus API-MDL-001/010
 * (SVC-API-SEARCH.md).
 *
 * <p>Deviation: API-MDL-004 (deactivate) returns 200 with the crafted
 * {@code ApiResponse<LookupTypeResponse>} body, NOT {@code build-create-controller}'s generic
 * delete template ({@code @ResponseStatus(NO_CONTENT)} + {@code void}). SVC-API-CRUD.md's own
 * response line is explicit: "200 · confirmation {lookupTypePk, isActiveFl: false}" — a
 * soft-deactivate with a body confirmation, not a hard delete. Named, spec-driven deviation.
 *
 * <p>{@code search}/{@code browseByOwner} are {@code POST} + {@code @RequestBody}, the platform's
 * generic {@code build-create-controller} template — the module's earlier GET + query-param
 * deviation was reversed so that both criteria-driven reads go through the same dynamic
 * JPA-{@code Specification} filtering (via {@code SpecBuilder}) with criteria carried in the
 * request body, matching every other module's search endpoints (see the SEC GET-to-POST reversal
 * this mirrors, governance/project-artifacts/sec-implementation-notes.md §8).
 */
@RestController
@RequestMapping("/api/v1/mdl/lookup-types")
@RequiredArgsConstructor
@Tag(name = "Lookup Type Management", description = "Master data lookup types - أنواع بيانات اللوكب الرئيسية")
public class LookupTypeController {

    private final LookupTypeService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create lookup type", description = "إنشاء نوع لوكب جديد")
    public ResponseEntity<ApiResponse<LookupTypeResponse>> create(
            @Valid @RequestBody LookupTypeCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update lookup type", description = "تعديل أسماء نوع اللوكب")
    public ResponseEntity<ApiResponse<LookupTypeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody LookupTypeUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate lookup type", description = "إلغاء تفعيل نوع اللوكب")
    public ResponseEntity<ApiResponse<LookupTypeResponse>> deactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search lookup types", description = "بحث في أنواع اللوكب")
    public ResponseEntity<ApiResponse<Page<LookupTypeResponse>>> search(
            @Valid @RequestBody LookupTypeSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @PostMapping("/by-owner/search")
    @Operation(summary = "Browse lookup type registry by owner", description = "استعراض سجل أنواع اللوكب حسب المالك")
    public ResponseEntity<ApiResponse<List<OwnerGroupResponse>>> browseByOwner(
            @Valid @RequestBody LookupTypeByOwnerSearchRequest searchRequest) {
        return operationCode.craftResponse(service.browseByOwner(searchRequest));
    }
}
