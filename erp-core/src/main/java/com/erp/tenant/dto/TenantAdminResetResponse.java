package com.erp.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** tenant-maturity B (REQ-TENANT-027) — the result of an admin-reset: never a password or a hash. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of a tenant administrator's password reset - نتيجة إعادة تعيين كلمة مرور المدير")
public class TenantAdminResetResponse {

    @Schema(description = "Username whose password was reset - اسم الدخول", example = "admin")
    private String username;

    @Schema(description = "Number of the user's sessions that were terminated - عدد الجلسات المنتهية", example = "1")
    private int sessionsTerminated;
}
