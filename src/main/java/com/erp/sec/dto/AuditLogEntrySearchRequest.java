package com.erp.sec.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import com.erp.common.search.SearchRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-SEC-023 search body. {@code actor} is an association (DBF-SEC-085), so the client filters on
 * the scalar {@code actorUserId}, which is lifted out of the generic set and resolved to a User
 * reference by the service. No {@code @AllArgsConstructor} — the class adds no field of its own, so
 * it would collide with the no-arg constructor.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for AuditLogEntry - طلب بحث سجل التدقيق")
public class AuditLogEntrySearchRequest extends BaseSearchContractRequest {

    private static final String ACTOR_USER_ID = "actorUserId";

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(ACTOR_USER_ID));
    }

    /**
     * Not a settable request field — {@code filters: [{"field":"actorUserId",...}]} is the client
     * contract (see class doc). Hidden from the OpenAPI schema so bean introspection doesn't
     * publish it as though it were an independent top-level field a client can POST directly.
     */
    @Schema(hidden = true)
    public Long getActorUserId() {
        return extractLongFilter(ACTOR_USER_ID);
    }
}
