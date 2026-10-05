package com.erp.tenant.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.SmartLifecycle;

/**
 * erp-core 1.2.0 — the tenant resolver turns strict in a lifecycle phase that starts before Boot's
 * web-server lifecycle, so no request is served while the bootstrap sentinel is still tolerated.
 */
class TenantHibernateConfigurationTest {

    @Test
    void startsBeforeTheWebServerLifecycle() throws Exception {
        Class<?> webServerLifecycle =
            Class.forName("org.springframework.boot.web.server.servlet.context.WebServerStartStopLifecycle");
        Constructor<?> constructor =
            webServerLifecycle.getDeclaredConstructor(ServletWebServerApplicationContext.class, WebServer.class);
        constructor.setAccessible(true);
        int webServerPhase = ((SmartLifecycle) constructor.newInstance(null, null)).getPhase();

        TenantHibernateConfiguration configuration = new TenantHibernateConfiguration();

        assertThat(configuration.getPhase()).isLessThan(webServerPhase);
        assertThat(configuration.isAutoStartup()).isTrue();
    }

    @Test
    void startMakesTheResolverStrict_andStopNeverRelaxesIt() {
        TenantHibernateConfiguration configuration = new TenantHibernateConfiguration();
        assertThat(configuration.getResolver().isBootstrapping()).isTrue();
        assertThat(configuration.isRunning()).isFalse();

        configuration.start();

        assertThat(configuration.getResolver().isBootstrapping()).isFalse();
        assertThat(configuration.isRunning()).isTrue();

        configuration.stop();
        assertThat(configuration.getResolver().isBootstrapping()).isFalse();
    }
}
