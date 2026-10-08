package com.erp.tenant.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.LocalizedException;
import com.erp.tenant.exception.TenantErrorCodes;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/** tenant-maturity C5 (srs-tenant.md X6, RULE-TENANT-027) — the archive's CSV conventions, manifest, structure and row cap. */
class TenantExportArchiveTest {

    private static final Instant AT = Instant.parse("2026-10-08T09:30:00Z");

    @Test
    void fields_followRfc4180_keepNullAndEmptyApart_andGuardFormulas() {
        assertThat(TenantExportArchive.field(null)).isEmpty();
        assertThat(TenantExportArchive.field("")).isEqualTo("\"\"");
        assertThat(TenantExportArchive.field("plain")).isEqualTo("plain");
        assertThat(TenantExportArchive.field("a,b")).isEqualTo("\"a,b\"");
        assertThat(TenantExportArchive.field("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(TenantExportArchive.field("line1\r\nline2")).isEqualTo("\"line1\r\nline2\"");
        assertThat(TenantExportArchive.field(" padded")).isEqualTo("\" padded\"");
        assertThat(TenantExportArchive.field("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        assertThat(TenantExportArchive.field("-5")).isEqualTo("'-5");
        assertThat(TenantExportArchive.field("@cmd")).isEqualTo("'@cmd");
        assertThat(TenantExportArchive.field("+1, 2")).isEqualTo("\"'+1, 2\"");
        assertThat(TenantExportArchive.field("عربي")).isEqualTo("عربي");
        assertThat(TenantExportArchive.field(-5L)).as("numbers are never guarded").isEqualTo("-5");
        assertThat(TenantExportArchive.field(new BigDecimal("1E+3"))).isEqualTo("1000");
        assertThat(TenantExportArchive.field(Boolean.TRUE)).isEqualTo("true");
        assertThat(TenantExportArchive.field(OffsetDateTime.of(2026, 10, 8, 13, 30, 0, 0, ZoneOffset.ofHours(4))))
            .isEqualTo("2026-10-08T09:30:00Z");
        assertThat(TenantExportArchive.field(LocalDateTime.of(2026, 10, 8, 9, 30))).isEqualTo("2026-10-08T09:30:00");
    }

    @Test
    void anArchive_hasOneBomPrefixedCsvPerFile_headerOnlyWhenEmpty_andAManifestCountingEveryRecord() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (TenantExportArchive archive = new TenantExportArchive(out, 7L, "ACME", 100, AT)) {
            archive.startModule("SEC");
            archive.csv("SEC_USER", List.of("USER_PK", "USERNAME"), rows -> {
                rows.add(1L, "admin");
                rows.add(2L, null);
            });
            archive.startModule("TENANT");
            archive.csv("CORE_TENANT", List.of("ID", "CODE"), rows -> { });
            archive.finish("operator", "1.3.0-TEST");
            assertThat(archive.rowCount()).isEqualTo(2);
        }

        Map<String, byte[]> entries = unzip(out.toByteArray());
        assertThat(entries.keySet()).containsExactly("SEC/SEC_USER.csv", "TENANT/CORE_TENANT.csv", "manifest.json");
        assertThat(new String(entries.get("SEC/SEC_USER.csv"), StandardCharsets.UTF_8))
            .isEqualTo("﻿USER_PK,USERNAME\r\n1,admin\r\n2,\r\n");
        assertThat(new String(entries.get("TENANT/CORE_TENANT.csv"), StandardCharsets.UTF_8)).isEqualTo("﻿ID,CODE\r\n");
        String manifest = new String(entries.get("manifest.json"), StandardCharsets.UTF_8);
        assertThat((String) JsonPath.read(manifest, "$.format")).isEqualTo("erp-tenant-export");
        assertThat((Integer) JsonPath.read(manifest, "$.tenantId")).isEqualTo(7);
        assertThat((String) JsonPath.read(manifest, "$.exportedAt")).isEqualTo("2026-10-08T09:30:00Z");
        assertThat((String) JsonPath.read(manifest, "$.erpCoreVersion")).isEqualTo("1.3.0-TEST");
        assertThat((Integer) JsonPath.read(manifest, "$.rowCount")).isEqualTo(2);
        assertThat((List<Integer>) JsonPath.read(manifest, "$.files[*].rows")).containsExactly(2, 0);
    }

    @Test
    void writingPastTheLimit_isRefusedAsTooLarge() {
        TenantExportArchive archive = new TenantExportArchive(new ByteArrayOutputStream(), 7L, "ACME", 2, AT);
        archive.startModule("SEC");

        assertThatThrownBy(() -> archive.csv("SEC_USER", List.of("USER_PK"), rows -> {
            rows.add(1L);
            rows.add(2L);
            rows.add(3L);
        }))
            .isInstanceOf(LocalizedException.class)
            .extracting(e -> ((LocalizedException) e).getErrorCode())
            .isEqualTo(TenantErrorCodes.TENANT_EXPORT_TOO_LARGE);
    }

    @Test
    void malformedOrDuplicateNamesAndWrongRecordWidths_areRefused() {
        TenantExportArchive archive = new TenantExportArchive(new ByteArrayOutputStream(), 7L, "ACME", 10, AT);
        assertThatThrownBy(() -> archive.startModule("../SEC")).isInstanceOf(LocalizedException.class);
        assertThatThrownBy(() -> archive.csv("SEC_USER", List.of("A"), rows -> { }))
            .as("no module started").isInstanceOf(LocalizedException.class);
        archive.startModule("SEC");
        assertThatThrownBy(() -> archive.startModule("SEC")).as("duplicate module").isInstanceOf(LocalizedException.class);
        assertThatThrownBy(() -> archive.csv("sec_user", List.of("A"), rows -> { })).isInstanceOf(LocalizedException.class);
        archive.csv("SEC_USER", List.of("A"), rows -> { });
        assertThatThrownBy(() -> archive.csv("SEC_USER", List.of("A"), rows -> { }))
            .as("duplicate file").isInstanceOf(LocalizedException.class);
        assertThatThrownBy(() -> archive.csv("SEC_ROLE", List.of("A", "B"), rows -> rows.add("only one")))
            .isInstanceOf(LocalizedException.class);
    }

    private static Map<String, byte[]> unzip(byte[] archive) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                assertThat(entry.getTime()).isEqualTo(AT.toEpochMilli());
                entries.put(entry.getName(), zip.readAllBytes());
            }
        }
        return entries;
    }
}
