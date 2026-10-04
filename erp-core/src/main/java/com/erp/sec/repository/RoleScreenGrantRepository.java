package com.erp.sec.repository;

import com.erp.sec.entity.RoleScreenGrant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-008 (RoleScreenGrant). Module-internal (A.2.3). QR-SEC-016 (SAVE) uses
 * the inherited {@code save}; QR-SEC-028's module-grant pre-check lives on
 * {@code RoleModuleGrantRepository}.
 */
@Repository
public interface RoleScreenGrantRepository
    extends JpaRepository<RoleScreenGrant, Long>,
            JpaSpecificationExecutor<RoleScreenGrant> {

    /**
     * QR-SEC-029 — RULE-SEC-002's fact (API-SEC-017), and API-SEC-016's duplication pre-check
     * ({@code UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN}). Fact only, never a verdict.
     */
    boolean existsByRole_RolePkAndScreen_ScreenRegPk(Long rolePk, Long screenRegPk);

    /**
     * QR-SEC-032 (screen half) — the cascade target set RULE-SEC-003 deletes when the module
     * grant is revoked (API-SEC-015). {@code JOIN FETCH} loads each grant's screen for the
     * {@code SCREEN_REVOKED} audit entries (A.2.6).
     */
    @Query("SELECT g FROM RoleScreenGrant g JOIN FETCH g.screen s "
        + "WHERE g.role.rolePk = :rolePk AND s.module.moduleRegPk = :moduleRegPk")
    List<RoleScreenGrant> findCascadeTargets(@Param("rolePk") Long rolePk,
                                             @Param("moduleRegPk") Long moduleRegPk);

    /**
     * Every screen grant one role holds — the screen half of the grant-tree read. The screen's
     * module comes with it because the tree nests each screen under it.
     */
    @Query("SELECT g FROM RoleScreenGrant g JOIN FETCH g.screen s JOIN FETCH s.module "
        + "WHERE g.role.rolePk = :rolePk")
    List<RoleScreenGrant> findAllByRoleWithScreen(@Param("rolePk") Long rolePk);
}
