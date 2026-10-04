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
 * API-SEC-007 update-user body (ENT-SEC-001). {@code username}, {@code passwordHash},
 * {@code statusCode} and {@code isActiveFl} are structurally absent — username is immutable after
 * create, and status moves only through API-SEC-009/010/011.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a user - تحديث مستخدم")
public class UserUpdateRequest {

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.invalid}")
    @Size(max = 255, message = "{validation.size}")
    @Schema(description = "Email address - البريد الإلكتروني", example = "u2@example.com")
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
