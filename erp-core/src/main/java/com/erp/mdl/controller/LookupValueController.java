package com.erp.mdl.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.mdl.dto.LookupValueCreateRequest;
import com.erp.mdl.dto.LookupValueReorderRequest;
import com.erp.mdl.dto.LookupValueResponse;
import com.erp.mdl.dto.LookupValueSearchRequest;
import com.erp.mdl.dto.LookupValueUpdateRequest;
import com.erp.mdl.service.LookupValueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-MDL-006/007/008/009 (SVC-API-CRUD.md) plus API-MDL-005
 * (SVC-API-SEARCH.md). Path asymmetry is deliberate, taken verbatim from the spec's own endpoint
 * lines, not normalized into one shape: create and reorder nest under
 * {@code /lookup-types/{id}/...} (the parent-scoped operations), update and deactivate address
 * the value directly at the flat {@code /lookup-values/{id}}, and search sits at the flat
 * {@code /lookup-types/values/search} — the parent id travels in the body, never as a path
 * variable, same shape as FIN's {@code DimensionController.searchDimensionValues}.
 *
 * <p>Deviation: API-MDL-008 (deactivate) returns 200 with the crafted
 * {@code ApiResponse<LookupValueResponse>} body, same reasoning and same named deviation as
 * {@code LookupTypeController.deactivate} — SVC-API-CRUD.md's response line is explicit: "200 ·
 * confirmation {lookupValuePk, isActiveFl: false}".
 *
 * <p>{@code search} is {@code POST} + {@code @RequestBody}, the platform's generic
 * {@code build-create-controller} template — the module's earlier GET + query-param deviation was
 * reversed alongside {@code LookupTypeController}'s, same reasoning (dynamic
 * JPA-{@code Specification} filtering with criteria carried in the request body).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Lookup Value Management", description = "Master data lookup values - قيم بيانات اللوكب")
public class LookupValueController {

    private final LookupValueService service;
    private final OperationCode operationCode;

    @PostMapping("/api/v1/mdl/lookup-types/{id}/values")
    @Operation(summary = "Create lookup value", description = "إنشاء قيمة لوكب جديدة ضمن نوع")
    public ResponseEntity<ApiResponse<LookupValueResponse>> create(
            @PathVariable("id") Long lookupTypeId,
            @Valid @RequestBody LookupValueCreateRequest request) {
        return operationCode.craftResponse(service.create(lookupTypeId, request));
    }

    @PutMapping("/api/v1/mdl/lookup-values/{id}")
    @Operation(summary = "Update lookup value", description = "تعديل قيمة لوكب")
    public ResponseEntity<ApiResponse<LookupValueResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody LookupValueUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    @DeleteMapping("/api/v1/mdl/lookup-values/{id}")
    @Operation(summary = "Deactivate lookup value", description = "إلغاء تفعيل قيمة لوكب")
    public ResponseEntity<ApiResponse<LookupValueResponse>> deactivate(@PathVariable Long id) {
        return operationCode.craftResponse(service.deactivate(id));
    }

    @PatchMapping("/api/v1/mdl/lookup-types/{id}/values/reorder")
    @Operation(summary = "Reorder lookup values", description = "إعادة ترتيب قيم نوع اللوكب")
    public ResponseEntity<ApiResponse<List<LookupValueResponse>>> reorder(
            @PathVariable("id") Long lookupTypeId,
            @Valid @RequestBody LookupValueReorderRequest request) {
        return operationCode.craftResponse(service.reorder(lookupTypeId, request));
    }

    @PostMapping("/api/v1/mdl/lookup-types/values/search")
    @Operation(summary = "Search lookup values of a type", description = "بحث في قيم نوع لوكب")
    public ResponseEntity<ApiResponse<Page<LookupValueResponse>>> search(
            @Valid @RequestBody LookupValueSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }
}
