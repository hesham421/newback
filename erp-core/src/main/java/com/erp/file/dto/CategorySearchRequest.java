package com.erp.file.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-FILE-007 search request (POST /files/categories/search). FileCategory is a flat/root config
 * entity, so there is no parent-id extractor override; filtering on categoryCode/nameAr (LIKE) and
 * isActive (EXACT) flows through the inherited generic filters consumed by the shared SpecBuilder.
 * The sort/filter whitelist (ALLOWED_SORT_FIELDS = {categoryCode, nameAr, createdAt}) lives in
 * FileCategoryService. No {@code @AllArgsConstructor} — the class adds zero fields, so it would
 * collide with the no-arg constructor; {@code @SuperBuilder} still provides full construction.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for FileCategory - طلب بحث فئات الملفات")
public class CategorySearchRequest extends BaseSearchContractRequest {
}
