package com.erp.notif.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-NOTIF-001 dispatch request body. The sender chooses target channels via {@code channelHint}
 * (LOV-NOTIF-001 codes); dispatch fans out one NOTIF_LOG per requested channel (RULE-NOTIF-001).
 * {@code variables} feed template placeholder substitution. Excludes id and audit fields — dispatch
 * creates the log rows internally, never accepts them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dispatch a notification (fan-out) - إرسال إشعار (تفرّع)")
public class DispatchRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Recipient UserAccount id (SEC) - معرّف المستلِم", example = "42")
    private Long recipientId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 80, message = "{validation.size}")
    @Schema(description = "Template code to resolve - رمز القالب", example = "PASSWORD_RESET")
    private String templateCode;

    @NotEmpty(message = "{validation.required}")
    @Schema(description = "Requested channels (LOV-NOTIF-001) - القنوات المطلوبة", example = "[\"EMAIL\",\"SMS\"]")
    private List<@NotBlank(message = "{validation.required}") @Size(max = 20, message = "{validation.size}") String> channelHint;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Sending module code - رمز الموديول المُرسِل", example = "SEC")
    private String moduleCode;

    @Schema(description = "Source entity reference id - معرّف المرجع", example = "1001")
    private Long referenceId;

    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Source entity reference type - نوع المرجع", example = "USER_ACCOUNT")
    private String referenceType;

    @Schema(description = "Template placeholder variables - متغيرات القالب")
    private Map<String, String> variables;
}
