package com.erp.cu.repository;

import com.erp.cu.entity.AppConfiguration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENTITY-CU-001 (AppConfiguration). configKey is the business-key addressing
 * mechanism for this module (DRV-003) and is immutable after creation (RULE-CU-003, structurally
 * enforced — configKey is excluded from ConfigurationUpdateRequest). Per build-create-repository
 * A.2.5, an existsByConfigKeyAndIdNot() variant is deliberately NOT provided: it would only ever
 * be needed to exclude the current row's own key during an update-time uniqueness re-check, and
 * since configKey can never change on update, that re-check never happens — the variant would be
 * dead code.
 */
@Repository
public interface AppConfigurationRepository
    extends JpaRepository<AppConfiguration, Long>,
            JpaSpecificationExecutor<AppConfiguration> {

    Optional<AppConfiguration> findByConfigKey(String configKey);

    boolean existsByConfigKey(String configKey);
}
