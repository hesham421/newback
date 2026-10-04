package com.erp.audit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One audit row as returned by {@code GET /api/v1/audit/events} (erp-core step 10). */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Audit event - حدث تدقيق")
public class AuditEventResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "101")
    private Long id;

    @Schema(description = "When it happened - وقت الحدوث", example = "2026-10-04T10:15:30.000Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant occurredAt;

    @Schema(description = "Username of the actor (system without caller) - اسم المستخدم المنفذ", example = "admin")
    private String actor;

    @Schema(description = "Actor realm: STAFF, CUSTOMER or SYSTEM - نطاق المنفذ", example = "STAFF")
    private String actorRealm;

    @Schema(description = "Actor user id when known - معرف المستخدم المنفذ", example = "7")
    private Long actorUserId;

    @Schema(description = "Action, e.g. CREATE, UPDATE, LOGIN - الإجراء", example = "UPDATE")
    private String action;

    @Schema(description = "Entity type - نوع الكيان", example = "SEC_USER")
    private String entityType;

    @Schema(description = "Entity id - معرف الكيان", example = "42")
    private String entityId;

    @Schema(description = "Summary (Arabic) - الملخص بالعربية", example = "تعديل SEC_USER رقم 42")
    private String summaryAr;

    @Schema(description = "Summary (English) - الملخص بالإنجليزية", example = "Updated SEC_USER #42")
    private String summaryEn;

    @Schema(description = "Field changes: JSON array of {field, old, new} - تغييرات الحقول",
        example = "[{\"field\":\"fullNameEn\",\"old\":\"Old\",\"new\":\"New\"}]")
    @JsonRawValue
    private String changes;

    @Schema(description = "Client IP - عنوان العميل", example = "203.0.113.7")
    private String ip;

    @Schema(description = "Client user agent - متصفح العميل", example = "Mozilla/5.0")
    private String userAgent;

    @Schema(description = "Correlation reference - مرجع الربط", example = "INV-2026-0001")
    private String reference;

    @Schema(description = "Created timestamp - تاريخ الإنشاء", example = "2026-10-04T10:15:30.000Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة", example = "admin")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث", example = "2026-10-04T10:15:30.000Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة", example = "admin")
    private String updatedBy;
}
