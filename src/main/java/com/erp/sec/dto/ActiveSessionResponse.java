package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-SEC-010 response for API-SEC-025. {@code tokenRef} is deliberately absent and never
 * serialized (DATA-DOM-TRANSACTIONAL.md ENT-SEC-010 DTO MEMBERSHIP).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Active session - جلسة نشطة")
public class ActiveSessionResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long activeSessionPk;

    @Schema(description = "Session owner id - معرف صاحب الجلسة", example = "1")
    private Long userId;

    @Schema(description = "Session owner login - اسم دخول صاحب الجلسة", example = "u2")
    private String username;

    @Schema(description = "Session start timestamp - تاريخ بدء الجلسة")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant startedAt;

    @Schema(description = "Last activity timestamp - تاريخ آخر نشاط")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant lastActivityAt;

    @Schema(description = "Client IP address - عنوان الـ IP", example = "10.0.0.8")
    private String ipAddress;
}
