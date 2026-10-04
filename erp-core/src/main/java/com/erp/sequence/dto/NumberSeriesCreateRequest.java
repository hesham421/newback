package com.erp.sequence.dto;

import com.erp.sequence.domain.ResetPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** {@code POST /api/v1/sequence/series} body (erp-core step 09). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a number series - إنشاء سلسلة ترقيم")
public class NumberSeriesCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "{validation.pattern}")
    @Schema(description = "Series code, unique per tenant (stored upper case) - رمز السلسلة", example = "SALES_INVOICE")
    private String code;

    @Size(max = 20, message = "{validation.size}")
    @Schema(description = "Value of the {PREFIX} token - البادئة", example = "INV")
    private String prefix;

    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Pattern; tokens {PREFIX} {YYYY} {YY} {MM} {SEQ:n} {TENANT}; default {PREFIX}-{YYYY}-{SEQ:6}"
        + " - نمط الترقيم", example = "{PREFIX}-{YYYY}-{SEQ:6}")
    private String pattern;

    @Schema(description = "Reset policy (default YEARLY; immutable) - سياسة إعادة الترقيم", example = "YEARLY")
    private ResetPolicy resetPolicy;

    @Min(value = 1, message = "{validation.min}")
    @Schema(description = "First value of the current period (default 1) - القيمة الأولى", example = "1")
    private Long nextValue;
}
