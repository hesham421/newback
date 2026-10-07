package com.erp.common.lookup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One option of a module-owned lookup (LOV) served from MDL — a slim, code-driven read model for
 * dropdowns, with no id or audit surface. See {@link OwnedLookups}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lookup option - خيار قائمة قيم")
public class LookupOptionResponse {

    @Schema(description = "Value code (natural key) - رمز القيمة", example = "EMAIL")
    private String code;

    @Schema(description = "Arabic display label - التسمية بالعربية", example = "بريد")
    private String labelAr;

    @Schema(description = "English display label - التسمية بالإنجليزية", example = "Email")
    private String labelEn;
}
