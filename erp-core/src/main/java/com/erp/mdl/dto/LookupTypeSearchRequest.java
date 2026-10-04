package com.erp.mdl.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-MDL-001 search body (POST /lookup-types/search). LookupType is a flat/root entity, so there
 * is no parent-id extractor override; filtering on {@code key} (LIKE), {@code ownerModuleCode}
 * (EXACT) and {@code isActiveFl} (EXACT) flows through the inherited generic filters consumed by
 * the shared {@code SpecBuilder} — the client now names the field and operator itself, same shape
 * as {@code ChannelSearchRequest}. No {@code @AllArgsConstructor} — the class adds zero fields, so
 * it would collide with the no-arg constructor; {@code @SuperBuilder} still provides full
 * construction.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for LookupType - طلب بحث أنواع اللوكب")
public class LookupTypeSearchRequest extends BaseSearchContractRequest {
}
