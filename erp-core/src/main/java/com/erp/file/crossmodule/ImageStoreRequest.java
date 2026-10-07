package com.erp.file.crossmodule;

import java.util.Set;

/**
 * One image for {@link FileImageStoreApi#storePublicImage}: the owner (polymorphic, RULE-FILE-005), the
 * raw content, a display file name, the size limit and the content types the caller accepts (any of the
 * {@code FileImageStoreApi.TYPE_*} constants; SVG only when listed). The type is detected from
 * {@code content}; no declared content type is taken.
 */
public record ImageStoreRequest(
    String ownerType,
    Long ownerId,
    String moduleCode,
    byte[] content,
    String fileName,
    long maxBytes,
    Set<String> allowedTypes) {
}
