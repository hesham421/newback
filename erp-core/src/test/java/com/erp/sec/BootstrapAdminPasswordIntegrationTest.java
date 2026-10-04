package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.common.domain.status.ServiceResult;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.domain.UserDomain;
import com.erp.sec.dto.LoginRequest;
import com.erp.sec.dto.LoginResponse;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.security.BootstrapAdminPasswordRunner;
import com.erp.sec.service.AuthService;
import com.erp.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * erp-core step 04 — the seeded bootstrap {@code admin} account ships without a usable password,
 * and {@link BootstrapAdminPasswordRunner} gives it one from
 * {@code erp.core.security.bootstrap-admin-password} exactly once.
 *
 * <p>The test profile does not set the property, so the Spring-managed runner left the seeded row
 * untouched when the context started; the cases below drive their own runner instance with the
 * property set. Class-level {@link Transactional} rolls every change back, so the shared database
 * keeps the seed state for the other test classes.
 */
@Transactional
class BootstrapAdminPasswordIntegrationTest extends AbstractIntegrationTest {

    private static final String CALLER_IP = "203.0.113.9";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;
    @Autowired
    private BootstrapAdminPasswordRunner springManagedRunner;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seededAdmin_isPendingWithThePlaceholderHash_holdsSysAdmin_andCannotLogIn() {
        assertThat(springManagedRunner).isNotNull();
        User admin = admin();

        assertThat(admin.getStatusCode()).isEqualTo("PENDING");
        assertThat(admin.getPasswordHash()).isEqualTo(UserDomain.BOOTSTRAP_PASSWORD_PLACEHOLDER);
        assertThat(UserDomain.awaitsBootstrapPassword(admin)).isTrue();
        assertThat(jdbcTemplate.queryForList(
            "select r.code from sec_user_role ur join sec_role r on r.role_pk = ur.role_id "
                + "join sec_user u on u.user_pk = ur.user_id where u.username = 'admin'", String.class))
            .containsExactly("SYS_ADMIN");

        for (String attempt : new String[] {"admin", UserDomain.BOOTSTRAP_PASSWORD_PLACEHOLDER}) {
            assertThatThrownBy(() -> authService.login(login(attempt), CALLER_IP))
                .isInstanceOf(LocalizedException.class)
                .extracting(e -> ((LocalizedException) e).getErrorCode())
                .isEqualTo(SecErrorCodes.SEC_401_INVALID_CREDENTIALS);
        }
    }

    @Test
    void runnerWithThePropertySet_setsTheHashActivatesTheAccount_andAdminLogsIn() {
        runnerWith("Bootstr@p-Secret-1").run(null);

        User admin = admin();
        assertThat(admin.getStatusCode()).isEqualTo("ACTIVE");
        assertThat(admin.getIsActiveFl()).isTrue();
        assertThat(passwordEncoder.matches("Bootstr@p-Secret-1", admin.getPasswordHash())).isTrue();
        assertThat(UserDomain.awaitsBootstrapPassword(admin)).isFalse();

        ServiceResult<LoginResponse> result = authService.login(login("Bootstr@p-Secret-1"), CALLER_IP);
        assertThat(result.getData().getAccessToken()).isNotBlank();
    }

    @Test
    void runnerIgnoresThePropertyOnceTheAccountIsInitialised() {
        runnerWith("First-Passw0rd!").run(null);
        String hashAfterFirstRun = admin().getPasswordHash();

        runnerWith("Second-Passw0rd!").run(null);

        User admin = admin();
        assertThat(admin.getPasswordHash()).isEqualTo(hashAfterFirstRun);
        assertThat(passwordEncoder.matches("First-Passw0rd!", admin.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("Second-Passw0rd!", admin.getPasswordHash())).isFalse();
    }

    @Test
    void runnerWithoutTheProperty_orWithABlankOne_leavesTheSeededAccountUntouched() {
        runnerWith(null).run(null);
        runnerWith("   ").run(null);

        User admin = admin();
        assertThat(admin.getStatusCode()).isEqualTo("PENDING");
        assertThat(admin.getPasswordHash()).isEqualTo(UserDomain.BOOTSTRAP_PASSWORD_PLACEHOLDER);
    }

    private BootstrapAdminPasswordRunner runnerWith(String bootstrapPassword) {
        ErpCoreProperties properties = new ErpCoreProperties();
        properties.getSecurity().setBootstrapAdminPassword(bootstrapPassword);
        return new BootstrapAdminPasswordRunner(userRepository, passwordEncoder, properties);
    }

    private User admin() {
        return userRepository.findByUsername(BootstrapAdminPasswordRunner.BOOTSTRAP_ADMIN_USERNAME).orElseThrow();
    }

    private static LoginRequest login(String password) {
        return LoginRequest.builder()
            .username(BootstrapAdminPasswordRunner.BOOTSTRAP_ADMIN_USERNAME)
            .password(password)
            .build();
    }
}
