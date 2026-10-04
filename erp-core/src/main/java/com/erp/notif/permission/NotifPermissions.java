package com.erp.notif.permission;

import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionModule;
import com.erp.sec.permission.PermissionScreen;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * NOTIF's permission catalog (erp-core step 06) — the rows {@code V7__sec_seed.sql} seeded for NOTIF,
 * with the same names. NOTIF_TEMPLATES and NOTIF_CHANNELS expose VIEW/CREATE/UPDATE/DELETE (the
 * soft-delete is the DELETE cell); NOTIF_LOG is read-only (VIEW). Dispatch (API-NOTIF-001) is tied to no
 * screen and declares no permission.
 */
@Component
public class NotifPermissions implements PermissionContributor {

    public static final String MODULE = "NOTIF";

    public static final String PERM_NOTIF_TEMPLATES_VIEW = "PERM_NOTIF_TEMPLATES_VIEW";
    public static final String PERM_NOTIF_TEMPLATES_CREATE = "PERM_NOTIF_TEMPLATES_CREATE";
    public static final String PERM_NOTIF_TEMPLATES_UPDATE = "PERM_NOTIF_TEMPLATES_UPDATE";
    /** NotificationTemplateService.deactivate(). */
    public static final String PERM_NOTIF_TEMPLATES_DELETE = "PERM_NOTIF_TEMPLATES_DELETE";
    public static final String PERM_NOTIF_CHANNELS_VIEW = "PERM_NOTIF_CHANNELS_VIEW";
    public static final String PERM_NOTIF_CHANNELS_CREATE = "PERM_NOTIF_CHANNELS_CREATE";
    public static final String PERM_NOTIF_CHANNELS_UPDATE = "PERM_NOTIF_CHANNELS_UPDATE";
    /** NotificationChannelConfigService.disable(). */
    public static final String PERM_NOTIF_CHANNELS_DELETE = "PERM_NOTIF_CHANNELS_DELETE";
    /** API-NOTIF-002, 003 — the notification log's only action. */
    public static final String PERM_NOTIF_LOG_VIEW = "PERM_NOTIF_LOG_VIEW";

    private static final PermissionScreen TEMPLATES =
        new PermissionScreen(MODULE, "NOTIF_TEMPLATES", "إدارة قوالب الإشعارات", "Notification Templates");
    private static final PermissionScreen CHANNELS =
        new PermissionScreen(MODULE, "NOTIF_CHANNELS", "تهيئة القنوات", "Channel Configuration");
    private static final PermissionScreen LOG = new PermissionScreen(MODULE, "NOTIF_LOG", "سجل الإشعارات", "Notification Log");

    @Override
    public List<PermissionModule> modules() {
        return List.of(new PermissionModule(MODULE, "خدمة الإشعارات", "Notification Service"));
    }

    @Override
    public List<PermissionScreen> screens() {
        return List.of(TEMPLATES, CHANNELS, LOG);
    }

    @Override
    public List<PermissionDef> permissions() {
        return List.of(
            PermissionDef.of(TEMPLATES, "VIEW", "عرض"),
            PermissionDef.of(TEMPLATES, "CREATE", "إنشاء"),
            PermissionDef.of(TEMPLATES, "UPDATE", "تعديل"),
            PermissionDef.of(TEMPLATES, "DELETE", "حذف"),
            PermissionDef.of(CHANNELS, "VIEW", "عرض"),
            PermissionDef.of(CHANNELS, "CREATE", "إنشاء"),
            PermissionDef.of(CHANNELS, "UPDATE", "تعديل"),
            PermissionDef.of(CHANNELS, "DELETE", "حذف"),
            PermissionDef.of(LOG, "VIEW", "عرض"));
    }
}
