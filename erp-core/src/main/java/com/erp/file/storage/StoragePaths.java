package com.erp.file.storage;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

/**
 * The object layout shared by the LOCAL and S3 providers:
 * {@code <tenantId>/<category>/<yyyy>/<MM>/<documentId>_<filename>}. Every segment is sanitised to
 * {@code [A-Za-z0-9._-]} (anything else becomes {@code _}, leading dots are removed), so neither a
 * category code nor a client-supplied file name can introduce a path separator or {@code ..}.
 */
final class StoragePaths {

    private static final int MAX_FILENAME_LENGTH = 150;

    private StoragePaths() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    static String relativeKey(StorageTarget target, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        String category = target.category() != null ? target.category() : StorageTarget.NO_CATEGORY;
        return target.tenantId() + "/" + sanitize(category.toLowerCase(Locale.ROOT)) + "/"
            + String.format(Locale.ROOT, "%04d/%02d/", today.getYear(), today.getMonthValue())
            + target.documentId() + "_" + sanitize(target.filename());
    }

    static String sanitize(String segment) {
        String value = segment == null || segment.isBlank() ? "file" : segment;
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("^\\.+", "");
        if (cleaned.isEmpty()) {
            cleaned = "file";
        }
        return cleaned.length() > MAX_FILENAME_LENGTH ? cleaned.substring(cleaned.length() - MAX_FILENAME_LENGTH) : cleaned;
    }
}
