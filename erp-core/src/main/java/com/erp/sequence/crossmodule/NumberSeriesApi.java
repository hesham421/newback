package com.erp.sequence.crossmodule;

/**
 * Formatted, tenant-scoped document numbers (erp-core step 09) — the sequence module's public surface.
 * Any module or application injects this interface (never the module's internals).
 *
 * <p>Series are configured per tenant ({@code CORE_NUMBER_SERIES}, admin API
 * {@code /api/v1/sequence/series}); applications seed theirs in their own migration ({@code V1000+}).
 * Nothing is created implicitly: an unknown or inactive code fails with {@code SEQUENCE_NOT_CONFIGURED}.
 * The current tenant ({@code com.erp.tenant.TenantContext}) is the one numbered.
 */
public interface NumberSeriesApi {

    /**
     * Allocates the next number of series {@code code} and returns it formatted, e.g.
     * {@code INV-2026-000123}. The allocation commits on its own ({@code REQUIRES_NEW}) and is atomic
     * under concurrency; if the caller's transaction rolls back, the number is not reused (a gap).
     */
    String next(String code);

    /** The number {@link #next} would return now, without consuming it. */
    String preview(String code);
}
