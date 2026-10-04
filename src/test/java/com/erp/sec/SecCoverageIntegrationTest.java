package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.util.TokenHasher;
import com.erp.main.ErpMainApplication;
import com.erp.notif.crossmodule.DispatchLogRecord;
import com.erp.notif.crossmodule.NotificationChannelAdminApi;
import com.erp.notif.crossmodule.NotificationLogQueryApi;
import com.erp.sec.controller.AuditLogController;
import com.erp.sec.crossmodule.SecUserDirectoryApi;
import com.erp.sec.crossmodule.UserContact;
import com.erp.sec.dto.ConfirmationResponse;
import com.erp.sec.dto.DashboardResponse;
import com.erp.sec.dto.LoginRequest;
import com.erp.sec.dto.LoginResponse;
import com.erp.sec.dto.PasswordResetCompleteRequest;
import com.erp.sec.dto.PasswordResetRequest;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.entity.PasswordResetToken;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.RoleActionGrant;
import com.erp.sec.entity.ScreenRegistry;
import com.erp.sec.entity.User;
import com.erp.sec.entity.UserRoleAssignment;
import com.erp.sec.permission.PermissionConstants;
import com.erp.sec.repository.ActionRegistryRepository;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.ModuleRegistryRepository;
import com.erp.sec.repository.PasswordResetTokenRepository;
import com.erp.sec.repository.RoleActionGrantRepository;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.ScreenRegistryRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.repository.UserRoleAssignmentRepository;
import com.erp.sec.service.AuthService;
import com.erp.sec.service.DashboardService;
import com.erp.sec.service.PasswordResetService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Coverage for the 7 SEC test-plan cases ({@code governance/modules/SEC/test_gen/backend-test-plan-sec.md})
 * that neither TestSprite nor the hand-written MODE-5 {@code requests}-based script
 * ({@code governance/modules/SEC/test-api/test_sec_apis.py}) can exercise, because each needs
 * either in-process access (a value never returned over HTTP, a cross-module Spring interface with
 * no HTTP surface) or fixture data no HTTP client can construct (a role missing one specific
 * permission, a deliberately deactivated grant row). This is a new, standalone JUnit suite — it
 * does not touch the TestSprite mechanism, the MODE-5 script, or {@code execution-state.json}.
 *
 * <p>Runs against the real dev Postgres/Redis (docker/docker-compose.yml, {@code dev} profile) the
 * same way the running application does; every test wraps its writes in the outer class-level
 * {@link Transactional} and rolls back on completion, so nothing here leaves data behind.
 *
 * <p>TC-SEC-033 is deliberately NOT covered here: it requires a real FIN endpoint gated by the
 * CORE module-access interceptor, and {@code com.erp.fin} does not exist anywhere under
 * {@code src/main/java} (confirmed via {@code find src/main/java/com/erp/fin}), nor is FIN listed
 * in {@code governance/modules-registry.json}. There is no code to exercise, in-process or
 * otherwise — see the task report rather than a stub test here.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class SecCoverageIntegrationTest {

    @Autowired
    private PasswordResetService passwordResetService;
    @Autowired
    private AuthService authService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private SecUserDirectoryApi secUserDirectoryApi;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Autowired
    private ActionRegistryRepository actionRegistryRepository;
    @Autowired
    private RoleActionGrantRepository roleActionGrantRepository;
    @Autowired
    private ModuleRegistryRepository moduleRegistryRepository;
    @Autowired
    private ScreenRegistryRepository screenRegistryRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private AuditLogEntryRepository auditLogEntryRepository;

    @Autowired
    private NotificationLogQueryApi notificationLogQueryApi;
    @Autowired
    private NotificationChannelAdminApi notificationChannelAdminApi;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------------------------
    // TC-SEC-007 — complete a password reset successfully
    // -----------------------------------------------------------------------------------------

    @Test
    void completePasswordReset_updatesHashSetsUsedAtAndAppendsAudit() {
        // Covers: TC-SEC-007, AC-SEC-007
        //
        // The raw reset token is only ever emailed (PasswordResetService.dispatchResetNotification)
        // — no HTTP response ever carries it, so an HTTP-only test cannot obtain one to complete a
        // reset with. Here the precondition ("an unexpired, unused PasswordResetToken") is built
        // directly through the repository with a known raw value, hashed exactly the way production
        // does it (TokenHasher.sha256Hex) — equivalent to the request step's own issueToken(), minus
        // the notification side-channel this TC does not need.
        User user = persistUser("pwreset");
        String originalHash = user.getPasswordHash();

        String rawToken = "raw-" + UUID.randomUUID();
        PasswordResetToken token = passwordResetTokenRepository.save(PasswordResetToken.builder()
            .user(user)
            .tokenHash(TokenHasher.sha256Hex(rawToken))
            .build());

        ServiceResult<ConfirmationResponse> result = passwordResetService.complete(
            PasswordResetCompleteRequest.builder()
                .token(rawToken)
                .newPassword("N3wP@ssw0rd!")
                .build());

        assertThat(result.getData()).isNotNull();

        User reloadedUser = userRepository.findById(user.getUserPk()).orElseThrow();
        assertThat(reloadedUser.getPasswordHash()).isNotEqualTo(originalHash);
        assertThat(passwordEncoder.matches("N3wP@ssw0rd!", reloadedUser.getPasswordHash())).isTrue();

        PasswordResetToken reloadedToken =
            passwordResetTokenRepository.findById(token.getPwdResetTokenPk()).orElseThrow();
        assertThat(reloadedToken.getUsedAt()).isNotNull();

        Specification<AuditLogEntry> completedForUser = (root, query, cb) -> cb.and(
            cb.equal(root.get("eventTypeCode"), "PASSWORD_RESET_COMPLETED"),
            cb.equal(root.get("actor").get("userPk"), user.getUserPk()));
        List<AuditLogEntry> auditRows = auditLogEntryRepository.findAll(completedForUser);
        assertThat(auditRows).hasSize(1);
    }

    // -----------------------------------------------------------------------------------------
    // TC-SEC-023 — hide an ungranted dashboard widget
    // -----------------------------------------------------------------------------------------

    @Test
    void dashboardSummary_hidesActiveSessionsWidgetWithoutThatPermission() {
        // Covers: TC-SEC-023, AC-SEC-023
        //
        // A role missing exactly one permission is not constructible over HTTP (there is no
        // "grant every permission except one" request shape) — built directly here via
        // Role/RoleActionGrant/UserRoleAssignment. The role is otherwise a full administrator (every
        // other VIEW widget granted) so the test proves the sessions widget disappears because of
        // that ONE missing grant, not because the role is generally unprivileged.
        User user = persistUser("dashboard");
        Role role = roleRepository.save(Role.builder()
            .code("TROLE" + uniqueSuffix())
            .nameAr("دور اختبار لوحة التحكم")
            .nameEn("Dashboard test role")
            .build());

        grantExistingSeededPermission(role, PermissionConstants.PERM_SEC_DASHBOARD_VIEW);
        grantExistingSeededPermission(role, PermissionConstants.PERM_SEC_USERS_VIEW);
        grantExistingSeededPermission(role, PermissionConstants.PERM_SEC_AUDIT_LOG_VIEW);
        grantExistingSeededPermission(role, PermissionConstants.PERM_SEC_ROLES_VIEW);
        // Deliberately NOT granted: PermissionConstants.PERM_SEC_SESSIONS_VIEW.

        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
            .user(user)
            .role(role)
            .assignedBy("SecCoverageIntegrationTest")
            .build());

        setAuthenticatedPrincipal(user.getUsername(),
            PermissionConstants.PERM_SEC_DASHBOARD_VIEW,
            PermissionConstants.PERM_SEC_USERS_VIEW,
            PermissionConstants.PERM_SEC_AUDIT_LOG_VIEW,
            PermissionConstants.PERM_SEC_ROLES_VIEW);

        ServiceResult<DashboardResponse> result = dashboardService.summary();
        DashboardResponse response = result.getData();

        assertThat(response.getActiveSessions())
            .as("active-sessions widget must be absent — role holds no PERM_SEC_SESSIONS_VIEW")
            .isNull();
        assertThat(response.getUsersOverview()).isNotNull();
        assertThat(response.getRolesPermissionsSummary()).isNotNull();
    }

    // -----------------------------------------------------------------------------------------
    // TC-SEC-024 — every security event appends an immutable audit entry
    // -----------------------------------------------------------------------------------------

    @Test
    void login_appendsAuditEntry_andNoControllerRouteCanMutateIt() {
        // Covers: TC-SEC-024, AC-SEC-024
        //
        // Step 1: perform a login (the plan's representative event) and confirm one AuditLogEntry
        // with the correct eventTypeCode/actor/timestamp was appended.
        // Step 2 ("attempt to update or delete the resulting AuditLogEntry — no such endpoint
        // exists"): POL-SEC-009 immutability is enforced structurally, by omission, at the HTTP
        // layer — AuditLogController declares only /search (POST) and /export (GET). A JUnit test
        // reflects on the controller's declared methods rather than trying a real HTTP PUT/PATCH/
        // DELETE against a route that, by design, was never mapped.
        String rawPassword = "L0gin!Pass1";
        User user = persistUser("login", rawPassword);

        Instant before = Instant.now().minusSeconds(1);
        ServiceResult<LoginResponse> result = authService.login(
            LoginRequest.builder().username(user.getUsername()).password(rawPassword).build(),
            "127.0.0.1");

        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getAccessToken()).isNotBlank();

        Specification<AuditLogEntry> loginSuccessForUser = (root, query, cb) -> cb.and(
            cb.equal(root.get("eventTypeCode"), "LOGIN_SUCCESS"),
            cb.equal(root.get("actor").get("userPk"), user.getUserPk()));
        List<AuditLogEntry> matches = auditLogEntryRepository.findAll(loginSuccessForUser);

        assertThat(matches).hasSize(1);
        AuditLogEntry entry = matches.get(0);
        assertThat(entry.getEventTypeCode()).isEqualTo("LOGIN_SUCCESS");
        assertThat(entry.getActor().getUserPk()).isEqualTo(user.getUserPk());
        assertThat(entry.getOccurredAt()).isAfterOrEqualTo(before);

        for (Method method : AuditLogController.class.getDeclaredMethods()) {
            assertThat(method.isAnnotationPresent(PutMapping.class))
                .as(method.getName() + " must not be a PUT route — audit log is append-only")
                .isFalse();
            assertThat(method.isAnnotationPresent(PatchMapping.class))
                .as(method.getName() + " must not be a PATCH route — audit log is append-only")
                .isFalse();
            assertThat(method.isAnnotationPresent(DeleteMapping.class))
                .as(method.getName() + " must not be a DELETE route — audit log is append-only")
                .isFalse();
        }
    }

    // -----------------------------------------------------------------------------------------
    // TC-SEC-029 — optional notification on password reset
    // -----------------------------------------------------------------------------------------

    @Test
    void passwordResetRequest_dispatchesNotification_andStillSucceedsWithChannelDisabled() {
        // Covers: TC-SEC-029, AC-SEC-029
        //
        // Half 1 ("integration ON"): request() must dispatch one NOTIF_LOG row carrying the
        // password-reset templateCode. SEC has no read endpoint of its own for this — read via
        // NOTIF's own crossmodule.NotificationLogQueryApi (added 2026-09-11) rather than reaching
        // into notif.repository/notif.entity directly, per CrossModuleBoundaryArchTest.
        User userWithNotifOn = persistUser("pwresetnotifon");
        passwordResetService.request(
            PasswordResetRequest.builder().email(userWithNotifOn.getEmail()).build());

        List<DispatchLogRecord> logsOn = notificationLogQueryApi.findByRecipientModuleAndReference(
            userWithNotifOn.getUserPk(), "SEC", "SEC_PWD_RESET_TOKEN");

        assertThat(logsOn).isNotEmpty();
        assertThat(logsOn)
            .allSatisfy(log -> assertThat(log.templateCode()).isEqualTo("PASSWORD_RESET"));

        // Half 2 ("integration OFF/unavailable"): REQ-SEC-006 must still succeed unchanged.
        // PasswordResetService.dispatchResetNotification only ever catches a RuntimeException from
        // the dispatch call; disabling the EMAIL channel config does not make NOTIF throw (RULE-
        // NOTIF-003 routes a disabled channel to a CHANNEL_DISABLED log row instead) — so this
        // exercises the "integration unavailable" half exactly the way NOTIF actually reports it,
        // rather than assuming a hard failure. Toggled via NOTIF's own crossmodule.
        // NotificationChannelAdminApi (added 2026-09-11) rather than mutating the entity/repository
        // directly, so NOTIF's own update path (and any future validation on it) still runs. Safe
        // to mutate: the whole test method is wrapped in the class-level @Transactional and rolls
        // back on completion.
        //
        // The channel-administration call is the ONLY step here that needs a principal:
        // NotificationChannelConfigService.update is gated on PERM_NOTIF_CHANNELS_UPDATE. The
        // elevation is opened immediately before it and cleared immediately after, so it covers
        // the setup call and nothing else. POST /api/v1/sec/auth/password-reset/request is an
        // ANONYMOUS endpoint in production, and the assertion this TC actually makes is that it
        // still succeeds with the channel disabled — letting an authenticated context leak into
        // that call would quietly convert an anonymous-path test into an authenticated-path one
        // and it would stop proving REQ-SEC-006. Hence the explicit clear below rather than
        // relying on the class's @AfterEach, which only runs after the assertions.
        setAuthenticatedPrincipal("notif-channel-admin-" + uniqueSuffix(),
            PermissionConstants.PERM_NOTIF_CHANNELS_UPDATE);
        notificationChannelAdminApi.setChannelEnabled("EMAIL", false);
        SecurityContextHolder.clearContext();

        User userWithNotifOff = persistUser("pwresetnotifoff");
        ServiceResult<ConfirmationResponse> resultOff = passwordResetService.request(
            PasswordResetRequest.builder().email(userWithNotifOff.getEmail()).build());

        assertThat(resultOff.getData())
            .as("REQ-SEC-006 must succeed unchanged even with the notification channel disabled")
            .isNotNull();

        Specification<PasswordResetToken> tokenForOffUser = (root, query, cb) ->
            cb.equal(root.get("user").get("userPk"), userWithNotifOff.getUserPk());
        assertThat(passwordResetTokenRepository.findAll(tokenForOffUser)).hasSize(1);
    }

    // -----------------------------------------------------------------------------------------
    // TC-SEC-034 — cross-module read of a user's contact details
    // -----------------------------------------------------------------------------------------

    @Test
    void findContact_returnsNarrowContact_forActiveDisabledAndUnknownUsers() {
        // Covers: TC-SEC-034, AC-SEC-034
        //
        // SecUserDirectoryApi has no HTTP surface by design — exercised here exactly the way a
        // consuming module's service would, by direct Spring interface injection.
        User active = persistUser("contactactive");
        User disabled = persistUser("contactdisabled");
        disabled.deactivate();
        userRepository.save(disabled);

        setAuthenticatedPrincipal("xm-caller-" + uniqueSuffix());

        Optional<UserContact> activeContact = secUserDirectoryApi.findContact(active.getUserPk());
        assertThat(activeContact).isPresent();
        UserContact contact = activeContact.get();
        assertThat(contact.userPk()).isEqualTo(active.getUserPk());
        assertThat(contact.email()).isEqualTo(active.getEmail());
        assertThat(contact.fullNameAr()).isEqualTo(active.getFullNameAr());
        assertThat(contact.fullNameEn()).isEqualTo(active.getFullNameEn());
        assertThat(contact.active()).isTrue();

        Optional<UserContact> disabledContact = secUserDirectoryApi.findContact(disabled.getUserPk());
        assertThat(disabledContact).isPresent();
        assertThat(disabledContact.get().active()).isFalse();
        // Never the password hash (POL-SEC-004), nor anything but the four contact fields — the
        // UserContact record type itself is the guarantee (see its own javadoc); there is no
        // getPasswordHash()/getStatusCode() to even accidentally assert against.

        Optional<UserContact> unknown = secUserDirectoryApi.findContact(-999_999_999L);
        assertThat(unknown).isEmpty();
    }

    // -----------------------------------------------------------------------------------------
    // TC-SEC-035 — cross-module read of the user ids holding a permission code
    // -----------------------------------------------------------------------------------------

    @Test
    void findUserIdsHoldingPermission_excludesInactiveRoleAndInactiveAction_isDistinct() {
        // Covers: TC-SEC-035, AC-SEC-035
        //
        // permissionCode carries a UNIQUE constraint (UQ_SEC_ACTION_REG_PERM), so the same code
        // cannot simultaneously back both an active and an inactive ActionRegistry row — the plan's
        // "user D holds an action row that is itself INACTIVE" is realised here as its own, third
        // permission code whose ActionRegistry row is deactivated: querying THAT code must come back
        // empty even though D holds an ACTIVE role with a grant row for it, because the action itself
        // is inactive. This still isolates and proves every predicate REQ-SEC-035's query applies:
        // active role required (excludes C), active action required (excludes D from its own code),
        // DISTINCT across multiple qualifying roles (A and B via two different roles), per-code
        // independence (a second, unrelated code), and "no action row at all" (a fourth, never-
        // registered code).
        ModuleRegistry module = moduleRegistryRepository.save(ModuleRegistry.builder()
            .code("T" + uniqueSuffix().substring(0, 6))
            .nameAr("وحدة اختبار الصلاحيات")
            .nameEn("XM permission test module")
            .build());
        ScreenRegistry screen = screenRegistryRepository.save(ScreenRegistry.builder()
            .pageCode("TSCR" + uniqueSuffix())
            .module(module)
            .nameAr("شاشة اختبار")
            .nameEn("XM permission test screen")
            .build());

        ActionRegistry permA = actionRegistryRepository.save(ActionRegistry.builder()
            .permissionCode("PERM_TEST_A_" + uniqueSuffix())
            .screen(screen).actionCode("ACT_A_" + uniqueSuffix())
            .nameAr("إجراء أ").nameEn("Action A")
            .build());
        ActionRegistry permB = actionRegistryRepository.save(ActionRegistry.builder()
            .permissionCode("PERM_TEST_B_" + uniqueSuffix())
            .screen(screen).actionCode("ACT_B_" + uniqueSuffix())
            .nameAr("إجراء ب").nameEn("Action B")
            .build());
        ActionRegistry inactivePerm = actionRegistryRepository.save(ActionRegistry.builder()
            .permissionCode("PERM_TEST_D_" + uniqueSuffix())
            .screen(screen).actionCode("ACT_D_" + uniqueSuffix())
            .nameAr("إجراء غير فعال").nameEn("Inactive action")
            .isActiveFl(false)
            .build());

        Role activeRole1 = roleRepository.save(Role.builder()
            .code("RACT1" + uniqueSuffix()).nameAr("دور فعال 1").nameEn("Active role 1").build());
        Role activeRole2 = roleRepository.save(Role.builder()
            .code("RACT2" + uniqueSuffix()).nameAr("دور فعال 2").nameEn("Active role 2").build());
        Role inactiveRole = roleRepository.save(Role.builder()
            .code("RINACT" + uniqueSuffix()).nameAr("دور غير فعال").nameEn("Inactive role")
            .isActiveFl(false).build());
        Role roleForD = roleRepository.save(Role.builder()
            .code("RFORD" + uniqueSuffix()).nameAr("دور المستخدم د").nameEn("Role for user D")
            .build());

        saveGrant(activeRole1, permA);
        saveGrant(activeRole2, permA);
        saveGrant(inactiveRole, permA);
        saveGrant(activeRole2, permB);
        saveGrant(roleForD, inactivePerm);

        User userA = persistUser("xmA");
        User userB = persistUser("xmB");
        User userC = persistUser("xmC");
        User userD = persistUser("xmD");

        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
            .user(userA).role(activeRole1).assignedBy("SecCoverageIntegrationTest").build());
        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
            .user(userB).role(activeRole2).assignedBy("SecCoverageIntegrationTest").build());
        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
            .user(userC).role(inactiveRole).assignedBy("SecCoverageIntegrationTest").build());
        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
            .user(userD).role(roleForD).assignedBy("SecCoverageIntegrationTest").build());

        setAuthenticatedPrincipal("xm-caller-" + uniqueSuffix());

        // 1. holders of the first permission code: exactly A and B, DISTINCT, C excluded (inactive role)
        List<Long> holdersOfA = secUserDirectoryApi.findUserIdsHoldingPermission(permA.getPermissionCode());
        assertThat(holdersOfA).containsExactlyInAnyOrder(userA.getUserPk(), userB.getUserPk());

        // 2. a second, independent code computed the same way: only B holds it
        List<Long> holdersOfB = secUserDirectoryApi.findUserIdsHoldingPermission(permB.getPermissionCode());
        assertThat(holdersOfB).containsExactly(userB.getUserPk());

        // D's only grant is through an ACTIVE role for an INACTIVE action — must not count as a holder
        List<Long> holdersOfInactive =
            secUserDirectoryApi.findUserIdsHoldingPermission(inactivePerm.getPermissionCode());
        assertThat(holdersOfInactive).isEmpty();

        // 3. a permission code no action row carries at all
        List<Long> holdersOfUnheld =
            secUserDirectoryApi.findUserIdsHoldingPermission("PERM_TEST_UNHELD_" + uniqueSuffix());
        assertThat(holdersOfUnheld).isEmpty();
    }

    // -----------------------------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------------------------

    private User persistUser(String tag) {
        return persistUser(tag, "Def@ultPass1");
    }

    private User persistUser(String tag, String rawPassword) {
        String unique = tag + "-" + uniqueSuffix();
        User user = User.builder()
            .username("tc-" + unique)
            .email("tc-" + unique + "@example.com")
            .passwordHash(passwordEncoder.encode(rawPassword))
            .fullNameAr("مستخدم اختبار " + tag)
            .fullNameEn("Test user " + tag)
            .statusCode(User.STATUS_ACTIVE)
            .build();
        return userRepository.save(user);
    }

    /** Looks up an ActionRegistry row seeded by V17__sec_security_seed.sql by its permission code. */
    private void grantExistingSeededPermission(Role role, String permissionCode) {
        Specification<ActionRegistry> byPermissionCode =
            (root, query, cb) -> cb.equal(root.get("permissionCode"), permissionCode);
        ActionRegistry action = actionRegistryRepository.findOne(byPermissionCode)
            .orElseThrow(() -> new IllegalStateException(
                "Expected V17-seeded action for permission code: " + permissionCode));
        saveGrant(role, action);
    }

    private void saveGrant(Role role, ActionRegistry action) {
        roleActionGrantRepository.save(RoleActionGrant.builder()
            .role(role)
            .action(action)
            .grantedBy("SecCoverageIntegrationTest")
            .build());
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
