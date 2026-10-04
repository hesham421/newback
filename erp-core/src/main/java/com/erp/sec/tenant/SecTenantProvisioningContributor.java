package com.erp.sec.tenant;

import com.erp.sec.entity.User;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.TenantProvisioning;
import com.erp.tenant.TenantProvisioningContributor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * SEC's part of tenant provisioning (erp-core step 05): gives a new tenant its role catalog and its
 * first administrator, so the tenant can log in and manage itself.
 * <ol>
 *   <li>Copies the catalog roles (with their {@code IS_SUPER} flag, erp-core step 06) ({@link #CATALOG_ROLE_CODES}) of the source (PLATFORM) tenant.</li>
 *   <li>Copies their three grant tiers, <b>except</b> everything under the {@code PLATFORM} module:
 *       {@code PLATFORM_TENANT_MANAGE} exists only in the PLATFORM tenant, so no tenant administrator
 *       can provision tenants.</li>
 *   <li>Creates the administrator account ({@code ACTIVE}, BCrypt-hashed password) holding the new
 *       tenant's {@code SYS_ADMIN}.</li>
 * </ol>
 * Explicit SQL by design (see {@link TenantProvisioningContributor}): the calling transaction's
 * Hibernate session belongs to the PLATFORM tenant, so tenant-aware entities cannot write the new
 * tenant's rows. Every statement names {@code TENANT_ID}: the new tenant on inserts, the source
 * tenant on reads. The permission catalog ({@code SEC_*_REG}) is global and is joined, never copied.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecTenantProvisioningContributor implements TenantProvisioningContributor {

    /** The roles every tenant starts with: the bootstrap role and the per-module admin roles (V7). */
    public static final List<String> CATALOG_ROLE_CODES = List.of("SYS_ADMIN", "CU_ADMIN", "NOTIF_ADMIN", "FILE_ADMIN");

    /** The role the first administrator holds. */
    public static final String ADMIN_ROLE_CODE = "SYS_ADMIN";

    /** Registry module whose grants stay in the PLATFORM tenant (V10). */
    public static final String PLATFORM_MODULE_CODE = "PLATFORM";

    /**
     * The platform-only authority (owned by the tenant module, {@code TenantPermissions}); named here as
     * a literal because SEC must not depend on tenant internals. Redundant with the module filter, kept
     * as defence in depth.
     */
    private static final String PLATFORM_TENANT_MANAGE = "PLATFORM_TENANT_MANAGE";

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    /** First: the administrator and roles depend on nothing else being provisioned. */
    @Override
    public int order() {
        return 0;
    }

    @Override
    public void provision(TenantProvisioning p) {
        Long target = p.tenantId();
        Long source = p.sourceTenantId();
        String by = p.provisionedBy();

        String rolePlaceholders = String.join(",", Collections.nCopies(CATALOG_ROLE_CODES.size(), "?"));
        List<Object> roleArgs = new ArrayList<>(List.of(target, by, source));
        roleArgs.addAll(CATALOG_ROLE_CODES);
        int roles = jdbcTemplate.update(
            "INSERT INTO SEC_ROLE (ROLE_PK, TENANT_ID, CODE, NAME_AR, NAME_EN, DESCRIPTION_AR, DESCRIPTION_EN,"
                + " IS_ACTIVE_FL, IS_SUPER, CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_SEC_ROLE'), ?, r.CODE, r.NAME_AR, r.NAME_EN, r.DESCRIPTION_AR, r.DESCRIPTION_EN,"
                + " r.IS_ACTIVE_FL, r.IS_SUPER, ?, now()"
                + " FROM SEC_ROLE r WHERE r.TENANT_ID = ? AND r.CODE IN (" + rolePlaceholders + ")"
                + " ORDER BY r.ROLE_PK",
            roleArgs.toArray());

        int moduleGrants = jdbcTemplate.update(
            "INSERT INTO SEC_ROLE_MODULE_GRANT (ROLE_MODULE_GRANT_PK, TENANT_ID, ROLE_ID, MODULE_ID, GRANTED_BY,"
                + " GRANTED_AT, CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_SEC_ROLE_MODULE_GRANT'), ?, tr.ROLE_PK, g.MODULE_ID, ?, now(), ?, now()"
                + " FROM SEC_ROLE_MODULE_GRANT g"
                + " JOIN SEC_ROLE sr ON sr.ROLE_PK = g.ROLE_ID AND sr.TENANT_ID = ?"
                + " JOIN SEC_ROLE tr ON tr.CODE = sr.CODE AND tr.TENANT_ID = ?"
                + " JOIN SEC_MODULE_REG m ON m.MODULE_REG_PK = g.MODULE_ID"
                + " WHERE g.TENANT_ID = ? AND m.CODE <> ?"
                + " ORDER BY g.ROLE_MODULE_GRANT_PK",
            target, by, by, source, target, source, PLATFORM_MODULE_CODE);

        int screenGrants = jdbcTemplate.update(
            "INSERT INTO SEC_ROLE_SCREEN_GRANT (ROLE_SCREEN_GRANT_PK, TENANT_ID, ROLE_ID, SCREEN_ID, GRANTED_BY,"
                + " GRANTED_AT, CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_SEC_ROLE_SCREEN_GRANT'), ?, tr.ROLE_PK, g.SCREEN_ID, ?, now(), ?, now()"
                + " FROM SEC_ROLE_SCREEN_GRANT g"
                + " JOIN SEC_ROLE sr ON sr.ROLE_PK = g.ROLE_ID AND sr.TENANT_ID = ?"
                + " JOIN SEC_ROLE tr ON tr.CODE = sr.CODE AND tr.TENANT_ID = ?"
                + " JOIN SEC_SCREEN_REG s ON s.SCREEN_REG_PK = g.SCREEN_ID"
                + " JOIN SEC_MODULE_REG m ON m.MODULE_REG_PK = s.MODULE_ID"
                + " WHERE g.TENANT_ID = ? AND m.CODE <> ?"
                + " ORDER BY g.ROLE_SCREEN_GRANT_PK",
            target, by, by, source, target, source, PLATFORM_MODULE_CODE);

        int actionGrants = jdbcTemplate.update(
            "INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, TENANT_ID, ROLE_ID, ACTION_ID, GRANTED_BY,"
                + " GRANTED_AT, CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'), ?, tr.ROLE_PK, g.ACTION_ID, ?, now(), ?, now()"
                + " FROM SEC_ROLE_ACTION_GRANT g"
                + " JOIN SEC_ROLE sr ON sr.ROLE_PK = g.ROLE_ID AND sr.TENANT_ID = ?"
                + " JOIN SEC_ROLE tr ON tr.CODE = sr.CODE AND tr.TENANT_ID = ?"
                + " JOIN SEC_ACTION_REG a ON a.ACTION_REG_PK = g.ACTION_ID"
                + " JOIN SEC_SCREEN_REG s ON s.SCREEN_REG_PK = a.SCREEN_ID"
                + " JOIN SEC_MODULE_REG m ON m.MODULE_REG_PK = s.MODULE_ID"
                + " WHERE g.TENANT_ID = ? AND m.CODE <> ? AND a.PERMISSION_CODE <> ?"
                + " ORDER BY g.ROLE_ACTION_GRANT_PK",
            target, by, by, source, target, source, PLATFORM_MODULE_CODE, PLATFORM_TENANT_MANAGE);

        TenantProvisioning.Administrator admin = p.admin();
        jdbcTemplate.update(
            "INSERT INTO SEC_USER (USER_PK, TENANT_ID, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR, FULL_NAME_EN,"
                + " STATUS_CODE, REALM, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " VALUES (nextval('SEQ_SEC_USER'), ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, now())",
            target, admin.username(), admin.email(), passwordEncoder.encode(admin.rawPassword()),
            admin.fullNameAr(), admin.fullNameEn(), STATUS_ACTIVE, User.REALM_STAFF, by);

        int adminRoles = jdbcTemplate.update(
            "INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, TENANT_ID, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT,"
                + " CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_SEC_USER_ROLE'), ?, u.USER_PK, r.ROLE_PK, ?, now(), ?, now()"
                + " FROM SEC_USER u JOIN SEC_ROLE r ON r.TENANT_ID = u.TENANT_ID AND r.CODE = ?"
                + " WHERE u.TENANT_ID = ? AND u.REALM = 'STAFF' AND u.USERNAME = ?",
            target, by, by, ADMIN_ROLE_CODE, target, admin.username());
        if (adminRoles != 1) {
            // The source tenant has no SYS_ADMIN to copy: the tenant would have no administrator.
            // Fail the whole provisioning (same transaction) rather than create an unmanageable tenant.
            log.error("Tenant {} provisioning: source tenant {} has no {} role; aborting",
                p.tenantCode(), source, ADMIN_ROLE_CODE);
            throw new LocalizedException(Status.INTERNAL_ERROR, CommonErrorCodes.INTERNAL_ERROR);
        }

        log.info("Tenant {} provisioned by SEC: {} roles, {}/{}/{} module/screen/action grants, administrator '{}'",
            p.tenantCode(), roles, moduleGrants, screenGrants, actionGrants, admin.username());
    }
}
