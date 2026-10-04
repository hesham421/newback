package com.erp.tenant.config;

import java.util.Map;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Registers {@link TenantIdentifierResolver} with Hibernate. Spring Boot 4's
 * {@code HibernateJpaConfiguration} does not pick up a {@code CurrentTenantIdentifierResolver} bean
 * on its own; it applies every {@link HibernatePropertiesCustomizer} bean, so the resolver is handed
 * over as {@code hibernate.tenant_identifier_resolver}. Together with the {@code @TenantId} field of
 * {@code AuditableEntity} this switches on Hibernate's discriminator (row-level) multi-tenancy.
 *
 * <p>When the application context has been refreshed (every bean created, before any
 * {@code ApplicationRunner} runs or any request is served) the resolver becomes strict.
 */
@Component
public class TenantHibernateConfiguration
        implements HibernatePropertiesCustomizer, ApplicationListener<ContextRefreshedEvent> {

    private final TenantIdentifierResolver resolver = new TenantIdentifierResolver();

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        resolver.bootstrapComplete();
    }

    /** The resolver handed to Hibernate (exposed for tests). */
    public TenantIdentifierResolver getResolver() {
        return resolver;
    }
}
