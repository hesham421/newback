package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The nested role view API-SEC-008 returns inside {@code UserResponse.roles}
 * (DATA-DOM-TRANSACTIONAL.md ENT-SEC-003: {@code {roleId, code, nameAr, nameEn}}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role assigned to a user - دور مُسند إلى مستخدم")
public class RoleSummaryResponse {

    @Schema(description = "Role identifier - معرّف الدور", example = "1")
    private Long roleId;

    @Schema(description = "Role code - رمز الدور", example = "SEC_ADMIN")
    private String code;

    @Schema(description = "Role name (Arabic) - اسم الدور بالعربية", example = "مدير الأمان")
    private String nameAr;

    @Schema(description = "Role name (English) - اسم الدور بالإنجليزية", example = "Security administrator")
    private String nameEn;
}
