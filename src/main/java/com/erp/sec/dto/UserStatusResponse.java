package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The {@code {userPk, statusCode}} confirmation API-SEC-009 and API-SEC-010 return. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User status confirmation - تأكيد حالة المستخدم")
public class UserStatusResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long userPk;

    @Schema(description = "USER_STATUS code after the transition - رمز الحالة بعد الانتقال", example = "DISABLED")
    private String statusCode;
}
