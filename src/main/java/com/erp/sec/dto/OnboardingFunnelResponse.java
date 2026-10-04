package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-022 onboarding-funnel widget — source screen SEC_USERS, whose "Pending sign-ups" tab
 * owns these rows (SCR-REQ-SEC-004 B3). QR-SEC-022.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Onboarding funnel widget - عنصر مسار التسجيل")
public class OnboardingFunnelResponse {

    @Schema(description = "Sign-up requests still PENDING - طلبات التسجيل المعلّقة", example = "3")
    private Long pendingSignups;

    @Schema(description = "PENDING sign-ups older than the stalled threshold - الطلبات المعلّقة المتأخرة", example = "1")
    private Long stalledCount;
}
