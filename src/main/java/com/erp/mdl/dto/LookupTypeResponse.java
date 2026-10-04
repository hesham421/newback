package com.erp.mdl.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-MDL-001 response — all fields + audit. Also reused, per SVC-API-CRUD.md, as the API-MDL-004
 * deactivate confirmation body (it already carries {@code lookupTypePk} and {@code isActiveFl}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lookup type - نوع لوكب")
public class LookupTypeResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long lookupTypePk;

    @Schema(description = "Unique lookup type key - مفتاح نوع اللوكب الفريد", example = "ORDER_STATUS")
    private String key;

    @Schema(description = "Owner module code - رمز الوحدة المالكة", example = "FIN")
    private String ownerModuleCode;

    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "حالة الطلب")
    private String nameAr;

    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Order Status")
    private String nameEn;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;
}
