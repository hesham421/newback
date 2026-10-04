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
 * ENTITY-NOTIF-002 — NotificationTemplate (bilingual notification template). Source:
 * db-script-NOTIF.md DBS-NOTIF-001, DATA-DOM.md. templateCode is the natural key
 * (RULE-NOTIF-006), normalized upper-case. attachmentFileId is a SOFT/service reference
 * to FILE (no FK — XM-NOTIF-002). Persistence-only; decisions live in NotificationTemplateDomain.
 */
@Entity
@Table(name = "NOTIF_TEMPLATE",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_NOTIF_TEMPLATE_CODE", columnNames = {"TEMPLATE_CODE"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class NotificationTemplate extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notif_template_seq")
    @SequenceGenerator(name = "notif_template_seq", sequenceName = "SEQ_NOTIF_TEMPLATE", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 80, message = "{validation.size}")
    @Column(name = "TEMPLATE_CODE", length = 80, nullable = false)
    private String templateCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 200, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 100, nullable = false)
    private String nameEn;

    @Size(max = 300, message = "{validation.size}")
    @Column(name = "SUBJECT_AR", length = 300)
    private String subjectAr;

    @Size(max = 300, message = "{validation.size}")
    @Column(name = "SUBJECT_EN", length = 300)
    private String subjectEn;

    @NotBlank(message = "{validation.required}")
    @Column(name = "BODY_AR", columnDefinition = "TEXT", nullable = false)
    private String bodyAr;

    @NotBlank(message = "{validation.required}")
    @Column(name = "BODY_EN", columnDefinition = "TEXT", nullable = false)
    private String bodyEn;

    @Column(name = "ATTACHMENT_FILE_ID")
    private Long attachmentFileId;

    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (templateCode != null) {
            templateCode = templateCode.trim().toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (templateCode != null) {
            templateCode = templateCode.trim().toUpperCase();
        }
    }

    public void activate() {
        this.isActive = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActive = Boolean.FALSE;
    }
}
