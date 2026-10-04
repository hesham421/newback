package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-019 request body (SVC-API-CRUD.md): {@code {docDate, fiscalYearId, periodId,
 * journalTypeCode="MANUAL", descriptionAr, descriptionEn, lines: [...]}} — explicitly excludes
 * {journalEntryPk, docNo, statusCode, postedAt, audit}.
 *
 * <p>{@code docNo} is generated (CORE.md "Numbering", {@code JV-{fiscalYearCode}-{NNNNNN}}) and is
 * immutable thereafter, so it appears in no request body at any time; {@code statusCode} and
 * {@code postedAt} are set by the posting pipeline, never submitted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create and post a manual journal entry - إنشاء وترحيل قيد يومية يدوي")
public class JournalEntryCreateRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Document date - تاريخ المستند", example = "2026-01-31")
    private LocalDate docDate;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Owning fiscal year id - معرّف السنة المالية", example = "1")
    private Long fiscalYearId;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Target fiscal period id - معرّف الفترة المالية", example = "1")
    private Long periodId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Schema(description = "Journal type, JOURNAL_TYPE lookup - نوع اليومية", example = "MANUAL")
    private String journalTypeCode;

    @Schema(description = "Entry description (Arabic) - وصف القيد بالعربية",
        example = "قيد إهلاك يناير")
    private String descriptionAr;

    @Schema(description = "Entry description (English) - وصف القيد بالإنجليزية",
        example = "January depreciation entry")
    private String descriptionEn;

    @NotEmpty(message = "{validation.required}")
    @Valid
    @Schema(description = "Entry lines - سطور القيد")
    private List<JournalLineCreateRequest> lines;
}
