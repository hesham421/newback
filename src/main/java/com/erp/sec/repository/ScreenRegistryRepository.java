package com.erp.sec.repository;

import com.erp.sec.entity.ScreenRegistry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-005 (ScreenRegistry). Module-internal.
 * <p>
 * QR-SEC-019 (save) is served by the inherited {@code save}.
 */
@Repository
public interface ScreenRegistryRepository
    extends JpaRepository<ScreenRegistry, Long>,
            JpaSpecificationExecutor<ScreenRegistry> {

    /**
     * QR-SEC-036 — page code uniqueness for API-SEC-019 register-screen, and the
     * "screen exists?" half of QR-SEC-037 for API-SEC-020. {@code pageCode} is the immutable
     * natural key, so no "...AndScreenRegPkNot" variant exists.
     */
    boolean existsByPageCode(String pageCode);

    /**
     * API-SEC-020's "resolve screen by pageCode" step — the action's {@code FK_ACTION_REG_SCREEN}
     * needs the ScreenRegistry row, not just the "screen exists?" verdict.
     */
    Optional<ScreenRegistry> findByPageCode(String pageCode);

    /** QR-SEC-021 — the active screens hanging under one page of registry modules (API-SEC-021). */
    @Query("SELECT s FROM ScreenRegistry s JOIN FETCH s.module m "
        + "WHERE s.isActiveFl = TRUE AND m.moduleRegPk IN :moduleRegPks ORDER BY s.pageCode")
    List<ScreenRegistry> findActiveByModules(@Param("moduleRegPks") List<Long> moduleRegPks);

    /** QR-SEC-027 — the screens one caller's roles reach; same inactive-role exclusion as the module query. */
    @Query("SELECT s FROM ScreenRegistry s JOIN FETCH s.module m "
        + "WHERE s.isActiveFl = TRUE AND m.isActiveFl = TRUE AND EXISTS ("
        + "  SELECT 1 FROM RoleScreenGrant g WHERE g.screen = s AND g.role.isActiveFl = TRUE "
        + "  AND g.role.rolePk IN ("
        + "    SELECT ura.role.rolePk FROM UserRoleAssignment ura WHERE ura.user.userPk = :userPk)) "
        + "ORDER BY s.pageCode")
    List<ScreenRegistry> findEffectiveScreensForUser(@Param("userPk") Long userPk);
}
