package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-031 — the income statement (REQ-FIN-042): QR-FIN-043's aggregation kept to REVENUE and
 * EXPENSE, scoped to one fiscal year and optionally to a range of its periods.
 *
 * <p>"Opens at zero" is not recomputed here either: REQ-FIN-036's year-end close moves every
 * result account's balance to Retained Earnings (POL-FIN-010), so a new year simply has no POSTED
 * result lines yet and this aggregation returns zero for them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Live-derived income statement - قائمة الدخل المُشتقة حيًا")
public class IncomeStatementResponse {

    @Schema(description = "Fiscal year the statement is scoped to - السنة المالية", example = "1")
    private Long fiscalYearId;

    @Schema(description = "First period of the requested range, null when unbounded - "
        + "أول فترة في المدى", example = "1")
    private Long fromPeriodId;

    @Schema(description = "Last period of the requested range, null when unbounded - "
        + "آخر فترة في المدى", example = "6")
    private Long toPeriodId;

    @Schema(description = "docDate lower bound derived from fromPeriodId - بداية المدى",
        example = "2026-01-01")
    private LocalDate fromDate;

    @Schema(description = "docDate upper bound derived from toPeriodId - نهاية المدى",
        example = "2026-06-30")
    private LocalDate toDate;

    @Schema(description = "Groups: REVENUE, EXPENSE - المجموعات")
    private List<AccountBalanceGroupResponse> groups;

    @Schema(description = "Revenue total less expense total, both signed per POL-FIN-002 - "
        + "صافي النتيجة", example = "8400.0000")
    private BigDecimal netResult;
}
