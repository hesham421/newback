package com.erp.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "A report result (one page, or the whole export) - نتيجة التقرير")
public class ReportRunResponse {

    @Schema(description = "Report code - رمز التقرير", example = "SEC_USER_LIST")
    private String code;

    @Schema(description = "Output columns in display order - أعمدة النتيجة")
    private List<ReportColumnResponse> columns;

    @Schema(description = "Rows keyed by column key - الصفوف")
    private List<Map<String, Object>> rows;

    @Schema(description = "Totals over the whole result keyed by column key - الإجماليات", example = "{\"count\":42}")
    private Map<String, Object> totals;

    @Schema(description = "Page number, zero-based (null for an export) - رقم الصفحة", example = "0")
    private Integer page;

    @Schema(description = "Page size (null for an export) - حجم الصفحة", example = "20")
    private Integer size;

    @Schema(description = "Rows of the whole result, when the report knows it - إجمالي عدد الصفوف", example = "42")
    private Long totalRows;
}
