package com.erp.tenant.config;

import com.erp.tenant.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Hibernate's view of {@link TenantContext}: the tenant a new session is bound to. Hibernate 7.2
 * resolves it eagerly when a session opens ({@code SessionFactoryImplementor.resolveTenantIdentifier()}
 * in the session builder) and applies it as the {@code @TenantId} discriminator: query restriction,
 * insert value, and load-by-id restriction (the {@code _tenantId} filter is {@code applyToLoadByKey}).
 *
 * <ul>
 *   <li>{@link #resolveCurrentTenantIdentifier()} returns {@link TenantContext#require()} once the
 *       application context has started: opening a session without a tenant then fails fast with
 *       {@code TENANT_CONTEXT_MISSING}.</li>
 *   <li>While the application context is still being created, Spring Data opens short-lived
 *       EntityManagers to build its repository queries — with no request, and so no tenant. Until
 *       {@link #bootstrapComplete()} is called (on {@code ContextRefreshedEvent}, see
 *       {@link TenantHibernateConfiguration}) a missing tenant resolves to {@link #BOOTSTRAP_TENANT_ID},
 *       an id no row can carry ({@code TENANT_ID} references {@code CORE_TENANT}, whose ids start at 1):
 *       such a session reads nothing and can insert nothing.</li>
 *   <li>{@link #validateExistingCurrentSessions()} is {@code false}: Spring manages sessions per
 *       transaction; there is no Hibernate "current session" to re-validate.</li>
 *   <li>{@code isRoot} keeps the interface default ({@code false}): no tenant — PLATFORM included —
 *       sees another tenant's rows.</li>
 * </ul>
 */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    /** The tenant of a session opened during application-context bootstrap: matches no row. */
    public static final Long BOOTSTRAP_TENANT_ID = -1L;

    private volatile boolean bootstrapping = true;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        if (bootstrapping) {
            Long current = TenantContext.current();
            return current != null ? current : BOOTSTRAP_TENANT_ID;
        }
        return TenantContext.require();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    /** From now on a session without a tenant is an error ({@code TENANT_CONTEXT_MISSING}). */
    public void bootstrapComplete() {
        this.bootstrapping = false;
    }

    /** Whether sessions without a tenant are still tolerated (application context not yet refreshed). */
    public boolean isBootstrapping() {
        return bootstrapping;
    }
}
