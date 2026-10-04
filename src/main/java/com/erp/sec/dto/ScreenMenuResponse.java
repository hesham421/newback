package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** API-SEC-027 leaf — one screen the caller's effective grants reach, under its module. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Menu screen - شاشة في القائمة")
public class ScreenMenuResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long screenRegPk;

    @Schema(description = "Page code - رمز الصفحة", example = "FIN_JOURNAL_ENTRIES")
    private String pageCode;

    @Schema(description = "Screen name (Arabic) - اسم الشاشة بالعربية", example = "قيود اليومية")
    private String nameAr;

    @Schema(description = "Screen name (English) - اسم الشاشة بالإنجليزية", example = "Journal entries")
    private String nameEn;
}
