package com.erp.cu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erp.autoconfigure.ErpCoreCacheAutoConfiguration;
import com.erp.cu.crossmodule.SettingsApi;
import com.erp.cu.crossmodule.SettingsApiImpl;
import com.erp.cu.domain.SettingScope;
import com.erp.cu.dto.ConfigurationCreateRequest;
import com.erp.cu.dto.ConfigurationUpdateRequest;
import com.erp.cu.entity.AppConfiguration;
import com.erp.cu.mapper.ConfigurationMapper;
import com.erp.cu.repository.AppConfigurationRepository;
import com.erp.cu.service.ConfigurationService;
import com.erp.tenant.TenantContext;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;

/**
 * erp-core step 09 — the {@code erpCoreSettings} cache, wired by the real auto-configurations
 * ({@link CacheAutoConfiguration} + {@link ErpCoreCacheAutoConfiguration}, {@code spring.cache.type=simple})
 * around the real {@link ConfigurationService} / {@link SettingsApiImpl} and a <b>mock repository</b>:
 * a repeated read hits the repository once, the key separates tenants, an absent key is cached too, and
 * every configuration write (create, update, deactivate) evicts.
 */
class SettingsCacheTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class, ErpCoreCacheAutoConfiguration.class))
        .withPropertyValues("spring.cache.type=simple")
        .withBean(AppConfigurationRepository.class, () -> mock(AppConfigurationRepository.class))
        .withBean(ConfigurationMapper.class)
        .withBean(ConfigurationService.class)
        .withBean(SettingsApiImpl.class);

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void aRepeatedRead_hitsTheRepositoryOnce_andIsCachedUnderTenantColonKey() {
        run(context -> {
            AppConfigurationRepository repository = context.getBean(AppConfigurationRepository.class);
            when(repository.findOverrideAndDefault(1L, "MAIL_HOST")).thenReturn(List.of(row(null, "MAIL_HOST", "smtp.default")));
            SettingsApi settings = context.getBean(SettingsApi.class);

            TenantContext.set(1L);
            assertThat(settings.get("mail_host")).isEqualTo("smtp.default");
            assertThat(settings.get("MAIL_HOST")).isEqualTo("smtp.default");
            assertThat(settings.find(" Mail_Host ")).contains("smtp.default");

            verify(repository, times(1)).findOverrideAndDefault(1L, "MAIL_HOST");
            assertThat(context.getBean(CacheManager.class).getCache(SettingsApi.CACHE_NAME).get("1:MAIL_HOST").get())
                .isEqualTo("smtp.default");
        });
    }

    @Test
    void theCacheKeySeparatesTenants() {
        run(context -> {
            AppConfigurationRepository repository = context.getBean(AppConfigurationRepository.class);
            when(repository.findOverrideAndDefault(1L, "K")).thenReturn(List.of(row(null, "K", "default")));
            when(repository.findOverrideAndDefault(7L, "K")).thenReturn(List.of(row(null, "K", "default"), row(7L, "K", "seven")));
            SettingsApi settings = context.getBean(SettingsApi.class);

            assertThat(TenantContext.callAs(1L, () -> settings.get("K"))).isEqualTo("default");
            assertThat(TenantContext.callAs(7L, () -> settings.get("K"))).isEqualTo("seven");
            assertThat(TenantContext.callAs(7L, () -> settings.get("K"))).isEqualTo("seven");
            assertThat(TenantContext.callAs(1L, () -> settings.get("K"))).isEqualTo("default");

            verify(repository, times(1)).findOverrideAndDefault(1L, "K");
            verify(repository, times(1)).findOverrideAndDefault(7L, "K");
        });
    }

    @Test
    void anAbsentKey_isCachedToo_andGetOrDefaultCoversIt() {
        run(context -> {
            AppConfigurationRepository repository = context.getBean(AppConfigurationRepository.class);
            when(repository.findOverrideAndDefault(anyLong(), anyString())).thenReturn(List.of());
            SettingsApi settings = context.getBean(SettingsApi.class);

            TenantContext.set(1L);
            assertThat(settings.find("NOPE")).isEmpty();
            assertThat(settings.getOrDefault("NOPE", Integer.class, 5)).isEqualTo(5);
            verify(repository, times(1)).findOverrideAndDefault(1L, "NOPE");
        });
    }

    @Test
    void create_update_andDeactivate_evictTheCache() {
        run(context -> {
            AppConfigurationRepository repository = context.getBean(AppConfigurationRepository.class);
            AppConfiguration stored = row(1L, "K", "v1");
            when(repository.findOverrideAndDefault(1L, "K")).thenReturn(List.of(stored));
            when(repository.existsByTenantIdAndConfigKey(1L, "NEW_KEY")).thenReturn(false);
            when(repository.findByTenantIdAndConfigKey(1L, "K")).thenReturn(Optional.of(stored));
            when(repository.save(any(AppConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(repository.saveAndFlush(any(AppConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));
            SettingsApi settings = context.getBean(SettingsApi.class);
            ConfigurationService service = context.getBean(ConfigurationService.class);
            TenantContext.set(1L);

            assertThat(settings.get("K")).isEqualTo("v1");
            assertThat(settings.get("K")).isEqualTo("v1");
            verify(repository, times(1)).findOverrideAndDefault(1L, "K");

            // update: evicted, the next read goes to the repository again and sees the new value
            service.update(SettingScope.TENANT, "k", ConfigurationUpdateRequest.builder().configValue("v2").build());
            assertThat(settings.get("K")).isEqualTo("v2");
            verify(repository, times(2)).findOverrideAndDefault(1L, "K");

            // create (another key): evicts everything
            service.create(SettingScope.TENANT, ConfigurationCreateRequest.builder().configKey("new_key").configValue("x").build());
            assertThat(settings.get("K")).isEqualTo("v2");
            verify(repository, times(3)).findOverrideAndDefault(1L, "K");

            // deactivate: evicted, and the deactivated override no longer resolves
            clearInvocations(repository);
            service.deactivate(SettingScope.TENANT, "K");
            assertThat(settings.find("K")).isEmpty();
            verify(repository, times(1)).findOverrideAndDefault(1L, "K");
        });
    }

    private void run(Consumer<AssertableApplicationContext> test) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            test.accept(context);
        });
    }

    private static AppConfiguration row(Long tenantId, String key, String value) {
        return AppConfiguration.builder().tenantId(tenantId).configKey(key).configValue(value).isActive(true).build();
    }
}
