package com.erp.cu.crossmodule;

import com.erp.cu.domain.SettingValueConverter;
import com.erp.cu.service.ConfigurationService;
import com.erp.tenant.TenantContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link SettingsApi} over {@link ConfigurationService#resolve}, which owns the read-only transaction and
 * the {@value SettingsApi#CACHE_NAME} cache (keyed by the tenant passed in here, so tenants never share
 * an entry). Conversion happens after the cache: the cache holds the raw text.
 */
@Component
@RequiredArgsConstructor
public class SettingsApiImpl implements SettingsApi {

    private final ConfigurationService configurationService;

    @Override
    public String get(String key) {
        return get(key, String.class);
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        String normalized = ConfigurationService.normalize(key);
        return find(normalized, type).orElseThrow(() -> new NoSuchSettingException(normalized));
    }

    @Override
    public Optional<String> find(String key) {
        return find(key, String.class);
    }

    @Override
    public <T> Optional<T> find(String key, Class<T> type) {
        String normalized = ConfigurationService.normalize(key);
        SettingValueConverter.assertSupported(normalized, type);
        String raw = configurationService.resolve(TenantContext.require(), normalized);
        return raw == null ? Optional.empty() : Optional.of(SettingValueConverter.convert(normalized, raw, type));
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return getOrDefault(key, String.class, defaultValue);
    }

    @Override
    public <T> T getOrDefault(String key, Class<T> type, T defaultValue) {
        return find(key, type).orElse(defaultValue);
    }
}
