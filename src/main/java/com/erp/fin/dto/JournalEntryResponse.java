package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-004 response — every business field, the persisted line set, its count, plus audit
 * (A.3.7). Serves API-FIN-019 (201, {@code statusCode=POSTED}). {@code docNo} is present here and
 * only here: generated once on create and immutable thereafter (CORE.md "Numbering").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Journal entry - قيد يومية")
public class JournalEntryResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long journalEntryPk;

    @Schema(description = "System-generated document number - رقم المستند",
        example = "JV-2026-000123")
    private String docNo;

    @Schema(description = "Document date - تاريخ المستند", example = "2026-01-31")
    private LocalDate docDate;

    @Schema(description = "Owning fiscal year id - معرّف السنة المالية", example = "1")
    private Long fiscalYearId;

    @Schema(description = "Target fiscal period id - معرّف الفترة المالية", example = "1")
    private Long periodId;

    @Schema(description = "Journal type, JOURNAL_TYPE lookup - نوع اليومية", example = "MANUAL")
    private String journalTypeCode;

    @Schema(description = "Status, JOURNAL_STATUS lookup - الحالة", example = "POSTED")
    private String statusCode;

    @Schema(description = "Source event reference, for event-built entries - مرجع الحدث",
        example = "INV-2026-0001")
    private String eventReference;

    @Schema(description = "Original entry id, when this entry is a reversal - معرّف القيد الأصلي",
        example = "7")
    private Long originalEntryId;

    @Schema(description = "Reversal entry id, when this entry has been reversed - معرّف قيد العكس",
        example = "9")
    private Long reversalEntryId;

    @Schema(description = "Entry description (Arabic) - وصف القيد بالعربية",
        example = "قيد إهلاك يناير")
    private String descriptionAr;

    @Schema(description = "Entry description (English) - وصف القيد بالإنجليزية",
        example = "January depreciation entry")
    private String descriptionEn;

    @Schema(description = "Posting timestamp - تاريخ الترحيل")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant postedAt;

    @Schema(description = "Number of entry lines - عدد السطور", example = "2")
    private Integer lineCount;

    @Schema(description = "Entry lines - سطور القيد")
    private List<JournalLineResponse> lines;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;
}
