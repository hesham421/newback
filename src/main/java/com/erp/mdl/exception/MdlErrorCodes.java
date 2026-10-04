package com.erp.mdl.exception;

/**
 * Module-specific error codes for Master Data Lookup (MDL) — one constant per row of the MDL
 * v1 API spec's Errors column (SVC-API-CRUD.md / SVC-API-SEARCH.md). The constant value is the
 * runtime code verbatim ({@code MDL-{http}[-{SLUG}]}, per CORE.md's
 * {@code error_code_format}), and is both the wire {@code code} in the {@code ApiError}
 * envelope and the i18n message key — same convention as {@code SecErrorCodes}.
 *
 * <p>Only the codes thrown by this sub's Domain layer (LookupTypeDomain, LookupValueDomain)
 * are registered here, plus the service-layer not-found codes named in SVC-API-CRUD.md /
 * SVC-API-SEARCH.md ({@code MDL-404-TYPE}, {@code MDL-404-VALUE}, {@code MDL-400-REORDER-
 * MISMATCH} added by the SVC-API-CRUD sub; {@code MDL-404-TYPE-KEY} added by this
 * SVC-API-SEARCH sub for API-MDL-011 / RULE-MDL-004).
 */
public final class MdlErrorCodes {

    private MdlErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /**
     * RULE-MDL-001 — a lookup type registration whose owner module code has no ModuleRegistry
     * row in SEC (checked via XM-MDL-001 / QR-MDL-012).
     * API: API-MDL-002. HTTP 409.
     */
    public static final String MDL_409_MODULE_NOT_REGISTERED = "MDL-409-MODULE-NOT-REGISTERED";

    /**
     * PLATFORM-STD (uniqueness, QR-MDL-013) — duplicate lookup type key.
     * API: API-MDL-002. HTTP 409.
     */
    public static final String MDL_409_TYPE_DUP = "MDL-409-TYPE-DUP";

    /**
     * RULE-MDL-002 — duplicate lookup value code within the same lookup type (QR-MDL-014).
     * API: API-MDL-006. HTTP 409.
     */
    public static final String MDL_409_VALUE_DUP = "MDL-409-VALUE-DUP";

    /**
     * PLATFORM-STD (not found) — unknown lookup type id.
     * APIs: API-MDL-003, 004, 006 (parent lookup). HTTP 404.
     */
    public static final String MDL_404_TYPE = "MDL-404-TYPE";

    /**
     * PLATFORM-STD (not found) — unknown lookup value id.
     * APIs: API-MDL-007, 008. HTTP 404.
     */
    public static final String MDL_404_VALUE = "MDL-404-VALUE";

    /**
     * PLATFORM-STD (referential) — a reordered id does not belong to the given lookup type.
     * API: API-MDL-009. HTTP 400.
     */
    public static final String MDL_400_REORDER_MISMATCH = "MDL-400-REORDER-MISMATCH";

    /**
     * RULE-MDL-004 — the consumer read's type-key resolution: unknown key OR a key that
     * resolves to an inactive type both surface as this single code (API-MDL-011's own Errors
     * line does not distinguish the two cases; QR-MDL-015).
     * API: API-MDL-011. HTTP 404.
     */
    public static final String MDL_404_TYPE_KEY = "MDL-404-TYPE-KEY";
}
