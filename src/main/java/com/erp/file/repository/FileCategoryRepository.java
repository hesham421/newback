package com.erp.file.repository;

import com.erp.file.entity.FileCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENTITY-FILE-002 (FileCategory). categoryCode is the immutable natural key
 * (RULE-FILE-007), so no {@code existsBy...AndIdNot} variant is provided — it can never change on
 * update. Module-internal; consumed only by the FILE services.
 */
@Repository
public interface FileCategoryRepository
    extends JpaRepository<FileCategory, Long>,
            JpaSpecificationExecutor<FileCategory> {

    /** QR-FILE-0011 (RULE-FILE-007) — categoryCode uniqueness pre-check for API-FILE-007 create. */
    boolean existsByCategoryCode(String categoryCode);
}
