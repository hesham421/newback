package com.erp.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * {@link AbstractFileStorageIntegrationTest} with {@code erp.core.files.storage=LOCAL} on a fresh
 * temporary root (its own Spring context). The bytes must be on disk under
 * {@code <root>/<tenantId>/<category>/<yyyy>/<MM>/<id>_<name>} and not in {@code FILE_CONTENT}.
 */
class LocalStorageFileIntegrationTest extends AbstractFileStorageIntegrationTest {

    private static final Path ROOT = createRoot();

    private static Path createRoot() {
        try {
            Path root = Files.createTempDirectory("erp-s07-local-");
            root.toFile().deleteOnExit();
            return root;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void localStorage(DynamicPropertyRegistry registry) {
        registry.add("erp.core.files.storage", () -> "LOCAL");
        registry.add("erp.core.files.local.root", ROOT::toString);
    }

    @Override
    protected String expectedProvider() {
        return "LOCAL";
    }

    @Override
    protected void assertStoredByProvider(long documentId, byte[] content) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "select storage_ref, file_content, tenant_id from file_document where id = ?", documentId);
        assertThat(row.get("file_content")).isNull();
        String ref = (String) row.get("storage_ref");
        assertThat(ref).matches(row.get("tenant_id") + "/uncategorized/\\d{4}/\\d{2}/" + documentId + "_secret\\.png");
        Path file = ROOT.resolve(ref);
        assertThat(file).exists();
        try {
            assertThat(Files.readAllBytes(file)).isEqualTo(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
