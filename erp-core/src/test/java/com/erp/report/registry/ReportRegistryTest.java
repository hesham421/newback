package com.erp.report.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.report.ParamType;
import com.erp.report.ReportParam;
import com.erp.report.ReportProvider;
import com.erp.report.ReportResult;
import com.erp.report.permission.ReportPermissions;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionScreen;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** erp-core step 11 — the registry's startup validation and the permission bridge. */
class ReportRegistryTest {

    @Test
    void duplicateCodes_failTheStartup_namingBothProviders() {
        ReportProvider first = provider("DUP_REPORT", "SEC", List.of());
        ReportProvider second = provider("DUP_REPORT", "NOTIF", List.of());

        assertThatThrownBy(() -> new ReportRegistry(List.of(first, second)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate report code 'DUP_REPORT'");
    }

    @Test
    void invalidDefinitions_failTheStartup() {
        assertThatThrownBy(() -> new ReportRegistry(List.of(provider("lower_case", "SEC", List.of()))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("code()");
        assertThatThrownBy(() -> new ReportRegistry(List.of(provider("R_" + "X".repeat(39), "SEC", List.of()))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("code()");
        assertThatThrownBy(() -> new ReportRegistry(List.of(provider("OK_REPORT", "TOO_LONG_MODULE", List.of()))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("moduleCode()");
        assertThatThrownBy(() -> new ReportRegistry(List.of(provider("OK_REPORT", "SEC", List.of(
                ReportParam.of("a", ParamType.STRING, false, "أ", "A"),
                ReportParam.of("a", ParamType.DATE, false, "أ", "A"))))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("duplicate parameter 'a'");
        assertThatThrownBy(() -> new ReportRegistry(List.of(provider("OK_REPORT", "SEC",
                List.of(new ReportParam("c", ParamType.LOOKUP, false, null, "ق", "C"))))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("needs a lookupKey");
    }

    @Test
    void reportsAreOrderedByModuleThenCode_andFoundByCode() {
        ReportRegistry registry = new ReportRegistry(List.of(provider("Z_REPORT", "SEC", List.of()),
            provider("B_REPORT", "NOTIF", List.of()), provider("A_REPORT", "SEC", List.of())));

        assertThat(registry.all()).extracting(ReportProvider::code).containsExactly("B_REPORT", "A_REPORT", "Z_REPORT");
        assertThat(registry.find("A_REPORT")).isPresent();
        assertThat(registry.find("NOPE")).isEmpty();
        assertThat(registry.find(null)).isEmpty();
    }

    @Test
    void permissionBridge_contributesOneScreenPerModule_itsViewGateway_andOnePermissionPerReport() {
        ReportPermissions permissions = new ReportPermissions(new ReportRegistry(List.of(
            provider("A_REPORT", "SEC", List.of()), provider("B_REPORT", "SEC", List.of()),
            provider("C_REPORT", "NOTIF", List.of()))));

        assertThat(permissions.screens()).extracting(PermissionScreen::screenCode)
            .containsExactly("NOTIF_REPORTS", "SEC_REPORTS");
        assertThat(permissions.modules()).isEmpty();
        assertThat(permissions.permissions()).extracting(PermissionDef::authority).containsExactly(
            "PERM_NOTIF_REPORTS_VIEW", "PERM_SEC_REPORTS_VIEW",
            "NOTIF:REPORT:C_REPORT", "SEC:REPORT:A_REPORT", "SEC:REPORT:B_REPORT");
        PermissionDef report = permissions.permissions().get(3);
        assertThat(report.moduleCode()).isEqualTo("SEC");
        assertThat(report.screenCode()).isEqualTo("SEC_REPORTS");
        assertThat(report.actionCode()).isEqualTo("A_REPORT");
        assertThat(report.nameEn()).isEqualTo("Title A_REPORT");
    }

    static ReportProvider provider(String code, String module, List<ReportParam> params) {
        return new ReportProvider() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public String moduleCode() {
                return module;
            }

            @Override
            public String titleAr() {
                return "عنوان " + code;
            }

            @Override
            public String titleEn() {
                return "Title " + code;
            }

            @Override
            public List<ReportParam> params() {
                return params;
            }

            @Override
            public ReportResult run(Map<String, Object> values, Pageable page) {
                return new ReportResult(List.of(), List.of(), Map.of());
            }
        };
    }
}
