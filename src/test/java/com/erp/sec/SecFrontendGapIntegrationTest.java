package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.main.ErpMainApplication;
import com.erp.sec.dto.RoleGrantTreeResponse;
import com.erp.sec.dto.RoleModuleGrantNodeResponse;
import com.erp.sec.dto.RoleResponse;
import com.erp.sec.dto.RoleScreenGrantNodeResponse;
import com.erp.sec.dto.RoleUpdateRequest;
import com.erp.sec.dto.SignupRequestResponse;
import com.erp.sec.dto.SignupRequestSearchRequest;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.RoleActionGrant;
import com.erp.sec.entity.RoleModuleGrant;
import com.erp.sec.entity.RoleScreenGrant;
import com.erp.sec.entity.ScreenRegistry;
import com.erp.sec.entity.SignupRequest;
import com.erp.sec.permission.PermissionConstants;
import com.erp.sec.repository.ActionRegistryRepository;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.ModuleRegistryRepository;
import com.erp.sec.repository.RoleActionGrantRepository;
import com.erp.sec.repository.RoleModuleGrantRepository;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.RoleScreenGrantRepository;
import com.erp.sec.repository.ScreenRegistryRepository;
import com.erp.sec.repository.SignupRequestRepository;
import com.erp.sec.service.AuditLogService;
import com.erp.sec.service.RoleGrantService;
import com.erp.sec.service.RoleService;
import com.erp.sec.service.SignupRequestService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers the four backend gaps the frontend E2E run of 2026-09-18 reported
 * ({@code sec-frontend-e2e-run-2026-09-18.md}): the sign-up request list, a role's currently-held
 * grants, role update, and the audit-log CSV export's charset. Each one was an endpoint or a
 * response detail the frontend consumes but the backend never published.
 *
 * <p>Same posture as {@link SecCoverageIntegrationTest}: real dev Postgres/Redis, {@code dev}
 * profile, every write rolled back by the class-level {@link Transactional}.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class SecFrontendGapIntegrationTest {

    @Autowired
    private SignupRequestService signupRequestService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleGrantService roleGrantService;
    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SignupRequestRepository signupRequestRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ModuleRegistryRepository moduleRegistryRepository;
    @Autowired
    private ScreenRegistryRepository screenRegistryRepository;
    @Autowired
    private ActionRegistryRepository actionRegistryRepository;
    @Autowired
    private RoleModuleGrantRepository roleModuleGrantRepository;
    @Autowired
    private RoleScreenGrantRepository roleScreenGrantRepository;
    @Autowired
    private RoleActionGrantRepository roleActionGrantRepository;
    @Autowired
    private AuditLogEntryRepository auditLogEntryRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------------------------
    // Gap 1 — the sign-up request list (blocked TC-SEC-040 / TC-SEC-041)
    // -----------------------------------------------------------------------------------------

    @Test
    void signupRequestSearch_listsPendingRows_theDashboardOnlyEverCounted() {
        SignupRequest pending = persistSignupRequest("PENDING");
        persistSignupRequest("REJECTED");

        setAuthenticatedPrincipal("signup-reviewer", PermissionConstants.PERM_SEC_USERS_VIEW);

        ServiceResult<Page<SignupRequestResponse>> result = signupRequestService.search(
            SignupRequestSearchRequest.builder()
                .filters(List.of(SearchFilter.builder()
                    .field("statusCode").operator(SearchOperator.EQUALS).value("PENDING").build()))
                .sortField("submittedAt")
                .size(200)
                .build());

        List<SignupRequestResponse> rows = result.getData().getContent();
        assertThat(rows)
            .as("the PENDING filter must return the row the dashboard counts")
            .anySatisfy(row -> assertThat(row.getSignupRequestPk())
                .isEqualTo(pending.getSignupRequestPk()));
        assertThat(rows)
            .as("no non-PENDING row may leak through the status filter")
            .allSatisfy(row -> assertThat(row.getStatusCode()).isEqualTo("PENDING"));
        assertThat(rows)
            .as("the list must carry the fields the review screen renders")
            .allSatisfy(row -> {
                assertThat(row.getEmail()).isNotBlank();
                assertThat(row.getFullNameAr()).isNotBlank();
                assertThat(row.getFullNameEn()).isNotBlank();
                assertThat(row.getSubmittedAt()).isNotNull();
            });
    }

    // -----------------------------------------------------------------------------------------
    // Gap 2 — a role's currently-held grants (blocked TC-SEC-049)
    // -----------------------------------------------------------------------------------------

    @Test
    void roleGrantTree_returnsHeldGrantsNested_soARevokeCanNameItsCascade() {
        Role role = persistRole();
        ActionRegistry action = anySeededAction();
        ScreenRegistry screen = action.getScreen();
        ModuleRegistry module = screen.getModule();

        roleModuleGrantRepository.save(RoleModuleGrant.builder()
            .role(role).module(module).grantedBy("SecFrontendGapIntegrationTest").build());
        roleScreenGrantRepository.save(RoleScreenGrant.builder()
            .role(role).screen(screen).grantedBy("SecFrontendGapIntegrationTest").build());
        roleActionGrantRepository.save(RoleActionGrant.builder()
            .role(role).action(action).grantedBy("SecFrontendGapIntegrationTest").build());

        setAuthenticatedPrincipal("role-auditor", PermissionConstants.PERM_SEC_ROLES_VIEW);

        RoleGrantTreeResponse tree = roleGrantService.grantsOf(role.getRolePk()).getData();

        assertThat(tree.getRolePk()).isEqualTo(role.getRolePk());
        RoleModuleGrantNodeResponse moduleNode = tree.getModules().stream()
            .filter(node -> node.getModuleRegPk().equals(module.getModuleRegPk()))
            .findFirst()
            .orElseThrow();
        assertThat(moduleNode.getGranted()).isTrue();
        assertThat(moduleNode.getGrantedAt()).isNotNull();

        RoleScreenGrantNodeResponse screenNode = moduleNode.getScreens().stream()
            .filter(node -> node.getScreenRegPk().equals(screen.getScreenRegPk()))
            .findFirst()
            .orElseThrow();
        assertThat(screenNode.getGranted()).isTrue();
        assertThat(screenNode.getActions())
            .as("the cascade a module revoke will remove must be enumerable from this tree")
            .anySatisfy(node -> assertThat(node.getPermissionCode())
                .isEqualTo(action.getPermissionCode()));
    }

    @Test
    void roleGrantTree_isEmptyForARoleHoldingNothing_soAnUncheckedBoxMeansNotGranted() {
        Role role = persistRole();

        setAuthenticatedPrincipal("role-auditor", PermissionConstants.PERM_SEC_ROLES_VIEW);

        assertThat(roleGrantService.grantsOf(role.getRolePk()).getData().getModules()).isEmpty();
    }

    // -----------------------------------------------------------------------------------------
    // Gap 3 — role update
    // -----------------------------------------------------------------------------------------

    @Test
    void roleUpdate_rewritesIdentityFields_andLeavesTheCodeUntouched() {
        Role role = persistRole();
        String originalCode = role.getCode();

        setAuthenticatedPrincipal("role-editor", PermissionConstants.PERM_SEC_ROLES_UPDATE);

        RoleResponse updated = roleService.update(role.getRolePk(), RoleUpdateRequest.builder()
            .nameAr("اسم مصحّح")
            .nameEn("Corrected name")
            .descriptionAr("وصف مصحّح")
            .descriptionEn("Corrected description")
            .build()).getData();

        assertThat(updated.getNameAr()).isEqualTo("اسم مصحّح");
        assertThat(updated.getNameEn()).isEqualTo("Corrected name");
        assertThat(updated.getDescriptionAr()).isEqualTo("وصف مصحّح");
        assertThat(updated.getDescriptionEn()).isEqualTo("Corrected description");
        assertThat(updated.getCode())
            .as("code is the immutable natural key — no update body can move it")
            .isEqualTo(originalCode);
    }

    // -----------------------------------------------------------------------------------------
    // Gap 4 — the audit-log CSV export's charset (TC-SEC-057)
    // -----------------------------------------------------------------------------------------

    @Test
    void auditLogExport_startsWithAUtf8Bom_soExcelReadsArabicAsArabic() {
        String eventTypeCode = "LOGIN_FAILED";
        auditLogEntryRepository.save(AuditLogEntry.builder()
            .eventTypeCode(eventTypeCode)
            .occurredAt(java.time.Instant.now())
            .targetRef("gap-test-" + uniqueSuffix())
            .detailsAr("محاولة دخول فاشلة")
            .detailsEn("Failed login attempt")
            .build());

        setAuthenticatedPrincipal("audit-reader", PermissionConstants.PERM_SEC_AUDIT_LOG_VIEW);

        String csv = auditLogService.export(eventTypeCode, null, null, null).getData();

        assertThat(csv)
            .as("without the BOM Excel opens the document as ANSI and mojibakes detailsAr")
            .startsWith("﻿");
        assertThat(csv).contains("محاولة دخول فاشلة");
    }

    // -----------------------------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------------------------

    private SignupRequest persistSignupRequest(String statusCode) {
        String unique = uniqueSuffix();
        return signupRequestRepository.save(SignupRequest.builder()
            .email("gap-" + unique + "@example.com")
            .fullNameAr("مقدّم طلب " + unique)
            .fullNameEn("Applicant " + unique)
            .statusCode(statusCode)
            .build());
    }

    private Role persistRole() {
        String unique = uniqueSuffix().toUpperCase();
        return roleRepository.save(Role.builder()
            .code("GAP_" + unique)
            .nameAr("دور اختبار")
            .nameEn("Test role")
            .build());
    }

    /** Any V17-seeded action row — the tree is asserted on identity, not on a particular code. */
    private ActionRegistry anySeededAction() {
        return actionRegistryRepository.findAll().stream()
            .filter(action -> action.getScreen() != null && action.getScreen().getModule() != null)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Expected at least one V17-seeded SEC_ACTION_REG row"));
    }

    private void setAuthenticatedPrincipal(String username, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities =
            Stream.of(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, "N/A", grantedAuthorities));
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
