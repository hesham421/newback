package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-002 request body (SVC-API-CRUD.md): {@code {code, nameAr, nameEn, accountTypeCode,
 * natureCode, parentAccountId?, isLeafFl}}. Excludes {accountPk, isActiveFl, audit} — a new
 * account is always created active.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a chart-of-accounts account - إنشاء حساب في دليل الحسابات")
public class AccountCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 30, message = "{validation.size}")
    @Schema(description = "Unique account code - رمز الحساب الفريد", example = "1101")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "النقدية بالصندوق")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Cash on hand")
    private String nameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Schema(description = "Account type code, ACCOUNT_TYPE lookup - نوع الحساب", example = "ASSET")
    private String accountTypeCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Schema(description = "Normal balance side, DEBIT_CREDIT lookup - طبيعة الحساب",
        example = "DEBIT")
    private String natureCode;

    @Schema(description = "Parent account id, omitted for a root account - معرّف الحساب الأب",
        example = "10")
    private Long parentAccountId;

    @Schema(description = "Accepts direct posting - يقبل الترحيل المباشر", example = "true")
    @Builder.Default
    private Boolean isLeafFl = Boolean.TRUE;
}
