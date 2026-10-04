package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-010 response — every business field, the parent's id, the line's ENT-FIN-016 tags
 * ({@code dimensionTags}, v2) and the single audit column the table carries ({@code createdAt},
 * DBF-FIN-108; {@code RuleLine} is deliberately not an {@code AuditableEntity}). Serves
 * API-FIN-011 (201).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event-type rule line - سطر قاعدة نوع الحدث")
public class RuleLineResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long ruleLinePk;

    @Schema(description = "Parent event-type rule id - معرّف القاعدة الأب", example = "1")
    private Long eventTypeRuleId;

    @Schema(description = "Line position within the rule - رقم السطر", example = "1")
    private Integer lineNo;

    @Schema(description = "Account derivation type, ACCOUNT_DERIVATION_TYPE lookup "
        + "- نوع اشتقاق الحساب", example = "CONSTANT")
    private String accountDerivationTypeCode;

    @Schema(description = "Constant account code, CONSTANT only - رمز الحساب الثابت",
        example = "1101")
    private String accountDerivationValue;

    @Schema(description = "Business field selecting the account, FIN_EVENT_BUSINESS_FIELD lookup, "
        + "MAPPING only - الحقل التجاري لربط الحساب", example = "PAYMENT_METHOD")
    private String accountBusinessFieldCode;

    @Schema(description = "Amount source type, AMOUNT_SOURCE_TYPE lookup - نوع مصدر المبلغ",
        example = "FIELD")
    private String amountSourceTypeCode;

    @Schema(description = "Amount source value - قيمة مصدر المبلغ", example = "netAmount")
    private String amountSourceValue;

    @Schema(description = "Direction, DEBIT_CREDIT lookup - الاتجاه", example = "DEBIT")
    private String directionCode;

    @Schema(description = "Distribution type, DISTRIBUTION_TYPE lookup - نوع التوزيع",
        example = "FIXED")
    private String distributionTypeCode;

    @Schema(description = "Carries the remainder of a percentage distribution - سطر الباقي",
        example = "false")
    private Boolean isRemainderFl;

    @Schema(description = "The line's dimension tags - وسوم الأبعاد على السطر")
    private List<RuleLineDimensionTagResponse> dimensionTags;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant createdAt;
}
