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
 * ENT-SEC-008 — RoleScreenGrant (SEC_ROLE_SCREEN_GRANT). Source: db-script-sec.md §1
 * DBF-SEC-065..069 / §3 BLOCK 3, DATA-DOM-TRANSACTIONAL.md ENT-SEC-008. Does not extend
 * AuditableEntity — see governance/project-artifacts/sec-implementation-notes.md. RULE-SEC-001
 * and the duplication guard are decided by {@code RoleScreenGrantDomain}.
 */
@Entity
@Table(name = "SEC_ROLE_SCREEN_GRANT",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN",
            columnNames = {"ROLE_ID", "SCREEN_ID"})
    },
    indexes = {
        @Index(name = "IDX_SEC_ROLE_SCREEN_GRANT_ROLE", columnList = "ROLE_ID"),
        @Index(name = "IDX_SEC_ROLE_SCREEN_GRANT_SCREEN", columnList = "SCREEN_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class RoleScreenGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_role_screen_grant_seq")
    @SequenceGenerator(name = "sec_role_screen_grant_seq",
        sequenceName = "SEQ_SEC_ROLE_SCREEN_GRANT", allocationSize = 1)
    @Column(name = "ROLE_SCREEN_GRANT_PK")
    private Long roleScreenGrantPk;

    /** DBF-SEC-066 — FK_ROLE_SCREEN_GRANT_ROLE; half of the composite UQ. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ROLE_SCREEN_GRANT_ROLE"))
    private Role role;

    /** DBF-SEC-067 — FK_ROLE_SCREEN_GRANT_SCREEN; half of the composite UQ. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCREEN_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ROLE_SCREEN_GRANT_SCREEN"))
    private ScreenRegistry screen;

    /** DBF-SEC-068 — system-set (CORE.md "Audit fields"); never in a request DTO. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "GRANTED_BY", length = 100, nullable = false)
    private String grantedBy;

    /** DBF-SEC-069 — system-set, DB DEFAULT now() mirrored below. */
    @Column(name = "GRANTED_AT", nullable = false)
    private Instant grantedAt;

    /** Mirrors the db-script DEFAULT now() for DBF-SEC-069 (A.1.17). */
    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = Instant.now();
        }
    }
}
