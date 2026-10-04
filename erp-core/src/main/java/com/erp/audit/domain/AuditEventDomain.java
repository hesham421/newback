package com.erp.audit.domain;

import com.erp.audit.crossmodule.AuditApi;
import com.erp.audit.crossmodule.AuditChange;
import com.erp.audit.exception.AuditErrorCodes;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The rules of an audit row (erp-core step 10): which actions are valid and which changes may be
 * written. A plain object — no Spring, no persistence.
 */
public final class AuditEventDomain {

    /** {@code ACTION}: free, but 3 to 64 upper-case letters or underscores. */
    public static final Pattern ACTION_PATTERN = Pattern.compile("^[A-Z_]{3,64}$");

    /**
     * Never written to {@code CHANGES} by the entity listener: the audit columns, the optimistic lock
     * and the tenant discriminator (noise on every row, and the row carries actor and tenant itself).
     */
    public static final Set<String> TECHNICAL_FIELDS =
        Set.of("createdBy", "createdAt", "updatedBy", "updatedAt", "version", "tenantId");

    private final String action;

    private AuditEventDomain(String action) {
        this.action = action;
    }

    /** Validates the action; throws {@code AUDIT_ACTION_INVALID} (400) otherwise. */
    public static AuditEventDomain create(String action) {
        if (action == null || !ACTION_PATTERN.matcher(action).matches()) {
            throw new LocalizedException(Status.VALIDATION_ERROR, AuditErrorCodes.AUDIT_ACTION_INVALID,
                String.valueOf(action));
        }
        return new AuditEventDomain(action);
    }

    public String getAction() {
        return action;
    }

    /** Whether a field name contains a word of the global denylist ({@link AuditApi#SENSITIVE_FIELD_WORDS}). */
    public static boolean isSensitive(String field) {
        if (field == null) {
            return true;
        }
        String lower = field.toLowerCase(Locale.ROOT);
        return AuditApi.SENSITIVE_FIELD_WORDS.stream().anyMatch(lower::contains);
    }

    /**
     * Whether the entity listener may write {@code field}: not sensitive, not technical and not in the
     * entity's own {@code @Audited(ignore = ...)} list.
     */
    public static boolean isRecordable(String field, Collection<String> ignored) {
        return !isSensitive(field) && !TECHNICAL_FIELDS.contains(field) && !ignored.contains(field);
    }

    /** The changes that may be written: sensitive fields dropped, whoever supplied them. */
    public List<AuditChange> recordableChanges(List<AuditChange> changes) {
        if (changes == null) {
            return List.of();
        }
        return changes.stream().filter(Objects::nonNull).filter(change -> !isSensitive(change.field())).toList();
    }
}
