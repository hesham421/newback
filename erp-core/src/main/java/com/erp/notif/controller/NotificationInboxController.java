package com.erp.notif.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.notif.dto.InboxItemResponse;
import com.erp.notif.service.NotificationInboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for the caller's in-app inbox (erp-core step 08, IN_APP channel) — both realms.
 * Pure delegation to {@link NotificationInboxService}.
 */
@RestController
@RequestMapping("/api/v1/notif/inbox")
@RequiredArgsConstructor
@Tag(name = "Notification Inbox", description = "The caller's in-app notifications - صندوق الإشعارات داخل التطبيق")
public class NotificationInboxController {

    private final NotificationInboxService service;
    private final OperationCode operationCode;

    @GetMapping
    @Operation(summary = "List my in-app notifications", description = "عرض إشعاراتي داخل التطبيق (الأحدث أولًا)")
    public ResponseEntity<ApiResponse<Page<InboxItemResponse>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return operationCode.craftResponse(service.list(unreadOnly, page, size));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark an in-app notification read", description = "تعليم الإشعار كمقروء")
    public ResponseEntity<ApiResponse<InboxItemResponse>> markRead(@PathVariable Long id) {
        return operationCode.craftResponse(service.markRead(id));
    }
}
