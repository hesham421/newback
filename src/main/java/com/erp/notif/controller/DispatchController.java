package com.erp.notif.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.notif.dto.DispatchRequest;
import com.erp.notif.dto.DispatchResponse;
import com.erp.notif.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-NOTIF-001 — notification dispatch (fan-out). Delegates to
 * {@link DispatchService}; the response carries the created NOTIF_LOG ids. Pure delegation.
 *
 * <p>Note: the plan specified HTTP 202. The shared response helper derives the status from the
 * service's {@code Status}, whose taxonomy expresses only 200/201 (common is out of scope to modify),
 * so a successful dispatch returns 200 (recorded as an api_doc_gap).
 */
@RestController
@RequestMapping("/api/v1/notifications/dispatch")
@RequiredArgsConstructor
@Tag(name = "Notification Dispatch", description = "Notification dispatch (fan-out) - إرسال الإشعارات")
public class DispatchController {

    private final DispatchService service;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Dispatch a notification", description = "إرسال إشعار (تفرّع لكل قناة)")
    public ResponseEntity<ApiResponse<DispatchResponse>> dispatch(
            @Valid @RequestBody DispatchRequest request) {
        return operationCode.craftResponse(service.dispatch(request));
    }
}
