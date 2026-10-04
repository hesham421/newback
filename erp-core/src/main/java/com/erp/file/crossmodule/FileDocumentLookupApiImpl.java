package com.erp.file.crossmodule;

import com.erp.file.domain.FileDocumentDomain;
import com.erp.file.repository.FileDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FileDocumentLookupApi} over the FILE repository. No {@code @PreAuthorize}: an existence
 * check reveals no file content or metadata, and the consuming service carries its own gate.
 */
@Component
@RequiredArgsConstructor
public class FileDocumentLookupApiImpl implements FileDocumentLookupApi {

    private final FileDocumentRepository repository;

    @Override
    @Transactional(readOnly = true)
    public boolean isAvailable(Long fileId) {
        return fileId != null
            && repository.existsByIdAndFileStatusIdNot(fileId, FileDocumentDomain.STATUS_DELETED);
    }
}
