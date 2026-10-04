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
 * API-FIN-028 — the account ledger (REQ-FIN-039). Every figure is computed at read time from
 * POSTED lines (QR-FIN-042, POL-FIN-009); the module stores no balance column anywhere.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Live-derived account ledger - دفتر الحساب المُشتق حيًا")
public class AccountLedgerResponse {

    @Schema(description = "Account id - معرّف الحساب", example = "5")
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

    @Schema(description = "Range start applied to docDate, null when unbounded - بداية المدى",
        example = "2026-01-01")
    private LocalDate fromDate;

    @Schema(description = "Range end applied to docDate, null when unbounded - نهاية المدى",
        example = "2026-01-31")
    private LocalDate toDate;

    @Schema(description = "Dimension filter applied, null when none - البُعد المُرشَّح",
        example = "2")
    private Long dimensionId;

    @Schema(description = "Dimension-value filter applied, null when none - قيمة البُعد المُرشَّحة",
        example = "7")
    private Long dimensionValueId;

    @Schema(description = "Total of DEBIT lines in range - إجمالي المدين", example = "5000.0000")
    private BigDecimal debitTotal;

    @Schema(description = "Total of CREDIT lines in range - إجمالي الدائن", example = "1800.0000")
    private BigDecimal creditTotal;

    @Schema(description = "Closing balance, signed per POL-FIN-002 - الرصيد الختامي",
        example = "3200.0000")
    private BigDecimal closingBalance;

    @Schema(description = "Ledger rows, oldest first - سطور الدفتر")
    private List<AccountLedgerRowResponse> rows;
}
