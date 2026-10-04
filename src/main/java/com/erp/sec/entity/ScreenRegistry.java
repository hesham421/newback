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
 * ENT-SEC-005 — ScreenRegistry / SEC_PAGES (SEC_SCREEN_REG). Source: db-script-sec.md §1
 * DBF-SEC-039..048 / §3 BLOCK 3, DATA-DOM-MASTER.md ENT-SEC-005. DBF-SEC-041 ({@code moduleId})
 * is the FK {@code FK_SCREEN_REG_MODULE}, modelled as the {@code @ManyToOne} {@code module};
 * RULE-SEC-004 is decided by {@code ScreenRegistryDomain}.
 */
@Entity
@Table(name = "SEC_SCREEN_REG",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SEC_SCREEN_REG_PAGE", columnNames = {"PAGE_CODE"})
    },
    indexes = {
        @Index(name = "IDX_SEC_SCREEN_REG_MODULE", columnList = "MODULE_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ScreenRegistry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_screen_reg_seq")
    @SequenceGenerator(name = "sec_screen_reg_seq", sequenceName = "SEQ_SEC_SCREEN_REG", allocationSize = 1)
    @Column(name = "SCREEN_REG_PK")
    private Long screenRegPk;

    /** DBF-SEC-040 — SEC_PAGES page code (SRS §7.1); upper-case natural key. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "PAGE_CODE", length = 50, nullable = false)
    private String pageCode;

    /** DBF-SEC-041 — FK_SCREEN_REG_MODULE; the structural half of RULE-SEC-004. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MODULE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_SCREEN_REG_MODULE"))
    private ModuleRegistry module;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 150, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 150, nullable = false)
    private String nameEn;

    /** DBF-SEC-044 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (see User). */
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
        if (pageCode != null) {
            pageCode = pageCode.trim().toUpperCase();
        }
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
