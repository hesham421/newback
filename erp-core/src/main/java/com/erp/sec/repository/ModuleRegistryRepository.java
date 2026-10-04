package com.erp.sec.repository;

import com.erp.sec.entity.ModuleRegistry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-004 (ModuleRegistry). Module-internal. QR-SEC-018 (save) and QR-SEC-021
 * (registry-tree search) are served by the inherited {@code save} / {@code findAll}.
 */
@Repository
public interface ModuleRegistryRepository
    extends JpaRepository<ModuleRegistry, Long>,
            JpaSpecificationExecutor<ModuleRegistry> {

    /**
     * QR-SEC-035 — module code uniqueness for API-SEC-018 register-module, and the
     * "module registered?" half of QR-SEC-036 (RULE-SEC-004) for API-SEC-019. {@code code} is
     * immutable after create, so no "...AndModuleRegPkNot" variant exists.
     */
    boolean existsByCode(String code);

    /**
     * API-SEC-019's "persist (QR-SEC-019)" step needs the ModuleRegistry row itself, not just the
     * RULE-SEC-004 verdict: {@code FK_SCREEN_REG_MODULE} is the screen's owning-module reference.
     */
    Optional<ModuleRegistry> findByCode(String code);

    /**
     * QR-MDL-012 — added by MDL's SVC-API-CRUD sub (XM-MDL-001) for {@code SecModuleRegistryApi}'s
     * "is this module registered and active?" existence check (RULE-MDL-001). Additive only —
     * every other method on this interface is unchanged.
     */
    boolean existsByCodeAndIsActiveFlTrue(String code);

    /**
     * QR-SEC-027 — the modules one caller's roles reach (REQ-SEC-021/032). An inactive role is not
     * an effective grant: deactivation is SEC's only withdrawal (SCR-REQ-SEC-005 B4).
     */
    @Query("SELECT m FROM ModuleRegistry m WHERE m.isActiveFl = TRUE AND EXISTS ("
        + "  SELECT 1 FROM RoleModuleGrant g WHERE g.module = m AND g.role.isActiveFl = TRUE "
        + "  AND g.role.rolePk IN ("
        + "    SELECT ura.role.rolePk FROM UserRoleAssignment ura WHERE ura.user.userPk = :userPk)) "
        + "ORDER BY m.code")
    List<ModuleRegistry> findEffectiveModulesForUser(@Param("userPk") Long userPk);
}
