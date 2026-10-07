package com.erp.file.crossmodule;

/** Why {@link FileImageStoreApi#storePublicImage} refused an image (RULE-FILE-008/009). */
public enum ImageRejection {
    /** No content. */
    EMPTY,
    /** More bytes than the request's {@code maxBytes}. */
    TOO_LARGE,
    /** Not a PNG, JPEG, WebP or SVG by its content, or a type the request does not allow. */
    TYPE_NOT_ALLOWED,
    /** An allowed SVG carrying active or external content. */
    UNSAFE_SVG
}
