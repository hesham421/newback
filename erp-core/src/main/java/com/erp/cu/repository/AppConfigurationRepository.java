package com.erp.cu.repository;

import com.erp.cu.entity.AppConfiguration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENTITY-CU-001 (AppConfiguration). configKey is the business-key addressing
 * mechanism for this module (DRV-003) and is immutable after creation (RULE-CU-003, structurally
 * enforced — configKey is excluded from ConfigurationUpdateRequest). Per build-create-repository
 * A.2.5, an existsBy...ConfigKeyAndIdNot() variant is deliberately NOT provided: configKey can never
 * change on update.
 *
 * <p>erp-core step 09: the entity is not tenant-filtered by Hibernate (TENANT_ID NULL = platform
 * default), so <b>every method names its owner</b>: {@code ...TenantIdIsNull...} for the platform
 * defaults, {@code ...TenantId...} with the caller's tenant for overrides. Nothing here returns rows of
 * an unnamed owner.
 */
@Repository
public interface AppConfigurationRepository
    extends JpaRepository<AppConfiguration, Long>,
            JpaSpecificationExecutor<AppConfiguration> {

    /** A tenant override. */
    Optional<AppConfiguration> findByTenantIdAndConfigKey(Long tenantId, String configKey);

    /** A platform default. */
    Optional<AppConfiguration> findByTenantIdIsNullAndConfigKey(String configKey);

    boolean existsByTenantIdAndConfigKey(Long tenantId, String configKey);

    boolean existsByTenantIdIsNullAndConfigKey(String configKey);

    /** Settings resolution: the tenant's override and the platform default of one key (0, 1 or 2 rows). */
    @Query("select c from AppConfiguration c where c.configKey = :configKey"
        + " and (c.tenantId = :tenantId or c.tenantId is null)")
    List<AppConfiguration> findOverrideAndDefault(@Param("tenantId") Long tenantId,
                                                  @Param("configKey") String configKey);

    /**
     * The owner restriction every search is AND-ed with ({@code null} = platform defaults). The shared
     * {@code SpecBuilder} has no IS NULL operator and must not see TENANT_ID as a client filter, so this
     * one predicate is defined here, next to the queries it mirrors.
     */
    static Specification<AppConfiguration> ownedBy(Long tenantId) {
        return (root, query, cb) -> tenantId == null
            ? cb.isNull(root.get("tenantId"))
            : cb.equal(root.get("tenantId"), tenantId);
    }
}
