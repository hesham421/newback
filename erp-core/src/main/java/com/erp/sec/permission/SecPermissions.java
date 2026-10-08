package com.erp.sec.permission;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * SEC's permission catalog (erp-core step 06): the {@code SEC} module, its nine screens and its
 * thirteen permissions — exactly the rows {@code V7__sec_seed.sql} seeded, with the same names, so
 * {@code PermissionCatalogSynchronizer} reproduces them identically. The constants are the authorities
 * SEC's own {@code @PreAuthorize} expressions reference
 * ({@code hasAuthority(T(com.erp.sec.permission.SecPermissions).PERM_SEC_USERS_VIEW)}); no other module
 * references them. Codes are {@code PERM_<PAGE_CODE>_<ACTION>} (DBF-SEC-050).
 */
@Component
public class SecPermissions implements PermissionContributor {

    public static final String MODULE = "SEC";

    /** API-SEC-005 — screen SEC_USERS. */
    public static final String PERM_SEC_USERS_VIEW = "PERM_SEC_USERS_VIEW";
    /** API-SEC-006 — screen SEC_USERS. */
    public static final String PERM_SEC_USERS_CREATE = "PERM_SEC_USERS_CREATE";
    /** API-SEC-007, 008, 009, 010, 011 — screen SEC_USERS. */
    public static final String PERM_SEC_USERS_UPDATE = "PERM_SEC_USERS_UPDATE";
    /** API-SEC-012 — screen SEC_ROLES. */
    public static final String PERM_SEC_ROLES_VIEW = "PERM_SEC_ROLES_VIEW";
    /** API-SEC-013 — screen SEC_ROLES. */
    public static final String PERM_SEC_ROLES_CREATE = "PERM_SEC_ROLES_CREATE";
    /** API-SEC-014, 015, 016, 017 — screen SEC_ROLES. */
    public static final String PERM_SEC_ROLES_UPDATE = "PERM_SEC_ROLES_UPDATE";
    /** Screen SEC_ROLES — registered, reserved (no gate consumes it today). */
    public static final String PERM_SEC_ROLES_DELETE = "PERM_SEC_ROLES_DELETE";
    /** API-SEC-021 — screen SEC_MODULE_REGISTRY. */
    public static final String PERM_SEC_MODULE_REGISTRY_VIEW = "PERM_SEC_MODULE_REGISTRY_VIEW";
    /** API-SEC-018, 019, 020 — screen SEC_MODULE_REGISTRY. */
    public static final String PERM_SEC_MODULE_REGISTRY_UPDATE = "PERM_SEC_MODULE_REGISTRY_UPDATE";
    /** API-SEC-022 — screen SEC_DASHBOARD; the gateway permission, per-widget VIEWs apply on top. */
    public static final String PERM_SEC_DASHBOARD_VIEW = "PERM_SEC_DASHBOARD_VIEW";
    /** API-SEC-024 — screen SEC_AUDIT_LOG; search and export share this VIEW permission. */
    public static final String PERM_SEC_AUDIT_LOG_VIEW = "PERM_SEC_AUDIT_LOG_VIEW";
    /** API-SEC-025 — screen SEC_SESSIONS. */
    public static final String PERM_SEC_SESSIONS_VIEW = "PERM_SEC_SESSIONS_VIEW";
    /** API-SEC-026 — screen SEC_SESSIONS. */
    public static final String PERM_SEC_SESSIONS_DELETE = "PERM_SEC_SESSIONS_DELETE";

    /**
     * erp-core step 06 — the single authority of a CUSTOMER-realm caller (customers hold no roles).
     * Not a catalog permission (never contributed, never granted): {@code JwtAuthenticationFilter}
     * installs it for every customer token. Customer endpoints authorize with it
     * ({@code hasRole('CUSTOMER')} is equivalent).
     */
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    /**
     * tenant-maturity B — the platform authority gating {@code SecAdminRecoveryApi} (REQ-SEC-091). Declared and
     * contributed by the tenant module ({@code TenantPermissions}); mirrored here because SEC does not depend on
     * that class. Not a SEC catalog permission (never contributed by SEC).
     */
    public static final String PLATFORM_TENANT_MANAGE = "PLATFORM_TENANT_MANAGE";

    private static final PermissionScreen LOGIN = new PermissionScreen(MODULE, "SEC_LOGIN", "تسجيل الدخول", "Login");
    private static final PermissionScreen SIGNUP = new PermissionScreen(MODULE, "SEC_SIGNUP", "إنشاء حساب", "Sign-up");
    private static final PermissionScreen PWD_RESET =
        new PermissionScreen(MODULE, "SEC_PWD_RESET", "إعادة تعيين كلمة المرور", "Forgot/reset password");
    private static final PermissionScreen USERS = new PermissionScreen(MODULE, "SEC_USERS", "المستخدمون", "Users");
    private static final PermissionScreen ROLES =
        new PermissionScreen(MODULE, "SEC_ROLES", "الأدوار والصلاحيات", "Roles & permissions");
    private static final PermissionScreen MODULE_REGISTRY = new PermissionScreen(MODULE, "SEC_MODULE_REGISTRY",
        "سجل الوحدات والشاشات والإجراءات", "Module/screen/action registry");
    private static final PermissionScreen DASHBOARD =
        new PermissionScreen(MODULE, "SEC_DASHBOARD", "لوحة تحكم المشرف", "Admin dashboard");
    private static final PermissionScreen AUDIT_LOG = new PermissionScreen(MODULE, "SEC_AUDIT_LOG", "سجل التدقيق", "Audit log");
    private static final PermissionScreen SESSIONS =
        new PermissionScreen(MODULE, "SEC_SESSIONS", "الجلسات النشطة", "Active sessions");

    @Override
    public List<PermissionModule> modules() {
        return List.of(new PermissionModule(MODULE, "الأمان", "Security"));
    }

    /** SEC_LOGIN / SEC_SIGNUP / SEC_PWD_RESET are public screens: registered, no action. */
    @Override
    public List<PermissionScreen> screens() {
        return List.of(LOGIN, SIGNUP, PWD_RESET, USERS, ROLES, MODULE_REGISTRY, DASHBOARD, AUDIT_LOG, SESSIONS);
    }

    @Override
    public List<PermissionDef> permissions() {
        return List.of(
            PermissionDef.of(USERS, "VIEW", "عرض"),
            PermissionDef.of(USERS, "CREATE", "إنشاء"),
            PermissionDef.of(USERS, "UPDATE", "تعديل"),
            PermissionDef.of(ROLES, "VIEW", "عرض"),
            PermissionDef.of(ROLES, "CREATE", "إنشاء"),
            PermissionDef.of(ROLES, "UPDATE", "تعديل"),
            PermissionDef.of(ROLES, "DELETE", "حذف"),
            PermissionDef.of(MODULE_REGISTRY, "VIEW", "عرض"),
            PermissionDef.of(MODULE_REGISTRY, "UPDATE", "تعديل"),
            PermissionDef.of(DASHBOARD, "VIEW", "عرض"),
            PermissionDef.of(AUDIT_LOG, "VIEW", "عرض"),
            PermissionDef.of(SESSIONS, "VIEW", "عرض"),
            PermissionDef.of(SESSIONS, "DELETE", "إنهاء"));
    }
}
