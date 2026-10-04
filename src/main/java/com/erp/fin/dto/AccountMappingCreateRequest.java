package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-039 request body (SVC-API-CRUD.md): {@code {eventTypeCode, businessFieldCode,
 * businessValue, accountId}}. Excludes {accountMappingPk, isActiveFl, audit} — a new mapping is
 * always created active.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create an account mapping - إنشاء ربط حساب")
public class AccountMappingCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Event type, ACCOUNTING_EVENT_TYPE lookup - نوع الحدث",
        example = "CASH_PAYMENT")
    private String eventTypeCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Business field, FIN_EVENT_BUSINESS_FIELD lookup - الحقل التجاري",
        example = "PAYMENT_METHOD")
    private String businessFieldCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Business value stored as a code - القيمة التجارية", example = "CHEQUE")
    private String businessValue;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Mapped account id, an active leaf account - معرّف الحساب",
        example = "12")
    private Long accountId;
}
