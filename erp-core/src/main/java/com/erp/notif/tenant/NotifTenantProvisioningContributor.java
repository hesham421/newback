package com.erp.notif.tenant;

import com.erp.tenant.TenantProvisioning;
import com.erp.tenant.TenantProvisioningContributor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * NOTIF's part of tenant provisioning (erp-core step 05): a new tenant starts with the source
 * (PLATFORM) tenant's channel configurations and templates, so e.g. its password-reset e-mail works.
 * Two things are deliberately not copied: a channel's {@code CONFIG_JSON} (tenant-specific settings,
 * possibly credentials — the tenant configures its own) and a template's {@code ATTACHMENT_FILE_ID}
 * (a file of the source tenant, invisible to the new one).
 *
 * <p>Explicit SQL by design (see {@link TenantProvisioningContributor}): every statement names
 * {@code TENANT_ID} — the new tenant on inserts, the source tenant on reads.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotifTenantProvisioningContributor implements TenantProvisioningContributor {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void provision(TenantProvisioning p) {
        int channels = jdbcTemplate.update(
            "INSERT INTO NOTIF_CHANNEL_CONFIG (ID, TENANT_ID, CHANNEL_TYPE_ID, IS_ENABLED_FL, CONFIG_JSON,"
                + " CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_NOTIF_CHANNEL_CONFIG'), ?, c.CHANNEL_TYPE_ID, c.IS_ENABLED_FL, NULL, ?, now()"
                + " FROM NOTIF_CHANNEL_CONFIG c WHERE c.TENANT_ID = ?"
                + " ORDER BY c.ID",
            p.tenantId(), p.provisionedBy(), p.sourceTenantId());

        int templates = jdbcTemplate.update(
            "INSERT INTO NOTIF_TEMPLATE (ID, TENANT_ID, TEMPLATE_CODE, NAME_AR, NAME_EN, SUBJECT_AR, SUBJECT_EN,"
                + " BODY_AR, BODY_EN, ATTACHMENT_FILE_ID, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_NOTIF_TEMPLATE'), ?, t.TEMPLATE_CODE, t.NAME_AR, t.NAME_EN, t.SUBJECT_AR,"
                + " t.SUBJECT_EN, t.BODY_AR, t.BODY_EN, NULL, t.IS_ACTIVE_FL, ?, now()"
                + " FROM NOTIF_TEMPLATE t WHERE t.TENANT_ID = ?"
                + " ORDER BY t.ID",
            p.tenantId(), p.provisionedBy(), p.sourceTenantId());

        log.info("Tenant {} provisioned by NOTIF: {} channel configurations, {} templates",
            p.tenantCode(), channels, templates);
    }
}
