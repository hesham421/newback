package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.ModuleGrantRevokeResponse;
import com.erp.sec.dto.RoleActionGrantRequest;
import com.erp.sec.dto.RoleActionGrantResponse;
import com.erp.sec.dto.RoleGrantTreeResponse;
import com.erp.sec.dto.RoleModuleGrantRequest;
import com.erp.sec.dto.RoleModuleGrantResponse;
import com.erp.sec.dto.RoleScreenGrantRequest;
import com.erp.sec.dto.RoleScreenGrantResponse;
import com.erp.sec.service.RoleGrantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for the 3-level grant editor — API-SEC-014/015/016/017 (SCR-REQ-SEC-005).
 * Revoking a module answers 200 with the cascade counts RULE-SEC-003 produced, so it is not the
 * generic 204 delete.
 */
@RestController
@RequestMapping("/api/v1/sec/roles")
@RequiredArgsConstructor
@Tag(name = "Role Grants", description = "Role module/screen/action grants - منح الوحدات والشاشات والإجراءات للأدوار")
public class RoleGrantController {

    private final RoleGrantService service;
    private final OperationCode operationCode;

    /**
     * The grant editor's read: what this role holds today, nested so the tree can be diffed
     * against the registry and a module revoke can name what its cascade will remove.
     */
    @GetMapping("/{id}/grants")
    @Operation(summary = "Get the grants a role holds",
        description = "عرض المنح التي يحملها الدور — وحدات وشاشات وإجراءات")
    public ResponseEntity<ApiResponse<RoleGrantTreeResponse>> grants(@PathVariable Long id) {
        return operationCode.craftResponse(service.grantsOf(id));
    }

    @PostMapping("/{id}/modules")
    @Operation(summary = "Grant module to role", description = "منح وحدة لدور")
    public ResponseEntity<ApiResponse<RoleModuleGrantResponse>> grantModule(
            @PathVariable Long id,
            @Valid @RequestBody RoleModuleGrantRequest request) {
        return operationCode.craftResponse(service.grantModule(id, request));
    }

    @DeleteMapping("/{id}/modules/{moduleId}")
    @Operation(summary = "Revoke module grant", description = "سحب منح وحدة مع منحها المتفرعة")
    public ResponseEntity<ApiResponse<ModuleGrantRevokeResponse>> revokeModule(
            @PathVariable Long id,
            @PathVariable Long moduleId) {
        return operationCode.craftResponse(service.revokeModule(id, moduleId));
    }

    @PostMapping("/{id}/screens")
    @Operation(summary = "Grant screen to role", description = "منح شاشة لدور")
    public ResponseEntity<ApiResponse<RoleScreenGrantResponse>> grantScreen(
            @PathVariable Long id,
            @Valid @RequestBody RoleScreenGrantRequest request) {
        return operationCode.craftResponse(service.grantScreen(id, request));
    }

    @PostMapping("/{id}/actions")
    @Operation(summary = "Grant action to role", description = "منح إجراء لدور")
    public ResponseEntity<ApiResponse<RoleActionGrantResponse>> grantAction(
            @PathVariable Long id,
            @Valid @RequestBody RoleActionGrantRequest request) {
        return operationCode.craftResponse(service.grantAction(id, request));
    }
}
