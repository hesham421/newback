package com.erp.testsupport;

import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 * Runs every integration test method — including its {@code @BeforeEach}/{@code @AfterEach} and the
 * test-managed transaction — as the PLATFORM tenant (erp-core step 05), the tenant that owns all
 * seeded rows. Ordered before {@link TransactionalTestExecutionListener} because Hibernate binds a
 * session to the current tenant when the session opens, i.e. when the test transaction begins.
 *
 * <p>A test that needs another tenant switches with {@code TenantContext.runAs/callAs} around a
 * call that opens its own session (a non-transactional test, or {@code Propagation.REQUIRES_NEW}),
 * or goes through HTTP, where the server thread takes the tenant from the token / header.
 */
public class TenantContextTestExecutionListener extends AbstractTestExecutionListener {

    /** Before {@code TransactionalTestExecutionListener} (4000). */
    private static final int ORDER = 3_900;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        TenantContext.set(TenantConstants.PLATFORM_TENANT_ID);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        TenantContext.clear();
    }
}
