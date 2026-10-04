package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry of API-FIN-016's {@code targets: [...]} (SVC-API-CRUD.md), carrying ENT-FIN-014's
 * business fields (DBF-FIN-142..146). Excludes {allocationTargetPk, allocationRuleId, lineNo} —
 * {@code lineNo} is the submitted list position, assigned by the service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One entry of a rule's target set — the field examples below describe a "
    + "single PERCENTAGE entry; the complete, valid payload is on AllocationRuleCreateRequest "
    + "- هدف قاعدة التوزيع")
public class AllocationTargetCreateRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Target account id - معرّف الحساب المستهدف", example = "21")
    private Long targetAccountId;

    @Schema(description = "Optional dimension value id - معرّف قيمة البُعد", example = "5")
    private Long dimensionValueId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 15, message = "{validation.size}")
    @Schema(description = "Distribution type, DISTRIBUTION_TYPE lookup - نوع التوزيع",
        example = "PERCENTAGE")
    private String distributionTypeCode;

    @Schema(description = "Distribution value — the percentage or fixed amount; omitted for the "
        + "remainder target - قيمة التوزيع", example = "25.0000")
    private BigDecimal distributionValue;

    @Schema(description = "Carries the remainder of a percentage distribution — exactly one target "
        + "per rule must set this true when any target is PERCENTAGE, and it must be the target "
        + "whose distributionTypeCode is REMAINDER (ENT-FIN-014, RULE-FIN-003 reused) - هدف الباقي",
        example = "false")
    @Builder.Default
    private Boolean isRemainderFl = Boolean.FALSE;
}
