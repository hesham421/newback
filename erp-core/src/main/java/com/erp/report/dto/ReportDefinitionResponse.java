package com.erp.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "A registered report - تقرير مسجل")
public class ReportDefinitionResponse {

    @Schema(description = "Report code - رمز التقرير", example = "SEC_USER_LIST")
    private String code;

    @Schema(description = "Owning module - الوحدة المالكة", example = "SEC")
    private String moduleCode;

    @Schema(description = "Arabic title - العنوان العربي", example = "قائمة المستخدمين")
    private String titleAr;

    @Schema(description = "English title - العنوان الإنجليزي", example = "User list")
    private String titleEn;

    @Schema(description = "Permission needed to run it - الصلاحية المطلوبة", example = "SEC:REPORT:SEC_USER_LIST")
    private String authority;

    @Schema(description = "Declared parameters - المعاملات")
    private List<ReportParamResponse> params;
}
