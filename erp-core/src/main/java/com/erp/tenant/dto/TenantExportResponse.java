package com.erp.tenant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tenant-maturity C5 (REQ-TENANT-037) — the stored export archive and its single-use download token, for
 * {@code GET /api/v1/files/download?token=} by the same platform operator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A tenant's data export: the stored archive and its download token - تصدير بيانات المستأجر: الملف المخزّن ورمز تنزيله")
public class TenantExportResponse {

    @Schema(description = "Exported tenant ID - معرّف المستأجر المصدَّر", example = "2")
    private Long tenantId;

    @Schema(description = "Exported tenant code - رمز المستأجر المصدَّر", example = "ACME")
    private String tenantCode;

    @Schema(description = "ID of the PRIVATE file document of the PLATFORM tenant holding the ZIP - معرّف مستند الملف المضغوط", example = "41")
    private Long fileId;

    @Schema(description = "Name of the ZIP - اسم الملف المضغوط", example = "tenant-export-ACME-20261008T093000Z.zip")
    private String fileName;

    @Schema(description = "Size of the ZIP in bytes - حجم الملف بالبايت", example = "48213")
    private long sizeBytes;

    @Schema(description = "Rows exported, all CSV files together - عدد السجلات المصدَّرة", example = "1520")
    private long rowCount;

    @Schema(description = "Single-use download token (10 minutes, bound to the caller) for GET /api/v1/files/download?token="
        + " - رمز تنزيل يُستخدم مرة واحدة", example = "q2c3...")
    private String downloadToken;

    @Schema(description = "When the download token expires (UTC) - انتهاء صلاحية رمز التنزيل", example = "2026-10-08T09:40:00.000Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant downloadTokenExpiresAt;
}
