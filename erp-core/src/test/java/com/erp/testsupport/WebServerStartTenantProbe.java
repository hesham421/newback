package com.erp.testsupport;

import com.erp.tenant.TenantContext;
import jakarta.persistence.EntityManagerFactory;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * erp-core 1.1.1 — at the moment the embedded web server starts accepting requests, tries to open a
 * JPA session with no tenant and records whether Hibernate's tenant resolver refused it (strict) or
 * still bound it to the bootstrap sentinel. Scanned by {@link CoreTestApplication} (it lives in
 * {@code com.erp.testsupport}), so every integration context carries it without changing the context
 * cache key. Asserted by {@code TenantBootstrapWindowIntegrationTest}.
 */
@Component
public class WebServerStartTenantProbe implements ApplicationListener<WebServerInitializedEvent> {

    private final ObjectProvider<EntityManagerFactory> entityManagerFactory;
    private final AtomicReference<Boolean> tenantlessSessionRejected = new AtomicReference<>();

    public WebServerStartTenantProbe(ObjectProvider<EntityManagerFactory> entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        EntityManagerFactory factory = entityManagerFactory.getIfAvailable();
        if (factory == null) {
            return;
        }
        Long previous = TenantContext.current();
        TenantContext.clear();
        boolean rejected;
        try {
            factory.createEntityManager().close();
            rejected = false;
        } catch (RuntimeException expected) {
            rejected = true;
        } finally {
            if (previous != null) {
                TenantContext.set(previous);
            }
        }
        tenantlessSessionRejected.compareAndSet(null, rejected);
    }

    /**
     * Whether a session without a tenant was refused when the web server started; {@code null} if the
     * web server start was never observed.
     */
    public Boolean tenantlessSessionRejectedAtWebServerStart() {
        return tenantlessSessionRejected.get();
    }
}
