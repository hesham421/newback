package com.erp.sec.repository;

import com.erp.sec.entity.UserRoleAssignment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-003 (UserRoleAssignment). Module-internal (A.2.3). QR-SEC-008 (SAVE,
 * batch) is the inherited {@code saveAll} / {@code deleteAll} pair implementing API-SEC-008's
 * "replace the assignment set". QR-SEC-031 is declared once on
 * {@code RoleActionGrantRepository}.
 */
@Repository
public interface UserRoleAssignmentRepository
    extends JpaRepository<UserRoleAssignment, Long>,
            JpaSpecificationExecutor<UserRoleAssignment> {

    /**
     * QR-SEC-008 — the current assignment set for a user, which API-SEC-008 replaces and
     * API-SEC-008's response renders as {@code roles: [{roleId, code, nameAr, nameEn}]}.
     * {@code JOIN FETCH} loads each assignment's Role in the same select (A.2.6, no N+1).
     */
    @Query("SELECT ura FROM UserRoleAssignment ura JOIN FETCH ura.role "
        + "WHERE ura.user.userPk = :userPk")
    List<UserRoleAssignment> findByUser(@Param("userPk") Long userPk);

    /**
     * The same set for a whole page of users in ONE select — API-SEC-005 renders {@code roles} for
     * every row, and asking per row would be a query per row (A.2.6). {@code JOIN FETCH} loads each
     * assignment's Role alongside it, exactly as {@link #findByUser} does. The caller must not pass
     * an empty collection: {@code IN ()} is a syntax error on Postgres.
     */
    @Query("SELECT ura FROM UserRoleAssignment ura JOIN FETCH ura.role "
        + "WHERE ura.user.userPk IN :userPks")
    List<UserRoleAssignment> findByUserIn(@Param("userPks") Collection<Long> userPks);
}
