package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-027 response (SVC-API-INT.md): {@code {closingEntry: JournalEntryResponse,
 * openingEntry: JournalEntryResponse}} — the two entries REQ-FIN-036 produces, each already
 * POSTED and each individually balanced.
 *
 * <p>No audit block of its own (A.3.7): this DTO owns no row. Both members carry their own entry's
 * audit fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Year-end close result - نتيجة إقفال نهاية السنة")
public class YearEndCloseResponse {

    @Schema(description = "Closing entry, result accounts to Retained Earnings - قيد الإقفال")
    private JournalEntryResponse closingEntry;

    @Schema(description = "Opening entry for the next fiscal year - القيد الافتتاحي")
    private JournalEntryResponse openingEntry;
}
