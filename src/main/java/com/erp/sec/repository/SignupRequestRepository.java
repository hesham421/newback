package com.erp.sec.repository;

import com.erp.sec.entity.SignupRequest;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-013 (SignupRequest). Module-internal (A.2.3). QR-SEC-002 (SAVE) and
 * QR-SEC-011 (UPDATE) both use the inherited {@code save}; the decision path loads its row with
 * {@code findById}, whose empty result is the {@code SEC-404-SIGNUP} path.
 */
@Repository
public interface SignupRequestRepository
    extends JpaRepository<SignupRequest, Long>,
            JpaSpecificationExecutor<SignupRequest> {

    /**
     * API-SEC-002's "not an already-PENDING SignupRequest.email" half — the User half is
     * {@code UserRepository.existsByEmail}, and the plan assigns this check no QR id of its own.
     * Fact only — the decision belongs to {@code SignupRequestDomain.create(..)}.
     */
    boolean existsByEmailAndStatusCode(String email, String statusCode);

    /** QR-SEC-022 — {@code pendingSignups}, shared by the users-overview and onboarding widgets. */
    @Query("SELECT COUNT(s) FROM SignupRequest s WHERE s.statusCode = :statusCode")
    long countByStatus(@Param("statusCode") String statusCode);

    /** QR-SEC-022 — the onboarding funnel's {@code stalledCount}: still PENDING and older than the cut-off. */
    @Query("SELECT COUNT(s) FROM SignupRequest s "
        + "WHERE s.statusCode = :statusCode AND s.submittedAt < :submittedBefore")
    long countStalled(@Param("statusCode") String statusCode,
                      @Param("submittedBefore") Instant submittedBefore);
}
