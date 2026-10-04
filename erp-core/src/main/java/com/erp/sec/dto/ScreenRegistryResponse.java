package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-SEC-005 response — all fields plus audit. The owning module is flattened to its id and code
 * so the client never receives the related entity; {@code actions} is filled on the API-SEC-021
 * registry-tree path only and omitted elsewhere.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registered screen - شاشة مسجَّلة")
public class ScreenRegistryResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long screenRegPk;

    @Schema(description = "Page code - رمز الصفحة", example = "TST_SCREEN")
    private String pageCode;

    @Schema(description = "Owning module id - معرف الوحدة المالكة", example = "1")
    private Long moduleId;

    @Schema(description = "Owning module code - رمز الوحدة المالكة", example = "TST")
    private String moduleCode;

    @Schema(description = "Screen name (Arabic) - اسم الشاشة بالعربية", example = "شاشة الاختبار")
    private String nameAr;

    @Schema(description = "Screen name (English) - اسم الشاشة بالإنجليزية", example = "Test screen")
    private String nameEn;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Active actions of this screen, returned by the registry search - إجراءات الشاشة النشطة")
    private List<ActionRegistryResponse> actions;

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
