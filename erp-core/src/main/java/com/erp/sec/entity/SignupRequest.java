package com.erp.sec.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-SEC-013 — SignupRequest (SEC_SIGNUP_REQUEST). Source: db-script-sec.md §1 DBF-SEC-097..104
 * / §3 BLOCK 2, DATA-DOM-TRANSACTIONAL.md ENT-SEC-013. Does not extend AuditableEntity — see
 * governance/project-artifacts/sec-implementation-notes.md. STATE MACHINE: {@code statusCode}
 * over SIGNUP_STATUS — PENDING → APPROVED or REJECTED, both terminal.
 */
@Entity
@Table(name = "SEC_SIGNUP_REQUEST",
    indexes = {
        @Index(name = "IDX_SEC_SIGNUP_REQUEST_STATUS", columnList = "STATUS_CODE"),
        @Index(name = "IDX_SEC_SIGNUP_REQUEST_EMAIL", columnList = "EMAIL")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class SignupRequest {

    /** SIGNUP_STATUS codes (A6 closed set, CHK_SEC_SIGNUP_REQUEST_STATUS). */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_signup_request_seq")
    @SequenceGenerator(name = "sec_signup_request_seq",
        sequenceName = "SEQ_SEC_SIGNUP_REQUEST", allocationSize = 1)
    @Column(name = "SIGNUP_REQUEST_PK")
    private Long signupRequestPk;

    /** DBF-SEC-098 — becomes the user's login on approval (SRS A3 ENT-SEC-013). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.size}")
    @Column(name = "EMAIL", length = 255, nullable = false)
    private String email;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "FULL_NAME_AR", length = 200, nullable = false)
    private String fullNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "FULL_NAME_EN", length = 200, nullable = false)
    private String fullNameEn;

    /** DBF-SEC-101 — system-set, DB DEFAULT now() mirrored in {@code @PrePersist}. */
    @Column(name = "SUBMITTED_AT", nullable = false)
    private Instant submittedAt;

    /** DBF-SEC-102 — SIGNUP_STATUS code, DB DEFAULT 'PENDING'. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "STATUS_CODE", length = 20, nullable = false)
    private String statusCode;

    /** DBF-SEC-103 — system-set (CORE.md "Audit fields"); null until a decision is taken. */
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "REVIEWED_BY", length = 100)
    private String reviewedBy;

    /** DBF-SEC-104 — system-set; null until a decision is taken. */
    @Column(name = "REVIEWED_AT")
    private Instant reviewedAt;

    /** Sole normalization site (A.1.17): mirrors the db-script DEFAULTs for DBF-SEC-101/102. */
    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
        if (statusCode == null) {
            statusCode = STATUS_PENDING;
        }
    }

    /**
     * PENDING → APPROVED (API-SEC-011). Pure field mutation; the "must currently be PENDING"
     * guard is decided by {@code SignupRequestDomain.assertCanDecide()} first.
     */
    public void approve(String reviewedByPrincipal) {
        this.statusCode = STATUS_APPROVED;
        this.reviewedBy = reviewedByPrincipal;
        this.reviewedAt = Instant.now();
    }

    /**
     * PENDING → REJECTED (API-SEC-011). Pure field mutation; no User row is created on this path
     * (REQ-SEC-005).
     */
    public void reject(String reviewedByPrincipal) {
        this.statusCode = STATUS_REJECTED;
        this.reviewedBy = reviewedByPrincipal;
        this.reviewedAt = Instant.now();
    }
}
