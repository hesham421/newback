package com.erp.tenant.config;

import java.util.Map;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Registers {@link TenantIdentifierResolver} with Hibernate. Spring Boot 4's
 * {@code HibernateJpaConfiguration} does not pick up a {@code CurrentTenantIdentifierResolver} bean
 * on its own; it applies every {@link HibernatePropertiesCustomizer} bean, so the resolver is handed
 * over as {@code hibernate.tenant_identifier_resolver}. Together with the {@code @TenantId} field of
 * {@code AuditableEntity} this switches on Hibernate's discriminator (row-level) multi-tenancy.
 *
 * <p>The resolver becomes strict once every bean has been created and <em>before the embedded web
 * server starts accepting requests</em> (erp-core 1.2.0): this bean is a {@link SmartLifecycle} whose
 * phase ({@value #PHASE}) is just below the phase of Boot's web-server start lifecycle
 * ({@code WebServerStartStopLifecycle}, {@code SmartLifecycle.DEFAULT_PHASE - 2048}). Lifecycles start
 * in ascending phase order during {@code finishRefresh}, so no request can ever be served with the
 * bootstrap sentinel tenant. Before 1.2.0 the switch happened on {@link ContextRefreshedEvent}, which is
 * published <em>after</em> the web server started, leaving a short window in which a request could run
 * with the sentinel. Repository bootstrap (singleton creation) still happens before any lifecycle
 * starts, so it keeps the sentinel. The {@link ContextRefreshedEvent} switch stays as a fallback (a
 * context whose lifecycle processor does not auto-start this bean).
 */
@Component
public class TenantHibernateConfiguration
        implements HibernatePropertiesCustomizer, ApplicationListener<ContextRefreshedEvent>, SmartLifecycle {

    /** One below Boot's servlet/reactive {@code WebServerStartStopLifecycle} phase. */
    static final int PHASE = SmartLifecycle.DEFAULT_PHASE - 2048 - 1;

    private final TenantIdentifierResolver resolver = new TenantIdentifierResolver();

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        resolver.bootstrapComplete();
    }

    /** Lifecycle start, before the web server's: from now on a session without a tenant is an error. */
    @Override
    public void start() {
        resolver.bootstrapComplete();
    }

    /** Nothing to undo: the resolver never goes back to tolerating a missing tenant. */
    @Override
    public void stop() {
        // intentionally empty
    }

    @Override
    public boolean isRunning() {
        return !resolver.isBootstrapping();
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    /** The resolver handed to Hibernate (exposed for tests). */
    public TenantIdentifierResolver getResolver() {
        return resolver;
    }
}
