package com.erp.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FILE-002 response — the freshly issued single-use AES/GCM download token (RULE-FILE-003) and
 * its expiry instant (~10m TTL). The token is opaque to the client and replayed verbatim on
 * API-FILE-003.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File access token - رمز الوصول للملف")
public class AccessTokenResponse {

    @Schema(description = "Opaque single-use download token - رمز التنزيل", example = "q1w2e3...")
    private String accessToken;

    @Schema(description = "Token expiry instant - وقت انتهاء الرمز")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant expiresAt;
}
