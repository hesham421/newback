package com.erp.mdl.repository;

import com.erp.mdl.entity.LookupType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-MDL-001 (LookupType). Module-internal (A.2.3). QR-MDL-001/010 (search /
 * browse-by-owner) are satisfied by the inherited {@code findAll(Specification, Pageable)} via
 * {@code JpaSpecificationExecutor} plus the shared {@code SpecBuilder}/{@code PageableBuilder}
 * layer at the service — no bespoke derived query needed for either. QR-MDL-002/003/004
 * (save/update/deactivate) use the inherited {@code save}.
 */
@Repository
public interface LookupTypeRepository
    extends JpaRepository<LookupType, Long>,
            JpaSpecificationExecutor<LookupType> {

    /**
     * QR-MDL-013 — API-MDL-002's key-uniqueness pre-check ({@code UQ_MDL_LOOKUP_TYPE_KEY}).
     * No {@code AndIdNot} variant: {@code key} is immutable on update (RULE-MDL-003, enforced
     * by DTO shape), so an update-time exclusion check would be dead code (A.2.5 / repository
     * skill's explicit prohibition).
     */
    boolean existsByKey(String key);

    /**
     * QR-MDL-015 — resolves a LookupType by its natural key for the consumer API
     * (API-MDL-011). The service checks {@code isActiveFl} on the result (RULE-MDL-004) rather
     * than filtering it here, since an inactive-but-found type is a distinct outcome (excluded
     * values) from an unknown key (404).
     */
    Optional<LookupType> findByKey(String key);
}
