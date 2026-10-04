package com.erp.sec.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-SEC-006 — ActionRegistry (SEC_ACTION_REG). Source: db-script-sec.md §1 DBF-SEC-049..059 /
 * §3 BLOCK 3, DATA-DOM-MASTER.md ENT-SEC-006. {@code permissionCode} is server-derived
 * (PERM_&lt;pageCode&gt;_&lt;actionCode&gt;), never caller-supplied; {@code actionCode} is an open
 * set (VIEW/CREATE/UPDATE/DELETE + module-declared codes), not lookup-backed.
 */
@Entity
@Table(name = "SEC_ACTION_REG",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SEC_ACTION_REG_PERM", columnNames = {"PERMISSION_CODE"})
    },
    indexes = {
        @Index(name = "IDX_SEC_ACTION_REG_SCREEN", columnList = "SCREEN_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ActionRegistry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_action_reg_seq")
    @SequenceGenerator(name = "sec_action_reg_seq", sequenceName = "SEQ_SEC_ACTION_REG", allocationSize = 1)
    @Column(name = "ACTION_REG_PK")
    private Long actionRegPk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "PERMISSION_CODE", length = 100, nullable = false)
    private String permissionCode;

    /** DBF-SEC-051 — FK_ACTION_REG_SCREEN. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCREEN_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ACTION_REG_SCREEN"))
    private ScreenRegistry screen;

    @NotBlank(message = "{validation.required}")
    @Size(max = 40, message = "{validation.size}")
    @Column(name = "ACTION_CODE", length = 40, nullable = false)
    private String actionCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 150, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 150, nullable = false)
    private String nameEn;

    /** DBF-SEC-055 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (see User). */
    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (isActiveFl == null) {
            isActiveFl = Boolean.TRUE;
        }
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        normalize();
    }

    private void normalize() {
        if (actionCode != null) {
            actionCode = actionCode.trim().toUpperCase();
        }
        if (permissionCode != null) {
            permissionCode = permissionCode.trim().toUpperCase();
        }
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
