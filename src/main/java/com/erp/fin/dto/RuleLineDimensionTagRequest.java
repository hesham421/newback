package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One dimension tag inside API-FIN-011's body (ENT-FIN-016, ADR-FIN-025): the tag travels with
 * its rule line and is never submitted on its own. {@code dimensionValueId} is CONSTANT-only and
 * {@code businessFieldCode} BUSINESS_FIELD-only (RULE-FIN-026, decided in the Domain).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dimension tag submitted with a rule line - وسم بُعد مُرسَل مع سطر القاعدة")
public class RuleLineDimensionTagRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Tagged dimension id - معرّف البُعد الموسوم", example = "1")
    private Long dimensionId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Value source, FIN_DIMENSION_VALUE_SOURCE lookup - مصدر القيمة",
        example = "CONSTANT")
    private String valueSourceCode;

    @Schema(description = "Constant dimension value id, CONSTANT only - معرّف القيمة الثابتة",
        example = "7")
    private Long dimensionValueId;

    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Business field, FIN_EVENT_BUSINESS_FIELD lookup, BUSINESS_FIELD only "
        + "- الحقل التجاري", example = "BRANCH_CODE")
    private String businessFieldCode;
}
