package com.erp.mdl.repository;

import com.erp.mdl.entity.LookupValue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-MDL-002 (LookupValue). Module-internal (A.2.3). QR-MDL-005 (paginated
 * search of one type's values) is satisfied by the inherited
 * {@code findAll(Specification, Pageable)} via {@code JpaSpecificationExecutor} plus the
 * shared {@code SpecBuilder}/{@code PageableBuilder} layer at the service (filtering by
 * {@code lookupType.lookupTypePk} + {@code code} LIKE) — no bespoke derived query needed.
 * QR-MDL-006/007/008/009 (save/update/deactivate/reorder) use the inherited {@code save} /
 * {@code saveAll}.
 */
@Repository
public interface LookupValueRepository
    extends JpaRepository<LookupValue, Long>,
            JpaSpecificationExecutor<LookupValue> {

    /**
     * QR-MDL-014 — RULE-MDL-002's fact and API-MDL-006's duplication pre-check
     * ({@code UQ_MDL_LOOKUP_VALUE_TYPE_CODE}). No {@code AndIdNot} variant: {@code code} is
     * immutable on update, so an update-time exclusion check would be dead code.
     */
    boolean existsByLookupType_LookupTypePkAndCode(Long lookupTypePk, String code);

    /**
     * QR-MDL-011 — the consumer API's (API-MDL-011) "active values of an active type, ordered
     * by sortOrder" read. Design choice: the type itself is resolved separately by
     * {@code LookupTypeRepository.findByKey(...)} and its {@code isActiveFl} checked by the
     * service (RULE-MDL-004); this method only needs the already-resolved
     * {@code lookupTypePk} plus a filter on the value's own {@code isActiveFl}, ordered by
     * {@code sortOrder}. Chosen over a single cross-entity JPQL join on {@code key} because
     * JPA's own {@code @ManyToOne} navigation (via the type's already-known pk) already
     * covers it — a needless join would duplicate what QR-MDL-015 already resolved.
     * {@code JOIN FETCH} avoids N+1 on the {@code lookupType} association (A.2.6).
     */
    @Query("SELECT v FROM LookupValue v JOIN FETCH v.lookupType "
        + "WHERE v.lookupType.lookupTypePk = :lookupTypePk AND v.isActiveFl = :active "
        + "ORDER BY v.sortOrder ASC")
    List<LookupValue> findActiveValuesOfActiveType(@Param("lookupTypePk") Long lookupTypePk,
                                                    @Param("active") Boolean active);
}
