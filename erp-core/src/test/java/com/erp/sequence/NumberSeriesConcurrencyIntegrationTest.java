package com.erp.sequence;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.sequence.crossmodule.NumberSeriesApi;
import com.erp.sequence.domain.ResetPolicy;
import com.erp.sequence.entity.NumberSeries;
import com.erp.sequence.repository.NumberSeriesRepository;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * erp-core step 09, task 6 — 50 threads calling {@code next("T")} at the same moment get 50 unique,
 * consecutive numbers (the row lock serialises them; nothing is lost or handed out twice).
 * Not transactional: every allocation commits in its own {@code REQUIRES_NEW} transaction, each thread
 * runs as the PLATFORM tenant.
 */
class NumberSeriesConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final int THREADS = 50;

    @Autowired
    private NumberSeriesApi numberSeriesApi;
    @Autowired
    private NumberSeriesRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void freshSeriesT() {
        jdbcTemplate.update("delete from core_number_series where tenant_id = 1 and code = 'T'");
        repository.save(NumberSeries.builder().code("T").prefix("T").pattern("{PREFIX}-{SEQ:4}")
            .resetPolicy(ResetPolicy.NEVER).periodKey("").nextValue(1L).build());
    }

    @Test
    void fiftyConcurrentCalls_produceFiftyUniqueConsecutiveNumbers() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                Callable<String> call = () -> {
                    start.await();
                    return TenantContext.callAs(TenantConstants.PLATFORM_TENANT_ID, () -> numberSeriesApi.next("T"));
                };
                futures.add(pool.submit(call));
            }
            start.countDown();
            List<String> numbers = new ArrayList<>();
            for (Future<String> future : futures) {
                numbers.add(future.get(60, TimeUnit.SECONDS));
            }

            List<String> expected = IntStream.rangeClosed(1, THREADS).mapToObj(n -> String.format("T-%04d", n)).toList();
            assertThat(numbers).doesNotHaveDuplicates().hasSize(THREADS).containsExactlyInAnyOrderElementsOf(expected);
            assertThat(jdbcTemplate.queryForObject(
                "select next_value from core_number_series where tenant_id = 1 and code = 'T'", Long.class))
                .isEqualTo(THREADS + 1L);
            assertThat(jdbcTemplate.queryForObject(
                "select count(*) from core_number_series where tenant_id = 1 and code = 'T'", Integer.class)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
