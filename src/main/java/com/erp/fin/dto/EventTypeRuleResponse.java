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
 * ENT-FIN-009 response — every business field, the persisted line set, its count, plus audit
 * (A.3.7). Serves API-FIN-010 (201), API-FIN-009 and API-FIN-034.
 *
 * <p>{@code lines} was added 2026-09-19. Until then ENT-FIN-010's seven fields were reachable only
 * as the response of API-FIN-011 — the line the caller had just created — so a rule could be
 * written and never read back: SCR-FIN-003's Detail pane had no source, and RULE-FIN-003 (exactly
 * one remainder line per rule) could not be checked by a client before it submitted. The shape
 * follows the two aggregates that already carry their children on their own search response,
 * {@code RecurringTemplateResponse.lines} and {@code AllocationRuleResponse.targets}, which is why
 * no by-id read was added instead.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Accounting event-type rule - قاعدة نوع حدث محاسبي")
public class EventTypeRuleResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long eventTypeRulePk;

    @Schema(description = "Event type code, ACCOUNTING_EVENT_TYPE lookup - رمز نوع الحدث",
        example = "SALES_INVOICE")
    private String eventTypeCode;

    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "فاتورة مبيعات")
    private String nameAr;

    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Sales invoice")
    private String nameEn;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Number of rule lines - عدد سطور القاعدة", example = "2")
    private Integer lineCount;

    @Schema(description = "Rule lines, ordered by lineNo - سطور القاعدة مرتبة حسب رقم السطر")
    private List<RuleLineResponse> lines;

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
