package com.erp.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.audit.service.AuditRetentionJob;
import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.testsupport.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * erp-core step 10 — {@link AuditRetentionJob} deletes rows older than
 * {@code erp.core.audit.retention-days}, in every tenant, keeps younger ones, and does nothing with the
 * default {@code 0}. Runs without a tenant (as a scheduled job would).
 */
class AuditRetentionJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditRetentionJob job;
    @Autowired
    private ErpCoreProperties properties;
    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void restoreRetention() {
        properties.getAudit().setRetentionDays(0);
    }

    @Test
    void retentionDeletesRowsOlderThanNDays_inEveryTenant_andKeepsTheRest() {
        long otherTenant = otherTenant();
        String marker = UUID.randomUUID().toString();
        insert(1L, marker + "-old", 40);
        insert(otherTenant, marker + "-old-other", 31);
        insert(1L, marker + "-young", 29);
        insert(otherTenant, marker + "-today", 0);

        assertThat(job.run()).as("default retention 0 keeps everything").isZero();
        assertThat(count(marker)).isEqualTo(4);

        properties.getAudit().setRetentionDays(30);
        assertThat(job.run()).isGreaterThanOrEqualTo(2);

        assertThat(jdbc.queryForList("SELECT entity_id FROM core_audit_event WHERE entity_id LIKE ? ORDER BY entity_id",
            String.class, marker + "%")).containsExactly(marker + "-today", marker + "-young");
    }

    private void insert(long tenantId, String entityId, int daysAgo) {
        // UTC wall-clock, like Hibernate and the audit store write Instants into TIMESTAMP columns
        jdbc.update("INSERT INTO core_audit_event (id, tenant_id, occurred_at, actor, actor_realm, action, entity_type,"
                + " entity_id, created_by, created_at) VALUES (nextval('SEQ_CORE_AUDIT_EVENT'), ?,"
                + " timezone('UTC', now()) - make_interval(days => ?), 'system', 'SYSTEM', 'CREATE', 'RETENTION_TEST', ?,"
                + " 'test', timezone('UTC', now()))",
            tenantId, daysAgo, entityId);
    }

    private int count(String marker) {
        return jdbc.queryForObject("SELECT count(*) FROM core_audit_event WHERE entity_id LIKE ?", Integer.class,
            marker + "%");
    }

    private long otherTenant() {
        long id = jdbc.queryForObject("SELECT nextval('SEQ_CORE_TENANT')", Long.class);
        jdbc.update("INSERT INTO core_tenant (id, code, name_ar, name_en, status_code, created_by, created_at)"
            + " VALUES (?, ?, 'مستأجر', 'Tenant', 'ACTIVE', 'test', now())", id,
            "RT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        return id;
    }
}
