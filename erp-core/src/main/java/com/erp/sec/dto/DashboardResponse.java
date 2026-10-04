package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-022 envelope. {@code NON_NULL} is what implements REQ-SEC-023: a widget the caller has
 * no source-screen VIEW for is left null and therefore disappears, rather than serializing as 0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Security dashboard summary - ملخص لوحة تحكم الأمان")
public class DashboardResponse {

    @Schema(description = "Users overview, requires SEC_USERS VIEW - نظرة عامة على المستخدمين")
    private UsersOverviewResponse usersOverview;

    @Schema(description = "Failed logins 24h, requires SEC_AUDIT_LOG VIEW - محاولات الدخول الفاشلة")
    private FailedLoginsResponse failedLogins24h;

    @Schema(description = "Active sessions, requires SEC_SESSIONS VIEW - الجلسات النشطة")
    private ActiveSessionsCountResponse activeSessions;

    @Schema(description = "Most recent audit entries, requires SEC_AUDIT_LOG VIEW - آخر الأحداث")
    private List<AuditLogEntryResponse> recentActivity;

    @Schema(description = "Roles summary, requires SEC_ROLES VIEW - ملخص الأدوار والصلاحيات")
    private RolesPermissionsSummaryResponse rolesPermissionsSummary;

    @Schema(description = "Onboarding funnel, requires SEC_USERS VIEW - مسار التسجيل")
    private OnboardingFunnelResponse onboardingFunnel;
}
