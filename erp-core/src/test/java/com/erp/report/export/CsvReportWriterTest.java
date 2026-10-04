package com.erp.report.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.report.ColumnType;
import com.erp.report.ReportColumn;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** erp-core step 11 — RFC 4180 output, UTF-8 BOM, bilingual headers, formula guard. */
class CsvReportWriterTest {

    private static final List<ReportColumn> COLUMNS = List.of(
        new ReportColumn("name", ColumnType.STRING, "الاسم", "Name"),
        new ReportColumn("amount", ColumnType.DECIMAL, "المبلغ", "Amount"),
        new ReportColumn("day", ColumnType.DATE, "اليوم", "Day"),
        new ReportColumn("at", ColumnType.DATETIME, "الوقت", "At"),
        new ReportColumn("ok", ColumnType.BOOLEAN, "صحيح", "OK"));

    @Test
    void startsWithTheUtf8Bom_andWritesArabicHeaders_whenArabic() {
        byte[] csv = CsvReportWriter.toBytes(COLUMNS, List.of(), true);

        assertThat(Arrays.copyOf(csv, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertThat(text).isEqualTo("الاسم,المبلغ,اليوم,الوقت,صحيح\r\n");
        assertThat(new String(CsvReportWriter.toBytes(COLUMNS, List.of(), false), StandardCharsets.UTF_8))
            .isEqualTo("﻿Name,Amount,Day,At,OK\r\n");
    }

    @Test
    void quotesCommasQuotesAndLineBreaks_andFormatsValues() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", "Smith, \"Jr\"\nline2");
        row.put("amount", new BigDecimal("1E+3"));
        row.put("day", LocalDate.of(2026, 10, 4));
        row.put("at", Instant.parse("2026-10-04T07:15:30Z"));
        row.put("ok", Boolean.TRUE);
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("name", null);

        String text = new String(CsvReportWriter.toBytes(COLUMNS, List.of(row, empty), false), StandardCharsets.UTF_8);

        assertThat(text).isEqualTo("﻿Name,Amount,Day,At,OK\r\n"
            + "\"Smith, \"\"Jr\"\"\nline2\",1000,2026-10-04,2026-10-04T07:15:30Z,true\r\n"
            + ",,,,\r\n");
    }

    @Test
    void stringCellsThatLookLikeFormulas_areNeutralized_numbersAreNot() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", "=HYPERLINK(\"x\")");
        row.put("amount", new BigDecimal("-5"));

        String text = new String(CsvReportWriter.toBytes(COLUMNS, List.of(row), false), StandardCharsets.UTF_8);

        assertThat(text).endsWith("\"'=HYPERLINK(\"\"x\"\")\",-5,,,\r\n");
        assertThat(CsvReportWriter.quote(" padded")).isEqualTo("\" padded\"");
        assertThat(CsvReportWriter.quote("plain")).isEqualTo("plain");
    }
}
