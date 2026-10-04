package com.erp.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.audit.crossmodule.AuditApi;
import com.erp.audit.crossmodule.AuditChange;
import com.erp.audit.crossmodule.AuditEntry;
import com.erp.audit.exception.AuditErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * erp-core step 10 — {@link AuditApi#record}: defaults from the security and tenant context, the
 * sensitive-field denylist, action validation, and the transaction rule (the row belongs to the
 * caller's transaction: rolled back with it, committed with it). Also checks that the permission
 * contributor's catalog row was synchronized.
 */
class AuditApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditApi auditApi;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void record_fillsDefaults_fromTheTenantAndSecurityContext() {
        String entityId = UUID.randomUUID().toString();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "auditor", null, List.of(new SimpleGrantedAuthority("PERM_X"))));

        auditApi.record(AuditEntry.builder().action("STATUS_CHANGE").entityType("TEST_DOC").entityId(entityId)
            .summaryAr("اعتماد").summaryEn("Approved").reference("REF-1")
            .change(new AuditChange("status", "DRAFT", "APPROVED"))
            .change(new AuditChange("approvedAt", null, Instant.parse("2026-10-04T10:00:00Z")))
            .build());

        Map<String, Object> row = single(entityId);
        assertThat(row).containsEntry("tenant_id", 1L).containsEntry("actor", "auditor")
            .containsEntry("actor_realm", "STAFF").containsEntry("action", "STATUS_CHANGE")
            .containsEntry("reference", "REF-1").containsEntry("summary_en", "Approved");
        assertThat(row.get("occurred_at")).isNotNull();
        assertThat(row.get("ip")).isNull();      // no HTTP request on this thread
        String changes = (String) row.get("changes");
        assertThat(JsonPath.<List<String>>read(changes, "$[?(@.field == 'status')].new")).containsExactly("APPROVED");
        assertThat(JsonPath.<List<String>>read(changes, "$[?(@.field == 'status')].old")).containsExactly("DRAFT");
        assertThat(JsonPath.<List<String>>read(changes, "$[?(@.field == 'approvedAt')].new"))
            .containsExactly("2026-10-04T10:00:00Z");
    }

    @Test
    void record_withoutCaller_isSystem_andCustomerAuthority_isCustomerRealm() {
        String systemId = UUID.randomUUID().toString();
        auditApi.record(AuditEntry.builder().action("CREATE").entityType("TEST_DOC").entityId(systemId).build());
        assertThat(single(systemId)).containsEntry("actor", "system").containsEntry("actor_realm", "SYSTEM");

        String customerId = UUID.randomUUID().toString();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "buyer@shop.test", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        auditApi.record(AuditEntry.builder().action("CREATE").entityType("TEST_DOC").entityId(customerId).build());
        assertThat(single(customerId)).containsEntry("actor", "buyer@shop.test").containsEntry("actor_realm", "CUSTOMER");
    }

    @Test
    void record_dropsSensitiveChanges_whoeverSuppliesThem() {
        String entityId = UUID.randomUUID().toString();
        auditApi.record(AuditEntry.builder().action("UPDATE").entityType("TEST_DOC").entityId(entityId)
            .change(new AuditChange("passwordHash", "a", "b"))
            .change(new AuditChange("resetToken", "a", "b"))
            .change(new AuditChange("clientSecret", "a", "b"))
            .change(new AuditChange("contentHash", "a", "b"))
            .change(new AuditChange("apiKey", "a", "b"))
            .change(new AuditChange("title", "a", "b"))
            .build());

        assertThat(AuditRows.changedFields(single(entityId))).containsExactly("title");
        AuditRows.assertNoSensitiveFieldAnywhere(jdbc);
    }

    @Test
    void record_withAnInvalidAction_isRejected() {
        for (String invalid : new String[] {"login", "AB", "LOG-IN", "A".repeat(65), ""}) {
            assertThatThrownBy(() -> auditApi.record(AuditEntry.builder().action(invalid).build()))
                .as(invalid).isInstanceOf(LocalizedException.class)
                .extracting(e -> ((LocalizedException) e).getErrorCode()).isEqualTo(AuditErrorCodes.AUDIT_ACTION_INVALID);
        }
        assertThatThrownBy(() -> auditApi.record(AuditEntry.builder().build())).isInstanceOf(LocalizedException.class);
    }

    @Test
    void record_inARolledBackTransaction_leavesNoRow_andInACommittedOne_staysWritten() {
        String rolledBack = UUID.randomUUID().toString();
        transactionTemplate.executeWithoutResult(status -> {
            auditApi.record(AuditEntry.builder().action("CREATE").entityType("TEST_DOC").entityId(rolledBack).build());
            assertThat(countInTransaction(rolledBack)).isEqualTo(1);   // visible inside the caller's transaction
            status.setRollbackOnly();
        });
        assertThat(AuditRows.of(jdbc, "TEST_DOC", rolledBack)).isEmpty();

        String committed = UUID.randomUUID().toString();
        transactionTemplate.executeWithoutResult(status ->
            auditApi.record(AuditEntry.builder().action("CREATE").entityType("TEST_DOC").entityId(committed).build()));
        assertThat(AuditRows.of(jdbc, "TEST_DOC", committed)).hasSize(1);
    }

    @Test
    void record_withoutAnyTenant_failsFast_andAnExplicitTenantWins() {
        String explicit = UUID.randomUUID().toString();
        TenantContext.clear();
        try {
            assertThatThrownBy(() -> auditApi.record(AuditEntry.builder().action("CREATE").build()))
                .isInstanceOf(LocalizedException.class);
            auditApi.record(AuditEntry.builder().tenantId(1L).action("CREATE").entityType("TEST_DOC")
                .entityId(explicit).build());
        } finally {
            TenantContext.set(1L);
        }
        assertThat(single(explicit)).containsEntry("tenant_id", 1L);
    }

    @Test
    void theReadPermission_isSynchronizedIntoTheCatalog_fromTheContributor() {
        Map<String, Object> row = jdbc.queryForMap("SELECT m.code module_code, s.page_code, a.action_code, a.name_en"
            + " FROM sec_action_reg a JOIN sec_screen_reg s ON s.screen_reg_pk = a.screen_id"
            + " JOIN sec_module_reg m ON m.module_reg_pk = s.module_id WHERE a.permission_code = 'AUDIT:EVENT:READ'");
        assertThat(row).containsEntry("module_code", "AUDIT").containsEntry("page_code", "AUDIT_EVENTS")
            .containsEntry("action_code", "VIEW").containsEntry("name_en", "Audit events - VIEW");
    }

    private Map<String, Object> single(String entityId) {
        return jdbc.queryForMap("SELECT tenant_id, actor, actor_realm, action, reference, summary_en, occurred_at, ip,"
            + " changes::text AS changes FROM core_audit_event WHERE entity_id = ?", entityId);
    }

    private int countInTransaction(String entityId) {
        return jdbc.queryForObject("SELECT count(*) FROM core_audit_event WHERE entity_id = ?", Integer.class, entityId);
    }
}
