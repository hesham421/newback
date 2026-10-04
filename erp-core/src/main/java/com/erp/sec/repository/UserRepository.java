package com.erp.sec.repository;

import com.erp.sec.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-001 (User). Module-internal — injected only by SEC's own services. Only
 * the plan's REPOSITORY OPS are declared: QR-SEC-005/006/007/009/010 are served by the inherited
 * {@code findAll(Specification, Pageable)} / {@code save} / {@code findById}.
 */
@Repository
public interface UserRepository
    extends JpaRepository<User, Long>,
            JpaSpecificationExecutor<User> {

    /** QR-SEC-001 — FIND_ONE by username; resolves the login identity for API-SEC-001. */
    Optional<User> findByUsername(String username);

    /**
     * API-SEC-003's internal "look up user by email" step (Orchestration line). Its empty result
     * never reaches the caller — the reset request answers the same generic 200 either way.
     */
    Optional<User> findByEmail(String email);

    /** QR-SEC-033 — username uniqueness on create (API-SEC-006); username is immutable on update. */
    boolean existsByUsername(String username);

    /** QR-SEC-033 — email uniqueness on create (API-SEC-006). */
    boolean existsByEmail(String email);

    /**
     * QR-SEC-033 — email uniqueness on update (API-SEC-007), excluding the current row. Only email
     * gets the "...AndPkNot" variant: it is the one unique field in the user update-request body.
     */
    boolean existsByEmailAndUserPkNot(String email, Long userPk);

    /** QR-SEC-022 — the dashboard's users-overview total (A.2.7: an explicit JPQL COUNT). */
    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();

    /** QR-SEC-022 — the users-overview ACTIVE / DISABLED sub-counts, one USER_STATUS code each. */
    @Query("SELECT COUNT(u) FROM User u WHERE u.statusCode = :statusCode")
    long countByStatus(@Param("statusCode") String statusCode);
}
