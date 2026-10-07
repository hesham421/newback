package com.erp.notif.repository;

import com.erp.notif.entity.NotificationLog;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENTITY-NOTIF-001 (NotificationLog). Search (QR-NOTIF-0002) is served by the
 * {@link JpaSpecificationExecutor}; single-row read (QR-NOTIF-0003) by the inherited {@code findById};
 * dispatch save/update of the fan-out rows (QR-NOTIF-0001/0006) by the inherited {@code save}. The
 * log carries no natural key and exposes no uniqueness or reference-count query. Module-internal.
 *
 * <p>erp-core step 08 adds the two reads of the asynchronous delivery: one row with its template
 * (the worker renders from it) and the stale {@code QUEUED} rows of the requeue job. Both are
 * tenant-filtered by Hibernate like every other query.
 */
@Repository
public interface NotificationLogRepository
    extends JpaRepository<NotificationLog, Long>,
            JpaSpecificationExecutor<NotificationLog> {

    /** One log row with its template fetched (the delivery worker renders the message from both). */
    @Query("SELECT l FROM NotificationLog l JOIN FETCH l.templateFk WHERE l.id = :id")
    Optional<NotificationLog> findWithTemplateById(@Param("id") Long id);

    /**
     * Rows in {@code status} whose next attempt — or, before any attempt, whose creation — is older
     * than {@code cutoff}: the stalled {@code QUEUED} rows the requeue job re-dispatches.
     */
    @Query("SELECT l FROM NotificationLog l JOIN FETCH l.templateFk WHERE l.notificationStatusId = :status"
        + " AND ((l.nextAttemptAt IS NULL AND l.createdAt < :cutoff) OR l.nextAttemptAt < :cutoff)"
        + " ORDER BY l.id")
    List<NotificationLog> findStale(@Param("status") String status, @Param("cutoff") Instant cutoff);

    /** tenant-maturity B — the current tenant's rows created at or after {@code since} ({@code NotificationLogQueryApi}). */
    long countByCreatedAtGreaterThanEqual(Instant since);
}
