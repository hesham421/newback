package com.erp.notif.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.notif.dto.TemplateCreateRequest;
import com.erp.notif.dto.TemplateResponse;
import com.erp.notif.dto.TemplateSearchRequest;
import com.erp.notif.dto.TemplateUpdateRequest;
import com.erp.notif.service.NotificationTemplateService;
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
 * Thin controller for API-NOTIF-004 (Templates CRUD, SCR-NOTIF-001). Search is POST /search per
 * build-create-controller A.6.6 (the shared search contract is POST-body-only). DELETE maps onto the
 * service's soft deactivate (204). Pure delegation — zero business logic.
 */
@RestController
@RequestMapping("/api/v1/notifications/templates")
@RequiredArgsConstructor
@Tag(name = "Notification Templates", description = "Notification template management - إدارة قوالب الإشعارات")
public class NotificationTemplateController {

    private final NotificationTemplateService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create notification template", description = "إنشاء قالب إشعار")
    public ResponseEntity<ApiResponse<TemplateResponse>> create(
            @Valid @RequestBody TemplateCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @PostMapping("/search")
    @Operation(summary = "Search notification templates", description = "البحث في قوالب الإشعارات")
    public ResponseEntity<ApiResponse<Page<TemplateResponse>>> search(
            @Valid @RequestBody TemplateSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification template by ID", description = "جلب قالب إشعار حسب المعرف")
    public ResponseEntity<ApiResponse<TemplateResponse>> getById(@PathVariable Long id) {
        return operationCode.craftResponse(service.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update notification template", description = "تحديث قالب إشعار حسب المعرف")
    public ResponseEntity<ApiResponse<TemplateResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TemplateUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    /**
     * API-NOTIF-004 DELETE — soft deactivate (isActiveFl = false). 204, void, no wrapped response.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate notification template", description = "إلغاء تفعيل قالب إشعار حسب المعرف")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
