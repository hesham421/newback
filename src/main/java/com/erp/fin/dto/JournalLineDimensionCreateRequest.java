package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry of a journal line's {@code dimensions: [{dimensionId, dimensionValueId}]}
 * (API-FIN-019, SVC-API-CRUD.md), carrying ENT-FIN-006's two business FKs (DBF-FIN-063,
 * DBF-FIN-064). Excludes {journalLineDimensionPk, journalLineId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Journal line dimension tag - بُعد تحليلي لسطر القيد")
public class JournalLineDimensionCreateRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Dimension id the value is claimed to belong to - معرّف البُعد",
        example = "1")
    private Long dimensionId;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Dimension value id - معرّف قيمة البُعد", example = "5")
    private Long dimensionValueId;
}
