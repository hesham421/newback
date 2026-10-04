package com.erp.audit.crossmodule;

import java.util.List;

/**
 * The generic audit log's write API (erp-core step 10) — usable by every core module and by
 * applications. Reading is the staff-only {@code GET /api/v1/audit/events}.
 *
 * <p><b>Transaction semantics.</b> {@link #record(AuditEntry)} inserts synchronously, in the caller's
 * transaction (on its JDBC connection): if the caller's business change rolls back, so does its audit
 * row, and an audit row exists only for a committed change. Called outside a transaction it commits
 * on its own.
 *
 * <p>For field-level history of an entity, annotate the entity with {@link Audited} instead of
 * calling this API.
 */
public interface AuditApi {

    String ACTION_CREATE = "CREATE";
    String ACTION_UPDATE = "UPDATE";
    String ACTION_DELETE = "DELETE";
    String ACTION_STATUS_CHANGE = "STATUS_CHANGE";
    String ACTION_LOGIN = "LOGIN";
    String ACTION_LOGOUT = "LOGOUT";
    String ACTION_PASSWORD_RESET = "PASSWORD_RESET";

    String REALM_STAFF = "STAFF";
    String REALM_CUSTOMER = "CUSTOMER";
    String REALM_SYSTEM = "SYSTEM";

    /**
     * The global denylist: a change whose field name contains one of these words (case-insensitive)
     * is never written to {@code CHANGES}, whoever records it. The step-10 list is
     * {@code password, secret, token, hash}; {@code credential, apikey, privatekey, salt} extend it.
     */
    List<String> SENSITIVE_FIELD_WORDS =
        List.of("password", "secret", "token", "hash", "credential", "apikey", "privatekey", "salt");

    /**
     * Records one audit row (defaults: see {@link AuditEntry}).
     *
     * @throws com.erp.common.exception.LocalizedException {@code AUDIT_ACTION_INVALID} (400) when the
     *         action does not match {@code ^[A-Z_]{3,64}$}; {@code TENANT_CONTEXT_MISSING} when no
     *         tenant is given and none is current
     */
    void record(AuditEntry entry);
}
