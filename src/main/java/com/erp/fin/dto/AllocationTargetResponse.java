package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-014 response — every business field plus the parent's id. The table carries no audit
 * column at all (DBF-FIN-139..146), so none is mapped.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Allocation rule target - هدف قاعدة التوزيع")
public class AllocationTargetResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long allocationTargetPk;

    @Schema(description = "Parent allocation rule id - معرّف قاعدة التوزيع الأب", example = "1")
    private Long allocationRuleId;

    @Schema(description = "Target position within the rule - رقم السطر", example = "1")
    private Integer lineNo;

    @Schema(description = "Target account id - معرّف الحساب المستهدف", example = "21")
    private Long targetAccountId;

    @Schema(description = "Optional dimension value id - معرّف قيمة البُعد", example = "5")
    private Long dimensionValueId;

    @Schema(description = "Distribution type, DISTRIBUTION_TYPE lookup - نوع التوزيع",
        example = "PERCENTAGE")
    private String distributionTypeCode;

    @Schema(description = "Distribution value - قيمة التوزيع", example = "25.0000")
    private BigDecimal distributionValue;

    @Schema(description = "Carries the remainder of a percentage distribution - هدف الباقي",
        example = "false")
    private Boolean isRemainderFl;
}
