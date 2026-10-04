package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-015 response — every business field, the account's display columns (the mapping stores
 * no name of its own, ADR-FIN-031) and audit (A.3.7). Serves API-FIN-039 (201), API-FIN-040 (200)
 * and API-FIN-041 (200, {@code isActiveFl=false}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account mapping - ربط حساب")
public class AccountMappingResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long accountMappingPk;

    @Schema(description = "Event type, ACCOUNTING_EVENT_TYPE lookup - نوع الحدث",
        example = "CASH_PAYMENT")
    private String eventTypeCode;

    @Schema(description = "Business field, FIN_EVENT_BUSINESS_FIELD lookup - الحقل التجاري",
        example = "PAYMENT_METHOD")
    private String businessFieldCode;

    @Schema(description = "Business value stored as a code - القيمة التجارية", example = "CHEQUE")
    private String businessValue;

    @Schema(description = "Mapped account id - معرّف الحساب", example = "12")
    private Long accountId;

    @Schema(description = "Mapped account code - رمز الحساب", example = "1101")
    private String accountCode;

    @Schema(description = "Mapped account name (Arabic) - اسم الحساب بالعربية",
        example = "النقدية بالصندوق")
    private String accountNameAr;

    @Schema(description = "Mapped account name (English) - اسم الحساب بالإنجليزية",
        example = "Cash on hand")
    private String accountNameEn;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant updatedAt;
}
