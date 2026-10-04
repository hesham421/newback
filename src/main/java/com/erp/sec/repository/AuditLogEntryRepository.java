package com.erp.sec.repository;

import com.erp.sec.entity.AuditLogEntry;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-011 (AuditLogEntry). Module-internal, append-only (POL-SEC-009): no
 * update and no delete is declared here, and no SEC service may call the inherited
 * {@code delete*} or the update half of {@code save} — see
 * governance/project-artifacts/sec-implementation-notes.md.
 */
@Repository
public interface AuditLogEntryRepository
    extends JpaRepository<AuditLogEntry, Long>,
            JpaSpecificationExecutor<AuditLogEntry> {

    /** QR-SEC-022 — the failed-logins widget: one AUDIT_EVENT_TYPE code over a rolling window. */
    @Query("SELECT COUNT(a) FROM AuditLogEntry a "
        + "WHERE a.eventTypeCode = :eventTypeCode AND a.occurredAt >= :since")
    long countByEventTypeSince(@Param("eventTypeCode") String eventTypeCode,
                               @Param("since") Instant since);

    /** QR-SEC-022 — the recent-activity widget; the page size carries the "last N" the plan left open. */
    @Query("SELECT a FROM AuditLogEntry a ORDER BY a.occurredAt DESC")
    List<AuditLogEntry> findRecent(Pageable pageable);
}
