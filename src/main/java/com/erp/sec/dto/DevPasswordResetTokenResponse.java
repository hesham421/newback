package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dev-profile test fixture: the raw password-reset token API-SEC-004 expects, plus its expiry. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Raw password-reset token (Dev only) - رمز إعادة تعيين كلمة المرور (بيئة التطوير فقط)")
public class DevPasswordResetTokenResponse {

    @Schema(description = "Raw token to submit to API-SEC-004 - الرمز الخام",
        example = "4f9b1c2e-7a10-4f1d-9c3e-0b5d8a6f2e11")
    private String token;

    @Schema(description = "Token expiry instant - وقت انتهاء صلاحية الرمز",
        example = "2026-09-18T16:06:19Z")
    private Instant expiresAt;
}
