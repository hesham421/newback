package com.erp.tenant.crossmodule;

/** A tenant as other modules may read it (tenant-maturity D, {@link TenantLookupApi#summaryOf}): plain values only. */
public record TenantSummary(Long id, String code, String nameAr, String nameEn) {
}
