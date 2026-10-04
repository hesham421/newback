package com.erp.notif.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-NOTIF-006 lookup option (LOV-NOTIF-001 NOTIF_CHANNEL / LOV-NOTIF-002 NOTIF_STATUS). Slim,
 * code-driven read model — the NOTIF LOVs are runtime-loaded codes with no lookup table, so there is
 * no id or audit surface — a lean projection suited to dropdowns.
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
