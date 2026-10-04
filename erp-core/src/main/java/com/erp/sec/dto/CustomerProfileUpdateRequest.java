package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * erp-core step 06 — {@code PATCH /api/v1/customers/me}: partial update of the caller's own profile; a
 * {@code null} field is left unchanged. E-mail (the login), password, status and realm are not
 * editable here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update the customer's own profile - تعديل الملف الشخصي للعميل")
public class CustomerProfileUpdateRequest {

    @Size(min = 1, max = 200, message = "{validation.size}")
    @Schema(description = "Full name (Arabic) - الاسم الكامل بالعربية", example = "جين دو")
    private String fullNameAr;

    @Size(min = 1, max = 200, message = "{validation.size}")
    @Schema(description = "Full name (English) - الاسم الكامل بالإنجليزية", example = "Jane Doe")
    private String fullNameEn;
}
