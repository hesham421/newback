package com.erp.notif.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.notif.dto.LookupOptionResponse;
import com.erp.notif.service.NotificationLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-NOTIF-006 — the NOTIF runtime LOV provider (LOV-NOTIF-001 NOTIF_CHANNEL /
 * LOV-NOTIF-002 NOTIF_STATUS). Any authenticated caller may read the code lists (gated by the
 * service's {@code isAuthenticated()}); an unknown key yields 404. Pure delegation — zero logic.
 */
@RestController
@RequestMapping("/api/v1/notifications/lookups")
@RequiredArgsConstructor
@Tag(name = "Notification Lookups", description = "Notification runtime value lists - قوائم قيم الإشعارات")
public class NotificationLookupController {

    private final NotificationLookupService service;
    private final OperationCode operationCode;

    @GetMapping("/{lookupKey}")
    @Operation(summary = "Get notification lookup values by key", description = "إرجاع قائمة قيم الإشعارات حسب المفتاح")
    public ResponseEntity<ApiResponse<List<LookupOptionResponse>>> get(@PathVariable String lookupKey) {
        return operationCode.craftResponse(service.get(lookupKey));
    }
}
