package com.erp.file.permission;

import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionModule;
import com.erp.sec.permission.PermissionScreen;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * FILE's permission catalog (erp-core step 06) — the rows {@code V7__sec_seed.sql} seeded for FILE,
 * with the same names. FILE_CATEGORIES is full CRUD; FILE_BROWSER is VIEW + CREATE (upload) + UPDATE
 * (archive) + DELETE, the last two picked by {@code FileService.softDelete}'s action argument.
 */
@Component
public class FilePermissions implements PermissionContributor {

    public static final String MODULE = "FILE";

    public static final String PERM_FILE_CATEGORIES_VIEW = "PERM_FILE_CATEGORIES_VIEW";
    public static final String PERM_FILE_CATEGORIES_CREATE = "PERM_FILE_CATEGORIES_CREATE";
    public static final String PERM_FILE_CATEGORIES_UPDATE = "PERM_FILE_CATEGORIES_UPDATE";
    /** FileCategoryService.deactivate(). */
    public static final String PERM_FILE_CATEGORIES_DELETE = "PERM_FILE_CATEGORIES_DELETE";
    /** File metadata, owner list, download token; the FILE_BROWSER gateway. */
    public static final String PERM_FILE_BROWSER_VIEW = "PERM_FILE_BROWSER_VIEW";
    /** Upload (FileService.store()). */
    public static final String PERM_FILE_BROWSER_CREATE = "PERM_FILE_BROWSER_CREATE";
    /** API-FILE-006 with action=ARCHIVE. */
    public static final String PERM_FILE_BROWSER_UPDATE = "PERM_FILE_BROWSER_UPDATE";
    /** API-FILE-006 with action=DELETE. */
    public static final String PERM_FILE_BROWSER_DELETE = "PERM_FILE_BROWSER_DELETE";

    private static final PermissionScreen CATEGORIES =
        new PermissionScreen(MODULE, "FILE_CATEGORIES", "إدارة فئات الملفات", "File Categories");
    private static final PermissionScreen BROWSER = new PermissionScreen(MODULE, "FILE_BROWSER", "مستعرض الملفات", "File Browser");

    @Override
    public List<PermissionModule> modules() {
        return List.of(new PermissionModule(MODULE, "خدمة الملفات", "File Service"));
    }

    @Override
    public List<PermissionScreen> screens() {
        return List.of(CATEGORIES, BROWSER);
    }

    @Override
    public List<PermissionDef> permissions() {
        return List.of(
            PermissionDef.of(CATEGORIES, "VIEW", "عرض"),
            PermissionDef.of(CATEGORIES, "CREATE", "إنشاء"),
            PermissionDef.of(CATEGORIES, "UPDATE", "تعديل"),
            PermissionDef.of(CATEGORIES, "DELETE", "حذف"),
            PermissionDef.of(BROWSER, "VIEW", "عرض"),
            PermissionDef.of(BROWSER, "CREATE", "إنشاء"),
            PermissionDef.of(BROWSER, "UPDATE", "تعديل"),
            PermissionDef.of(BROWSER, "DELETE", "حذف"));
    }
}
