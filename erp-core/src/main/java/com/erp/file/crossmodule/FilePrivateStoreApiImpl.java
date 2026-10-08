package com.erp.file.crossmodule;

import com.erp.file.service.FilePrivateStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** {@link FilePrivateStoreApi} delegating to FILE's {@link FilePrivateStoreService}. */
@Component
@RequiredArgsConstructor
public class FilePrivateStoreApiImpl implements FilePrivateStoreApi {

    private final FilePrivateStoreService service;

    @Override
    public StoredPrivateFile storePrivateFile(PrivateFileStoreRequest request) {
        return service.store(request).getData();
    }

    @Override
    public DownloadGrant issueDownloadToken(Long documentId) {
        return service.issueDownloadToken(documentId).getData();
    }
}
