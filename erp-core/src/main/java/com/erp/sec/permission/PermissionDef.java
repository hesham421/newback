package com.erp.sec.permission;

/**
 * One permission (one {@code SEC_ACTION_REG} row) contributed by a module through a
 * {@link PermissionContributor} (erp-core step 06).
 *
 * <p><b>Authority format.</b> The authority a caller's token carries — and every
 * {@code @PreAuthorize("hasAuthority(...)")} compares against — is the registry's
 * {@code PERMISSION_CODE}, exactly what {@code MenuService.effectiveAuthorityCodes()} has always
 * produced. That format is {@code PERM_<SCREEN>_<ACTION>} (RegistryService's derivation, DBF-SEC-050),
 * not {@code module:screen:action}, so the step file's rule "adopt the existing format" applies:
 * {@link #authority()} defaults to {@link #defaultAuthority(String, String)}. A few recorded legacy
 * codes deviate from it (CU's {@code CONFIG_*}, the platform's {@code PLATFORM_TENANT_MANAGE}); they
 * pass their literal code as {@code authority}.
 *
 * @param moduleCode  registry module ({@code SEC_MODULE_REG.CODE}, ≤ 10 chars)
 * @param screenCode  registry screen ({@code SEC_SCREEN_REG.PAGE_CODE})
 * @param actionCode  action on that screen ({@code VIEW}, {@code CREATE}, ...; {@code VIEW} is the
 *                    screen's gateway, RULE-SEC-007)
 * @param nameAr      Arabic display name of the action
 * @param nameEn      English display name of the action
 * @param authority   the permission code; {@code null}/blank → {@code PERM_<screenCode>_<actionCode>}
 */
public record PermissionDef(String moduleCode, String screenCode, String actionCode,
                            String nameAr, String nameEn, String authority) {

    public PermissionDef {
        moduleCode = normalize(moduleCode);
        screenCode = normalize(screenCode);
        actionCode = normalize(actionCode);
        nameAr = nameAr == null || nameAr.isBlank() ? screenCode + " - " + actionCode : nameAr;
        nameEn = nameEn == null || nameEn.isBlank() ? screenCode + " - " + actionCode : nameEn;
        authority = authority == null || authority.isBlank()
            ? defaultAuthority(screenCode, actionCode) : authority.trim().toUpperCase();
    }

    /** The step file's five-component shape; the authority is derived. */
    public PermissionDef(String moduleCode, String screenCode, String actionCode, String nameAr, String nameEn) {
        this(moduleCode, screenCode, actionCode, nameAr, nameEn, null);
    }

    /**
     * An action of {@code screen} named like every core seed names its actions:
     * {@code "<screen name AR> - <action AR>"} / {@code "<screen name EN> - <ACTION_CODE>"}.
     */
    public static PermissionDef of(PermissionScreen screen, String actionCode, String actionNameAr) {
        return of(screen, actionCode, actionNameAr, null);
    }

    /** As {@link #of(PermissionScreen, String, String)}, with an explicit (legacy) permission code. */
    public static PermissionDef of(PermissionScreen screen, String actionCode, String actionNameAr, String authority) {
        return new PermissionDef(screen.moduleCode(), screen.screenCode(), actionCode,
            screen.nameAr() + " - " + actionNameAr, screen.nameEn() + " - " + actionCode, authority);
    }

    /** {@code PERM_<SCREEN>_<ACTION>} — the registry's derived permission-code format. */
    public static String defaultAuthority(String screenCode, String actionCode) {
        return "PERM_" + screenCode + "_" + actionCode;
    }

    /** Whether every code is present (the synchronizer skips, and logs, an incomplete definition). */
    public boolean isComplete() {
        return moduleCode != null && screenCode != null && actionCode != null;
    }

    /** Trimmed upper case; {@code null} for a missing code. */
    static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
