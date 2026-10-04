package com.erp.sec.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.sec.dto.DevPasswordResetTokenResponse;
import com.erp.sec.dto.PasswordResetRequest;
import com.erp.sec.service.DevPasswordResetSupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-profile test fixture, not part of the SEC contract — it carries no API-SEC id and must never
 * acquire one. {@code @Profile("dev")} means the bean, and therefore the path, does not exist in
 * any other profile; the caller must still be authenticated, since SecurityConfig's permitAll list
 * names only the four pre-authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/sec/dev")
@Profile("dev")
@RequiredArgsConstructor
@Tag(name = "Dev support", description = "Dev-only test fixtures - أدوات اختبار لبيئة التطوير فقط")
public class DevPasswordResetController {

    private final DevPasswordResetSupportService service;
    private final OperationCode operationCode;

    @PostMapping("/password-reset-token")
    @Operation(summary = "Issue a raw password-reset token (Dev only)",
        description = "إصدار رمز إعادة تعيين كلمة المرور للاختبار — بيئة التطوير فقط")
    public ResponseEntity<ApiResponse<DevPasswordResetTokenResponse>> issueToken(
            @Valid @RequestBody PasswordResetRequest request) {
        return operationCode.craftResponse(service.issueToken(request.getEmail()));
    }
}
