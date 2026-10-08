package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** tenant-maturity D — the result of an admin-set (REQ-SEC-083) or a self-change (REQ-SEC-085). No secret. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password change result - نتيجة تغيير كلمة المرور")
public class PasswordChangeResponse {

    @Schema(description = "User identifier - معرّف المستخدم", example = "12")
    private Long userPk;

    @Schema(description = "Whether the user must still change the password - هل يلزم المستخدم تغيير كلمة المرور", example = "true")
    private Boolean passwordChangeRequired;

    @Schema(description = "When the password was set - وقت تعيين كلمة المرور")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant passwordChangedAt;

    @Schema(description = "Sessions of the user this change terminated - عدد جلسات المستخدم التي أُنهيت", example = "1")
    private Integer sessionsTerminated;
}
