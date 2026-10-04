package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-028 — one POSTED journal line in an account's ledger, with the running balance after it
 * (QR-FIN-042, POL-FIN-009: derived live, never read from a stored balance column).
 *
 * <p>{@code journalEntryId} and {@code eventReference} are the REQ-FIN-046 drill-down handles:
 * report row → originating entry → its source event.
 *
 * <p>No audit fields: this is an aggregation row, not an entity projection — there is no single
 * row in {@code FIN_JOURNAL_LINE} carrying a {@code runningBalance}, so A.3.7's audit-field clause
 * has nothing to map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account ledger row - سطر في دفتر الحساب")
public class AccountLedgerRowResponse {

    @Schema(description = "Originating journal entry id, for drill-down (REQ-FIN-046) - "
        + "معرّف القيد المصدر", example = "12")
    private Long journalEntryId;

    @Schema(description = "Journal entry document number - رقم مستند القيد",
        example = "JV-2026-000123")
    private String docNo;

    @Schema(description = "Journal entry document date - تاريخ مستند القيد", example = "2026-01-31")
    private LocalDate docDate;

    @Schema(description = "Journal type, JOURNAL_TYPE lookup - نوع اليومية", example = "MANUAL")
    private String journalTypeCode;

    @Schema(description = "Source event reference, when the entry was event-built - مرجع الحدث",
        example = "INV-2026-0001")
    private String eventReference;

    @Schema(description = "Journal line id - معرّف سطر القيد", example = "44")
    private Long journalLineId;

    @Schema(description = "Line number inside the entry - رقم السطر داخل القيد", example = "1")
    private Integer lineNo;

    @Schema(description = "Line amount, always positive (POL-FIN-005) - المبلغ",
        example = "1500.0000")
    private BigDecimal amount;

    @Schema(description = "Direction, DEBIT_CREDIT lookup - الاتجاه", example = "DEBIT")
    private String directionCode;

    @Schema(description = "Amount signed against the account's nature (POL-FIN-002) - "
        + "المبلغ بإشارة طبيعة الحساب", example = "1500.0000")
    private BigDecimal signedAmount;

    @Schema(description = "Running balance after this line, signed per POL-FIN-002 - "
        + "الرصيد الجاري بعد هذا السطر", example = "3200.0000")
    private BigDecimal runningBalance;

    @Schema(description = "Line description (Arabic) - وصف السطر بالعربية", example = "إيجار يناير")
    private String descriptionAr;

    @Schema(description = "Line description (English) - وصف السطر بالإنجليزية",
        example = "January rent")
    private String descriptionEn;
}
