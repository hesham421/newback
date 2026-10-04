package com.erp.sec.repository;

import com.erp.sec.entity.CustomerVerifyToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for {@link CustomerVerifyToken} (erp-core step 06). Module-internal. */
@Repository
public interface CustomerVerifyTokenRepository
    extends JpaRepository<CustomerVerifyToken, Long>,
            JpaSpecificationExecutor<CustomerVerifyToken> {

    /** The token behind a submitted raw value (looked up by its hash), with its user. */
    @Query("SELECT t FROM CustomerVerifyToken t JOIN FETCH t.user WHERE t.tokenHash = :tokenHash")
    Optional<CustomerVerifyToken> findByTokenHash(@Param("tokenHash") String tokenHash);
}
