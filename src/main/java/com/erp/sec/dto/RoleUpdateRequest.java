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
 * Update-role body (ENT-SEC-002). {@code code} is structurally absent — it is the immutable
 * natural key, exactly as {@link RoleCreateRequest} documents, so a rename reaches the identity
 * fields only. {@code isActiveFl} is absent too: SEC declares no role activate/deactivate API.
 * The description pair keeps the create body's both-or-neither rule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a role - تحديث دور")
public class RoleUpdateRequest {

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
