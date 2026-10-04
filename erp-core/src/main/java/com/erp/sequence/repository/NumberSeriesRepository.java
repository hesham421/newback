package com.erp.sequence.repository;

import com.erp.sequence.entity.NumberSeries;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

/**
 * Repository of {@link NumberSeries} (erp-core step 09). Module-internal. Every query is JPQL on a
 * tenant-aware entity, so Hibernate adds {@code TENANT_ID = <current tenant>} to it — the two locking
 * finders included ({@code SELECT ... WHERE ... AND tenant_id = ? FOR UPDATE}); there is no native SQL.
 *
 * <p>No {@code existsByCodeAndIdNot}: the code is immutable.
 */
@Repository
public interface NumberSeriesRepository
    extends JpaRepository<NumberSeries, Long>,
            JpaSpecificationExecutor<NumberSeries> {

    /** Code uniqueness on create. */
    boolean existsByCode(String code);

    /**
     * Allocation, step 1: locks the series' anchor row (lowest id of the code) — every allocation of a
     * code takes this lock first, which serialises them and makes creating a new period row race-free.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<NumberSeries> findFirstByCodeOrderByIdAsc(String code);

    /** Allocation, step 2: locks the row of the current period (when it is not the anchor itself). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<NumberSeries> findByCodeAndPeriodKey(String code, String periodKey);

    /** Preview: the anchor row, read without a lock. */
    Optional<NumberSeries> readFirstByCodeOrderByIdAsc(String code);

    /** Preview: the current period row, read without a lock. */
    Optional<NumberSeries> readByCodeAndPeriodKey(String code, String periodKey);

    /** Admin update/activate/deactivate: every period row of a code, kept in step. */
    List<NumberSeries> findAllByCodeOrderByIdAsc(String code);
}
