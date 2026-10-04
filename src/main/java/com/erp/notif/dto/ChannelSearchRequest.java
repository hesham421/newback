package com.erp.notif.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-NOTIF-005 search request (POST /channels/search). NotificationChannelConfig is a flat/root
 * config entity, so there is no parent-id extractor override; filtering on channelTypeId (LIKE) and
 * isEnabled (EXACT) flows through the inherited generic filters consumed by the shared SpecBuilder.
 * No {@code @AllArgsConstructor} — the class adds zero fields, so it would collide with the no-arg
 * constructor; {@code @SuperBuilder} still provides full construction.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for NotificationChannelConfig - طلب بحث تهيئة القنوات")
public class ChannelSearchRequest extends BaseSearchContractRequest {
}
