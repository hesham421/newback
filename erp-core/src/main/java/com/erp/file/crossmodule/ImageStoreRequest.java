package com.erp.file.crossmodule;

import java.util.Set;

/**
 * One image for {@link FileImageStoreApi#storePublicImage}: the owner (polymorphic, RULE-FILE-005), the
 * raw content, a base name for the stored file ({@code photo}, {@code logo}; the extension comes from the
 * detected type, never from the client), the size limit and the content types the caller accepts (any of
 * the {@code FileImageStoreApi.TYPE_*} constants; SVG only when listed).
 */
public record ImageStoreRequest(
    String ownerType,
    Long ownerId,
    String moduleCode,
    byte[] content,
    String baseName,
    long maxBytes,
    Set<String> allowedTypes) {
}
