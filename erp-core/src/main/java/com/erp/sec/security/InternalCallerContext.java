package com.erp.sec.security;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The internal-caller utility build-create-service's "Internal trusted-caller calls" prescribes:
 * it installs a synthetic authentication carrying {@link #INTERNAL_AUTHORITY} for the duration of
 * one in-process call and restores the previous context in a {@code finally}. Reachable only from
 * within this JVM — {@code JwtAuthenticationFilter} strips this authority from every authority set
 * it builds, so no external request can obtain it.
 */
public final class InternalCallerContext {

    /** The one authority a principal-less in-process caller runs with; never granted by a request. */
    public static final String INTERNAL_AUTHORITY = "INTERNAL_TRUSTED_CALLER";

    private static final String INTERNAL_PRINCIPAL = "internal";

    private InternalCallerContext() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static <T> T call(Supplier<T> action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            SecurityContext internal = SecurityContextHolder.createEmptyContext();
            internal.setAuthentication(new UsernamePasswordAuthenticationToken(
                INTERNAL_PRINCIPAL, null,
                List.of(new SimpleGrantedAuthority(INTERNAL_AUTHORITY))));
            SecurityContextHolder.setContext(internal);
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
