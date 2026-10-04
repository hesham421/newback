package com.erp.file.storage;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.entity.FileDocument;
import com.erp.file.exception.FileErrorCodes;
import com.erp.file.repository.FileDocumentRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * The default {@code DB} provider: the content stays in {@code FILE_DOCUMENT.FILE_CONTENT} (BYTEA) of
 * the document's own row, and {@code storageRef} is that row's id. {@link #put} must run inside the
 * transaction that persisted the document (the row is looked up in the same persistence context and
 * the bytes are written with it at flush). Reads go through the tenant-filtered repository, so a
 * reference of another tenant's row finds nothing. This class and the entity are the only places that
 * know the {@code FILE_CONTENT} column (erp-core step 07 acceptance).
 */
@RequiredArgsConstructor
public class DbStorageProvider implements StorageProvider {

    private final FileDocumentRepository repository;

    @Override
    public String key() {
        return StorageKeys.DB;
    }

    @Override
    public StoredObject put(StorageTarget target, InputStream in, long size, String contentType) {
        FileDocument document = repository.findById(target.documentId())
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, target.documentId()));
        byte[] content = readAll(in);
        document.setFileContent(content);
        return new StoredObject(String.valueOf(target.documentId()), content.length);
    }

    @Override
    public InputStream get(String storageRef) {
        Long id = parse(storageRef);
        byte[] content = repository.findContentTupleById(id)
            .map(tuple -> tuple.get(FileDocumentRepository.CONTENT_ALIAS, byte[].class))
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, FileErrorCodes.FILE_DOCUMENT_NOT_FOUND, id));
        if (content == null) {
            throw new LocalizedException(Status.INTERNAL_ERROR, FileErrorCodes.FILE_STORAGE_UNAVAILABLE, key());
        }
        return new ByteArrayInputStream(content);
    }

    /** Clears the column (effective inside a transaction); deleting the row deletes the content anyway. */
    @Override
    public void delete(String storageRef) {
        repository.findById(parse(storageRef)).ifPresent(document -> document.setFileContent(null));
    }

    @Override
    public Optional<String> publicUrl(String storageRef) {
        return Optional.empty();
    }

    private Long parse(String storageRef) {
        try {
            return Long.valueOf(storageRef);
        } catch (NumberFormatException e) {
            throw new LocalizedException(Status.INTERNAL_ERROR, FileErrorCodes.FILE_STORAGE_UNAVAILABLE, key());
        }
    }

    private byte[] readAll(InputStream in) {
        try (InputStream stream = in) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new LocalizedException(Status.INTERNAL_ERROR, FileErrorCodes.FILE_STORAGE_UNAVAILABLE, key());
        }
    }
}
