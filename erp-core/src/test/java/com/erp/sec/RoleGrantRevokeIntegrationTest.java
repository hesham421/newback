package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.RoleActionGrant;
import com.erp.sec.entity.RoleModuleGrant;
import com.erp.sec.entity.RoleScreenGrant;
import com.erp.sec.entity.ScreenRegistry;
import com.erp.sec.entity.User;
import com.erp.sec.entity.UserRoleAssignment;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.permission.SecPermissions;
import com.erp.sec.repository.ActionRegistryRepository;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.RoleActionGrantRepository;
import com.erp.sec.repository.RoleModuleGrantRepository;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.RoleScreenGrantRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.repository.UserRoleAssignmentRepository;
import com.erp.sec.service.MenuService;
import com.erp.sec.service.RoleGrantService;
import com.erp.testsupport.AbstractIntegrationTest;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * erp-core 1.3.0, package TM-G: revoking one screen grant (REQ-SEC-080, RULE-SEC-054) or one action
 * grant (REQ-SEC-081, RULE-SEC-055 / ADR-SEC-062), their audit rows, their 404s, and the unchanged
 * module revoke. Shared test database, every write rolled back by the class-level transaction.
 */
@Transactional
class RoleGrantRevokeIntegrationTest extends AbstractIntegrationTest {

    private static final String EDITOR = "grant-revoker";

    @Autowired
    private RoleGrantService roleGrantService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private RoleRepository roleRepository;
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
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ActionRegistry rolesView;
    private ActionRegistry rolesCreate;
    private ActionRegistry rolesUpdate;
    private ActionRegistry usersView;

