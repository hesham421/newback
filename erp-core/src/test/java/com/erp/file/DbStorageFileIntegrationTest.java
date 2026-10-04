package com.erp.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

/** {@link AbstractFileStorageIntegrationTest} with the default {@code DB} provider (no property set). */
class DbStorageFileIntegrationTest extends AbstractFileStorageIntegrationTest {

    @Override
    protected String expectedProvider() {
        return "DB";
    }

    @Override
    protected void assertStoredByProvider(long documentId, byte[] content) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "select storage_ref, file_content from file_document where id = ?", documentId);
        assertThat(row.get("storage_ref")).isEqualTo(String.valueOf(documentId));
        assertThat((byte[]) row.get("file_content")).isEqualTo(content);
    }
}
