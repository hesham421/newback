package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-008 body (ENT-SEC-003) — the complete role set the user should end up holding; the
 * assignment set is replaced, not appended to. An empty list clears every assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Assign roles to a user - إسناد أدوار إلى مستخدم")
public class UserRoleAssignmentRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Role identifiers to assign - معرّفات الأدوار المطلوب إسنادها", example = "[1, 2]")
    private List<Long> roleIds;
}
