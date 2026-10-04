package com.erp.mdl.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-MDL-002 response — all fields + audit + the parent's id ({@code lookupTypeId}), for the
 * client's benefit (SVC-API-CRUD.md). Also reused as the API-MDL-008 deactivate confirmation body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lookup value - قيمة لوكب")
public class LookupValueResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long lookupValuePk;

    @Schema(description = "Parent lookup type id - معرّف نوع اللوكب الأب", example = "1")
    private Long lookupTypeId;

    @Schema(description = "Unique code within the parent lookup type - الرمز الفريد ضمن النوع", example = "PENDING")
    private String code;

    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "قيد الانتظار")
    private String nameAr;

    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Pending")
    private String nameEn;

    @Schema(description = "Display sort order - ترتيب العرض", example = "10")
    private Integer sortOrder;

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
