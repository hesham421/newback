package com.erp.common.idempotency;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** {@code CORE_IDEMPOTENCY_KEY}, restricted to the current tenant by {@code @TenantId}; private to the mechanism. */
@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long>, JpaSpecificationExecutor<IdempotencyKey> {

    Optional<IdempotencyKey> findByIdempotencyKeyAndEndpoint(String idempotencyKey, String endpoint);

    /** Deletes an expired row by id; {@code 0} when a concurrent request deleted it first (no optimistic-lock error). */
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.id = :id")
    int deleteExpired(@Param("id") Long id);
}
