package com.erp.cu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-CU-003 request body. Excludes configKey — immutable after creation (RULE-CU-003).
 *
 * <p>Carries an optional isActive, mapping API-CU-003's {@code isActiveFl?: Boolean} field —
 * SVC-API.md's field list is a literal DTO contract here (unlike the GET/POST search prose,
 * which was stale). This is the general Update operation's own reactivation path; it is not the
 * dedicated activate endpoint the module deliberately omits (no {@code PUT /{key}/activate}).
 * When present, the mapper applies it via AppConfiguration's own {@code activate()}/
 * {@code deactivate()} helpers — never a raw setter. Omitted (null) means "no change" (partial
 * update semantics).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a platform configuration entry - تحديث إعداد منصة")
public class ConfigurationUpdateRequest {

    @NotBlank(message = "{validation.required}")
    @Schema(description = "Configuration value - قيمة الإعداد", example = "smtp.example.com")
    private String configValue;

    @Size(max = 2000, message = "{validation.size}")
    @Schema(description = "Optional notes describing this configuration entry - ملاحظات اختيارية", example = "SMTP host used by the notification module")
    private String notes;

    @Schema(description = "Active status; omit to leave unchanged - حالة التفعيل، اتركه فارغاً لعدم التغيير", example = "true")
    private Boolean isActive;
}
