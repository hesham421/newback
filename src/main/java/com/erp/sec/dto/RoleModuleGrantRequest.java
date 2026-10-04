package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** API-SEC-014 body (ENT-SEC-007) — the registered module to grant to the path role. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Grant a module to a role - منح وحدة لدور")
public class RoleModuleGrantRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Registered module identifier - معرّف الوحدة المسجّلة", example = "1")
    private Long moduleId;
}
