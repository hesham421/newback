package com.erp.notif.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.notif.dto.ChannelCreateRequest;
import com.erp.notif.dto.ChannelResponse;
import com.erp.notif.dto.ChannelSearchRequest;
import com.erp.notif.dto.ChannelUpdateRequest;
import com.erp.notif.service.NotificationChannelConfigService;
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
 * Thin controller for API-NOTIF-005 (Channels CRUD / enable-disable, SCR-NOTIF-002). Search is POST
 * /search per build-create-controller A.6.6. DELETE maps onto the service's soft disable (204) —
 * the channel entity carries IS_ENABLED_FL only (no IS_ACTIVE_FL). Pure delegation.
 */
@RestController
@RequestMapping("/api/v1/notifications/channels")
@RequiredArgsConstructor
@Tag(name = "Notification Channels", description = "Notification channel config management - إدارة تهيئة قنوات الإشعارات")
public class NotificationChannelController {

    private final NotificationChannelConfigService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create notification channel config", description = "إنشاء تهيئة قناة إشعار")
    public ResponseEntity<ApiResponse<ChannelResponse>> create(
            @Valid @RequestBody ChannelCreateRequest request) {
        return operationCode.craftResponse(service.create(request));
    }

    @PostMapping("/search")
    @Operation(summary = "Search notification channel configs", description = "البحث في تهيئة القنوات")
    public ResponseEntity<ApiResponse<Page<ChannelResponse>>> search(
            @Valid @RequestBody ChannelSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification channel config by ID", description = "جلب تهيئة قناة إشعار حسب المعرف")
    public ResponseEntity<ApiResponse<ChannelResponse>> getById(@PathVariable Long id) {
        return operationCode.craftResponse(service.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update notification channel config", description = "تحديث تهيئة قناة إشعار حسب المعرف")
    public ResponseEntity<ApiResponse<ChannelResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ChannelUpdateRequest request) {
        return operationCode.craftResponse(service.update(id, request));
    }

    /**
     * API-NOTIF-005 DELETE — soft disable (isEnabledFl = false). 204, void, no wrapped response.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disable notification channel config", description = "تعطيل تهيئة قناة إشعار حسب المعرف")
    public void disable(@PathVariable Long id) {
        service.disable(id);
    }
}
