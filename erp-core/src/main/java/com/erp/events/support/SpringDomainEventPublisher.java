package com.erp.events.support;

import com.erp.events.DomainEvent;
import com.erp.events.DomainEventPublisher;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The default {@link DomainEventPublisher}: hands the event to Spring's
 * {@link ApplicationEventPublisher}, so ordinary {@code @EventListener} and
 * {@code @TransactionalEventListener} methods (core or application) receive it. Built by
 * {@code com.erp.autoconfigure.ErpCoreEventsAutoConfiguration}.
 */
@Slf4j
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    public SpringDomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event");
        log.debug("Publishing {}", event);
        delegate.publishEvent(event);
    }
}
