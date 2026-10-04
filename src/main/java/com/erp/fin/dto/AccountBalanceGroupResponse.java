package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One {@code accountTypeCode} group of a grouped financial statement — ASSET / LIABILITY / EQUITY
 * for API-FIN-030, REVENUE / EXPENSE for API-FIN-031. Both statements group the SAME QR-FIN-043
 * rows, so they share this container.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Statement group by account type - مجموعة القائمة حسب نوع الحساب")
public class AccountBalanceGroupResponse {

    @Schema(description = "Account type, ACCOUNT_TYPE lookup - نوع الحساب", example = "ASSET")
    private String accountTypeCode;

    @Schema(description = "Accounts in this group, ordered by code - حسابات المجموعة")
    private List<AccountBalanceRowResponse> rows;

    @Schema(description = "Sum of the group's signed balances (POL-FIN-002) - إجمالي المجموعة",
        example = "42000.0000")
    private BigDecimal groupTotal;
}
