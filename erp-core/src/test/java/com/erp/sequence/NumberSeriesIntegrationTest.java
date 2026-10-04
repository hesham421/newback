package com.erp.sequence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.LocalizedException;
import com.erp.sequence.crossmodule.NumberSeriesApi;
import com.erp.sequence.domain.ResetPolicy;
import com.erp.sequence.entity.NumberSeries;
import com.erp.sequence.exception.SequenceErrorCodes;
import com.erp.sequence.repository.NumberSeriesRepository;
import com.erp.sequence.tenant.SequenceTenantProvisioningContributor;
import com.erp.tenant.TenantContext;
import com.erp.tenant.TenantProvisioning;
import com.erp.testsupport.AbstractIntegrationTest;
import com.erp.testsupport.StaffApiClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * erp-core step 09 — {@code NumberSeriesApi} against PostgreSQL: YEARLY reset creates a new period row,
 * tenants A and B count independently from 1, unknown/inactive codes are {@code SEQUENCE_NOT_CONFIGURED},
 * preview does not consume, {@code {TENANT}} renders the tenant code, and tenant provisioning copies the
 * series definitions. Not transactional (every allocation commits on its own); codes are unique per test.
 */
class NumberSeriesIntegrationTest extends AbstractIntegrationTest {

    private static final String THIS_YEAR = String.valueOf(LocalDate.now().getYear());

