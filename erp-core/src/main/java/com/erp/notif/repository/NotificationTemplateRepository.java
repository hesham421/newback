package com.erp.notif.repository;

import com.erp.notif.entity.NotificationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENTITY-NOTIF-002 (NotificationTemplate). templateCode is the immutable natural key
 * (RULE-NOTIF-006), so no {@code existsBy...AndIdNot} variant is provided — it can never change on
 * update. Module-internal; consumed only by the NOTIF services.
 */
@Repository
public interface NotificationTemplateRepository
    extends JpaRepository<NotificationTemplate, Long>,
            JpaSpecificationExecutor<NotificationTemplate> {

    /** QR-NOTIF-0012 (RULE-NOTIF-006) — templateCode uniqueness pre-check for API-NOTIF-004 create. */
    boolean existsByTemplateCode(String templateCode);

    /** QR-NOTIF-0007 — template resolution by natural key for API-NOTIF-001 dispatch. */
    Optional<NotificationTemplate> findByTemplateCode(String templateCode);
}
