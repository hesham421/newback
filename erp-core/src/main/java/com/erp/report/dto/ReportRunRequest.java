package com.erp.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Run a report: parameters and the page to return - تشغيل تقرير: المعاملات والصفحة المطلوبة")
public class ReportRunRequest {

    @Schema(description = "Report parameters by name (ISO-8601 dates) - معاملات التقرير حسب الاسم",
        example = "{\"realm\":\"STAFF\",\"createdFrom\":\"2026-01-01\"}")
    @Builder.Default
    private Map<String, Object> params = new LinkedHashMap<>();

    @Min(value = 0, message = "{validation.min}")
    @Schema(description = "Page number, zero-based - رقم الصفحة", example = "0")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "{validation.min}")
    @Max(value = 200, message = "{validation.max}")
    @Schema(description = "Page size (1..200) - حجم الصفحة", example = "20")
    @Builder.Default
    private Integer size = 20;
}
