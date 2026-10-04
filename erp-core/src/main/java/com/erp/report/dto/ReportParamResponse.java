package com.erp.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "A report parameter - معامل تقرير")
public class ReportParamResponse {

    @Schema(description = "Parameter name - اسم المعامل", example = "createdFrom")
    private String name;

    @Schema(description = "Type: STRING, INTEGER, DECIMAL, DATE, DATETIME, BOOLEAN, LOOKUP - النوع", example = "DATE")
    private String type;

    @Schema(description = "Whether the parameter is required - هل المعامل إلزامي", example = "false")
    private Boolean required;

    @Schema(description = "MDL lookup type key of a LOOKUP parameter - مفتاح نوع القائمة", example = "NOTIF_CHANNEL")
    private String lookupKey;

    @Schema(description = "Arabic label - التسمية العربية", example = "من تاريخ")
    private String labelAr;

    @Schema(description = "English label - التسمية الإنجليزية", example = "Created from")
    private String labelEn;
}
