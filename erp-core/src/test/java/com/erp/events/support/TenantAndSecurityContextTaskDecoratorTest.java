package com.erp.events.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.tenant.TenantContext;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit test (erp-core step 08): the decorator carries the submitting thread's tenant and principal
 * into the worker and leaves the pooled worker thread clean afterwards — the next, undecorated task
 * on the same thread sees neither.
 */
class TenantAndSecurityContextTaskDecoratorTest {

    private final TenantAndSecurityContextTaskDecorator decorator = new TenantAndSecurityContextTaskDecorator();
    private final ExecutorService singleThread = Executors.newSingleThreadExecutor();

    @AfterEach
    void cleanUp() throws InterruptedException {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        singleThread.shutdownNow();
        singleThread.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void propagatesTenantAndPrincipal_andDoesNotLeakThemIntoTheNextTaskOnThePooledThread() throws Exception {
        TenantContext.set(42L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", null, List.of()));

        AtomicReference<Long> tenantInTask = new AtomicReference<>();
        AtomicReference<String> principalInTask = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> {
            tenantInTask.set(TenantContext.current());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            principalInTask.set(authentication == null ? null : authentication.getName());
        });

        // the submitting thread changes its own state after submission: the task keeps the captured one
        TenantContext.set(99L);
        singleThread.submit(decorated).get(5, TimeUnit.SECONDS);

        assertThat(tenantInTask.get()).isEqualTo(42L);
        assertThat(principalInTask.get()).isEqualTo("alice");

        AtomicReference<Long> tenantAfter = new AtomicReference<>(-1L);
        AtomicReference<Authentication> authenticationAfter = new AtomicReference<>();
        singleThread.submit(() -> {
            tenantAfter.set(TenantContext.current());
            authenticationAfter.set(SecurityContextHolder.getContext().getAuthentication());
        }).get(5, TimeUnit.SECONDS);

        assertThat(tenantAfter.get()).as("tenant left on the pooled thread").isNull();
        assertThat(authenticationAfter.get()).as("principal left on the pooled thread").isNull();
    }

    @Test
    void aTaskSubmittedWithoutTenantOrPrincipal_runsWithoutThem_evenOnAThreadThatHadThem() throws Exception {
        // a worker thread that (wrongly) still carries state from elsewhere
        singleThread.submit(() -> TenantContext.set(7L)).get(5, TimeUnit.SECONDS);

        AtomicReference<Long> tenantInTask = new AtomicReference<>(-1L);
        singleThread.submit(decorator.decorate(() -> tenantInTask.set(TenantContext.current())))
            .get(5, TimeUnit.SECONDS);
        assertThat(tenantInTask.get()).isNull();

        // the decorator restores what the worker had before, it does not wipe foreign state
        AtomicReference<Long> restored = new AtomicReference<>();
        singleThread.submit(() -> restored.set(TenantContext.current())).get(5, TimeUnit.SECONDS);
        assertThat(restored.get()).isEqualTo(7L);
    }
}
