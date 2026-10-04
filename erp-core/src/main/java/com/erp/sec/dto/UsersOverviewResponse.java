package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** API-SEC-022 users-overview widget — source screen SEC_USERS (QR-SEC-022). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Users overview widget - عنصر نظرة عامة على المستخدمين")
public class UsersOverviewResponse {

    @Schema(description = "Total users - إجمالي المستخدمين", example = "120")
    private Long total;

    @Schema(description = "Users with statusCode ACTIVE - المستخدمون النشطون", example = "100")
    private Long active;

    @Schema(description = "Users with statusCode DISABLED - المستخدمون المعطَّلون", example = "20")
    private Long disabled;

    @Schema(description = "Sign-up requests still PENDING - طلبات التسجيل المعلّقة", example = "3")
    private Long pendingSignups;
}
