package com.erp.tenant.repository;

import com.erp.tenant.entity.Tenant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Tenant}. Module-internal: used by {@code TenantService} and by the tenant
 * module's own {@code TenantResolutionFilter}. {@code CORE_TENANT} is global, so no tenant
 * restriction applies to these queries whatever tenant the session belongs to.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long>, JpaSpecificationExecutor<Tenant> {

    /** Resolves {@code X-Tenant-Code} (exact, upper-case code). */
    Optional<Tenant> findByCode(String code);

    /** Code uniqueness on create (the code is immutable, so there is no "...AndIdNot" variant). */
    boolean existsByCode(String code);
}