    @BeforeEach
    void seededSecActions() {
        rolesView = action(SecPermissions.PERM_SEC_ROLES_VIEW);
        rolesCreate = action(SecPermissions.PERM_SEC_ROLES_CREATE);
        rolesUpdate = action(SecPermissions.PERM_SEC_ROLES_UPDATE);
        usersView = action(SecPermissions.PERM_SEC_USERS_VIEW);
        setPrincipal(EDITOR, SecPermissions.PERM_SEC_ROLES_UPDATE);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revokeScreen_cascadesItsActionGrants_auditsNPlusOneRows_andLeavesTheRestAlone() {
        Role role = roleHolding(false, rolesView, rolesCreate, rolesUpdate, usersView);
        ScreenRegistry rolesScreen = rolesView.getScreen();

        int revoked = roleGrantService.revokeScreen(role.getRolePk(), rolesScreen.getScreenRegPk())
            .getData().getRevokedActionGrants();

        assertThat(revoked).isEqualTo(3);
        assertThat(roleScreenGrantRepository.existsByRole_RolePkAndScreen_ScreenRegPk(
            role.getRolePk(), rolesScreen.getScreenRegPk())).isFalse();
        assertThat(heldPermissionCodes(role)).containsExactly(SecPermissions.PERM_SEC_USERS_VIEW);
        assertThat(roleScreenGrantRepository.existsByRole_RolePkAndScreen_ScreenRegPk(
            role.getRolePk(), usersView.getScreen().getScreenRegPk())).isTrue();
        assertThat(roleModuleGrantRepository.existsByRole_RolePkAndModule_ModuleRegPk(
            role.getRolePk(), rolesScreen.getModule().getModuleRegPk())).isTrue();
        assertThat(auditRows(role, "ACTION_REVOKED")).hasSize(3);
        assertThat(auditRows(role, "SCREEN_REVOKED")).singleElement()
            .satisfies(row -> assertThat(row.getTargetRef())
                .isEqualTo(role.getRolePk() + "/" + rolesScreen.getScreenRegPk()));
    }

    @Test
    void revokeAction_ofANonViewAction_removesExactlyThatGrant() {
        Role role = roleHolding(false, rolesView, rolesCreate, rolesUpdate);

        int revoked = roleGrantService.revokeAction(role.getRolePk(), rolesCreate.getActionRegPk())
            .getData().getRevokedActionGrants();

        assertThat(revoked).isEqualTo(1);
        assertThat(heldPermissionCodes(role)).containsExactlyInAnyOrder(
            SecPermissions.PERM_SEC_ROLES_VIEW, SecPermissions.PERM_SEC_ROLES_UPDATE);
        assertThat(auditRows(role, "ACTION_REVOKED")).singleElement()
            .satisfies(row -> assertThat(row.getTargetRef())
                .isEqualTo(role.getRolePk() + "/" + rolesCreate.getActionRegPk()));
    }

    @Test
    void revokeAction_ofView_cascadesTheScreensOtherActions_andKeepsTheScreenGrant() {
        Role role = roleHolding(false, rolesView, rolesCreate, rolesUpdate, usersView);

        int revoked = roleGrantService.revokeAction(role.getRolePk(), rolesView.getActionRegPk())
            .getData().getRevokedActionGrants();

        assertThat(revoked).as("VIEW plus the two other actions of SEC_ROLES").isEqualTo(3);
        assertThat(heldPermissionCodes(role)).containsExactly(SecPermissions.PERM_SEC_USERS_VIEW);
        assertThat(roleScreenGrantRepository.existsByRole_RolePkAndScreen_ScreenRegPk(
            role.getRolePk(), rolesView.getScreen().getScreenRegPk())).isTrue();
        assertThat(auditRows(role, "ACTION_REVOKED")).hasSize(3);
        assertThat(auditRows(role, "SCREEN_REVOKED")).isEmpty();
    }

    @Test
    void unknownGrant_isSec404Grant_forBothRevokes() {
        Role role = roleHolding(false, rolesView);

        assertNotFound(() -> roleGrantService.revokeScreen(role.getRolePk(),
            usersView.getScreen().getScreenRegPk()), SecErrorCodes.SEC_404_GRANT);
        assertNotFound(() -> roleGrantService.revokeAction(role.getRolePk(),
            rolesCreate.getActionRegPk()), SecErrorCodes.SEC_404_GRANT);
        assertNotFound(() -> roleGrantService.revokeAction(role.getRolePk(), Long.MAX_VALUE),
            SecErrorCodes.SEC_404_GRANT);
        assertThat(heldPermissionCodes(role)).containsExactly(SecPermissions.PERM_SEC_ROLES_VIEW);
    }

    @Test
    void anotherTenantsRole_isSec404Role_andKeepsItsGrants() {
        long otherTenant = jdbcTemplate.queryForObject("select nextval('SEQ_CORE_TENANT')", Long.class);
        jdbcTemplate.update("insert into core_tenant (id, code, name_ar, name_en, status_code, created_by, created_at)"
            + " values (?, ?, 'مستأجر', 'Tenant', 'ACTIVE', 'test', now())", otherTenant, "RV" + unique());
        long otherRole = jdbcTemplate.queryForObject("insert into sec_role (role_pk, tenant_id, code, name_ar, name_en,"
            + " is_active_fl, is_super, created_by, created_at) values (nextval('SEQ_SEC_ROLE'), ?, 'RV_ROLE', 'دور',"
            + " 'Role', true, false, 'test', now()) returning role_pk", Long.class, otherTenant);
        jdbcTemplate.update("insert into sec_role_action_grant (role_action_grant_pk, tenant_id, role_id, action_id,"
            + " granted_by, granted_at) values (nextval('SEQ_SEC_ROLE_ACTION_GRANT'), ?, ?, ?, 'test', now())",
            otherTenant, otherRole, rolesView.getActionRegPk());

        assertNotFound(() -> roleGrantService.revokeScreen(otherRole, rolesView.getScreen().getScreenRegPk()),
            SecErrorCodes.SEC_404_ROLE);
        assertNotFound(() -> roleGrantService.revokeAction(otherRole, rolesView.getActionRegPk()),
            SecErrorCodes.SEC_404_ROLE);
        assertThat(jdbcTemplate.queryForObject("select count(*) from sec_role_action_grant where role_id = ?",
            Integer.class, otherRole)).isEqualTo(1);
    }

    @Test
    void moduleRevoke_stillCascadesEveryScreenAndAction() {
        Role role = roleHolding(false, rolesView, rolesCreate, usersView);
        Long moduleId = rolesView.getScreen().getModule().getModuleRegPk();

        var counts = roleGrantService.revokeModule(role.getRolePk(), moduleId).getData();

        assertThat(counts.getRevokedScreenGrants()).isEqualTo(2);
        assertThat(counts.getRevokedActionGrants()).isEqualTo(3);
        assertThat(heldPermissionCodes(role)).isEmpty();
        assertThat(auditRows(role, "MODULE_REVOKED")).hasSize(1);
    }

    @Test
    void revoke_takesEffectOnTheNextAuthorityRead_butASuperRoleKeepsEveryAuthority() {
        Role plain = roleHolding(false, rolesView, rolesCreate);
        Role superRole = roleHolding(true, rolesView, rolesCreate);
        String plainUser = userHolding(plain);
        String superUser = userHolding(superRole);

        roleGrantService.revokeAction(plain.getRolePk(), rolesView.getActionRegPk());
        roleGrantService.revokeAction(superRole.getRolePk(), rolesView.getActionRegPk());

        assertThat(authoritiesOf(plainUser)).doesNotContain(
            SecPermissions.PERM_SEC_ROLES_VIEW, SecPermissions.PERM_SEC_ROLES_CREATE);
        assertThat(authoritiesOf(superUser)).contains(
            SecPermissions.PERM_SEC_ROLES_VIEW, SecPermissions.PERM_SEC_ROLES_CREATE);
    }

    private Role roleHolding(boolean isSuper, ActionRegistry... actions) {
        Role role = roleRepository.save(Role.builder()
            .code("RV_" + unique()).nameAr("دور سحب").nameEn("Revoke role").isSuper(isSuper).build());
        Stream.of(actions).map(a -> a.getScreen().getModule()).distinct().forEach(module ->
            roleModuleGrantRepository.save(RoleModuleGrant.builder()
                .role(role).module(module).grantedBy(EDITOR).build()));
        Stream.of(actions).map(ActionRegistry::getScreen).distinct().forEach(screen ->
            roleScreenGrantRepository.save(RoleScreenGrant.builder()
                .role(role).screen(screen).grantedBy(EDITOR).build()));
        Stream.of(actions).forEach(action ->
            roleActionGrantRepository.save(RoleActionGrant.builder()
                .role(role).action(action).grantedBy(EDITOR).build()));
        roleActionGrantRepository.flush();
        return role;
    }

    private String userHolding(Role role) {
        String username = "rv-" + unique().toLowerCase();
        User user = userRepository.save(User.builder()
            .username(username).email(username + "@revoke.test").passwordHash("not-a-real-hash")
            .fullNameAr("مستخدم").fullNameEn("User").statusCode("ACTIVE").realm(User.REALM_STAFF).build());
        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
            .user(user).role(role).assignedBy(EDITOR).build());
        userRoleAssignmentRepository.flush();
        return username;
    }

