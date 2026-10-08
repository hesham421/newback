package com.erp.events.support;

import com.erp.tenant.TenantContext;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Propagates the submitting thread's {@link TenantContext} and Spring Security context into the
 * pooled worker thread that runs the task (erp-core step 08), and restores the worker's previous
 * state afterwards — normally "nothing", so a pooled thread never carries a tenant or a principal
 * from one task into the next.
 *
 * <p>Both values are captured when the task is <em>submitted</em> (on the publishing thread, e.g.
 * right after the dispatching transaction committed), not when it runs. The security context is
 * copied into a fresh {@link SecurityContext}, so a later change on either thread never leaks into
 * the other.
 */
public class TenantAndSecurityContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Long capturedTenant = TenantContext.current();
        SecurityContext capturedSecurity = copy(SecurityContextHolder.getContext());
        return () -> {
            SecurityContext previousSecurity = SecurityContextHolder.getContext();
            SecurityContextHolder.setContext(capturedSecurity);
            try {
                // spike ADR-TENANT-004: the captured tenant is bound for the task only
                TenantContext.callScoped(capturedTenant, () -> {
                    runnable.run();
                    return null;
                });
            } finally {
                if (previousSecurity == null || previousSecurity.getAuthentication() == null) {
                    SecurityContextHolder.clearContext();
                } else {
                    SecurityContextHolder.setContext(previousSecurity);
                }
            }
        };
    }

    private static SecurityContext copy(SecurityContext source) {
        SecurityContext copy = SecurityContextHolder.createEmptyContext();
        if (source != null) {
            copy.setAuthentication(source.getAuthentication());
        }
        return copy;
    }
}
