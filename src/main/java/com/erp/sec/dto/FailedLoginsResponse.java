package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** API-SEC-022 failed-logins-24h widget — source screen SEC_AUDIT_LOG (QR-SEC-022). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Failed logins in the last 24 hours - عنصر محاولات الدخول الفاشلة خلال ٢٤ ساعة")
public class FailedLoginsResponse {

    @Schema(description = "LOGIN_FAILED entries in the last 24 hours - عدد محاولات الدخول الفاشلة", example = "7")
    private Long count;
}
