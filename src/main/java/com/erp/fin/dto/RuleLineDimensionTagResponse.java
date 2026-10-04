package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-016 response — a rule line's dimension tag, returned inside {@link RuleLineResponse}
 * (API-FIN-011). {@code createdAt} (DBF-FIN-165) is the table's only audit column.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rule line dimension tag - وسم بُعد على سطر القاعدة")
public class RuleLineDimensionTagResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long ruleLineDimensionPk;

    @Schema(description = "Owning rule line id - معرّف سطر القاعدة", example = "1")
    private Long ruleLineId;

    @Schema(description = "Tagged dimension id - معرّف البُعد الموسوم", example = "1")
    private Long dimensionId;

    @Schema(description = "Value source, FIN_DIMENSION_VALUE_SOURCE lookup - مصدر القيمة",
        example = "CONSTANT")
    private String valueSourceCode;

    @Schema(description = "Constant dimension value id, CONSTANT only - معرّف القيمة الثابتة",
        example = "7")
    private Long dimensionValueId;

    @Schema(description = "Business field, FIN_EVENT_BUSINESS_FIELD lookup, BUSINESS_FIELD only "
        + "- الحقل التجاري", example = "BRANCH_CODE")
    private String businessFieldCode;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant createdAt;
}
