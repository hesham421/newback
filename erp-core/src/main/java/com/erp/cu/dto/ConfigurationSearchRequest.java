package com.erp.cu.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-CU-002 request body (POST /search). No parent-id extractor override — AppConfiguration is
 * a root/flat entity (ROOT module, no child tables), unlike the child-entity variant in
 * build-create-dto. Filtering on configKey (LIKE) / isActive (EXACT) flows through the inherited
 * generic filters list, consumed by the shared SpecBuilder in ConfigurationService.search().
 *
 * <p>No {@code @AllArgsConstructor} here (unlike the skill's default child-DTO template): this
 * class adds zero fields of its own, so an all-args constructor would be a second, colliding
 * no-arg constructor. {@code @SuperBuilder} alone still provides full construction, including
 * inherited fields.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for Configuration - طلب بحث الإعدادات")
public class ConfigurationSearchRequest extends BaseSearchContractRequest {
}
