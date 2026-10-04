package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-027 node — a module the caller's effective grants reach, carrying only the screens that
 * same caller holds (REQ-SEC-021/032). Audit fields are omitted: this is a navigation view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Menu module - وحدة في القائمة")
public class ModuleMenuResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long moduleRegPk;

    @Schema(description = "Module code - رمز الوحدة", example = "FIN")
    private String code;

    @Schema(description = "Module name (Arabic) - اسم الوحدة بالعربية", example = "المالية")
    private String nameAr;

    @Schema(description = "Module name (English) - اسم الوحدة بالإنجليزية", example = "Finance")
    private String nameEn;

    @Schema(description = "Granted screens beneath this module - الشاشات الممنوحة ضمن الوحدة")
    private List<ScreenMenuResponse> screens;
}
