package com.erp.mdl.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.mdl.dto.LookupValueResponse;
import com.erp.mdl.service.LookupConsumerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-MDL-011 (SVC-API-SEARCH.md) — the consumer-facing read, called by other
 * modules' backends rather than an end-user screen. Distinct from {@code LookupTypeController}/
 * {@code LookupValueController} per the spec's own "controller → LookupConsumerController"
 * line, not folded into either CRUD controller.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Lookup Consumer API", description = "Consumer-facing lookup value reads - قراءة قيم اللوكب للمستهلكين")
public class LookupConsumerController {

    private final LookupConsumerService service;
    private final OperationCode operationCode;

    @GetMapping("/api/v1/mdl/lookups")
    @Operation(summary = "Read lookup values by type key", description = "قراءة قيم اللوكب حسب مفتاح النوع")
    public ResponseEntity<ApiResponse<List<LookupValueResponse>>> readByKey(
            @RequestParam("type") String type) {
        return operationCode.craftResponse(service.readByKey(type));
    }
}
