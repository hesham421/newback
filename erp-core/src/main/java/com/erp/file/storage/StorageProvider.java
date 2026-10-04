package com.erp.file.storage;

import java.io.InputStream;
import java.util.Optional;

/**
 * Storage SPI of the FILE module (erp-core step 07): where a document's content lives. erp-core ships
 * {@code DB} ({@link DbStorageProvider}, the {@code FILE_DOCUMENT} row itself — the default),
 * {@code LOCAL} ({@link LocalFsStorageProvider}) and the optional {@code S3}
 * ({@link S3StorageProvider}); {@code erp.core.files.storage} selects the one new uploads go to, and
 * every document remembers its provider ({@code STORAGE_PROVIDER}) and reference ({@code STORAGE_REF}),
 * so switching the setting never strands existing files.
 *
 * <p>Infrastructure contract: implementations signal failures with a {@code LocalizedException}
 * ({@code FILE_STORAGE_UNAVAILABLE}), never a raw I/O exception.
 */
public interface StorageProvider {

    /** The provider key stored in {@code FILE_DOCUMENT.STORAGE_PROVIDER}: "DB" | "LOCAL" | "S3". */
    String key();

    /** Stores {@code size} bytes from {@code in} for {@code target}; returns the provider-specific reference. */
    StoredObject put(StorageTarget target, InputStream in, long size, String contentType);

    /** Opens the content stored under {@code storageRef}; the caller closes the stream. */
    InputStream get(String storageRef);

    /** Removes the content stored under {@code storageRef} (no-op when it is already gone). */
    void delete(String storageRef);

    /** A URL that serves the content directly (CDN, bucket website), or empty if the provider cannot. */
    Optional<String> publicUrl(String storageRef);
}
