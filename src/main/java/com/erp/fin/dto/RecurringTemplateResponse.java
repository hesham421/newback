package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-011 response — every business field, the persisted line set, its count, plus audit
 * (A.3.7). Serves API-FIN-013 (201).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recurring journal template - قالب قيد متكرر")
public class RecurringTemplateResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long recurringTemplatePk;

    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "إهلاك شهري")
    private String nameAr;

    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Monthly depreciation")
    private String nameEn;

    @Schema(description = "Schedule type, RECURRING_SCHEDULE_TYPE lookup - نوع الجدولة",
        example = "RECURRING")
    private String scheduleTypeCode;

    @Schema(description = "Frequency, RECURRING_FREQUENCY lookup - التكرار", example = "MONTHLY")
    private String frequencyCode;

    @Schema(description = "First run date - تاريخ البداية", example = "2026-01-31")
    private LocalDate startDate;

    @Schema(description = "Next scheduled run date - تاريخ التشغيل التالي", example = "2026-01-31")
    private LocalDate nextRunDate;

    @Schema(description = "Last run date - تاريخ النهاية", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Number of template lines - عدد السطور", example = "2")
    private Integer lineCount;

    @Schema(description = "Template lines - سطور القالب")
    private List<RecurringTemplateLineResponse> lines;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;
}
