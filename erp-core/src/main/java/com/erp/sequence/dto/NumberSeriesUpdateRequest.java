package com.erp.sequence.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code PUT /api/v1/sequence/series/{id}} body (erp-core step 09). Excludes the immutable code, reset
 * policy and period, and the counter (only allocation moves it). Applied to every period row of the code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a number series - تعديل سلسلة ترقيم")
public class NumberSeriesUpdateRequest {

    @Size(max = 20, message = "{validation.size}")
    @Schema(description = "Value of the {PREFIX} token; null clears it - البادئة", example = "INV")
    private String prefix;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Pattern - نمط الترقيم", example = "{PREFIX}/{YY}/{SEQ:5}")
    private String pattern;
}
