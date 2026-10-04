package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-022 roles/permissions widget — source screen SEC_ROLES (QR-SEC-022). No SEC artifact
 * defines "privileged", so it is read as RULE-SEC-007's distinction: a role holding a non-VIEW action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Roles and permissions summary widget - عنصر ملخص الأدوار والصلاحيات")
public class RolesPermissionsSummaryResponse {

    @Schema(description = "Total roles - إجمالي الأدوار", example = "9")
    private Long roleCount;

    @Schema(description = "Roles holding at least one non-VIEW action grant - الأدوار ذات الصلاحيات المتقدمة", example = "3")
    private Long privilegedRoleCount;

    @Schema(description = "User count per role - عدد المستخدمين لكل دور")
    private List<RoleUserCountResponse> usersPerRole;
}
