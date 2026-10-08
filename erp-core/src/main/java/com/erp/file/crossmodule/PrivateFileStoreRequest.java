package com.erp.file.crossmodule;

import java.nio.file.Path;

/**
 * One file for {@link FilePrivateStoreApi#storePrivateFile}: the owner (RULE-FILE-005), the stored name, the declared
 * content type (the producer generated it, so it is not sniffed), the readable local file holding the content and the
 * authority a caller must hold to see the document at all (RULE-FILE-012; null = an ordinary private document).
 */
public record PrivateFileStoreRequest(
    String ownerType,
    Long ownerId,
    String moduleCode,
    String fileName,
    String contentType,
    Path content,
    String requiredAuthority) {
}
