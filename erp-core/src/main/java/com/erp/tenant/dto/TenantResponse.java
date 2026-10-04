package com.erp.tenant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A tenant, as every platform-tenant endpoint returns it (business fields + audit). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tenant - مستأجر")
public class TenantResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "2")
    private Long id;

    @Schema(description = "Tenant code (X-Tenant-Code) - رمز المستأجر", example = "ACME")
    private String code;

    @Schema(description = "Tenant name (Arabic) - اسم المستأجر بالعربية", example = "شركة أكمي")
    private String nameAr;

    @Schema(description = "Tenant name (English) - اسم المستأجر بالإنجليزية", example = "Acme Ltd")
    private String nameEn;

    @Schema(description = "Status: ACTIVE or SUSPENDED - الحالة", example = "ACTIVE")
    private String statusCode;

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
