package com.erp.sec.permission;

/**
 * A registry screen ({@code SEC_SCREEN_REG}) declared by a {@link PermissionContributor}, with its
 * bilingual name (erp-core step 06). A screen may be declared without any action (e.g. SEC's public
 * login screen); a screen referenced only by a {@link PermissionDef} is created with its code as name.
 *
 * @param moduleCode owning registry module
 * @param screenCode page code (upper case)
 * @param nameAr     Arabic name
 * @param nameEn     English name
 */
public record PermissionScreen(String moduleCode, String screenCode, String nameAr, String nameEn) {

    public PermissionScreen {
        moduleCode = PermissionDef.normalize(moduleCode);
        screenCode = PermissionDef.normalize(screenCode);
    }
}
