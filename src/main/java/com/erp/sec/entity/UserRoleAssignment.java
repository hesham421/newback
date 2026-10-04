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
import jakarta.persistence.UniqueConstraint;
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
 * ENT-SEC-003 — UserRoleAssignment (SEC_USER_ROLE). Source: db-script-sec.md §1 DBF-SEC-025..029
 * / §3 BLOCK 3, DATA-DOM-TRANSACTIONAL.md ENT-SEC-003. Does not extend AuditableEntity — see
 * governance/project-artifacts/sec-implementation-notes.md. RULE-SEC-005 (SoD) is decided by
 * {@code UserRoleAssignmentDomain}; an assignment is created or deleted, never flag-toggled.
 */
@Entity
@Table(name = "SEC_USER_ROLE",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SEC_USER_ROLE_USER_ROLE", columnNames = {"USER_ID", "ROLE_ID"})
    },
    indexes = {
        @Index(name = "IDX_SEC_USER_ROLE_USER", columnList = "USER_ID"),
        @Index(name = "IDX_SEC_USER_ROLE_ROLE", columnList = "ROLE_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class UserRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_user_role_seq")
    @SequenceGenerator(name = "sec_user_role_seq", sequenceName = "SEQ_SEC_USER_ROLE", allocationSize = 1)
    @Column(name = "USER_ROLE_PK")
    private Long userRolePk;

    /** DBF-SEC-026 — FK_USER_ROLE_USER; half of UQ_SEC_USER_ROLE_USER_ROLE. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_USER_ROLE_USER"))
    private User user;

    /** DBF-SEC-027 — FK_USER_ROLE_ROLE; half of UQ_SEC_USER_ROLE_USER_ROLE. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_USER_ROLE_ROLE"))
    private Role role;

    /** DBF-SEC-028 — system-set (CORE.md "Audit fields"); never present in a request DTO. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "ASSIGNED_BY", length = 100, nullable = false)
    private String assignedBy;

    /** DBF-SEC-029 — system-set; DB DEFAULT now(), mirrored below so the INSERT is never null. */
    @Column(name = "ASSIGNED_AT", nullable = false)
    private Instant assignedAt;

    /** Mirrors the db-script DEFAULT now() for DBF-SEC-029 (A.1.17 — the sole normalization site). */
    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }
}
