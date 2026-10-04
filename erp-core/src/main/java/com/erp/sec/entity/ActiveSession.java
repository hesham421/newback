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
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-SEC-010 — ActiveSession (SEC_ACTIVE_SESSION). Source: db-script-sec.md §1 DBF-SEC-075..082
 * / §3 BLOCK 3, DATA-DOM-TRANSACTIONAL.md ENT-SEC-010. Does not extend AuditableEntity — see
 * governance/project-artifacts/sec-implementation-notes.md. STATE: {@code terminatedAt IS NULL}
 * = active; there is no {@code is_active_fl} column.
 */
@Entity
@Table(name = "SEC_ACTIVE_SESSION",
    indexes = {
        @Index(name = "IDX_SEC_ACTIVE_SESSION_USER", columnList = "USER_ID"),
        @Index(name = "IDX_SEC_ACTIVE_SESSION_TERMINATED", columnList = "TERMINATED_AT")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_active_session_seq")
    @SequenceGenerator(name = "sec_active_session_seq",
        sequenceName = "SEQ_SEC_ACTIVE_SESSION", allocationSize = 1)
    @Column(name = "ACTIVE_SESSION_PK")
    private Long activeSessionPk;

    /** DBF-SEC-076 — FK_ACTIVE_SESSION_USER. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ACTIVE_SESSION_USER"))
    private User user;

    /** DBF-SEC-077 — opaque token reference; never the raw token/hash, never serialized back. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "TOKEN_REF", length = 200, nullable = false)
    private String tokenRef;

    /** DBF-SEC-078 — system-set, DB DEFAULT now() mirrored in {@code @PrePersist}. */
    @Column(name = "STARTED_AT", nullable = false)
    private Instant startedAt;

    /** DBF-SEC-079 — system-set, DB DEFAULT now() mirrored in {@code @PrePersist}. */
    @Column(name = "LAST_ACTIVITY_AT", nullable = false)
    private Instant lastActivityAt;

    /** DBF-SEC-080 — nullable. */
    @Size(max = 64, message = "{validation.size}")
    @Column(name = "IP_ADDRESS", length = 64)
    private String ipAddress;

    /** DBF-SEC-081 — null while the session is active (state machine above). */
    @Column(name = "TERMINATED_AT")
    private Instant terminatedAt;

    /** DBF-SEC-082 — system-set (CORE.md "Audit fields"); null while active. */
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "TERMINATED_BY", length = 100)
    private String terminatedBy;

    /** Mirrors the db-script DEFAULT now() for DBF-SEC-078/079 (A.1.17). */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (startedAt == null) {
            startedAt = now;
        }
        if (lastActivityAt == null) {
            lastActivityAt = now;
        }
    }

    /**
     * Lifecycle mutator for API-SEC-026 and API-SEC-009's bulk termination — pure field mutation;
     * whether the session may still be terminated is decided by
     * {@code ActiveSessionDomain.assertCanTerminate()}.
     */
    public void terminate(String terminatedByPrincipal) {
        this.terminatedAt = Instant.now();
        this.terminatedBy = terminatedByPrincipal;
    }
}
