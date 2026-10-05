package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.tenant.config.TenantHibernateConfiguration;
import com.erp.testsupport.AbstractIntegrationTest;
import com.erp.testsupport.WebServerStartTenantProbe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * erp-core 1.1.1 — by the time the embedded web server accepts requests, the tenant resolver is
 * already strict: there is no window in which a request runs with the bootstrap sentinel tenant.
 * Before 1.1.1 the switch happened on {@code ContextRefreshedEvent}, after the web server started.
 */
class TenantBootstrapWindowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebServerStartTenantProbe probe;
    @Autowired
    private TenantHibernateConfiguration tenantConfiguration;

    @Test
    void theResolverIsStrictBeforeTheWebServerAcceptsRequests() {
        assertThat(probe.tenantlessSessionRejectedAtWebServerStart()).as("web server start observed").isNotNull();
        assertThat(probe.tenantlessSessionRejectedAtWebServerStart())
            .as("a session without a tenant is refused once the web server accepts requests").isTrue();
        assertThat(tenantConfiguration.getResolver().isBootstrapping()).isFalse();
        assertThat(tenantConfiguration.isRunning()).isTrue();
    }
}
