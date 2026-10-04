package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-032 — one account × dimension-value combination (QR-FIN-044, POL-FIN-011). The pair IS
 * the posting identity, so the same account appears once per dimension value it was posted
 * against, never collapsed into a single base-account row (AC-FIN-043).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dimension report row - سطر تقرير الأبعاد")
public class DimensionReportRowResponse {

    @Schema(description = "Account id - معرّف الحساب", example = "5")
    private Long accountId;

    @Schema(description = "Account code - رمز الحساب", example = "5201")
    private String accountCode;

    @Schema(description = "Account name (Arabic) - اسم الحساب بالعربية", example = "مصروف إيجار")
    private String accountNameAr;

    @Schema(description = "Account name (English) - اسم الحساب بالإنجليزية",
        example = "Rent expense")
    private String accountNameEn;

    @Schema(description = "Normal balance side, DEBIT_CREDIT lookup - طبيعة الحساب",
        example = "DEBIT")
    private String natureCode;

    @Schema(description = "Dimension id - معرّف البُعد", example = "2")
    private Long dimensionId;

    @Schema(description = "Dimension value id - معرّف قيمة البُعد", example = "7")
    private Long dimensionValueId;

    @Schema(description = "Dimension value code - رمز قيمة البُعد", example = "PRJ-A")
    private String dimensionValueCode;

    @Schema(description = "Dimension value name (Arabic) - اسم قيمة البُعد بالعربية",
        example = "مشروع أ")
    private String dimensionValueNameAr;

    @Schema(description = "Dimension value name (English) - اسم قيمة البُعد بالإنجليزية",
        example = "Project A")
    private String dimensionValueNameEn;

    @Schema(description = "Sum of DEBIT line amounts - إجمالي المدين", example = "5000.0000")
    private BigDecimal debitTotal;

    @Schema(description = "Sum of CREDIT line amounts - إجمالي الدائن", example = "0.0000")
    private BigDecimal creditTotal;

    @Schema(description = "Net balance signed against the account's nature (POL-FIN-002) - "
        + "الرصيد الصافي", example = "5000.0000")
    private BigDecimal signedBalance;
}
