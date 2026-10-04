package com.erp.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.LocalizedException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** erp-core step 07 — the LOCAL provider: layout, round trip, delete and the path-traversal guard. */
class LocalFsStorageProviderTest {

    private static final Clock MARCH_2026 = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path root;

    @Test
    void put_writesUnderTenantCategoryYearMonth_andGetReadsItBack() throws IOException {
        LocalFsStorageProvider provider = new LocalFsStorageProvider(root, MARCH_2026);
        byte[] content = "hello local".getBytes(StandardCharsets.UTF_8);

        StoredObject stored = provider.put(new StorageTarget(7L, "PRODUCT_IMAGE", 42L, "photo.png"),
            new ByteArrayInputStream(content), content.length, "image/png");

        assertThat(stored.storageRef()).isEqualTo("7/product_image/2026/03/42_photo.png");
        assertThat(stored.size()).isEqualTo(content.length);
        assertThat(Files.readAllBytes(root.resolve(stored.storageRef()))).isEqualTo(content);
        try (InputStream in = provider.get(stored.storageRef())) {
            assertThat(in.readAllBytes()).isEqualTo(content);
        }
        assertThat(provider.key()).isEqualTo("LOCAL");
        assertThat(provider.publicUrl(stored.storageRef())).isEmpty();
        // no temporary upload file is left behind
        try (var files = Files.list(root.resolve("7/product_image/2026/03"))) {
            assertThat(files.map(p -> p.getFileName().toString())).containsExactly("42_photo.png");
        }
    }

    @Test
    void hostileFileNameAndCategory_areSanitised_andStayInsideTheRoot() {
        LocalFsStorageProvider provider = new LocalFsStorageProvider(root, MARCH_2026);

        StoredObject stored = provider.put(new StorageTarget(1L, "../../etc", 5L, "../../../evil\\name?.txt"),
            new ByteArrayInputStream(new byte[] {1, 2, 3}), 3, "text/plain");

        assertThat(stored.storageRef().split("/")).hasSize(5).doesNotContain("..", ".").allSatisfy(segment ->
            assertThat(segment).doesNotStartWith(".").doesNotContain("\\").doesNotContain("?"));
        assertThat(stored.storageRef()).startsWith("1/").endsWith(".txt");
        assertThat(root.resolve(stored.storageRef()).normalize()).startsWith(root);
        assertThat(root.resolve(stored.storageRef())).exists();
    }

    @Test
    void get_refusesAReferenceThatEscapesTheRoot() {
        LocalFsStorageProvider provider = new LocalFsStorageProvider(root, MARCH_2026);

        for (String hostile : new String[] {"../outside.txt", "1/../../outside.txt", root.getParent().resolve("x").toString(), ""}) {
            assertThatThrownBy(() -> provider.get(hostile))
                .as(hostile)
                .isInstanceOf(LocalizedException.class)
                .extracting(e -> ((LocalizedException) e).getErrorCode())
                .isEqualTo("FILE_STORAGE_UNAVAILABLE");
        }
    }

    @Test
    void get_missingObject_isStorageUnavailable_andDeleteIsIdempotent() {
        LocalFsStorageProvider provider = new LocalFsStorageProvider(root, MARCH_2026);
        StoredObject stored = provider.put(new StorageTarget(1L, null, 9L, "a.bin"),
            new ByteArrayInputStream(new byte[] {9}), 1, "application/octet-stream");
        assertThat(stored.storageRef()).isEqualTo("1/uncategorized/2026/03/9_a.bin");

        provider.delete(stored.storageRef());
        provider.delete(stored.storageRef());

        assertThat(root.resolve(stored.storageRef())).doesNotExist();
        assertThatThrownBy(() -> provider.get(stored.storageRef())).isInstanceOf(LocalizedException.class);
    }
}
