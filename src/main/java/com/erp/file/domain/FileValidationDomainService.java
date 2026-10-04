package com.erp.file.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.exception.FileErrorCodes;
import java.util.Arrays;

/**
 * Cross-cutting upload validation for RULE-FILE-001 (size limits) and RULE-FILE-002 (allowed
 * content types). A stateless decision helper: it receives already-detected values (server-detected
 * MIME, measured byte sizes, the effective FileCategory limits) as plain arguments and never reads
 * multipart input or performs I/O. Defaults are overridable per FileCategory (maxSizeBytes /
 * allowedContentTypes). No Spring/JPA annotations, no persistence access.
 */
public final class FileValidationDomainService {

    /** RULE-FILE-001 default per-file content cap (5 MB). */
    public static final long DEFAULT_MAX_CONTENT_BYTES = 5L * 1024 * 1024;

    /** RULE-FILE-001 whole-request cap (10 MB). */
    public static final long MAX_REQUEST_BYTES = 10L * 1024 * 1024;

    private FileValidationDomainService() {
        throw new UnsupportedOperationException("Domain service — cannot be instantiated");
    }

    /**
     * RULE-FILE-001 — content size guard. Effective limit is the category override when present,
     * otherwise {@link #DEFAULT_MAX_CONTENT_BYTES}.
     */
    public static void assertContentSizeAllowed(long contentSizeBytes, Long categoryMaxSizeBytes) {
        long effectiveLimit = (categoryMaxSizeBytes != null && categoryMaxSizeBytes > 0)
            ? categoryMaxSizeBytes
            : DEFAULT_MAX_CONTENT_BYTES;
        if (contentSizeBytes > effectiveLimit) {
            throw new LocalizedException(Status.PAYLOAD_TOO_LARGE,
                FileErrorCodes.FILE_DOCUMENT_SIZE_EXCEEDED, contentSizeBytes, effectiveLimit);
        }
    }

    /** RULE-FILE-001 — whole-request size guard (fixed 10 MB ceiling). */
    public static void assertRequestSizeAllowed(long requestSizeBytes) {
        if (requestSizeBytes > MAX_REQUEST_BYTES) {
            throw new LocalizedException(Status.PAYLOAD_TOO_LARGE,
                FileErrorCodes.FILE_DOCUMENT_SIZE_EXCEEDED, requestSizeBytes, MAX_REQUEST_BYTES);
        }
    }

    /**
     * RULE-FILE-002 — content-type guard. {@code verifiedContentType} is the MIME the server
     * determined from the file <em>content</em> (magic bytes); it is {@code null} when the content
     * could not be verified. The client-supplied filename is never trusted for this decision, so an
     * unverifiable upload is rejected whenever a restriction applies. {@code reportedContentType} is
     * the best-effort display type used only in the error message. {@code categoryAllowedContentTypes}
     * is a comma-separated allow-list; a null/blank list means the category imposes no type
     * restriction. Matching is case-insensitive.
     */
    public static void assertContentTypeAllowed(String verifiedContentType,
                                                String reportedContentType,
                                                String categoryAllowedContentTypes) {
        if (categoryAllowedContentTypes == null || categoryAllowedContentTypes.isBlank()) {
            return;
        }
        boolean allowed = verifiedContentType != null && Arrays.stream(categoryAllowedContentTypes.split(","))
            .map(String::trim)
            .filter(type -> !type.isEmpty())
            .anyMatch(type -> type.equalsIgnoreCase(verifiedContentType.trim()));
        if (!allowed) {
            throw new LocalizedException(Status.UNSUPPORTED_MEDIA_TYPE,
                FileErrorCodes.FILE_DOCUMENT_TYPE_NOT_ALLOWED, reportedContentType);
        }
    }
}
