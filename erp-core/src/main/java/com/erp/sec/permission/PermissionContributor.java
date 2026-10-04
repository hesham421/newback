package com.erp.sec.permission;

import java.util.List;

/**
 * The permission catalog SPI (erp-core step 06). Every module — core or application — declares its
 * own permissions by exposing one Spring bean implementing this interface; there is no central
 * constants class any more. At startup {@code PermissionCatalogSynchronizer} upserts every contributed
 * module, screen and permission into the global registry ({@code SEC_MODULE_REG} /
 * {@code SEC_SCREEN_REG} / {@code SEC_ACTION_REG}); it never deletes a row.
 *
 * <p>Convention: the contributor class of module {@code x} is {@code com.erp.x.permission.XPermissions}
 * (an application uses its own package) and also holds the {@code public static final String}
 * authority constants its own {@code @PreAuthorize} expressions reference, e.g.
 * {@code hasAuthority(T(com.erp.file.permission.FilePermissions).PERM_FILE_CATEGORIES_VIEW)}. A
 * {@code @PreAuthorize} never references another module's constants class.
 *
 * <p>A screen with non-{@code VIEW} actions should also declare its {@code VIEW} action: RULE-SEC-007
 * only lets a role's non-VIEW grants count together with the screen's VIEW (super roles hold all).
 */
public interface PermissionContributor {

    /** The permissions this module contributes. */
    List<PermissionDef> permissions();

    /** The modules this contributor names (bilingual names for new module rows). */
    default List<PermissionModule> modules() {
        return List.of();
    }

    /** The screens this contributor names (bilingual names; may include screens without actions). */
    default List<PermissionScreen> screens() {
        return List.of();
    }
}
