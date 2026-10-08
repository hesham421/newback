package com.erp.common.idempotency;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A stored answer of a request sent with an {@code Idempotency-Key} ({@code CORE_IDEMPOTENCY_KEY},
 * {@code V21__core_idempotency_key.sql}). Tenant-scoped; {@code CREATED_BY} is the key's owner. Private to the
 * mechanism: never exposed over HTTP and never audited.
 */
@Entity
@Table(name = "CORE_IDEMPOTENCY_KEY",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_CORE_IDEMPOTENCY_KEY", columnNames = {"TENANT_ID", "IDEMPOTENCY_KEY", "ENDPOINT"})
    },
    indexes = {
        @Index(name = "IDX_CORE_IDEMPOTENCY_KEY_TENANT", columnList = "TENANT_ID"),
        @Index(name = "IDX_CORE_IDEMPOTENCY_KEY_CREATED_AT", columnList = "CREATED_AT")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class IdempotencyKey extends AuditableEntity {

    /** {@code RESPONSE_STATUS} of a claimed row whose answer is not known yet; never committed. */
    public static final int CLAIMED_STATUS = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_idempotency_key_seq")
    @SequenceGenerator(name = "core_idempotency_key_seq", sequenceName = "SEQ_CORE_IDEMPOTENCY_KEY", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "IDEMPOTENCY_KEY", length = 64, nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "ENDPOINT", length = 200, nullable = false, updatable = false)
    private String endpoint;

    @Column(name = "REQUEST_HASH", length = 64, nullable = false, updatable = false)
    private String requestHash;

    @Column(name = "RESPONSE_STATUS", nullable = false)
    private Integer responseStatus;

    @Column(name = "RESPONSE_BODY", columnDefinition = "TEXT")
    private String responseBody;

    /** A claim: inserted before the work it protects, in the same transaction. */
    public static IdempotencyKey claim(String idempotencyKey, String endpoint, String requestHash) {
        return IdempotencyKey.builder()
            .idempotencyKey(idempotencyKey)
            .endpoint(endpoint)
            .requestHash(requestHash)
            .responseStatus(CLAIMED_STATUS)
            .build();
    }

    /** Records the answer of the claimed request. */
    public void answer(int status, String body) {
        this.responseStatus = status;
        this.responseBody = body;
    }
}
