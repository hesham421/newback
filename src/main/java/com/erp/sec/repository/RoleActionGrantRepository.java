package com.erp.sec.repository;

import com.erp.sec.entity.RoleActionGrant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-009 (RoleActionGrant). Module-internal (A.2.3). Every method returns a
 * FACT, never a verdict — RULE-SEC-002/005/007 are decided by {@code RoleActionGrantDomain} /
 * {@code UserRoleAssignmentDomain}. QR-SEC-029 lives on {@code RoleScreenGrantRepository}.
 */
@Repository
public interface RoleActionGrantRepository
    extends JpaRepository<RoleActionGrant, Long>,
            JpaSpecificationExecutor<RoleActionGrant> {

    /**
     * API-SEC-017 grant-duplication pre-check, backed by
     * {@code UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION} → {@code SEC-409-GRANT-DUP}.
     */
    boolean existsByRole_RolePkAndAction_ActionRegPk(Long rolePk, Long actionRegPk);

    /**
     * QR-SEC-030 — RULE-SEC-007's fact. The gateway code is passed in rather than hard-coded so
     * the query carries no business constant
     * ({@code RoleActionGrantDomain.GATEWAY_ACTION_CODE}).
     */
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN TRUE ELSE FALSE END FROM RoleActionGrant g "
        + "WHERE g.role.rolePk = :rolePk "
        + "AND g.action.screen.screenRegPk = :screenRegPk "
        + "AND g.action.actionCode = :gatewayActionCode")
    boolean existsGatewayGrantForScreen(@Param("rolePk") Long rolePk,
                                        @Param("screenRegPk") Long screenRegPk,
                                        @Param("gatewayActionCode") String gatewayActionCode);

    /**
     * QR-SEC-031 (API-SEC-008 shape) — does this user already hold the given action through ANY
     * currently assigned role? Spans SEC_USER_ROLE → SEC_ROLE_ACTION_GRANT, hence JPQL.
     */
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN TRUE ELSE FALSE END FROM RoleActionGrant g "
        + "WHERE g.action.actionRegPk = :actionRegPk "
        + "AND g.role.rolePk IN ("
        + "  SELECT ura.role.rolePk FROM UserRoleAssignment ura WHERE ura.user.userPk = :userPk)")
    boolean existsActionHeldByUser(@Param("userPk") Long userPk,
                                   @Param("actionRegPk") Long actionRegPk);

    /**
     * QR-SEC-031 (API-SEC-017 shape) — does ANY user holding this role already hold the given
     * action, through any of their roles? API-SEC-017's "check RULE-SEC-005 across every user
     * holding this role".
     */
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN TRUE ELSE FALSE END FROM RoleActionGrant g "
        + "WHERE g.action.actionRegPk = :actionRegPk "
        + "AND g.role.rolePk IN ("
        + "  SELECT held.role.rolePk FROM UserRoleAssignment held WHERE held.user.userPk IN ("
        + "    SELECT target.user.userPk FROM UserRoleAssignment target "
        + "    WHERE target.role.rolePk = :rolePk))")
    boolean existsActionHeldByAnyUserOfRole(@Param("rolePk") Long rolePk,
                                            @Param("actionRegPk") Long actionRegPk);

    /**
     * QR-SEC-032 (action half) — the cascade target set RULE-SEC-003 deletes with the module
     * grant (API-SEC-015). {@code JOIN FETCH} loads each grant's action and that action's screen
     * for the {@code ACTION_REVOKED} audit entries (A.2.6).
     */
    @Query("SELECT g FROM RoleActionGrant g JOIN FETCH g.action a JOIN FETCH a.screen s "
        + "WHERE g.role.rolePk = :rolePk AND s.module.moduleRegPk = :moduleRegPk")
    List<RoleActionGrant> findCascadeTargets(@Param("rolePk") Long rolePk,
                                             @Param("moduleRegPk") Long moduleRegPk);

    /**
     * QR-SEC-022 — {@code privilegedRoleCount}. The gateway code is a parameter so the query
     * carries no business constant (same treatment as QR-SEC-030 above).
     */
    @Query("SELECT COUNT(DISTINCT g.role.rolePk) FROM RoleActionGrant g "
        + "WHERE g.action.actionCode <> :gatewayActionCode")
    long countPrivilegedRoles(@Param("gatewayActionCode") String gatewayActionCode);

    /**
     * QR-SEC-027, permission-code shape — the caller's effective action grants, the read path
     * REQ-SEC-023's per-widget filter and CORE.md's REQ-SEC-033 gateway both name.
     */
    @Query("SELECT DISTINCT a.permissionCode FROM RoleActionGrant g "
        + "JOIN g.action a JOIN a.screen s JOIN s.module m "
        + "WHERE a.isActiveFl = TRUE AND g.role.isActiveFl = TRUE "
        + "AND s.isActiveFl = TRUE AND m.isActiveFl = TRUE "
        + "AND g.role.rolePk IN ("
        + "  SELECT ura.role.rolePk FROM UserRoleAssignment ura WHERE ura.user.userPk = :userPk)")
    List<String> findEffectivePermissionCodesForUser(@Param("userPk") Long userPk);

    /**
     * QR-SEC-027, registry shape — the same effective grants as
     * {@link #findEffectivePermissionCodesForUser}, each carrying the {@code SEC_SCREEN_REG} row and
     * {@code SEC_ACTION_REG} action code behind it, so RULE-SEC-007's gateway can be decided on
     * registry facts instead of on the permission code's text. One query for the whole authority
     * set: the screen of every held code arrives with it, so the gateway costs no further round
     * trip (REQ-SEC-033 runs this on every authenticated request).
     */
    @Query("SELECT DISTINCT new com.erp.sec.repository.EffectiveGrantProjection("
        + "a.permissionCode, s.screenRegPk, a.actionCode) FROM RoleActionGrant g "
        + "JOIN g.action a JOIN a.screen s JOIN s.module m "
        + "WHERE a.isActiveFl = TRUE AND g.role.isActiveFl = TRUE "
        + "AND s.isActiveFl = TRUE AND m.isActiveFl = TRUE "
        + "AND g.role.rolePk IN ("
        + "  SELECT ura.role.rolePk FROM UserRoleAssignment ura WHERE ura.user.userPk = :userPk)")
    List<EffectiveGrantProjection> findEffectiveGrantsForUser(@Param("userPk") Long userPk);

    /**
     * QR-SEC-039 (REQ-SEC-035) — the user ids holding {@code permissionCode} through an active
     * role: the inverse of {@link #findEffectivePermissionCodesForUser}, with the same active-flag
     * predicates. Caller: {@code UserService.findUserIdsHoldingPermission} (A.2.9).
     */
    @Query("SELECT DISTINCT ura.user.userPk FROM UserRoleAssignment ura "
        + "WHERE ura.role.rolePk IN ("
        + "  SELECT g.role.rolePk FROM RoleActionGrant g "
        + "  JOIN g.action a JOIN a.screen s JOIN s.module m "
        + "  WHERE a.permissionCode = :permissionCode "
        + "  AND a.isActiveFl = TRUE AND g.role.isActiveFl = TRUE "
        + "  AND s.isActiveFl = TRUE AND m.isActiveFl = TRUE)")
    List<Long> findUserIdsHoldingPermission(@Param("permissionCode") String permissionCode);

    /**
     * Every action grant one role holds — the action half of the grant-tree read. The action's
     * screen and that screen's module come with it because the tree nests each action under them.
     * No active-flag predicate: this is an audit of what the role holds, not an effective-access
     * resolution, so a grant on a deactivated registry row must still be visible.
     */
    @Query("SELECT g FROM RoleActionGrant g JOIN FETCH g.action a JOIN FETCH a.screen s "
        + "JOIN FETCH s.module WHERE g.role.rolePk = :rolePk")
    List<RoleActionGrant> findAllByRoleWithAction(@Param("rolePk") Long rolePk);
}
