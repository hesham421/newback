package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-013 request body (SVC-API-CRUD.md): {@code {nameAr, nameEn, scheduleTypeCode,
 * frequencyCode?, startDate, endDate?, lines: [...]}}. Excludes {recurringTemplatePk,
 * nextRunDate, isActiveFl, audit} — {@code nextRunDate} is set to {@code startDate} by the
 * service, never submitted.
 *
 * <p>{@code frequencyCode} is optional at the structural level because its requiredness depends on
 * {@code scheduleTypeCode} (required unless {@code REVERSING}); that conditional check is a
 * service-layer input guard raising {@code FIN-400-MISSING-FREQUENCY}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a recurring journal template - إنشاء قالب قيد متكرر")
public class RecurringTemplateCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "إهلاك شهري")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Monthly depreciation")
    private String nameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 15, message = "{validation.size}")
    @Schema(description = "Schedule type, RECURRING_SCHEDULE_TYPE lookup - نوع الجدولة",
        example = "RECURRING")
    private String scheduleTypeCode;

    @Size(max = 15, message = "{validation.size}")
    @Schema(description = "Frequency, RECURRING_FREQUENCY lookup; required unless the schedule "
        + "type is REVERSING - التكرار", example = "MONTHLY")
    private String frequencyCode;

    @NotNull(message = "{validation.required}")
    @Schema(description = "First run date - تاريخ البداية", example = "2026-01-31")
    private LocalDate startDate;

    @Schema(description = "Last run date, open-ended when omitted - تاريخ النهاية",
        example = "2026-12-31")
    private LocalDate endDate;

    @NotEmpty(message = "{validation.required}")
    @Valid
    @Schema(description = "Template lines - سطور القالب")
    private List<RecurringTemplateLineCreateRequest> lines;
}
