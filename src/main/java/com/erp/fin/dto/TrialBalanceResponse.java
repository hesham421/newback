package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-029 — the trial balance (REQ-FIN-040), one row per account from QR-FIN-043's live
 * aggregation over POSTED lines.
 *
 * <p>{@code balanced} restates POL-FIN-008 as an observable fact rather than a gate: the two
 * column totals match BY CONSTRUCTION, because every contributing entry individually satisfied
 * RULE-FIN-006 before it could post (QR-FIN-029). The flag is therefore reported, never enforced —
 * a false value means the posted data or the query is wrong, not the presentation, and the plan's
 * own Validations line says no separate check is needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Live-derived trial balance - ميزان المراجعة المُشتق حيًا")
public class TrialBalanceResponse {

    @Schema(description = "Fiscal period filter applied, null when unbounded - الفترة المُرشَّحة",
        example = "3")
    private Long periodId;

    @Schema(description = "Account-type filter applied, null when all types - النوع المُرشَّح",
        example = "ASSET")
    private String accountTypeCode;

    @Schema(description = "Account rows, ordered by account code - سطور الحسابات")
    private List<AccountBalanceRowResponse> rows;

    @Schema(description = "Total of the debit-balance column - إجمالي الأرصدة المدينة",
        example = "12500.0000")
    private BigDecimal totalDebitBalance;

    @Schema(description = "Total of the credit-balance column - إجمالي الأرصدة الدائنة",
        example = "12500.0000")
    private BigDecimal totalCreditBalance;

    @Schema(description = "Whether the two column totals match, an invariant by construction "
        + "(POL-FIN-008) - هل يتوازن الميزان", example = "true")
    private Boolean balanced;
}
