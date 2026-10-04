package com.erp.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.file.service.DownloadTokenStore;
import com.erp.file.service.InMemoryDownloadTokenStore;
import com.acme.AcmeAppCoveringCorePackages;
import com.acme.AcmeAppWithOwnRepositories;
import com.acme.AcmeAppWithoutRepositoryConfig;
import com.acme.widget.WidgetRepository;
import com.erp.ErpRootConsumerApplication;
import com.erp.mdl.repository.LookupTypeRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.testsupport.TestPostgres;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
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

    /** No user configuration yet: each test (or {@link #runner}) adds the consumer it needs. */
    private final WebApplicationContextRunner baseRunner = new WebApplicationContextRunner()
        .withClassLoader(new FilteredClassLoader("org.springframework.data.redis", "org.springframework.mail",
            "jakarta.mail"))
        .withPropertyValues(
            "spring.datasource.url=" + TestPostgres.jdbcUrl(),
            "spring.datasource.username=" + TestPostgres.username(),
            "spring.datasource.password=" + TestPostgres.password(),
            "spring.jpa.open-in-view=false",
            "logging.level.root=WARN");

    private final WebApplicationContextRunner runner = baseRunner.withUserConfiguration(ConsumerApplication.class);

    @Test
    void contextLoads_withRequiredPropertiesOnly_noRedis_noMail() {
        runner.withPropertyValues(JWT_SECRET, FILE_SECRET).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean(ErpCoreSecurityAutoConfiguration.FILTER_CHAIN_BEAN_NAME);
            assertThat(context).hasBean(ErpCoreSecurityAutoConfiguration.CUSTOMER_FILTER_CHAIN_BEAN_NAME);
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
                // erp-core step 06: the only other chain is the core customer chain (own bean name)
                assertThat(context).getBeans(SecurityFilterChain.class).containsOnlyKeys(
                    ErpCoreSecurityAutoConfiguration.FILTER_CHAIN_BEAN_NAME,
                    ErpCoreSecurityAutoConfiguration.CUSTOMER_FILTER_CHAIN_BEAN_NAME);
            });
    }

    /** erp-core step 06 — the customer chain backs off by its own bean name, the staff chain stays. */
    @Test
    void coreCustomerSecurityFilterChain_backsOff_whenTheApplicationDefinesOneWithTheSameName() {
        runner.withPropertyValues(JWT_SECRET, FILE_SECRET)
            .withUserConfiguration(OwnCustomerChain.class)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(ErpCoreSecurityAutoConfiguration.CUSTOMER_FILTER_CHAIN_BEAN_NAME))
                    .isSameAs(OwnCustomerChain.OWN);
                assertThat(context).getBeans(SecurityFilterChain.class).containsOnlyKeys(
                    ErpCoreSecurityAutoConfiguration.FILTER_CHAIN_BEAN_NAME,
                    ErpCoreSecurityAutoConfiguration.CUSTOMER_FILTER_CHAIN_BEAN_NAME);
                assertThat(context.getBean(ErpCoreProperties.class).getSecurity().getCustomerPublicPaths())
                    .containsExactlyElementsOf(ErpCoreProperties.Security.DEFAULT_CUSTOMER_PUBLIC_PATHS);
            });
    }

    /**
     * An application's replacement customer chain. It must be ordered before the core staff chain,
     * which matches every request (Spring Security rejects an unreachable chain).
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class OwnCustomerChain {

        static final SecurityFilterChain OWN = new DefaultSecurityFilterChain(
            request -> request.getRequestURI().startsWith("/api/v1/customers/"), List.of());

        @org.springframework.context.annotation.Bean(ErpCoreSecurityAutoConfiguration.CUSTOMER_FILTER_CHAIN_BEAN_NAME)
        @org.springframework.core.annotation.Order(ErpCoreSecurityAutoConfiguration.CUSTOMER_FILTER_CHAIN_ORDER)
        SecurityFilterChain erpCoreCustomerSecurityFilterChain() {
            return OWN;
        }
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

    // --- Review round 1: repository / entity scan back-off and package overlap -------------------

    @Test
    void appWithItsOwnEnableJpaRepositories_coreRepositoriesStillRegisteredOnce_appRepositoryOnce() {
        // Case A: consumer in com.acme declares @EnableJpaRepositories("com.acme").
        baseRunner.withUserConfiguration(AcmeAppWithOwnRepositories.class)
            .withPropertyValues(JWT_SECRET, FILE_SECRET)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(WidgetRepository.class);
                assertThat(context).hasSingleBean(UserRepository.class);
                assertThat(context).hasSingleBean(LookupTypeRepository.class);
                assertSingleFactoryBeanPerRepository(context);
            });
    }

    @Test
    void appWhoseEnableJpaRepositoriesCoversCorePackages_coreDoesNotRegisterThemAgain() {
        baseRunner.withUserConfiguration(AcmeAppCoveringCorePackages.class)
            .withPropertyValues(JWT_SECRET, FILE_SECRET)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(WidgetRepository.class);
                assertThat(context).hasSingleBean(UserRepository.class);
                assertSingleFactoryBeanPerRepository(context);
            });
    }

    @Test
    void appWithoutRepositoryConfig_getsItsOwnRepositoriesAndEntitiesFromItsPackage() {
        baseRunner.withUserConfiguration(AcmeAppWithoutRepositoryConfig.class)
            .withPropertyValues(JWT_SECRET, FILE_SECRET)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(WidgetRepository.class);
                assertThat(context).hasSingleBean(UserRepository.class);
                assertThat(EntityScanPackages.get(context.getBeanFactory()).getPackageNames())
                    .contains("com.acme").contains(ErpCoreAutoConfiguration.CORE_PACKAGES);
                assertSingleFactoryBeanPerRepository(context);
            });
    }

    @Test
    void appConfigurationInAParentOfTheCorePackages_collapsesOverlappingPackages() {
        // Case B: consumer configuration in com.erp, which covers every core package.
        baseRunner.withUserConfiguration(ErpRootConsumerApplication.class)
            .withPropertyValues(JWT_SECRET, FILE_SECRET)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(UserRepository.class);
                assertThat(context).hasSingleBean(LookupTypeRepository.class);
                assertThat(EntityScanPackages.get(context.getBeanFactory()).getPackageNames())
                    .containsExactly("com.erp");
                assertSingleFactoryBeanPerRepository(context);
            });
    }

    @Test
    void collapsePackages_dropsPackagesCoveredByAnotherEntry() {
        assertThat(ErpCoreAutoConfiguration.collapsePackages(List.of(
            "com.erp.common", "com.erp.sec", "com.erp", "com.erp.sec", "com.acme", "com.acmeplus", "com.acme.widget")))
            .containsExactly("com.erp", "com.acme", "com.acmeplus");
        assertThat(ErpCoreAutoConfiguration.collapsePackages(List.of(ErpCoreAutoConfiguration.CORE_PACKAGES)))
            .containsExactly(ErpCoreAutoConfiguration.CORE_PACKAGES);
    }

    /** Every repository interface is backed by exactly one Spring Data factory bean definition. */
    private static void assertSingleFactoryBeanPerRepository(
            org.springframework.boot.test.context.assertj.AssertableWebApplicationContext context) {
        String[] factoryBeans = context.getBeanFactory().getBeanNamesForType(RepositoryFactoryBeanSupport.class, true, false);
        assertThat(factoryBeans).isNotEmpty();
        List<String> interfaces = java.util.Arrays.stream(factoryBeans)
            .map(name -> context.getBeanFactory().getBean("&" + name, RepositoryFactoryBeanSupport.class)
                .getObjectType().getName())
            .toList();
        assertThat(interfaces).doesNotHaveDuplicates();
    }

    @Test
    void corePackages_areTheSevenCoreModules() {
        // erp-core step 05 appended com.erp.tenant, step 08 com.erp.events, step 09 com.erp.sequence
        assertThat(ErpCoreAutoConfiguration.CORE_PACKAGES).containsExactly(
            "com.erp.common", "com.erp.cu", "com.erp.mdl", "com.erp.sec", "com.erp.file", "com.erp.notif",
            "com.erp.tenant", "com.erp.events", "com.erp.sequence");
    }
}
