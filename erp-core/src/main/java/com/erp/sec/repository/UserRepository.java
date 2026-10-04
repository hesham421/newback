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

    /*
     * erp-core step 06 — usernames and e-mails are unique per (tenant, REALM), so a username alone no
     * longer names one row. The five lookups below keep their names and every existing staff caller,
     * but are now explicitly STAFF-realm queries (the staff flows — login, password reset, sign-up,
     * user management, actor resolution — are unchanged); the customer realm uses the realm-aware
     * variants further down.
     */

    /** QR-SEC-001 — FIND_ONE by username in the STAFF realm; resolves the staff login identity (API-SEC-001). */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.realm = 'STAFF'")
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * API-SEC-003's internal "look up user by email" step (Orchestration line), STAFF realm. Its empty
     * result never reaches the caller — the reset request answers the same generic 200 either way.
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.realm = 'STAFF'")
    Optional<User> findByEmail(@Param("email") String email);

    /** QR-SEC-033 — username uniqueness on create (API-SEC-006), STAFF realm; username is immutable on update. */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.realm = 'STAFF'")
    boolean existsByUsername(@Param("username") String username);

    /** QR-SEC-033 — email uniqueness on create (API-SEC-006) and sign-up (API-SEC-002), STAFF realm. */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.realm = 'STAFF'")
    boolean existsByEmail(@Param("email") String email);

    /**
     * QR-SEC-033 — email uniqueness on update (API-SEC-007), excluding the current row, within the
     * current row's own realm. Only email gets the "...AndPkNot" variant: it is the one unique field
     * in the user update-request body.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.userPk <> :userPk"
        + " AND u.realm = (SELECT x.realm FROM User x WHERE x.userPk = :userPk)")
    boolean existsByEmailAndUserPkNot(@Param("email") String email, @Param("userPk") Long userPk);

    /** erp-core step 06 — the identity of a token's subject in the token's realm (JwtAuthenticationFilter, customer service). */
    Optional<User> findByUsernameAndRealm(String username, String realm);

    /** erp-core step 06 — customer password reset: the customer account behind an e-mail. */
    Optional<User> findByEmailAndRealm(String email, String realm);

    /** erp-core step 06 — customer registration: e-mail uniqueness per (tenant, realm). */
    boolean existsByEmailAndRealm(String email, String realm);

    /** QR-SEC-022 — the dashboard's users-overview total (A.2.7: an explicit JPQL COUNT). */
    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();

    /** QR-SEC-022 — the users-overview ACTIVE / DISABLED sub-counts, one USER_STATUS code each. */
    @Query("SELECT COUNT(u) FROM User u WHERE u.statusCode = :statusCode")
    long countByStatus(@Param("statusCode") String statusCode);
}
