package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The {@code {activeSessionPk, terminatedAt}} confirmation API-SEC-026 returns. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Session termination confirmation - تأكيد إنهاء الجلسة")
public class SessionTerminationResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long activeSessionPk;

    @Schema(description = "Termination timestamp - تاريخ الإنهاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant terminatedAt;
}
