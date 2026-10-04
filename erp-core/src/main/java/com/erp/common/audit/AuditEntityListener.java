package com.erp.common.audit;

import com.erp.common.domain.AuditableEntity;
import com.erp.common.util.SecurityContextHelper;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

public class AuditEntityListener {

    @PrePersist
    public void prePersist(AuditableEntity entity) {
        String currentUser = SecurityContextHelper.getCurrentUsername();
        Instant now = Instant.now();
        entity.setCreatedBy(currentUser);
        entity.setCreatedAt(now);
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(now);
    }

    @PreUpdate
    public void preUpdate(AuditableEntity entity) {
        entity.setUpdatedBy(SecurityContextHelper.getCurrentUsername());
        entity.setUpdatedAt(Instant.now());
    }
}
