package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The generic confirmation payload of API-SEC-003 and API-SEC-004. Carries both languages because
 * CORE.md "Languages" forbids a single-language value in any response; it never varies with whether
 * the email or token existed (API-SEC-003 Response line).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic confirmation - تأكيد عام")
public class ConfirmationResponse {

    @Schema(description = "Confirmation message (Arabic) - رسالة التأكيد بالعربية",
        example = "إذا كان البريد الإلكتروني مسجلًا فسيتم إرسال رابط إعادة التعيين")
    private String messageAr;

    @Schema(description = "Confirmation message (English) - رسالة التأكيد بالإنجليزية",
        example = "If that email is registered, a reset link has been sent")
    private String messageEn;
}
