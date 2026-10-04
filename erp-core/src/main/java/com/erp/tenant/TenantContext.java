package com.erp.tenant;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.exception.TenantErrorCodes;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The current tenant of this thread — the value Hibernate's {@code @TenantId} discriminator uses
 * for every query and insert (erp-core step 05). Part of the tenant module's public surface: any
 * module may read it, and system code may switch it with {@link #runAs}/{@link #callAs}.
 *
 * <p><b>Who sets it.</b> On a web request the core security chain sets it — from the JWT claim
 * {@code tid} when the caller is authenticated, otherwise from the {@code X-Tenant-Code} header — and
 * clears it in a {@code finally}. Code that runs outside a request (startup runners, schedulers,
 * async listeners) has no tenant and must wrap its work in {@link #runAs}/{@link #callAs}.
 *
 * <p><b>Hibernate binds the tenant when a session opens.</b> A {@code @Transactional} method gets its
 * session — and so its tenant — at transaction begin. Switching the tenant inside an open transaction
 * does not affect that transaction; call {@code runAs} <em>around</em> the transactional call (or use
 * {@code Propagation.REQUIRES_NEW} inside it) so that a new session opens under the new tenant.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** The current tenant id, or {@code null} when none is set. */
    public static Long current() {
        return CURRENT.get();
    }

    /** The current tenant id, if any. */
    public static Optional<Long> find() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * The current tenant id; fails fast with {@code TENANT_CONTEXT_MISSING} when none is set — a
     * programming error (e.g. a system job that forgot {@link #runAs}), never a client error.
     */
    public static Long require() {
        Long tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new LocalizedException(Status.INTERNAL_ERROR, TenantErrorCodes.TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }

    /** Sets the current tenant. Whoever sets it owns clearing it ({@link #clear()} in a finally). */
    public static void set(Long tenantId) {
        CURRENT.set(Objects.requireNonNull(tenantId, "tenantId"));
    }

    /** Removes the current tenant from this thread. */
    public static void clear() {
        CURRENT.remove();
    }

    /** Runs {@code action} as {@code tenantId}, then restores whatever tenant (or none) was current. */
    public static void runAs(Long tenantId, Runnable action) {
        callAs(tenantId, () -> {
            action.run();
            return null;
        });
    }

    /** Calls {@code action} as {@code tenantId}, then restores whatever tenant (or none) was current. */
    public static <T> T callAs(Long tenantId, Supplier<T> action) {
        Objects.requireNonNull(tenantId, "tenantId");
        Long previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
