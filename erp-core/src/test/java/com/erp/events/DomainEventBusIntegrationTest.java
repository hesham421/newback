package com.erp.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractAsyncIntegrationTest;
import com.erp.testsupport.DomainEventProbe;
import com.erp.testsupport.ProbeEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * erp-core step 08 — the domain event bus end to end, through {@link DomainEventPublisher} and an
 * {@code @Async(ErpCoreEvents.EXECUTOR) @TransactionalEventListener(AFTER_COMMIT, fallbackExecution)}
 * listener ({@link DomainEventProbe}): delivery after commit only, nothing on rollback, immediate
 * delivery outside a transaction, and the publisher's tenant and principal on the worker thread.
 */
class DomainEventBusIntegrationTest extends AbstractAsyncIntegrationTest {

    @Autowired
    private DomainEventPublisher publisher;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private List<TaskExecutor> defaultTaskExecutors;

    @Test
    void eventPublishedInATransaction_isDeliveredOnlyAfterCommit_andNeverOnRollback() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            publisher.publish(new ProbeEvent("rolled-back"));
            status.setRollbackOnly();
        });

        tx.executeWithoutResult(status -> {
            publisher.publish(new ProbeEvent("committed"));
            // still inside the transaction: an AFTER_COMMIT listener has not been invoked yet
            assertThat(probe.find(ProbeEvent.class, e -> e.getMarker().equals("committed"))).isEmpty();
        });

        DomainEventProbe.Received committed = await().atMost(ASYNC_TIMEOUT)
            .until(() -> probe.find(ProbeEvent.class, e -> e.getMarker().equals("committed")).orElse(null),
                r -> r != null);
        assertThat(committed.threadName()).startsWith("erp-event-");
        assertThat(committed.transactionActive()).isFalse();

        // The rolled-back event was discarded at completion of its transaction and never handed to
        // the executor; once the executor is idle its absence is final.
        awaitExecutorIdle();
        assertThat(probe.find(ProbeEvent.class, e -> e.getMarker().equals("rolled-back"))).isEmpty();
    }

    @Test
    void eventPublishedOutsideATransaction_isDeliveredImmediately_withTheTenantAndPrincipalOfThePublisher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "event-tester", null, List.of(new SimpleGrantedAuthority("ANY"))));

        TenantContext.runAs(777L, () -> publisher.publish(new ProbeEvent("tenant-777")));

        DomainEventProbe.Received received = await().atMost(ASYNC_TIMEOUT)
            .until(() -> probe.find(ProbeEvent.class, e -> e.getMarker().equals("tenant-777")).orElse(null),
                r -> r != null);
        assertThat(received.event().getTenantId()).isEqualTo(777L);
        assertThat(received.tenantInThread()).as("TenantContext on the listener thread").isEqualTo(777L);
        assertThat(received.principalInThread()).as("principal on the listener thread").isEqualTo("event-tester");
        assertThat(received.event().getActor()).isEqualTo("event-tester");
        assertThat(received.event().getRealm()).isEqualTo(DomainEvent.REALM_STAFF);
        assertThat(received.threadName()).startsWith("erp-event-");
    }

    @Test
    void theCoreEventExecutor_isNotADefaultCandidate_soTheApplicationsTaskExecutorIsUntouched() {
        // Boot's applicationTaskExecutor is the only TaskExecutor an unqualified injection sees.
        assertThat(defaultTaskExecutors).doesNotContain(eventExecutor);
        assertThat(defaultTaskExecutors).isNotEmpty();
    }
}
