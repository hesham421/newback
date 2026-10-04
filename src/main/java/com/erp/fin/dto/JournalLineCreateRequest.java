package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry of API-FIN-019's {@code lines: [...]} (SVC-API-CRUD.md): {@code {accountId, amount,
 * directionCode, descriptionAr?, descriptionEn?, dimensions: [...]}}. Excludes {journalLinePk,
 * journalEntryId, lineNo, isRemainderFl, createdAt} — {@code lineNo} is the submitted list
 * position, assigned by the service, and a manual entry carries no remainder line (that flag
 * belongs to the rule-driven and allocation-driven builds).
 *
 * <p>{@code amount} is {@code @Positive} to match {@code CHK_FIN_JOURNAL_LINE_AMOUNT_POSITIVE}
 * (POL-FIN-005) — the side is carried by {@code directionCode}, never by the sign.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Journal entry line - سطر قيد يومية")
public class JournalLineCreateRequest {

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

    @Schema(description = "Line description (Arabic) - وصف السطر بالعربية", example = "إهلاك")
    private String descriptionAr;

    @Schema(description = "Line description (English) - وصف السطر بالإنجليزية",
        example = "Depreciation")
    private String descriptionEn;

    @Valid
    @Schema(description = "Analysis dimension tags - الأبعاد التحليلية")
    private List<JournalLineDimensionCreateRequest> dimensions;
}
