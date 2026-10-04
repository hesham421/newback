package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-013 response — every business field, the persisted target set, its count, plus audit
 * (A.3.7). Serves API-FIN-016 (201).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cost-allocation rule - قاعدة توزيع تكلفة")
public class AllocationRuleResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long allocationRulePk;

    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "توزيع مصاريف الإدارة")
    private String nameAr;

    @Schema(description = "Name (English) - الاسم بالإنجليزية",
        example = "Administrative expense allocation")
    private String nameEn;

    @Schema(description = "Source account id - معرّف الحساب المصدر", example = "31")
    private Long sourceAccountId;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Number of allocation targets - عدد الأهداف", example = "3")
    private Integer targetCount;

    @Schema(description = "Allocation targets - أهداف التوزيع")
    private List<AllocationTargetResponse> targets;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;
}
