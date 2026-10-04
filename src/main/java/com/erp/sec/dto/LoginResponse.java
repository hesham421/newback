package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-001 success payload. Carries the signed access token only — never the session's
 * {@code tokenRef}, the password or any hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Issued access token - رمز الوصول الصادر")
public class LoginResponse {

    @Schema(description = "Signed JWT access token - رمز الوصول الموقَّع", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Token type - نوع الرمز", example = "Bearer")
    private String tokenType;

    @Schema(description = "Lifetime in seconds - مدة الصلاحية بالثواني", example = "3600")
    private Long expiresIn;
}
