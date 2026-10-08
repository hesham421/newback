package com.erp.tenant.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.tenant.export.TenantExportGuard.Start;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** tenant-maturity C5 (RULE-TENANT-028) — one export of a tenant at a time on a node, at most {@code maxConcurrent} in all. */
class TenantExportGuardTest {

    @Test
    void aTenantsSlotIsTakenOnce_untilItIsReleased_andOtherTenantsAreIndependent() {
        TenantExportGuard guard = new TenantExportGuard();

        assertThat(guard.tryStart(7L, 10)).isEqualTo(Start.STARTED);
        assertThat(guard.tryStart(7L, 10)).isEqualTo(Start.ALREADY_RUNNING);
        assertThat(guard.tryStart(8L, 10)).isEqualTo(Start.STARTED);
        assertThat(guard.isRunning(7L)).isTrue();
        guard.finish(7L);
        assertThat(guard.isRunning(7L)).isFalse();
        assertThat(guard.tryStart(7L, 10)).isEqualTo(Start.STARTED);
        guard.finish(9L);
        assertThat(guard.isRunning(8L)).isTrue();
    }

    @Test
    void atTheConcurrencyLimit_anotherTenantIsBusy_theRunningTenantStaysAlreadyRunning_andAReleaseFreesASlot() {
        TenantExportGuard guard = new TenantExportGuard();
        assertThat(guard.tryStart(1L, 2)).isEqualTo(Start.STARTED);
        assertThat(guard.tryStart(2L, 2)).isEqualTo(Start.STARTED);

        assertThat(guard.tryStart(3L, 2)).isEqualTo(Start.BUSY);
        assertThat(guard.isRunning(3L)).as("a refused start takes no slot").isFalse();
        assertThat(guard.tryStart(1L, 2)).isEqualTo(Start.ALREADY_RUNNING);
        guard.finish(2L);
        assertThat(guard.tryStart(3L, 2)).isEqualTo(Start.STARTED);
    }

    @Test
    void simultaneousStarts_letExactlyOneExportRun() throws Exception {
        TenantExportGuard guard = new TenantExportGuard();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Future<Start>> attempts = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                long tenant = 42L + (i % 2) * 1000L;
                attempts.add(pool.submit(() -> {
                    start.await();
                    return guard.tryStart(tenant, 1);
                }));
            }
            start.countDown();
            int started = 0;
            for (Future<Start> attempt : attempts) {
                started += attempt.get() == Start.STARTED ? 1 : 0;
            }
            assertThat(started).as("one tenant slot and a limit of one: exactly one export runs").isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
