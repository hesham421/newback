package com.erp.sec.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-SEC-001 — User (SEC_USER). Source: db-script-sec.md §1 DBF-SEC-001..013 / §3 BLOCK 2,
 * DATA-DOM-MASTER.md ENT-SEC-001. Persistence-only, no Domain-rule logic here.
 * {@code statusCode} holds the USER_STATUS lookup CODE as a plain String, closed set enforced by
 * {@code CHK_SEC_USER_STATUS}.
 */
@Entity
@Table(name = "SEC_USER",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SEC_USER_USERNAME", columnNames = {"USERNAME"}),
        @UniqueConstraint(name = "UQ_SEC_USER_EMAIL", columnNames = {"EMAIL"})
    },
    indexes = {
        @Index(name = "IDX_SEC_USER_STATUS", columnList = "STATUS_CODE")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class User extends AuditableEntity {

    /**
     * USER_STATUS codes (A6 closed set, CHK_SEC_USER_STATUS). {@code STATUS_ACTIVE} is public only
     * so {@code UserService} can derive {@code UserContact.active} (REQ-SEC-034) from DBF-SEC-007
     * instead of repeating the literal; the value and the lifecycle are unchanged.
     */
    public static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_user_seq")
    @SequenceGenerator(name = "sec_user_seq", sequenceName = "SEQ_SEC_USER", allocationSize = 1)
    @Column(name = "USER_PK")
    private Long userPk;

    /** DBF-SEC-002 — login identity; immutable after create (DTO MEMBERSHIP). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "USERNAME", length = 100, nullable = false)
    private String username;

    /** DBF-SEC-003 — updatable (API-SEC-007 update-request body). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.size}")
    @Column(name = "EMAIL", length = 255, nullable = false)
    private String email;

    /** DBF-SEC-004 — password_hash TEXT; never returned to any client (POL-SEC-004). */
    @NotBlank(message = "{validation.required}")
    @Column(name = "PASSWORD_HASH", columnDefinition = "TEXT", nullable = false)
    private String passwordHash;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "FULL_NAME_AR", length = 200, nullable = false)
    private String fullNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "FULL_NAME_EN", length = 200, nullable = false)
    private String fullNameEn;

    /** DBF-SEC-007 — USER_STATUS code (PENDING/ACTIVE/DISABLED), DB DEFAULT 'ACTIVE'. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "STATUS_CODE", length = 20, nullable = false)
    private String statusCode;

    @Column(name = "LAST_LOGIN_AT")
    private Instant lastLoginAt;

    /**
     * DBF-SEC-009 — native postgres BOOLEAN NOT NULL DEFAULT TRUE. Deliberately mapped without a
     * converter: the project's BooleanNumberConverter / BooleanCharYNConverter exist for numeric
     * and CHAR(1) columns and would break a native BOOLEAN column.
     */
    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;

    /**
     * Defaults only — the SRS treats {@code username} as a login identity and {@code email} as an
     * address (SRS A3 ENT-SEC-001); neither is an upper-cased natural key, so neither is
     * case-normalized here.
     */
    @PrePersist
    protected void onCreate() {
        if (isActiveFl == null) {
            isActiveFl = Boolean.TRUE;
        }
        if (statusCode == null) {
            statusCode = STATUS_ACTIVE;
        }
    }

    /** Field mutation only — the decision to (re)activate belongs to the service/API (API-SEC-010). */
    public void activate() {
        this.isActiveFl = Boolean.TRUE;
        this.statusCode = STATUS_ACTIVE;
    }

    /** Field mutation only — ACTIVE→DISABLED per A7 state machine (API-SEC-009). */
    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
        this.statusCode = STATUS_DISABLED;
    }
}
