package com.erp.fin.repository;

import com.erp.fin.entity.RuleLineDimension;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-016 (RuleLineDimension, v2). Module-internal (A.2.3).
 *
 * <p>QR-FIN-056 (save a line's tags with the line, API-FIN-011) uses the inherited
 * {@code saveAll} inside the line's transaction (QR-FIN-015).
 */
@Repository
public interface RuleLineDimensionRepository
    extends JpaRepository<RuleLineDimension, Long>,
            JpaSpecificationExecutor<RuleLineDimension> {

    /**
     * QR-FIN-057 — every dimension tag of the active rule's lines in one read (API-FIN-020), the
     * batch shape {@code RuleLineRepository.findByEventTypeRulePkIn} already uses. The line, the
     * dimension and the (nullable, hence LEFT) constant value are all {@code JOIN FETCH}ed so the
     * build resolves each tag without one query per row (A.2.6); the service groups by line.
     */
    @Query("SELECT d FROM RuleLineDimension d JOIN FETCH d.ruleLine JOIN FETCH d.dimension "
        + "LEFT JOIN FETCH d.dimensionValue "
        + "WHERE d.ruleLine.ruleLinePk IN :ruleLinePks "
        + "ORDER BY d.ruleLine.ruleLinePk ASC, d.ruleLineDimensionPk ASC")
    List<RuleLineDimension> findByRuleLine_RuleLinePkIn(
        @Param("ruleLinePks") List<Long> ruleLinePks);
}
