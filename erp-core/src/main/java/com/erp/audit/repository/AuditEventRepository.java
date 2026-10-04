package com.erp.audit.repository;

import com.erp.audit.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Read access to {@code CORE_AUDIT_EVENT} (erp-core step 10) — tenant-filtered by Hibernate. Writes go
 * through {@code AuditEventStore} (JDBC), never through this repository. Module-internal.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {
}
