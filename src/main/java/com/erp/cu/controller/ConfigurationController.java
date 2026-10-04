package com.erp.cu.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.cu.dto.ConfigurationCreateRequest;
import com.erp.cu.dto.ConfigurationResponse;
import com.erp.cu.dto.ConfigurationSearchRequest;
import com.erp.cu.dto.ConfigurationUpdateRequest;
import com.erp.cu.service.ConfigurationService;
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
 * Thin REST controller for the 5-API registry (API-CU-001..005). Base path taken literally from
 * SVC-API.md / the Plan Index (/api/v1/common/configurations) rather than the
 * build-create-controller default /api/&lt;module&gt;/&lt;entity-url&gt; template — an
 * already-confirmed plan-level path decision, not a fresh deviation made in this sub.
 *
 * <p>No activate endpoint and no GET /{key}/usage — see the SVC-API sub's report for the
 * reasoned, documented deviations (no reactivation API; AppConfiguration has no LOVs, feeds no
 * dropdown, and can never be referenced by another entity).
 */
@RestController
@RequestMapping("/api/v1/common/configurations")
@RequiredArgsConstructor
@Tag(name = "Configuration Management", description = "Platform runtime configuration store - مخزن إعدادات المنصة")
public class ConfigurationController {

    private final ConfigurationService configurationService;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create configuration", description = "إنشاء إعداد جديد")
    public ResponseEntity<ApiResponse<ConfigurationResponse>> create(
            @Valid @RequestBody ConfigurationCreateRequest request) {
        return operationCode.craftResponse(configurationService.create(request));
    }

    @PostMapping("/search")
    @Operation(summary = "Search configurations", description = "البحث في الإعدادات")
    public ResponseEntity<ApiResponse<Page<ConfigurationResponse>>> search(
            @Valid @RequestBody ConfigurationSearchRequest searchRequest) {
        return operationCode.craftResponse(configurationService.search(searchRequest));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update configuration", description = "تحديث إعداد حسب المفتاح")
    public ResponseEntity<ApiResponse<ConfigurationResponse>> update(
            @PathVariable String key,
            @Valid @RequestBody ConfigurationUpdateRequest request) {
        return operationCode.craftResponse(configurationService.update(key, request));
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get configuration by key", description = "جلب إعداد حسب المفتاح")
    public ResponseEntity<ApiResponse<ConfigurationResponse>> getByKey(@PathVariable String key) {
        return operationCode.craftResponse(configurationService.getByKey(key));
    }

    /**
     * API-CU-004 — DELETE verb IS the deactivate action for this entity (soft deactivate, no
     * hard delete anywhere in its lifecycle). Mirrors build-create-controller's delete() shape
     * exactly (204, void, no wrapped response) while calling the service's deactivate().
     */
    @DeleteMapping("/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate configuration", description = "إلغاء تفعيل إعداد حسب المفتاح")
    public void deactivate(@PathVariable String key) {
        configurationService.deactivate(key);
    }
}
