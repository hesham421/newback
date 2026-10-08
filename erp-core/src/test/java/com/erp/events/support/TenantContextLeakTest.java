package com.erp.events.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * tenant-maturity C6 (ADR-TENANT-004, M1): {@code runAs}/{@code callAs} (failing, nested), the event decorator and new
 * threads. Only POOLED_PLATFORM reuses a thread, so only it can show a leak into the next task; VIRTUAL runs each task
 * on a new thread (a virtual thread is never reused, and a {@code ThreadLocal} belongs to it, not to its carrier), so
 * the VIRTUAL variants pin the in-task semantics only.
 */
class TenantContextLeakTest {

    enum Threads { POOLED_PLATFORM, VIRTUAL }

    private ExecutorService executor;

    @AfterEach
    void cleanUp() throws InterruptedException {
        TenantContext.clear();
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @ParameterizedTest(name = "{0}: in-task restore; next-task leak checked on POOLED_PLATFORM only")
    @EnumSource(Threads.class)
    void aFailingOrNestedCallAs_restoresInTheTask_andLeavesNoTenantForTheNextTaskOnAPooledThread(Threads threads)
            throws Exception {
        executor = executorOf(threads);
        String firstThread = run(() -> {
            assertThatThrownBy(() -> TenantContext.runAs(5L, () -> {
                TenantContext.runAs(6L, () -> {
                    throw new IllegalStateException("inner");
                });
            })).hasMessage("inner");
            assertThat(TenantContext.callAs(5L, () -> TenantContext.callAs(6L, TenantContext::current))).isEqualTo(6L);
            assertThat(TenantContext.callAs(5L, () -> {
                TenantContext.runAs(6L, () -> { });
                return TenantContext.current();
            })).as("restored after the nested scope").isEqualTo(5L);
            return Thread.currentThread().getName();
        });

        if (threads == Threads.POOLED_PLATFORM) {
            AtomicReference<String> secondThread = new AtomicReference<>();
            assertThat(run(() -> {
                secondThread.set(Thread.currentThread().getName());
                return TenantContext.current();
            })).as("next task on the reused pooled thread").isNull();
            assertThat(secondThread.get()).as("the pool reused its thread").isEqualTo(firstThread);
        }
    }

    @ParameterizedTest(name = "{0}: decorated task sees the tenant; next-task leak checked on POOLED_PLATFORM only")
    @EnumSource(Threads.class)
    void aDecoratedTask_seesTheSubmittersTenant_andLeavesNoneForTheNextTaskOnAPooledThread(Threads threads)
            throws Exception {
        executor = executorOf(threads);
        TenantAndSecurityContextTaskDecorator decorator = new TenantAndSecurityContextTaskDecorator();
        AtomicReference<Long> inTask = new AtomicReference<>();
        AtomicReference<Long> nestedInTask = new AtomicReference<>();

        Runnable decorated = TenantContext.callAs(42L, () -> decorator.decorate(() -> {
            inTask.set(TenantContext.current());
            nestedInTask.set(TenantContext.callAs(TenantConstants.PLATFORM_TENANT_ID, TenantContext::current));
        }));
        executor.submit(decorated).get(5, TimeUnit.SECONDS);

        assertThat(inTask.get()).isEqualTo(42L);
        assertThat(nestedInTask.get()).as("nested callAs inside the task (C12 listeners)")
            .isEqualTo(TenantConstants.PLATFORM_TENANT_ID);
        if (threads == Threads.POOLED_PLATFORM) {
            assertThat(run(TenantContext::current)).as("next undecorated task on the reused pooled thread").isNull();
        }
    }

    @ParameterizedTest(name = "{0}: a new thread inherits no tenant")
    @EnumSource(Threads.class)
    void aThreadStartedInsideCallAs_doesNotInheritTheTenant(Threads threads) throws Exception {
        AtomicReference<Long> child = new AtomicReference<>(-1L);
        TenantContext.runAs(9L, () -> {
            Thread thread = threads == Threads.VIRTUAL
                ? Thread.ofVirtual().unstarted(() -> child.set(TenantContext.current()))
                : Thread.ofPlatform().unstarted(() -> child.set(TenantContext.current()));
            thread.start();
            try {
                thread.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(child.get()).isNull();
    }

    @Test
    void manyConcurrentVirtualThreads_eachSeeOnlyTheirOwnTenant_inTheTask() throws Exception {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<Boolean>> results = new ArrayList<>();
        for (long tenant = 1; tenant <= 2_000; tenant++) {
            long id = tenant;
            results.add(executor.submit(() -> TenantContext.callAs(id, () -> {
                Thread.yield();
                return TenantContext.current() == id;
            }) && TenantContext.current() == null));
        }
        for (Future<Boolean> result : results) {
            assertThat(result.get(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void aRawSetWithoutClear_onAPooledThread_isSeenByTheNextTask_whichIsWhyRequestsClearItFirst() throws Exception {
        executor = Executors.newSingleThreadExecutor();
        run(() -> {
            TenantContext.set(7L);
            return null;
        });
        assertThat(run(TenantContext::current)).as("REQ-TENANT-023: the leak the JWT filter guards against")
            .isEqualTo(7L);
        assertThat(run(() -> TenantContext.callAs(8L, TenantContext::current))).isEqualTo(8L);
        assertThat(run(TenantContext::current)).as("callAs restores the leaked value, it does not clean it").isEqualTo(7L);
    }

    private static ExecutorService executorOf(Threads threads) {
        return threads == Threads.VIRTUAL ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newSingleThreadExecutor();
    }

    private <T> T run(Callable<T> task) throws Exception {
        return executor.submit(task).get(5, TimeUnit.SECONDS);
    }
}
