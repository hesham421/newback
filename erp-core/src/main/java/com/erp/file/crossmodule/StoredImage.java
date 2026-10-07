package com.erp.file.crossmodule;

/** A stored, published image: its {@code FILE_DOCUMENT} id, public URL, detected content type and size. */
public record StoredImage(Long documentId, String publicUrl, String contentType, long size) {
}
