package com.erp.tenant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tenant-maturity B (REQ-TENANT-028) — {@code GET /api/v1/platform/tenants/{id}/usage}: figures counted inside the
 * tenant through SEC, FILE and NOTIF. Not an eligibility {@code UsageResponse}: a tenant is never deleted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Usage figures of a tenant - أرقام استخدام المستأجر")
public class TenantUsageResponse {

    @Schema(description = "Tenant id - معرف المستأجر", example = "2")
    private Long id;

    @Schema(description = "Staff users, any status - عدد مستخدمي الموظفين", example = "12")
    private int staffUsers;

    @Schema(description = "Customer accounts, any status - عدد حسابات العملاء", example = "340")
    private int customerUsers;

    @Schema(description = "Open sessions (not terminated), both realms - الجلسات المفتوحة", example = "5")
    private int activeSessions;

    @Schema(description = "Documents that are not deleted - عدد المستندات", example = "87")
    private long fileDocuments;

    @Schema(description = "Bytes held by those documents - حجم المستندات بالبايت", example = "10485760")
    private long fileBytes;

    @Schema(description = "Notification log rows created in the last 30 days - الإشعارات خلال آخر 30 يومًا", example = "42")
    private long notificationsLast30Days;

    @Schema(description = "When the figures were collected - وقت جمع الأرقام")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant collectedAt;
}
