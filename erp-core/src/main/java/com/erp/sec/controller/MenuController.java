package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.ModuleMenuResponse;
import com.erp.sec.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-SEC-027 (SCR-REQ-SEC-010). The caller is resolved from the authenticated
 * session, never from a request parameter, so the endpoint takes no input at all.
 */
@RestController
@RequestMapping("/api/v1/sec/menu")
@RequiredArgsConstructor
@Tag(name = "Menu", description = "Effective navigation menu - القائمة الفعلية للتنقّل")
public class MenuController {

    private final MenuService service;
    private final OperationCode operationCode;

    @GetMapping
    @Operation(summary = "Get the effective menu", description = "عرض القائمة المبنية على المنح الفعلية للمستخدم")
    public ResponseEntity<ApiResponse<List<ModuleMenuResponse>>> effective() {
        return operationCode.craftResponse(service.effective());
    }
}
