package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry of API-FIN-013's {@code lines: [...]} (SVC-API-CRUD.md), carrying ENT-FIN-012's
 * business fields (DBF-FIN-125..128). Excludes {recurringTemplateLinePk, recurringTemplateId,
 * lineNo, createdAt} — {@code lineNo} is the submitted list position, assigned by the service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recurring template line - سطر قالب متكرر")
public class RecurringTemplateLineCreateRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Target account id - معرّف الحساب", example = "12")
    private Long accountId;

    @NotNull(message = "{validation.required}")
    @Positive(message = "{validation.min}")
    @Schema(description = "Line amount, always positive - المبلغ", example = "1500.0000")
    private BigDecimal amount;

    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Schema(description = "Direction, DEBIT_CREDIT lookup - الاتجاه", example = "DEBIT")
    private String directionCode;

    @Schema(description = "Optional dimension value id - معرّف قيمة البُعد", example = "5")
    private Long dimensionValueId;
}
