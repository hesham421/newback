package com.erp.sequence.entity;

import com.erp.common.domain.AuditableEntity;
import com.erp.sequence.domain.ResetPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * erp-core step 09 — one period of a tenant's number series ({@code CORE_NUMBER_SERIES},
 * {@code V14__sequence_and_settings.sql}). A series is identified by its {@link #code}; every period
 * (see {@link ResetPolicy}) has its own row and its own {@link #nextValue}. The row with the lowest id
 * of a code is the series' <em>anchor</em>: the allocation locks it first, so concurrent allocations of
 * one code are serialised (and a new period row is never created twice).
 *
 * <p>Tenant-scoped ({@link AuditableEntity}): Hibernate adds {@code TENANT_ID} to every query, the
 * locking ones included. The PK column is {@code ID} (README §4 for new tables). Configuration fields
 * ({@code prefix}, {@code pattern}, {@code resetPolicy}, {@code isActive}) are the same on every row of a
 * code; the service keeps them in step.
 */
@Entity
@Table(name = "CORE_NUMBER_SERIES",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_CORE_NUMBER_SERIES_CODE_PERIOD", columnNames = {"TENANT_ID", "CODE", "PERIOD_KEY"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class NumberSeries extends AuditableEntity {

    /** Default {@code PATTERN} (also the column default). */
    public static final String DEFAULT_PATTERN = "{PREFIX}-{YYYY}-{SEQ:6}";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_number_series_seq")
    @SequenceGenerator(name = "core_number_series_seq", sequenceName = "SEQ_CORE_NUMBER_SERIES", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "CODE", length = 50, nullable = false, updatable = false)
    private String code;

    @Size(max = 20, message = "{validation.size}")
    @Column(name = "PREFIX", length = 20)
    private String prefix;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "PATTERN", length = 100, nullable = false)
    @Builder.Default
    private String pattern = DEFAULT_PATTERN;

    @NotNull(message = "{validation.required}")
    @Enumerated(EnumType.STRING)
    @Column(name = "RESET_POLICY", length = 10, nullable = false, updatable = false)
    @Builder.Default
    private ResetPolicy resetPolicy = ResetPolicy.YEARLY;

    @NotNull(message = "{validation.required}")
    @Column(name = "PERIOD_KEY", length = 7, nullable = false, updatable = false)
    @Builder.Default
    private String periodKey = "";

    @NotNull(message = "{validation.required}")
    @Column(name = "NEXT_VALUE", nullable = false)
    @Builder.Default
    private Long nextValue = 1L;

    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }

    /** Hands out {@link #nextValue} and moves the counter on (the caller holds the row lock). */
    public long takeNextValue() {
        long value = nextValue;
        nextValue = value + 1;
        return value;
    }

    public void activate() {
        this.isActive = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActive = Boolean.FALSE;
    }
}
