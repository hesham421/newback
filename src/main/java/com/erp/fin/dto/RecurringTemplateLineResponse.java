package com.erp.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-FIN-012 response — every business field, the parent's id, and the single audit column the
 * table carries ({@code createdAt}, DBF-FIN-129).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recurring template line - سطر قالب متكرر")
public class RecurringTemplateLineResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long recurringTemplateLinePk;

    @Schema(description = "Parent template id - معرّف القالب الأب", example = "1")
    private Long recurringTemplateId;

    @Schema(description = "Line position within the template - رقم السطر", example = "1")
    private Integer lineNo;

    @Schema(description = "Target account id - معرّف الحساب", example = "12")
    private Long accountId;

    @Schema(description = "Line amount, always positive - المبلغ", example = "1500.0000")
    private BigDecimal amount;

    @Schema(description = "Direction, DEBIT_CREDIT lookup - الاتجاه", example = "DEBIT")
    private String directionCode;

    @Schema(description = "Optional dimension value id - معرّف قيمة البُعد", example = "5")
    private Long dimensionValueId;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        timezone = "UTC")
    private Instant createdAt;
}
