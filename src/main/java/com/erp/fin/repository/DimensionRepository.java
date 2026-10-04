package com.erp.fin.repository;

import com.erp.fin.entity.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-002 (Dimension). Module-internal (A.2.3).
 *
 * <p>QR-FIN-007 (search dimensions, API-FIN-005) is satisfied by the inherited
 * {@code findAll(Specification, Pageable)} from {@code JpaSpecificationExecutor} plus the shared
 * {@code SpecBuilder}/{@code PageableBuilder} layer at the service — no bespoke derived query.
 * QR-FIN-008 (create, API-FIN-006) uses the inherited {@code save}.
 */
@Repository
public interface DimensionRepository
    extends JpaRepository<Dimension, Long>,
            JpaSpecificationExecutor<Dimension> {

    /**
     * API-FIN-006's duplicate-code pre-check ({@code UQ_FIN_DIMENSION_CODE},
     * {@code FIN-409-DIMENSION-DUP}). No {@code AndIdNot} variant: DATA-DOM-LOOKUP.md gives
     * ENT-FIN-002 no update endpoint, so {@code code} is never mutated and an update-time
     * exclusion check would be dead code (A.2.5).
     */
    boolean existsByCode(String code);
}
