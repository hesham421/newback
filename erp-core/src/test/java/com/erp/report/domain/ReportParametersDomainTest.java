package com.erp.report.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.report.ParamType;
import com.erp.report.ReportParam;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** erp-core step 11 — parameter validation and conversion, and the run/export rules. */
class ReportParametersDomainTest {

    private static final Function<String, Set<String>> LOOKUPS =
        key -> "NOTIF_CHANNEL".equals(key) ? Set.of("EMAIL", "SMS") : Set.of();

    private final ReportParametersDomain domain = ReportParametersDomain.from(List.of(
        ReportParam.of("name", ParamType.STRING, true, "الاسم", "Name"),
        ReportParam.of("count", ParamType.INTEGER, false, "العدد", "Count"),
        ReportParam.of("amount", ParamType.DECIMAL, false, "المبلغ", "Amount"),
        ReportParam.of("day", ParamType.DATE, false, "اليوم", "Day"),
        ReportParam.of("at", ParamType.DATETIME, false, "الوقت", "At"),
        ReportParam.of("flag", ParamType.BOOLEAN, false, "علم", "Flag"),
        ReportParam.lookup("channel", "NOTIF_CHANNEL", false, "القناة", "Channel"),
        ReportParam.lookup("broken", "NO_SUCH_TYPE", false, "معطل", "Broken")));

    @Test
    void validValues_areConvertedToTheirTypes_andAbsentOptionalsAreLeftOut() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("name", "  Alice ");
        raw.put("count", "42");
        raw.put("amount", 12.5);
        raw.put("day", "2026-10-04");
        raw.put("at", "2026-10-04T10:15:30+03:00");
        raw.put("flag", "TRUE");
        raw.put("channel", "EMAIL");
        raw.put("broken", "");

        Map<String, Object> converted = domain.validate(raw, LOOKUPS);

        assertThat(converted).containsEntry("name", "Alice").containsEntry("count", 42L)
            .containsEntry("amount", new BigDecimal("12.5")).containsEntry("day", LocalDate.of(2026, 10, 4))
            .containsEntry("at", Instant.parse("2026-10-04T07:15:30Z")).containsEntry("flag", Boolean.TRUE)
            .containsEntry("channel", "EMAIL").doesNotContainKey("broken");
        assertThat(domain.validate(Map.of("name", "x", "at", "2026-10-04T10:15:30"), LOOKUPS))
            .containsEntry("at", Instant.parse("2026-10-04T10:15:30Z"));
        assertThat(domain.validate(Map.of("name", "x", "count", 7), LOOKUPS)).containsEntry("count", 7L);
    }

    @Test
    void missingRequired_wrongTypes_inactiveLookups_unknownParams_areAllReported() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("name", "   ");
        raw.put("count", "4.5");
        raw.put("amount", "abc");
        raw.put("day", "04/10/2026");
        raw.put("at", "yesterday");
        raw.put("flag", "yes");
        raw.put("channel", "FAX");
        raw.put("broken", "X");
        raw.put("extra", 1);

        assertThatThrownBy(() -> domain.validate(raw, LOOKUPS))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                assertThat(e.getErrorCode()).isEqualTo("REPORT_PARAM_INVALID");
                assertThat(e.getErrors()).extracting(ErrorDetail::field).containsExactly(
                    "name", "count", "amount", "day", "at", "flag", "channel", "broken", "extra");
                assertThat(e.getErrors()).extracting(ErrorDetail::errorCode).containsOnly("REPORT_PARAM_INVALID");
            });
    }

    @Test
    void nullInput_failsOnlyForRequiredParameters() {
        assertThatThrownBy(() -> domain.validate(null, LOOKUPS)).isInstanceOf(LocalizedException.class);
        assertThat(ReportParametersDomain.from(List.of()).validate(null, LOOKUPS)).isEmpty();
    }

    @Test
    void runRules_accessExportCapAndFormat() {
        ReportRunDomain.assertCanRun(true);
        assertThatThrownBy(() -> ReportRunDomain.assertCanRun(false))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.FORBIDDEN);
                assertThat(e.getErrorCode()).isEqualTo("ACCESS_DENIED");
            });

        ReportRunDomain.assertWithinExportCap(5, null, 5);
        ReportRunDomain.assertWithinExportCap(5, 5L, 5);
        assertThatThrownBy(() -> ReportRunDomain.assertWithinExportCap(6, null, 5))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.BUSINESS_RULE_VIOLATION);
                assertThat(e.getErrorCode()).isEqualTo("REPORT_EXPORT_TOO_LARGE");
            });
        assertThatThrownBy(() -> ReportRunDomain.assertWithinExportCap(3, 600L, 5)).isInstanceOf(LocalizedException.class);

        assertThat(ReportRunDomain.exportFormat(null)).isEqualTo(ReportRunDomain.ExportFormat.CSV);
        assertThat(ReportRunDomain.exportFormat("Json")).isEqualTo(ReportRunDomain.ExportFormat.JSON);
        assertThatThrownBy(() -> ReportRunDomain.exportFormat("pdf"))
            .isInstanceOfSatisfying(LocalizedException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("REPORT_PARAM_INVALID"));
    }
}
