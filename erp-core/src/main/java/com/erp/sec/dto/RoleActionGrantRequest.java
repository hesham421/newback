package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** API-SEC-017 body (ENT-SEC-009) — the registered action to grant to the path role. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Grant an action to a role - منح إجراء لدور")
public class RoleActionGrantRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Registered action identifier - معرّف الإجراء المسجّل", example = "1")
    private Long actionId;
}
