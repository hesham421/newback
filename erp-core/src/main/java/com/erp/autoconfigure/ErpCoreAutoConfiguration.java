package com.erp.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.util.Streamable;
import org.springframework.util.StringUtils;

/**
 * The erp-core composition root, applied to any Spring Boot application that has erp-core on its
 * classpath. It component-scans, entity-scans and registers the JPA repositories of the core
 * packages ({@link #CORE_PACKAGES}), and relies on Boot's standard
 * {@code HibernateJpaAutoConfiguration} for the EntityManagerFactory (dialect auto-detected, no
 * default schema set by core).
 *
 * <p>Declaring core entity-scan and repository packages switches off Boot's default "scan the
 * {@code @SpringBootApplication} package" behaviour for entities and repositories, so the
 * application's auto-configuration packages are added to both as well: an application keeps
 * finding its own entities and repositories without extra annotations.
 *
 * <p>Also contributes the {@code messageSource}: the core i18n bundle ({@value #CORE_MESSAGES_BASENAME})
 * behind any bundles the application lists in {@code spring.messages.basename}, so application keys
 * win and core error codes always resolve.
 */
@AutoConfiguration(before = {
    HibernateJpaAutoConfiguration.class,
    DataJpaRepositoriesAutoConfiguration.class,
    MessageSourceAutoConfiguration.class})
@EnableConfigurationProperties(ErpCoreProperties.class)
@ComponentScan(basePackages = ErpCoreAutoConfiguration.CORE_PACKAGE_LIST, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)})
@Import({ErpCoreAutoConfiguration.CoreEntityScanRegistrar.class,
    ErpCoreAutoConfiguration.CoreJpaRepositoriesRegistrar.class})
public class ErpCoreAutoConfiguration {

    /**
     * The core packages, comma-separated — <b>the one place to edit</b> when a step adds a core
     * package (tenant in 05, events in 08, sequence in 09, audit in 10, report in 11). It is a
     * {@code String} because an annotation attribute cannot reference a {@code String[]} constant;
     * {@code @ComponentScan} tokenizes it on commas, and {@link #CORE_PACKAGES} is derived from it.
     */
    public static final String CORE_PACKAGE_LIST =
        "com.erp.common,com.erp.cu,com.erp.mdl,com.erp.sec,com.erp.file,com.erp.notif";

    /** The core packages: component scan, entity scan and JPA repositories all use exactly these. */
    public static final String[] CORE_PACKAGES = StringUtils.commaDelimitedListToStringArray(CORE_PACKAGE_LIST);

    /** Classpath basename of the core message bundles ({@code messages.properties}, {@code messages_ar.properties}). */
    public static final String CORE_MESSAGES_BASENAME = "i18n/messages";

    /**
     * Application bundles first ({@code spring.messages.basename}, when set), then the core bundle.
     * UTF-8 by default, and no fallback to the JVM locale: on a host whose default locale is Arabic
     * a fallback would answer English requests with Arabic messages.
     */
    @Bean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)
    @ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME,
        search = SearchStrategy.CURRENT)
    public MessageSource messageSource(Environment environment) {
        List<String> basenames = new ArrayList<>();
        for (String basename : StringUtils.commaDelimitedListToStringArray(
                environment.getProperty("spring.messages.basename", ""))) {
            String trimmed = basename.trim();
            if (!trimmed.isEmpty() && !trimmed.equals(CORE_MESSAGES_BASENAME)) {
                basenames.add(trimmed);
            }
        }
        basenames.add(CORE_MESSAGES_BASENAME);

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames(basenames.toArray(String[]::new));
        messageSource.setDefaultEncoding(environment.getProperty("spring.messages.encoding", "UTF-8"));
        messageSource.setFallbackToSystemLocale(
            environment.getProperty("spring.messages.fallback-to-system-locale", Boolean.class, false));
        messageSource.setUseCodeAsDefaultMessage(
            environment.getProperty("spring.messages.use-code-as-default-message", Boolean.class, false));
        messageSource.setAlwaysUseMessageFormat(
            environment.getProperty("spring.messages.always-use-message-format", Boolean.class, false));
        return messageSource;
    }

    /** The core packages followed by the application's auto-configuration packages (when known), without duplicates. */
    static String[] corePlusApplicationPackages(BeanFactory beanFactory) {
        Set<String> packages = new LinkedHashSet<>(Arrays.asList(CORE_PACKAGES));
        if (beanFactory != null && AutoConfigurationPackages.has(beanFactory)) {
            packages.addAll(AutoConfigurationPackages.get(beanFactory));
        }
        return packages.toArray(String[]::new);
    }

    /** Registers the core (plus application) packages for Boot's JPA entity scanning. */
    static class CoreEntityScanRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

        private BeanFactory beanFactory;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            EntityScanPackages.register(registry, corePlusApplicationPackages(beanFactory));
        }
    }

    /**
     * Registers the Spring Data JPA repositories found in the core (plus application) packages, the
     * way Boot's own {@code DataJpaRepositoriesRegistrar} does for the application packages alone
     * (an {@code @EnableJpaRepositories} annotation cannot take the derived array).
     */
    static class CoreJpaRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

        private BeanFactory beanFactory;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            super.setBeanFactory(beanFactory);
            this.beanFactory = beanFactory;
        }

        @Override
        protected Streamable<String> getBasePackages() {
            return Streamable.of(corePlusApplicationPackages(beanFactory));
        }

        @Override
        protected Class<? extends java.lang.annotation.Annotation> getAnnotation() {
            return EnableJpaRepositories.class;
        }

        @Override
        protected Class<?> getConfiguration() {
            return EnableCoreJpaRepositories.class;
        }

        @Override
        protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
            return new JpaRepositoryConfigExtension();
        }

        @EnableJpaRepositories
        private static final class EnableCoreJpaRepositories {
        }
    }
}
