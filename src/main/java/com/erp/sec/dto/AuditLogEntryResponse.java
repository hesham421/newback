package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-SEC-011 response — every stored field, returned unmodified (REQ-SEC-025, "without altering
 * any of them"). The append-only table carries no createdBy/updatedAt audit pair of its own.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit log entry - قيد سجل التدقيق")
public class AuditLogEntryResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long auditLogPk;

    @Schema(description = "AUDIT_EVENT_TYPE code - رمز نوع الحدث", example = "LOGIN_FAILED")
    private String eventTypeCode;

    @Schema(description = "Acting user id, null when unknown - معرف المستخدم الفاعل", example = "1")
    private Long actorUserId;

    @Schema(description = "Event timestamp - تاريخ وقوع الحدث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant occurredAt;

    @Schema(description = "Affected record reference - مرجع السجل المتأثر", example = "12")
    private String targetRef;

    @Schema(description = "Details (Arabic) - التفاصيل بالعربية", example = "محاولة دخول فاشلة")
    private String detailsAr;

    @Schema(description = "Details (English) - التفاصيل بالإنجليزية", example = "Failed login attempt")
    private String detailsEn;

    @Schema(description = "Client IP address - عنوان الـ IP", example = "10.0.0.8")
    private String ipAddress;
}
