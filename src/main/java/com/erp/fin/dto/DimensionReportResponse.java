package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-032 — the dimension report (REQ-FIN-043): POSTED lines aggregated by the full
 * account-and-dimension-value combination (QR-FIN-044, POL-FIN-011).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Live-derived dimension report - تقرير الأبعاد المُشتق حيًا")
public class DimensionReportResponse {

    @Schema(description = "Dimension the report is grouped by - البُعد", example = "2")
    private Long dimensionId;

    @Schema(description = "Dimension value filter applied, null when all values - "
        + "قيمة البُعد المُرشَّحة", example = "7")
    private Long dimensionValueId;

    @Schema(description = "Fiscal period filter applied, null when unbounded - الفترة المُرشَّحة",
        example = "3")
    private Long periodId;

    @Schema(description = "Rows, one per account and dimension value - السطور")
    private List<DimensionReportRowResponse> rows;
}
