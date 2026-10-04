package com.erp.sec.repository;

import com.erp.sec.entity.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-002 (Role). Module-internal. QR-SEC-012 (search) and QR-SEC-013 (save)
 * are served by the inherited {@code findAll(Specification, Pageable)} / {@code save}.
 */
@Repository
public interface RoleRepository
    extends JpaRepository<Role, Long>,
            JpaSpecificationExecutor<Role> {

    /**
     * QR-SEC-034 — role code uniqueness for API-SEC-013 create. {@code code} is immutable after
     * create (DTO MEMBERSHIP: update-request excludes it), so no "...AndRolePkNot" variant exists.
     */
    boolean existsByCode(String code);

    /** QR-SEC-022 — the roles/permissions widget's {@code roleCount}. */
    @Query("SELECT COUNT(r) FROM Role r")
    long countAllRoles();

    /**
     * QR-SEC-022 — the roles/permissions widget's {@code usersPerRole}. LEFT JOIN so a role with
     * no holder still reports zero rather than dropping out of the widget.
     */
    @Query("SELECT r.rolePk AS rolePk, r.code AS code, r.nameAr AS nameAr, r.nameEn AS nameEn, "
        + "COUNT(ura.userRolePk) AS userCount "
        + "FROM Role r LEFT JOIN UserRoleAssignment ura ON ura.role = r "
        + "GROUP BY r.rolePk, r.code, r.nameAr, r.nameEn ORDER BY r.code")
    List<RoleUserCountProjection> findUserCountsPerRole();
}
