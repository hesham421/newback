package com.erp.app;

import com.erp.events.ErpCoreEvents;
import com.erp.events.UserCreatedEvent;
import com.erp.tenant.TenantContext;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * erp-core step 08 — the application-side extension door: an application bean listening to a core
 * domain event ({@link UserCreatedEvent}) exactly as the event bus documents it, with no change to
 * erp-core. Test-only; it records each event and the tenant its worker thread ran with.
 */
public class UserCreatedEventProbe {

    /** A received event and the {@code TenantContext} of the listener thread. */
    public record Received(UserCreatedEvent event, Long tenantInThread, String threadName) {
    }

    private final List<Received> received = new CopyOnWriteArrayList<>();

    @Async(ErpCoreEvents.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserCreated(UserCreatedEvent event) {
        received.add(new Received(event, TenantContext.current(), Thread.currentThread().getName()));
    }

    public List<Received> received() {
        return List.copyOf(received);
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {

        @Bean
        public UserCreatedEventProbe userCreatedEventProbe() {
            return new UserCreatedEventProbe();
        }
    }
}
