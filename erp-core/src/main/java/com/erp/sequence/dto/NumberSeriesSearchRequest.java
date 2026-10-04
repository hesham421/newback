package com.erp.sequence.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * {@code POST /api/v1/sequence/series/search} body (erp-core step 09): the generic filters/sorts/paging.
 * No {@code @AllArgsConstructor}: the class adds no field (same reason as CU's search request).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for number series - طلب بحث سلاسل الترقيم")
public class NumberSeriesSearchRequest extends BaseSearchContractRequest {
}
