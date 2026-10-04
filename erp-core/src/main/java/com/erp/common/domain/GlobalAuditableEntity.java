package com.erp.common.domain;

import com.erp.common.audit.AuditEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Audit columns + optimistic lock, <b>without</b> a tenant: the base of the few entities that are
 * global — the same rows for every tenant (erp-core step 05). Today that is exactly the code-defined
 * permission catalog ({@code SEC_MODULE_REG}, {@code SEC_SCREEN_REG}, {@code SEC_ACTION_REG}) and the
 * tenant registry itself ({@code CORE_TENANT}). Every other entity extends the tenant-aware
 * {@link AuditableEntity}; a new global entity needs a step file that names it global.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditEntityListener.class)
public abstract class GlobalAuditableEntity {

    // length = 100 mirrors the NARROWEST live physical width: SEC/MDL/CORE tables define
    // CREATED_BY/UPDATED_BY as VARCHAR(100); CU/NOTIF/FILE (6 tables) are intentionally wider at
    // VARCHAR(255). The value itself is the SEC username (JWT subject), itself capped at 100 by
    // SEC_USER.username, so 100 is the true upper bound — do NOT widen this back "for consistency".
    @Column(name = "CREATED_BY", updatable = false, length = 100)
    private String createdBy;

    @Column(name = "CREATED_AT", updatable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    @Column(name = "UPDATED_AT")
    private Instant updatedAt;

    /**
     * Optimistic lock ({@code VERSION BIGINT NOT NULL DEFAULT 0}, V10). Hibernate sets 0 on insert
     * and increments it on every update; a stale update fails with an optimistic-locking error,
     * mapped to 409 {@code CONCURRENT_MODIFICATION} by the global exception handler. Never set it by hand.
     */
    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;
}
