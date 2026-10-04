package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-040 request body (SVC-API-CRUD.md): {@code {accountId}} and nothing else. The key
 * (eventTypeCode, businessFieldCode, businessValue) is fixed after create (ADR-FIN-024, A.3.6);
 * {@code isActiveFl} moves only through API-FIN-041.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Change the account of a mapping - تغيير حساب الربط")
public class AccountMappingUpdateRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Mapped account id, an active leaf account - معرّف الحساب",
        example = "12")
    private Long accountId;
}
