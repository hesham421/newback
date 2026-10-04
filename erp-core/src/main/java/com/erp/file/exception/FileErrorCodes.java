package com.erp.file.exception;

/**
 * Module-specific error codes for File Service (FILE).
 * Descriptive &lt;ENTITY&gt;_&lt;SCENARIO&gt; format (never the numbered ERR-xxxx tracking id).
 * Codes map to SECTION A — ERROR CATALOG (ERR-0001..0006) plus the internal state-machine
 * invariant (RULE-FILE-006). Not-found is split per entity (both under ERR-0006 PLATFORM-STD).
 */
public final class FileErrorCodes {

    private FileErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** ERR-0001 (RULE-FILE-001) — content/request size exceeds the allowed (or per-category) limit. */
    public static final String FILE_DOCUMENT_SIZE_EXCEEDED = "FILE_DOCUMENT_SIZE_EXCEEDED";

    /** ERR-0002 (RULE-FILE-002) — server-detected content type outside the category's allowed types. */
    public static final String FILE_DOCUMENT_TYPE_NOT_ALLOWED = "FILE_DOCUMENT_TYPE_NOT_ALLOWED";

    /** ERR-0003 (RULE-FILE-003) — AES/GCM access token invalid, tampered, or expired. */
    public static final String FILE_ACCESS_TOKEN_INVALID = "FILE_ACCESS_TOKEN_INVALID";

    /** ERR-0004 (RULE-FILE-005) — ownerId/ownerType/moduleCode missing on upload. */
    public static final String FILE_DOCUMENT_OWNERSHIP_REQUIRED = "FILE_DOCUMENT_OWNERSHIP_REQUIRED";

    /** ERR-0005 (RULE-FILE-007) — duplicate categoryCode on category create. */
    public static final String FILE_CATEGORY_CODE_DUPLICATE = "FILE_CATEGORY_CODE_DUPLICATE";

    /** RULE-FILE-002 — an upload referenced a deactivated (retired) category. */
    public static final String FILE_CATEGORY_INACTIVE = "FILE_CATEGORY_INACTIVE";

    /** API-FILE-008 — the requested runtime lookup key is not a known FILE LOV. */
    public static final String FILE_LOOKUP_KEY_UNKNOWN = "FILE_LOOKUP_KEY_UNKNOWN";

    /** RULE-FILE-006 (A6) — illegal fileStatusId lifecycle transition (internal invariant). */
    public static final String FILE_DOCUMENT_INVALID_TRANSITION = "FILE_DOCUMENT_INVALID_TRANSITION";

    /** ERR-0006 (PLATFORM-STD) — file document not found by id. */
    public static final String FILE_DOCUMENT_NOT_FOUND = "FILE_DOCUMENT_NOT_FOUND";

    /** ERR-0006 (PLATFORM-STD) — file category not found by id. */
    public static final String FILE_CATEGORY_NOT_FOUND = "FILE_CATEGORY_NOT_FOUND";
}
