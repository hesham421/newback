package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-013 create-role body (ENT-SEC-002). {@code code} is the natural key, immutable after
 * create. The description pair is optional but both-or-neither (API-SEC-013 Localization line).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a role - إنشاء دور")
public class RoleCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Unique role code - رمز الدور الفريد", example = "SEC_ADMIN")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Role name (Arabic) - اسم الدور بالعربية", example = "مدير الأمان")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Role name (English) - اسم الدور بالإنجليزية", example = "Security administrator")
    private String nameEn;

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Description (Arabic) - الوصف بالعربية", example = "إدارة المستخدمين والأدوار")
    private String descriptionAr;

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Description (English) - الوصف بالإنجليزية", example = "Manages users and roles")
    private String descriptionEn;

    /** Structural bilingual pairing check — not a RULE-SEC-* decision, so it stays on the DTO. */
    @AssertTrue(message = "{validation.required}")
    @Schema(hidden = true)
    public boolean isDescriptionBilingual() {
        boolean arPresent = descriptionAr != null && !descriptionAr.isBlank();
        boolean enPresent = descriptionEn != null && !descriptionEn.isBlank();
        return arPresent == enPresent;
    }
}
