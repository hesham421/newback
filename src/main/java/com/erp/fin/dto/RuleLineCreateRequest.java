package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-011 request body (SVC-API-CRUD.md, MODIFIED v2): {@code {accountDerivationTypeCode,
 * accountDerivationValue?, accountBusinessFieldCode?, amountSourceTypeCode, amountSourceValue?,
 * directionCode, distributionTypeCode, isRemainderFl, dimensionTags[]}}. The owning
 * {@code eventTypeRuleId} travels in the path. Excludes {ruleLinePk, lineNo, createdAt} —
 * {@code lineNo} (DBF-FIN-100) is the rule's highest lineNo + 1, assigned by the service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add a line to an event-type rule - إضافة سطر إلى قاعدة نوع الحدث")
public class RuleLineCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Schema(description = "Account derivation type, ACCOUNT_DERIVATION_TYPE lookup "
        + "- نوع اشتقاق الحساب", example = "CONSTANT")
    private String accountDerivationTypeCode;

    @Schema(description = "Constant account code, CONSTANT only - رمز الحساب الثابت",
        example = "1101")
    private String accountDerivationValue;

    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Business field selecting the account, FIN_EVENT_BUSINESS_FIELD lookup, "
        + "MAPPING only - الحقل التجاري لربط الحساب", example = "PAYMENT_METHOD")
    private String accountBusinessFieldCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Schema(description = "Amount source type, AMOUNT_SOURCE_TYPE lookup - نوع مصدر المبلغ",
        example = "FIELD")
    private String amountSourceTypeCode;

    @Schema(description = "Amount source value - قيمة مصدر المبلغ", example = "netAmount")
    private String amountSourceValue;

    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Schema(description = "Direction, DEBIT_CREDIT lookup - الاتجاه", example = "DEBIT")
    private String directionCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 15, message = "{validation.size}")
    @Schema(description = "Distribution type, DISTRIBUTION_TYPE lookup - نوع التوزيع",
        example = "FIXED")
    private String distributionTypeCode;

    @Schema(description = "Carries the remainder of a percentage distribution - سطر الباقي",
        example = "false")
    @Builder.Default
    private Boolean isRemainderFl = Boolean.FALSE;

    @Valid
    @Schema(description = "Dimension tags submitted with the line - وسوم الأبعاد على السطر")
    @Builder.Default
    private List<RuleLineDimensionTagRequest> dimensionTags = new ArrayList<>();
}
