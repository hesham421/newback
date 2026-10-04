package com.erp.sequence.crossmodule;

import com.erp.sequence.service.NumberAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link NumberSeriesApi} delegating to the module's {@link NumberAllocationService}, which owns the
 * transactions: {@code next} runs in {@code REQUIRES_NEW} (the number commits independently of the
 * caller, so concurrent callers never wait on each other's business transactions), {@code preview} is a
 * read-only transaction.
 */
@Component
@RequiredArgsConstructor
public class NumberSeriesApiImpl implements NumberSeriesApi {

    private final NumberAllocationService allocationService;

    @Override
    public String next(String code) {
        return allocationService.next(code);
    }

    @Override
    public String preview(String code) {
        return allocationService.preview(code);
    }
}
