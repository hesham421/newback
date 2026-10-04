package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-008 response — every business field plus audit (A.3.7). Serves API-FIN-024/025/026
 * (200) and rides inside {@link FiscalYearResponse} for API-FIN-023 (201).
 *
 * <p>{@code closedBy}/{@code closedAt} are business columns, not audit columns (DBF-FIN-083/084,
 * CORE.md "Audit fields"): they record the period-close approver, deliberately a different
 * principal from the entry creators in that period (REQ-FIN-037).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Fiscal period - الفترة المحاسبية")
public class FiscalPeriodResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long fiscalPeriodPk;

    @Schema(description = "Owning fiscal year id - معرّف السنة المالية", example = "1")
    private Long fiscalYearId;

    @Schema(description = "Period number within the year - رقم الفترة", example = "1")
    private Integer periodNo;

    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "الفترة 1")
    private String nameAr;

    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Period 1")
    private String nameEn;

    @Schema(description = "First day of the period - أول أيام الفترة", example = "2026-01-01")
    private LocalDate startDate;

    @Schema(description = "Last day of the period - آخر أيام الفترة", example = "2026-01-31")
    private LocalDate endDate;

    @Schema(description = "Status, PERIOD_STATE lookup - حالة الفترة", example = "OPEN")
    private String statusCode;

    @Schema(description = "Close-approval principal - معتمد الإغلاق", example = "finance.approver")
    private String closedBy;

    @Schema(description = "Close-approval moment - وقت اعتماد الإغلاق")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant closedAt;

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
