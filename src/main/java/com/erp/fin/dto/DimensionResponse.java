package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** ENT-FIN-002 response — every business field plus audit (A.3.7). Serves API-FIN-006 (201). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Analysis dimension - بُعد تحليلي")
public class DimensionResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long dimensionPk;

    @Schema(description = "Unique dimension code - رمز البُعد الفريد", example = "COST_CENTER")
    private String code;

    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "مركز التكلفة")
    private String nameAr;

    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Cost centre")
    private String nameEn;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

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
