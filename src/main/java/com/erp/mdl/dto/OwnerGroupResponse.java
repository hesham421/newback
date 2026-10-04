package com.erp.mdl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-MDL-010 — one owner-module group in the registry-browse response: an
 * {@code ownerModuleCode} with its nested list of active {@link LookupTypeResponse} rows
 * (SVC-API-SEARCH.md; QR-MDL-010 groups in the service layer, not via a SQL GROUP BY, per
 * DATA-DOM.md's "single-table, grouped in the service layer" note).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lookup type registry grouped by owner module - سجل أنواع اللوكب مجمّعاً حسب الوحدة المالكة")
public class OwnerGroupResponse {

    @Schema(description = "Owner module code - رمز الوحدة المالكة", example = "FIN")
    private String ownerModuleCode;

    @Schema(description = "Active lookup types owned by this module - أنواع اللوكب النشطة التابعة لهذه الوحدة")
    private List<LookupTypeResponse> types;
}