    private Set<String> authoritiesOf(String username) {
        setPrincipal(username);
        try {
            return menuService.effectiveAuthorityCodes().getData();
        } finally {
            setPrincipal(EDITOR, SecPermissions.PERM_SEC_ROLES_UPDATE);
        }
    }

    private List<String> heldPermissionCodes(Role role) {
        return roleActionGrantRepository.findAllByRoleWithAction(role.getRolePk()).stream()
            .map(grant -> grant.getAction().getPermissionCode())
            .toList();
    }

    private List<AuditLogEntry> auditRows(Role role, String eventTypeCode) {
        String prefix = role.getRolePk() + "/";
        return auditLogEntryRepository.findAll().stream()
            .filter(row -> eventTypeCode.equals(row.getEventTypeCode()))
            .filter(row -> row.getTargetRef() != null && row.getTargetRef().startsWith(prefix))
            .toList();
    }

    private ActionRegistry action(String permissionCode) {
        return actionRegistryRepository.findByPermissionCode(permissionCode).orElseThrow();
    }

    private static void assertNotFound(Runnable call, String errorCode) {
        assertThatThrownBy(call::run)
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
                assertThat(e.getErrorCode()).isEqualTo(errorCode);
            });
    }

    private static void setPrincipal(String username, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            username, "N/A", Stream.of(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private static String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
