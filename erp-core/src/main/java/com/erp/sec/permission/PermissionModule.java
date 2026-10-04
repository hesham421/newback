package com.erp.sec.permission;

/**
 * A registry module ({@code SEC_MODULE_REG}) declared by a {@link PermissionContributor}, so the
 * synchronizer can give a new module row its bilingual name (erp-core step 06). A module referenced
 * only by a {@link PermissionDef} is created with its code as name.
 *
 * @param code   module code (≤ 10 chars, upper case)
 * @param nameAr Arabic name
 * @param nameEn English name
 */
public record PermissionModule(String code, String nameAr, String nameEn) {

    public PermissionModule {
        code = PermissionDef.normalize(code);
    }
}
