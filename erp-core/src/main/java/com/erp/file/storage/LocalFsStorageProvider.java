package com.erp.file.storage;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.exception.FileErrorCodes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * The {@code LOCAL} provider: content is a file under {@code erp.core.files.local.root}, at
 * {@code <tenantId>/<category>/<yyyy>/<MM>/<documentId>_<filename>}; {@code storageRef} is that path
 * relative to the root (forward slashes). Path traversal is guarded twice: every segment is sanitised
 * when the reference is built ({@link StoragePaths}), and every reference is resolved, normalised and
 * required to stay inside the root before any file is touched — a stored reference such as
 * {@code ../../etc/passwd} is refused with {@code FILE_STORAGE_UNAVAILABLE}.
 *
 * <p>Writes go to a temporary file in the target directory that is then moved into place, so a
 * half-written upload is never visible under its final name.
 */
@Slf4j
public class LocalFsStorageProvider implements StorageProvider {

    private final Path root;
    private final Clock clock;

    public LocalFsStorageProvider(Path root) {
        this(root, Clock.systemUTC());
    }

    public LocalFsStorageProvider(Path root, Clock clock) {
        this.root = root.toAbsolutePath().normalize();
        this.clock = clock;
    }

    @Override
    public String key() {
        return StorageKeys.LOCAL;
    }

    @Override
    public StoredObject put(StorageTarget target, InputStream in, long size, String contentType) {
        String ref = StoragePaths.relativeKey(target, clock);
        Path file = resolve(ref);
        try (InputStream stream = in) {
            Files.createDirectories(file.getParent());
            Path temp = Files.createTempFile(file.getParent(), ".upload-", ".tmp");
            try {
                long written = Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return new StoredObject(ref, written);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            log.error("LOCAL storage write failed for {}", ref, e);
            throw unavailable();
        }
    }

    @Override
    public InputStream get(String storageRef) {
        Path file = resolve(storageRef);
        try {
            return Files.newInputStream(file);
        } catch (NoSuchFileException e) {
            log.error("LOCAL storage object missing: {}", storageRef);
            throw unavailable();
        } catch (IOException e) {
            log.error("LOCAL storage read failed for {}", storageRef, e);
            throw unavailable();
        }
    }

    @Override
    public void delete(String storageRef) {
        try {
            Files.deleteIfExists(resolve(storageRef));
        } catch (IOException e) {
            log.warn("LOCAL storage delete failed for {}", storageRef, e);
        }
    }

    @Override
    public Optional<String> publicUrl(String storageRef) {
        return Optional.empty();
    }

    /** The root this provider writes under (absolute, normalised). */
    public Path root() {
        return root;
    }

    /** Resolves a reference inside the root; anything that would escape it is refused. */
    Path resolve(String storageRef) {
        if (storageRef == null || storageRef.isBlank()) {
            throw unavailable();
        }
        Path candidate = root.resolve(storageRef).normalize();
        if (!candidate.startsWith(root) || candidate.equals(root)) {
            log.error("LOCAL storage reference escapes the root: {}", storageRef);
            throw unavailable();
        }
        return candidate;
    }

    private LocalizedException unavailable() {
        return new LocalizedException(Status.INTERNAL_ERROR, FileErrorCodes.FILE_STORAGE_UNAVAILABLE, key());
    }
}
