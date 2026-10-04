package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-006 response — every business field plus the parent's id. The table carries no audit
 * column at all (DBF-FIN-061..064), so none is mapped.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Journal line dimension tag - بُعد تحليلي لسطر القيد")
public class JournalLineDimensionResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long journalLineDimensionPk;

    @Schema(description = "Parent journal line id - معرّف سطر القيد الأب", example = "1")
    private Long journalLineId;

    @Schema(description = "Dimension id - معرّف البُعد", example = "1")
    private Long dimensionId;

    @Schema(description = "Dimension value id - معرّف قيمة البُعد", example = "5")
    private Long dimensionValueId;
}
