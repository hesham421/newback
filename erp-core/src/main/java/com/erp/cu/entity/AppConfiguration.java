package com.erp.cu.entity;

import com.erp.common.converter.BooleanNumberConverter;
import com.erp.common.domain.GlobalAuditableEntity;
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
 *
 * <p>erp-core step 09 — platform defaults and tenant overrides: {@link #tenantId} {@code NULL} is a
 * platform default, a tenant id is that tenant's override. This is the plan's one deliberate exception to
 * "every scoped table has a NOT NULL tenant": the entity extends {@link GlobalAuditableEntity} (no
 * Hibernate {@code @TenantId}), so <b>no query is tenant-filtered automatically</b> — every repository call
 * names the owner explicitly ({@code ConfigurationService} scopes each operation). Uniqueness is the
 * expression index {@code UQ_CU_APP_CONFIG_CONFIG_KEY (COALESCE(TENANT_ID, 0), CONFIG_KEY)} (V14), which
 * {@code @UniqueConstraint} cannot express.
 */
@Entity
@Table(name = "CU_APP_CONFIGURATION")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AppConfiguration extends GlobalAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_configuration_seq")
    @SequenceGenerator(name = "app_configuration_seq", sequenceName = "SEQ_CU_APP_CONFIGURATION", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    /** Owner: {@code NULL} = platform default, otherwise the overriding tenant ({@code CORE_TENANT.ID}). */
    @Column(name = "TENANT_ID", updatable = false)
    private Long tenantId;

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
