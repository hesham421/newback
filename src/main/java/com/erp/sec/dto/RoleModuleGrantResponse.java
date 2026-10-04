package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-SEC-007 response — DBF-SEC-060..064. {@code grantedBy} / {@code grantedAt} are this table's
 * system-set equivalents of the audit columns it does not carry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role module grant - منح وحدة لدور")
public class RoleModuleGrantResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long roleModuleGrantPk;

    @Schema(description = "Role identifier - معرّف الدور", example = "1")
    private Long roleId;

    @Schema(description = "Registered module identifier - معرّف الوحدة المسجّلة", example = "1")
    private Long moduleId;

    @Schema(description = "Granted by - مُنح بواسطة", example = "admin")
    private String grantedBy;

    @Schema(description = "Granted timestamp - تاريخ المنح")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant grantedAt;
}
