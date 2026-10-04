package com.erp.audit.permission;

import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionModule;
import com.erp.sec.permission.PermissionScreen;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The audit module's permission catalog (erp-core step 10). Not seeded by a migration: the
 * synchronizer upserts it at startup, and every super role ({@code SYS_ADMIN} of every tenant) holds it.
 *
 * <p>{@link #AUDIT_EVENT_READ} is the step file's literal code (passed explicitly, like
 * {@code FILE:DOCUMENT:PUBLISH}) and is also the screen's {@code VIEW} action — the gateway
 * RULE-SEC-007 needs, so one grant suffices.
 */
@Component
public class AuditPermissions implements PermissionContributor {

    public static final String MODULE = "AUDIT";

    /** {@code GET /api/v1/audit/events} (staff only; customers never read audit). */
    public static final String AUDIT_EVENT_READ = "AUDIT:EVENT:READ";

    private static final PermissionScreen EVENTS =
        new PermissionScreen(MODULE, "AUDIT_EVENTS", "سجل أحداث التدقيق", "Audit events");

    @Override
    public List<PermissionModule> modules() {
        return List.of(new PermissionModule(MODULE, "سجل التدقيق العام", "Audit Log"));
    }

    @Override
    public List<PermissionScreen> screens() {
        return List.of(EVENTS);
    }

    @Override
    public List<PermissionDef> permissions() {
        return List.of(PermissionDef.of(EVENTS, "VIEW", "عرض", AUDIT_EVENT_READ));
    }
}
