package com.erp.file.crossmodule;

/** A privately stored file: its {@code FILE_DOCUMENT} id, stored name, size in bytes and SHA-256 content hash (hex). */
public record StoredPrivateFile(Long documentId, String fileName, long size, String contentHash) {
}
