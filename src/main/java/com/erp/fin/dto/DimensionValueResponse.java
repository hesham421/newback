package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-003 response — every business field, the parent's id, plus audit (A.3.7). Serves
 * API-FIN-007 (201).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dimension value - قيمة بُعد")
public class DimensionValueResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long dimensionValuePk;

    @Schema(description = "Parent dimension id - معرّف البُعد الأب", example = "1")
    private Long dimensionId;

    @Schema(description = "Unique code within the parent dimension - الرمز الفريد ضمن البُعد",
        example = "CC-100")
    private String code;

    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "الإدارة المالية")
    private String nameAr;

    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Finance department")
    private String nameEn;

    @Schema(description = "Display sort order - ترتيب العرض", example = "10")
    private Integer sortOrder;

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
