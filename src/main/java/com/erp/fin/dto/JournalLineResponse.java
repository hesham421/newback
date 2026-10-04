package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-005 response — every business field, the parent's id, the line's dimension tags, and
 * the single audit column the table carries ({@code createdAt}, DBF-FIN-060).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Journal entry line - سطر قيد يومية")
public class JournalLineResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long journalLinePk;

    @Schema(description = "Parent journal entry id - معرّف القيد الأب", example = "1")
    private Long journalEntryId;

    @Schema(description = "Line position within the entry - رقم السطر", example = "1")
    private Integer lineNo;

    @Schema(description = "Target account id - معرّف الحساب", example = "12")
    private Long accountId;

    @Schema(description = "Line amount, always positive - المبلغ", example = "1500.0000")
    private BigDecimal amount;

    @Schema(description = "Direction, DEBIT_CREDIT lookup - الاتجاه", example = "DEBIT")
    private String directionCode;

    @Schema(description = "Carries the remainder of a percentage distribution - سطر الباقي",
        example = "false")
    private Boolean isRemainderFl;

    @Schema(description = "Line description (Arabic) - وصف السطر بالعربية", example = "إهلاك")
    private String descriptionAr;

    @Schema(description = "Line description (English) - وصف السطر بالإنجليزية",
        example = "Depreciation")
    private String descriptionEn;

    @Schema(description = "Analysis dimension tags - الأبعاد التحليلية")
    private List<JournalLineDimensionResponse> dimensions;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant createdAt;
}
