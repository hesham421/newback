package com.erp.common.idempotency;

import com.erp.common.util.SecurityContextHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The claim of a key (RULE-TENANT-026, review round 1): one plain-SQL insert that does nothing on
 * {@code UQ_CORE_IDEMPOTENCY_KEY}, on the current transaction's connection. It waits for a concurrent uncommitted claim of
 * the same key; a lost claim is "0 rows", never a logged constraint violation. {@code TENANT_ID} is the session's tenant.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyKeyClaims {

    static final String CLAIM = "INSERT INTO CORE_IDEMPOTENCY_KEY (ID, TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT, REQUEST_HASH,"
        + " RESPONSE_STATUS, CREATED_BY, CREATED_AT, UPDATED_BY, UPDATED_AT, VERSION)"
        + " VALUES (nextval('SEQ_CORE_IDEMPOTENCY_KEY'), ?, ?, ?, ?, " + IdempotencyKey.CLAIMED_STATUS + ", ?, ?, ?, ?, 0)"
        + " ON CONFLICT ON CONSTRAINT UQ_CORE_IDEMPOTENCY_KEY DO NOTHING";

    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /** Inside a transaction: {@code true} when this request claimed the key, {@code false} when another one holds it. */
    public boolean claim(String idempotencyKey, String endpoint, String requestHash) {
        Object tenantId = entityManager.unwrap(Session.class).getTenantIdentifierValue();
        String owner = SecurityContextHelper.getCurrentUsername();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return jdbcTemplate.update(CLAIM, tenantId, idempotencyKey, endpoint, requestHash, owner, now, owner, now) == 1;
    }
}
