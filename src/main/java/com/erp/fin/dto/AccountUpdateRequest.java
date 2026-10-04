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
 * API-FIN-003 request body (SVC-API-CRUD.md): {@code {nameAr, nameEn, isLeafFl}} — explicitly
 * excludes {accountPk, code, accountTypeCode, natureCode, isActiveFl, audit}. {@code code} is the
 * natural key and {@code parentAccountId} the FK: both immutable (A.3.6). {@code isActiveFl} moves
 * only through API-FIN-004.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update an account - تعديل حساب")
public class AccountUpdateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "النقدية بالصندوق")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Cash on hand")
    private String nameEn;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Accepts direct posting - يقبل الترحيل المباشر", example = "true")
    private Boolean isLeafFl;
}
