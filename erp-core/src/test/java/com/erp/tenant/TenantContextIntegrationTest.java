package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.exception.LocalizedException;
import com.erp.tenant.exception.TenantErrorCodes;
import com.erp.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * erp-core step 05 — {@code TenantContext.runAs} outside a request: a system job on a plain thread
 * (no request, no tenant) reads tenant data only through {@code runAs}, sees exactly that tenant,
 * and without it fails fast with {@code TENANT_CONTEXT_MISSING}. JPQL by entity name keeps this
 * test free of SEC classes (the module boundary covers tests too).
 */
class TenantContextIntegrationTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void runAs_onAThreadWithoutARequest_seesExactlyThatTenant_andWithoutItFailsFast() throws Exception {
        TenantHttp http = new TenantHttp(port);
        String platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE,
            TenantHttp.platformOperator(jdbcTemplate, passwordEncoder));
        String code = TenantHttp.unique("JOB");
        long tenantId = http.provisionTenant(platformToken, code);
        http.createUser(http.token(code, "admin"), "job-user");

        AtomicReference<Long> tenantSeenWithoutRunAs = new AtomicReference<>(-99L);
        AtomicReference<Throwable> withoutRunAs = new AtomicReference<>();
        AtomicReference<List<String>> viaEntityManager = new AtomicReference<>();
        AtomicReference<List<String>> viaTransaction = new AtomicReference<>();
        AtomicReference<Long> tenantAfterRunAs = new AtomicReference<>(-99L);

        Thread job = new Thread(() -> {
            tenantSeenWithoutRunAs.set(TenantContext.current());
            try {
                entityManagerFactory.createEntityManager().close();
            } catch (Throwable e) {
                withoutRunAs.set(e);
            }
            viaEntityManager.set(TenantContext.callAs(tenantId, this::usernames));
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            TenantContext.runAs(tenantId, () -> viaTransaction.set(tx.execute(status -> usernames())));
            tenantAfterRunAs.set(TenantContext.current());
        }, "tenant-system-job");
        job.start();
        job.join(30_000);

        assertThat(tenantSeenWithoutRunAs.get()).as("a plain thread has no tenant").isNull();
        assertThat(rootCause(withoutRunAs.get()))
            .isInstanceOf(LocalizedException.class)
            .extracting(e -> ((LocalizedException) e).getErrorCode())
            .isEqualTo(TenantErrorCodes.TENANT_CONTEXT_MISSING);
        assertThat(viaEntityManager.get()).containsExactly("admin", "job-user");
        assertThat(viaTransaction.get()).containsExactly("admin", "job-user");
        assertThat(tenantAfterRunAs.get()).as("runAs restores the previous (absent) tenant").isNull();
    }

    private List<String> usernames() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery("select u.username from User u order by u.username", String.class).getResultList();
        } finally {
            em.close();
        }
    }

    private static Throwable rootCause(Throwable error) {
        assertThat(error).as("opening a session without a tenant must fail").isNotNull();
        Throwable cause = error;
        while (!(cause instanceof LocalizedException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
