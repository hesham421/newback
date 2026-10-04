package com.erp.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Export a report (all rows up to the export cap) - تصدير تقرير (كل الصفوف حتى الحد الأقصى)")
public class ReportExportRequest {

    @Schema(description = "Report parameters by name (ISO-8601 dates) - معاملات التقرير حسب الاسم",
        example = "{\"channel\":\"EMAIL\"}")
    @Builder.Default
    private Map<String, Object> params = new LinkedHashMap<>();
}
