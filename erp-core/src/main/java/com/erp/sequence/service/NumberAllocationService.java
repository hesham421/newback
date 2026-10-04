package com.erp.sequence.service;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sequence.domain.NumberSeriesDomain;
import com.erp.sequence.entity.NumberSeries;
import com.erp.sequence.exception.SequenceErrorCodes;
import com.erp.sequence.mapper.NumberSeriesMapper;
import com.erp.sequence.repository.NumberSeriesRepository;
import com.erp.tenant.TenantContext;
import com.erp.tenant.crossmodule.TenantLookupApi;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draws numbers from the current tenant's number series (erp-core step 09) — the engine behind
 * {@code com.erp.sequence.crossmodule.NumberSeriesApi}.
 *
 * <p><b>Atomicity.</b> {@link #next} runs in its own {@code REQUIRES_NEW} transaction: it locks the
 * series' anchor row ({@code SELECT ... FOR UPDATE}, tenant predicate added by Hibernate), then the row of
 * the current period (creating it, at 1, when the period is new), takes the counter and commits. Two
 * allocations of one code never see the same value, and the values of one period are consecutive. A
 * caller whose own transaction later rolls back does not give its number back, so a business document
 * sequence may have gaps across rolled-back transactions — accepted by the plan (strict gap-free
 * numbering is a business-module concern). Because of {@code REQUIRES_NEW}, a caller inside a
 * transaction holds two connections for the duration of the call.
 *
 * <p><b>Tenant.</b> The new transaction opens its session under {@link TenantContext}'s current tenant,
 * so the caller's tenant is the one numbered; a caller without a tenant fails with
 * {@code TENANT_CONTEXT_MISSING}.
 *
 * <p><b>Dates.</b> The period and the {@code {YYYY}/{YY}/{MM}} tokens use the application's
 * {@link Clock} bean when there is one, otherwise the JVM's default time zone.
 *
 * <p>No {@code @PreAuthorize}: an in-process library service other modules call while doing their own,
 * already authorized work (the same precedent as CU's former internal {@code getValue}); it is never
 * bound to a controller — the admin API is {@link NumberSeriesService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NumberAllocationService {

    private final NumberSeriesRepository repository;
    private final NumberSeriesMapper mapper;
    private final TenantLookupApi tenantLookup;
    private final ObjectProvider<Clock> clock;

    /** Allocates and formats the next number of series {@code code} (committed before it returns). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String code) {
        String normalized = normalize(code);
        log.debug("Allocating a number of series {}", normalized);

        // 1. Lock the anchor: serialises every allocation of this code in this tenant.
        NumberSeries anchor = repository.findFirstByCodeOrderByIdAsc(normalized)
            .orElseThrow(() -> notConfigured(code));
        NumberSeriesDomain domain = NumberSeriesDomain.from(anchor);
        domain.assertCanAllocate();

        // 2. The current period's row — locked; created at 1 when the period is new (YEARLY/MONTHLY reset).
        LocalDate today = today();
        String period = domain.periodKey(today);
        NumberSeries current = period.equals(anchor.getPeriodKey())
            ? anchor
            : repository.findByCodeAndPeriodKey(normalized, period)
                .orElseGet(() -> repository.save(mapper.toNewPeriod(anchor, period)));

        // 3. Take the value; the UPDATE is flushed at commit, while the row lock is still held.
        long value = current.takeNextValue();
        return domain.format(today, value, tenantCode(domain));
    }

    /** The number {@link #next} would return now, without consuming it (no lock, nothing written). */
    @Transactional(readOnly = true)
    public String preview(String code) {
        String normalized = normalize(code);
        log.debug("Previewing the next number of series {}", normalized);

        NumberSeries anchor = repository.readFirstByCodeOrderByIdAsc(normalized)
            .orElseThrow(() -> notConfigured(code));
        NumberSeriesDomain domain = NumberSeriesDomain.from(anchor);
        domain.assertCanAllocate();

        LocalDate today = today();
        String period = domain.periodKey(today);
        long value = period.equals(anchor.getPeriodKey())
            ? anchor.getNextValue()
            : repository.readByCodeAndPeriodKey(normalized, period).map(NumberSeries::getNextValue).orElse(1L);
        return domain.format(today, value, tenantCode(domain));
    }

    private String tenantCode(NumberSeriesDomain domain) {
        return domain.needsTenantCode() ? tenantLookup.codeOf(TenantContext.require()).orElse("") : null;
    }

    private LocalDate today() {
        return LocalDate.now(clock.getIfAvailable(Clock::systemDefaultZone));
    }

    private static LocalizedException notConfigured(String code) {
        return new LocalizedException(Status.BUSINESS_RULE_VIOLATION, SequenceErrorCodes.SEQUENCE_NOT_CONFIGURED, code);
    }

    static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
