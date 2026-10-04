package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One account's live-derived balance — the row type shared by API-FIN-029 (trial balance),
 * API-FIN-030 (balance sheet) and API-FIN-031 (income statement), because all three read the SAME
 * aggregation (QR-FIN-043) and differ only in which {@code accountTypeCode}s they keep.
 *
 * <p>{@code debitBalance} / {@code creditBalance} are the POL-FIN-002 presentation: the net
 * movement is shown on the account's own normal side, so a debit-natured account with debits in
 * excess carries a {@code debitBalance} and a zero {@code creditBalance}, and vice versa. Exactly
 * one of the two is non-zero, which is what makes API-FIN-029's column totals match by
 * construction (POL-FIN-008).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Live-derived account balance row - سطر رصيد حساب مُشتق حيًا")
public class AccountBalanceRowResponse {

    @Schema(description = "Account id, for drill-down (REQ-FIN-046) - معرّف الحساب", example = "5")
    private Long accountId;

    @Schema(description = "Account code - رمز الحساب", example = "1101")
    private String accountCode;

    @Schema(description = "Account name (Arabic) - اسم الحساب بالعربية",
        example = "النقدية بالصندوق")
    private String accountNameAr;

    @Schema(description = "Account name (English) - اسم الحساب بالإنجليزية",
        example = "Cash on hand")
    private String accountNameEn;

    @Schema(description = "Account type, ACCOUNT_TYPE lookup - نوع الحساب", example = "ASSET")
    private String accountTypeCode;

    @Schema(description = "Normal balance side, DEBIT_CREDIT lookup - طبيعة الحساب",
        example = "DEBIT")
    private String natureCode;

    @Schema(description = "Sum of DEBIT line amounts - إجمالي المدين", example = "5000.0000")
    private BigDecimal debitTotal;

    @Schema(description = "Sum of CREDIT line amounts - إجمالي الدائن", example = "1800.0000")
    private BigDecimal creditTotal;

    @Schema(description = "Balance shown on the debit column, zero otherwise (POL-FIN-002) - "
        + "الرصيد المدين", example = "3200.0000")
    private BigDecimal debitBalance;

    @Schema(description = "Balance shown on the credit column, zero otherwise (POL-FIN-002) - "
        + "الرصيد الدائن", example = "0.0000")
    private BigDecimal creditBalance;

    @Schema(description = "Net balance signed against the account's nature (POL-FIN-002) - "
        + "الرصيد الصافي بإشارة طبيعة الحساب", example = "3200.0000")
    private BigDecimal signedBalance;
}
