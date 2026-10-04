package com.erp.events;

import lombok.Getter;

/**
 * A file document became available (FILE): published when an upload is stored
 * ({@value #VISIBILITY_PRIVATE}). The public-files work of erp-core step 07 publishes it too when a
 * document is made public ({@value #VISIBILITY_PUBLIC}).
 */
@Getter
public final class FileDocumentPublishedEvent extends DomainEvent {

    /** Visibility of a stored, token-protected document. */
    public static final String VISIBILITY_PRIVATE = "PRIVATE";

    /** Visibility of a document served on a public URL. */
    public static final String VISIBILITY_PUBLIC = "PUBLIC";

    private final Long documentId;
    private final String moduleCode;
    private final String ownerType;
    private final Long ownerId;
    private final String fileName;
    private final String contentType;
    private final String visibility;

    public FileDocumentPublishedEvent(Long documentId, String moduleCode, String ownerType, Long ownerId,
                                      String fileName, String contentType, String visibility) {
        this.documentId = documentId;
        this.moduleCode = moduleCode;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.visibility = visibility;
    }
}
