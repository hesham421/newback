package com.erp.fin.repository;

import com.erp.fin.entity.DimensionValue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-003 (DimensionValue). Module-internal (A.2.3).
 *
 * <p>QR-FIN-011 (search dimension values, API-FIN-008) is satisfied by the inherited
 * {@code findAll(Specification, Pageable)} plus the shared {@code SpecBuilder}/
 * {@code PageableBuilder} layer at the service (filtering by {@code dimension.dimensionPk} and
 * {@code code}). QR-FIN-009 (create, API-FIN-007) uses the inherited {@code save}.
 */
@Repository
public interface DimensionValueRepository
    extends JpaRepository<DimensionValue, Long>,
            JpaSpecificationExecutor<DimensionValue> {

    /**
     * QR-FIN-010 — RULE-FIN-002's fact and API-FIN-007's duplicate pre-check
     * ({@code UQ_FIN_DIMENSION_VALUE_DIM_CODE}). The service passes the result into
     * {@code DimensionValueDomain.create(...)}, which owns the decision. No {@code AndIdNot}
     * variant: {@code code} is create-only, so the update-time exclusion check would be dead
     * code (A.2.5).
     */
    boolean existsByDimension_DimensionPkAndCode(Long dimensionPk, String code);

    /**
     * QR-FIN-058 (v2) — RULE-FIN-027's fact for API-FIN-020: the value whose {@code code} equals
     * the event's business-field value inside the tagged dimension (ADR-FIN-026; served by
     * {@code UQ_FIN_DIMENSION_VALUE_DIM_CODE}, so at most one row). The service passes the result
     * into {@code DimensionValueDomain.assertResolvedByCode(...)}; an inactive match is then
     * RULE-FIN-009's business. QR-FIN-061 (a CONSTANT tag's value with its owning dimension, by
     * id) is the inherited {@code findById}: {@code DimensionValueDomain.from} reads only the
     * lazy dimension's id, which Hibernate serves without initialising the proxy.
     */
    Optional<DimensionValue> findByDimension_DimensionPkAndCode(Long dimensionPk, String code);
}
