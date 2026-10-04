package com.erp.sec.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
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
 * ENT-SEC-004 — ModuleRegistry (SEC_MODULE_REG). Source: db-script-sec.md §1 DBF-SEC-030..038 /
 * §3 BLOCK 2, DATA-DOM-MASTER.md ENT-SEC-004. {@code code} is the platform module prefix (FIN,
 * SEC, ...), an immutable upper-case natural key. No child collection is declared — the
 * ScreenRegistry side owns the association.
 */
@Entity
@Table(name = "SEC_MODULE_REG",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SEC_MODULE_REG_CODE", columnNames = {"CODE"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ModuleRegistry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_module_reg_seq")
    @SequenceGenerator(name = "sec_module_reg_seq", sequenceName = "SEQ_SEC_MODULE_REG", allocationSize = 1)
    @Column(name = "MODULE_REG_PK")
    private Long moduleRegPk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "CODE", length = 10, nullable = false)
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 150, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 150, nullable = false)
    private String nameEn;

    /** DBF-SEC-034 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (see User). */
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
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
