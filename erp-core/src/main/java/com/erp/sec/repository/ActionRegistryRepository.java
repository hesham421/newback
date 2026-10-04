package com.erp.sec.repository;

import com.erp.sec.entity.ActionRegistry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-006 (ActionRegistry). Module-internal.
 * <p>
 * QR-SEC-020 (save) is served by the inherited {@code save}.
 */
@Repository
public interface ActionRegistryRepository
    extends JpaRepository<ActionRegistry, Long>,
            JpaSpecificationExecutor<ActionRegistry> {

    /**
     * QR-SEC-037 — uniqueness of the server-derived permission code for API-SEC-020
     * register-action. {@code permissionCode} is derived and never updated, so no
     * "...AndActionRegPkNot" variant exists.
     */
    boolean existsByPermissionCode(String permissionCode);

    /** erp-core step 06 — the row behind a contributed permission (PermissionCatalogSynchronizer upsert). */
    Optional<ActionRegistry> findByPermissionCode(String permissionCode);

    /**
     * erp-core step 06 — every active catalog authority (active action, screen and module), except the
     * modules named in {@code excludedModules}: what a super role holds (MenuService).
     */
    @Query("SELECT a.permissionCode FROM ActionRegistry a JOIN a.screen s JOIN s.module m "
        + "WHERE a.isActiveFl = TRUE AND s.isActiveFl = TRUE AND m.isActiveFl = TRUE "
        + "AND m.code NOT IN :excludedModules")
    List<String> findActiveAuthorityCodesExcludingModules(@Param("excludedModules") List<String> excludedModules);

    /** QR-SEC-021 — the active actions hanging under one page of registry screens (API-SEC-021). */
    @Query("SELECT a FROM ActionRegistry a JOIN FETCH a.screen s "
        + "WHERE a.isActiveFl = TRUE AND s.screenRegPk IN :screenRegPks ORDER BY a.actionCode")
    List<ActionRegistry> findActiveByScreens(@Param("screenRegPks") List<Long> screenRegPks);
}
