package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-010 request body (SVC-API-CRUD.md): {@code {eventTypeCode, nameAr, nameEn}}. Excludes
 * {eventTypeRulePk, isActiveFl, audit}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create an accounting event-type rule - إنشاء قاعدة نوع حدث محاسبي")
public class EventTypeRuleCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Event type code, ACCOUNTING_EVENT_TYPE lookup - رمز نوع الحدث",
        example = "SALES_INVOICE")
    private String eventTypeCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "فاتورة مبيعات")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Sales invoice")
    private String nameEn;
}
