package com.erp.sec.security;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.sec.domain.UserDomain;
import com.erp.sec.entity.User;
import com.erp.sec.repository.UserRepository;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Gives the seeded bootstrap {@code admin} account its password on the first start.
 *
 * <p>The core seed ({@code V7__sec_seed.sql}) creates {@code admin} (holding {@code SYS_ADMIN})
 * with status {@code PENDING} and a placeholder instead of a password hash, so the account cannot
 * log in and no well-known credential ships with the library. When
 * {@code erp.core.security.bootstrap-admin-password} is set, this runner BCrypt-hashes it into the
 * account and activates it — but only while the account still awaits its bootstrap password
 * ({@link UserDomain#awaitsBootstrapPassword(User)}). After that the property is ignored, so a
 * password changed later (e.g. through a password reset) is never overwritten by a restart.
 *
 * <p>This is startup infrastructure, not a request path: it runs with no principal, so it uses
 * the repository directly (as {@link JwtAuthenticationFilter} does) instead of a
 * {@code @PreAuthorize}-gated service method. The password is never logged.
 *
 * <p>Tenant (erp-core step 05): the bootstrap admin belongs to the PLATFORM tenant, and a runner has
 * no request tenant, so the whole run executes inside {@code TenantContext.runAs(PLATFORM_TENANT_ID)}.
 * The method is deliberately not {@code @Transactional}: a transaction opened before {@code runAs}
 * would bind its Hibernate session to no tenant. The lookup and the save are each their own
 * repository transaction (the save merges the loaded row, version-checked); at startup nothing races it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdminPasswordRunner implements ApplicationRunner {

    /** USERNAME of the account the core seed creates. */
    public static final String BOOTSTRAP_ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ErpCoreProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        TenantContext.runAs(TenantConstants.PLATFORM_TENANT_ID, this::initialiseBootstrapAdmin);
    }

    private void initialiseBootstrapAdmin() {
        String password = properties.getSecurity().getBootstrapAdminPassword();
        if (!StringUtils.hasText(password)) {
            log.debug("erp.core.security.bootstrap-admin-password is not set; bootstrap admin left as it is");
            return;
        }

        User admin = userRepository.findByUsername(BOOTSTRAP_ADMIN_USERNAME).orElse(null);
        if (admin == null) {
            log.warn("erp.core.security.bootstrap-admin-password is set but no '{}' account exists; ignored",
                BOOTSTRAP_ADMIN_USERNAME);
            return;
        }
        if (!UserDomain.awaitsBootstrapPassword(admin)) {
            log.info("Bootstrap admin '{}' is already initialised; erp.core.security.bootstrap-admin-password ignored",
                BOOTSTRAP_ADMIN_USERNAME);
            return;
        }

        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.activate();
        userRepository.save(admin);
        log.info("Bootstrap admin '{}' initialised from erp.core.security.bootstrap-admin-password (User ID: {})",
            BOOTSTRAP_ADMIN_USERNAME, admin.getUserPk());
    }
}
