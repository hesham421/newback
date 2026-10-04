package com.erp.audit.config;

import com.erp.audit.listener.AuditedEntityListener;
import java.util.List;
import java.util.Map;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Registers the {@link AuditedEntityListener} with Hibernate (erp-core step 10). Spring Boot applies
 * every {@link HibernatePropertiesCustomizer} bean when it builds the EntityManagerFactory; this one
 * hands Hibernate an {@link IntegratorProvider} ({@value JpaSettings#INTEGRATOR_PROVIDER}) whose
 * {@link Integrator} appends the listener to the {@code POST_INSERT}, {@code POST_UPDATE} and
 * {@code POST_DELETE} events of the {@link EventListenerRegistry}.
 *
 * <p>An application that sets its own {@code hibernate.integrator_provider} keeps its integrators:
 * a provider already present is composed with this one.
 */
@Component
public class AuditHibernateConfiguration implements HibernatePropertiesCustomizer {

    private final AuditedEntityListener listener;

    public AuditHibernateConfiguration(AuditedEntityListener listener) {
        this.listener = listener;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        Object existing = hibernateProperties.get(JpaSettings.INTEGRATOR_PROVIDER);
        Integrator audit = new AuditIntegrator(listener);
        if (existing instanceof IntegratorProvider other) {
            hibernateProperties.put(JpaSettings.INTEGRATOR_PROVIDER, (IntegratorProvider) () -> {
                List<Integrator> integrators = new java.util.ArrayList<>(other.getIntegrators());
                integrators.add(audit);
                return integrators;
            });
        } else {
            hibernateProperties.put(JpaSettings.INTEGRATOR_PROVIDER, (IntegratorProvider) () -> List.of(audit));
        }
    }

    /** Appends the audit listener to Hibernate's post-insert/update/delete events. */
    static final class AuditIntegrator implements Integrator {

        private final AuditedEntityListener listener;

        AuditIntegrator(AuditedEntityListener listener) {
            this.listener = listener;
        }

        @Override
        public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
                              SessionFactoryImplementor sessionFactory) {
            EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
            registry.appendListeners(EventType.POST_INSERT, listener);
            registry.appendListeners(EventType.POST_UPDATE, listener);
            registry.appendListeners(EventType.POST_DELETE, listener);
        }
    }
}
