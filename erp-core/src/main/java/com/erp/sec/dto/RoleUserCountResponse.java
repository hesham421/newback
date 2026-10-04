package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One {@code usersPerRole} entry of API-SEC-022's roles/permissions widget (QR-SEC-022). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Users per role - عدد المستخدمين لكل دور")
public class RoleUserCountResponse {

    @Schema(description = "Role id - معرف الدور", example = "1")
    private Long roleId;

    @Schema(description = "Role code - رمز الدور", example = "SEC_ADMIN")
    private String code;

    @Schema(description = "Role name (Arabic) - اسم الدور بالعربية", example = "مدير الأمان")
    private String nameAr;

    @Schema(description = "Role name (English) - اسم الدور بالإنجليزية", example = "Security administrator")
    private String nameEn;

    @Schema(description = "Users currently holding this role - عدد حاملي الدور", example = "4")
    private Long userCount;
}
