package com.erp.mdl.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-MDL-010 search body (POST /lookup-types/by-owner/search). {@code ownerModuleCode} (EXACT)
 * and {@code key} (LIKE) flow through the inherited generic filters exactly like
 * {@link LookupTypeSearchRequest}; {@code isActiveFl} is deliberately absent here — QR-MDL-010
 * restricts this browse to active types unconditionally, so the service ANDs that predicate in
 * itself rather than exposing it as a client-supplied filter (same pattern as
 * {@code ActiveSessionSearchRequest}'s unconditional {@code terminatedAt IS NULL}). {@code page} /
 * {@code size} / {@code sortField} are inherited but unused — the endpoint's response is a flat,
 * ungrouped-by-page {@code List<OwnerGroupResponse>}, not a {@code Page<T>}, same as before this
 * REST decision. No {@code @AllArgsConstructor} — the class adds zero fields, so it would collide
 * with the no-arg constructor.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for browsing LookupType registry by owner - طلب استعراض سجل أنواع اللوكب حسب المالك")
public class LookupTypeByOwnerSearchRequest extends BaseSearchContractRequest {
}
