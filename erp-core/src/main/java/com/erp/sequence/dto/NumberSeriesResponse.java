package com.erp.sequence.dto;

import com.erp.sequence.domain.ResetPolicy;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One period row of a number series (erp-core step 09). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Number series period - فترة سلسلة ترقيم")
public class NumberSeriesResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long id;

    @Schema(description = "Series code - رمز السلسلة", example = "SALES_INVOICE")
    private String code;

    @Schema(description = "Prefix - البادئة", example = "INV")
    private String prefix;

    @Schema(description = "Pattern - نمط الترقيم", example = "{PREFIX}-{YYYY}-{SEQ:6}")
    private String pattern;

    @Schema(description = "Reset policy - سياسة إعادة الترقيم", example = "YEARLY")
    private ResetPolicy resetPolicy;

    @Schema(description = "Period of this row ('' for NEVER) - الفترة", example = "2026")
    private String periodKey;

    @Schema(description = "Next value handed out in this period - القيمة التالية", example = "124")
    private Long nextValue;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActive;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;
}
