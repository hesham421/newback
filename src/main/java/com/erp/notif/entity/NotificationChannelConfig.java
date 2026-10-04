package com.erp.notif.entity;

import com.erp.common.converter.BooleanNumberConverter;
import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENTITY-NOTIF-003 — NotificationChannelConfig (per-channel enable flag + provider config).
 * Source: db-script-NOTIF.md DBS-NOTIF-001, DATA-DOM.md. channelTypeId is the unique natural
 * key (RULE-NOTIF-006), normalized upper-case. Uses IS_ENABLED_FL only (enable/disable) — no
 * IS_ACTIVE_FL, per SRS A3 / CORE. Persistence-only; decisions live in NotificationChannelConfigDomain.
 */
@Entity
@Table(name = "NOTIF_CHANNEL_CONFIG",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_NOTIF_CHANNEL_CONFIG_TYPE", columnNames = {"CHANNEL_TYPE_ID"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class NotificationChannelConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notif_channel_config_seq")
    @SequenceGenerator(name = "notif_channel_config_seq", sequenceName = "SEQ_NOTIF_CHANNEL_CONFIG", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "CHANNEL_TYPE_ID", length = 20, nullable = false)
    private String channelTypeId;

    @Column(name = "IS_ENABLED_FL", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isEnabled = Boolean.TRUE;

    @Column(name = "CONFIG_JSON", columnDefinition = "TEXT")
    private String configJson;

    @PrePersist
    protected void onCreate() {
        if (isEnabled == null) {
            isEnabled = Boolean.TRUE;
        }
        if (channelTypeId != null) {
            channelTypeId = channelTypeId.trim().toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (channelTypeId != null) {
            channelTypeId = channelTypeId.trim().toUpperCase();
        }
    }

    public void enable() {
        this.isEnabled = Boolean.TRUE;
    }

    public void disable() {
        this.isEnabled = Boolean.FALSE;
    }
}
