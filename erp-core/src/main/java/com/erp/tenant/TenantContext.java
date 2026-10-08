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
 *
 * <p><b>Binding (spike ADR-TENANT-004).</b> The tenant lives in a {@link ScopedValue} frame: {@link #callAs} and
 * {@link #callScoped} bind a new frame for a bounded scope; {@link #set}/{@link #clear} update the innermost frame.
 * Outside any scope they fall back to a per-thread value (API compatibility), refused when the environment variable
 * {@code ERP_TENANT_CONTEXT_STRICT=true} is set (spike probe M5).
 */
public final class TenantContext {

    private static final ScopedValue<Frame> SCOPE = ScopedValue.newInstance();

    /** The value of {@link #set} outside any scope — what the {@code ThreadLocal} held before the spike. */
    private static final ThreadLocal<Long> UNSCOPED = new ThreadLocal<>();

    private static final boolean STRICT = Boolean.parseBoolean(System.getenv("ERP_TENANT_CONTEXT_STRICT"));

    private TenantContext() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** The current tenant id, or {@code null} when none is set. */
    public static Long current() {
        return SCOPE.isBound() ? SCOPE.get().tenantId : UNSCOPED.get();
    }

    /** The current tenant id, if any. */
    public static Optional<Long> find() {
        return Optional.ofNullable(current());
    }

    /**
     * The current tenant id; fails fast with {@code TENANT_CONTEXT_MISSING} when none is set — a
     * programming error (e.g. a system job that forgot {@link #runAs}), never a client error.
     */
    public static Long require() {
        Long tenantId = current();
        if (tenantId == null) {
            throw new LocalizedException(Status.INTERNAL_ERROR, TenantErrorCodes.TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }

    /** Whether the current tenant is the PLATFORM tenant; {@code false} when none is set. */
    public static boolean isPlatform() {
        return Long.valueOf(TenantConstants.PLATFORM_TENANT_ID).equals(current());
    }

    /** Sets the current tenant. Whoever sets it owns clearing it ({@link #clear()} in a finally). */
    public static void set(Long tenantId) {
        Objects.requireNonNull(tenantId, "tenantId");
        if (SCOPE.isBound()) {
            SCOPE.get().tenantId = tenantId;
        } else if (STRICT) {
            throw new IllegalStateException("TenantContext.set outside a tenant scope (ERP_TENANT_CONTEXT_STRICT)");
        } else {
            UNSCOPED.set(tenantId);
        }
    }

    /** Removes the current tenant from this thread. */
    public static void clear() {
        if (SCOPE.isBound()) {
            SCOPE.get().tenantId = null;
        } else {
            UNSCOPED.remove();
        }
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
        return ScopedValue.where(SCOPE, new Frame(tenantId)).call(action::get);
    }

    /**
     * Spike ADR-TENANT-004 — calls {@code op} in a new scope that starts with {@code tenantId} ({@code null} = none);
     * {@link #set}/{@link #clear} inside it change only this scope, which ends with {@code op}.
     */
    public static <T, X extends Throwable> T callScoped(Long tenantId, ScopedValue.CallableOp<T, X> op) throws X {
        return ScopedValue.where(SCOPE, new Frame(tenantId)).call(op);
    }

    /** One binding of the scoped value; only the thread that bound it reads or writes it. */
    private static final class Frame {

        private Long tenantId;

        private Frame(Long tenantId) {
            this.tenantId = tenantId;
        }
    }
}
