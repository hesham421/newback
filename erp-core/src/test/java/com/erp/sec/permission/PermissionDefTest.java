package com.erp.sec.permission;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** erp-core step 06 — the permission-definition record keeps the registry's existing authority format. */
class PermissionDefTest {

    private static final PermissionScreen SCREEN = new PermissionScreen("file", " file_browser ", "مستعرض الملفات", "File Browser");

    @Test
    void authority_defaultsToTheRegistryFormat_PERM_screen_action() {
        PermissionDef def = new PermissionDef("FILE", "FILE_BROWSER", "view", "a", "b");
        assertThat(def.authority()).isEqualTo("PERM_FILE_BROWSER_VIEW");
        assertThat(def.actionCode()).isEqualTo("VIEW");
        assertThat(def.isComplete()).isTrue();
    }

    @Test
    void of_namesTheActionLikeTheCoreSeeds_andNormalizesCodes() {
        PermissionDef def = PermissionDef.of(SCREEN, "DELETE", "حذف");
        assertThat(def.moduleCode()).isEqualTo("FILE");
        assertThat(def.screenCode()).isEqualTo("FILE_BROWSER");
        assertThat(def.nameAr()).isEqualTo("مستعرض الملفات - حذف");
        assertThat(def.nameEn()).isEqualTo("File Browser - DELETE");
        assertThat(def.authority()).isEqualTo("PERM_FILE_BROWSER_DELETE");
    }

    @Test
    void anExplicitLegacyAuthority_isKept() {
        PermissionDef def = PermissionDef.of(new PermissionScreen("CU", "CU_CONFIGURATIONS", "x", "y"), "VIEW", "عرض", "config_view");
        assertThat(def.authority()).isEqualTo("CONFIG_VIEW");
    }

    @Test
    void anIncompleteDefinition_isFlaggedNotThrown() {
        PermissionDef def = new PermissionDef("FILE", " ", "VIEW", null, null);
        assertThat(def.isComplete()).isFalse();
    }
}
