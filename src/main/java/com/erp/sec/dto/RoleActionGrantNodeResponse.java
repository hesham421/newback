package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Leaf of the role grant tree — one ENT-SEC-009 action grant the role holds. Only held actions
 * appear, so there is no {@code granted} flag: presence is the grant. The registry action the
 * client must diff against is API-SEC-021's tree.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Action grant held by a role - إجراء ممنوح للدور")
public class RoleActionGrantNodeResponse {

    @Schema(description = "Action registry identifier - معرف الإجراء", example = "1")
    private Long actionRegPk;

    @Schema(description = "Action code - رمز الإجراء", example = "VIEW")
    private String actionCode;

    @Schema(description = "Derived permission code - رمز الصلاحية", example = "PERM_SEC_USERS_VIEW")
    private String permissionCode;

    @Schema(description = "Action name (Arabic) - اسم الإجراء بالعربية", example = "عرض")
    private String nameAr;

    @Schema(description = "Action name (English) - اسم الإجراء بالإنجليزية", example = "View")
    private String nameEn;

    @Schema(description = "Granted timestamp - تاريخ المنح")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant grantedAt;
}
