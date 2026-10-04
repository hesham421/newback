package com.erp.sec.entity;

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
 * ENT-SEC-012 — PasswordResetToken (SEC_PWD_RESET_TOKEN). Source: db-script-sec.md §1
 * DBF-SEC-091..096 / §3 BLOCK 3, DATA-DOM-TRANSACTIONAL.md ENT-SEC-012. Does not extend
 * AuditableEntity — see governance/project-artifacts/sec-implementation-notes.md. Whether the
 * token may still be used (RULE-SEC-006) is decided by {@code PasswordResetTokenDomain}.
 */
@Entity
@Table(name = "SEC_PWD_RESET_TOKEN",
    indexes = {
        @Index(name = "IDX_SEC_PWD_RESET_TOKEN_USER", columnList = "USER_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class PasswordResetToken {

    /**
     * A7 DEFAULT window: {@code expiresAt = requestedAt + 30 minutes} — stated by
     * DATA-DOM-TRANSACTIONAL.md ENT-SEC-012 and db-script-sec.md §1 DBF-SEC-095
     * ("A7 DEFAULT: requestedAt + 30 min"), non-breaking.
     */
    private static final Duration DEFAULT_VALIDITY_WINDOW = Duration.ofMinutes(30);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_pwd_reset_token_seq")
    @SequenceGenerator(name = "sec_pwd_reset_token_seq",
        sequenceName = "SEQ_SEC_PWD_RESET_TOKEN", allocationSize = 1)
    @Column(name = "PWD_RESET_TOKEN_PK")
    private Long pwdResetTokenPk;

    /** DBF-SEC-092 — FK_PWD_RESET_TOKEN_USER. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_PWD_RESET_TOKEN_USER"))
    private User user;

    /** DBF-SEC-093 — TEXT, algorithm-agnostic width (db-script §4 DEFAULT); write-once. */
    @NotBlank(message = "{validation.required}")
    @Column(name = "TOKEN_HASH", columnDefinition = "TEXT", nullable = false)
    private String tokenHash;

    /** DBF-SEC-094 — system-set, DB DEFAULT now() mirrored in {@code @PrePersist}. */
    @Column(name = "REQUESTED_AT", nullable = false)
    private Instant requestedAt;

    /** DBF-SEC-095 — NOT NULL with no DB default; the A7 window is applied in {@code @PrePersist}. */
    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    /** DBF-SEC-096 — null until the token is consumed [RULE-SEC-006]. */
    @Column(name = "USED_AT")
    private Instant usedAt;

    /**
     * Sole normalization site (A.1.17): mirrors the db-script DEFAULT now() for DBF-SEC-094 and
     * applies the A7 DEFAULT window to DBF-SEC-095, which is NOT NULL and carries no DB default.
     */
    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
        if (expiresAt == null) {
            expiresAt = requestedAt.plus(DEFAULT_VALIDITY_WINDOW);
        }
    }

    /**
     * Lifecycle mutator for API-SEC-004 (complete password reset) — pure field mutation, no guard
     * logic. RULE-SEC-006 is decided by {@code PasswordResetTokenDomain} before this is called.
     */
    public void markUsed() {
        this.usedAt = Instant.now();
    }
}
