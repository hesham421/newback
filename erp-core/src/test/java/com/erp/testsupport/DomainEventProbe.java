package com.erp.testsupport;

import com.erp.events.DomainEvent;
import com.erp.events.ErpCoreEvents;
import com.erp.tenant.TenantContext;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Test-only listener for every {@link DomainEvent} (erp-core step 08), declared exactly the way a core
 * or application listener is: {@code @Async(ErpCoreEvents.EXECUTOR)} +
 * {@code @TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)}. It records each event
 * together with what the worker thread saw — its {@code TenantContext}, its principal, its name and
 * whether a transaction was active — so tests can assert delivery timing and context propagation.
 */
public class DomainEventProbe {

    /** One received event and the worker thread's view at that moment. */
    public record Received(DomainEvent event, Long tenantInThread, String principalInThread, String threadName,
                           boolean transactionActive) {
    }

    private final List<Received> received = new CopyOnWriteArrayList<>();

    @Async(ErpCoreEvents.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DomainEvent event) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        received.add(new Received(event, TenantContext.current(),
            authentication == null ? null : authentication.getName(), Thread.currentThread().getName(),
            TransactionSynchronizationManager.isActualTransactionActive()));
    }

    public List<Received> all() {
        return List.copyOf(received);
    }

    /** The first received event of {@code type} matching {@code filter}. */
    public <E extends DomainEvent> Optional<Received> find(Class<E> type, Predicate<E> filter) {
        return received.stream()
            .filter(r -> type.isInstance(r.event()) && filter.test(type.cast(r.event())))
            .findFirst();
    }

    public void clear() {
        received.clear();
    }

    /** Registers the probe as a bean in the test context. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {

        @Bean
        public DomainEventProbe domainEventProbe() {
            return new DomainEventProbe();
        }
    }
}
