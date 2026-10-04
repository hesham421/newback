package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.LocalizedException;
import com.erp.main.ErpMainApplication;
import com.erp.sec.dto.DevPasswordResetTokenResponse;
import com.erp.sec.dto.PasswordResetCompleteRequest;
import com.erp.sec.dto.RoleResponse;
import com.erp.sec.dto.UserResponse;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.permission.PermissionConstants;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.service.DevPasswordResetSupportService;
import com.erp.sec.service.PasswordResetService;
import com.erp.sec.service.RoleService;
import com.erp.sec.service.UserRoleService;
import com.erp.sec.service.UserService;
import com.erp.sec.dto.UserRoleAssignmentRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers the two backend asks of the frontend E2E second pass of 2026-09-18: the read-one endpoints
 * behind {@code GET /roles/{id}} and {@code GET /users/{id}} (which answered 405, so a deep-linked
 * drawer could not resolve its row on a cold load), and the dev-profile fixture that finally makes
 * TC-SEC-038 — completing a reset with a VALID token — reachable by an automated run.
 *
 * <p>Same posture as {@link SecFrontendGapIntegrationTest}: real dev Postgres/Redis, {@code dev}
 * profile, every write rolled back by the class-level {@link Transactional}.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class SecReadOneIntegrationTest {

    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private DevPasswordResetSupportService devPasswordResetSupportService;
    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void roleGetById_returnsTheSameRowTheSearchWouldReturn() {
        Role role = persistRole();
        setAuthenticatedPrincipal("role-reader", PermissionConstants.PERM_SEC_ROLES_VIEW);

        RoleResponse response = roleService.getById(role.getRolePk()).getData();

        assertThat(response.getRolePk()).isEqualTo(role.getRolePk());
        assertThat(response.getCode()).isEqualTo(role.getCode());
        assertThat(response.getNameEn()).isEqualTo(role.getNameEn());
    }

    @Test
    void roleGetById_unknownId_raisesSec404Role() {
        setAuthenticatedPrincipal("role-reader", PermissionConstants.PERM_SEC_ROLES_VIEW);

        assertThatThrownBy(() -> roleService.getById(-1L))
            .isInstanceOf(LocalizedException.class)
            .hasMessageContaining(SecErrorCodes.SEC_404_ROLE);
    }

    /** The drawer needs the roles array the search learned to carry in round 1, not a bare user. */
    @Test
    void userGetById_carriesTheAssignedRoles() {
        User user = persistUser("readone");
        Role role = persistRole();
        setAuthenticatedPrincipal("user-admin",
            PermissionConstants.PERM_SEC_USERS_VIEW, PermissionConstants.PERM_SEC_USERS_UPDATE);
        userRoleService.assign(user.getUserPk(),
            UserRoleAssignmentRequest.builder().roleIds(List.of(role.getRolePk())).build());

        UserResponse response = userService.getById(user.getUserPk()).getData();

        assertThat(response.getUserPk()).isEqualTo(user.getUserPk());
        assertThat(response.getUsername()).isEqualTo(user.getUsername());
        assertThat(response.getRoles()).extracting("roleId").containsExactly(role.getRolePk());
    }

    @Test
    void userGetById_unknownId_raisesSec404User() {
        setAuthenticatedPrincipal("user-admin", PermissionConstants.PERM_SEC_USERS_VIEW);

        assertThatThrownBy(() -> userService.getById(-1L))
            .isInstanceOf(LocalizedException.class)
            .hasMessageContaining(SecErrorCodes.SEC_404_USER);
    }

    /** TC-SEC-038's missing half: a token the run can actually read, then spend at API-SEC-004. */
    @Test
    void devIssuedToken_completesARealPasswordReset() {
        User user = persistUser("reset");
        String previousHash = user.getPasswordHash();
        setAuthenticatedPrincipal("dev-harness");

        DevPasswordResetTokenResponse issued =
            devPasswordResetSupportService.issueToken(user.getEmail()).getData();
        assertThat(issued.getToken()).isNotBlank();
        assertThat(issued.getExpiresAt()).isNotNull();

        passwordResetService.complete(PasswordResetCompleteRequest.builder()
            .token(issued.getToken())
            .newPassword("N3wP@ssw0rd!")
            .build());

        User reloaded = userRepository.findById(user.getUserPk()).orElseThrow();
        assertThat(reloaded.getPasswordHash()).isNotEqualTo(previousHash);
        assertThat(passwordEncoder.matches("N3wP@ssw0rd!", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void devIssueToken_unknownEmail_raisesSec404User() {
        setAuthenticatedPrincipal("dev-harness");

        assertThatThrownBy(() -> devPasswordResetSupportService.issueToken("nobody@example.com"))
            .isInstanceOf(LocalizedException.class)
            .hasMessageContaining(SecErrorCodes.SEC_404_USER);
    }

    private Role persistRole() {
        String unique = uniqueSuffix().toUpperCase();
        return roleRepository.save(Role.builder()
            .code("READ1_" + unique)
            .nameAr("دور اختبار")
            .nameEn("Read-one test role")
            .build());
    }

    private User persistUser(String tag) {
        String unique = uniqueSuffix();
        return userRepository.save(User.builder()
            .username("r1-" + unique)
            .email("r1-" + unique + "@example.com")
            .passwordHash(passwordEncoder.encode("Def@ultPass1"))
            .fullNameAr("مستخدم اختبار " + tag)
            .fullNameEn("Read-one test user " + tag)
            .statusCode(User.STATUS_ACTIVE)
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
