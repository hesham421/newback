package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-030 — the balance sheet (REQ-FIN-041): QR-FIN-043's aggregation kept to ASSET,
 * LIABILITY and EQUITY and grouped by type.
 *
 * <p>Continuity across years is NOT recomputed here. It is guaranteed upstream by REQ-FIN-036's
 * year-end close, which posts the prior year's closing balances as this year's opening entry
 * (POL-FIN-010) — so those opening lines are ordinary POSTED lines this aggregation already reads.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Live-derived balance sheet - الميزانية العمومية المُشتقة حيًا")
public class BalanceSheetResponse {

    @Schema(description = "Fiscal year the statement is scoped to - السنة المالية", example = "1")
    private Long fiscalYearId;

    @Schema(description = "Cut-off applied to docDate, null when unbounded - تاريخ الإقفال",
        example = "2026-12-31")
    private LocalDate asOfDate;

    @Schema(description = "Groups: ASSET, LIABILITY, EQUITY - المجموعات")
    private List<AccountBalanceGroupResponse> groups;
}
