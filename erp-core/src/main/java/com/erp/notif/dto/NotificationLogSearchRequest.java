package com.erp.notif.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-NOTIF-002 search request (POST /logs/search). NotificationLog is a flat/root transactional
 * entity, so there is no parent-id extractor override; the generic filters consumed by the shared
 * SpecBuilder support recipientId / moduleCode / channelTypeId / notificationStatusId (EXACT),
 * referenceType (EXACT/LIKE) and a sentAt date range (GREATER_THAN_OR_EQUAL / LESS_THAN_OR_EQUAL),
 * whitelisted by the service's ALLOWED_FILTER_FIELDS. Empty result → 200 empty page (never 404).
 * No {@code @AllArgsConstructor} — the class adds zero fields, so it would collide with the no-arg
 * constructor; {@code @SuperBuilder} still provides full construction.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for NotificationLog - طلب بحث سجل الإشعارات")
public class NotificationLogSearchRequest extends BaseSearchContractRequest {
}
