package com.erp.cu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Platform configuration entry - إعداد منصة")
public class ConfigurationResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long id;

    @Schema(description = "Unique configuration key - مفتاح الإعداد الفريد", example = "MAIL_SMTP_HOST")
    private String configKey;

    @Schema(description = "Configuration value - قيمة الإعداد", example = "smtp.example.com")
    private String configValue;

    @Schema(description = "Optional notes describing this configuration entry - ملاحظات اختيارية", example = "SMTP host used by the notification module")
    private String notes;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActive;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;
}
