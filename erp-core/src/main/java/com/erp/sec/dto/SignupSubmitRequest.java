package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-002 submit body (ENT-SEC-013 DTO MEMBERSHIP). {@code statusCode} is absent — a submitted
 * request always starts PENDING — and no credential is carried (REQ-SEC-003).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Submit a sign-up request - تقديم طلب تسجيل")
public class SignupSubmitRequest {

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.invalid}")
    @Size(max = 255, message = "{validation.size}")
    @Schema(description = "Email address - البريد الإلكتروني", example = "new@example.com")
    private String email;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Full name (Arabic) - الاسم الكامل بالعربية", example = "أحمد علي")
    private String fullNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Full name (English) - الاسم الكامل بالإنجليزية", example = "Ahmed Ali")
    private String fullNameEn;
}
