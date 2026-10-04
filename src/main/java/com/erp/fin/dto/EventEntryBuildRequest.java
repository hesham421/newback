package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-020 request body (SVC-API-INT.md) — "the canonical accounting event payload (opaque
 * shape, out-of-scope Event consumer, POL-FIN-020)". Because the plan declares the shape opaque
 * and out of scope, this class fixes the minimum FIN itself must read to run the rule engine;
 * nothing here invents a column, a lookup key or a rule; v2 fixes the {@code fields} contract
 * (CORE.md "Event business fields", ADR-FIN-031).
 *
 * <p>What each field feeds:
 * <ul>
 *   <li>{@code eventReference} — RULE-FIN-004's idempotency key, stored on DBF-FIN-041;</li>
 *   <li>{@code eventTypeCode} — RULE-FIN-005's lookup into the active {@code EventTypeRule}
 *       (ACCOUNTING_EVENT_TYPE, DBF-FIN-090);</li>
 *   <li>{@code docDate} — the entry's document date, and the date whose fiscal period and year
 *       the entry posts into (RULE-FIN-008 then gates that period);</li>
 *   <li>{@code baseAmount} — the total a {@code PERCENTAGE} rule line takes its share of and the
 *       total a {@code REMAINDER} line completes (RULE-FIN-010);</li>
 *   <li>{@code amounts} — the event's named amount fields, read by an {@code AMOUNT_SOURCE_TYPE
 *       = FIELD} rule line through its {@code amountSourceValue} (DBF-FIN-104);</li>
 *   <li>{@code fields} — the event's business facts keyed by FIN_EVENT_BUSINESS_FIELD code: a
 *       MAPPING rule line reads the field its {@code accountBusinessFieldCode} names (DBF-FIN-148)
 *       and a BUSINESS_FIELD dimension tag the one its {@code businessFieldCode} names
 *       (DBF-FIN-161); no account code is ever read from the payload (REQ-FIN-047).</li>
 * </ul>
 *
 * <p>{@code docNo}, {@code statusCode}, {@code postedAt} and every audit field are system-assigned
 * and appear in no request body (A.3.5).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Accounting event payload to build and post an entry from - "
    + "حمولة حدث محاسبي لبناء وترحيل قيد")
public class EventEntryBuildRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Event idempotency reference - المرجع الفريد للحدث",
        example = "SALES-INV-2026-000811")
    private String eventReference;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Event type, ACCOUNTING_EVENT_TYPE lookup - نوع الحدث المحاسبي",
        example = "SALES_INVOICE")
    private String eventTypeCode;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Document date; selects the target fiscal period - تاريخ المستند",
        example = "2026-01-31")
    private LocalDate docDate;

    @NotNull(message = "{validation.required}")
    @PositiveOrZero(message = "{validation.min}")
    @Schema(description = "Event base amount the rule distributes - المبلغ الأساسي للحدث",
        example = "1000.0000")
    private BigDecimal baseAmount;

    @Schema(description = "Named amount fields of the event - حقول المبالغ المسماة للحدث")
    private Map<String, BigDecimal> amounts;

    @Schema(description = "Business facts of the event, keyed by FIN_EVENT_BUSINESS_FIELD code "
        + "(PAYMENT_METHOD, EXPENSE_TYPE_CODE, ORGANISATION_CODE, BRANCH_CODE); the key set grows as "
        + "MDL data, not as a new request field. No account code is read from the payload - "
        + "الحقائق التجارية للحدث مفتاحها رمز FIN_EVENT_BUSINESS_FIELD")
    private Map<String, String> fields;

    @Schema(description = "Entry description (Arabic) - وصف القيد بالعربية",
        example = "فاتورة مبيعات")
    private String descriptionAr;

    @Schema(description = "Entry description (English) - وصف القيد بالإنجليزية",
        example = "Sales invoice")
    private String descriptionEn;
}
