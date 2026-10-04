package com.erp.sequence.tenant;

import com.erp.tenant.TenantProvisioning;
import com.erp.tenant.TenantProvisioningContributor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The sequence module's part of tenant provisioning (erp-core step 09): a new tenant starts with the
 * source (PLATFORM) tenant's series definitions — one row per code (the code's anchor row: configuration,
 * reset policy and period), with the counter back at 1 — so numbering configured on the platform (e.g.
 * by an application's {@code V1000+} seed) also works in tenants created later. Each tenant then counts
 * on its own.
 *
 * <p>Explicit SQL by design (see {@link TenantProvisioningContributor}): the insert names the new tenant,
 * the read names the source tenant ({@code TENANT_ID} in every predicate, the sub-select included).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SequenceTenantProvisioningContributor implements TenantProvisioningContributor {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int order() {
        return 40;
    }

    @Override
    public void provision(TenantProvisioning p) {
        int series = jdbcTemplate.update(
            "INSERT INTO CORE_NUMBER_SERIES (ID, TENANT_ID, CODE, PREFIX, PATTERN, RESET_POLICY, PERIOD_KEY,"
                + " NEXT_VALUE, IS_ACTIVE, CREATED_BY, CREATED_AT)"
                + " SELECT nextval('SEQ_CORE_NUMBER_SERIES'), ?, s.CODE, s.PREFIX, s.PATTERN, s.RESET_POLICY,"
                + " s.PERIOD_KEY, 1, s.IS_ACTIVE, ?, now()"
                + " FROM CORE_NUMBER_SERIES s WHERE s.TENANT_ID = ?"
                + " AND s.ID = (SELECT MIN(x.ID) FROM CORE_NUMBER_SERIES x WHERE x.TENANT_ID = ? AND x.CODE = s.CODE)"
                + " ORDER BY s.ID",
            p.tenantId(), p.provisionedBy(), p.sourceTenantId(), p.sourceTenantId());
        log.info("Tenant {} provisioned by SEQUENCE: {} number series", p.tenantCode(), series);
    }
}
