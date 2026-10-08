package com.erp.tenant.export;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.PlainJson;
import com.erp.tenant.TenantExport;
import com.erp.tenant.domain.TenantDomain;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * tenant-maturity C5 (srs-tenant.md X6) — the ZIP of one tenant export: each contributor's CSV files are written record by
 * record as its rows arrive (UTF-8 with a byte-order mark, RFC 4180, formula guard), then {@code manifest.json}. Counts
 * every record against the row limit (RULE-TENANT-027). Not thread-safe: one export, one thread.
 */
public final class TenantExportArchive implements TenantExport, AutoCloseable {

    static final Pattern MODULE_CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,31}$");
    static final Pattern FILE_NAME = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");
    public static final String MANIFEST = "manifest.json";
    public static final String FORMAT = "erp-tenant-export";
    public static final int FORMAT_VERSION = 1;

    private static final char BOM = '﻿';
    private static final String CRLF = "\r\n";
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private final Long tenantId;
    private final String tenantCode;
    private final long maxRows;
    private final Instant exportedAt;
    private final ZipOutputStream zip;
    private final Writer writer;
    private final Set<String> modules = new HashSet<>();
    private final Set<String> paths = new HashSet<>();
    private final List<Map<String, Object>> files = new ArrayList<>();
    private String module;
    private long rowCount;

    public TenantExportArchive(OutputStream out, Long tenantId, String tenantCode, long maxRows, Instant exportedAt) {
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.maxRows = maxRows;
        this.exportedAt = exportedAt;
        this.zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
        this.writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
    }

    @Override
    public Long tenantId() {
        return tenantId;
    }

    @Override
    public String tenantCode() {
        return tenantCode;
    }

    /** The module whose files follow; its code is the folder and must be well-formed and unique. */
    public void startModule(String moduleCode) {
        if (moduleCode == null || !MODULE_CODE.matcher(moduleCode).matches() || !modules.add(moduleCode)) {
            throw internal("Malformed or duplicate export module code: " + moduleCode);
        }
        module = moduleCode;
    }

    @Override
    public void csv(String fileName, List<String> columns, Consumer<Rows> rows) {
        if (module == null || fileName == null || !FILE_NAME.matcher(fileName).matches() || columns == null
            || columns.isEmpty()) {
            throw internal("Malformed export file " + module + "/" + fileName);
        }
        String path = module + "/" + fileName + ".csv";
        if (!paths.add(path)) {
            throw internal("Duplicate export file " + path);
        }
        FileRows sink = new FileRows(columns.size());
        try {
            openEntry(path);
            writer.write(BOM);
            writeRecord(new ArrayList<>(columns));
            rows.accept(sink);
            writer.flush();
            zip.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("module", module);
        file.put("path", path);
        file.put("rows", sink.count);
        files.add(file);
    }

    /** Writes {@code manifest.json} and completes the ZIP (the underlying stream stays open). */
    public void finish(String exportedBy, String erpCoreVersion) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("format", FORMAT);
        manifest.put("formatVersion", FORMAT_VERSION);
        manifest.put("tenantId", tenantId);
        manifest.put("tenantCode", tenantCode);
        manifest.put("exportedAt", DateTimeFormatter.ISO_INSTANT.format(exportedAt));
        manifest.put("exportedBy", exportedBy);
        manifest.put("erpCoreVersion", erpCoreVersion);
        manifest.put("rowCount", rowCount);
        manifest.put("files", files);
        Map<String, Object> csv = new LinkedHashMap<>();
        csv.put("encoding", "UTF-8 with byte-order mark");
        csv.put("separator", ",");
        csv.put("recordSeparator", "CRLF");
        csv.put("quoting", "RFC 4180");
        csv.put("nullValue", "empty field");
        csv.put("emptyText", "\"\"");
        csv.put("formulaGuard", "a text value starting with = + - @ TAB or CR is prefixed with one apostrophe");
        manifest.put("csv", csv);
        try {
            openEntry(MANIFEST);
            zip.write(PlainJson.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
            zip.closeEntry();
            zip.finish();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Records written so far, all files together. */
    public long rowCount() {
        return rowCount;
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

    private void openEntry(String path) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        entry.setTime(exportedAt.toEpochMilli());
        zip.putNextEntry(entry);
    }

    private void writeRecord(List<Object> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(field(values.get(i)));
        }
        writer.write(CRLF);
    }

    /** One CSV field (srs-tenant.md X6): NULL empty, empty text {@code ""}, text guarded and quoted per RFC 4180. */
    static String field(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof CharSequence text) {
            return text.isEmpty() ? "\"\"" : quote(guard(text.toString()));
        }
        return quote(switch (value) {
            case BigDecimal decimal -> decimal.toPlainString();
            case Instant instant -> DateTimeFormatter.ISO_INSTANT.format(instant);
            case OffsetDateTime dateTime -> DateTimeFormatter.ISO_INSTANT.format(dateTime.toInstant());
            case LocalDateTime dateTime -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
            case LocalDate date -> DateTimeFormatter.ISO_LOCAL_DATE.format(date);
            default -> value.toString();
        });
    }

    private static String guard(String text) {
        return FORMULA_PREFIXES.indexOf(text.charAt(0)) >= 0 ? "'" + text : text;
    }

    private static String quote(String field) {
        boolean needsQuotes = field.indexOf(',') >= 0 || field.indexOf('"') >= 0 || field.indexOf('\n') >= 0
            || field.indexOf('\r') >= 0 || (!field.isEmpty() && (Character.isWhitespace(field.charAt(0))
            || Character.isWhitespace(field.charAt(field.length() - 1))));
        return needsQuotes ? '"' + field.replace("\"", "\"\"") + '"' : field;
    }

    /** A column's value by its SQL type: zoned timestamps as instants, binary content refused (never exported). */
    private static Object columnValue(ResultSet resultSet, ResultSetMetaData meta, int column) throws SQLException {
        String type = meta.getColumnTypeName(column).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "timestamptz" -> resultSet.getObject(column, OffsetDateTime.class);
            case "timestamp" -> resultSet.getObject(column, LocalDateTime.class);
            case "json", "jsonb" -> resultSet.getString(column);
            case "bytea" -> throw internal("Binary column " + meta.getColumnLabel(column) + " is never exported");
            default -> resultSet.getObject(column);
        };
    }

    private static LocalizedException internal(String reason) {
        LocalizedException failure = new LocalizedException(Status.INTERNAL_ERROR, CommonErrorCodes.INTERNAL_ERROR);
        failure.initCause(new IllegalStateException(reason));
        return failure;
    }

    /** The records of one file; every record counts against the export's row limit. */
    private final class FileRows implements Rows {

        private final int width;
        private long count;

        private FileRows(int width) {
            this.width = width;
        }

        @Override
        public void addRow(ResultSet resultSet) throws SQLException {
            ResultSetMetaData meta = resultSet.getMetaData();
            if (meta.getColumnCount() != width) {
                throw internal("Export query selects " + meta.getColumnCount() + " columns for " + width + " headers");
            }
            List<Object> values = new ArrayList<>(width);
            for (int column = 1; column <= width; column++) {
                values.add(columnValue(resultSet, meta, column));
            }
            write(values);
        }

        @Override
        public void add(Object... values) {
            if (values == null || values.length != width) {
                throw internal("Export record has " + (values == null ? 0 : values.length) + " values for " + width
                    + " headers");
            }
            write(Arrays.asList(values));
        }

        private void write(List<Object> values) {
            count++;
            rowCount++;
            TenantDomain.assertExportWithinLimit(rowCount, maxRows);
            try {
                writeRecord(values);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
