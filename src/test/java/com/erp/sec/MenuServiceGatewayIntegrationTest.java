package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.main.ErpMainApplication;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.RoleActionGrant;
import com.erp.sec.entity.ScreenRegistry;
import com.erp.sec.entity.User;
import com.erp.sec.entity.UserRoleAssignment;
import com.erp.sec.repository.ActionRegistryRepository;
import com.erp.sec.repository.ModuleRegistryRepository;
import com.erp.sec.repository.RoleActionGrantRepository;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.ScreenRegistryRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.repository.UserRoleAssignmentRepository;
import com.erp.sec.service.MenuService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * RULE-SEC-007's gateway for a MULTI-WORD action code — the authority set REQ-SEC-033's
 * {@code JwtAuthenticationFilter} installs on every authenticated request.
 *
 * <p>The registry's shape is {@code PERM_<PAGE_CODE>_<ACTION_CODE>}, and nothing forbids an action
 * code from containing an underscore of its own: {@code FIN_PERIODS} / {@code CLOSE_APPROVE} yields
 * {@code PERM_FIN_PERIODS_CLOSE_APPROVE} (V24__fin_security_seed.sql), whose screen is
 * {@code FIN_PERIODS} and whose gateway is therefore {@code PERM_FIN_PERIODS_VIEW}. Deriving the
 * screen by splitting the code at its LAST underscore instead yields a screen that does not exist,
 * so the gateway can never be satisfied and the authority is silently stripped for every role. The
 * fixture below is built from registry rows rather than the seeded FIN ones so it stays independent
 * of any one module's seed, but it reproduces exactly that shape.
 *
 * <p>Runs against the real dev Postgres the same way {@link SecCoverageIntegrationTest} does; the
 * class-level {@link Transactional} rolls every fixture row back on completion.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class MenuServiceGatewayIntegrationTest {

    private static final String GATEWAY_ACTION_CODE = "VIEW";
    private static final String MULTI_WORD_ACTION_CODE = "CLOSE_APPROVE";

    @Autowired
    private MenuService menuService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Autowired
    private ModuleRegistryRepository moduleRegistryRepository;
    @Autowired
    private ScreenRegistryRepository screenRegistryRepository;
    @Autowired
    private ActionRegistryRepository actionRegistryRepository;
    @Autowired
    private RoleActionGrantRepository roleActionGrantRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void effectiveAuthorityCodes_retainsMultiWordAction_whenItsScreenViewIsHeld() {
        ScreenRegistry screen = persistScreen();
        ActionRegistry view = persistAction(screen, GATEWAY_ACTION_CODE);
        ActionRegistry closeApprove = persistAction(screen, MULTI_WORD_ACTION_CODE);

        // The code the old, text-derived rule would have demanded: PERM_<page>_CLOSE_VIEW. It is a
        // registry row NOWHERE — asserting its absence is the point, not an oversight in the fixture.
        String screenViewCode = view.getPermissionCode();
        String misderivedGatewayCode = closeApprove.getPermissionCode()
            .substring(0, closeApprove.getPermissionCode().lastIndexOf('_') + 1)
            + GATEWAY_ACTION_CODE;
        assertThat(actionRegistryRepository.findAll().stream()
            .map(ActionRegistry::getPermissionCode))
            .as("the screen derived by splitting at the last underscore must not exist")
            .doesNotContain(misderivedGatewayCode);

        User user = persistUserWithRoleHolding("gwheld", view, closeApprove);
        authenticateAs(user);

        Set<String> authorities = menuService.effectiveAuthorityCodes().getData();

        assertThat(authorities)
            .as("a multi-word action resolves to screen %s, whose VIEW (%s) IS held",
                screen.getPageCode(), screenViewCode)
            .contains(closeApprove.getPermissionCode(), screenViewCode);
    }

    @Test
    void effectiveAuthorityCodes_stripsMultiWordAction_whenItsScreenViewIsAbsent() {
        ScreenRegistry screen = persistScreen();
        persistAction(screen, GATEWAY_ACTION_CODE);
        ActionRegistry closeApprove = persistAction(screen, MULTI_WORD_ACTION_CODE);

        // The gateway row exists on the screen, but this role does not hold it.
        User user = persistUserWithRoleHolding("gwmissing", closeApprove);
        authenticateAs(user);

        Set<String> authorities = menuService.effectiveAuthorityCodes().getData();

        assertThat(authorities)
            .as("RULE-SEC-007 still strips a non-VIEW permission whose screen VIEW is not held")
            .doesNotContain(closeApprove.getPermissionCode());
    }

    // -----------------------------------------------------------------------------------------
    // Fixture helpers — same construction style as SecCoverageIntegrationTest's TC-SEC-035
    // -----------------------------------------------------------------------------------------

    private ScreenRegistry persistScreen() {
        ModuleRegistry module = moduleRegistryRepository.save(ModuleRegistry.builder()
            .code("G" + uniqueSuffix().substring(0, 6))
            .nameAr("وحدة اختبار البوابة")
            .nameEn("Gateway test module")
            .build());
        return screenRegistryRepository.save(ScreenRegistry.builder()
            .pageCode("GPERIODS" + uniqueSuffix())
            .module(module)
            .nameAr("شاشة الفترات")
            .nameEn("Gateway test periods screen")
            .build());
    }

    /** Registers one action on the screen with the registry's own PERM_&lt;PAGE&gt;_&lt;ACTION&gt; shape. */
    private ActionRegistry persistAction(ScreenRegistry screen, String actionCode) {
        return actionRegistryRepository.save(ActionRegistry.builder()
            .permissionCode("PERM_" + screen.getPageCode() + "_" + actionCode)
            .screen(screen)
            .actionCode(actionCode)
            .nameAr("إجراء " + actionCode)
            .nameEn(screen.getNameEn() + " - " + actionCode)
            .build());
    }

    private User persistUserWithRoleHolding(String tag, ActionRegistry... actions) {
        User user = userRepository.save(User.builder()
            .username("gw-" + tag + "-" + uniqueSuffix())
            .email("gw-" + tag + "-" + uniqueSuffix() + "@example.com")
            .passwordHash(passwordEncoder.encode("Def@ultPass1"))
            .fullNameAr("مستخدم اختبار البوابة")
            .fullNameEn("Gateway test user " + tag)
            .statusCode(User.STATUS_ACTIVE)
            .build());
        Role role = roleRepository.save(Role.builder()
            .code("GROLE" + uniqueSuffix())
            .nameAr("دور اختبار البوابة")
            .nameEn("Gateway test role")
            .build());
        for (ActionRegistry action : actions) {
            roleActionGrantRepository.save(RoleActionGrant.builder()
                .role(role)
                .action(action)
                .grantedBy("MenuServiceGatewayIntegrationTest")
                .build());
        }
        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
            .user(user)
            .role(role)
            .assignedBy("MenuServiceGatewayIntegrationTest")
            .build());
        return user;
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user.getUsername(), "N/A", List.of()));
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
