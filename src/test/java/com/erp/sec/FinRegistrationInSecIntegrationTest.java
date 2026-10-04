package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.main.ErpMainApplication;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.entity.ScreenRegistry;
import com.erp.sec.permission.PermissionConstants;
import com.erp.sec.repository.ActionRegistryRepository;
import com.erp.sec.repository.ModuleRegistryRepository;
import com.erp.sec.repository.ScreenRegistryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coverage for the FIN test-plan case <b>TC-FIN-044</b> ("FIN registers itself into SEC at
 * onboarding", {@code governance/modules/FIN/packages/backend-test/RULE-SCENARIOS.md}).
 *
 * <p><b>Why a FIN-owned case lives in {@code com.erp.sec}:</b> the assertion is entirely about the
 * contents of SEC's own registry ({@code SEC_MODULE_REG} / {@code SEC_SCREEN_REG} /
 * {@code SEC_ACTION_REG}), which are readable only through SEC's own repositories and entities —
 * SEC publishes no module-registry READ endpoint, and its sanctioned cross-module surface
 * {@code SecModuleRegistryApi} exposes only {@code boolean isModuleActive(String)}, far too narrow
 * to assert 13 screens and 30 action rows. {@code com.erp.architecture.CrossModuleBoundaryArchTest
 * #modules_only_expose_their_crossmodule_package_to_outsiders} forbids any class outside
 * {@code com.erp.sec} — {@code com.erp.fin} included — from depending on SEC internals, so this
 * test cannot live beside the rest of FIN's in-process coverage in
 * {@code com.erp.fin.FinYearEndCoverageIntegrationTest}, where it originally sat and where it
 * produced 19 arch-rule violations. <b>Do not "helpfully" move it back into {@code com.erp.fin}:</b>
 * doing so re-breaks that ArchUnit rule. {@code com.erp.sec.SecCoverageIntegrationTest} is the
 * standing precedent for reading SEC repositories directly from this package.
 *
 * <p>Same conventions as that precedent: the real dev Postgres/Redis under the {@code dev} profile
 * and a class-level {@link Transactional}. Nothing here writes — the rows under test are committed
 * Flyway migration output — and no {@link org.springframework.security.core.context.SecurityContext}
 * is needed, since repositories are read directly rather than through a gated service.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class FinRegistrationInSecIntegrationTest {

    /** The module code {@code V24__fin_security_seed.sql} registers FIN under. */
    private static final String FIN_MODULE_CODE = "FIN";

    @Autowired
    private ModuleRegistryRepository moduleRegistryRepository;
    @Autowired
    private ScreenRegistryRepository screenRegistryRepository;
    @Autowired
    private ActionRegistryRepository actionRegistryRepository;

    // =========================================================================================
    // TC-FIN-044 — FIN registers itself into SEC at onboarding
    // =========================================================================================

    @Test
    void finIsRegisteredIntoSecWithOneModuleItsScreensAndItsActions() {
        // Covers: TC-FIN-044, AC-FIN-044 / REQ-FIN-044.
        //
        // FIN's onboarding IS V24__fin_security_seed.sql (its header records why a Flyway seed and
        // not a runtime call to API-SEC-018/019/020: no HTTP server exists at migration time, and
        // V17/V19 set the precedent for SEC's own and MDL's registrations). Flyway runs it on
        // application startup, so by the time this context is up the onboarding has run and the
        // rows below are its observable result. SEC publishes no module-registry READ endpoint,
        // which is exactly why this case is here and not in the api-verify run.
        //
        // Nothing is written and no SecurityContext is needed: this is a pure read of committed
        // migration output.
        List<ModuleRegistry> finModules = moduleRegistryRepository.findAll(
            (Specification<ModuleRegistry>) (root, query, cb) ->
                cb.equal(root.get("code"), FIN_MODULE_CODE));
        assertThat(finModules)
            .as("exactly 1 ModuleRegistry row for FIN")
            .hasSize(1);
        ModuleRegistry finModule = finModules.get(0);
        assertThat(finModule.getIsActiveFl()).isTrue();

        List<ScreenRegistry> finScreens = screenRegistryRepository.findAll(
            (Specification<ScreenRegistry>) (root, query, cb) ->
                cb.equal(root.get("module").get("code"), FIN_MODULE_CODE));

        // v2: thirteen page codes. V24's "2. SEC_SCREEN_REG" VALUES list seeds the twelve v1
        // ones and V37__fin_v2_account_mappings_security_seed.sql (section 1) adds
        // FIN_ACCOUNT_MAPPINGS for SCR-REQ-FIN-013 — srs-fin.md v2 records "SCR-REQ count: 13".
        // Named here so the assertion is on the identities and not merely on a count.
        assertThat(finScreens)
            .extracting(ScreenRegistry::getPageCode)
            .containsExactlyInAnyOrder(
                "FIN_ACCOUNTS", "FIN_DIMENSIONS", "FIN_RULES", "FIN_RECURRING_TEMPLATES",
                "FIN_ALLOCATION_RULES", "FIN_JOURNAL_ENTRIES", "FIN_PERIODS",
                "FIN_ACCOUNT_LEDGER", "FIN_TRIAL_BALANCE", "FIN_BALANCE_SHEET",
                "FIN_INCOME_STATEMENT", "FIN_DIMENSION_REPORTS",
                "FIN_ACCOUNT_MAPPINGS");
        assertThat(finScreens).hasSize(13);
        assertThat(finScreens)
            .as("1 ScreenRegistry row PER screen — no duplicate page code")
            .extracting(ScreenRegistry::getPageCode)
            .doesNotHaveDuplicates();
        assertThat(finScreens).allSatisfy(screen ->
            assertThat(screen.getModule().getModuleRegPk())
                .isEqualTo(finModule.getModuleRegPk()));

        // "1 ActionRegistry row per action", thirty in v2: V24's section 3 (26 rows), plus
        // V28__fin_dimensions_update_action.sql (FIN_DIMENSIONS/UPDATE), plus V37 section 2's
        // three FIN_ACCOUNT_MAPPINGS actions VIEW/CREATE/UPDATE. Asserted as the set of
        // PERM_<PAGE_CODE>_<ACTION_CODE> codes, one row each — the shape V24/V28/V37 derive.
        List<ActionRegistry> finActions = actionRegistryRepository.findAll(
            (Specification<ActionRegistry>) (root, query, cb) ->
                cb.equal(root.get("screen").get("module").get("code"), FIN_MODULE_CODE));

        assertThat(finActions)
            .as("1 ActionRegistry row per action — no duplicate permission code")
            .extracting(ActionRegistry::getPermissionCode)
            .doesNotHaveDuplicates();
        assertThat(finActions).allSatisfy(action -> assertThat(action.getPermissionCode())
            .isEqualTo("PERM_" + action.getScreen().getPageCode() + "_"
                + action.getActionCode()));
        assertThat(finActions)
            .extracting(ActionRegistry::getPermissionCode)
            .containsExactlyInAnyOrder(
                "PERM_FIN_ACCOUNTS_VIEW", "PERM_FIN_ACCOUNTS_CREATE", "PERM_FIN_ACCOUNTS_UPDATE",
                "PERM_FIN_DIMENSIONS_VIEW", "PERM_FIN_DIMENSIONS_CREATE",
                "PERM_FIN_DIMENSIONS_UPDATE",
                "PERM_FIN_RULES_VIEW", "PERM_FIN_RULES_CREATE", "PERM_FIN_RULES_UPDATE",
                "PERM_FIN_RECURRING_TEMPLATES_VIEW", "PERM_FIN_RECURRING_TEMPLATES_CREATE",
                "PERM_FIN_RECURRING_TEMPLATES_UPDATE",
                "PERM_FIN_ALLOCATION_RULES_VIEW", "PERM_FIN_ALLOCATION_RULES_CREATE",
                "PERM_FIN_ALLOCATION_RULES_UPDATE",
                "PERM_FIN_JOURNAL_ENTRIES_VIEW", "PERM_FIN_JOURNAL_ENTRIES_CREATE",
                "PERM_FIN_JOURNAL_ENTRIES_REVERSE",
                "PERM_FIN_PERIODS_VIEW", "PERM_FIN_PERIODS_CREATE", "PERM_FIN_PERIODS_UPDATE",
                "PERM_FIN_PERIODS_CLOSE_APPROVE",
                "PERM_FIN_ACCOUNT_LEDGER_VIEW", "PERM_FIN_TRIAL_BALANCE_VIEW",
                "PERM_FIN_BALANCE_SHEET_VIEW", "PERM_FIN_INCOME_STATEMENT_VIEW",
                "PERM_FIN_DIMENSION_REPORTS_VIEW",
                "PERM_FIN_ACCOUNT_MAPPINGS_VIEW", "PERM_FIN_ACCOUNT_MAPPINGS_CREATE",
                "PERM_FIN_ACCOUNT_MAPPINGS_UPDATE");
        assertThat(finActions).hasSize(30);

        // Every PERM_FIN_* constant FIN's @PreAuthorize gates reference must resolve to one of
        // those rows — a registration that missed one would leave an endpoint permanently
        // ungrantable.
        assertThat(finActions)
            .extracting(ActionRegistry::getPermissionCode)
            .contains(PermissionConstants.PERM_FIN_PERIODS_CLOSE_APPROVE,
                PermissionConstants.PERM_FIN_JOURNAL_ENTRIES_CREATE);
    }
}
