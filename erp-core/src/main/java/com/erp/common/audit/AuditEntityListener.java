package com.erp.common.audit;

import com.erp.common.domain.GlobalAuditableEntity;
import com.erp.common.util.SecurityContextHelper;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

/** Fills the audit columns of every {@link GlobalAuditableEntity} (and so of every tenant-aware entity). */
public class AuditEntityListener {

    @PrePersist
    public void prePersist(GlobalAuditableEntity entity) {
        String currentUser = SecurityContextHelper.getCurrentUsername();
        Instant now = Instant.now();
        entity.setCreatedBy(currentUser);
        entity.setCreatedAt(now);
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(now);
    }

    @PreUpdate
    public void preUpdate(GlobalAuditableEntity entity) {
        entity.setUpdatedBy(SecurityContextHelper.getCurrentUsername());
        entity.setUpdatedAt(Instant.now());
    }
}
