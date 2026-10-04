package com.erp.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FILE-008 lookup option (LOV-FILE-001 FILE_FILE_TYPE / LOV-FILE-002 FILE_FILE_STATUS). Slim,
 * code-driven read model — the FILE LOVs are runtime-loaded codes with no lookup table, so there is
 * no id or audit surface.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lookup option - خيار قائمة قيم")
public class LookupOptionResponse {

    @Schema(description = "Value code (natural key) - رمز القيمة", example = "IMAGE")
    private String code;

    @Schema(description = "Arabic display label - التسمية بالعربية", example = "صورة")
    private String labelAr;

    @Schema(description = "English display label - التسمية بالإنجليزية", example = "Image")
    private String labelEn;
}
