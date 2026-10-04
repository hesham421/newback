package com.erp.audit.exception;

/** Error codes of the generic audit log (erp-core step 10); messages in both i18n bundles. */
public final class AuditErrorCodes {

    private AuditErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** The action of an audit entry does not match {@code ^[A-Z_]{3,64}$} (argument: the action). */
    public static final String AUDIT_ACTION_INVALID = "AUDIT_ACTION_INVALID";
}
