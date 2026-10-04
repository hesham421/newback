package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-001 response — every business field plus audit (A.3.7). Serves API-FIN-002 (201),
 * API-FIN-003 (200) and API-FIN-004 (200, {@code isActiveFl=false}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chart-of-accounts account - حساب في دليل الحسابات")
public class AccountResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long accountPk;

    @Schema(description = "Unique account code - رمز الحساب الفريد", example = "1101")
    private String code;

    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "النقدية بالصندوق")
    private String nameAr;

    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Cash on hand")
    private String nameEn;

    @Schema(description = "Account type code, ACCOUNT_TYPE lookup - نوع الحساب", example = "ASSET")
    private String accountTypeCode;

    @Schema(description = "Normal balance side, DEBIT_CREDIT lookup - طبيعة الحساب",
        example = "DEBIT")
    private String natureCode;

    @Schema(description = "Parent account id - معرّف الحساب الأب", example = "10")
    private Long parentAccountId;

    @Schema(description = "Accepts direct posting - يقبل الترحيل المباشر", example = "true")
    private Boolean isLeafFl;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Marks the single Retained Earnings account used by year-end close; "
        + "set once by the data seed (V34__fin_account_retained_earnings_seed.sql, DBF-FIN-147), "
        + "never settable through the account APIs by decision - "
        + "يُحدِّد حساب الأرباح المُبقاة الوحيد المستخدم في إقفال نهاية السنة", example = "false")
    private Boolean isRetainedEarningsFl;

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
