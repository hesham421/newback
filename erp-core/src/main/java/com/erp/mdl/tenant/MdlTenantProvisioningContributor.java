package com.erp.mdl.tenant;

import com.erp.tenant.TenantProvisioning;
import com.erp.tenant.TenantProvisioningContributor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * MDL's part of tenant provisioning (erp-core step 05): a new tenant starts with a copy of the
 * source (PLATFORM) tenant's lookup catalog — types and their values — because other modules read
 * their code lists from it (e.g. FILE's file statuses, NOTIF's channels and statuses).
 *
 * <p>Explicit SQL by design (see {@link TenantProvisioningContributor}): every statement names
 * {@code TENANT_ID} — the new tenant on inserts, the source tenant on reads; values are re-linked to
 * the copied types by their natural key.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MdlTenantProvisioningContributor implements TenantProvisioningContributor {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void provision(TenantProvisioning p) {
        int types = jdbcTemplate.update(
            "INSERT INTO MDL_LOOKUP_TYPE (LOOKUP_TYPE_PK, TENANT_ID, KEY, OWNER_MODULE_CODE, NAME_AR, NAME_EN,"
                + " IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_MDL_LOOKUP_TYPE'), ?, t.KEY, t.OWNER_MODULE_CODE, t.NAME_AR, t.NAME_EN,"
                + " t.IS_ACTIVE_FL, ?, now()"
                + " FROM MDL_LOOKUP_TYPE t WHERE t.TENANT_ID = ?"
                + " ORDER BY t.LOOKUP_TYPE_PK",
            p.tenantId(), p.provisionedBy(), p.sourceTenantId());

        int values = jdbcTemplate.update(
            "INSERT INTO MDL_LOOKUP_VALUE (LOOKUP_VALUE_PK, TENANT_ID, LOOKUP_TYPE_ID, CODE, NAME_AR, NAME_EN,"
                + " SORT_ORDER, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_MDL_LOOKUP_VALUE'), ?, tt.LOOKUP_TYPE_PK, v.CODE, v.NAME_AR, v.NAME_EN,"
                + " v.SORT_ORDER, v.IS_ACTIVE_FL, ?, now()"
                + " FROM MDL_LOOKUP_VALUE v"
                + " JOIN MDL_LOOKUP_TYPE st ON st.LOOKUP_TYPE_PK = v.LOOKUP_TYPE_ID AND st.TENANT_ID = ?"
                + " JOIN MDL_LOOKUP_TYPE tt ON tt.KEY = st.KEY AND tt.TENANT_ID = ?"
                + " WHERE v.TENANT_ID = ?"
                + " ORDER BY v.LOOKUP_VALUE_PK",
            p.tenantId(), p.provisionedBy(), p.sourceTenantId(), p.tenantId(), p.sourceTenantId());

        log.info("Tenant {} provisioned by MDL: {} lookup types, {} lookup values", p.tenantCode(), types, values);
    }
}
