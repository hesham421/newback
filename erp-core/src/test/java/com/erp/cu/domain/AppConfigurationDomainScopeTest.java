package com.erp.cu.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.cu.entity.AppConfiguration;
import com.erp.cu.exception.CuErrorCodes;
import java.util.List;
import org.junit.jupiter.api.Test;

/** erp-core step 09 — the scope rule and the resolution order, as pure domain decisions. */
class AppConfigurationDomainScopeTest {

    @Test
    void platformScope_onlyFromThePlatformTenant() {
        AppConfigurationDomain.assertScopeAllowed(SettingScope.PLATFORM, true);
        AppConfigurationDomain.assertScopeAllowed(SettingScope.TENANT, true);
        AppConfigurationDomain.assertScopeAllowed(SettingScope.TENANT, false);

        assertThatThrownBy(() -> AppConfigurationDomain.assertScopeAllowed(SettingScope.PLATFORM, false))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(CuErrorCodes.SETTING_PLATFORM_SCOPE_FORBIDDEN);
                assertThat(e.getStatus()).isEqualTo(Status.FORBIDDEN);
            });
    }

    @Test
    void resolution_override_thenDefault_inactiveRowsCountAsAbsent() {
        AppConfiguration def = row(null, "d", true);
        AppConfiguration mine = row(5L, "mine", true);
        AppConfiguration other = row(6L, "other", true);

        assertThat(AppConfigurationDomain.resolve(List.of(def, mine), 5L)).contains("mine");
        assertThat(AppConfigurationDomain.resolve(List.of(mine, def), 5L)).contains("mine");
        assertThat(AppConfigurationDomain.resolve(List.of(def, other), 5L)).contains("d");
        assertThat(AppConfigurationDomain.resolve(List.of(def, row(5L, "off", false)), 5L)).contains("d");
        assertThat(AppConfigurationDomain.resolve(List.of(row(null, "off", false)), 5L)).isEmpty();
        assertThat(AppConfigurationDomain.resolve(List.of(), 5L)).isEmpty();
    }

    private static AppConfiguration row(Long tenantId, String value, boolean active) {
        return AppConfiguration.builder().tenantId(tenantId).configKey("K").configValue(value).isActive(active).build();
    }
}
