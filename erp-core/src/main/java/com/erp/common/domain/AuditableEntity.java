package com.erp.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.TenantId;

/**
 * The base of every tenant-scoped entity — i.e. of every core entity except the few global ones
 * ({@link GlobalAuditableEntity}). <b>It is tenant-aware</b> (erp-core step 05): besides the audit
 * columns and the optimistic lock it inherits, it carries {@code TENANT_ID} as Hibernate's
 * {@link TenantId} discriminator. Hibernate therefore
 * <ul>
 *   <li>adds {@code TENANT_ID = <current tenant>} to every query on the entity — HQL/JPQL, criteria
 *       ({@code SpecBuilder}), joins and loads by id ({@code findById} of another tenant's row finds
 *       nothing);</li>
 *   <li>sets the column on insert from the session's tenant (resolved from
 *       {@code com.erp.tenant.TenantContext}); it is never updatable and never set by application code.</li>
 * </ul>
 * The name stays {@code AuditableEntity} (no {@code TenantAwareEntity} alias) to avoid churn: every
 * entity that already extended it became tenant-scoped without an edit.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class AuditableEntity extends GlobalAuditableEntity {

    /** Owning tenant ({@code CORE_TENANT.ID}); assigned by Hibernate from the current tenant. */
    @TenantId
    @Column(name = "TENANT_ID", nullable = false, updatable = false)
    private Long tenantId;
}
