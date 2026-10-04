package com.erp.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.file.service.DownloadTokenStore;
import com.erp.file.service.InMemoryDownloadTokenStore;
import com.erp.testsupport.TestPostgres;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * erp-core's auto-configuration as a consumer gets it: an application with nothing but
 * {@code @EnableAutoConfiguration}, a datasource and the {@code erp.core.*} properties. Redis and
 * mail classes are hidden from the context class loader, so their absence from a consumer's
 * classpath is what is exercised (both are optional dependencies of erp-core).
 */
class ErpCoreAutoConfigurationTest {

    private static final String JWT_SECRET = "erp.core.security.jwt.secret="
        + "autoconfig-test-jwt-secret-0123456789abcdef0123456789abcdef";
    private static final String FILE_SECRET = "erp.core.files.access-token-secret="
        + "autoconfig-test-file-token-secret-0123456789abcdef";

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class ConsumerApplication {
    }

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withClassLoader(new FilteredClassLoader("org.springframework.data.redis", "org.springframework.mail",
            "jakarta.mail"))
        .withUserConfiguration(ConsumerApplication.class)
        .withPropertyValues(
            "spring.datasource.url=" + TestPostgres.jdbcUrl(),
            "spring.datasource.username=" + TestPostgres.username(),
            "spring.datasource.password=" + TestPostgres.password(),
            "spring.jpa.open-in-view=false",
            "logging.level.root=WARN");

    @Test
    void contextLoads_withRequiredPropertiesOnly_noRedis_noMail() {
        runner.withPropertyValues(JWT_SECRET, FILE_SECRET).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean(ErpCoreSecurityAutoConfiguration.FILTER_CHAIN_BEAN_NAME);
            assertThat(context).getBeanNames(StringRedisTemplate.class).isEmpty();
            assertThat(context).getBeanNames(JavaMailSender.class).isEmpty();
            assertThat(context.getBean(DownloadTokenStore.class)).isInstanceOf(InMemoryDownloadTokenStore.class);
            ErpCoreProperties properties = context.getBean(ErpCoreProperties.class);
            assertThat(properties.getSecurity().getPublicPaths())
                .containsExactlyElementsOf(ErpCoreProperties.Security.DEFAULT_PUBLIC_PATHS);
            assertThat(properties.getSecurity().getJwt().getExpirationMs()).isEqualTo(3_600_000L);
            assertThat(properties.getFiles().getMaxContentBytes()).isEqualTo(5_242_880L);
            assertThat(properties.getFiles().getMaxRequestBytes()).isEqualTo(10_485_760L);
            assertThat(properties.getFrontend().getPasswordResetPath()).isEqualTo("/reset");
            // Core i18n bundle resolves without any spring.messages.* configuration.
            assertThat(context.getMessage("SEC-409-RESET-TOKEN-INVALID", null, java.util.Locale.ENGLISH))
                .isEqualTo("This reset link is invalid or has expired");
        });
    }

    @Test
    void coreSecurityFilterChain_backsOff_whenTheApplicationDefinesOneWithTheSameName() {
        SecurityFilterChain own = new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE, List.of());
        runner.withPropertyValues(JWT_SECRET, FILE_SECRET)
            .withBean(ErpCoreSecurityAutoConfiguration.FILTER_CHAIN_BEAN_NAME, SecurityFilterChain.class, () -> own)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(ErpCoreSecurityAutoConfiguration.FILTER_CHAIN_BEAN_NAME)).isSameAs(own);
                assertThat(context).getBeans(SecurityFilterChain.class).hasSize(1);
            });
    }

    @Test
    void missingJwtSecret_failsFast_withAMessageNamingTheProperty() {
        runner.withPropertyValues(FILE_SECRET).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .rootCause()
                .hasMessageContaining("erp.core.security.jwt.secret must be set");
        });
    }

    @Test
    void corePackages_areTheSixCoreModules() {
        assertThat(ErpCoreAutoConfiguration.CORE_PACKAGES).containsExactly(
            "com.erp.common", "com.erp.cu", "com.erp.mdl", "com.erp.sec", "com.erp.file", "com.erp.notif");
    }
}
