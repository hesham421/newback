package com.erp.file.storage;

/** The provider keys erp-core ships ({@code CHK_FILE_DOCUMENT_STORAGE_PROVIDER}). */
public final class StorageKeys {

    private StorageKeys() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static final String DB = "DB";
    public static final String LOCAL = "LOCAL";
    public static final String S3 = "S3";
}
