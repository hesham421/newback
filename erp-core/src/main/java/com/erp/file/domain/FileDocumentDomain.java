package com.erp.file.domain;

import com.erp.common.domain.StatusTransitions;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.entity.FileDocument;
import com.erp.file.exception.FileErrorCodes;
import java.util.Collection;
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

    /** erp-core step 07 — VISIBILITY values (CHK_FILE_DOCUMENT_VISIBILITY). */
    public static final String VISIBILITY_PRIVATE = "PRIVATE";
    public static final String VISIBILITY_PUBLIC = "PUBLIC";

    private static final StatusTransitions TRANSITIONS = new StatusTransitions(Map.of(
        STATUS_ACTIVE, Set.of(STATUS_ARCHIVED, STATUS_DELETED),
        STATUS_ARCHIVED, Set.of(STATUS_DELETED),
        STATUS_DELETED, Set.of()
    ), FileErrorCodes.FILE_DOCUMENT_INVALID_TRANSITION);

    private final String currentStatus;

    private FileDocumentDomain(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    /** Reconstructs a Domain view over a persisted entity — no validation. */
    /**
     * RULE-FILE-012 (tenant-maturity C5) — a restricted document ({@code requiredAuthority} set) exists only for a caller
     * holding that authority; anyone else gets 404 {@code FILE_DOCUMENT_NOT_FOUND}, so its existence is not revealed.
     */
    public static void assertVisibleTo(Long documentId, String requiredAuthority, Collection<String> callerAuthorities) {
        if (requiredAuthority != null && !callerAuthorities.contains(requiredAuthority)) {
            throw new LocalizedException(Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, documentId);
        }
    }

    /** RULE-FILE-012 — deleting a restricted document removes its content (a tombstone row stays); others keep it (RULE-FILE-006). */
    public static boolean purgesContentOn(String requiredAuthority, String targetStatus) {
        return requiredAuthority != null && STATUS_DELETED.equals(targetStatus);
    }

    public static FileDocumentDomain from(FileDocument entity) {
        return new FileDocumentDomain(entity.getFileStatusId());
    }

    /**
     * LOV-FILE-002 (A6) — decision only: throws on an illegal transition from the current status.
     * The service calls this before mutating fileStatusId.
     */
    public void assertCanTransitionTo(String targetStatus) {
        TRANSITIONS.assertAllowed(currentStatus, targetStatus);
    }

    /**
     * erp-core step 07 — decision only: a document may become PUBLIC only when its category allows
     * public files ({@code FILE_CATEGORY.ALLOW_PUBLIC}); a document without a category never may.
     * Refused with 409 {@code FILE_PUBLIC_NOT_ALLOWED}.
     */
    public void assertCanBePublic(boolean categoryAllowsPublic) {
        if (!categoryAllowsPublic) {
            throw new LocalizedException(Status.CONFLICT, FileErrorCodes.FILE_PUBLIC_NOT_ALLOWED);
        }
    }

    /**
     * erp-core step 07 — decision only: a soft-deleted document is treated as gone (RULE-FILE-006), so
     * its visibility cannot change; answered 404 {@code FILE_DOCUMENT_NOT_FOUND} like every other access.
     */
    public void assertNotDeleted(Long documentId) {
        if (STATUS_DELETED.equals(currentStatus)) {
            throw new LocalizedException(Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, documentId);
        }
    }

    /**
     * erp-core step 07 — content types a public file may be rendered {@code inline} with: raster images
     * and PDF. Everything else (HTML, SVG, XML, JavaScript, text, ...) is served as an
     * {@code attachment}, so no tenant can host active content on the platform origin. SVG is
     * deliberately absent (it can carry script).
     */
    public static final Set<String> INLINE_SAFE_CONTENT_TYPES = Set.of(
        "image/png", "image/jpeg", "image/gif", "image/webp", "image/avif", "image/bmp",
        "application/pdf");

    /** Whether a public file of this content type may be served inline (parameters such as charset ignored). */
    public static boolean isInlineSafe(String contentType) {
        if (contentType == null) {
            return false;
        }
        int semicolon = contentType.indexOf(';');
        String base = (semicolon >= 0 ? contentType.substring(0, semicolon) : contentType).trim().toLowerCase(java.util.Locale.ROOT);
        return INLINE_SAFE_CONTENT_TYPES.contains(base);
    }

    /**
     * erp-core step 07 — whether the public URL of a document actually serves it: PUBLIC with a slug,
     * ACTIVE, and in a category that (still) allows public files. The public lookup query applies the
     * same conditions, so no URL is handed out that would answer 404.
     */
    public static boolean isPubliclyServable(String visibility, String publicSlug, String fileStatusId,
                                             Boolean categoryAllowPublic) {
        return VISIBILITY_PUBLIC.equals(visibility) && publicSlug != null
            && STATUS_ACTIVE.equals(fileStatusId) && Boolean.TRUE.equals(categoryAllowPublic);
    }

    /**
     * tenant-maturity D.4 (RULE-FILE-010) — as above, but an uncategorised document (no
     * {@code categoryId}: only the image store publishes one) needs no category permission. The public
     * lookup query applies the same conditions.
     */
    public static boolean isPubliclyServable(String visibility, String publicSlug, String fileStatusId,
                                             Long categoryId, Boolean categoryAllowPublic) {
        return categoryId == null
            ? VISIBILITY_PUBLIC.equals(visibility) && publicSlug != null && STATUS_ACTIVE.equals(fileStatusId)
            : isPubliclyServable(visibility, publicSlug, fileStatusId, categoryAllowPublic);
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
