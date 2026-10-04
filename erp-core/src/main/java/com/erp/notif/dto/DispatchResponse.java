package com.erp.notif.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-NOTIF-001 dispatch response body. Carries the ids of the NOTIF_LOG rows created by the fan-out
 * (RULE-NOTIF-001). An inactive recipient (RULE-NOTIF-007) yields an empty list — no logs created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dispatch result - نتيجة الإرسال")
public class DispatchResponse {

    @Schema(description = "Ids of created notification log rows - معرّفات سجلات الإشعار المُنشأة")
    private List<Long> logIds;
}
