package com.erp.file.storage;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.exception.FileErrorCodes;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Every configured {@link StorageProvider} by key, plus the one selected for new uploads
 * ({@code erp.core.files.storage}). Reads always use the provider recorded on the document, so a
 * document written under an earlier setting stays readable as long as its provider is configured;
 * otherwise {@code FILE_STORAGE_UNAVAILABLE}. Built by {@code FileStorageAutoConfiguration}, which
 * fails the startup when the selected provider is not available.
 */
public class StorageProviderRegistry {

    private final Map<String, StorageProvider> providers = new LinkedHashMap<>();
    private final StorageProvider active;

    public StorageProviderRegistry(Collection<StorageProvider> providers, String activeKey) {
        for (StorageProvider provider : providers) {
            this.providers.putIfAbsent(provider.key(), provider);
        }
        this.active = this.providers.get(activeKey);
        if (this.active == null) {
            throw new IllegalStateException("erp.core.files.storage=" + activeKey + " but no such storage provider "
                + "is configured (available: " + this.providers.keySet() + ")");
        }
    }

    /** The provider new uploads are written to. */
    public StorageProvider active() {
        return active;
    }

    /** The provider with this key; {@code FILE_STORAGE_UNAVAILABLE} when it is not configured. */
    public StorageProvider forKey(String key) {
        StorageProvider provider = key == null ? null : providers.get(key);
        if (provider == null) {
            throw new LocalizedException(Status.INTERNAL_ERROR, FileErrorCodes.FILE_STORAGE_UNAVAILABLE, key);
        }
        return provider;
    }

    /** The keys of every configured provider. */
    public Set<String> keys() {
        return providers.keySet();
    }
}
