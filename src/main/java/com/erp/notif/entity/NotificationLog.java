package com.erp.notif.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENTITY-NOTIF-001 — NotificationLog (per-channel notification log row). Source:
 * db-script-NOTIF.md DBS-NOTIF-001, DATA-DOM.md. Transactional (technical) log — one row per
 * requested channel (RULE-NOTIF-001); no isActive flag. recipientId is a SOFT-READ to SEC (no
 * FK — XM-NOTIF-001). templateFk is the intra-module FK to NOTIF_TEMPLATE. Lifecycle transitions
 * (LOV-NOTIF-002, A6) are decided in NotificationLogDomain.
 */
@Entity
@Table(name = "NOTIF_LOG",
    indexes = {
        @Index(name = "IDX_NOTIF_LOG_TEMPLATE_FK", columnList = "TEMPLATE_FK"),
        @Index(name = "IDX_NOTIF_LOG_RECIPIENT_ID", columnList = "RECIPIENT_ID"),
        @Index(name = "IDX_NOTIF_LOG_STATUS", columnList = "NOTIFICATION_STATUS_ID"),
        @Index(name = "IDX_NOTIF_LOG_MODULE_CODE", columnList = "MODULE_CODE")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class NotificationLog extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notif_log_seq")
    @SequenceGenerator(name = "notif_log_seq", sequenceName = "SEQ_NOTIF_LOG", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotNull(message = "{validation.required}")
    @Column(name = "RECIPIENT_ID", nullable = false)
    private Long recipientId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "CHANNEL_TYPE_ID", length = 20, nullable = false)
    private String channelTypeId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 30, message = "{validation.size}")
    @Column(name = "NOTIFICATION_STATUS_ID", length = 30, nullable = false)
    private String notificationStatusId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "MODULE_CODE", length = 50, nullable = false)
    private String moduleCode;

    @Column(name = "REFERENCE_ID")
    private Long referenceId;

    @Size(max = 100, message = "{validation.size}")
    @Column(name = "REFERENCE_TYPE", length = 100)
    private String referenceType;

    @Column(name = "RETRY_COUNT", nullable = false)
    @Builder.Default
    private Short retryCount = (short) 0;

    @Column(name = "ERROR_MESSAGE", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "SENT_AT")
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_FK", nullable = false,
        foreignKey = @ForeignKey(name = "FK_NOTIF_LOG_TEMPLATE"))
    private NotificationTemplate templateFk;
}
