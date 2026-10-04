package com.erp.notif.repository;

import com.erp.notif.entity.NotificationLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only aggregate over {@link NotificationLog} for the {@code NOTIF_LOG_SUMMARY} report (erp-core
 * step 11). JPQL, so Hibernate's tenant filter applies — no native SQL. A separate, narrow interface
 * (not a method on {@link NotificationLogRepository}): it exposes nothing but this one grouped read.
 * Module-internal.
 */
@org.springframework.stereotype.Repository
public interface NotificationLogSummaryRepository extends Repository<NotificationLog, Long> {

    /**
     * Notification log rows grouped by channel, status and creation day, newest day first. Each row is
     * {@code [channelTypeId (String), notificationStatusId (String), day (LocalDate), count (Long)]}.
     * {@code channel}/{@code status} empty = all; the window is {@code [from, to)}.
     */
    @Query("SELECT l.channelTypeId, l.notificationStatusId, cast(l.createdAt as LocalDate), count(l) "
        + "FROM NotificationLog l "
        + "WHERE (:channel = '' OR l.channelTypeId = :channel) "
        + "AND (:status = '' OR l.notificationStatusId = :status) "
        + "AND l.createdAt >= :from AND l.createdAt < :to "
        + "GROUP BY l.channelTypeId, l.notificationStatusId, cast(l.createdAt as LocalDate) "
        + "ORDER BY cast(l.createdAt as LocalDate) DESC, l.channelTypeId, l.notificationStatusId")
    List<Object[]> summarize(@Param("channel") String channel, @Param("status") String status,
                             @Param("from") Instant from, @Param("to") Instant to);
}
