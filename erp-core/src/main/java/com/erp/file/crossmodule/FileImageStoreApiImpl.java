package com.erp.file.crossmodule;

import com.erp.file.service.FileImageStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** {@link FileImageStoreApi} delegating to FILE's {@link FileImageStoreService}. */
@Component
@RequiredArgsConstructor
public class FileImageStoreApiImpl implements FileImageStoreApi {

    private final FileImageStoreService service;

    @Override
    public ImageStoreResult storePublicImage(ImageStoreRequest request) {
        return service.store(request).getData();
    }

    @Override
    public void discard(Long documentId) {
        service.discard(documentId);
    }
}
