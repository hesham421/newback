package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** API-SEC-022 active-sessions widget — source screen SEC_SESSIONS (QR-SEC-022). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Active sessions widget - عنصر الجلسات النشطة")
public class ActiveSessionsCountResponse {

    @Schema(description = "Staff (STAFF realm) sessions with terminatedAt IS NULL - عدد جلسات الموظفين غير المنتهية", example = "14")
    private Long count;
}
