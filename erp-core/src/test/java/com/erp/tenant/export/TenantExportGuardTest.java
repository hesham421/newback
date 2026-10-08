package com.erp.tenant.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** tenant-maturity C5 (RULE-TENANT-028) — one export of a tenant at a time on a node; other tenants are independent. */
class TenantExportGuardTest {

    @Test
    void aTenantsSlotIsTakenOnce_untilItIsReleased_andOtherTenantsAreIndependent() {
        TenantExportGuard guard = new TenantExportGuard();

        assertThat(guard.tryStart(7L)).isTrue();
        assertThat(guard.tryStart(7L)).isFalse();
        assertThat(guard.tryStart(8L)).isTrue();
        assertThat(guard.isRunning(7L)).isTrue();
        guard.finish(7L);
        assertThat(guard.isRunning(7L)).isFalse();
        assertThat(guard.tryStart(7L)).isTrue();
        guard.finish(9L);
        assertThat(guard.isRunning(8L)).isTrue();
    }

    @Test
    void simultaneousStarts_letExactlyOneExportRun() throws Exception {
        TenantExportGuard guard = new TenantExportGuard();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Future<Boolean>> attempts = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                attempts.add(pool.submit(() -> {
                    start.await();
                    return guard.tryStart(42L);
                }));
            }
            start.countDown();
            int started = 0;
            for (Future<Boolean> attempt : attempts) {
                started += attempt.get() ? 1 : 0;
            }
            assertThat(started).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
