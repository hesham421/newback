package com.erp.sec.repository;

import com.erp.sec.entity.RoleModuleGrant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-007 (RoleModuleGrant). Module-internal (A.2.3). QR-SEC-014 (SAVE) and
 * QR-SEC-015 (DELETE) use the inherited {@code save} / {@code delete}; QR-SEC-032's FIND_ALL
 * halves live on the repositories of the entities they return.
 */
@Repository
public interface RoleModuleGrantRepository
    extends JpaRepository<RoleModuleGrant, Long>,
            JpaSpecificationExecutor<RoleModuleGrant> {

    /**
     * QR-SEC-028 — RULE-SEC-001's fact, and API-SEC-014's grant-duplication pre-check
     * ({@code UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE}). The service resolves the screen's module
     * first. Fact only — the decision belongs to the Domain objects.
     */
    boolean existsByRole_RolePkAndModule_ModuleRegPk(Long rolePk, Long moduleRegPk);

    /**
     * QR-SEC-015 — locates the grant API-SEC-015 revokes; an empty result is the
     * {@code SEC-404-GRANT} path. {@code JOIN FETCH} loads both parents for the audit entry
     * written alongside the delete (A.2.6).
     */
    @Query("SELECT g FROM RoleModuleGrant g JOIN FETCH g.role JOIN FETCH g.module "
        + "WHERE g.role.rolePk = :rolePk AND g.module.moduleRegPk = :moduleRegPk")
    Optional<RoleModuleGrant> findByRoleAndModule(@Param("rolePk") Long rolePk,
                                                  @Param("moduleRegPk") Long moduleRegPk);

    /**
     * Every module grant one role holds — the module half of the grant-tree read. {@code JOIN
     * FETCH} loads the registry row each node is rendered from, so the tree costs one query per
     * level and never one per row.
     */
    @Query("SELECT g FROM RoleModuleGrant g JOIN FETCH g.module WHERE g.role.rolePk = :rolePk")
    List<RoleModuleGrant> findAllByRoleWithModule(@Param("rolePk") Long rolePk);
}