    @Autowired
    private NumberSeriesApi api;
    @Autowired
    private NumberSeriesRepository repository;
    @Autowired
    private SequenceTenantProvisioningContributor provisioningContributor;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void yearlyReset_createsANewPeriodRow_startingAtOne_andLeavesThePreviousYearUntouched() {
        String code = StaffApiClient.unique("YR");
        // the series' anchor row belongs to an earlier year (as if created then and used up to 41)
        repository.save(series(code, "INV", NumberSeries.DEFAULT_PATTERN, ResetPolicy.YEARLY, "2000", 42L));

        String first = api.next(code);
        String second = api.next(code);

        assertThat(first).isEqualTo("INV-" + THIS_YEAR + "-000001");
        assertThat(second).isEqualTo("INV-" + THIS_YEAR + "-000002");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "select period_key, next_value from core_number_series where tenant_id = 1 and code = ? order by id", code);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("period_key", "2000").containsEntry("next_value", 42L);
        assertThat(rows.get(1)).containsEntry("period_key", THIS_YEAR).containsEntry("next_value", 3L);
    }

    @Test
    void tenantsAandB_eachStartAtOne_forTheSameCode() {
        String code = StaffApiClient.unique("AB");
        long tenantA = newTenant("SQA");
        long tenantB = newTenant("SQB");
        for (long tenant : List.of(tenantA, tenantB)) {
            TenantContext.runAs(tenant, () ->
                repository.save(series(code, "D", "{PREFIX}-{SEQ:3}", ResetPolicy.NEVER, "", 1L)));
        }

        String a1 = TenantContext.callAs(tenantA, () -> api.next(code));
        String a2 = TenantContext.callAs(tenantA, () -> api.next(code));
        String b1 = TenantContext.callAs(tenantB, () -> api.next(code));

        assertThat(a1).isEqualTo("D-001");
        assertThat(a2).isEqualTo("D-002");
        assertThat(b1).isEqualTo("D-001");
        // and PLATFORM has no such series at all
        assertThatThrownBy(() -> api.next(code)).isInstanceOfSatisfying(LocalizedException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(SequenceErrorCodes.SEQUENCE_NOT_CONFIGURED));
    }

    @Test
    void unknownOrInactiveCode_isSequenceNotConfigured_andNothingIsCreated() {
        String unknown = StaffApiClient.unique("NO");
        assertThatThrownBy(() -> api.next(unknown)).isInstanceOfSatisfying(LocalizedException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(SequenceErrorCodes.SEQUENCE_NOT_CONFIGURED));
        assertThatThrownBy(() -> api.preview(unknown)).isInstanceOfSatisfying(LocalizedException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(SequenceErrorCodes.SEQUENCE_NOT_CONFIGURED));
        assertThat(jdbcTemplate.queryForObject("select count(*) from core_number_series where code = ?",
            Integer.class, unknown)).isZero();

        String inactive = StaffApiClient.unique("IN");
        NumberSeries row = series(inactive, "X", "{PREFIX}{SEQ:2}", ResetPolicy.NEVER, "", 1L);
        row.deactivate();
        repository.save(row);
        assertThatThrownBy(() -> api.next(inactive)).isInstanceOfSatisfying(LocalizedException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(SequenceErrorCodes.SEQUENCE_NOT_CONFIGURED));
    }

    @Test
    void preview_showsTheNextNumber_withoutConsumingIt_andCodesAreCaseInsensitive() {
        String code = StaffApiClient.unique("PV");
        repository.save(series(code, "PV", "{PREFIX}-{SEQ:5}", ResetPolicy.NEVER, "", 7L));

        assertThat(api.preview(code.toLowerCase())).isEqualTo("PV-00007");
        assertThat(api.preview(code)).isEqualTo("PV-00007");
        assertThat(api.next(" " + code.toLowerCase() + " ")).isEqualTo("PV-00007");
        assertThat(api.preview(code)).isEqualTo("PV-00008");

        // a new period that has no row yet previews as 1, and creates nothing
        String yearly = StaffApiClient.unique("PY");
        repository.save(series(yearly, "Y", "{PREFIX}{YY}{SEQ:3}", ResetPolicy.YEARLY, "1999", 50L));
        assertThat(api.preview(yearly)).isEqualTo("Y" + THIS_YEAR.substring(2) + "001");
        assertThat(jdbcTemplate.queryForObject("select count(*) from core_number_series where code = ?",
            Integer.class, yearly)).isEqualTo(1);
    }

    @Test
    void tenantToken_rendersTheTenantCode() {
        String code = StaffApiClient.unique("TN");
        repository.save(series(code, "SO", "{TENANT}/{PREFIX}/{SEQ:2}", ResetPolicy.NEVER, "", 1L));

        assertThat(api.next(code)).isEqualTo("PLATFORM/SO/01");
    }

    @Test
    void tenantProvisioning_copiesTheSeriesDefinitions_withTheCounterBackAtOne() {
        String code = StaffApiClient.unique("PR");
        repository.save(series(code, "PR", "{PREFIX}-{YYYY}-{SEQ:4}", ResetPolicy.YEARLY, "2001", 900L));
        api.next(code);                                           // PLATFORM: adds the current-year row
        long tenant = newTenant("SQP");

        provisioningContributor.provision(new TenantProvisioning(tenant, "SQP", 1L, "test", null));

        List<Map<String, Object>> copied = jdbcTemplate.queryForList(
            "select period_key, next_value, pattern, prefix, reset_policy, is_active from core_number_series"
                + " where tenant_id = ? and code = ?", tenant, code);
        assertThat(copied).singleElement().satisfies(row -> assertThat(row)
            .containsEntry("period_key", "2001").containsEntry("next_value", 1L)
            .containsEntry("pattern", "{PREFIX}-{YYYY}-{SEQ:4}").containsEntry("prefix", "PR")
            .containsEntry("reset_policy", "YEARLY").containsEntry("is_active", true));
        assertThat(TenantContext.callAs(tenant, () -> api.next(code))).isEqualTo("PR-" + THIS_YEAR + "-0001");
    }

    // ------------------------------------------------------------------------------------------

    private static NumberSeries series(String code, String prefix, String pattern, ResetPolicy policy,
                                       String periodKey, long nextValue) {
        return NumberSeries.builder().code(code).prefix(prefix).pattern(pattern).resetPolicy(policy)
            .periodKey(periodKey).nextValue(nextValue).build();
    }

    private long newTenant(String prefix) {
        String code = StaffApiClient.unique(prefix);
        return jdbcTemplate.queryForObject(
            "insert into core_tenant (id, code, name_ar, name_en, status_code, created_by, created_at)"
                + " values (nextval('seq_core_tenant'), ?, 'ت', 'T', 'ACTIVE', 'test', now()) returning id",
            Long.class, code);
    }
}
