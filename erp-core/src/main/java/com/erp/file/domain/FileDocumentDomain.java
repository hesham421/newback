package com.erp.file.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.entity.FileDocument;
import com.erp.file.exception.FileErrorCodes;
import java.util.Map;
import java.util.Set;

/**
 * Domain companion for ENTITY-FILE-001 (FileDocument) — lifecycle state-machine guardian for
 * LOV-FILE-002 (A6, RULE-FILE-006 soft-delete). Valid transitions: ACTIVE→ARCHIVED, ACTIVE→DELETED,
 * ARCHIVED→DELETED. DELETED is terminal. Soft-delete retains bytes (RULE-FILE-006). No Spring/JPA
 * annotations, no repository access; constructed only via the static factory.
 */
public final class FileDocumentDomain {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_DELETED = "DELETED";

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
        STATUS_ACTIVE, Set.of(STATUS_ARCHIVED, STATUS_DELETED),
        STATUS_ARCHIVED, Set.of(STATUS_DELETED),
        STATUS_DELETED, Set.of()
    );

    private final String currentStatus;

    private FileDocumentDomain(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    /** Reconstructs a Domain view over a persisted entity — no validation. */
    public static FileDocumentDomain from(FileDocument entity) {
        return new FileDocumentDomain(entity.getFileStatusId());
    }

    /**
     * LOV-FILE-002 (A6) — decision only: throws on an illegal transition from the current status.
     * The service calls this before mutating fileStatusId.
     */
    public void assertCanTransitionTo(String targetStatus) {
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (targetStatus == null || !allowed.contains(targetStatus)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FileErrorCodes.FILE_DOCUMENT_INVALID_TRANSITION, currentStatus, targetStatus);
        }
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
