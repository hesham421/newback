package com.erp.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tenant-maturity B (REQ-TENANT-025) — {@code PUT /api/v1/platform/tenants/{id}}: the editable names and
 * profile, a full replacement (an absent or empty optional field is cleared). No {@code code} (immutable,
 * RULE-TENANT-003) and no {@code statusCode} (its own endpoint); a JSON field of either name is ignored.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a tenant's names and profile - تعديل أسماء المستأجر وملفه")
public class TenantUpdateRequest {

    /** Same format as SEC's user phone: optional {@code +}, digits, spaces, hyphens; 7..30 characters. */
    static final String PHONE_PATTERN = "^$|^\\+?[0-9][0-9 -]{5,28}[0-9]$";
    static final String COUNTRY_PATTERN = "^$|^[A-Z]{2}$";
    static final String LOCALE_PATTERN = "^$|^(ar|en)$";
    static final String TIMEZONE_PATTERN = "^$|^[A-Za-z][A-Za-z0-9_+-]*(/[A-Za-z0-9_+-]+)*$";

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Tenant name (Arabic) - اسم المستأجر بالعربية", example = "شركة أكمي")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Tenant name (English) - اسم المستأجر بالإنجليزية", example = "Acme Ltd")
    private String nameEn;

    @Email(message = "{validation.invalid}")
    @Size(max = 255, message = "{validation.size}")
    @Schema(description = "Contact e-mail - بريد التواصل", example = "contact@acme.example")
    private String contactEmail;

    @Pattern(regexp = PHONE_PATTERN, message = "{validation.pattern}")
    @Size(max = 30, message = "{validation.size}")
    @Schema(description = "Contact phone: optional +, digits, spaces, hyphens - هاتف التواصل", example = "+966 11 555 0100")
    private String contactPhone;

    @Pattern(regexp = COUNTRY_PATTERN, message = "{validation.pattern}")
    @Schema(description = "ISO 3166-1 alpha-2 country code, upper case - رمز الدولة", example = "SA")
    private String countryCode;

    @Pattern(regexp = LOCALE_PATTERN, message = "{validation.pattern}")
    @Schema(description = "Default UI language: ar or en - اللغة الافتراضية", example = "ar")
    private String defaultLocale;

    @Pattern(regexp = TIMEZONE_PATTERN, message = "{validation.pattern}")
    @Size(max = 64, message = "{validation.size}")
    @Schema(description = "IANA time-zone id - المنطقة الزمنية", example = "Asia/Riyadh")
    private String timezone;

    @Size(max = 1000, message = "{validation.size}")
    @Schema(description = "Operator's notes - ملاحظات", example = "Pilot customer, invoiced yearly")
    private String notes;
}
