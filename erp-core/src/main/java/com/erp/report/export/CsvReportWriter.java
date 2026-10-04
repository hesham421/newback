package com.erp.report.export;

import com.erp.report.ColumnType;
import com.erp.report.ReportColumn;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Hand-written RFC 4180 CSV writer of a report result (erp-core step 11).
 * <ul>
 *   <li>UTF-8 with a byte-order mark ({@code EF BB BF}), so Excel opens Arabic text correctly;</li>
 *   <li>header row = the columns' Arabic or English labels;</li>
 *   <li>records separated by CRLF; a field containing a comma, a double quote, CR or LF (or leading /
 *       trailing blanks) is enclosed in double quotes, embedded quotes doubled;</li>
 *   <li>values: numbers plain ({@link BigDecimal#toPlainString()}), dates/times ISO-8601,
 *       booleans {@code true}/{@code false}, {@code null} empty;</li>
 *   <li>spreadsheet formula injection: a {@link ColumnType#STRING} value starting with {@code =},
 *       {@code +}, {@code -}, {@code @}, TAB or CR is prefixed with an apostrophe, so Excel shows it as
 *       text instead of evaluating it.</li>
 * </ul>
 */
public final class CsvReportWriter {

    /** The UTF-8 byte-order mark written first. */
    public static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final String CRLF = "\r\n";

    private CsvReportWriter() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** The whole CSV document as bytes. */
    public static byte[] toBytes(List<ReportColumn> columns, List<Map<String, Object>> rows, boolean arabic) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        write(columns, rows, arabic, buffer);
        return buffer.toByteArray();
    }

    /** Writes the CSV document to {@code out} (not closed). */
    public static void write(List<ReportColumn> columns, List<Map<String, Object>> rows, boolean arabic,
                             OutputStream out) {
        try {
            out.write(UTF8_BOM);
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            writeRecord(writer, columns.stream().map(c -> arabic ? c.labelAr() : c.labelEn()).toList());
            for (Map<String, Object> row : rows) {
                writeRecord(writer, columns.stream().map(c -> format(c, row.get(c.key()))).toList());
            }
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeRecord(Writer writer, List<String> fields) throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(quote(fields.get(i)));
        }
        writer.write(CRLF);
    }

    /** RFC 4180 quoting. */
    static String quote(String field) {
        if (field == null || field.isEmpty()) {
            return "";
        }
        boolean needsQuotes = field.indexOf(',') >= 0 || field.indexOf('"') >= 0 || field.indexOf('\n') >= 0
            || field.indexOf('\r') >= 0 || Character.isWhitespace(field.charAt(0))
            || Character.isWhitespace(field.charAt(field.length() - 1));
        return needsQuotes ? '"' + field.replace("\"", "\"\"") + '"' : field;
    }

    static String format(ReportColumn column, Object value) {
        if (value == null) {
            return "";
        }
        String text = switch (value) {
            case BigDecimal decimal -> decimal.toPlainString();
            case Instant instant -> DateTimeFormatter.ISO_INSTANT.format(instant);
            case OffsetDateTime dateTime -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
            case LocalDateTime dateTime -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
            case LocalDate date -> DateTimeFormatter.ISO_LOCAL_DATE.format(date);
            default -> value.toString();
        };
        if (column.type() == ColumnType.STRING && !text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text;
    }
}
