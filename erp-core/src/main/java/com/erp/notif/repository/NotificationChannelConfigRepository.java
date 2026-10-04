package com.erp.notif.repository;

import com.erp.notif.entity.NotificationChannelConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENTITY-NOTIF-003 (NotificationChannelConfig). channelTypeId is the immutable
 * natural key (RULE-NOTIF-006), so no {@code existsBy...AndIdNot} variant is provided. Module-internal;
 * consumed only by the NOTIF services.
 */
@Repository
public interface NotificationChannelConfigRepository
    extends JpaRepository<NotificationChannelConfig, Long>,
            JpaSpecificationExecutor<NotificationChannelConfig> {

    /** QR-NOTIF-0013 (RULE-NOTIF-006) — channelTypeId uniqueness pre-check for API-NOTIF-005 create. */
    boolean existsByChannelTypeId(String channelTypeId);

    /** QR-NOTIF-0011 (RULE-NOTIF-003) — channel resolution/enabled check for API-NOTIF-001 dispatch. */
    Optional<NotificationChannelConfig> findByChannelTypeId(String channelTypeId);
}
