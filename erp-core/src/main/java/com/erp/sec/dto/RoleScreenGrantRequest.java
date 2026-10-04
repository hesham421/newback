package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** API-SEC-016 body (ENT-SEC-008) — the registered screen to grant to the path role. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Grant a screen to a role - منح شاشة لدور")
public class RoleScreenGrantRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Registered screen identifier - معرّف الشاشة المسجّلة", example = "1")
    private Long screenId;
}
