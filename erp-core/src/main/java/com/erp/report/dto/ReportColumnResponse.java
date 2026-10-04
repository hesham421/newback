package com.erp.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "A report result column - عمود نتيجة التقرير")
public class ReportColumnResponse {

    @Schema(description = "Key of the column's value in each row - مفتاح القيمة في كل صف", example = "username")
    private String key;

    @Schema(description = "Type: STRING, INTEGER, DECIMAL, DATE, DATETIME, BOOLEAN - النوع", example = "STRING")
    private String type;

    @Schema(description = "Arabic header - العنوان العربي", example = "اسم المستخدم")
    private String labelAr;

    @Schema(description = "English header - العنوان الإنجليزي", example = "Username")
    private String labelEn;
}
