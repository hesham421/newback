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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * erp-core step 06 — the one-time e-mail verification token of a self-registered CUSTOMER account
 * (SEC_CUSTOMER_VERIFY_TOKEN, {@code V11__sec_realms.sql}). Only the SHA-256 hash of the raw token is
 * stored ({@code TokenHasher}); the raw value travels only in the verification e-mail. Whether the token
 * may still be used is decided by {@code CustomerVerifyTokenDomain}.
 *
 * <p>Tenant-scoped (extends {@link AuditableEntity}). The PK column is {@code ID} (README §4 for new
 * tables), not a {@code *_PK} name.
 */
@Entity
@Table(name = "SEC_CUSTOMER_VERIFY_TOKEN",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SEC_CUSTOMER_VERIFY_TOKEN_HASH", columnNames = {"TENANT_ID", "TOKEN_HASH"})
    },
    indexes = {
        @Index(name = "IDX_SEC_CUSTOMER_VERIFY_TOKEN_USER", columnList = "USER_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class CustomerVerifyToken extends AuditableEntity {

    /** How long a verification link stays valid. */
    public static final Duration DEFAULT_VALIDITY_WINDOW = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_customer_verify_token_seq")
    @SequenceGenerator(name = "sec_customer_verify_token_seq",
        sequenceName = "SEQ_SEC_CUSTOMER_VERIFY_TOKEN", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_SEC_CUSTOMER_VERIFY_TOKEN_USER"))
    private User user;

    /** SHA-256 hex of the raw token; write-once. */
    @NotBlank(message = "{validation.required}")
    @Column(name = "TOKEN_HASH", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String tokenHash;

    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    /** Null until the token is consumed. */
    @Column(name = "USED_AT")
    private Instant usedAt;

    /** Sole normalization site: applies the default validity window. */
    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            expiresAt = Instant.now().plus(DEFAULT_VALIDITY_WINDOW);
        }
    }

    /** Pure field mutation; usability is decided by {@code CustomerVerifyTokenDomain} first. */
    public void markUsed() {
        this.usedAt = Instant.now();
    }
}
