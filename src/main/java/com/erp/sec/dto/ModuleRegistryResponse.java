package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** ENT-SEC-004 response — all fields plus audit (DATA-DOM-MASTER.md ENT-SEC-004). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registered module - وحدة مسجَّلة")
public class ModuleRegistryResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long moduleRegPk;

    @Schema(description = "Module code - رمز الوحدة", example = "TST")
    private String code;

    @Schema(description = "Module name (Arabic) - اسم الوحدة بالعربية", example = "وحدة الاختبار")
    private String nameAr;

    @Schema(description = "Module name (English) - اسم الوحدة بالإنجليزية", example = "Test module")
    private String nameEn;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة", example = "admin")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة", example = "admin")
    private String updatedBy;
}
