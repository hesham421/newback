package com.erp.notif.repository;

import com.erp.notif.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENTITY-NOTIF-001 (NotificationLog). Search (QR-NOTIF-0002) is served by the
 * {@link JpaSpecificationExecutor}; single-row read (QR-NOTIF-0003) by the inherited {@code findById};
 * dispatch save/update of the fan-out rows (QR-NOTIF-0001/0006) by the inherited {@code save}. The
 * log carries no natural key and exposes no uniqueness or reference-count query. Module-internal.
 */
@Repository
public interface NotificationLogRepository
    extends JpaRepository<NotificationLog, Long>,
            JpaSpecificationExecutor<NotificationLog> {
}
