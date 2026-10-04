package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.SignupDecisionRequest;
import com.erp.sec.dto.SignupRequestResponse;
import com.erp.sec.dto.SignupRequestSearchRequest;
import com.erp.sec.service.SignupRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller for API-SEC-011 (SCR-REQ-SEC-004, "Pending sign-ups" tab). */
@RestController
@RequestMapping("/api/v1/sec/signup-requests")
@RequiredArgsConstructor
@Tag(name = "Sign-up Requests", description = "Sign-up request review - مراجعة طلبات التسجيل")
public class SignupRequestController {

    private final SignupRequestService service;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "Search sign-up requests",
        description = "بحث طلبات التسجيل — رشّح على statusCode للحصول على الطلبات المعلّقة")
    public ResponseEntity<ApiResponse<Page<SignupRequestResponse>>> search(
            @Valid @RequestBody SignupRequestSearchRequest searchRequest) {
        return operationCode.craftResponse(service.search(searchRequest));
    }

    /** The payload is the created user on APPROVE and the updated sign-up request on REJECT. */
    @PatchMapping("/{id}")
    @Operation(summary = "Approve or reject a sign-up request",
        description = "الموافقة على طلب تسجيل أو رفضه")
    public ResponseEntity<ApiResponse<Object>> decide(
            @PathVariable Long id,
            @Valid @RequestBody SignupDecisionRequest request) {
        return operationCode.craftResponse(service.decide(id, request));
    }
}
