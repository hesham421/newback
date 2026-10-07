package com.erp.file.crossmodule;

import com.erp.file.domain.FileDocumentDomain;
import com.erp.file.repository.FileDocumentRepository;
import com.erp.file.repository.FileMetadataView;
import com.erp.file.service.PublicFileUrls;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FileDocumentLookupApi} over the FILE repository. No {@code @PreAuthorize}: an existence
 * check reveals no file content or metadata, a public URL is public by definition, and the consuming
 * service carries its own gate. Reads are tenant-filtered (current tenant only).
 */
@Component
@RequiredArgsConstructor
public class FileDocumentLookupApiImpl implements FileDocumentLookupApi {

    private final FileDocumentRepository repository;
    private final PublicFileUrls publicFileUrls;

    @Override
    @Transactional(readOnly = true)
    public boolean isAvailable(Long fileId) {
        return fileId != null
            && repository.existsByIdAndFileStatusIdNot(fileId, FileDocumentDomain.STATUS_DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> publicUrl(Long documentId) {
        if (documentId == null) {
            return Optional.empty();
        }
        return repository.findMetadataTupleById(documentId)
            .map(FileMetadataView::from)
            .flatMap(publicFileUrls::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> publicUrls(Collection<Long> documentIds) {
        List<Long> ids = documentIds == null ? List.of()
            : documentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> urls = new HashMap<>();
        repository.findMetadataTuplesByIdIn(ids).stream()
            .map(FileMetadataView::from)
            .forEach(view -> publicFileUrls.of(view).ifPresent(url -> urls.put(view.getId(), url)));
        return urls;
    }
}
