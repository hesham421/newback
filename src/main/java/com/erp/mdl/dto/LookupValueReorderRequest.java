package com.erp.mdl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-MDL-009 request body. Every id must belong to the {@code lookupTypeId} path variable (else
 * {@code MDL-400-REORDER-MISMATCH}); the list's position becomes each value's new {@code sortOrder}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reorder a lookup type's values - إعادة ترتيب قيم نوع اللوكب")
public class LookupValueReorderRequest {

    @NotEmpty(message = "{validation.required}")
    @Schema(description = "Lookup value ids in their new display order - معرّفات القيم بترتيبها الجديد")
    private List<Long> orderedValueIds;
}
