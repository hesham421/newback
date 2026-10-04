package com.erp.file.storage;

/**
 * Where a document's content is to be stored: the owning tenant, a category segment (the category
 * code, or {@code uncategorized}), the document id and its file name. Providers derive their
 * {@code storageRef} from it (LOCAL: {@code tenantId/category/yyyy/MM/documentId_filename}).
 */
public record StorageTarget(Long tenantId, String category, Long documentId, String filename) {

    /** Category segment used when a document has no category. */
    public static final String NO_CATEGORY = "uncategorized";
}
