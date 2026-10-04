package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Everything one role currently holds across ENT-SEC-007/008/009, nested module → screen → action
 * so it can be diffed directly against the registry tree API-SEC-021 returns and against the menu
 * shape of API-SEC-027. Only held grants appear: an entry absent from this tree is a grant the
 * role does not hold, which is what lets the grant editor render a real unchecked box.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Grants held by a role - المنح التي يحملها الدور")
public class RoleGrantTreeResponse {

    @Schema(description = "Role identifier - معرف الدور", example = "1")
    private Long rolePk;

    @Schema(description = "Role code - رمز الدور", example = "SEC_ADMIN")
    private String code;

    @Schema(description = "Role name (Arabic) - اسم الدور بالعربية", example = "مدير الأمان")
    private String nameAr;

    @Schema(description = "Role name (English) - اسم الدور بالإنجليزية", example = "Security administrator")
    private String nameEn;

    @Schema(description = "Modules reached by this role's grants - الوحدات التي تصلها منح الدور")
    private List<RoleModuleGrantNodeResponse> modules;
}
