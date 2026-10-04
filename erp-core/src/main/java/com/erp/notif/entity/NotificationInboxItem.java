package com.erp.notif.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One in-app notification of one recipient ({@code NOTIF_INBOX}, erp-core step 08) — written by the
 * IN_APP channel provider, read and marked read by its recipient through {@code /api/v1/notif/inbox}.
 * {@code recipientUserId} is a {@code SEC_USER} id of either realm (SOFT-READ to SEC, no FK). The
 * bilingual title/body are already rendered (placeholders substituted). Who may read an item is
 * decided by {@code NotificationInboxDomain}.
 */
@Entity
@Table(name = "NOTIF_INBOX",
    indexes = {
        @Index(name = "IDX_NOTIF_INBOX_TENANT", columnList = "TENANT_ID"),
        @Index(name = "IDX_NOTIF_INBOX_RECIPIENT", columnList = "TENANT_ID, RECIPIENT_USER_ID, READ_AT")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class NotificationInboxItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notif_inbox_seq")
    @SequenceGenerator(name = "notif_inbox_seq", sequenceName = "SEQ_NOTIF_INBOX", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotNull(message = "{validation.required}")
    @Column(name = "RECIPIENT_USER_ID", nullable = false)
    private Long recipientUserId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 300, message = "{validation.size}")
    @Column(name = "TITLE_AR", length = 300, nullable = false)
    private String titleAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 300, message = "{validation.size}")
    @Column(name = "TITLE_EN", length = 300, nullable = false)
    private String titleEn;

    @NotBlank(message = "{validation.required}")
    @Column(name = "BODY_AR", columnDefinition = "TEXT", nullable = false)
    private String bodyAr;

    @NotBlank(message = "{validation.required}")
    @Column(name = "BODY_EN", columnDefinition = "TEXT", nullable = false)
    private String bodyEn;

    @Column(name = "READ_AT")
    private Instant readAt;

    @Size(max = 100, message = "{validation.size}")
    @Column(name = "REFERENCE_TYPE", length = 100)
    private String referenceType;

    @Column(name = "REFERENCE_ID")
    private Long referenceId;

    /** Marks the item read now; pure field mutation — the service decides whether it may (Domain). */
    public void markRead(Instant now) {
        this.readAt = now;
    }
}
