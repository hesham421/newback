package com.erp.file.storage;

/** Result of {@link StorageProvider#put}: the provider-specific reference and the stored size in bytes. */
public record StoredObject(String storageRef, long size) {
}
