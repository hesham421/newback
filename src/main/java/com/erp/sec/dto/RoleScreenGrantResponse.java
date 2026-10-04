package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** ENT-SEC-008 response — DBF-SEC-065..069. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role screen grant - منح شاشة لدور")
public class RoleScreenGrantResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long roleScreenGrantPk;

    @Schema(description = "Role identifier - معرّف الدور", example = "1")
    private Long roleId;

    @Schema(description = "Registered screen identifier - معرّف الشاشة المسجّلة", example = "1")
    private Long screenId;

    @Schema(description = "Granted by - مُنح بواسطة", example = "admin")
    private String grantedBy;

    @Schema(description = "Granted timestamp - تاريخ المنح")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant grantedAt;
}
