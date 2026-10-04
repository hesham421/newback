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
 * ENT-FIN-007 response — every business field, the generated period set, its count, plus audit
 * (A.3.7). Serves API-FIN-023 (201, "{@code FiscalYearResponse} with its generated periods").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Fiscal year with its periods - السنة المالية وفتراتها")
public class FiscalYearResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long fiscalYearPk;

    @Schema(description = "Fiscal year code - رمز السنة المالية", example = "2026")
    private String code;

    @Schema(description = "First day of the fiscal year - أول أيام السنة المالية",
        example = "2026-01-01")
    private LocalDate startDate;

    @Schema(description = "Last day of the fiscal year - آخر أيام السنة المالية",
        example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "Status, FISCAL_YEAR_STATUS lookup - حالة السنة", example = "OPEN")
    private String statusCode;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Number of periods - عدد الفترات", example = "12")
    private Integer periodCount;

    @Schema(description = "Generated periods. Populated by the create response only; ALWAYS EMPTY "
        + "on a search row, where the period set is read through the fiscal-period search scoped "
        + "by fiscalYearId - الفترات المولَّدة، تُملأ في استجابة الإنشاء فقط وتكون فارغة دائمًا في "
        + "صف البحث")
    private List<FiscalPeriodResponse> periods;

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
