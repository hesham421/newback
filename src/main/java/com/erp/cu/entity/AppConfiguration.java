package com.erp.cu.entity;

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
 * ENTITY-CU-001 — AppConfiguration (platform runtime key/value configuration store).
 * Source: db-script-CU.md DBS-CU-001, DATA-DOM.md ENTITY-CU-001.
 */
@Entity
@Table(name = "CU_APP_CONFIGURATION",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_CU_APP_CONFIG_CONFIG_KEY", columnNames = {"CONFIG_KEY"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AppConfiguration extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_configuration_seq")
    @SequenceGenerator(name = "app_configuration_seq", sequenceName = "SEQ_CU_APP_CONFIGURATION", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "CONFIG_KEY", length = 150, nullable = false)
    private String configKey;

    @NotBlank(message = "{validation.required}")
    @Column(name = "CONFIG_VALUE", columnDefinition = "TEXT", nullable = false)
    private String configValue;

    @Size(max = 2000, message = "{validation.size}")
    @Column(name = "NOTES", length = 2000)
    private String notes;

    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (configKey != null) {
            configKey = configKey.toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (configKey != null) {
            configKey = configKey.toUpperCase();
        }
    }

    public void activate() {
        this.isActive = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActive = Boolean.FALSE;
    }
}
