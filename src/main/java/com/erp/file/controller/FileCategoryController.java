package com.erp.file.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.file.dto.CategoryCreateRequest;
import com.erp.file.dto.CategoryResponse;
import com.erp.file.dto.CategorySearchRequest;
import com.erp.file.dto.CategoryUpdateRequest;
import com.erp.file.service.FileCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-FILE-007 (Categories CRUD, SCR-FILE-001). Search is POST /search per
 * build-create-controller A.6.6 (the shared search contract is POST-body-only). DELETE maps onto the
 * service's soft deactivate (204). Pure delegation — zero business logic.
 */
@RestController
@RequestMapping("/api/v1/files/categories")
@RequiredArgsConstructor
@Tag(name = "File Categories", description = "File category management - إدارة فئات الملفات")
public class FileCategoryController {

    private final FileCategoryService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create file category", description = "إنشاء فئة ملف")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @PostMapping("/search")
    @Operation(summary = "Search file categories", description = "البحث في فئات الملفات")
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> search(
            @Valid @RequestBody CategorySearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file category by ID", description = "جلب فئة ملف حسب المعرف")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return operationCode.craftResponse(service.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update file category", description = "تحديث فئة ملف حسب المعرف")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    /**
     * API-FILE-007 DELETE — soft deactivate (isActiveFl = false). 204, void, no wrapped response.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate file category", description = "إلغاء تفعيل فئة ملف حسب المعرف")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
